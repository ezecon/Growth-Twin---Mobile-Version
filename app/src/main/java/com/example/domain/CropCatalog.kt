package com.example.domain

import com.example.ui.theme.AppLang

data class Crop(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val family: String,
    val seasons: List<String>,
    val phMin: Double,
    val phMax: Double,
    val durationDays: Int,
    val soils: List<String>,
    val waterNeed: String,
    val y: Triple<Double, Double, Double>, // bad, base, good kg/decimal
    val p: Triple<Double, Double, Double>, // bad, base, good BDT/kg
    val cost: Double // BDT/decimal
) {
    fun displayName(lang: AppLang): String = if (lang == AppLang.BN) nameBn else nameEn
}

data class CropStage(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val end: Double,
    val kc: Double,
    val demand: Double,
    val sens: Double,
    val irrigate: Boolean = true
) {
    fun displayName(lang: AppLang): String = if (lang == AppLang.BN) nameBn else nameEn
}

data class RotationRule(
    val prevFamily: String,
    val nextFamily: String,
    val severity: String, // warn | block | ok
    val penaltyPct: Double,
    val noteBn: String,
    val noteEn: String
)

data class SoilWaterProfile(
    val stress: Double,
    val wilt: Double,
    val trigger: Double,
    val target: Double,
    val emergency: Double,
    val pumpMm: Double = 25.0
)

data class TempBounds(
    val optMin: Double,
    val optMax: Double,
    val critMin: Double,
    val critMax: Double
)

data class FertSpec(
    val key: String,
    val nameBn: String,
    val nameEn: String,
    val kgPerDecimal: Double,
    val gain: Double
)

object CropCatalog {
    val SEASONS_BN = mapOf("rabi" to "রবি", "kharif1" to "খরিফ-১", "kharif2" to "খরিফ-২")
    val SEASONS_EN = mapOf("rabi" to "Rabi", "kharif1" to "Kharif-1", "kharif2" to "Kharif-2")

    val SOILS_BN = mapOf("doash" to "দোআঁশ", "entel" to "এঁটেল", "beley" to "বেলে")
    val SOILS_EN = mapOf("doash" to "Loam", "entel" to "Clay", "beley" to "Sandy")

    val IRRIGATION_BN = mapOf("good" to "ভালো", "partial" to "আংশিক", "none" to "নেই (বৃষ্টিনির্ভর)")
    val IRRIGATION_EN = mapOf("good" to "Good", "partial" to "Partial", "none" to "None (Rainfed)")

    val CROPS: List<Crop> = listOf(
        Crop("boro", "বোরো ধান", "Boro rice", "cereal", listOf("rabi"), 5.5, 7.5, 140, listOf("doash", "entel"), "high",
            Triple(16.0, 24.0, 28.0), Triple(24.0, 30.0, 34.0), 600.0),
        Crop("aman", "আমন ধান", "Aman rice", "cereal", listOf("kharif2"), 5.5, 7.0, 125, listOf("doash", "entel"), "high",
            Triple(12.0, 18.0, 22.0), Triple(24.0, 30.0, 34.0), 450.0),
        Crop("aus", "আউশ ধান", "Aus rice", "cereal", listOf("kharif1"), 5.5, 7.0, 100, listOf("doash", "entel", "beley"), "medium",
            Triple(10.0, 16.0, 20.0), Triple(24.0, 30.0, 34.0), 400.0),
        Crop("wheat", "গম", "Wheat", "cereal", listOf("rabi"), 6.0, 7.5, 110, listOf("doash"), "medium",
            Triple(5.0, 9.0, 11.0), Triple(35.0, 45.0, 55.0), 300.0),
        Crop("maize", "ভুট্টা", "Maize", "cereal", listOf("rabi", "kharif1"), 5.8, 7.0, 130, listOf("doash", "beley"), "medium",
            Triple(18.0, 32.0, 40.0), Triple(18.0, 24.0, 30.0), 650.0),
        Crop("potato", "আলু", "Potato", "solanaceae", listOf("rabi"), 5.0, 6.5, 90, listOf("doash", "beley"), "medium",
            Triple(60.0, 100.0, 130.0), Triple(12.0, 20.0, 32.0), 1800.0),
        Crop("tomato", "টমেটো", "Tomato", "solanaceae", listOf("rabi"), 6.0, 7.0, 110, listOf("doash"), "medium",
            Triple(120.0, 220.0, 300.0), Triple(12.0, 25.0, 50.0), 2500.0),
        Crop("mustard", "সরিষা", "Mustard", "brassica", listOf("rabi"), 6.0, 7.5, 90, listOf("doash", "beley", "entel"), "low",
            Triple(3.0, 5.0, 6.5), Triple(70.0, 90.0, 110.0), 250.0),
        Crop("lentil", "মসুর ডাল", "Lentil", "legume", listOf("rabi"), 6.0, 7.5, 100, listOf("doash", "beley"), "low",
            Triple(2.5, 4.0, 5.0), Triple(90.0, 120.0, 150.0), 250.0),
        Crop("jute", "পাট", "Jute", "fiber", listOf("kharif1"), 6.0, 7.5, 120, listOf("doash", "entel"), "medium",
            Triple(6.0, 9.0, 11.0), Triple(50.0, 70.0, 90.0), 600.0)
    )

