package com.example.domain

import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class DistrictMarketInfo(
    val nameEn: String,
    val nameBn: String,
    val lat: Double,
    val lon: Double
)

data class BetterMarketOption(
    val districtEn: String,
    val districtBn: String,
    val wholesalePrice: Double,
    val distanceKm: Int,
    val estNetPrice: Double,
    val gainVsLocal: Double
)

data class MarketPriceAnalysis(
    val query: String,
    val mode: String, // price | market | rights | other
    val cropKey: String?,
    val cropBn: String?,
    val cropEn: String?,
    val districtEn: String?,
    val districtBn: String?,
    val fairFarmGate: Double?,
    val wholesalePrice: Double?,
    val retailPrice: Double?,
    val offerPerKg: Double?,
    val quantityKg: Double,
    val verdictBn: String?,
    val verdictEn: String?,
    val lossPerKg: Double?,
    val totalLoss: Int?,
    val middlemanMarginPct: Int?,
    val basisBn: String?,
    val basisEn: String?,
    val localNetPrice: Double?,
    val betterMarkets: List<BetterMarketOption>,
    val routeNoteBn: String,
    val routeNoteEn: String,
    val answerBn: String,
    val answerEn: String,
    val warningsBn: List<String>,
    val warningsEn: List<String>,
    val policyCitations: List<Pair<String, String>> = emptyList()
)

object FairMarketEngine {
    const val FARM_SHARE = 0.70
    const val TRANSPORT_TK_PER_KG_KM = 0.01
    const val ROUTE_MAX_KM = 200.0

    val DISTRICTS: List<DistrictMarketInfo> = listOf(
        DistrictMarketInfo("Dhaka", "ঢাকা", 23.81, 90.41),
        DistrictMarketInfo("Bogura", "বগুড়া", 24.85, 89.37),
        DistrictMarketInfo("Rangpur", "রংপুর", 25.75, 89.25),
        DistrictMarketInfo("Dinajpur", "দিনাজপুর", 25.63, 88.64),
        DistrictMarketInfo("Rajshahi", "রাজশাহী", 24.37, 88.60),
        DistrictMarketInfo("Jashore", "যশোর", 23.17, 89.21),
        DistrictMarketInfo("Khulna", "খুলনা", 22.85, 89.54),
        DistrictMarketInfo("Chattogram", "চট্টগ্রাম", 22.36, 91.78),
        DistrictMarketInfo("Cumilla", "কুমিল্লা", 23.46, 91.18),
        DistrictMarketInfo("Sylhet", "সিলেট", 24.90, 91.87),
        DistrictMarketInfo("Mymensingh", "ময়মনসিংহ", 24.75, 90.40),
        DistrictMarketInfo("Barishal", "বরিশাল", 22.70, 90.37),
        DistrictMarketInfo("Pabna", "পাবনা", 24.00, 89.23),
        DistrictMarketInfo("Faridpur", "ফরিদপুর", 23.60, 89.84),
        DistrictMarketInfo("Kushtia", "কুষ্টিয়া", 23.90, 89.12),
        DistrictMarketInfo("Naogaon", "নওগাঁ", 24.80, 88.94),
        DistrictMarketInfo("Natore", "নাটোর", 24.41, 89.00),
        DistrictMarketInfo("Sirajganj", "সিরাজগঞ্জ", 24.45, 89.70),
        DistrictMarketInfo("Tangail", "টাঙ্গাইল", 24.25, 89.92),
        DistrictMarketInfo("Gazipur", "গাজীপুর", 24.00, 90.43),
        DistrictMarketInfo("Munshiganj", "মুন্সীগঞ্জ", 23.54, 90.53),
        DistrictMarketInfo("Narsingdi", "নরসিংদী", 23.92, 90.72),
        DistrictMarketInfo("Noakhali", "নোয়াখালী", 22.87, 91.10),
        DistrictMarketInfo("Feni", "ফেনী", 23.02, 91.40),
        DistrictMarketInfo("Joypurhat", "জয়পুরহাট", 25.10, 89.02),
        DistrictMarketInfo("Thakurgaon", "ঠাকুরগাঁও", 26.03, 88.47),
        DistrictMarketInfo("Jamalpur", "জামালপুর", 24.92, 89.94),
        DistrictMarketInfo("Jhenaidah", "ঝিনাইদহ", 23.54, 89.17),
        DistrictMarketInfo("Satkhira", "সাতক্ষীরা", 22.72, 89.07),
        DistrictMarketInfo("Chapainawabganj", "চাঁপাইনবাবগঞ্জ", 24.60, 88.28),
        DistrictMarketInfo("Gaibandha", "গাইবান্ধা", 25.33, 89.53),
        DistrictMarketInfo("Kurigram", "কুড়িগ্রাম", 25.81, 89.65),
        DistrictMarketInfo("Habiganj", "হবিগঞ্জ", 24.38, 91.42),
        DistrictMarketInfo("Moulvibazar", "মৌলভীবাজার", 24.48, 91.77),
        DistrictMarketInfo("Brahmanbaria", "ব্রাহ্মণবাড়িয়া", 23.96, 91.11)
    )

