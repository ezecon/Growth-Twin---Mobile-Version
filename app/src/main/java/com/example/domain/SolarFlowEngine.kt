package com.example.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class SolarFlowInput(
    val soilMoisturePct: Double = 24.0,
    val targetMoisturePct: Double = 35.0,
    val areaM2: Double = 100.0,
    val rootZoneAvailableMm: Double = 60.0,
    val dailyCropWaterMm: Double = 4.0,
    val forecastRainMm: Double = 8.0,
    val rainProbability: Double = 0.6,
    val solarPowerW: Double = 80.0,
    val pumpPowerW: Double = 60.0,
    val batteryCapacityWh: Double = 240.0,
    val batteryPct: Double = 42.0,
    val batteryReservePct: Double = 25.0,
    val tankWaterL: Double = 500.0,
    val pumpFlowLMin: Double = 5.0,
    val horizonHours: Double = 6.0
)

data class SolarFlowCandidate(
    val action: String, // solar_now | battery_now | wait
    val energySource: String, // solar | battery | none
    val pumpMinutes: Double,
    val waterL: Double,
    val energyWh: Double,
    val unmetWaterMm: Double,
    val score: Double
)

data class SolarFlowResult(
    val engine: String = "model_based_optimizer_v1",
    val status: String = "prototype_not_field_validated",
    val trainedMlModel: Boolean = false,
    val recommendation: String,
    val energySource: String,
    val pumpMinutes: Double,
    val estimatedWaterL: Double,
    val estimatedEnergyWh: Double,
    val waterDemandMm: Double,
    val waterDemandRangeMm: Pair<Double, Double>,
    val estimatedBatteryAfterPct: Double,
    val candidates: List<SolarFlowCandidate>,
    val reasons: List<String>,
    val flags: List<String>,
    val assumptions: List<String>
)

object SolarFlowEngine {
    private fun clamp(v: Double, lo: Double, hi: Double): Double = max(lo, min(hi, v))
    private fun round1(v: Double): Double = round(v * 10.0) / 10.0
    private fun round2(v: Double): Double = round(v * 100.0) / 100.0