    val BY_ID: Map<String, Crop> = CROPS.associateBy { it.id }

    val ROTATION_RULES: Map<Pair<String, String>, RotationRule> = listOf(
        RotationRule(
            "cereal", "cereal", "warn", 6.0,
            "পরপর ধান/গমজাতীয় ফসলে একই রোগ-পোকা ও পুষ্টির ঘাটতি বাড়ে",
            "Repeated cereal crops increase shared pest/disease load and nutrient depletion."
        ),
        RotationRule(
            "solanaceae", "solanaceae", "block", 25.0,
            "আলু/টমেটো পরপর চাষে ঢলে পড়া ও নাবিধ্বসা রোগের ঝুঁকি খুব বেশি",
            "Consecutive potato/tomato cropping carries very high risk of wilt and late blight."
        ),
        RotationRule(
            "brassica", "brassica", "warn", 10.0,
            "সরিষাজাতীয় ফসল পরপর চাষে রোগ ও পোকার চাপ বাড়ে",
            "Back-to-back brassica crops elevate pest and disease pressure."
        ),
        RotationRule(
            "legume", "legume", "warn", 8.0,
            "ডালজাতীয় ফসল পরপর চাষে মাটিবাহিত রোগ বাড়তে পারে",
            "Consecutive legumes can increase soil-borne diseases."
        ),
        RotationRule(
            "legume", "cereal", "ok", -5.0,
            "ডালজাতীয় ফসলের পর মাটিতে নাইট্রোজেন বেশি থাকে যা পরবর্তী ধান/ভুট্টায় সুবিধা দেয়",
            "Legumes enrich soil nitrogen, giving a rotation benefit to the next cereal crop."
        )
    ).associateBy { it.prevFamily to it.nextFamily }

