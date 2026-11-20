package com.example.domain

import java.util.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

data class HarvestRecord(
    val plotId: String,
    val userId: String,
    val cropId: String,
    val soil: String,
    val area: Double,
    val yieldKg: Int,
    val yieldKgPerDecimal: Double,
    val revenue: Int,
    val cost: Int,
    val profit: Int,
    val days: Int,
    val health: Int,
    val startDate: String,
    val outcome: String, // harvested | dead
    val source: String = "simulation"
)

data class MyHarvestSummary(
    val profitPerDecimal: Int,
    val yieldPerDecimal: Double,
    val outcome: String,
    val startDate: String,
    val betterThanShare: Double?
)

data class TrustHistogram(
    val lo: Int,
    val hi: Int,
    val counts: List<Int>
)

data class TrustReport(
    val cropId: String,
    val nFarmers: Int,
    val nPlots: Int,
    val growingNow: Int,
    val failedCount: Int,
    val profitShare: Double,
    val medianProfitPerDecimal: Int,
    val p25: Int,
    val p75: Int,
    val best: Int,
    val worst: Int,
    val medianYieldPerDecimal: Double?,
    val medianDays: Int?,
    val hist: TrustHistogram,
    val tableBadY: Double,
    val tableBaseY: Double,
    val tableGoodY: Double,
    val tableCostPerDec: Double,
    val sources: Map<String, Int>,
    val mine: List<MyHarvestSummary>
)

object TrustAggregator {
    private const val BINS = 8

    private fun ppd(r: HarvestRecord): Double = r.profit / max(r.area, 0.01)

    private fun medianDouble(vals: List<Double>): Double {
        if (vals.isEmpty()) return 0.0
        val sorted = vals.sorted()
        val m = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[m] else (sorted[m - 1] + sorted[m]) / 2.0
    }

    private fun quantile(vals: List<Double>, p: Double): Double {
        if (vals.isEmpty()) return 0.0
        val sorted = vals.sorted()
        val idx = (p * (sorted.size - 1) + 0.5).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[idx]
    }

    /** Seeded anonymous community cohort (clearly marked source='simulation') combined with user's actual harvests/failures. */
    fun buildSeededCohort(): List<HarvestRecord> {
        val rng = Random(20261024L)
        val out = mutableListOf<HarvestRecord>()
        val soils = listOf("doash", "entel", "beley")
        CropCatalog.CROPS.forEach { c ->
            val farmerCount = 14
            for (f in 1..farmerCount) {
                val area = listOf(15.0, 33.0, 50.0, 66.0).random(kotlin.random.Random(rng.nextLong()))
                val dead = f == 7 || (c.id == "tomato" && f == 12)
                val soil = soils[(f + c.id.length) % soils.size]
                val cost = (c.cost * area).roundToInt()
                if (dead) {
                    out.add(
                        HarvestRecord(
                            plotId = "sim_${c.id}_$f",
                            userId = "sim_farmer_$f",
                            cropId = c.id,
                            soil = soil,
                            area = area,
                            yieldKg = 0,
                            yieldKgPerDecimal = 0.0,
                            revenue = 0,
                            cost = cost,
                            profit = -cost,
                            days = (c.durationDays * 0.7).roundToInt(),
                            health = 0,
                            startDate = "2026-01-${(f % 25 + 1).toString().padStart(2, '0')}",
                            outcome = "dead",
                            source = "simulation"
                        )
                    )
                } else {
                    val yFac = 0.72 + rng.nextDouble() * 0.50
                    val pFac = 0.85 + rng.nextDouble() * 0.32
                    val yDec = round(c.y.second * yFac * 10.0) / 10.0
                    val totalKg = (yDec * area).roundToInt()
                    val rev = (totalKg * c.p.second * pFac).roundToInt()
                    out.add(
                        HarvestRecord(
                            plotId = "sim_${c.id}_$f",
                            userId = "sim_farmer_$f",
                            cropId = c.id,
                            soil = soil,
                            area = area,
                            yieldKg = totalKg,
                            yieldKgPerDecimal = yDec,
                            revenue = rev,
                            cost = cost,
                            profit = rev - cost,
                            days = (c.durationDays + (-5..6).random(kotlin.random.Random(rng.nextLong()))),
                            health = (72..98).random(kotlin.random.Random(rng.nextLong())),
                            startDate = "2026-01-${(f % 25 + 1).toString().padStart(2, '0')}",
                            outcome = "harvested",
                            source = "simulation"
                        )
                    )
                }
            }
        }
        return out
    }

    fun aggregate(
        cropId: String,
        currentUserId: String,
        allHarvests: List<HarvestRecord>,
        growingNowCount: Int
    ): TrustReport {
        val c = CropCatalog.BY_ID[cropId] ?: CropCatalog.CROPS.first()
        val rows = allHarvests.filter { it.cropId == cropId }
        val mine = rows.filter { it.userId == currentUserId }
        val others = rows.filter { it.userId != currentUserId }
        val nFarmers = rows.map { it.userId }.distinct().size
        val sources = rows.groupingBy { it.source }.eachCount()

        val allP = rows.map { ppd(it) }
        val ok = rows.filter { it.outcome == "harvested" }
        val lo = allP.minOrNull() ?: 0.0
        val hi = allP.maxOrNull() ?: 0.0
        val width = if (hi > lo) (hi - lo) / BINS else 1.0
        val counts = IntArray(BINS)
        for (v in allP) {
            val idx = min(BINS - 1, ((v - lo) / width).toInt().coerceAtLeast(0))
            counts[idx]++
        }

        val otherP = others.map { ppd(it) }
        val mineOut = mine.map { r ->
            val rp = ppd(r)
            val better = if (otherP.isNotEmpty()) {
                round(otherP.count { it < rp }.toDouble() / otherP.size * 1000.0) / 1000.0
            } else null
            MyHarvestSummary(
                profitPerDecimal = rp.roundToInt(),
                yieldPerDecimal = round(r.yieldKgPerDecimal * 10.0) / 10.0,
                outcome = r.outcome,
                startDate = r.startDate,
                betterThanShare = better
            )
        }

        return TrustReport(
            cropId = cropId,
            nFarmers = nFarmers,
            nPlots = rows.size,
            growingNow = growingNowCount,
            failedCount = rows.count { it.outcome == "dead" },
            profitShare = if (allP.isNotEmpty()) round(allP.count { it > 0.0 }.toDouble() / allP.size * 1000.0) / 1000.0 else 0.0,
            medianProfitPerDecimal = medianDouble(allP).roundToInt(),
            p25 = quantile(allP, 0.25).roundToInt(),
            p75 = quantile(allP, 0.75).roundToInt(),
            best = hi.roundToInt(),
            worst = lo.roundToInt(),
            medianYieldPerDecimal = if (ok.isNotEmpty()) round(medianDouble(ok.map { it.yieldKgPerDecimal }) * 10.0) / 10.0 else null,
            medianDays = if (ok.isNotEmpty()) medianDouble(ok.map { it.days.toDouble() }).roundToInt() else null,
            hist = TrustHistogram(lo.roundToInt(), hi.roundToInt(), counts.toList()),
            tableBadY = c.y.first,
            tableBaseY = c.y.second,
            tableGoodY = c.y.third,
            tableCostPerDec = c.cost,
            sources = sources,
            mine = mineOut
        )
    }
}
