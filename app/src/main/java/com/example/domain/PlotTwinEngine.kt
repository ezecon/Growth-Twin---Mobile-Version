package com.example.domain

import android.graphics.Bitmap
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Random
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

data class WhyFact(val labelBn: String, val labelEn: String, val value: String)
data class WhyExplanation(val ruleBn: String, val ruleEn: String, val facts: List<WhyFact>)

data class TwinActionSpec(
    val type: String, // irrigate | pump | fertilize | lime | spray | harvest
    val nutrient: String? = null,
    val on: Boolean? = null
)

data class PlotAlert(
    val key: String,
    val level: String, // critical | action | warn | info | ok
    val titleBn: String,
    val titleEn: String,
    val textBn: String,
    val textEn: String,
    val day: Int,
    val action: TwinActionSpec? = null,
    val why: WhyExplanation? = null
)

data class TwinQuestionOption(val value: String, val labelBn: String, val labelEn: String)
data class TwinQuestionView(
    val id: String,
    val textBn: String,
    val textEn: String,
    val dayAsked: Int,
    val options: List<TwinQuestionOption>
)

data class PhotoReviewItem(
    val id: String,
    val day: Int,
    val label: String?,
    val labelBn: String?,
    val score: Double?,
    val reason: String,
    val status: String, // pending | reviewed
    val verdict: String? = null,
    val expertLabel: String? = null,
    val note: String? = null,
    val by: String? = null
)

data class PlotResult(
    val yieldKg: Int,
    val revenue: Int,
    val cost: Int,
    val profit: Int,
    val yieldFactor: Double,
    val outcome: String,
    val day: Int,
    val health: Int
)

data class ExperimentMetrics(
    val daysSimulated: Int,
    val status: String,
    val health: Double,
    val soilMoisturePct: Double,
    val waterL: Double,
    val irrigationMm: Double,
    val estimatedYieldKg: Int,
    val estimatedProfitBdt: Int,
    val cropStress: Double
)

data class ExperimentRecord(
    val id: String,
    val name: String,
    val createdAt: String,
    val plotDayStart: Int,
    val horizonDays: Int,
    val intervention: String, // irrigation | fertilizer
    val interventionBn: String,
    val interventionEn: String,
    val baseline: ExperimentMetrics,
    val scenario: ExperimentMetrics,
    val deltaHealth: Double,
    val deltaWaterL: Double,
    val deltaIrrigMm: Double,
    val deltaYieldKg: Int,
    val deltaProfitBdt: Int,
    val deltaStress: Double,
    val explanationBn: String,
    val explanationEn: String
)

data class TwinEvent(
    val day: Int,
    val kind: String,
    val textBn: String,
    val textEn: String,
    val key: String? = null,
    val why: WhyExplanation? = null
)

data class DailyLogEntry(
    val day: Int,
    val date: String,
    val stageId: String,
    val stageBn: String,
    val stageEn: String,
    val status: String,
    val aw: Double,
    val ph: Double,
    val ec: Double,
    val n: Double,
    val p: Double,
    val k: Double,
    val dis: Int,
    val health: Double,
    val pumpOn: Boolean,
    val irrigationMm: Double,
    val estYieldKg: Int,
    val tmax: Double,
    val tmin: Double,
    val rainMm: Double,
    val rh: Int
)

data class AdviceItem(
    val id: String,
    val level: String,
    val icon: String,
    val titleBn: String,
    val titleEn: String,
    val textBn: String,
    val textEn: String,
    val action: TwinActionSpec? = null,
    val btnBn: String? = null,
    val btnEn: String? = null
)

data class PlotConfig(
    val autoIrrigation: Boolean = true,
    val autoInputs: Boolean = false,
    val smsEnabled: Boolean = false,
    val phone: String = ""
)

data class PlotState(
    val id: String,
    val userId: String,
    val name: String,
    val cropId: String,
    val soil: String,
    val area: Double,
    val seed: Int,
    val startDate: String,
    var config: PlotConfig = PlotConfig(),
    var day: Int = 0,
    var status: String = "growing", // growing | ready | harvested | dead
    var endReason: String? = null,
    var result: PlotResult? = null,
    val awc: Double = 100.0,
    var wMm: Double = 80.0,
    var ph: Double = 6.3,
    var ec: Double = 1.0,
    var n: Double = 55.0,
    var p: Double = 50.0,
    var k: Double = 55.0,
    var health: Double = 100.0,
    var pumpOn: Boolean = false,
    var pumpManual: Boolean = false,
    var pumpDays: Int = 0,
    var wlDays: Int = 0,
    var darkDays: Int = 0,
    var dis: Double = 0.0,
    var sprayUntil: Int = -1,
    var protectNeeded: Boolean = false,
    var yfNum: Double = 0.0,
    var yfDen: Double = 0.0,
    var stand: Double = 1.0,
    val lastRain: MutableList<Double> = mutableListOf(),
    var totalWaterL: Double = 0.0,
    var totalIrrigMm: Double = 0.0,
    var totalUreaKg: Double = 0.0,
    var totalTspKg: Double = 0.0,
    var totalMopKg: Double = 0.0,
    var totalLimeKg: Double = 0.0,
    var totalRainMm: Double = 0.0,
    var totalSprays: Int = 0,
    val lastFertDay: MutableMap<String, Int> = mutableMapOf("N" to -99, "P" to -99, "K" to -99),
    var lastLimeDay: Int = -99,
    val alertLast: MutableMap<String, Int> = mutableMapOf(),
    val alerts: MutableList<PlotAlert> = mutableListOf(),
    val openQ: MutableMap<String, Int> = mutableMapOf(),
    val qLast: MutableMap<String, Int> = mutableMapOf(),
    val answers: MutableMap<String, String> = mutableMapOf(),
    val reviewPending: MutableList<PhotoReviewItem> = mutableListOf(),
    var lastWx: DayWeather? = null,
    val lastFactors: MutableMap<String, Double> = mutableMapOf(
        "water" to 1.0, "nutrient" to 1.0, "ph" to 1.0, "temp" to 1.0,
        "salt" to 1.0, "disease" to 1.0, "light" to 1.0
    ),
    val warningsBn: MutableList<String> = mutableListOf(),
    val warningsEn: MutableList<String> = mutableListOf(),
    val experiments: MutableList<ExperimentRecord> = mutableListOf(),
    var sensorConfig: MoistureCalibration? = null,
    var harvestSaved: Boolean = false
) {
    fun awPct(): Double = wMm / awc * 100.0
    fun currentDate(): LocalDate = LocalDate.parse(startDate).plusDays(day.toLong())

    fun deepCopy(): PlotState = PlotTwinEngine.fromJson(PlotTwinEngine.toJson(this))
}

data class AdvanceOutcome(
    val logs: List<DailyLogEntry>,
    val events: List<TwinEvent>,
    val stoppedForBn: String?,
    val stoppedForEn: String?
)

data class PhotoAnalysisResult(
    val status: String, // predicted | unknown
    val label: String?,
    val labelBn: String?,
    val score: Double,
    val healthy: Boolean,
    val needsReview: Boolean
)

object PlotTwinEngine {
    const val M2_PER_DECIMAL = 40.47
    const val DEATH_AFTER_READY = 15
    const val HARVEST_WINDOW = 10
    const val LOW_LEVEL = 38.0
    const val MANUAL_PUMP_MAX_DAYS = 2
    const val AUTO_PUMP_MAX_DAYS = 7

    private val ACTIVE = setOf("growing", "ready")
    private fun clamp(x: Double, lo: Double, hi: Double): Double = max(lo, min(hi, x))
    private fun round1(x: Double): Double = round(x * 10.0) / 10.0
    private fun round2(x: Double): Double = round(x * 100.0) / 100.0
    private fun round3(x: Double): Double = round(x * 1000.0) / 1000.0

    fun newPlot(
        userId: String,
        name: String,
        cropId: String,
        soil: String,
        area: Double,
        ph: Double? = null,
        ec: Double? = null,
        startDate: String? = null,
        seed: Int? = null,
        config: PlotConfig = PlotConfig()
    ): PlotState {
        val crop = CropCatalog.BY_ID[cropId] ?: error("Unknown crop: $cropId")
        val awc = CropCatalog.AWC_MM[soil] ?: error("Unknown soil: $soil")
        val actualSeed = seed ?: Random().nextInt(1_000_000)
        val rnd = Random("init|$actualSeed".hashCode().toLong())
        val rice = CropCatalog.templateOf(cropId) == "rice"
        val start = startDate ?: LocalDate.now().toString()
        val initPh = ph ?: 6.3
        val initEc = ec ?: 1.0

        val wBn = mutableListOf<String>()
        val wEn = mutableListOf<String>()
        val m = LocalDate.parse(start).monthValue
        if (CropCatalog.seasonOfMonth(m) !in crop.seasons) {
            val sBn = crop.seasons.joinToString(" / ") { CropCatalog.seasonName(it, AppLang.BN) }
            val sEn = crop.seasons.joinToString(" / ") { CropCatalog.seasonName(it, AppLang.EN) }
            wBn.add("${crop.nameBn} সাধারণত $sBn মৌসুমের ফসল; এই সময়ে বপন করলে ঝুঁকি বাড়বে।")
            wEn.add("${crop.nameEn} is usually a $sEn crop; planting now raises weather risk.")
        }
        if (soil !in crop.soils) {
            wBn.add("${crop.nameBn} এর জন্য এই মাটি আদর্শ নয়।")
            wEn.add("This soil type is not ideal for ${crop.nameEn}.")
        }
        if (initPh < crop.phMin || initPh > crop.phMax) {
            wBn.add("মাটির pH (${BnFormat.num(initPh, 1, AppLang.BN)}) এই ফসলের আদর্শ সীমার (${crop.phMin}–${crop.phMax}) বাইরে।")
            wEn.add("Soil pH ($initPh) is outside ideal range (${crop.phMin}–${crop.phMax}).")
        }

        return PlotState(
            id = UUID.randomUUID().toString().replace("-", "").take(12),
            userId = userId,
            name = name.ifBlank { crop.nameBn },
            cropId = cropId,
            soil = soil,
            area = area.coerceIn(0.5, 5000.0),
            seed = actualSeed,
            startDate = start,
            config = config,
            awc = awc,
            wMm = awc * (if (rice) 1.2 else 0.8),
            ph = initPh,
            ec = initEc,
            n = round1(45.0 + rnd.nextDouble() * 20.0),
            p = round1(40.0 + rnd.nextDouble() * 20.0),
            k = round1(45.0 + rnd.nextDouble() * 20.0),
            warningsBn = wBn,
            warningsEn = wEn
        )
    }

