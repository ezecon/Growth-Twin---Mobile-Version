package com.example.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

data class PumpSession(
    val startedAtMs: Long,
    val minutes: Int,
    val source: String, // manual | schedule
    val moistureBefore: Double
)

data class IrrigationHistoryEntry(
    val source: String,
    val actualSeconds: Int,
    val stopReason: String, // manual | target_reached | completed
    val moistureBefore: Int,
    val moistureAfter: Int,
    val startedAtMs: Long
)

data class IrrigationSample(
    val timestampMs: Long,
    val valuePct: Double,
    val pumpOn: Boolean
)

data class IrrigationSchedule(
    val enabled: Boolean = false,
    val everyMinutes: Int = 1440,
    val minutes: Int = 10,
    val onlyIfDry: Boolean = true,
    val anchorMs: Long = System.currentTimeMillis(),
    val lastSkipReasonBn: String? = null,
    val lastSkipReasonEn: String? = null
)

data class IrrigationView(
    val plotId: String,
    val mode: String, // sim | sensor
    val moisturePct: Double,
    val emergencyBelow: Int = 20,
    val irrigateBelow: Int = 35,
    val stopAt: Int = 60,
    val pump: PumpSession?,
    val maxMinutes: Int = 30,
    val dailyCapMin: Int = 90,
    val used24hMin: Double = 0.0,
    val left24hMin: Double = 90.0,
    val schedule: IrrigationSchedule = IrrigationSchedule(),
    val history: List<IrrigationHistoryEntry> = emptyList(),
    val series: List<IrrigationSample> = emptyList(),
    val mmPerMin: Double = 0.5
)

object IrrigationController {
    const val EMERGENCY_BELOW = 20
    const val IRRIGATE_BELOW = 35
    const val STOP_AT = 60
    const val MAX_MIN = 30
    const val DAILY_CAP = 90
    const val COOLDOWN_MS = 5000L
    const val MM_PER_MIN = 0.5

    private data class InternalState(
        var moisture: Double,
        var mode: String = "sim",
        var lastSensorMs: Long? = null,
        var pump: PumpSession? = null,
        val series: MutableList<IrrigationSample> = mutableListOf(),
        val history: MutableList<IrrigationHistoryEntry> = mutableListOf(),
        var lastStopMs: Long = 0L,
        var lastSampleMs: Long = 0L,
        var lastTickMs: Long = System.currentTimeMillis(),
        var schedule: IrrigationSchedule = IrrigationSchedule()
    )

    private val states = mutableMapOf<String, InternalState>()
    private val rng = Random(System.currentTimeMillis())

    private fun getOrInit(plotId: String, initialMoisture: Double? = null): InternalState {
        return states.getOrPut(plotId) {
            val init = (initialMoisture ?: (28.0 + rng.nextDouble() * 27.0)).coerceIn(0.0, 125.0)
            InternalState(moisture = init)
        }
    }

    fun seedFromPlot(plotId: String, plotMoisture: Double) {
        if (plotId !in states) {
            getOrInit(plotId, plotMoisture)
        }
    }

    fun pushSensorReading(plotId: String, pct: Double) {
        val st = getOrInit(plotId)
        st.moisture = pct.coerceIn(0.0, 125.0)
        st.mode = "sensor"
        st.lastSensorMs = System.currentTimeMillis()
    }

    private fun used24hMin(st: InternalState, now: Long): Double {
        val cutoff = now - 86_400_000L
        var secs = st.history.filter { it.startedAtMs > cutoff }.sumOf { it.actualSeconds }.toDouble()
        st.pump?.let { secs += max(0L, (now - it.startedAtMs) / 1000L) }
        return secs / 60.0
    }

    /** Returns null if pump didn't stop, or (kind, seconds) pair for twin sync. */
    fun tick(plotId: String, plotMoisture: Double? = null): Pair<String, Int>? {
        val st = getOrInit(plotId, plotMoisture)
        val now = System.currentTimeMillis()
        val dt = ((now - st.lastTickMs) / 1000.0).coerceIn(0.0, 5.0)
        st.lastTickMs = now

        if (st.mode == "sensor" && st.lastSensorMs != null && now - st.lastSensorMs!! > 300_000L) {
            st.mode = "sim"
        }
        if (st.mode == "sim") {
            val drift = if (st.pump != null) 0.5 * dt else -0.03 * dt
            val noise = (rng.nextDouble() - 0.5) * 0.3
            st.moisture = (st.moisture + drift + noise).coerceIn(0.0, 125.0)
        }

        var syncEvent: Pair<String, Int>? = null
        val p = st.pump
        if (p != null) {
            when {
                st.moisture >= STOP_AT -> syncEvent = stopInternal(st, "target_reached", now)
                now - p.startedAtMs >= p.minutes * 60_000L -> syncEvent = stopInternal(st, "completed", now)
                used24hMin(st, now) >= DAILY_CAP -> syncEvent = stopInternal(st, "completed", now)
            }
        }

        val sc = st.schedule
        if (sc.enabled && st.pump == null && now >= sc.anchorMs + sc.everyMinutes * 60_000L) {
            when {
                sc.onlyIfDry && st.moisture >= IRRIGATE_BELOW -> {
                    st.schedule = sc.copy(
                        anchorMs = now,
                        lastSkipReasonBn = "মাটি যথেষ্ট ভেজা (${st.moisture.toInt()}%), সেচ বাদ",
                        lastSkipReasonEn = "Soil wet enough (${st.moisture.toInt()}%), schedule skipped"
                    )
                }
                used24hMin(st, now) + sc.minutes > DAILY_CAP -> {
                    st.schedule = sc.copy(
                        anchorMs = now,
                        lastSkipReasonBn = "দৈনিক সীমা পূর্ণ, সেচ বাদ",
                        lastSkipReasonEn = "Daily 90 min cap reached, skipped"
                    )
                }
                else -> {
                    st.pump = PumpSession(now, sc.minutes, "schedule", st.moisture)
                    st.schedule = sc.copy(lastSkipReasonBn = null, lastSkipReasonEn = null)
                    syncEvent = "on" to 0
                }
            }
        }

        if (now - st.lastSampleMs >= 2500L) {
            st.lastSampleMs = now
            st.series.add(IrrigationSample(now, round(st.moisture * 10.0) / 10.0, st.pump != null))
            if (st.series.size > 120) st.series.removeAt(0)
        }
        return syncEvent
    }