    private data class RefCropPrice(
        val key: String,
        val bn: String,
        val en: String,
        val wholesaleRef: Double,
        val retailRef: Double,
        val damProducerRef: Double?, // Official DAM reference when available
        val aliases: List<String>,
        val marketFactorMap: Map<String, Double>
    )

    private val REF_CROPS = listOf(
        RefCropPrice("potato", "আলু", "Potato", 22.0, 32.0, 17.5,
            listOf("আলু", "potato"), mapOf("Munshiganj" to 0.95, "Bogura" to 0.92, "Dhaka" to 1.18, "Gazipur" to 1.12, "Chattogram" to 1.22, "Sylhet" to 1.16)),
        RefCropPrice("sweet potato", "মিষ্টি আলু", "Sweet Potato", 28.0, 40.0, null,
            listOf("মিষ্টি আলু", "sweet potato"), mapOf("Brahmanbaria" to 0.94, "Dhaka" to 1.15, "Cumilla" to 1.08)),
        RefCropPrice("onion", "পেঁয়াজ", "Onion", 68.0, 82.0, 56.0,
            listOf("পেঁয়াজ", "পিয়াজ", "পেয়াজ", "onion", "piyaj"), mapOf("Faridpur" to 0.92, "Pabna" to 0.94, "Dhaka" to 1.14, "Rajshahi" to 1.02, "Chattogram" to 1.18)),
        RefCropPrice("tomato", "টমেটো", "Tomato", 34.0, 50.0, 26.0,
            listOf("টমেটো", "tomato"), mapOf("Bogura" to 0.88, "Jashore" to 0.94, "Dhaka" to 1.20, "Chattogram" to 1.24, "Sylhet" to 1.15)),
        RefCropPrice("paddy", "ধান", "Paddy", 32.0, 36.0, 32.0,
            listOf("ধান", "বোরো", "আমন", "paddy", "boro", "aman"), mapOf("Dinajpur" to 0.95, "Naogaon" to 0.96, "Dhaka" to 1.09, "Bogura" to 1.01, "Sylhet" to 1.08)),
        RefCropPrice("rice", "চাল", "Rice", 54.0, 62.0, null,
            listOf("চাল", "rice"), mapOf("Dinajpur" to 0.93, "Dhaka" to 1.10, "Bogura" to 0.98, "Chattogram" to 1.12)),
        RefCropPrice("garlic", "রসুন", "Garlic", 130.0, 165.0, null,
            listOf("রসুন", "garlic"), mapOf("Natore" to 0.92, "Pabna" to 0.95, "Dhaka" to 1.14, "Chattogram" to 1.16)),
        RefCropPrice("dried chilli", "শুকনা মরিচ", "Dried Chilli", 290.0, 350.0, null,
            listOf("শুকনা মরিচ", "শুকনো মরিচ", "dried chilli", "dried chili"), mapOf("Bogura" to 0.94, "Dhaka" to 1.12)),
        RefCropPrice("chilli", "কাঁচা মরিচ", "Green Chilli", 85.0, 110.0, null,
            listOf("মরিচ", "কাঁচা মরিচ", "chilli", "chili"), mapOf("Bogura" to 0.90, "Pabna" to 0.95, "Dhaka" to 1.18, "Sylhet" to 1.20)),
        RefCropPrice("brinjal", "বেগুন", "Brinjal", 38.0, 55.0, null,
            listOf("বেগুন", "brinjal", "eggplant"), mapOf("Jashore" to 0.88, "Bogura" to 0.90, "Dhaka" to 1.22, "Chattogram" to 1.18)),
        RefCropPrice("lentil", "মসুর ডাল", "Lentil", 112.0, 132.0, 95.0,
            listOf("মসুর", "lentil"), mapOf("Faridpur" to 0.94, "Jashore" to 0.95, "Dhaka" to 1.09)),
        RefCropPrice("ginger", "আদা", "Ginger", 150.0, 190.0, null,
            listOf("আদা", "ginger"), mapOf("Rangpur" to 0.92, "Dhaka" to 1.15)),
        RefCropPrice("jute", "পাট", "Jute", 72.0, 82.0, null,
            listOf("পাট", "jute"), mapOf("Faridpur" to 0.98, "Jamalpur" to 0.96, "Dhaka" to 1.08)),
        RefCropPrice("wheat", "গম", "Wheat", 42.0, 48.0, null,
            listOf("গম", "wheat"), mapOf("Dinajpur" to 0.95, "Thakurgaon" to 0.94, "Dhaka" to 1.08)),
        RefCropPrice("cabbage", "বাঁধাকপি", "Cabbage", 20.0, 30.0, null,
            listOf("বাঁধাকপি", "cabbage"), mapOf("Bogura" to 0.85, "Jashore" to 0.90, "Dhaka" to 1.25)),
        RefCropPrice("cauliflower", "ফুলকপি", "Cauliflower", 26.0, 40.0, null,
            listOf("ফুলকপি", "cauliflower"), mapOf("Bogura" to 0.86, "Dhaka" to 1.24)),
        RefCropPrice("pumpkin", "কুমড়া", "Pumpkin", 22.0, 34.0, null,
            listOf("কুমড়া", "কুমড়া", "pumpkin"), mapOf("Dhaka" to 1.15)),
        RefCropPrice("cucumber", "শসা", "Cucumber", 30.0, 45.0, null,
            listOf("শসা", "cucumber"), mapOf("Jashore" to 0.90, "Dhaka" to 1.20)),
        RefCropPrice("okra", "ঢেঁড়স", "Okra", 34.0, 50.0, null,
            listOf("ঢেঁড়স", "ঢেঁড়স", "okra"), mapOf("Dhaka" to 1.18))
    )

