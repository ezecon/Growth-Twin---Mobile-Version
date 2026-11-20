package com.example.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class MoistureCalibration(
    val deviceId: String = "SIM-ESP32-01",
    val adcAtWiltingPoint: Int = 3000,
    val adcAtFieldCapacity: Int = 1500,
    val adcMin: Int = 0,
    val adcMax: Int = 4095
) {
    fun validate(): String? {
        if (adcMin < 0 || adcMin >= adcMax || adcMax > 65535) {
            return "ADC bounds must satisfy 0 <= adcMin < adcMax <= 65535"
        }
        if (adcAtWiltingPoint !in adcMin..adcMax) {
            return "Wilting-point ADC is outside configured range"
        }
        if (adcAtFieldCapacity !in adcMin..adcMax) {
            return "Field-capacity ADC is outside configured range"
        }
        if (abs(adcAtFieldCapacity - adcAtWiltingPoint) < 20) {
            return "Wilting-point and field-capacity ADC values must differ by at least 20 counts"
        }
        return null
    }

    fun convert(rawAdc: Int): Pair<Double, String> {
        val err = validate()
        require(err == null) { err ?: "Invalid calibration" }
        require(rawAdc in adcMin..adcMax) { "raw_adc must be between $adcMin and $adcMax" }
        val low = min(adcAtWiltingPoint, adcAtFieldCapacity)
        val high = max(adcAtWiltingPoint, adcAtFieldCapacity)
        val inSpan = rawAdc in low..high
        val frac = (rawAdc - adcAtWiltingPoint).toDouble() / (adcAtFieldCapacity - adcAtWiltingPoint).toDouble()
        val pct = round(max(0.0, min(100.0, frac * 100.0)) * 100.0) / 100.0
        return pct to (if (inSpan) "valid" else "suspect")
    }
}

data class SensorHealthAssessment(
    val quality: String, // valid | suspect
    val reasons: List<String>,
    val consecutiveIdentical: Int,
    val adcDelta: Int?
)

data class StateEstimate(
    val predictedValue: Double,
    val measuredValue: Double?,
    val residual: Double?,
    val estimatedValue: Double,
    val alpha: Double,
    val measurementUsed: Boolean,
    val quality: String,
    val reason: String,
    val estimatorVersion: String = "reality-gap-v1"
)

data class SentinelScenario(
    val id: String, // fresh | stale | suspect
    val labelBn: String,
    val labelEn: String,
    val quality: String,
    val rawAdc: Int,
    val calibratedPct: Double,
    val ageMinutes: Int,
    val consecutiveIdentical: Int,
    val summaryBn: String,
    val summaryEn: String
)

object SentinelEngine {
    const val ALPHA_DEFAULT = 0.65

    fun assessReading(
        rawAdc: Int,
        prevAdc: Int?,
        prevIdentical: Int,
        calibrationQuality: String,
        flatlineLimit: Int = 5,
        jumpLimit: Int = 1500
    ): SensorHealthAssessment {
        val reasons = mutableListOf<String>()
        val delta = prevAdc?.let { abs(rawAdc - it) }
        val identical = if (prevAdc != null && prevAdc == rawAdc) prevIdentical + 1 else 1
        if (calibrationQuality != "valid") reasons.add("outside_calibrated_span")
        if (identical >= flatlineLimit) reasons.add("possible_sensor_flatline")
        if (delta != null && delta > jumpLimit) reasons.add("implausible_adc_jump")
        return SensorHealthAssessment(
            quality = if (reasons.isNotEmpty()) "suspect" else "valid",
            reasons = reasons,
            consecutiveIdentical = identical,
            adcDelta = delta
        )
    }

    fun correctState(
        predictedValue: Double,
        measuredValue: Double?,
        alpha: Double = ALPHA_DEFAULT,
        minimum: Double = 0.0,
        maximum: Double = 160.0,
        quality: String = "valid"
    ): StateEstimate {
        val residual = measuredValue?.let { round((it - predictedValue) * 100.0) / 100.0 }
        val usable = measuredValue != null && quality == "valid"
        val rawEst = if (usable) {
            predictedValue + alpha * (measuredValue!! - predictedValue)
        } else {
            predictedValue
        }
        val clampedEst = round(min(maximum, max(minimum, rawEst)) * 100.0) / 100.0
        return StateEstimate(
            predictedValue = round(predictedValue * 100.0) / 100.0,
            measuredValue = measuredValue?.let { round(it * 100.0) / 100.0 },
            residual = residual,
            estimatedValue = clampedEst,
            alpha = alpha,
            measurementUsed = usable,
            quality = if (measuredValue != null) quality else "missing",
            reason = if (usable) "valid_measurement_blended_with_prediction" else "prediction_retained_without_valid_measurement"
        )
    }

    fun demoScenarios(): List<SentinelScenario> = listOf(
        SentinelScenario(
            id = "fresh",
            labelBn = "তাজা (Fresh)",
            labelEn = "Fresh",
            quality = "valid",
            rawAdc = 2280,
            calibratedPct = 48.0,
            ageMinutes = 2,
            consecutiveIdentical = 1,
            summaryBn = "সেন্সর ২ মিনিট আগে সুস্থ রিডিং দিয়েছে — টুইনের অবস্থা আপডেট করার উপযুক্ত।",
            summaryEn = "Reading is 2m old inside calibrated span — blended with twin prediction (α=0.65)."
        ),
        SentinelScenario(
            id = "stale",
            labelBn = "পুরনো (Stale)",
            labelEn = "Stale",
            quality = "stale",
            rawAdc = 2490,
            calibratedPct = 34.0,
            ageMinutes = 360,
            consecutiveIdentical = 1,
            summaryBn = "রিডিং ৬ ঘণ্টা পুরনো (Stale) — সেন্টিনেল নীরবে ব্যবহার না করে মডেল পূর্বানুমান ধরে রাখে।",
            summaryEn = "Reading is 6h old (stale) — Reality Gap rejects update and retains model prediction."
        ),
        SentinelScenario(
            id = "suspect",
            labelBn = "সন্দেহজনক (Suspect)",
            labelEn = "Suspect",
            quality = "suspect",
            rawAdc = 4080,
            calibratedPct = 0.0,
            ageMinutes = 1,
            consecutiveIdentical = 6,
            summaryBn = "সম্ভাব্য তার খোলা বা ফ্ল্যাটলাইন (৬ বার একই মান / সীমার বাইরে) — মানটি বাদ দেওয়া হয়েছে।",
            summaryEn = "Out of calibrated span & flatlined (6 identical packets) — flagged suspect and ignored."
        )
    )
}
