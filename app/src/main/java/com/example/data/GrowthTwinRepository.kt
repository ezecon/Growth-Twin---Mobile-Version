package com.example.data

import com.example.domain.CropCatalog
import com.example.domain.DailyLogEntry
import com.example.domain.HarvestCropStat
import com.example.domain.HarvestRecord
import com.example.domain.MoistureCalibration
import com.example.domain.PlotState
import com.example.domain.PlotTwinEngine
import com.example.domain.SentinelEngine
import com.example.domain.SlotValue
import com.example.domain.TrustAggregator
import com.example.domain.TwinEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

class GrowthTwinRepository(
    private val db: GrowthTwinDatabase,
    val prefs: PreferencesStore
) {
    private val plotDao = db.plotDao()
    private val logDao = db.plotLogDao()
    private val eventDao = db.plotEventDao()
    private val harvestDao = db.harvestDao()
    private val advisorDao = db.advisorDao()
    private val telemetryDao = db.telemetryDao()
    private val userAccountDao = db.userAccountDao()

    fun observePlots(): Flow<List<PlotState>> = plotDao.observeAll().map { rows ->
        rows.map { PlotTwinEngine.fromJson(it.stateJson) }
    }

    fun observeLogs(plotId: String): Flow<List<DailyLogEntry>> = logDao.observeForPlot(plotId).map { rows ->
        rows.map {
            DailyLogEntry(
                it.day, it.date, it.stageId, it.stageBn, it.stageEn, it.status,
                it.aw, it.ph, it.ec, it.n, it.p, it.k, it.dis, it.health,
                it.pumpOn, it.irrigationMm, it.estYieldKg, it.tmax, it.tmin, it.rainMm, it.rh
            )
        }
    }

    fun observeEvents(plotId: String): Flow<List<TwinEvent>> = eventDao.observeForPlot(plotId).map { rows ->
        rows.map { TwinEvent(it.day, it.kind, it.textBn, it.textEn, it.eventKey, null) }
    }

    fun observeTelemetry(plotId: String): Flow<List<TelemetryReadingEntity>> = telemetryDao.observeForPlot(plotId)

    fun observeUserHarvests(): Flow<List<HarvestRecord>> = harvestDao.observeAll().map { rows ->
        rows.map {
            HarvestRecord(
                it.plotId, it.userId, it.cropId, it.soil, it.area, it.yieldKg,
                it.yieldKgPerDecimal, it.revenue, it.cost, it.profit, it.days,
                it.health, it.startDate, it.outcome, it.source
            )
        }
    }

    fun observeReviewQueue(): Flow<List<AdvisorSessionEntity>> = advisorDao.observeReviewQueue()

    fun observeAdvisorSessions(): Flow<List<AdvisorSessionEntity>> = advisorDao.observeAll()

    suspend fun ensureSeeded(userId: String) {
        val existing = plotDao.getAll()
        if (existing.isEmpty()) {
            val sample = PlotTwinEngine.newPlot(
                userId = userId,
                name = "উত্তর জমি • বোরো",
                cropId = "boro",
                soil = "doash",
                area = 33.0,
                ph = 6.2,
                ec = 1.1,
                startDate = LocalDate.now().minusDays(18).toString(),
                seed = 424242
            )
            val outcome = PlotTwinEngine.advance(sample, 18, stopOnAlert = false, autoHarvest = false)
            savePlotAndSideEffects(sample, outcome.logs, outcome.events)
        }
    }

    suspend fun savePlot(s: PlotState) {
        plotDao.upsert(
            PlotEntity(
                id = s.id,
                userId = s.userId,
                cropId = s.cropId,
                name = s.name,
                status = s.status,
                day = s.day,
                health = s.health.toInt(),
                stateJson = PlotTwinEngine.toJson(s),
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun savePlotAndSideEffects(
        s: PlotState,
        logs: List<DailyLogEntry> = emptyList(),
        events: List<TwinEvent> = emptyList()
    ) {
        if (s.status in setOf("harvested", "dead") && !s.harvestSaved && s.result != null) {
            val r = s.result!!
            harvestDao.upsert(
                HarvestEntity(
                    plotId = s.id,
                    userId = s.userId,
                    cropId = s.cropId,
                    soil = s.soil,
                    area = s.area,
                    yieldKg = r.yieldKg,
                    yieldKgPerDecimal = round(r.yieldKg / s.area * 100.0) / 100.0,
                    revenue = r.revenue,
                    cost = r.cost,
                    profit = r.profit,
                    days = r.day,
                    health = r.health,
                    startDate = s.startDate,
                    outcome = r.outcome,
                    source = "simulation"
                )
            )
            s.harvestSaved = true
        }
        savePlot(s)
        if (logs.isNotEmpty()) {
            logDao.insertAll(logs.map {
                PlotLogEntity(
                    s.id, it.day, it.date, it.stageId, it.stageBn, it.stageEn, it.status,
                    it.aw, it.ph, it.ec, it.n, it.p, it.k, it.dis, it.health,
                    it.pumpOn, it.irrigationMm, it.estYieldKg, it.tmax, it.tmin, it.rainMm, it.rh
                )
            })
        }
        if (events.isNotEmpty()) {
            val now = System.currentTimeMillis()
            eventDao.insertAll(events.mapIndexed { i, e ->
                PlotEventEntity(0, s.id, e.day, e.kind, e.textBn, e.textEn, e.key, now + i)
            })
        }
    }

    suspend fun deletePlot(plotId: String) {
        plotDao.delete(plotId)
        logDao.deleteForPlot(plotId)
        eventDao.deleteForPlot(plotId)
        telemetryDao.deleteForPlot(plotId)
    }

    suspend fun generateSimulatedTelemetry(plot: PlotState, count: Int = 10, deviceId: String = "SIM-ESP32-01") {
        val stateMoisture = plot.awPct().coerceIn(0.0, 100.0)
        val rng = Random(System.currentTimeMillis() + plot.day)
        val cal = plot.sensorConfig ?: MoistureCalibration()
        val existing = telemetryDao.getForPlot(plot.id)
        val startSeq = (existing.maxOfOrNull { it.sequence } ?: 0)
        val now = Instant.now()
        val rows = mutableListOf<TelemetryReadingEntity>()
        val denom = if (cal.adcAtFieldCapacity != cal.adcAtWiltingPoint) {
            (cal.adcAtFieldCapacity - cal.adcAtWiltingPoint).toDouble()
        } else -1500.0

        for (i in 0 until count) {
            val moisture = round((stateMoisture + (rng.nextDouble() - 0.5) * 14.0 + (i - count / 2.0) * 0.18).coerceIn(0.0, 100.0) * 100.0) / 100.0
            val rawAdc = (cal.adcAtWiltingPoint + (moisture / 100.0) * denom).toInt().coerceIn(cal.adcMin, cal.adcMax)
            val gap = SentinelEngine.correctState(stateMoisture, moisture, SentinelEngine.ALPHA_DEFAULT, 0.0, 160.0, "valid")
            val ts = now.minusSeconds(((count - i - 1) * 30).toLong()).toString()
            rows.add(
                TelemetryReadingEntity(
                    plotId = plot.id,
                    source = "simulation",
                    deviceId = deviceId,
                    sequence = startSeq + i + 1,
                    timestampIso = ts,
                    rawMoistureAdc = rawAdc,
                    calibratedMoisturePct = moisture,
                    predictedMoisturePct = gap.predictedValue,
                    correctedMoisturePct = gap.estimatedValue,
                    residualPct = gap.residual,
                    measurementUsed = gap.measurementUsed,
                    quality = gap.quality,
                    temperatureC = round((24.0 + rng.nextDouble() * 10.0) * 10.0) / 10.0,
                    humidityPct = round((55.0 + rng.nextDouble() * 35.0) * 10.0) / 10.0
                )
            )
        }
        telemetryDao.insertAll(rows)
    }

    suspend fun ingestDeviceReading(
        plot: PlotState,
        deviceId: String,
        sequence: Int,
        rawAdc: Int,
        temperatureC: Double?,
        humidityPct: Double?
    ): Result<TelemetryReadingEntity> {
        val cal = plot.sensorConfig ?: MoistureCalibration(deviceId = deviceId)
        val err = cal.validate()
        if (err != null) return Result.failure(IllegalArgumentException(err))
        if (rawAdc !in cal.adcMin..cal.adcMax) {
            return Result.failure(IllegalArgumentException("raw_adc must be between ${cal.adcMin} and ${cal.adcMax}"))
        }
        val prev = telemetryDao.getForPlot(plot.id).firstOrNull { it.deviceId == deviceId && it.source == "real" }
        var prevIdentical = 0
        if (prev != null && prev.rawMoistureAdc == rawAdc) {
            val allPrev = telemetryDao.getForPlot(plot.id).filter { it.deviceId == deviceId && it.source == "real" }
            for (r in allPrev) {
                if (r.rawMoistureAdc == rawAdc) prevIdentical++ else break
            }
        }
        val (calibratedPct, calQuality) = cal.convert(rawAdc)
        val health = SentinelEngine.assessReading(rawAdc, prev?.rawMoistureAdc, prevIdentical, calQuality)
        val predicted = plot.awPct()
        val gap = SentinelEngine.correctState(predicted, calibratedPct, SentinelEngine.ALPHA_DEFAULT, 0.0, 160.0, health.quality)
        if (gap.measurementUsed) {
            val ev = PlotTwinEngine.applyReading(plot, ph = null, ec = null, moisturePct = gap.estimatedValue, source = "sensor")
            savePlotAndSideEffects(plot, events = listOf(ev))
        }
        val entity = TelemetryReadingEntity(
            plotId = plot.id,
            source = "real",
            deviceId = deviceId,
            sequence = sequence,
            timestampIso = Instant.now().toString(),
            rawMoistureAdc = rawAdc,
            calibratedMoisturePct = calibratedPct,
            predictedMoisturePct = gap.predictedValue,
            correctedMoisturePct = gap.estimatedValue,
            residualPct = gap.residual,
            measurementUsed = gap.measurementUsed,
            quality = health.quality,
            temperatureC = temperatureC,
            humidityPct = humidityPct
        )
        telemetryDao.insertAll(listOf(entity))
        return Result.success(entity)
    }

    suspend fun getHarvestStats(userId: String): List<HarvestCropStat> {
        val mine = harvestDao.getAll().filter { it.userId == userId && it.outcome == "harvested" }
        return mine.groupBy { it.cropId }.map { (cropId, list) ->
            val avg = round(list.map { it.yieldKgPerDecimal }.average() * 100.0) / 100.0
            HarvestCropStat(
                cropId = cropId,
                harvests = list.size,
                avgYieldPerDecimal = avg,
                best = list.maxOf { it.yieldKgPerDecimal },
                worst = list.minOf { it.yieldKgPerDecimal }
            )
        }
    }

    suspend fun saveAdvisorSession(
        sessionId: String,
        userId: String,
        prompt: String,
        slots: Map<String, SlotValue>,
        askedSlots: List<String>,
        reviewStatus: String = "none",
        expertVerdict: String? = null,
        expertNote: String? = null
    ) {
        val obj = JSONObject()
        slots.forEach { (k, v) ->
            obj.put(k, JSONObject().apply { put("v", v.value); put("s", v.source) })
        }
        advisorDao.upsert(
            AdvisorSessionEntity(
                sessionId = sessionId,
                userId = userId,
                prompt = prompt,
                slotsJson = obj.toString(),
                askedSlotsCsv = askedSlots.joinToString(","),
                reviewStatus = reviewStatus,
                expertVerdict = expertVerdict,
                expertNote = expertNote,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    fun decodeSlotsJson(raw: String): Map<String, SlotValue> {
        val out = mutableMapOf<String, SlotValue>()
        val obj = JSONObject(raw)
        obj.keys().forEach { k ->
            val sub = obj.getJSONObject(k)
            out[k] = SlotValue(sub.getString("v"), sub.getString("s"))
        }
        return out
    }

    private fun hashPassword(email: String, password: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("${email.lowercase().trim()}:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun registerLocal(
        name: String,
        mobile: String,
        email: String,
        password: String,
        role: String
    ): Result<AuthSessionResult> {
        val cleanEmail = email.lowercase().trim()
        if (userAccountDao.getByEmail(cleanEmail) != null) {
            return Result.failure(IllegalStateException("An account with this email already exists"))
        }
        val uid = cleanEmail.substringBefore("@").replace(Regex("[^a-zA-Z0-9_-]"), "").take(24).ifBlank { "farmer" }
        val ent = UserAccountEntity(
            email = cleanEmail,
            userId = uid,
            name = name.trim(),
            mobile = mobile.trim(),
            role = role,
            passwordHash = hashPassword(cleanEmail, password),
            approved = role == "farmer",
            createdAtMs = System.currentTimeMillis()
        )
        userAccountDao.upsert(ent)
        return Result.success(
            AuthSessionResult(
                userId = ent.userId,
                name = ent.name,
                email = ent.email,
                mobile = ent.mobile,
                role = ent.role,
                approved = ent.approved,
                accessToken = "local_${ent.userId}",
                provider = "local"
            )
        )
    }

    suspend fun loginLocal(email: String, password: String): Result<AuthSessionResult> {
        val cleanEmail = email.lowercase().trim()
        val ent = userAccountDao.getByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("Account not found. Create an account or use Dev Mode."))
        if (ent.passwordHash != hashPassword(cleanEmail, password)) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }
        return Result.success(
            AuthSessionResult(
                userId = ent.userId,
                name = ent.name,
                email = ent.email,
                mobile = ent.mobile,
                role = ent.role,
                approved = ent.approved,
                accessToken = "local_${ent.userId}",
                provider = "local"
            )
        )
    }
}