    val TEMPLATES: Map<String, List<CropStage>> = mapOf(
        "rice" to listOf(
            CropStage("germ", "বীজতলা ও অঙ্কুরোদগম", "Nursery & Germination", 0.10, 1.05, 0.3, 0.6),
            CropStage("seedling", "চারা", "Seedling", 0.22, 1.10, 0.6, 0.7),
            CropStage("tiller", "কুশি", "Tillering", 0.45, 1.15, 1.2, 1.0),
            CropStage("panicle", "থোড়", "Panicle Initiation", 0.60, 1.20, 1.4, 1.3),
            CropStage("flower", "ফুল আসা", "Flowering", 0.72, 1.20, 0.8, 2.0),
            CropStage("fill", "দানা ভরা", "Grain Filling", 0.92, 1.10, 0.6, 1.5),
            CropStage("ripen", "পরিপক্ব", "Ripening", 1.00, 0.90, 0.1, 0.5, irrigate = false)
        ),
        "cereal" to listOf(
            CropStage("germ", "অঙ্কুরোদগম", "Germination", 0.10, 0.40, 0.3, 0.6),
            CropStage("seedling", "চারা", "Seedling", 0.25, 0.70, 0.7, 0.8),
            CropStage("veg", "বৃদ্ধি", "Vegetative", 0.55, 1.05, 1.3, 1.0),
            CropStage("flower", "ফুল আসা", "Flowering", 0.70, 1.15, 0.9, 2.0),
            CropStage("fill", "দানা ভরা", "Grain Filling", 0.90, 0.90, 0.5, 1.5),
            CropStage("ripen", "পরিপক্ব", "Ripening", 1.00, 0.50, 0.1, 0.5, irrigate = false)
        ),
        "tuber" to listOf(
            CropStage("germ", "অঙ্কুরোদগম", "Sprouting", 0.12, 0.50, 0.3, 0.6),
            CropStage("veg", "গাছের বৃদ্ধি", "Vegetative", 0.40, 0.90, 1.2, 1.0),
            CropStage("tuber_init", "আলু ধরা শুরু", "Tuber Initiation", 0.60, 1.10, 1.4, 1.8),
            CropStage("bulk", "আলু বড় হওয়া", "Tuber Bulking", 0.90, 1.05, 0.8, 1.5),
            CropStage("ripen", "পরিপক্ব", "Maturity", 1.00, 0.70, 0.1, 0.5, irrigate = false)
        ),
        "veg" to listOf(
            CropStage("germ", "চারা তৈরি", "Transplant & Root", 0.15, 0.50, 0.4, 0.7),
            CropStage("veg", "বৃদ্ধি", "Vegetative", 0.40, 0.90, 1.2, 1.0),
            CropStage("flower", "ফুল ও ফল ধরা", "Flowering & Fruit Set", 0.65, 1.15, 1.3, 2.0),
            CropStage("fruit", "ফল বড় হওয়া", "Fruit Development", 0.92, 1.05, 0.9, 1.5),
            CropStage("ripen", "পরিপক্ব", "Ripening", 1.00, 0.85, 0.1, 0.5, irrigate = false)
        ),
        "oil" to listOf(
            CropStage("germ", "অঙ্কুরোদগম", "Germination", 0.10, 0.40, 0.3, 0.6),
            CropStage("veg", "বৃদ্ধি", "Vegetative", 0.45, 0.95, 1.3, 1.0),
            CropStage("flower", "ফুল আসা", "Flowering", 0.65, 1.10, 1.0, 2.0),
            CropStage("pod", "শুঁটি ভরা", "Pod Filling", 0.90, 0.85, 0.6, 1.5),
            CropStage("ripen", "পরিপক্ব", "Ripening", 1.00, 0.40, 0.1, 0.5, irrigate = false)
        ),
        "pulse" to listOf(
            CropStage("germ", "অঙ্কুরোদগম", "Germination", 0.10, 0.40, 0.2, 0.6),
            CropStage("veg", "বৃদ্ধি", "Vegetative", 0.45, 0.80, 1.0, 1.0),
            CropStage("flower", "ফুল আসা", "Flowering", 0.65, 1.00, 1.0, 2.0),
            CropStage("pod", "শুঁটি ভরা", "Pod Filling", 0.90, 0.80, 0.6, 1.5),
            CropStage("ripen", "পরিপক্ব", "Ripening", 1.00, 0.40, 0.1, 0.5, irrigate = false)
        ),
        "fiber" to listOf(
            CropStage("germ", "অঙ্কুরোদগম", "Germination", 0.10, 0.50, 0.3, 0.6),
            CropStage("veg", "দ্রুত বৃদ্ধি", "Rapid Growth", 0.70, 1.10, 1.4, 1.2),
            CropStage("bud", "কুঁড়ি আসা", "Budding", 0.90, 1.00, 0.7, 1.5),
            CropStage("ripen", "কর্তনযোগ্য", "Harvest Ready", 1.00, 0.80, 0.1, 0.5, irrigate = false)
        )
    )

    val CROP_TEMPLATE = mapOf(
        "boro" to "rice", "aman" to "rice", "aus" to "rice",
        "wheat" to "cereal", "maize" to "cereal",
        "potato" to "tuber", "tomato" to "veg",
        "mustard" to "oil", "lentil" to "pulse", "jute" to "fiber"
    )

