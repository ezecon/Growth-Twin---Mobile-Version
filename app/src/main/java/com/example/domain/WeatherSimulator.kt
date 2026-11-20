package com.example.domain

import java.time.LocalDate
import java.time.YearMonth
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

data class DayWeather(
    val date: String,
    val tmax: Double,
    val tmin: Double,
    val tmean: Double,
    val rainMm: Double,
    val rh: Int,
    val et0: Double,
    val storm: Boolean,
    val daylightH: Double,
    val sunH: Double
)

data class WeatherForecastDay(
    val date: String,
    val leadDays: Int,
    val rainMm: Double,
    val prob: Int,
    val tmax: Double,
    val normalMm: Double,
    val vsNormal: String // more | less | normal
)

object WeatherSimulator {
    private val TMEAN = doubleArrayOf(19.0, 22.0, 26.0, 28.0, 29.0, 29.0, 29.0, 29.0, 29.0, 28.0, 24.0, 20.0)
    private val RAIN_MM = doubleArrayOf(8.0, 25.0, 60.0, 130.0, 270.0, 500.0, 560.0, 400.0, 300.0, 160.0, 30.0, 8.0)
    private val WET_MEAN = doubleArrayOf(6.0, 8.0, 10.0, 14.0, 20.0, 24.0, 24.0, 22.0, 20.0, 18.0, 10.0, 6.0)
    private val RH = doubleArrayOf(68.0, 62.0, 60.0, 68.0, 76.0, 84.0, 86.0, 86.0, 84.0, 80.0, 72.0, 70.0)

    private fun clamp(x: Double, lo: Double, hi: Double): Double = max(lo, min(hi, x))

    private fun seededRandom(key: String): Random = Random(key.hashCode().toLong())

    private fun expovariate(rng: Random, mean: Double): Double {
        val u = 1.0 - rng.nextDouble().coerceIn(1e-9, 0.999999999)
        return -ln(u) * mean
    }

    private fun interp(table: DoubleArray, d: LocalDate): Double {
        val dim = YearMonth.of(d.year, d.monthValue).lengthOfMonth().toDouble()
        val pos = d.monthValue - 1 + (d.dayOfMonth - 0.5) / dim - 0.5
        val fl = floor(pos).toInt()
        val i = ((fl % 12) + 12) % 12
        val f = pos - fl
        return table[i] * (1.0 - f) + table[(i + 1) % 12] * f
    }

    fun normalRain(d: LocalDate): Double {
        val dim = YearMonth.of(d.year, d.monthValue).lengthOfMonth()
        return RAIN_MM[d.monthValue - 1] / dim
    }

    private fun round1(x: Double): Double = round(x * 10.0) / 10.0
    private fun round2(x: Double): Double = round(x * 100.0) / 100.0

    fun dayWeather(seed: Int, d: LocalDate): DayWeather {
        val m = d.monthValue - 1
        val dim = YearMonth.of(d.year, d.monthValue).lengthOfMonth()
        val p = clamp(RAIN_MM[m] / (dim * WET_MEAN[m]), 0.03, 0.9)
        val q = clamp((p - 0.1) / 0.6, 0.0, 1.0)
        val doy = d.dayOfYear
        val block = doy / 4
        val blockWet = seededRandom("$seed|blk|${d.year}|$block").nextDouble() < q
        val r = seededRandom("$seed|$d")
        var wet = r.nextDouble() < (if (blockWet) 0.7 else 0.1)
        var rain = 0.0
        var storm = false
        if (wet) {
            rain = min(120.0, max(0.5, expovariate(r, WET_MEAN[m])))
        }
        if (m in 3..10 && seededRandom("$seed|storm|$d").nextDouble() < 0.012) {
            rain = 70.0 + r.nextDouble() * 70.0
            storm = true
            wet = true
        }
        val tmean = interp(TMEAN, d) + r.nextGaussian() * 1.5 - (if (wet) 1.5 else 0.0)
        val half = if (wet) 2.2 else 4.2
        val tmax = tmean + half
        val tmin = tmean - half
        val rh = clamp(interp(RH, d) + (if (wet) 8.0 else -5.0) + r.nextGaussian() * 4.0, 35.0, 98.0)
        val daylight = 12.1 + 1.4 * sin(2.0 * PI * (doy - 80) / 365.0)
        val cloud = clamp(0.15 + (rh - 60.0) / 100.0 + (if (wet) 0.4 else 0.0), 0.05, 0.95)
        val sunH = daylight * (1.0 - 0.85 * cloud)
        val ra = 14.2 + 2.3 * cos(2.0 * PI * (doy - 172) / 365.0)
        val et0 = 0.0023 * (tmean + 17.8) * sqrt(max(tmax - tmin, 2.0)) * ra
        return DayWeather(
            date = d.toString(),
            tmax = round1(tmax),
            tmin = round1(tmin),
            tmean = round1(tmean),
            rainMm = round1(rain),
            rh = round(rh).toInt(),
            et0 = round2(et0),
            storm = storm,
            daylightH = round1(daylight),
            sunH = round1(sunH)
        )
    }

    fun forecast(seed: Int, fromDate: LocalDate, n: Int = 5): List<WeatherForecastDay> {
        val out = mutableListOf<WeatherForecastDay>()
        for (k in 0 until n) {
            val d = fromDate.plusDays(k.toLong())
            val wx = dayWeather(seed, d)
            val actual = wx.rainMm
            val r = seededRandom("$seed|fc|$d|$k")
            val sigma = 0.25 + 0.1 * k
            val rain: Double
            val prob: Double
            if (actual > 0.0) {
                rain = actual * exp(r.nextGaussian() * sigma)
                prob = clamp(62.0 + min(25.0, rain * 1.5) - 4.0 * k + r.nextGaussian() * 4.0, 20.0, 97.0)
            } else if (r.nextDouble() < 0.08 + 0.03 * k) {
                rain = 2.0 + r.nextDouble() * 6.0
                prob = clamp(35.0 + r.nextGaussian() * 6.0, 15.0, 55.0)
            } else {
                rain = 0.0
                prob = clamp(8.0 + 3.0 * k + r.nextGaussian() * 3.0, 2.0, 30.0)
            }
            val normal = normalRain(d)
            val rel = when {
                rain >= max(10.0, 1.3 * normal) -> "more"
                rain < max(1.0, 0.3 * normal) -> "less"
                else -> "normal"
            }
            val tmaxF = wx.tmax + r.nextGaussian() * (1.0 + 0.4 * k)
            out.add(
                WeatherForecastDay(
                    date = d.toString(),
                    leadDays = k,
                    rainMm = round1(rain),
                    prob = round(prob).toInt(),
                    tmax = round1(tmaxF),
                    normalMm = round1(normal),
                    vsNormal = rel
                )
            )
        }
        return out
    }
}
