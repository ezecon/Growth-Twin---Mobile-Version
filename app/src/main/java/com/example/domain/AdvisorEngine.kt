package com.example.domain

import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

data class SlotValue(
    val value: String, // normalized string representation
    val source: String // user | assumed
)

data class AdvisorQuestion(
    val slot: String,
    val textBn: String,
    val textEn: String,
    val options: List<AdvisorOption>
)

data class AdvisorOption(
    val value: String,
    val labelBn: String,
    val labelEn: String
)

data class ScenarioMoney(
    val revenue: Int,
    val profit: Int
)

data class CropEconomics(
    val cost: Int,
    val bad: ScenarioMoney,
    val base: ScenarioMoney,
    val good: ScenarioMoney,
    val breakevenPrice: Double?,
    val marginPct: Double
)

data class CropAssessment(
    val cropId: String,
    val nameBn: String,
    val nameEn: String,
    val status: String, // ok | warn | blocked
    val rating: String, // go | caution | no
    val ratingBn: String,
    val ratingEn: String,
    val reasonsBn: List<String>,
    val reasonsEn: List<String>,
    val penaltyPct: Double,
    val scenarios: CropEconomics,
    val score: Int
)

data class CropComparison(
    val verdict: String,
    val verdictBn: String,
    val verdictEn: String,
    val current: CropAssessment,
    val nextCrop: CropAssessment,
    val deltaBase: Int,
    val deltaWorst: Int,
    val deltaGood: Int
)

data class HarvestCropStat(
    val cropId: String,
    val harvests: Int,
    val avgYieldPerDecimal: Double,
    val best: Double,
    val worst: Double
)

data class OwnYieldRow(
    val cropId: String,
    val nameBn: String,
    val nameEn: String,
    val harvests: Int,
    val ownAvgKgDec: Double,
    val tableLowKgDec: Double,
    val tableBaseKgDec: Double,
    val tableHighKgDec: Double
)

data class AdvisorReport(
    val sessionId: String,
    val prompt: String,
    val slots: Map<String, SlotValue>,
    val pendingQuestions: List<AdvisorQuestion>,
    val needsFollowup: Boolean,
    val confidence: Double,
    val headlineBn: String,
    val headlineEn: String,
    val target: CropAssessment?,
    val alternatives: List<CropAssessment>,
    val best: CropAssessment?,
    val comparison: CropComparison?,
    val ownYields: List<OwnYieldRow>,
    val narrativeBn: String,
    val narrativeEn: String,
    val reviewRecommended: Boolean,
    val reviewStatus: String, // none | requested | reviewed
    val expertVerdict: String? = null,
    val expertNote: String? = null
)

object AdvisorEngine {
    private const val W_BASE = 0.6
    private const val W_WORST = 0.4
    private const val GO_MARGIN = 0.15
    private const val MAX_LOSS_SHARE = 0.20
    private const val MIN_CONF_GO = 0.60
    private const val REVIEW_CONF_THRESHOLD = 0.50

    private val ASK_ORDER = listOf("target_crop", "land_decimal", "last_crop", "season", "irrigation")

    private val ASSUMED_WEIGHT = mapOf(
        "target_crop" to 0.10, "land_decimal" to 0.10, "last_crop" to 0.15, "season" to 0.20,
        "irrigation" to 0.20, "soil_type" to 0.15, "ph" to 0.05, "years_same" to 0.05, "location" to 0.05
    )

    private val WORD_NUM = mapOf(
        "এক" to 1, "দুই" to 2, "তিন" to 3, "চার" to 4, "পাঁচ" to 5,
        "ছয়" to 6, "সাত" to 7, "আট" to 8, "নয়" to 9, "দশ" to 10
    )

    private val ALIASES: List<Pair<String, String>> = listOf(
        "বোরো ধান" to "boro", "বোরো" to "boro", "boro rice" to "boro", "boro" to "boro",
        "আমন ধান" to "aman", "আমন" to "aman", "aman" to "aman",
        "আউশ ধান" to "aus", "আউশ" to "aus", "aus" to "aus",
        "মসুর ডাল" to "lentil", "মসুর" to "lentil", "মশুর" to "lentil", "lentil" to "lentil",
        "টমেটো" to "tomato", "tomato" to "tomato",
        "ভুট্টা" to "maize", "ভূট্টা" to "maize", "maize" to "maize", "corn" to "maize",
        "সরিষা" to "mustard", "mustard" to "mustard",
        "আলু" to "potato", "potato" to "potato",
        "পাট" to "jute", "jute" to "jute",
        "গম" to "wheat", "wheat" to "wheat",
        "ধান" to "rice", "চাল" to "rice", "rice" to "rice", "paddy" to "rice"
    ).sortedByDescending { it.first.length }