    private val DISTRICT_ALIASES = mapOf(
        "ঢাকা" to "Dhaka", "ধাকা" to "Dhaka",
        "বগুড়া" to "Bogura", "বগুড়া" to "Bogura", "bogra" to "Bogura",
        "রংপুর" to "Rangpur", "দিনাজপুর" to "Dinajpur", "রাজশাহী" to "Rajshahi",
        "যশোর" to "Jashore", "jessore" to "Jashore", "খুলনা" to "Khulna",
        "চট্টগ্রাম" to "Chattogram", "chittagong" to "Chattogram",
        "কুমিল্লা" to "Cumilla", "comilla" to "Cumilla",
        "সিলেট" to "Sylhet", "ময়মনসিংহ" to "Mymensingh", "বরিশাল" to "Barishal", "barisal" to "Barishal",
        "পাবনা" to "Pabna", "ফরিদপুর" to "Faridpur", "কুষ্টিয়া" to "Kushtia",
        "নওগাঁ" to "Naogaon", "নাটোর" to "Natore", "সিরাজগঞ্জ" to "Sirajganj",
        "টাঙ্গাইল" to "Tangail", "গাজীপুর" to "Gazipur", "মুন্সীগঞ্জ" to "Munshiganj",
        "নরসিংদী" to "Narsingdi", "নোয়াখালী" to "Noakhali", "ফেনী" to "Feni",
        "জয়পুরহাট" to "Joypurhat", "ঠাকুরগাঁও" to "Thakurgaon", "জামালপুর" to "Jamalpur",
        "ঝিনাইদহ" to "Jhenaidah", "সাতক্ষীরা" to "Satkhira", "ব্রাহ্মণবাড়িয়া" to "Brahmanbaria"
    )

    private fun haversineKm(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val p1 = Math.toRadians(a.first)
        val p2 = Math.toRadians(b.first)
        val dp = Math.toRadians(b.first - a.first)
        val dl = Math.toRadians(b.second - a.second)
        val h = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return 6371.0 * 2.0 * asin(sqrt(h))
    }

    private fun parseDistrict(query: String): DistrictMarketInfo? {
        val qLow = query.lowercase()
        for ((alias, en) in DISTRICT_ALIASES) {
            if (alias in qLow) return DISTRICTS.firstOrNull { it.nameEn == en }
        }
        return DISTRICTS.firstOrNull { it.nameEn.lowercase() in qLow || it.nameBn in query }
    }