    val TEMP = mapOf(
        "rice" to TempBounds(18.0, 30.0, 12.0, 38.0),
        "cereal" to TempBounds(14.0, 30.0, 4.0, 38.0),
        "tuber" to TempBounds(12.0, 24.0, 4.0, 34.0),
        "veg" to TempBounds(18.0, 30.0, 10.0, 38.0),
        "oil" to TempBounds(12.0, 26.0, 4.0, 36.0),
        "pulse" to TempBounds(12.0, 26.0, 4.0, 36.0),
        "fiber" to TempBounds(24.0, 34.0, 16.0, 40.0)
    )

    val LIGHT_NEED = mapOf(
        "rice" to 6.0, "cereal" to 6.0, "tuber" to 6.0, "veg" to 6.0,
        "oil" to 6.0, "pulse" to 5.5, "fiber" to 5.0
    )

    val SALT_TOL = mapOf(
        "boro" to 3.0, "aman" to 3.0, "aus" to 3.0, "wheat" to 6.0,
        "maize" to 1.7, "potato" to 1.7, "tomato" to 2.5, "mustard" to 4.0,
        "lentil" to 1.5, "jute" to 3.0
    )

    val FERT = mapOf(
        "N" to FertSpec("N", "ইউরিয়া", "Urea (N)", 0.35, 25.0),
        "P" to FertSpec("P", "টিএসপি", "TSP (P)", 0.25, 20.0),
        "K" to FertSpec("K", "এমওপি", "MOP (K)", 0.20, 20.0)
    )

    val AWC_MM = mapOf("doash" to 100.0, "entel" to 120.0, "beley" to 70.0)
    val SOIL_BUFFER = mapOf("doash" to 1.0, "entel" to 1.4, "beley" to 0.7)
    const val LIME_KG_PER_DECIMAL_PER_PH = 3.0

    fun templateOf(cropId: String): String = CROP_TEMPLATE[cropId] ?: "cereal"

    fun stagesOf(cropId: String): List<CropStage> = TEMPLATES[templateOf(cropId)] ?: TEMPLATES.getValue("cereal")

    fun stageAt(cropId: String, day: Int, duration: Int): Pair<Int, CropStage> {
        val st = stagesOf(cropId)
        val frac = day.toDouble() / duration.coerceAtLeast(1)
        st.forEachIndexed { idx, s ->
            if (frac <= s.end) return idx to s
        }
        return (st.size - 1) to st.last()
    }

    fun profile(cropId: String, waterNeed: String): SoilWaterProfile {
        return if (templateOf(cropId) == "rice") {
            SoilWaterProfile(
                stress = 95.0,
                wilt = 40.0,
                trigger = 110.0,
                target = 130.0,
                emergency = 40.0 + 0.5 * (95.0 - 40.0)
            )
        } else {
            val stress = when (waterNeed) {
                "high" -> 55.0
                "low" -> 35.0
                else -> 45.0
            }
            val wilt = 10.0
            SoilWaterProfile(
                stress = stress,
                wilt = wilt,
                trigger = stress + 10.0,
                target = 95.0,
                emergency = wilt + 0.5 * (stress - wilt)
            )
        }
    }

    fun seasonOfMonth(month: Int): String = when (month) {
        11, 12, 1, 2 -> "rabi"
        3, 4, 5, 6 -> "kharif1"
        else -> "kharif2"
    }

    fun familyOf(cropId: String?): String? = when (cropId) {
        null, "none" -> null
        "rice" -> "cereal"
        else -> BY_ID[cropId]?.family
    }

    fun cropName(cropId: String, lang: AppLang): String = when (cropId) {
        "rice" -> if (lang == AppLang.BN) "ধান" else "Rice"
        "none" -> if (lang == AppLang.BN) "কিছু নয়" else "None"
        else -> BY_ID[cropId]?.displayName(lang) ?: cropId
    }

    fun seasonName(seasonId: String, lang: AppLang): String =
        (if (lang == AppLang.BN) SEASONS_BN[seasonId] else SEASONS_EN[seasonId]) ?: seasonId

    fun soilName(soilId: String, lang: AppLang): String =
        (if (lang == AppLang.BN) SOILS_BN[soilId] else SOILS_EN[soilId]) ?: soilId

    fun irrigationName(irrId: String, lang: AppLang): String =
        (if (lang == AppLang.BN) IRRIGATION_BN[irrId] else IRRIGATION_EN[irrId]) ?: irrId
}