    private val UNIT_TO_DECIMAL = mapOf(
        "বিঘা" to 33.0, "bigha" to 33.0,
        "শতাংশ" to 1.0, "শতক" to 1.0, "decimal" to 1.0, "decimals" to 1.0,
        "একর" to 100.0, "acre" to 100.0, "acres" to 100.0
    )

    private val LAND_RE = Regex("""(\d+(?:\.\d+)?)\s*(বিঘা|bigha|শতাংশ|শতক|decimals?|একর|acres?)""", RegexOption.IGNORE_CASE)
    private val YEARS_RE = Regex("""(\d+)\s*(?:বছর|বৎসর|years?|yrs?)""", RegexOption.IGNORE_CASE)
    private val PH_RE = Regex("""(?:ph|পিএইচ)\s*[:=]?\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private val LOC_RE = Regex("""([\u0980-\u09FFA-Za-z]+)\s*(?:উপজেলা|জেলা|upazila|district)""", RegexOption.IGNORE_CASE)
    private val SPLIT_RE = Regex("""[,،;।\.\n]|কিন্তু|এখন|এবার|তারপর|\bnow\b|\bbut\b""", RegexOption.IGNORE_CASE)

    private val HIST_CUES = listOf("গত", "আগে", "করেছি", "করেছিলাম", "করেছিলেন", "ছিল", "last", "previous", "grew", "planted")
    private val TGT_CUES = listOf("চাই", "করতে", "করব", "লাগাতে", "লাগাব", "want", "plan", "next", "would like")

    private val SOIL_KW = mapOf(
        "doash" to listOf("দোআঁশ", "দো-আঁশ", "loam"),
        "entel" to listOf("এঁটেল", "এটেল", "clay"),
        "beley" to listOf("বেলে", "sandy")
    )

    private val IRR_KW = listOf(
        "none" to listOf("সেচ নেই", "সেচের ব্যবস্থা নেই", "বৃষ্টিনির্ভর", "rainfed", "no irrigation"),
        "partial" to listOf("আংশিক সেচ", "কিছু সেচ", "partial irrigation"),
        "good" to listOf("সেচের ব্যবস্থা আছে", "গভীর নলকূপ", "শ্যালো", "পাম্প আছে", "good irrigation")
    )

    private val SEASON_KW = listOf(
        "kharif1" to listOf("খরিফ-১", "খরিফ ১", "kharif-1", "kharif1"),
        "kharif2" to listOf("খরিফ-২", "খরিফ ২", "kharif-2", "kharif2"),
        "rabi" to listOf("রবি", "rabi")
    )

    fun normalizeText(input: String): String {
        var t = BnFormat.toAsciiDigits(input)
        val wordAlt = WORD_NUM.keys.joinToString("|")
        val rx = Regex("($wordAlt)\\s+(?=বিঘা|শতাংশ|শতক|একর|বছর)")
        t = rx.replace(t) { m -> "${WORD_NUM[m.groupValues[1]]} " }
        return t
    }

    fun findCrops(text: String): List<String> {
        var t = text.lowercase()
        val hits = mutableListOf<Pair<Int, String>>()
        for ((alias, cid) in ALIASES) {
            val a = alias.lowercase()
            var start = 0
            while (true) {
                val i = t.indexOf(a, start)
                if (i == -1) break
                hits.add(i to cid)
                t = t.substring(0, i) + " ".repeat(a.length) + t.substring(i + a.length)
                start = i + a.length
            }
        }
        hits.sortBy { it.first }
        return hits.map { it.second }.distinct()
    }

    fun extractSlots(prompt: String): MutableMap<String, SlotValue> {
        val t = normalizeText(prompt)
        val low = t.lowercase()
        val out = mutableMapOf<String, SlotValue>()

        LAND_RE.find(t)?.let { m ->
            val v = m.groupValues[1].toDoubleOrNull()
            val mult = UNIT_TO_DECIMAL[m.groupValues[2].lowercase()] ?: 1.0
            if (v != null && v * mult in 1.0..5000.0) {
                out["land_decimal"] = SlotValue((v * mult).toString(), "user")
            }
        }
        PH_RE.find(t)?.let { m ->
            val v = m.groupValues[1].toDoubleOrNull()
            if (v != null && v in 3.0..10.0) out["ph"] = SlotValue(v.toString(), "user")
        }
        LOC_RE.find(t)?.let { m ->
            val loc = m.groupValues[1].trim().take(80)
            if (loc.isNotEmpty()) out["location"] = SlotValue(loc, "user")
        }
        for ((k, kws) in SOIL_KW) {
            if (kws.any { it in low }) {
                out["soil_type"] = SlotValue(k, "user")
                break
            }
        }
        for ((k, kws) in IRR_KW) {
            if (kws.any { it in low }) {
                out["irrigation"] = SlotValue(k, "user")
                break
            }
        }
        for ((k, kws) in SEASON_KW) {
            if (kws.any { it in low }) {
                out["season"] = SlotValue(k, "user")
                break
            }
        }

        var target: String? = null
        var last: String? = null
        for (clause in SPLIT_RE.split(t)) {
            val found = findCrops(clause)
            if (found.isEmpty()) continue
            val clLow = clause.lowercase()
            val hist = HIST_CUES.any { it in clLow }
            val tgt = TGT_CUES.any { it in clLow }
            if (hist && !tgt) {
                if (last == null) last = found[0]
                YEARS_RE.find(clause)?.groupValues?.get(1)?.toIntOrNull()?.let { y ->
                    if (y in 0..60) out["years_same"] = SlotValue(y.toString(), "user")
                }
            } else if (tgt && !hist) {
                if (target == null) target = found[0]
            }
        }
        if (target == null && last == null) {
            val allFound = findCrops(t)
            if (allFound.size == 1) target = allFound[0]
        }
        if (target != null) out["target_crop"] = SlotValue(target, "user")
        if (last != null) out["last_crop"] = SlotValue(last, "user")
        return out
    }

    fun inferSeason(today: LocalDate = LocalDate.now()): String = when (today.monthValue) {
        10, 11, 12, 1, 2 -> "rabi"
        3, 4, 5, 6 -> "kharif1"
        else -> "kharif2"
    }

    fun defaultValues(today: LocalDate = LocalDate.now()): Map<String, String> = mapOf(
        "target_crop" to "any",
        "land_decimal" to "33.0",
        "last_crop" to "none",
        "season" to inferSeason(today),
        "irrigation" to "partial",
        "soil_type" to "doash",
        "ph" to "6.3",
        "years_same" to "1"
    )

    fun buildQuestions(slotNames: List<String>): List<AdvisorQuestion> {
        val cropOpts = CropCatalog.CROPS.map { AdvisorOption(it.id, it.nameBn, it.nameEn) }
        return slotNames.mapNotNull { slot ->
            when (slot) {
                "target_crop" -> AdvisorQuestion(
                    slot,
                    "আপনি এখন কোন ফসল চাষ করতে চান?",
                    "Which crop do you want to grow next?",
                    cropOpts + AdvisorOption("any", "ঠিক করিনি, পরামর্শ দিন", "Not sure, suggest best")
                )
                "land_decimal" -> AdvisorQuestion(
                    slot,
                    "আপনার জমির পরিমাণ কত?",
                    "How much land do you have?",
                    listOf(
                        AdvisorOption("15", "১৫ শতাংশ", "15 decimal"),
                        AdvisorOption("33", "১ বিঘা (৩৩ শতাংশ)", "1 bigha (33 dec)"),
                        AdvisorOption("66", "২ বিঘা (৬৬ শতাংশ)", "2 bigha (66 dec)"),
                        AdvisorOption("100", "৩ বিঘা বা বেশি (১০০ শতাংশ)", "3+ bigha (100 dec)"),
                        AdvisorOption("unknown", "জানি না", "Unknown")
                    )
                )
                "last_crop" -> AdvisorQuestion(
                    slot,
                    "জমিতে সর্বশেষ কোন ফসল চাষ করেছিলেন?",
                    "What was the previous crop on this field?",
                    cropOpts + listOf(
                        AdvisorOption("none", "পতিত ছিল / কিছু চাষ হয়নি", "Fallow / None"),
                        AdvisorOption("unknown", "মনে নেই", "Don't remember")
                    )
                )
                "season" -> AdvisorQuestion(
                    slot,
                    "কোন মৌসুমে রোপণ/বপন করবেন?",
                    "Which planting season?",
                    listOf(
                        AdvisorOption("rabi", "রবি", "Rabi (Winter)"),
                        AdvisorOption("kharif1", "খরিফ-১", "Kharif-1 (Summer)"),
                        AdvisorOption("kharif2", "খরিফ-২", "Kharif-2 (Monsoon)"),
                        AdvisorOption("unknown", "জানি না", "Unknown")
                    )
                )
                "irrigation" -> AdvisorQuestion(
                    slot,
                    "সেচের ব্যবস্থা কেমন?",
                    "What is your irrigation access?",
                    listOf(
                        AdvisorOption("good", "ভালো", "Good irrigation"),
                        AdvisorOption("partial", "আংশিক", "Partial irrigation"),
                        AdvisorOption("none", "নেই (বৃষ্টিনির্ভর)", "None (Rainfed)"),
                        AdvisorOption("unknown", "জানি না", "Unknown")
                    )
                )
                else -> null
            }
        }
    }

    private fun computeConfidence(slots: Map<String, SlotValue>): Double {
        val pen = slots.entries.sumOf { (k, v) -> if (v.source == "assumed") (ASSUMED_WEIGHT[k] ?: 0.0) else 0.0 }
        return round(max(0.0, 1.0 - pen) * 100.0) / 100.0
    }

    private fun evaluateCrop(crop: Crop, slots: Map<String, SlotValue>, land: Double, confidence: Double): CropAssessment {
        val season = slots["season"]?.value ?: inferSeason()
        val soil = slots["soil_type"]?.value ?: "doash"
        val ph = slots["ph"]?.value?.toDoubleOrNull() ?: 6.3
        val irr = slots["irrigation"]?.value ?: "partial"
        val lastCrop = slots["last_crop"]?.value ?: "none"
        val yearsSame = slots["years_same"]?.value?.toIntOrNull() ?: 1
        val assumed = slots.filterValues { it.source == "assumed" }.keys

        var status = "ok"
        var penalty = 0.0
        val reasonsBn = mutableListOf<String>()
        val reasonsEn = mutableListOf<String>()

        fun warn(bn: String, en: String, pct: Double) {
            if (status != "blocked") status = "warn"
            penalty += pct
            reasonsBn.add(bn)
            reasonsEn.add(en)
        }

        fun block(bn: String, en: String, slot: String) {
            if (slot in assumed) {
                warn("$bn (এই তথ্য অনুমান করা, তাই যাচাই করুন)", "$en (assumed value — please verify)", 8.0)
            } else {
                status = "blocked"
                reasonsBn.add(bn)
                reasonsEn.add(en)
            }
        }

        if (season !in crop.seasons) {
            val okBn = crop.seasons.joinToString("/") { CropCatalog.seasonName(it, AppLang.BN) }
            val okEn = crop.seasons.joinToString("/") { CropCatalog.seasonName(it, AppLang.EN) }
            block(
                "${crop.nameBn} ${CropCatalog.seasonName(season, AppLang.BN)} মৌসুমের ফসল নয় (উপযুক্ত: $okBn)",
                "${crop.nameEn} is not suited for ${CropCatalog.seasonName(season, AppLang.EN)} (best: $okEn)",
                "season"
            )
        }

        if (ph < crop.phMin - 1.0 || ph > crop.phMax + 1.0) {
            block(
                "মাটির pH $ph এই ফসলের জন্য অনেক বেমানান (দরকার ${crop.phMin}-${crop.phMax})",
                "Soil pH $ph is far outside the safe band (${crop.phMin}-${crop.phMax})",
                "ph"
            )
        } else if (ph < crop.phMin || ph > crop.phMax) {
            warn(
                "মাটির pH $ph আদর্শ সীমার (প্রয়োজন ${crop.phMin}-${crop.phMax}) বাইরে",
                "Soil pH $ph is slightly outside ideal range (${crop.phMin}-${crop.phMax})",
                5.0
            )
        }

        if (soil !in crop.soils) {
            val okBn = crop.soils.joinToString("/") { CropCatalog.soilName(it, AppLang.BN) }
            val okEn = crop.soils.joinToString("/") { CropCatalog.soilName(it, AppLang.EN) }
            warn(
                "${CropCatalog.soilName(soil, AppLang.BN)} মাটি আদর্শ নয় (ভালো: $okBn)",
                "${CropCatalog.soilName(soil, AppLang.EN)} soil is not ideal (preferred: $okEn)",
                8.0
            )
        }

        when {
            crop.waterNeed == "high" && irr == "none" -> block(
                "${crop.nameBn}-এ প্রচুর পানি লাগে, কিন্তু সেচের ব্যবস্থা নেই",
                "${crop.nameEn} requires high irrigation, but field is rainfed",
                "irrigation"
            )
            crop.waterNeed == "high" && irr == "partial" -> warn(
                "${crop.nameBn}-এ প্রচুর পানি লাগে, আংশিক সেচে ঘাটতির ঝুঁকি আছে",
                "${crop.nameEn} needs high water; partial irrigation adds drought risk",
                10.0
            )
            crop.waterNeed == "medium" && irr == "none" -> warn(
                "${crop.nameBn}-এ মাঝারি সেচ লাগে, বৃষ্টিনির্ভর হলে ফলন কমতে পারে",
                "${crop.nameEn} needs moderate water; yields may drop if purely rainfed",
                10.0
            )
        }

        val prevFam = CropCatalog.familyOf(lastCrop)
        if (prevFam != null) {
            val rule = CropCatalog.ROTATION_RULES[prevFam to crop.family]
            if (rule != null) {
                val prevBn = CropCatalog.cropName(lastCrop, AppLang.BN)
                val prevEn = CropCatalog.cropName(lastCrop, AppLang.EN)
                when (rule.severity) {
                    "block" -> block("আগের ফসল $prevBn: ${rule.noteBn}", "Previous crop $prevEn: ${rule.noteEn}", "last_crop")
                    "warn" -> warn("আগের ফসল $prevBn: ${rule.noteBn}", "Previous crop $prevEn: ${rule.noteEn}", rule.penaltyPct)
                    "ok" -> if (rule.penaltyPct < 0) {
                        penalty += rule.penaltyPct
                        reasonsBn.add("ভালো দিক: ${rule.noteBn}")
                        reasonsEn.add("Rotation bonus: ${rule.noteEn}")
                    }
                }
            }
            if (prevFam == crop.family && yearsSame >= 3) {
                val extra = min(15.0, (yearsSame - 2) * 5.0)
                warn(
                    "একই ধরনের ফসল টানা $yearsSame বছর হয়েছে, মাটির স্বাস্থ্য ও রোগের চাপ বাড়ছে",
                    "Same crop family for $yearsSame years increases soil stress and pest buildup",
                    extra
                )
            }
        }

        val clampedPenalty = max(-10.0, min(penalty, 60.0))
        val factor = 1.0 - clampedPenalty / 100.0
        val cost = (crop.cost * land).roundToInt()
        val revBad = (crop.y.first * factor * crop.p.first * land).roundToInt()
        val revBase = (crop.y.second * factor * crop.p.second * land).roundToInt()
        val revGood = (crop.y.third * factor * crop.p.third * land).roundToInt()
        val baseYield = crop.y.second * factor * land
        val breakeven = if (baseYield > 0) round(cost / baseYield * 10.0) / 10.0 else null
        val baseProfit = revBase - cost
        val marginPct = if (cost > 0) round(baseProfit.toDouble() / cost * 1000.0) / 10.0 else 0.0

        val scenarios = CropEconomics(
            cost = cost,
            bad = ScenarioMoney(revBad, revBad - cost),
            base = ScenarioMoney(revBase, baseProfit),
            good = ScenarioMoney(revGood, revGood - cost),
            breakevenPrice = breakeven,
            marginPct = marginPct
        )

        val costRef = max(1, cost).toDouble()
        val healthy = status == "ok" &&
            baseProfit / costRef >= GO_MARGIN &&
            scenarios.bad.profit >= -MAX_LOSS_SHARE * costRef
        val rating = when {
            status == "blocked" || baseProfit <= 0 -> "no"
            healthy && confidence >= MIN_CONF_GO -> "go"
            else -> "caution"
        }
        val (ratingBn, ratingEn) = when (rating) {
            "go" -> "এগিয়ে যান" to "Go"
            "caution" -> "সতর্কতাসহ" to "Caution"
            else -> "সুপারিশ নয়" to "Not Advised"
        }
        val score = (W_BASE * baseProfit + W_WORST * scenarios.bad.profit).roundToInt()
        return CropAssessment(
            cropId = crop.id,
            nameBn = crop.nameBn,
            nameEn = crop.nameEn,
            status = status,
            rating = rating,
            ratingBn = ratingBn,
            ratingEn = ratingEn,
            reasonsBn = reasonsBn,
            reasonsEn = reasonsEn,
            penaltyPct = clampedPenalty,
            scenarios = scenarios,
            score = score
        )
    }

    private fun resolveTarget(target: String, season: String): String {
        if (target == "rice") {
            return when (season) {
                "rabi" -> "boro"
                "kharif1" -> "aus"
                else -> "aman"
            }
        }
        return target
    }

    fun evaluate(
        sessionId: String,
        prompt: String,
        userSlots: Map<String, SlotValue>,
        askedSlots: List<String>,
        harvestStats: List<HarvestCropStat>,
        finishNow: Boolean = false,
        reviewStatus: String = "none",
        expertVerdict: String? = null,
        expertNote: String? = null
    ): AdvisorReport {
        val missing = if (finishNow) emptyList() else ASK_ORDER.filter { it !in userSlots && it !in askedSlots }.take(3)
        val defaults = defaultValues()
        val fullSlots = userSlots.toMutableMap()
        defaults.forEach { (k, v) ->
            fullSlots.putIfAbsent(k, SlotValue(v, "assumed"))
        }

        val confidence = computeConfidence(fullSlots)
        val land = fullSlots["land_decimal"]?.value?.toDoubleOrNull() ?: 33.0
        val season = fullSlots["season"]?.value ?: inferSeason()
        val rawTarget = fullSlots["target_crop"]?.value ?: "any"
        val targetId = resolveTarget(rawTarget, season)

        val targetCrop = CropCatalog.BY_ID[targetId]
        val target = targetCrop?.let { evaluateCrop(it, fullSlots, land, confidence) }
        val pool = CropCatalog.CROPS.filter { season in it.seasons && it.id != targetId }
        val ranked = pool.map { evaluateCrop(it, fullSlots, land, confidence) }.sortedByDescending { it.score }
        val alternatives = ranked.filter { it.rating != "no" }.take(3)
        val best = if (target != null && target.rating != "no") target else alternatives.firstOrNull()

        val lastVal = fullSlots["last_crop"]?.value
        val newForCmp = target ?: best
        var comparison: CropComparison? = null
        if (lastVal != null && lastVal != "none" && newForCmp != null) {
            val curId = resolveTarget(lastVal, season)
            val curCrop = CropCatalog.BY_ID[curId]
            if (curCrop != null && curId != newForCmp.cropId) {
                val cur = evaluateCrop(curCrop, fullSlots, land, confidence)
                val verdict = when {
                    cur.status == "blocked" -> "current_not_feasible"
                    cur.rating == "no" && newForCmp.rating == "no" -> "both_weak"
                    newForCmp.rating == "no" -> "stay_better"
                    else -> {
                        val avgCost = ((cur.scenarios.cost + newForCmp.scenarios.cost) / 2.0).coerceAtLeast(1.0)
                        val dBase = newForCmp.scenarios.base.profit - cur.scenarios.base.profit
                        val dWorst = newForCmp.scenarios.bad.profit - cur.scenarios.bad.profit
                        when {
                            dBase > 0.05 * avgCost -> if (dWorst >= -0.10 * avgCost) "switch_better" else "switch_riskier"
                            dBase < -0.05 * avgCost -> "stay_better"
                            else -> "similar"
                        }
                    }
                }
                val dBase = newForCmp.scenarios.base.profit - cur.scenarios.base.profit
                val dWorst = newForCmp.scenarios.bad.profit - cur.scenarios.bad.profit
                val dGood = newForCmp.scenarios.good.profit - cur.scenarios.good.profit
                val verdictBn = when (verdict) {
                    "current_not_feasible" -> "${cur.nameBn} এই অবস্থায় আবার চাষ করা সম্ভব নয় (${cur.reasonsBn.take(2).joinToString("; ")})।"
                    "both_weak" -> "${cur.nameBn} চালিয়ে যাওয়া ও ${newForCmp.nameBn}-এ যাওয়া, দুটোই এই অবস্থায় দুর্বল; বিকল্প ফসল দেখুন।"
                    "stay_better" -> "${cur.nameBn} চালিয়ে গেলে গড় লাভ ${newForCmp.nameBn}-এর চেয়ে ${BnFormat.num(kotlin.math.abs(dBase), AppLang.BN)} টাকা বেশি।"
                    "switch_better" -> "${cur.nameBn}-এর বদলে ${newForCmp.nameBn} চাষে গড় লাভ ${BnFormat.num(dBase, AppLang.BN)} টাকা বেশি, খারাপ পরিস্থিতিতেও ক্ষতি বেশি বাড়ে না।"
                    "switch_riskier" -> "${newForCmp.nameBn}-এ গেলে গড় লাভ ${BnFormat.num(dBase, AppLang.BN)} টাকা বেশি, কিন্তু খারাপ পরিস্থিতিতে লোকসান ${BnFormat.num(kotlin.math.abs(dWorst), AppLang.BN)} টাকা বেশি। ঝুঁকি নিতে পারলে বিবেচনা করুন।"
                    else -> "${cur.nameBn} ও ${newForCmp.nameBn}-এর লাভের পার্থক্য সামান্য; মাটির স্বাস্থ্য ও দামের ঝুঁকি দেখে সিদ্ধান্ত নিন।"
                }
                val verdictEn = when (verdict) {
                    "current_not_feasible" -> "${cur.nameEn} is blocked under these conditions (${cur.reasonsEn.take(2).joinToString("; ")})."
                    "both_weak" -> "Both keeping ${cur.nameEn} and switching to ${newForCmp.nameEn} look weak; check alternatives."
                    "stay_better" -> "Keeping ${cur.nameEn} yields ${BnFormat.num(kotlin.math.abs(dBase), AppLang.EN)} BDT more base profit than switching to ${newForCmp.nameEn}."
                    "switch_better" -> "Switching to ${newForCmp.nameEn} increases base profit by ${BnFormat.num(dBase, AppLang.EN)} BDT without significantly higher downside."
                    "switch_riskier" -> "Switching adds ${BnFormat.num(dBase, AppLang.EN)} BDT base profit, but worst-case downside rises by ${BnFormat.num(kotlin.math.abs(dWorst), AppLang.EN)} BDT."
                    else -> "Profit difference between ${cur.nameEn} and ${newForCmp.nameEn} is minor; weigh soil health and price volatility."
                }
                comparison = CropComparison(verdict, verdictBn, verdictEn, cur, newForCmp, dBase, dWorst, dGood)
            }
        }

        val headlineBn = when {
            target == null -> if (best != null) "আপনার জমির জন্য সবচেয়ে উপযুক্ত ফসল: ${best.nameBn}" else "এই অবস্থায় কোনো ফসলের নিরাপদ সুপারিশ নেই"
            target.rating == "go" -> "${target.nameBn} চাষে এগিয়ে যাওয়া যায়"
            target.rating == "caution" -> "${target.nameBn} চাষ সম্ভব, তবে সতর্ক থাকতে হবে"
            else -> "${target.nameBn} এই অবস্থায় সুপারিশ করা হচ্ছে না;" + if (best != null) " বরং ${best.nameBn} বিবেচনা করুন" else ""
        }
        val headlineEn = when {
            target == null -> if (best != null) "Best crop option for your plot: ${best.nameEn}" else "No safe crop recommendation for these inputs"
            target.rating == "go" -> "${target.nameEn} looks feasible (Go)"
            target.rating == "caution" -> "${target.nameEn} is feasible with caution"
            else -> "${target.nameEn} is not recommended here;" + if (best != null) " consider ${best.nameEn} instead" else ""
        }

        val reviewRecommended = confidence < REVIEW_CONF_THRESHOLD ||
            (target != null && target.scenarios.bad.profit < -0.5 * target.scenarios.cost)

        val ownYields = harvestStats.mapNotNull { st ->
            val crop = CropCatalog.BY_ID[st.cropId] ?: return@mapNotNull null
            OwnYieldRow(
                cropId = crop.id,
                nameBn = crop.nameBn,
                nameEn = crop.nameEn,
                harvests = st.harvests,
                ownAvgKgDec = st.avgYieldPerDecimal,
                tableLowKgDec = crop.y.first,
                tableBaseKgDec = crop.y.second,
                tableHighKgDec = crop.y.third
            )
        }

        val narrativeBn = buildNarrativeBn(headlineBn, fullSlots, target, alternatives, comparison)
        val narrativeEn = buildNarrativeEn(headlineEn, fullSlots, target, alternatives, comparison)

        return AdvisorReport(
            sessionId = sessionId,
            prompt = prompt,
            slots = fullSlots,
            pendingQuestions = buildQuestions(missing),
            needsFollowup = missing.isNotEmpty(),
            confidence = confidence,
            headlineBn = headlineBn,
            headlineEn = headlineEn,
            target = target,
            alternatives = alternatives,
            best = best,
            comparison = comparison,
            ownYields = ownYields,
            narrativeBn = narrativeBn,
            narrativeEn = narrativeEn,
            reviewRecommended = reviewRecommended,
            reviewStatus = reviewStatus,
            expertVerdict = expertVerdict,
            expertNote = expertNote
        )
    }

    private fun buildNarrativeBn(
        headline: String,
        slots: Map<String, SlotValue>,
        target: CropAssessment?,
        alternatives: List<CropAssessment>,
        cmp: CropComparison?
    ): String {
        val sb = StringBuilder()
        sb.appendLine(headline)
        target?.let { t ->
            val s = t.scenarios
            sb.appendLine()
            sb.appendLine("${t.nameBn} — ${t.ratingBn}")
            sb.appendLine("সম্ভাব্য লাভ/লোকসান: ভালো ${BnFormat.num(s.good.profit, AppLang.BN)} ৳, গড় ${BnFormat.num(s.base.profit, AppLang.BN)} ৳, খারাপ ${BnFormat.num(s.bad.profit, AppLang.BN)} ৳ (খরচ ${BnFormat.num(s.cost, AppLang.BN)} ৳)।")
            s.breakevenPrice?.let {
                sb.appendLine("লোকসান এড়াতে প্রতি কেজি অন্তত ${BnFormat.num(it, 1, AppLang.BN)} টাকা দাম পেতে হবে।")
            }
            if (t.reasonsBn.isNotEmpty()) {
                sb.appendLine("মূল বিবেচ্য: " + t.reasonsBn.joinToString(" • "))
            }
        }
        cmp?.let {
            sb.appendLine()
            sb.appendLine("তুলনা: ${it.verdictBn}")
        }
        if (alternatives.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("বিকল্প: " + alternatives.joinToString(" | ") { a ->
                "${a.nameBn} (${a.ratingBn}: গড় ${BnFormat.num(a.scenarios.base.profit, AppLang.BN)} ৳)"
            })
        }
        val assumedCount = slots.values.count { it.source == "assumed" }
        if (assumedCount > 0) {
            sb.appendLine()
            sb.appendLine("নোট: ${BnFormat.num(assumedCount, AppLang.BN)}টি তথ্য অনুমান করা হয়েছে; আসল তথ্য দিলে নির্ভুলতা বাড়বে।")
        }
        return sb.toString().trim()
    }

    private fun buildNarrativeEn(
        headline: String,
        slots: Map<String, SlotValue>,
        target: CropAssessment?,
        alternatives: List<CropAssessment>,
        cmp: CropComparison?
    ): String {
        val sb = StringBuilder()
        sb.appendLine(headline)
        target?.let { t ->
            val s = t.scenarios
            sb.appendLine()
            sb.appendLine("${t.nameEn} — ${t.ratingEn}")
            sb.appendLine("Profit scenarios: Good ${s.good.profit} BDT, Base ${s.base.profit} BDT, Bad ${s.bad.profit} BDT (Cost ${s.cost} BDT).")
            s.breakevenPrice?.let {
                sb.appendLine("Break-even price needed: $it BDT/kg.")
            }
            if (t.reasonsEn.isNotEmpty()) {
                sb.appendLine("Key notes: " + t.reasonsEn.joinToString(" • "))
            }
        }
        cmp?.let {
            sb.appendLine()
            sb.appendLine("Comparison: ${it.verdictEn}")
        }
        if (alternatives.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Alternatives: " + alternatives.joinToString(" | ") { a ->
                "${a.nameEn} (${a.ratingEn}: base ${a.scenarios.base.profit} BDT)"
            })
        }
        val assumedCount = slots.values.count { it.source == "assumed" }
        if (assumedCount > 0) {
            sb.appendLine()
            sb.appendLine("Note: $assumedCount field values were defaulted; update pH/soil/season for higher confidence.")
        }
        return sb.toString().trim()
    }
}