    fun recommend(d: SolarFlowInput): SolarFlowResult {
        val moisture = clamp(d.soilMoisturePct, 0.0, 100.0)
        val target = clamp(d.targetMoisturePct, 1.0, 100.0)
        val rainProb = clamp(d.rainProbability, 0.0, 1.0)
        val batteryPct = clamp(d.batteryPct, 0.0, 100.0)
        val reservePct = clamp(d.batteryReservePct, 0.0, 95.0)
        val area = max(0.1, d.areaM2)
        val rootMm = max(1.0, d.rootZoneAvailableMm)
        val dailyNeed = max(0.0, d.dailyCropWaterMm)
        val horizon = clamp(d.horizonHours, 1.0, 24.0)
        val flow = max(0.1, d.pumpFlowLMin)
        val pumpW = max(1.0, d.pumpPowerW)
        val solarW = max(0.0, d.solarPowerW)
        val tankL = max(0.0, d.tankWaterL)

        val deficitMm = max(0.0, (target - moisture) / 100.0 * rootMm)
        val cropNeedMm = dailyNeed * horizon / 24.0
        val expectedRainMm = max(0.0, d.forecastRainMm) * rainProb * 0.75
        val demandMm = if (moisture >= target) 0.0 else max(0.0, max(deficitMm, cropNeedMm) - expectedRainMm)
        val requestedL = demandMm * area
        val tankLimitedL = min(requestedL, tankL)
        val requestedMinutes = tankLimitedL / flow
        val maxMinutes = min(60.0, requestedMinutes)
        val solarFraction = clamp(solarW / pumpW, 0.0, 1.0)
        val solarSupportedMinutes = if (solarFraction >= 0.95) maxMinutes else maxMinutes * solarFraction
        val batteryAvailableWh = max(0.0, d.batteryCapacityWh * (batteryPct - reservePct) / 100.0)
        val batterySupportedMinutes = min(maxMinutes, batteryAvailableWh / pumpW * 60.0)

        val reasons = mutableListOf<String>()
        val flags = mutableListOf<String>()
        if (requestedL > tankL) {
            flags.add("tank_limited")
            reasons.add("Available tank water limits the requested irrigation volume.")
        }
        if (moisture >= target) {
            reasons.add("Soil moisture is at or above the configured target.")
        }
        if (rainProb >= 0.65 && d.forecastRainMm >= 3.0) {
            reasons.add("Meaningful rain is forecast, so expected rainfall reduces the irrigation target.")
        }
        if (batteryPct <= reservePct) {
            flags.add("battery_at_reserve")
            reasons.add("Battery is at or below its protected reserve; battery pumping is not recommended.")
        }

        val rawCandidates = listOf(
            Triple("solar_now", min(maxMinutes, solarSupportedMinutes), "solar" to 0.02),
            Triple("battery_now", min(maxMinutes, batterySupportedMinutes), "battery" to 0.25),
            Triple("wait", 0.0, "none" to 0.0)
        ).map { (action, minutes, sourceCost) ->
            val (energySource, cost) = sourceCost
            val deliveredL = min(tankL, minutes * flow)
            val deliveredMm = deliveredL / area
            val unmetMm = max(0.0, demandMm - deliveredMm)
            val energyWh = minutes / 60.0 * pumpW
            val batteryWh = when (energySource) {
                "battery" -> energyWh
                "solar" -> max(0.0, energyWh * (1.0 - solarFraction))
                else -> 0.0
            }
            var score = unmetMm * 10.0 + energyWh * cost + batteryWh * 0.08
            if (energySource == "battery" && batteryPct <= reservePct) {
                score += 10000.0
            }
            SolarFlowCandidate(
                action = action,
                energySource = energySource,
                pumpMinutes = round1(minutes),
                waterL = round1(deliveredL),
                energyWh = round1(energyWh),
                unmetWaterMm = round2(unmetMm),
                score = round2(score)
            )
        }

        val waitCand = rawCandidates[2]
        val chosen = if (demandMm <= 0.25 || tankL < 0.5) {
            reasons.add("Estimated irrigation demand is negligible or usable tank water is unavailable.")
            waitCand
        } else {
            val best = rawCandidates.minByOrNull { it.score } ?: waitCand
            if (best.pumpMinutes <= 0.0) waitCand else best
        }

        val recommendation = chosen.action
        when (recommendation) {
            "solar_now" -> reasons.add("Solar-first pumping is preferred for the currently estimated conditions.")
            "battery_now" -> reasons.add("Battery pumping is selected only within the configured reserve constraint.")
            else -> reasons.add("Wait and reassess with fresh sensor and weather observations.")
        }

        val demandLow = max(0.0, demandMm * 0.8)
        val demandHigh = demandMm * 1.2 + (if (rainProb > 0.0) 1.0 else 0.0)
        val batteryAfter = if (chosen.energySource == "battery") {
            max(0.0, batteryPct - (chosen.energyWh / max(1.0, d.batteryCapacityWh) * 100.0))
        } else {
            batteryPct
        }

        return SolarFlowResult(
            recommendation = recommendation,
            energySource = chosen.energySource,
            pumpMinutes = chosen.pumpMinutes,
            estimatedWaterL = chosen.waterL,
            estimatedEnergyWh = chosen.energyWh,
            waterDemandMm = round2(demandMm),
            waterDemandRangeMm = round2(demandLow) to round2(demandHigh),
            estimatedBatteryAfterPct = round1(batteryAfter),
            candidates = rawCandidates.sortedBy { it.score },
            reasons = reasons,
            flags = flags,
            assumptions = listOf(
                "Soil-moisture percentage is calibrated to plant-available water; verify this for the installed probe and soil.",
                "Solar power is the current usable power at the pump controller, not a weather-only estimate.",
                "Rain effectiveness is approximated at 75%; calibrate with local rainfall and infiltration observations.",
                "Pump flow and wattage must be measured on the actual hardware before field operation.",
                "This is transparent model-based decision support, not a trained or field-validated AI model."
            )
        )
    }
}