    private fun parseOfferPerKg(query: String): Triple<Double?, String?, Boolean> {
        val t = BnFormat.toAsciiDigits(query)
        val regex = Regex("""(\d+(?:\.\d+)?)\s*(টাকা|tk|taka)?\s*(?:প্রতি|per|/)?\s*(কেজি|kg|মণ|মন|maund|কুইন্টাল|quintal)?""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(t).toList()
        val chosen = matches.firstOrNull { it.groupValues[1].toDoubleOrNull() != null && (it.groupValues[2].isNotEmpty() || it.groupValues[3].isNotEmpty() || "বল" in t || "offer" in t.lowercase() || "দাম" in t || "বেচ" in t) }
            ?: return Triple(null, null, false)
        val raw = chosen.groupValues[1].toDoubleOrNull() ?: return Triple(null, null, false)
        val unitRaw = chosen.groupValues[3].lowercase()
        val unitKg = when {
            unitRaw in listOf("মণ", "মন", "maund") -> 40.0 to "মণ"
            unitRaw in listOf("কুইন্টাল", "quintal") -> 100.0 to "কুইন্টাল"
            else -> 1.0 to "কেজি"
        }
        return Triple(round((raw / unitKg.first) * 10.0) / 10.0, unitKg.second, false)
    }

    fun analyze(rawQuery: String, quantityKg: Double = 100.0): MarketPriceAnalysis {
        val q = rawQuery.trim()
        val qLow = q.lowercase()
        val qty = max(1.0, quantityKg)

        // Check rights mode keywords
        val isRights = listOf("অধিকার", "ওজন", "কম ওজন", "প্রতারণা", "অভিযোগ", "লাইসেন্স", "আইন", "দালাল", "rights", "fraud", "weigh", "complaint", "dam", "law")
            .any { it in qLow }
        if (isRights) {
            val policies = listOf(
                "Agricultural Marketing Act 2018 (DAM)" to "অতিরিক্ত মুনাফা, ওজনে কারচুপি বা অবৈধ সিন্ডিকেটের বিরুদ্ধে জেলা কৃষি বিপণন কর্মকর্তার (DAM) কাছে অভিযোগ করা যায়।",
                "Right to Fair Weighment" to "বিক্রির সময় ডিজিটাল বা স্ট্যাম্পযুক্ত সঠিক বাটখারা ব্যবহারের দাবি করার অধিকার কৃষকের আছে।",
                "Local Dispute & DAM Helpline" to "বাজার ইজারাদারের অতিরিক্ত খাজনা বা দালালদের চাপের ক্ষেত্রে উপজেলা কৃষি অফিসার বা ১৬১২৩ / ৩৩৩ হেল্পলাইনে সহায়তা পাওয়া যায়।"
            )
            val ansBn = policies.joinToString("\n• ", prefix = "• ") { it.second } + "\n\n(নোট: এটি সাধারণ তথ্য, আইনি পরামর্শ নয়।)"
            val ansEn = "Under the Bangladesh Agricultural Marketing framework (DAM):\n" +
                "• Farmers can report weighing fraud, forced deductions, or illegal tolls to the Upazila/District Agricultural Marketing Officer (DAM).\n" +
                "• Buyers must use calibrated scales; extra hidden commission ('dhali') beyond market rules can be challenged.\n" +
                "(Note: Informational reference only, not formal legal advice.)"
            return MarketPriceAnalysis(
                query = q, mode = "rights", cropKey = null, cropBn = null, cropEn = null,
                districtEn = null, districtBn = null, fairFarmGate = null, wholesalePrice = null,
                retailPrice = null, offerPerKg = null, quantityKg = qty, verdictBn = null, verdictEn = null,
                lossPerKg = null, totalLoss = null, middlemanMarginPct = null, basisBn = null, basisEn = null,
                localNetPrice = null, betterMarkets = emptyList(), routeNoteBn = "", routeNoteEn = "",
                answerBn = ansBn, answerEn = ansEn, warningsBn = emptyList(), warningsEn = emptyList(),
                policyCitations = policies
            )
        }

        val crop = REF_CROPS.firstOrNull { c -> c.aliases.any { it.lowercase() in qLow } }
        if (crop == null) {
            return MarketPriceAnalysis(
                query = q, mode = "other", cropKey = null, cropBn = null, cropEn = null,
                districtEn = null, districtBn = null, fairFarmGate = null, wholesalePrice = null,
                retailPrice = null, offerPerKg = null, quantityKg = qty, verdictBn = null, verdictEn = null,
                lossPerKg = null, totalLoss = null, middlemanMarginPct = null, basisBn = null, basisEn = null,
                localNetPrice = null, betterMarkets = emptyList(), routeNoteBn = "", routeNoteEn = "",
                answerBn = "কোন ফসলের দাম বা বাজার জানতে চান? যেমন: আলু, পেঁয়াজ, টমেটো, ধান বা মরিচ লিখুন।",
                answerEn = "Which crop price or market would you like to check? Try potato, onion, tomato, paddy, or chilli.",
                warningsBn = emptyList(), warningsEn = emptyList()
            )
        }

        val district = parseDistrict(q)
        val distFactor = district?.let { crop.marketFactorMap[it.nameEn] } ?: 1.0
        val localWholesale = round(crop.wholesaleRef * distFactor * 10.0) / 10.0
        val localRetail = round(crop.retailRef * distFactor * 10.0) / 10.0
        val fairPrice = crop.damProducerRef ?: round(localWholesale * FARM_SHARE * 10.0) / 10.0
        val (basisBn, basisEn) = if (crop.damProducerRef != null) {
            "DAM রেফারেন্স উৎপাদক-মূল্য" to "DAM Reference Producer Baseline"
        } else {
            "আনুমানিক (পাইকারি দামের ৭০%)" to "Estimated (70% of wholesale ref.)"
        }

        val (offerPerKg, _, _) = parseOfferPerKg(q)
        var verdictBn: String? = null
        var verdictEn: String? = null
        var lossPerKg: Double? = null
        var totalLoss: Int? = null
        var marginPct: Int? = null
        val warningsBn = mutableListOf<String>()
        val warningsEn = mutableListOf<String>()

        if (offerPerKg != null) {
            lossPerKg = round(max(fairPrice - offerPerKg, 0.0) * 10.0) / 10.0
            totalLoss = (lossPerKg * qty).roundToInt()
            marginPct = round((localRetail - offerPerKg) / localRetail * 100.0).toInt()
            when {
                offerPerKg < fairPrice * 0.85 -> {
                    verdictBn = "অন্যায্য রকম কম"
                    verdictEn = "Unfairly Low"
                }
                offerPerKg < fairPrice -> {
                    verdictBn = "কিছুটা কম"
                    verdictEn = "Slightly Below Fair"
                }
                else -> {
                    verdictBn = "ন্যায্য"
                    verdictEn = "Fair Offer"
                }
            }
            if (offerPerKg > localRetail * 1.5) {
                warningsBn.add("অফার খুচরা দামের চেয়ে অস্বাভাবিক বেশি; একক (কেজি/মণ) যাচাই করুন।")
                warningsEn.add("Offer exceeds retail by >50%; verify kg vs maund (40kg) unit.")
            }
        }

        // Route calculation using Haversine distances
        val srcCoord = district?.let { it.lat to it.lon }
        var localNet: Double? = null
        val betterList = mutableListOf<BetterMarketOption>()
        var routeNoteBn = ""
        var routeNoteEn = ""

        if (srcCoord != null && district != null) {
            localNet = localWholesale
            val cands = DISTRICTS.filter { it.nameEn != district.nameEn }.mapNotNull { cand ->
                val km = haversineKm(srcCoord, cand.lat to cand.lon)
                if (km > ROUTE_MAX_KM) return@mapNotNull null
                val f = crop.marketFactorMap[cand.nameEn] ?: 1.0
                val candWhole = round(crop.wholesaleRef * f * 10.0) / 10.0
                val net = round((candWhole - km * TRANSPORT_TK_PER_KG_KM) * 10.0) / 10.0
                val gain = round((net - localNet) * 10.0) / 10.0
                BetterMarketOption(cand.nameEn, cand.nameBn, candWhole, km.roundToInt(), net, gain)
            }.sortedByDescending { it.estNetPrice }

            val minGain = max(1.0, 0.05 * localNet)
            betterList.addAll(cands.filter { it.gainVsLocal >= minGain }.take(3))
            if (betterList.isEmpty()) {
                routeNoteBn = "${district.nameBn} জেলার বাজারেই বেচা ভালো, দূরে গিয়ে পরিবহন খরচ বাদে উল্লেখযোগ্য লাভ নেই।"
                routeNoteEn = "Selling locally in ${district.nameEn} is optimal; hauling elsewhere adds little net gain after transport."
            }
        } else {
            // Show top reference markets across Bangladesh
            val topCands = crop.marketFactorMap.entries
                .sortedByDescending { it.value }
                .take(3)
                .mapNotNull { (distName, factor) ->
                    val dInfo = DISTRICTS.firstOrNull { it.nameEn == distName } ?: return@mapNotNull null
                    val w = round(crop.wholesaleRef * factor * 10.0) / 10.0
                    BetterMarketOption(dInfo.nameEn, dInfo.nameBn, w, 0, w, round((w - localWholesale) * 10.0) / 10.0)
                }
            betterList.addAll(topCands)
            routeNoteBn = "জানান কোন জেলা থেকে বলছেন, পরিবহন খরচ (০.০১ ৳/কেজি/কিমি) বাদে ভালো বাজার দেখাবে।"
            routeNoteEn = "Include your district name to compute net-of-transport market prices (0.01 BDT/kg/km)."
        }

        val ansBn = buildString {
            append("${crop.bn}-র রেফারেন্স ন্যায্য খামার-দাম ≈ ${BnFormat.num(fairPrice, 1, AppLang.BN)} ৳/কেজি ($basisBn)। ")
            append("পাইকারি ≈ ${BnFormat.num(localWholesale, 1, AppLang.BN)} ৳, খুচরা ≈ ${BnFormat.num(localRetail, 1, AppLang.BN)} ৳। ")
            if (offerPerKg != null && lossPerKg != null) {
                append("\nদালালের ${BnFormat.num(offerPerKg, 1, AppLang.BN)} ৳ অফারটি '$verdictBn' — কেজিতে ক্ষতি ${BnFormat.num(lossPerKg, 1, AppLang.BN)} ৳ (${BnFormat.num(qty, AppLang.BN)} কেজিতে ${BnFormat.num(totalLoss ?: 0, AppLang.BN)} ৳)। ")
            }
            if (betterList.isNotEmpty() && district != null) {
                val top = betterList.first()
                append("\nভালো বাজার: ${top.districtBn} (~${BnFormat.num(top.distanceKm, AppLang.BN)} কিমি দূরে, নিট ${BnFormat.num(top.estNetPrice, 1, AppLang.BN)} ৳/কেজি, লাভ +${BnFormat.num(top.gainVsLocal, 1, AppLang.BN)} ৳)।")
            } else if (routeNoteBn.isNotEmpty()) {
                append("\n$routeNoteBn")
            }
        }

        val ansEn = buildString {
            append("${crop.en} reference fair farm-gate ≈ $fairPrice BDT/kg ($basisEn). ")
            append("Wholesale ≈ $localWholesale BDT, Retail ≈ $localRetail BDT. ")
            if (offerPerKg != null && lossPerKg != null) {
                append("\nBuyer offer $offerPerKg BDT/kg is $verdictEn — loss of $lossPerKg BDT/kg ($totalLoss BDT on ${qty.toInt()} kg). ")
            }
            if (betterList.isNotEmpty() && district != null) {
                val top = betterList.first()
                append("\nBest nearby market: ${top.districtEn} (${top.distanceKm} km, net ${top.estNetPrice} BDT/kg, +${top.gainVsLocal} gain).")
            } else if (routeNoteEn.isNotEmpty()) {
                append("\n$routeNoteEn")
            }
        }

        return MarketPriceAnalysis(
            query = q,
            mode = if (district != null) "market" else "price",
            cropKey = crop.key,
            cropBn = crop.bn,
            cropEn = crop.en,
            districtEn = district?.nameEn,
            districtBn = district?.nameBn,
            fairFarmGate = fairPrice,
            wholesalePrice = localWholesale,
            retailPrice = localRetail,
            offerPerKg = offerPerKg,
            quantityKg = qty,
            verdictBn = verdictBn,
            verdictEn = verdictEn,
            lossPerKg = lossPerKg,
            totalLoss = totalLoss,
            middlemanMarginPct = marginPct,
            basisBn = basisBn,
            basisEn = basisEn,
            localNetPrice = localNet,
            betterMarkets = betterList,
            routeNoteBn = routeNoteBn,
            routeNoteEn = routeNoteEn,
            answerBn = ansBn,
            answerEn = ansEn,
            warningsBn = warningsBn,
            warningsEn = warningsEn
        )
    }
}