    private fun limeDosePerDecimal(s: PlotState, crop: Crop): Double {
        val target = min((crop.phMin + crop.phMax) / 2.0, crop.phMin + 0.6)
        val buf = CropCatalog.SOIL_BUFFER[s.soil] ?: 1.0
        return round1(max(0.0, (target - s.ph) * CropCatalog.LIME_KG_PER_DECIMAL_PER_PH * buf))
    }

    fun estimateYield(s: PlotState, crop: Crop): PlotResult {
        val yf = if (s.yfDen > 0.0) s.yfNum / s.yfDen else 1.0
        val overM = max(0.5, 1.0 - 0.02 * max(0, s.day - crop.durationDays))
        val kg = crop.y.second * s.area * yf * s.stand * overM
        val rev = kg * crop.p.second
        val cost = crop.cost * s.area
        return PlotResult(
            yieldKg = kg.roundToInt(),
            revenue = rev.roundToInt(),
            cost = cost.roundToInt(),
            profit = (rev - cost).roundToInt(),
            yieldFactor = round3(yf),
            outcome = s.status,
            day = s.day,
            health = s.health.roundToInt()
        )
    }

    fun applyAction(
        s: PlotState,
        type: String,
        nutrient: String? = null,
        mmOverride: Double? = null,
        kgPerDecimalOverride: Double? = null,
        pumpOnVal: Boolean = false
    ): TwinEvent {
        require(s.status in ACTIVE) { "Crop lifecycle has ended" }
        val crop = CropCatalog.BY_ID.getValue(s.cropId)
        val prof = CropCatalog.profile(crop.id, crop.waterNeed)

        return when (type) {
            "irrigate" -> {
                val need = max(0.0, (prof.target - s.awPct()) / 100.0 * s.awc)
                val mm = clamp(mmOverride ?: min(prof.pumpMm, need), 0.0, 60.0)
                s.wMm = min(s.awc + 80.0, s.wMm + mm)
                s.totalIrrigMm += mm
                s.totalWaterL += mm * M2_PER_DECIMAL * s.area
                TwinEvent(
                    s.day, "action",
                    "${BnFormat.num(mm, 0, AppLang.BN)} মিমি সেচ দেওয়া হলো।",
                    "Irrigated ${round1(mm)} mm."
                )
            }
            "pump" -> {
                s.pumpOn = pumpOnVal
                s.pumpManual = pumpOnVal
                s.pumpDays = 0
                TwinEvent(
                    s.day, "action",
                    if (pumpOnVal) "পাম্প চালু করা হলো (সর্বোচ্চ $MANUAL_PUMP_MAX_DAYS দিন)।" else "পাম্প বন্ধ করা হলো।",
                    if (pumpOnVal) "Pump turned ON (max $MANUAL_PUMP_MAX_DAYS days fail-safe)." else "Pump turned OFF."
                )
            }
            "fertilize" -> {
                val nKey = nutrient ?: error("Nutrient required")
                val spec = CropCatalog.FERT[nKey] ?: error("Unknown fertilizer")
                when (nKey) {
                    "N" -> { s.n = min(100.0, s.n + spec.gain); s.totalUreaKg += spec.kgPerDecimal * s.area; s.ph -= 0.03 }
                    "P" -> { s.p = min(100.0, s.p + spec.gain); s.totalTspKg += spec.kgPerDecimal * s.area }
                    "K" -> { s.k = min(100.0, s.k + spec.gain); s.totalMopKg += spec.kgPerDecimal * s.area }
                }
                s.lastFertDay[nKey] = s.day
                val totalKg = spec.kgPerDecimal * s.area
                TwinEvent(
                    s.day, "action",
                    "${spec.nameBn} ${BnFormat.num(totalKg, 1, AppLang.BN)} কেজি দেওয়া হলো।",
                    "Applied ${round1(totalKg)} kg ${spec.nameEn}."
                )
            }
            "lime" -> {
                val d = clamp(kgPerDecimalOverride ?: limeDosePerDecimal(s, crop), 0.0, 15.0)
                val buf = CropCatalog.SOIL_BUFFER[s.soil] ?: 1.0
                s.ph += d / (CropCatalog.LIME_KG_PER_DECIMAL_PER_PH * buf)
                s.lastLimeDay = s.day
                val totalKg = d * s.area
                s.totalLimeKg += totalKg
                TwinEvent(
                    s.day, "action",
                    "চুন ${BnFormat.num(totalKg, 1, AppLang.BN)} কেজি দেওয়া হলো।",
                    "Applied ${round1(totalKg)} kg agricultural lime."
                )
            }
            "spray" -> {
                s.dis = max(0.0, s.dis - 60.0)
                s.protectNeeded = false
                s.sprayUntil = s.day + 7
                s.totalSprays += 1
                TwinEvent(
                    s.day, "action",
                    "রোগ/পোকা দমনের স্প্রে দেওয়া হলো (৭ দিন সুরক্ষা)।",
                    "Protective spray applied (7 days coverage)."
                )
            }
            "harvest" -> {
                require(s.status == "ready") { "Crop is not ready for harvest yet" }
                finishPlot(s, "harvested", "ফসল কাটা হয়েছে")
            }
            else -> error("Unknown action: $type")
        }
    }

    private fun finishPlot(s: PlotState, outcome: String, reasonBn: String): TwinEvent {
        val crop = CropCatalog.BY_ID.getValue(s.cropId)
        val est = estimateYield(s, crop)
        val finalResult = if (outcome == "dead") {
            est.copy(yieldKg = 0, revenue = 0, profit = -est.cost, outcome = "dead")
        } else {
            est.copy(outcome = "harvested")
        }
        s.status = outcome
        s.endReason = reasonBn
        s.pumpOn = false
        s.openQ.clear()
        s.result = finalResult
        val txtBn = if (outcome == "harvested") {
            "ফসল কাটা হলো। ফলন প্রায় ${BnFormat.num(finalResult.yieldKg, AppLang.BN)} কেজি, লাভ/লোকসান ${BnFormat.num(finalResult.profit, AppLang.BN)} ৳।"
        } else {
            "গাছ মারা গেছে। কারণ: $reasonBn।"
        }
        val txtEn = if (outcome == "harvested") {
            "Harvested ~${finalResult.yieldKg} kg, est. profit ${finalResult.profit} BDT."
        } else {
            "Crop failed: $reasonBn"
        }
        return TwinEvent(s.day, "status", txtBn, txtEn)
    }

    private fun raiseAlert(
        s: PlotState,
        events: MutableList<TwinEvent>,
        key: String,
        level: String,
        titleBn: String,
        titleEn: String,
        textBn: String,
        textEn: String,
        cooldown: Int = 7,
        action: TwinActionSpec? = null,
        why: WhyExplanation? = null
    ) {
        val last = s.alertLast[key]
        if (last != null && s.day - last < cooldown) return
        s.alertLast[key] = s.day
        val alert = PlotAlert(key, level, titleBn, titleEn, textBn, textEn, s.day, action, why)
        s.alerts.add(alert)
        if (s.alerts.size > 16) s.alerts.removeAt(0)
        events.add(TwinEvent(s.day, "alert", "$titleBn — $textBn", "$titleEn — $textEn", key, why))
    }