    private fun stopInternal(st: InternalState, reason: String, now: Long): Pair<String, Int>? {
        val p = st.pump ?: return null
        val actualS = ((now - p.startedAtMs) / 1000.0).toInt().coerceAtLeast(1)
        st.history.add(
            0,
            IrrigationHistoryEntry(
                source = p.source,
                actualSeconds = actualS,
                stopReason = reason,
                moistureBefore = p.moistureBefore.toInt(),
                moistureAfter = st.moisture.toInt(),
                startedAtMs = p.startedAtMs
            )
        )
        if (st.history.size > 30) st.history.removeAt(st.history.lastIndex)
        st.pump = null
        st.lastStopMs = now
        st.schedule = st.schedule.copy(anchorMs = now)
        return "off" to actualS
    }

    fun startPump(plotId: String, minutes: Int, force: Boolean = false): Result<Unit> {
        val st = getOrInit(plotId)
        val now = System.currentTimeMillis()
        val m = minutes.coerceIn(1, MAX_MIN)
        if (st.pump != null) return Result.failure(IllegalStateException("পাম্প আগে থেকেই চালু আছে / Pump already running"))
        if (now - st.lastStopMs < COOLDOWN_MS) return Result.failure(IllegalStateException("কয়েক সেকেন্ড পরে আবার চেষ্টা করুন / Wait a few seconds (pump cooldown)"))
        if (st.moisture >= STOP_AT + 15) return Result.failure(IllegalStateException("জমি অতিরিক্ত ভেজা, সেচ দেওয়া হবে না / Plot flooded"))
        if (st.moisture >= STOP_AT && !force) return Result.failure(IllegalStateException("মাটির আর্দ্রতা এখনই ${st.moisture.toInt()}% (টার্গেটের ওপরে) — Force চালু করতে পারেন"))
        if (used24hMin(st, now) + m > DAILY_CAP) return Result.failure(IllegalStateException("আজকের সেচের সীমা (৯০ মিনিট) পূর্ণ হয়ে গেছে / Daily cap reached"))
        st.pump = PumpSession(now, m, "manual", st.moisture)
        return Result.success(Unit)
    }

    fun stopPump(plotId: String): Pair<String, Int>? {
        val st = getOrInit(plotId)
        return stopInternal(st, "manual", System.currentTimeMillis())
    }

    fun updateSchedule(plotId: String, enabled: Boolean, everyMinutes: Int, minutes: Int, onlyIfDry: Boolean): Result<Unit> {
        if (minutes >= everyMinutes) {
            return Result.failure(IllegalArgumentException("সেচের সময় ব্যবধানের চেয়ে ছোট হতে হবে / Run duration must be shorter than interval"))
        }
        val st = getOrInit(plotId)
        val now = System.currentTimeMillis()
        val newAnchor = if (enabled && !st.schedule.enabled) now else st.schedule.anchorMs
        st.schedule = st.schedule.copy(
            enabled = enabled,
            everyMinutes = everyMinutes.coerceIn(2, 10080),
            minutes = minutes.coerceIn(1, MAX_MIN),
            onlyIfDry = onlyIfDry,
            anchorMs = newAnchor
        )
        return Result.success(Unit)
    }

    fun setDemoMoisture(plotId: String, pct: Double): Result<Unit> {
        val st = getOrInit(plotId)
        if (st.mode == "sensor") return Result.failure(IllegalStateException("আসল সেন্সর চালু, সিমুলেশন বদলানো যাবে না / Real sensor active"))
        st.moisture = pct.coerceIn(0.0, 125.0)
        return Result.success(Unit)
    }

    fun view(plotId: String): IrrigationView {
        val st = getOrInit(plotId)
        val now = System.currentTimeMillis()
        val used = round(used24hMin(st, now) * 10.0) / 10.0
        return IrrigationView(
            plotId = plotId,
            mode = st.mode,
            moisturePct = round(st.moisture * 10.0) / 10.0,
            pump = st.pump,
            used24hMin = used,
            left24hMin = round(max(0.0, DAILY_CAP - used) * 10.0) / 10.0,
            schedule = st.schedule,
            history = st.history.toList(),
            series = st.series.toList()
        )
    }
}