    fun step(s: PlotState): Pair<DailyLogEntry, List<TwinEvent>> {
        val crop = CropCatalog.BY_ID.getValue(s.cropId)
        val prof = CropCatalog.profile(crop.id, crop.waterNeed)
        val tmpl = CropCatalog.templateOf(crop.id)
        val rice = tmpl == "rice"
        val events = mutableListOf<TwinEvent>()

        s.day += 1
        val d = s.day
        val today = s.currentDate()
        val wx = WeatherSimulator.dayWeather(s.seed, today)
        val fc = WeatherSimulator.forecast(s.seed, today, 3)
        val (idx, st) = CropCatalog.stageAt(crop.id, d, crop.durationDays)
        var aw = s.awPct()
        val rainSoon = fc.take(2).sumOf { it.rainMm * it.prob / 100.0 }
        val stormSoon = fc.take(2).any { it.rainMm >= 60.0 && it.prob >= 50 }
        val emergency = aw < prof.emergency
        val tb = CropCatalog.TEMP.getValue(tmpl)

        // Irrigation controller
        if (st.irrigate && s.status == "growing") {
            if (stormSoon && s.pumpOn && !emergency) {
                s.pumpOn = false
                s.pumpDays = 0
                val why = WhyExplanation(
                    "প্রবল বৃষ্টির (৬০ মিমি+) পূর্বাভাস থাকলে, জরুরি না হলে পাম্প বন্ধ থাকে",
                    "Pump pauses when heavy rain (60mm+) is forecast unless moisture is critical.",
                    listOf(
                        WhyFact("বৃষ্টির পূর্বাভাস", "Forecast rain", "${fc.take(2).maxOf { it.rainMm }} mm"),
                        WhyFact("মাটির আর্দ্রতা", "Soil moisture", "${round1(aw)}%")
                    )
                )
                events.add(TwinEvent(d, "auto_action", "প্রবল বৃষ্টির পূর্বাভাসে পাম্প বন্ধ করা হলো।", "Pump paused for incoming storm.", "pump_off", why))
            }
            if (s.config.autoIrrigation) {
                if (!s.pumpOn) {
                    if (emergency) {
                        s.pumpOn = true; s.pumpManual = false; s.pumpDays = 0
                        val why = WhyExplanation(
                            "আর্দ্রতা জরুরি সীমার নিচে নামলে বৃষ্টির পূর্বাভাস থাকলেও পাম্প চালু হয়",
                            "Moisture fell below emergency threshold; pump starts even if rain is forecast.",
                            listOf(
                                WhyFact("মাটির আর্দ্রতা", "Moisture", "${round1(aw)}%"),
                                WhyFact("জরুরি সীমা", "Emergency limit", "${round1(prof.emergency)}%"),
                                WhyFact("২ দিনে সম্ভাব্য বৃষ্টি", "Expected 2d rain", "${round1(rainSoon)} mm")
                            )
                        )
                        events.add(TwinEvent(d, "auto_action", "জরুরি: মাটির আর্দ্রতা কমেছে, পাম্প চালু হলো।", "Emergency pump ON (moisture critical).", "pump_on", why))
                        raiseAlert(
                            s, events, "emergency", "warn",
                            "জরুরি স্বয়ংক্রিয় সেচ", "Emergency Auto Irrigation",
                            "আর্দ্রতা ${BnFormat.num(aw, AppLang.BN)}% এ নেমেছে, পাম্প চালু হয়েছে।",
                            "Moisture dropped to ${round1(aw)}%; pump turned on.", 3, null, why
                        )
                    } else if (aw < prof.trigger) {
                        if (rainSoon >= 10.0 || stormSoon) {
                            raiseAlert(
                                s, events, "irrigation_skipped", "info",
                                "বৃষ্টির পূর্বাভাসে সেচ স্থগিত", "Irrigation Delayed for Rain",
                                "আগামী ২ দিনে প্রায় ${BnFormat.num(rainSoon, AppLang.BN)} মিমি বৃষ্টির সম্ভাবনা।",
                                "Skipping pump: ~${round1(rainSoon)} mm expected rain in 48h.", 3,
                                why = WhyExplanation(
                                    "আর্দ্রতা সেচের সীমার নিচে, কিন্তু ২ দিনে ১০ মিমি+ বৃষ্টির সম্ভাবনা থাকলে সেচ স্থগিত থাকে",
                                    "Moisture is below trigger, but >10mm forecast rain delays pumping to save water.",
                                    listOf(
                                        WhyFact("মাটির আর্দ্রতা", "Moisture", "${round1(aw)}%"),
                                        WhyFact("সেচের সীমা", "Trigger", "${round1(prof.trigger)}%"),
                                        WhyFact("২ দিনে বৃষ্টি", "48h Rain", "${round1(rainSoon)} mm")
                                    )
                                )
                            )
                        } else {
                            s.pumpOn = true; s.pumpManual = false; s.pumpDays = 0
                            val why = WhyExplanation(
                                "আর্দ্রতা সেচের সীমার নিচে নামলে এবং বৃষ্টির সম্ভাবনা কম থাকলে পাম্প চালু হয়",
                                "Moisture fell below trigger and rain probability is low.",
                                listOf(
                                    WhyFact("মাটির আর্দ্রতা", "Moisture", "${round1(aw)}%"),
                                    WhyFact("সেচের সীমা", "Trigger", "${round1(prof.trigger)}%")
                                )
                            )
                            events.add(TwinEvent(d, "auto_action", "আর্দ্রতা সীমার নিচে; পাম্প চালু হলো।", "Moisture below trigger; auto-pump ON.", "pump_on", why))
                        }
                    }
                }
            } else if (!s.pumpOn && aw < prof.trigger) {
                raiseAlert(
                    s, events, "irrigate", if (emergency) "critical" else "action",
                    if (emergency) "জরুরি সেচ দরকার" else "সেচ দিতে হবে",
                    if (emergency) "Emergency Irrigation Needed" else "Irrigation Recommended",
                    "মাটির আর্দ্রতা ${BnFormat.num(aw, AppLang.BN)}%।",
                    "Soil moisture is ${round1(aw)}% (auto-irrigation is OFF).",
                    3, TwinActionSpec("irrigate"),
                    WhyExplanation(
                        "আর্দ্রতা সেচের সীমার নিচে নামলে সেচ প্রস্তাব করা হয়",
                        "Soil water dropped below crop trigger level while auto-irrigation is disabled.",
                        listOf(WhyFact("আর্দ্রতা", "Moisture", "${round1(aw)}%"), WhyFact("সীমা", "Trigger", "${round1(prof.trigger)}%"))
                    )
                )
            }
        }
        if (!st.irrigate) s.pumpOn = false

        // Water balance
        val rain = wx.rainMm
        val effRain = if (rain < 40.0) rain else 40.0 + (rain - 40.0) * 0.5
        val etc = wx.et0 * st.kc
        var irr = 0.0
        if (s.pumpOn) {
            val need = max(0.0, prof.target / 100.0 * s.awc - (s.wMm + effRain - etc))
            irr = min(prof.pumpMm, need)
            s.totalIrrigMm += irr
            s.totalWaterL += irr * M2_PER_DECIMAL * s.area
        }
        s.wMm = max(0.0, s.wMm + effRain + irr - etc)
        s.wMm -= max(0.0, s.wMm - s.awc) * (if (rice) 0.08 else 0.5)
        s.wMm = min(s.wMm, s.awc + 80.0)
        aw = s.awPct()
        s.totalRainMm += rain
        s.lastRain.add(rain)
        if (s.lastRain.size > 3) s.lastRain.removeAt(0)

        if (s.pumpOn && aw >= prof.target - 0.05) {
            s.pumpOn = false; s.pumpDays = 0
            val why = WhyExplanation(
                "আর্দ্রতা লক্ষ্যমাত্রায় পৌঁছালে পাম্প বন্ধ হয় (হিস্টেরেসিস)",
                "Moisture reached target threshold — hysteresis stops the pump cleanly.",
                listOf(WhyFact("আর্দ্রতা", "Moisture", "${round1(aw)}%"), WhyFact("লক্ষ্য", "Target", "${round1(prof.target)}%"))
            )
            events.add(TwinEvent(d, "auto_action", "আর্দ্রতা লক্ষ্যমাত্রায় পৌঁছেছে, পাম্প বন্ধ হলো।", "Target moisture reached; pump OFF.", "pump_off", why))
        } else if (s.pumpOn) {
            s.pumpDays += 1
            val limit = if (s.pumpManual) MANUAL_PUMP_MAX_DAYS else AUTO_PUMP_MAX_DAYS
            if (s.pumpDays >= limit) {
                s.pumpOn = false
                val why = WhyExplanation(
                    "পাম্প সর্বোচ্চ নির্ধারিত দিনের বেশি চলতে পারে না (সুরক্ষা সীমা)",
                    "Safety cap reached — check pump hose or sensor probe.",
                    listOf(WhyFact("টানা দিন", "Days run", "$limit"), WhyFact("আর্দ্রতা", "Moisture", "${round1(aw)}%"))
                )
                s.pumpDays = 0
                events.add(TwinEvent(d, "auto_action", "নিরাপত্তা সীমায় পাম্প বন্ধ হয়েছে।", "Pump stopped at safety cap.", "pump_failsafe", why))
                raiseAlert(
                    s, events, "pump_failsafe", "warn",
                    "পাম্প নিরাপত্তায় বন্ধ হয়েছে", "Pump Safety Cutoff",
                    "সময়সীমায় পাম্প বন্ধ হয়েছে; নল ও সেন্সর পরীক্ষা করুন।",
                    "Pump stopped after $limit days without hitting target; inspect hardware.", 5, null, why
                )
            }
        } else {
            s.pumpDays = 0
        }

        // Nutrients, pH, Salinity
        if (s.status == "growing") {
            s.n -= 0.45 * st.demand
            s.p -= 0.20 * st.demand
            s.k -= 0.30 * st.demand
        }
        if (rain > 40.0) {
            s.n -= if (rice) 3.0 else 8.0
            s.k -= 3.0
        }
        s.n = clamp(s.n, 0.0, 100.0)
        s.p = clamp(s.p, 0.0, 100.0)
        s.k = clamp(s.k, 0.0, 100.0)
        if (rain > 20.0) s.ph -= 0.01
        s.ec = max(0.3, s.ec + irr * 0.002 + (if (rain == 0.0) 0.005 else 0.0) - rain * 0.006 - max(0.0, s.ec - 0.8) * 0.012)

        // Disease risk
        if (wx.rh >= 85 && wx.tmean in 20.0..32.0 && d > s.sprayUntil) {
            s.dis = min(100.0, s.dis + 12.0)
        } else {
            s.dis = max(0.0, s.dis - (if (d <= s.sprayUntil) 10.0 else 6.0))
        }

        // Stress factors
        val sens = st.sens
        var wf = clamp((aw - prof.wilt) / (prof.stress - prof.wilt), 0.1, 1.0)
        if (rice) {
            s.wlDays = 0
        } else {
            s.wlDays = if (aw > 115.0) s.wlDays + 1 else max(0, s.wlDays - 1)
            wf *= max(0.5, 1.0 - 0.12 * max(0, s.wlDays - 1))
        }
        val fWater = clamp(1.0 - (1.0 - wf) * min(sens, 1.5), 0.1, 1.0)
        fun lv(x: Double) = if (x >= 30.0) 1.0 else 0.4 + 0.6 * x / 30.0
        val fNut = if (idx >= 1) min(lv(s.n), min(lv(s.p), lv(s.k))) else 1.0
        val fPh = when {
            s.ph < crop.phMin -> max(0.4, 1.0 - (crop.phMin - s.ph) * 0.25)
            s.ph > crop.phMax -> max(0.4, 1.0 - (s.ph - crop.phMax) * 0.25)
            else -> 1.0
        }
        val t = wx.tmean
        val tf = when {
            t in tb.optMin..tb.optMax -> 1.0
            t < tb.optMin -> 1.0 - 0.6 * min(1.0, (tb.optMin - t) / (tb.optMin - tb.critMin))
            else -> 1.0 - 0.6 * min(1.0, (t - tb.optMax) / (tb.critMax - tb.optMax))
        }
        val fTemp = clamp(1.0 - (1.0 - tf) * min(sens, 2.0), 0.2, 1.0)
        val tol = CropCatalog.SALT_TOL[crop.id] ?: 3.0
        val fSalt = if (s.ec <= tol) 1.0 else max(0.4, 1.0 - 0.12 * (s.ec - tol))
        var fDis = if (s.dis < 60.0) 1.0 else max(0.6, 1.0 - (s.dis - 60.0) / 100.0)
        if (s.protectNeeded) fDis *= 0.88
        val needSun = CropCatalog.LIGHT_NEED[tmpl] ?: 6.0
        val lf = if (wx.sunH >= needSun) 1.0 else 0.75 + 0.25 * wx.sunH / needSun
        val fLight = clamp(1.0 - (1.0 - lf) * min(sens, 1.5), 0.5, 1.0)
        s.darkDays = if (wx.sunH < needSun * 0.5) s.darkDays + 1 else 0

        s.lastFactors["water"] = round3(fWater)
        s.lastFactors["nutrient"] = round3(fNut)
        s.lastFactors["ph"] = round3(fPh)
        s.lastFactors["temp"] = round3(fTemp)
        s.lastFactors["salt"] = round3(fSalt)
        s.lastFactors["disease"] = round3(fDis)
        s.lastFactors["light"] = round3(fLight)

        val sAll = fWater * fNut * fPh * fTemp * fSalt * fDis * fLight
        if (sAll >= 0.9) s.health = min(100.0, s.health + 1.0) else s.health -= (0.9 - sAll) * 12.0
        if (s.status == "growing") {
            s.yfNum += sens * sAll
            s.yfDen += sens
        }

        // Weather & Agronomy Alerts
        if (stormSoon) {
            raiseAlert(s, events, "storm", "warn", "প্রবল বৃষ্টির পূর্বাভাস", "Heavy Storm Forecast",
                "সার ও সেচ স্থগিত রাখুন, জমির নিকাশ ঠিক রাখুন।", "Hold fertilizer/irrigation; clear field drainage.", 3)
        }
        val heatHot = fc.take(2).maxOf { it.tmax }
        if (heatHot >= tb.optMax + 5.0) {
            raiseAlert(s, events, "heat_soon", "info", "গরমের পূর্বাভাস", "Heat Stress Outlook",
                "আগামী ২ দিনে তাপ ${BnFormat.num(heatHot, AppLang.BN)}°সে হতে পারে।", "Forecast highs up to ${round1(heatHot)}°C; water morning/evening.", 4)
        }
        if (s.darkDays >= 4) {
            raiseAlert(s, events, "lowlight", "info", "টানা মেঘলা দিন", "Extended Low Sunlight",
                "কয়েক দিন ধরে রোদ কম; সার ও সেচ কমান।", "Four cloudy days in a row; reduce nitrogen & watch disease.", 10)
        }

        if (s.status == "growing" && st.irrigate) {
            val nLevels = mapOf("N" to s.n, "P" to s.p, "K" to s.k)
            for ((nKey, spec) in CropCatalog.FERT) {
                val curVal = nLevels.getValue(nKey)
                val lastDay = s.lastFertDay[nKey] ?: -99
                if (idx >= 1 && st.demand >= 0.5 && curVal < LOW_LEVEL && d - lastDay >= 10) {
                    if (rainSoon >= 15.0) {
                        raiseAlert(s, events, "fert_wait", "info", "বৃষ্টির আগে সার নয়", "Hold Fertilizer Before Rain",
                            "প্রবল বৃষ্টিতে সার ধুয়ে যাবে; বৃষ্টির পর দিন।", "Heavy rain expected; wait before fertilizing.", 4)
                    } else if (s.config.autoInputs) {
                        val ev = applyAction(s, "fertilize", nutrient = nKey)
                        events.add(ev.copy(kind = "auto_action", key = "auto_fert_$nKey"))
                    } else {
                        raiseAlert(
                            s, events, "fert_$nKey", "action",
                            "${spec.nameBn} সার দিতে হবে", "Apply ${spec.nameEn}",
                            "$nKey কমে ${BnFormat.num(curVal, AppLang.BN)}% হয়েছে। প্রস্তাবিত ${BnFormat.num(spec.kgPerDecimal * s.area, 1, AppLang.BN)} কেজি।",
                            "$nKey dropped to ${round1(curVal)}%. Recommended: ${round1(spec.kgPerDecimal * s.area)} kg.",
                            5, TwinActionSpec("fertilize", nutrient = nKey)
                        )
                    }
                }
            }
            val totalStages = CropCatalog.stagesOf(crop.id).size
            if (s.ph < crop.phMin - 0.15 && d - s.lastLimeDay >= 20 && idx < totalStages - 1) {
                val dose = limeDosePerDecimal(s, crop)
                if (s.config.autoInputs) {
                    val ev = applyAction(s, "lime")
                    events.add(ev.copy(kind = "auto_action", key = "auto_lime"))
                } else {
                    raiseAlert(
                        s, events, "lime", "action",
                        "চুন দিতে হবে", "Apply Agricultural Lime",
                        "মাটির pH ${BnFormat.num(s.ph, 1, AppLang.BN)}, ন্যূনতম ${crop.phMin} দরকার।",
                        "Soil pH ${round1(s.ph)} is below ${crop.phMin}. Add ${round1(dose * s.area)} kg lime.",
                        10, TwinActionSpec("lime")
                    )
                }
            } else if (s.ph > crop.phMax + 0.5) {
                raiseAlert(s, events, "alkaline", "info", "মাটি ক্ষারীয়", "Alkaline Soil",
                    "pH ${BnFormat.num(s.ph, 1, AppLang.BN)} বেশি; জৈব সার ব্যবহার করুন।", "Soil pH ${round1(s.ph)} is high; add organic matter.", 30)
            }
            if ((s.dis >= 60.0 || s.protectNeeded) && d > s.sprayUntil) {
                if (s.config.autoInputs) {
                    val ev = applyAction(s, "spray")
                    events.add(ev.copy(kind = "auto_action", key = "auto_spray"))
                } else {
                    raiseAlert(
                        s, events, "spray", "action",
                        "রোগ/পোকা দমন দরকার", "Spray Protection Needed",
                        "আর্দ্র ও গরম আবহাওয়ায় রোগের ঝুঁকি বেড়েছে।",
                        "Disease risk ${s.dis.roundToInt()}%; consult local extension agent and spray.",
                        7, TwinActionSpec("spray")
                    )
                }
            }
        }

        if (fTemp < 0.75) {
            raiseAlert(s, events, "temp", "warn", "তাপমাত্রার চাপ", "Temperature Stress",
                "দৈনিক গড় তাপমাত্রা ${BnFormat.num(t, 1, AppLang.BN)}°সে।", "Mean temperature ${round1(t)}°C is outside ideal band.", 5)
        }
        if (s.ec > tol) {
            raiseAlert(s, events, "salt", "warn", "মাটিতে লবণাক্ততা বেশি", "High Soil Salinity",
                "EC ${BnFormat.num(s.ec, 1, AppLang.BN)} dS/m (সীমা $tol); মিঠা পানিতে সেচ দিন।",
                "Salinity EC ${round1(s.ec)} exceeds tolerance ($tol dS/m).", 10)
        }
        if (s.wlDays >= 2) {
            raiseAlert(s, events, "waterlog", "warn", "জলাবদ্ধতা", "Waterlogging Alert",
                "জমিতে পানি জমে আছে। নালা কেটে নিকাশ করুন।", "Excess standing water for ${s.wlDays} days; drain plot.", 5)
        }

        // Open check-in questions (max 3 open)
        expireAndRaiseQuestions(s, idx, aw, wx, rice, events)

        s.lastWx = wx
        if (s.health <= 0.0) {
            val worst = s.lastFactors.minByOrNull { it.value }?.key ?: "water"
            val causeBn = when (worst) {
                "water" -> if (aw > 100.0) "জলাবদ্ধতা" else "পানির অভাব (খরা)"
                "nutrient" -> "পুষ্টির অভাব"
                "ph" -> "মাটির pH অনুপযোগী"
                "temp" -> "চরম তাপমাত্রা"
                "salt" -> "লবণাক্ততা"
                "disease" -> "রোগ/পোকার আক্রমণ"
                else -> "আলোর অভাব"
            }
            events.add(finishPlot(s, "dead", causeBn))
        } else if (s.status == "growing" && d >= crop.durationDays) {
            s.status = "ready"
            s.pumpOn = false
            raiseAlert(
                s, events, "harvest", "action",
                "ফসল কাটার সময় হয়েছে", "Crop Ready for Harvest",
                "ফসল পরিপক্ব। ${BnFormat.num(HARVEST_WINDOW, AppLang.BN)} দিনের মধ্যে কাটুন।",
                "Crop matured! Harvest within $HARVEST_WINDOW days to avoid yield loss.",
                99, TwinActionSpec("harvest")
            )
        } else if (s.status == "ready" && d >= crop.durationDays + DEATH_AFTER_READY) {
            events.add(finishPlot(s, "dead", "কর্তনের সময় পার হয়ে গিয়ে স্বাভাবিক মৃত্যু"))
        }

        val estYield = if (s.status != "dead") estimateYield(s, crop).yieldKg else 0
        val log = DailyLogEntry(
            day = d, date = today.toString(), stageId = st.id, stageBn = st.nameBn, stageEn = st.nameEn,
            status = s.status, aw = round1(aw), ph = round2(s.ph), ec = round2(s.ec),
            n = round1(s.n), p = round1(s.p), k = round1(s.k), dis = s.dis.roundToInt(),
            health = round1(s.health), pumpOn = s.pumpOn, irrigationMm = round1(irr),
            estYieldKg = estYield, tmax = wx.tmax, tmin = wx.tmin, rainMm = wx.rainMm, rh = wx.rh
        )
        return log to events
    }

    fun advance(s: PlotState, days: Int, stopOnAlert: Boolean = true, autoHarvest: Boolean = false): AdvanceOutcome {
        val logs = mutableListOf<DailyLogEntry>()
        val events = mutableListOf<TwinEvent>()
        var stopBn: String? = null
        var stopEn: String? = null
        for (i in 0 until days) {
            if (s.status !in ACTIVE) break
            if (s.status == "ready" && autoHarvest) {
                events.add(applyAction(s, "harvest"))
                break
            }
            val (log, ev) = step(s)
            logs.add(log)
            events.addAll(ev)
            if (s.status == "ready" && autoHarvest) continue
            if (stopOnAlert) {
                val urgent = s.alerts.lastOrNull { it.day == s.day && it.level in setOf("critical", "action") }
                if (urgent != null) {
                    stopBn = urgent.titleBn
                    stopEn = urgent.titleEn
                    break
                }
            }
        }
        if (s.status == "ready" && autoHarvest) {
            events.add(applyAction(s, "harvest"))
        }
        return AdvanceOutcome(logs, events, stopBn, stopEn)
    }

    // ---- Questions (3-tap daily check-in) ----
    private data class QDef(
        val id: String,
        val textBn: String,
        val textEn: String,
        val cooldown: Int,
        val options: List<TwinQuestionOption>,
        val condition: (PlotState, Int, Double, DayWeather, Boolean) -> Boolean,
        val apply: (PlotState, String, Double) -> Pair<String, String>
    )

    private val NO_FEW_MANY = listOf(
        TwinQuestionOption("none", "না", "None"),
        TwinQuestionOption("few", "অল্প", "A few"),
        TwinQuestionOption("many", "অনেক", "Many")
    )

    private val Q_DEFS = listOf(
        QDef(
            "leaf_spots", "পাতায় দাগ বা ছত্রাক দেখা যাচ্ছে?", "Any spots or fungus visible on leaves?", 10, NO_FEW_MANY,
            { s, _, _, _, _ -> s.dis >= 35.0 },
            { s, v, _ ->
                when (v) {
                    "many" -> { s.dis = max(s.dis, 70.0); s.protectNeeded = true; "রোগ নিশ্চিত। স্প্রে করার পরামর্শ দিন।" to "Disease confirmed; spray protection recommended." }
                    "few" -> { s.dis = max(s.dis, 45.0); "অল্প দাগ, নজরে রাখুন।" to "Mild spots noted; keep monitoring." }
                    else -> { s.dis = max(0.0, s.dis - 25.0); "দাগ নেই, রোগের ঝুঁকি কমালাম।" to "No spots; lowered disease risk estimate." }
                }
            }
        ),
        QDef(
            "wilting", "দুপুরে গাছের পাতা কি ঝিমিয়ে পড়ছে?", "Are leaves wilting around midday?", 10,
            listOf(
                TwinQuestionOption("no", "না", "No"),
                TwinQuestionOption("noon", "দুপুরে ঝিমায়, সকালে ঠিক", "Midday only, recovers by morning"),
                TwinQuestionOption("allday", "সারাদিন ঝিমিয়ে থাকে", "Wilted all day")
            ),
            { s, _, aw, wx, _ -> s.day >= 5 && (aw < 55.0 || wx.tmax >= 34.0) },
            { s, v, aw ->
                when {
                    v == "allday" && aw >= 60.0 -> { s.ec = round2(s.ec + 0.5); "মাটিতে পানি থাকা সত্ত্বেও ঝিমালে লবণাক্ততা বা শিকড়ের সমস্যা হতে পারে।" to "Wilting despite moisture suggests salinity/root stress." }
                    v == "allday" -> "পানির অভাব নিশ্চিত হলো; সেচ জরুরি।" to "Water stress confirmed; irrigation urgent."
                    v == "noon" -> "দুপুরে সামান্য ঝিমানো গরমের কারণে স্বাভাবিক।" to "Midday heat wilt is normal if leaves recover."
                    else -> "ঠিক আছে।" to "Look good."
                }
            }
        ),
        QDef(
            "standing_water", "জমিতে কি পানি জমে আছে?", "Is standing water trapped in the plot?", 7,
            listOf(TwinQuestionOption("no", "না", "No"), TwinQuestionOption("yes", "হ্যাঁ", "Yes")),
            { s, _, aw, _, rice -> !rice && (s.lastRain.takeLast(2).sum() >= 40.0 || aw > 110.0) },
            { s, v, aw ->
                if (v == "yes") {
                    s.wlDays += 1
                    if (aw < 100.0) s.wMm = s.awc * 1.15
                    "জলাবদ্ধতা নিশ্চিত; নালা কেটে পানি বের করুন।" to "Waterlogging confirmed; open drainage channels."
                } else "জল জমেনি, ঠিক আছে।" to "Draining well."
            }
        ),
        QDef(
            "germination", "প্রায় কত শতাংশ বীজ/চারা গজিয়েছে?", "Roughly what % of seeds/seedlings germinated?", 10_000,
            listOf(
                TwinQuestionOption("high", "৮০% এর বেশি", ">80%"),
                TwinQuestionOption("mid", "৫০–৮০%", "50–80%"),
                TwinQuestionOption("low", "৫০% এর কম", "<50%")
            ),
            { s, _, _, _, _ -> s.day in 6..12 },
            { s, v, _ ->
                s.stand = mapOf("high" to 1.0, "mid" to 0.85, "low" to 0.6)[v] ?: 1.0
                when (v) {
                    "high" -> "চারা সংখ্যা ভালো, চালিয়ে যান।" to "Good plant stand."
                    "mid" -> "চারা কিছুটা কম, ফাঁকা জায়গায় গ্যাপ ফিলিং করা যায়।" to "Moderate stand; consider gap-filling."
                    else -> "চারা অনেক কম; পুনরায় বপন বিবেচনা করুন।" to "Poor stand (<50%); consider re-seeding gaps."
                }
            }
        ),
        QDef(
            "leaf_color", "পাতার রং কেমন?", "What does the leaf color look like?", 14,
            listOf(
                TwinQuestionOption("green", "গাঢ় সবুজ", "Deep green"),
                TwinQuestionOption("pale", "ফ্যাকাশে/হালকা সবুজ", "Pale / light green"),
                TwinQuestionOption("lower_yellow", "নিচের পাতা হলুদ", "Lower leaves yellow"),
                TwinQuestionOption("purple", "বেগুনি/লালচে", "Purplish / reddish")
            ),
            { s, idx, _, _, _ -> idx >= 1 && s.day >= 12 && (s.n < 50.0 || s.p < 40.0) },
            { s, v, _ ->
                when (v) {
                    "pale" -> { s.n = max(0.0, s.n - 8.0); "ফ্যাকাশে পাতা নাইট্রোজেন কমের লক্ষণ।" to "Pale color indicates low nitrogen; N estimate adjusted." }
                    "lower_yellow" -> { s.n = max(0.0, s.n - 12.0); "নিচের পাতা হলুদ হলে নাইট্রোজেনের ঘাটতি প্রায় নিশ্চিত।" to "Yellow lower leaves suggest N deficiency." }
                    "purple" -> { s.p = max(0.0, s.p - 12.0); "বেগুনি পাতা ফসফরাসের অভাবের লক্ষণ।" to "Purpling suggests phosphorus deficiency." }
                    else -> { if (s.n < 35.0) s.n = min(100.0, s.n + 8.0); "পাতা সবুজ, ঠিক আছে।" to "Healthy green foliage." }
                }
            }
        ),
        QDef(
            "pests", "পোকা বা পোকার ক্ষতির চিহ্ন আছে?", "Any insects or pest feeding damage?", 14, NO_FEW_MANY,
            { s, idx, _, _, _ -> idx >= 1 && idx < CropCatalog.stagesOf(s.cropId).size - 1 && s.day >= 15 },
            { s, v, _ ->
                when (v) {
                    "many" -> { s.protectNeeded = true; "পোকার আক্রমণ বেশি, দ্রুত ব্যবস্থা নিন।" to "Heavy pest pressure; spray protection advised." }
                    "few" -> "অল্প পোকা; ২-৩ দিন পর আবার দেখুন।" to "Minor insects; monitor over next 2–3 days."
                    else -> "পোকার সমস্যা নেই।" to "No pest damage."
                }
            }
        )
    )

    private fun expireAndRaiseQuestions(s: PlotState, idx: Int, aw: Double, wx: DayWeather, rice: Boolean, events: MutableList<TwinEvent>) {
        val expired = s.openQ.filterValues { s.day - it > 10 }.keys.toList()
        expired.forEach { s.openQ.remove(it) }
        for (q in Q_DEFS) {
            if (s.openQ.size >= 3) break
            if (q.id in s.openQ) continue
            val last = s.qLast[q.id]
            if (last != null && s.day - last < q.cooldown) continue
            if (q.condition(s, idx, aw, wx, rice)) {
                s.openQ[q.id] = s.day
                s.qLast[q.id] = s.day
                events.add(TwinEvent(s.day, "question", q.textBn, q.textEn))
            }
        }
    }

    fun openQuestionViews(s: PlotState): List<TwinQuestionView> {
        val byId = Q_DEFS.associateBy { it.id }
        return s.openQ.entries.mapNotNull { (id, dayAsked) ->
            val q = byId[id] ?: return@mapNotNull null
            TwinQuestionView(q.id, q.textBn, q.textEn, dayAsked, q.options)
        }
    }

    fun answerQuestions(s: PlotState, answers: Map<String, String>): List<TwinEvent> {
        val byId = Q_DEFS.associateBy { it.id }
        val aw = s.awPct()
        val evs = mutableListOf<TwinEvent>()
        answers.forEach { (qid, valStr) ->
            val q = byId[qid] ?: return@forEach
            if (qid !in s.openQ) return@forEach
            val (bnNote, enNote) = q.apply(s, valStr, aw)
            s.answers[qid] = "$valStr@${s.day}"
            s.openQ.remove(qid)
            evs.add(TwinEvent(s.day, "answer", bnNote, enNote))
        }
        return evs
    }

    fun applyReading(s: PlotState, ph: Double?, ec: Double?, moisturePct: Double?, source: String = "manual"): TwinEvent {
        val partsBn = mutableListOf<String>()
        val partsEn = mutableListOf<String>()
        if (ph != null) {
            s.ph = round2(clamp(ph, 3.5, 9.5))
            partsBn.add("pH ${BnFormat.num(s.ph, 1, AppLang.BN)}")
            partsEn.add("pH ${s.ph}")
        }
        if (ec != null) {
            s.ec = round2(clamp(ec, 0.0, 20.0))
            partsBn.add("লবণাক্ততা ${BnFormat.num(s.ec, 1, AppLang.BN)} dS/m")
            partsEn.add("EC ${s.ec} dS/m")
        }
        if (moisturePct != null) {
            s.wMm = clamp(moisturePct, 0.0, 160.0) / 100.0 * s.awc
            val aw = round1(s.awPct())
            partsBn.add("মাটির আর্দ্রতা ${BnFormat.num(aw, AppLang.BN)}%")
            partsEn.add("Moisture $aw%")
        }
        return TwinEvent(
            s.day, "reading",
            "মাপা মান ($source) নেওয়া হয়েছে: ${partsBn.joinToString(", ")}।",
            "Recorded ($source) reading: ${partsEn.joinToString(", ")}."
        )
    }

    fun inspectLeafBitmap(bitmap: Bitmap?, cropId: String): PhotoAnalysisResult {
        if (bitmap == null) {
            return PhotoAnalysisResult("unknown", null, null, 0.42, healthy = false, needsReview = true)
        }
        // Lightweight on-device pixel color ratio heuristic (advisory triage only)
        val w = bitmap.width
        val h = bitmap.height
        val stepX = max(1, w / 32)
        val stepY = max(1, h / 32)
        var greenPx = 0
        var yellowBrownPx = 0
        var total = 0
        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                val px = bitmap.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                total++
                if (g > r + 15 && g > b + 15) greenPx++
                else if (r > 120 && g in 70..185 && b < 95) yellowBrownPx++
            }
        }
        val greenRatio = if (total > 0) greenPx.toDouble() / total else 0.0
        val spotRatio = if (total > 0) yellowBrownPx.toDouble() / total else 0.0
        return when {
            greenRatio < 0.12 && spotRatio < 0.08 -> PhotoAnalysisResult(
                "unknown", null, null, 0.45, healthy = false, needsReview = true
            )
            spotRatio >= 0.18 -> PhotoAnalysisResult(
                "predicted",
                if (cropId in listOf("potato", "tomato")) "Early / Late Blight Suspect" else "Leaf Spot / Blight Suspect",
                if (cropId in listOf("potato", "tomato")) "পাতায় দাগ / ধসা রোগের লক্ষণ" else "পাতায় দাগ / ছত্রাকের লক্ষণ",
                round2((0.68 + min(0.24, spotRatio)).coerceAtMost(0.92)),
                healthy = false,
                needsReview = spotRatio < 0.30
            )
            else -> PhotoAnalysisResult(
                "predicted", "Healthy Foliage", "সুস্থ পাতা",
                round2((0.76 + min(0.18, greenRatio)).coerceAtMost(0.94)),
                healthy = true,
                needsReview = false
            )
        }
    }

    fun recordPhoto(s: PlotState, pred: PhotoAnalysisResult): List<TwinEvent> {
        val events = mutableListOf<TwinEvent>()
        val item = PhotoReviewItem(
            id = UUID.randomUUID().toString().take(8),
            day = s.day,
            label = pred.label,
            labelBn = pred.labelBn,
            score = pred.score,
            reason = if (pred.status == "unknown") "unknown" else "low_confidence",
            status = "pending"
        )
        if (pred.status == "unknown") {
            s.reviewPending.add(item)
            if (s.reviewPending.size > 20) s.reviewPending.removeAt(0)
            events.add(TwinEvent(s.day, "photo", "ছবি থেকে নিশ্চিত হওয়া গেল না; বিশেষজ্ঞের রিভিউয়ে পাঠানো হয়েছে।", "Photo inconclusive; queued for expert review."))
            if ("leaf_spots" !in s.openQ) s.openQ["leaf_spots"] = s.day
            return events
        }
        val pct = (pred.score * 100).roundToInt()
        if (pred.healthy) {
            events.add(TwinEvent(s.day, "photo", "ছবিতে গাছ সুস্থ মনে হচ্ছে (${BnFormat.num(pct, AppLang.BN)}% নিশ্চয়তা)।", "Leaf looks healthy ($pct% confidence). Advisory only."))
            return events
        }
        if (pred.needsReview) {
            s.reviewPending.add(item)
            if (s.reviewPending.size > 20) s.reviewPending.removeAt(0)
        }
        val lblBn = pred.labelBn ?: pred.label ?: "সমস্যা"
        val lblEn = pred.label ?: "Suspected disease"
        events.add(TwinEvent(s.day, "photo", "ছবি অনুযায়ী সম্ভাব্য সমস্যা: $lblBn ($pct%)। এটি শুধু পরামর্শ।", "Advisory detection: $lblEn ($pct%). Confirm with check-in or expert."))
        raiseAlert(
            s, events, "photo_disease", "warn",
            "ছবিতে সম্ভাব্য রোগ: $lblBn", "Photo Advisory: $lblEn",
            "মডেলের নিশ্চয়তা ${BnFormat.num(pct, AppLang.BN)}%। এটি পাম্প বা স্প্রে স্বয়ংক্রিয়ভাবে চালায় না।",
            "Advisory score $pct%. Photo never triggers pump or spray automatically.", 3
        )
        if ("leaf_spots" !in s.openQ) s.openQ["leaf_spots"] = s.day
        return events
    }

    fun resolvePhotoReview(s: PlotState, reviewId: String, verdict: String, label: String, note: String, by: String): TwinEvent {
        val idx = s.reviewPending.indexOfFirst { it.id == reviewId && it.status == "pending" }
        require(idx >= 0) { "Review item not found" }
        val old = s.reviewPending[idx]
        s.reviewPending[idx] = old.copy(status = "reviewed", verdict = verdict, expertLabel = label, note = note, by = by)
        when (verdict) {
            "disease" -> s.protectNeeded = true
            "healthy" -> { s.dis = max(0.0, s.dis - 25.0); s.protectNeeded = false }
        }
        val (bn, en) = when (verdict) {
            "disease" -> "বিশেষজ্ঞ নিশ্চিত করেছেন: ${label.ifBlank { "রোগ আছে" }}।" to "Expert confirmed: ${label.ifBlank { "disease present" }}."
            "healthy" -> "বিশেষজ্ঞের মতে গাছ সুস্থ।" to "Expert verified plant is healthy."
            else -> "বিশেষজ্ঞ নিশ্চিত হতে পারেননি; জমি সরেজমিনে দেখা দরকার।" to "Expert marked unsure; inspect plot in person."
        }
        return TwinEvent(s.day, "review", bn, en)
    }

    fun runExperiment(
        live: PlotState,
        name: String,
        horizonDays: Int,
        intervention: String, // irrigation | fertilizer
        irrigationAuto: Boolean?,
        nutrient: String?
    ): ExperimentRecord {
        require(live.status in ACTIVE) { "Plot lifecycle has ended" }
        val baseline = live.deepCopy()
        val variant = live.deepCopy()
        val (intBn, intEn) = if (intervention == "irrigation") {
            val auto = irrigationAuto ?: true
            variant.config = variant.config.copy(autoIrrigation = auto)
            ("অটো সেচ " + if (auto) "চালু" else "বন্ধ") to ("Auto-Irrigation " + if (auto) "ON" else "OFF")
        } else {
            val nKey = nutrient ?: "N"
            applyAction(variant, "fertilize", nutrient = nKey)
            val spec = CropCatalog.FERT.getValue(nKey)
            "শুরুতে ${spec.nameBn} প্রয়োগ" to "Initial ${spec.nameEn} dose"
        }

        val baseRun = advance(baseline, horizonDays, stopOnAlert = false, autoHarvest = false)
        val varRun = advance(variant, horizonDays, stopOnAlert = false, autoHarvest = false)

        fun toMetrics(st: PlotState, daysSim: Int): ExperimentMetrics {
            val crop = CropCatalog.BY_ID.getValue(st.cropId)
            val est = if (st.status != "dead") estimateYield(st, crop) else PlotResult(0, 0, (crop.cost * st.area).roundToInt(), -(crop.cost * st.area).roundToInt(), 0.0, "dead", st.day, 0)
            return ExperimentMetrics(
                daysSimulated = daysSim,
                status = st.status,
                health = round1(st.health),
                soilMoisturePct = round1(st.awPct()),
                waterL = round1(st.totalWaterL),
                irrigationMm = round1(st.totalIrrigMm),
                estimatedYieldKg = est.yieldKg,
                estimatedProfitBdt = est.profit,
                cropStress = round1(max(0.0, 100.0 - st.health))
            )
        }

        val before = toMetrics(baseline, baseRun.logs.size)
        val after = toMetrics(variant, varRun.logs.size)
        val rec = ExperimentRecord(
            id = UUID.randomUUID().toString().replace("-", "").take(12),
            name = name.ifBlank { intBn },
            createdAt = LocalDate.now().toString(),
            plotDayStart = live.day,
            horizonDays = horizonDays,
            intervention = intervention,
            interventionBn = intBn,
            interventionEn = intEn,
            baseline = before,
            scenario = after,
            deltaHealth = round1(after.health - before.health),
            deltaWaterL = round1(after.waterL - before.waterL),
            deltaIrrigMm = round1(after.irrigationMm - before.irrigationMm),
            deltaYieldKg = after.estimatedYieldKg - before.estimatedYieldKg,
            deltaProfitBdt = after.estimatedProfitBdt - before.estimatedProfitBdt,
            deltaStress = round1(after.cropStress - before.cropStress),
            explanationBn = "এটি একই প্রাথমিক অবস্থা ও একই সিমুলেটেড আবহাওয়া দিয়ে করা তুলনামূলক পরীক্ষা। বাস্তব ফলনের নিশ্চয়তা নয়।",
            explanationEn = "Counterfactual simulation with identical start state and weather seed. Live plot was not modified."
        )
        live.experiments.add(rec)
        if (live.experiments.size > 20) live.experiments.removeAt(0)
        return rec
    }

    fun buildAdvice(s: PlotState): List<AdviceItem> {
        if (s.status !in ACTIVE) return emptyList()
        val crop = CropCatalog.BY_ID.getValue(s.cropId)
        val prof = CropCatalog.profile(crop.id, crop.waterNeed)
        val (_, st) = CropCatalog.stageAt(crop.id, max(s.day, 0), crop.durationDays)
        val fc = WeatherSimulator.forecast(s.seed, s.currentDate().plusDays(1), 7)
        val rainSoon = fc.take(2).sumOf { it.rainMm * it.prob / 100.0 }
        val heavy = fc.take(3).firstOrNull { it.rainMm >= 40.0 && it.prob >= 50 }
        val ripening = st.id == "ripen"
        val isRice = CropCatalog.templateOf(crop.id) == "rice"
        val aw = s.awPct()
        val out = mutableListOf<AdviceItem>()

        if (s.status == "ready") {
            out.add(AdviceItem("harvest", "action", "🌾", "ফসল কাটার সময় হয়েছে", "Crop Ready for Harvest",
                "ফসল পরিপক্ব। আবহাওয়া ভালো থাকতে থাকতে কেটে নিন।", "Crop is mature; harvest before weather deteriorates.",
                TwinActionSpec("harvest"), "কর্তন করুন", "Harvest Now"))
        }

        if (s.pumpOn) {
            if (aw >= prof.target) {
                out.add(AdviceItem("pump_off", "action", "🛑", "পাম্প বন্ধ করুন", "Stop Pump",
                    "মাটিতে পানি যথেষ্ট (${aw.roundToInt()}%)। আর চালালে অপচয় হবে।", "Moisture reached target (${aw.roundToInt()}%).",
                    TwinActionSpec("pump", on = false), "পাম্প বন্ধ করুন", "Stop Pump"))
            } else {
                out.add(AdviceItem("pump_run", "info", "💧", "পাম্প চলছে", "Pump Running",
                    "মাটির পানি এখন ${aw.roundToInt()}%, লক্ষ্য ${prof.target.roundToInt()}%।",
                    "Current moisture ${aw.roundToInt()}%, target ${prof.target.roundToInt()}%."))
            }
        } else if (aw < prof.emergency) {
            out.add(AdviceItem("irrigate", "critical", "🚨", "এখনই সেচ দিন", "Irrigate Immediately",
                "মাটির পানি মাত্র ${aw.roundToInt()}% (জরুরি সীমা ${prof.emergency.roundToInt()}%)।",
                "Moisture critical at ${aw.roundToInt()}% (limit ${prof.emergency.roundToInt()}%).",
                TwinActionSpec("irrigate"), "এখনই সেচ দিন", "Irrigate Now"))
        } else if (aw < prof.trigger && !ripening) {
            if (rainSoon >= 10.0) {
                out.add(AdviceItem("irrigate_wait", "info", "⛅", "সেচ একটু অপেক্ষা করুন", "Hold Irrigation for Rain",
                    "পানি কমে ${aw.roundToInt()}%, তবে ২ দিনে ~${rainSoon.roundToInt()} মিমি বৃষ্টির সম্ভাবনা।",
                    "Moisture ${aw.roundToInt()}%, but ~${rainSoon.roundToInt()} mm rain expected in 48h."))
            } else {
                out.add(AdviceItem("irrigate", "action", "💧", "আজ সেচ দিন", "Irrigate Today",
                    "মাটির পানি ${aw.roundToInt()}%, সীমা ${prof.trigger.roundToInt()}%-এর নিচে।",
                    "Moisture ${aw.roundToInt()}% is below trigger (${prof.trigger.roundToInt()}%).",
                    TwinActionSpec("irrigate"), "সেচ দিন", "Irrigate"))
            }
        }

        if (heavy != null) {
            out.add(AdviceItem("drain", "warn", "🌧️",
                if (isRice) "আইল ও পানির স্তর দেখুন" else "পানি নিষ্কাশনের ব্যবস্থা রাখুন",
                if (isRice) "Check Bunds & Overflow" else "Prepare Field Drainage",
                "${heavy.date.substring(5)} তারিখে ~${heavy.rainMm.roundToInt()} মিমি বৃষ্টির পূর্বাভাস।",
                "~${heavy.rainMm.roundToInt()} mm heavy rain forecast on ${heavy.date.substring(5)}."))
        }

        if (!ripening) {
            val map = mapOf("N" to s.n, "P" to s.p, "K" to s.k)
            for ((nKey, spec) in CropCatalog.FERT) {
                val lvl = map.getValue(nKey)
                if (lvl < LOW_LEVEL) {
                    if (heavy != null || rainSoon >= 25.0) {
                        out.add(AdviceItem("fert_${nKey}_wait", "info", "⏳", "${spec.nameBn} একটু পরে দিন", "Delay ${spec.nameEn}",
                            "$nKey কম (${lvl.roundToInt()}%), তবে ভারী বৃষ্টিতে ধুয়ে যাবে।",
                            "$nKey low (${lvl.roundToInt()}%), wait until heavy rain passes."))
                    } else {
                        out.add(AdviceItem("fert_$nKey", "action", "🌱", "${spec.nameBn} দিন", "Apply ${spec.nameEn}",
                            "$nKey কমে ${lvl.roundToInt()}%-এ নেমেছে (সীমা ${LOW_LEVEL.roundToInt()})।",
                            "$nKey dropped to ${lvl.roundToInt()}% (threshold ${LOW_LEVEL.roundToInt()}%).",
                            TwinActionSpec("fertilize", nutrient = nKey), "${spec.nameBn} দিন", "Apply $nKey"))
                    }
                }
            }
        }

        if (s.ph < crop.phMin) {
            out.add(AdviceItem("lime", "action", "🧪", "মাটি অম্লীয়, চুন দিন", "Soil Acidic — Add Lime",
                "pH ${round2(s.ph)}, ফসলের জন্য চাই ${crop.phMin}–${crop.phMax}।",
                "Soil pH ${round2(s.ph)} below ideal ${crop.phMin}–${crop.phMax}.",
                TwinActionSpec("lime"), "চুন দিন", "Apply Lime"))
        } else if (s.ph > crop.phMax) {
            out.add(AdviceItem("ph_high", "warn", "🧪", "মাটির pH বেশি", "Soil Alkaline",
                "pH ${round2(s.ph)} (সীমা ${crop.phMax}); জৈব সার বাড়ান।",
                "Soil pH ${round2(s.ph)} exceeds ${crop.phMax}; increase organic compost."))
        }
        val saltTol = CropCatalog.SALT_TOL[crop.id] ?: 3.0
        if (s.ec > saltTol) {
            out.add(AdviceItem("salt", "warn", "🧂", "মাটিতে লবণ বেশি", "High Salinity",
                "EC ${round2(s.ec)} dS/m (সহনসীমা $saltTol); মিঠা পানিতে সেচ দিন।",
                "EC ${round2(s.ec)} dS/m exceeds crop tolerance $saltTol dS/m."))
        }

        if (s.dis >= 60.0) {
            out.add(AdviceItem("spray", "action", "🛡️", "রোগের ঝুঁকি বেশি, স্প্রে করুন", "High Disease Risk — Spray",
                "রোগের ঝুঁকি ${s.dis.roundToInt()}%। কৃষি অফিসের পরামর্শে স্প্রে করুন।",
                "Disease risk index ${s.dis.roundToInt()}%; apply recommended protectant.",
                TwinActionSpec("spray"), "স্প্রে করুন", "Spray Now"))
        } else if (s.dis >= 40.0) {
            out.add(AdviceItem("disease_watch", "warn", "🔍", "রোগের দিকে নজর রাখুন", "Watch for Disease",
                "ঝুঁকি ${s.dis.roundToInt()}% (মাঝারি)। পাতা পরীক্ষা করুন।",
                "Moderate risk (${s.dis.roundToInt()}%); inspect leaves closely."))
        }

        fc.take(3).firstOrNull { it.tmax >= 38.0 }?.let { hot ->
            out.add(AdviceItem("heat", "warn", "🔥", "গরমের চাপ আসছে", "Heat Wave Ahead",
                "${hot.date.substring(5)} তারিখে সর্বোচ্চ তাপ ~${hot.tmax.roundToInt()}°সে।",
                "Highs near ${hot.tmax.roundToInt()}°C on ${hot.date.substring(5)}; irrigate early/late."))
        }

        if (s.openQ.isNotEmpty()) {
            out.add(AdviceItem("questions", "info", "❓", "টুইনের ${BnFormat.num(s.openQ.size, AppLang.BN)}টি প্রশ্নের উত্তর দিন",
                "Answer ${s.openQ.size} Twin Check-in Questions",
                "আপনার উত্তর পেলে টুইন আরও নির্ভুল পরামর্শ দেবে।",
                "Answering quick field observations calibrates your twin state."))
        }

        if (out.none { it.level in setOf("critical", "action", "warn") }) {
            out.add(0, AdviceItem("all_ok", "ok", "✅", "আজ বাড়তি কিছু করতে হবে না", "Plot Conditions Healthy Today",
                "পানি, সার ও রোগের ঝুঁকি ঠিক আছে। দৈনিক চেক-ইন করে রাখুন।",
                "Water, nutrients, and disease pressure are within safe bands."))
        }

        val order = mapOf("critical" to 0, "action" to 1, "warn" to 2, "info" to 3, "ok" to 4)
        return out.sortedBy { order[it.level] ?: 9 }
    }

    fun buildSummaryText(s: PlotState, lang: AppLang): String {
        val crop = CropCatalog.BY_ID.getValue(s.cropId)
        val (_, st) = CropCatalog.stageAt(crop.id, max(s.day, 0), crop.durationDays)
        val est = if (s.status in ACTIVE) estimateYield(s, crop) else null
        return if (lang == AppLang.BN) {
            buildString {
                append("${s.name} (${crop.nameBn}): দিন ${BnFormat.num(s.day, AppLang.BN)}/${BnFormat.num(crop.durationDays, AppLang.BN)}, পর্যায় ${st.nameBn}, স্বাস্থ্য ${BnFormat.num(s.health, AppLang.BN)}%। ")
                append("মাটির আর্দ্রতা ${BnFormat.num(s.awPct(), AppLang.BN)}%, pH ${BnFormat.num(s.ph, 1, AppLang.BN)}, পাম্প ${if (s.pumpOn) "চালু" else "বন্ধ"}। ")
                est?.let { append("আনুমানিক ফলন ${BnFormat.num(it.yieldKg, AppLang.BN)} কেজি, লাভ ${BnFormat.num(it.profit, AppLang.BN)} টাকা।") }
                s.result?.let { append(" চূড়ান্ত ফলন ${BnFormat.num(it.yieldKg, AppLang.BN)} কেজি, লাভ/লোকসান ${BnFormat.num(it.profit, AppLang.BN)} টাকা।") }
            }
        } else {
            buildString {
                append("${s.name} (${crop.nameEn}): Day ${s.day}/${crop.durationDays}, stage ${st.nameEn}, health ${s.health.roundToInt()}%. ")
                append("Moisture ${s.awPct().roundToInt()}%, pH ${round1(s.ph)}, pump ${if (s.pumpOn) "ON" else "OFF"}. ")
                est?.let { append("Estimated yield ${it.yieldKg} kg, profit ${it.profit} BDT.") }
                s.result?.let { append(" Final outcome: ${it.yieldKg} kg, profit ${it.profit} BDT.") }
            }
        }
    }

    fun toJson(s: PlotState): String {
        val o = JSONObject()
        o.put("id", s.id)
        o.put("userId", s.userId)
        o.put("name", s.name)
        o.put("cropId", s.cropId)
        o.put("soil", s.soil)
        o.put("area", s.area)
        o.put("seed", s.seed)
        o.put("startDate", s.startDate)
        o.put("config", JSONObject().apply {
            put("autoIrrigation", s.config.autoIrrigation)
            put("autoInputs", s.config.autoInputs)
            put("smsEnabled", s.config.smsEnabled)
            put("phone", s.config.phone)
        })
        o.put("day", s.day)
        o.put("status", s.status)
        o.put("endReason", s.endReason ?: JSONObject.NULL)
        o.put("result", s.result?.let { r ->
            JSONObject().apply {
                put("yieldKg", r.yieldKg); put("revenue", r.revenue); put("cost", r.cost)
                put("profit", r.profit); put("yieldFactor", r.yieldFactor); put("outcome", r.outcome)
                put("day", r.day); put("health", r.health)
            }
        } ?: JSONObject.NULL)
        o.put("awc", s.awc); o.put("wMm", s.wMm); o.put("ph", s.ph); o.put("ec", s.ec)
        o.put("n", s.n); o.put("p", s.p); o.put("k", s.k); o.put("health", s.health)
        o.put("pumpOn", s.pumpOn); o.put("pumpManual", s.pumpManual); o.put("pumpDays", s.pumpDays)
        o.put("wlDays", s.wlDays); o.put("darkDays", s.darkDays); o.put("dis", s.dis)
        o.put("sprayUntil", s.sprayUntil); o.put("protectNeeded", s.protectNeeded)
        o.put("yfNum", s.yfNum); o.put("yfDen", s.yfDen); o.put("stand", s.stand)
        o.put("lastRain", JSONArray(s.lastRain))
        o.put("totalWaterL", s.totalWaterL); o.put("totalIrrigMm", s.totalIrrigMm)
        o.put("totalUreaKg", s.totalUreaKg); o.put("totalTspKg", s.totalTspKg)
        o.put("totalMopKg", s.totalMopKg); o.put("totalLimeKg", s.totalLimeKg)
        o.put("totalRainMm", s.totalRainMm); o.put("totalSprays", s.totalSprays)
        o.put("lastFertDay", JSONObject(s.lastFertDay))
        o.put("lastLimeDay", s.lastLimeDay)
        o.put("alertLast", JSONObject(s.alertLast))
        o.put("openQ", JSONObject(s.openQ))
        o.put("qLast", JSONObject(s.qLast))
        o.put("answers", JSONObject(s.answers))
        o.put("lastFactors", JSONObject(s.lastFactors))
        o.put("warningsBn", JSONArray(s.warningsBn))
        o.put("warningsEn", JSONArray(s.warningsEn))
        o.put("harvestSaved", s.harvestSaved)
        s.sensorConfig?.let { sc ->
            o.put("sensorConfig", JSONObject().apply {
                put("deviceId", sc.deviceId)
                put("wp", sc.adcAtWiltingPoint)
                put("fc", sc.adcAtFieldCapacity)
                put("min", sc.adcMin)
                put("max", sc.adcMax)
            })
        }
        o.put("alerts", JSONArray().apply {
            s.alerts.forEach { a ->
                put(JSONObject().apply {
                    put("key", a.key); put("level", a.level)
                    put("titleBn", a.titleBn); put("titleEn", a.titleEn)
                    put("textBn", a.textBn); put("textEn", a.textEn); put("day", a.day)
                    a.action?.let { act ->
                        put("actionType", act.type)
                        act.nutrient?.let { put("actionNutrient", it) }
                        act.on?.let { put("actionOn", it) }
                    }
                    a.why?.let { w ->
                        put("whyRuleBn", w.ruleBn); put("whyRuleEn", w.ruleEn)
                        put("whyFacts", JSONArray().apply {
                            w.facts.forEach { f ->
                                put(JSONObject().apply {
                                    put("bn", f.labelBn); put("en", f.labelEn); put("val", f.value)
                                })
                            }
                        })
                    }
                })
            }
        })
        o.put("reviewPending", JSONArray().apply {
            s.reviewPending.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id); put("day", r.day); put("label", r.label ?: "")
                    put("labelBn", r.labelBn ?: ""); put("score", r.score ?: 0.0)
                    put("reason", r.reason); put("status", r.status)
                    put("verdict", r.verdict ?: ""); put("expertLabel", r.expertLabel ?: "")
                    put("note", r.note ?: ""); put("by", r.by ?: "")
                })
            }
        })
        o.put("experiments", JSONArray().apply {
            s.experiments.forEach { ex ->
                put(JSONObject().apply {
                    put("id", ex.id); put("name", ex.name); put("createdAt", ex.createdAt)
                    put("plotDayStart", ex.plotDayStart); put("horizonDays", ex.horizonDays)
                    put("intervention", ex.intervention); put("interventionBn", ex.interventionBn); put("interventionEn", ex.interventionEn)
                    put("dH", ex.deltaHealth); put("dW", ex.deltaWaterL); put("dI", ex.deltaIrrigMm)
                    put("dY", ex.deltaYieldKg); put("dP", ex.deltaProfitBdt); put("dS", ex.deltaStress)
                    put("expBn", ex.explanationBn); put("expEn", ex.explanationEn)
                    fun mJson(m: ExperimentMetrics) = JSONObject().apply {
                        put("days", m.daysSimulated); put("st", m.status); put("h", m.health)
                        put("aw", m.soilMoisturePct); put("wl", m.waterL); put("im", m.irrigationMm)
                        put("y", m.estimatedYieldKg); put("p", m.estimatedProfitBdt); put("cs", m.cropStress)
                    }
                    put("base", mJson(ex.baseline))
                    put("scen", mJson(ex.scenario))
                })
            }
        })
        return o.toString()
    }

    fun fromJson(raw: String): PlotState {
        val o = JSONObject(raw)
        val cfgO = o.getJSONObject("config")
        val cfg = PlotConfig(
            autoIrrigation = cfgO.optBoolean("autoIrrigation", true),
            autoInputs = cfgO.optBoolean("autoInputs", false),
            smsEnabled = cfgO.optBoolean("smsEnabled", false),
            phone = cfgO.optString("phone", "")
        )
        val resObj = o.optJSONObject("result")
        val res = resObj?.let { r ->
            PlotResult(
                yieldKg = r.getInt("yieldKg"), revenue = r.getInt("revenue"), cost = r.getInt("cost"),
                profit = r.getInt("profit"), yieldFactor = r.getDouble("yieldFactor"),
                outcome = r.getString("outcome"), day = r.getInt("day"), health = r.getInt("health")
            )
        }
        val state = PlotState(
            id = o.getString("id"),
            userId = o.getString("userId"),
            name = o.getString("name"),
            cropId = o.getString("cropId"),
            soil = o.getString("soil"),
            area = o.getDouble("area"),
            seed = o.getInt("seed"),
            startDate = o.getString("startDate"),
            config = cfg,
            day = o.getInt("day"),
            status = o.getString("status"),
            endReason = o.optString("endReason", "").takeIf { it.isNotEmpty() && !o.isNull("endReason") },
            result = res,
            awc = o.getDouble("awc"),
            wMm = o.getDouble("wMm"),
            ph = o.getDouble("ph"),
            ec = o.getDouble("ec"),
            n = o.getDouble("n"),
            p = o.getDouble("p"),
            k = o.getDouble("k"),
            health = o.getDouble("health"),
            pumpOn = o.getBoolean("pumpOn"),
            pumpManual = o.getBoolean("pumpManual"),
            pumpDays = o.getInt("pumpDays"),
            wlDays = o.getInt("wlDays"),
            darkDays = o.getInt("darkDays"),
            dis = o.getDouble("dis"),
            sprayUntil = o.getInt("sprayUntil"),
            protectNeeded = o.getBoolean("protectNeeded"),
            yfNum = o.getDouble("yfNum"),
            yfDen = o.getDouble("yfDen"),
            stand = o.getDouble("stand"),
            totalWaterL = o.getDouble("totalWaterL"),
            totalIrrigMm = o.getDouble("totalIrrigMm"),
            totalUreaKg = o.getDouble("totalUreaKg"),
            totalTspKg = o.getDouble("totalTspKg"),
            totalMopKg = o.getDouble("totalMopKg"),
            totalLimeKg = o.getDouble("totalLimeKg"),
            totalRainMm = o.getDouble("totalRainMm"),
            totalSprays = o.getInt("totalSprays"),
            lastLimeDay = o.getInt("lastLimeDay"),
            harvestSaved = o.optBoolean("harvestSaved", false)
        )
        o.optJSONArray("lastRain")?.let { arr -> for (i in 0 until arr.length()) state.lastRain.add(arr.getDouble(i)) }
        o.optJSONObject("lastFertDay")?.let { m -> m.keys().forEach { state.lastFertDay[it] = m.getInt(it) } }
        o.optJSONObject("alertLast")?.let { m -> m.keys().forEach { state.alertLast[it] = m.getInt(it) } }
        o.optJSONObject("openQ")?.let { m -> m.keys().forEach { state.openQ[it] = m.getInt(it) } }
        o.optJSONObject("qLast")?.let { m -> m.keys().forEach { state.qLast[it] = m.getInt(it) } }
        o.optJSONObject("answers")?.let { m -> m.keys().forEach { state.answers[it] = m.getString(it) } }
        o.optJSONObject("lastFactors")?.let { m -> m.keys().forEach { state.lastFactors[it] = m.getDouble(it) } }
        o.optJSONArray("warningsBn")?.let { arr -> for (i in 0 until arr.length()) state.warningsBn.add(arr.getString(i)) }
        o.optJSONArray("warningsEn")?.let { arr -> for (i in 0 until arr.length()) state.warningsEn.add(arr.getString(i)) }
        o.optJSONObject("sensorConfig")?.let { sc ->
            state.sensorConfig = MoistureCalibration(
                deviceId = sc.getString("deviceId"),
                adcAtWiltingPoint = sc.getInt("wp"),
                adcAtFieldCapacity = sc.getInt("fc"),
                adcMin = sc.getInt("min"),
                adcMax = sc.getInt("max")
            )
        }
        o.optJSONArray("alerts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                val act = if (a.has("actionType")) TwinActionSpec(
                    type = a.getString("actionType"),
                    nutrient = a.optString("actionNutrient", "").takeIf { it.isNotEmpty() },
                    on = if (a.has("actionOn")) a.getBoolean("actionOn") else null
                ) else null
                val why = if (a.has("whyRuleBn")) {
                    val facts = mutableListOf<WhyFact>()
                    a.optJSONArray("whyFacts")?.let { fa ->
                        for (j in 0 until fa.length()) {
                            val fj = fa.getJSONObject(j)
                            facts.add(WhyFact(fj.getString("bn"), fj.getString("en"), fj.getString("val")))
                        }
                    }
                    WhyExplanation(a.getString("whyRuleBn"), a.getString("whyRuleEn"), facts)
                } else null
                state.alerts.add(PlotAlert(a.getString("key"), a.getString("level"), a.getString("titleBn"), a.getString("titleEn"), a.getString("textBn"), a.getString("textEn"), a.getInt("day"), act, why))
            }
        }
        o.optJSONArray("reviewPending")?.let { arr ->
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                state.reviewPending.add(
                    PhotoReviewItem(
                        id = r.getString("id"), day = r.getInt("day"),
                        label = r.optString("label").takeIf { it.isNotEmpty() },
                        labelBn = r.optString("labelBn").takeIf { it.isNotEmpty() },
                        score = r.optDouble("score"),
                        reason = r.getString("reason"), status = r.getString("status"),
                        verdict = r.optString("verdict").takeIf { it.isNotEmpty() },
                        expertLabel = r.optString("expertLabel").takeIf { it.isNotEmpty() },
                        note = r.optString("note").takeIf { it.isNotEmpty() },
                        by = r.optString("by").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }
        o.optJSONArray("experiments")?.let { arr ->
            fun parseM(m: JSONObject) = ExperimentMetrics(
                m.getInt("days"), m.getString("st"), m.getDouble("h"), m.getDouble("aw"),
                m.getDouble("wl"), m.getDouble("im"), m.getInt("y"), m.getInt("p"), m.getDouble("cs")
            )
            for (i in 0 until arr.length()) {
                val ex = arr.getJSONObject(i)
                state.experiments.add(
                    ExperimentRecord(
                        id = ex.getString("id"), name = ex.getString("name"), createdAt = ex.getString("createdAt"),
                        plotDayStart = ex.getInt("plotDayStart"), horizonDays = ex.getInt("horizonDays"),
                        intervention = ex.getString("intervention"), interventionBn = ex.getString("interventionBn"),
                        interventionEn = ex.getString("interventionEn"),
                        baseline = parseM(ex.getJSONObject("base")),
                        scenario = parseM(ex.getJSONObject("scen")),
                        deltaHealth = ex.getDouble("dH"), deltaWaterL = ex.getDouble("dW"),
                        deltaIrrigMm = ex.getDouble("dI"), deltaYieldKg = ex.getInt("dY"),
                        deltaProfitBdt = ex.getInt("dP"), deltaStress = ex.getDouble("dS"),
                        explanationBn = ex.getString("expBn"), explanationEn = ex.getString("expEn")
                    )
                )
            }
        }
        return state
    }
}
