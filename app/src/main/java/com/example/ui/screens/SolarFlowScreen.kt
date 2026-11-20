package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.LiveWeatherSnapshot
import com.example.domain.SolarFlowInput
import com.example.domain.SolarFlowResult
import com.example.ui.components.GTCard
import com.example.ui.components.MetricGaugeBar
import com.example.ui.components.NoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr

@Composable
fun SolarFlowScreen(
    lang: AppLang,
    input: SolarFlowInput,
    result: SolarFlowResult,
    liveWeather: LiveWeatherSnapshot? = null,
    onApplyLiveRain: () -> Unit = {},
    onUpdateInput: (SolarFlowInput) -> Unit,
    onResetDemo: () -> Unit
) {
    val c = GTTheme.colors

    var moisture by remember(input) { mutableStateOf(input.soilMoisturePct.toString()) }
    var target by remember(input) { mutableStateOf(input.targetMoisturePct.toString()) }
    var area by remember(input) { mutableStateOf(input.areaM2.toString()) }
    var rootMm by remember(input) { mutableStateOf(input.rootZoneAvailableMm.toString()) }
    var dailyWater by remember(input) { mutableStateOf(input.dailyCropWaterMm.toString()) }
    var rainMm by remember(input) { mutableStateOf(input.forecastRainMm.toString()) }
    var rainProb by remember(input) { mutableStateOf(input.rainProbability.toString()) }
    var solarW by remember(input) { mutableStateOf(input.solarPowerW.toString()) }
    var pumpW by remember(input) { mutableStateOf(input.pumpPowerW.toString()) }
    var batCap by remember(input) { mutableStateOf(input.batteryCapacityWh.toString()) }
    var batPct by remember(input) { mutableStateOf(input.batteryPct.toString()) }
    var batRes by remember(input) { mutableStateOf(input.batteryReservePct.toString()) }
    var tankL by remember(input) { mutableStateOf(input.tankWaterL.toString()) }
    var flowRate by remember(input) { mutableStateOf(input.pumpFlowLMin.toString()) }
    var horizonH by remember(input) { mutableStateOf(input.horizonHours.toString()) }

    fun submitCurrent() {
        onUpdateInput(
            SolarFlowInput(
                soilMoisturePct = moisture.toDoubleOrNull() ?: input.soilMoisturePct,
                targetMoisturePct = target.toDoubleOrNull() ?: input.targetMoisturePct,
                areaM2 = area.toDoubleOrNull() ?: input.areaM2,
                rootZoneAvailableMm = rootMm.toDoubleOrNull() ?: input.rootZoneAvailableMm,
                dailyCropWaterMm = dailyWater.toDoubleOrNull() ?: input.dailyCropWaterMm,
                forecastRainMm = rainMm.toDoubleOrNull() ?: input.forecastRainMm,
                rainProbability = rainProb.toDoubleOrNull() ?: input.rainProbability,
                solarPowerW = solarW.toDoubleOrNull() ?: input.solarPowerW,
                pumpPowerW = pumpW.toDoubleOrNull() ?: input.pumpPowerW,
                batteryCapacityWh = batCap.toDoubleOrNull() ?: input.batteryCapacityWh,
                batteryPct = batPct.toDoubleOrNull() ?: input.batteryPct,
                batteryReservePct = batRes.toDoubleOrNull() ?: input.batteryReservePct,
                tankWaterL = tankL.toDoubleOrNull() ?: input.tankWaterL,
                pumpFlowLMin = flowRate.toDoubleOrNull() ?: input.pumpFlowLMin,
                horizonHours = horizonH.toDoubleOrNull() ?: input.horizonHours
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            kicker = "SOLARFLOW AI · ADVISORY PROTOTYPE",
            title = tr(lang, "সোলারফ্লো সৌর-পাম্প সিদ্ধান্ত", "SolarFlow Irrigation Optimizer"),
            subtitle = tr(
                lang,
                "মাটির পানির ঘাটতি, বৃষ্টি ও সৌরশক্তি/ব্যাটারি ব্যালেন্স মিলিয়ে সোলার বনাম ব্যাটারি বনাম অপেক্ষা যাচাই।",
                "Transparent water-balance and solar/battery scoring engine for off-grid irrigation."
            )
        )

        NoticeBanner(
            title = tr(lang, "প্রোটোটাইপ ইঞ্জিন (মডেল-ভিত্তিক)", "Model-Based Prototype"),
            body = tr(
                lang,
                "এটি স্বচ্ছ গাণিতিক অপ্টিমাইজার (প্রশিক্ষিত ML মডেল নয়)। মাঠপর্যায়ে হার্ডওয়্যার ওয়াটেজ ও প্রবাহ যাচাই করুন।",
                "Status: prototype_not_field_validated. Verify pump wattage, flow rate, and soil probe calibration on site."
            ),
            tone = "warn"
        )

        // Recommendation Summary Card
        val recTitle = when (result.recommendation) {
            "solar_now" -> tr(lang, "এখন সৌরশক্তিতে পাম্প চালান (Solar Now)", "Run Pump on Solar Now")
            "battery_now" -> tr(lang, "ব্যাটারি দিয়ে পাম্প চালান (Battery Now)", "Run Pump on Battery Now")
            else -> tr(lang, "অপেক্ষা করুন (Wait)", "Wait & Reassess")
        }
        GTCard(accentBorder = c.lime.copy(alpha = 0.6f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lang, "প্রস্তাবিত সিদ্ধান্ত", "RECOMMENDATION"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                StatusBadge(
                    text = result.recommendation.uppercase(),
                    tone = if (result.recommendation == "wait") "caution" else "go"
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(recTitle, style = MaterialTheme.typography.headlineMedium, color = c.ink)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCol(tr(lang, "সময়", "Runtime"), "${BnFormat.num(result.pumpMinutes, 1, lang)} m", Modifier.weight(1f))
                MetricCol(tr(lang, "পানি", "Water"), "${BnFormat.num(result.estimatedWaterL, 1, lang)} L", Modifier.weight(1f))
                MetricCol(tr(lang, "শক্তি", "Energy"), "${BnFormat.num(result.estimatedEnergyWh, 1, lang)} Wh", Modifier.weight(1f))
                MetricCol(tr(lang, "পরবর্তী ব্যাটারি", "Battery After"), "${BnFormat.num(result.estimatedBatteryAfterPct, 1, lang)}%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            val targetNeedL = (result.waterDemandMm * input.areaM2).coerceAtLeast(1.0)
            MetricGaugeBar(
                label = tr(lang, "প্রস্তাবিত পানি বনাম চাহিদা", "Recommended Water vs Demand"),
                valueText = "${BnFormat.num(result.estimatedWaterL, 0, lang)} / ${BnFormat.num(targetNeedL, 0, lang)} L",
                fraction = (result.estimatedWaterL / targetNeedL).toFloat(),
                color = if (result.recommendation == "wait") c.warn else c.mint
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tr(
                    lang,
                    "পানির চাহিদা: ${BnFormat.num(result.waterDemandMm, 2, lang)} মিমি (ব্যাপ্তি ${BnFormat.num(result.waterDemandRangeMm.first, 1, lang)}–${BnFormat.num(result.waterDemandRangeMm.second, 1, lang)} মিমি)",
                    "Water demand: ${result.waterDemandMm} mm (uncertainty band ${result.waterDemandRangeMm.first}–${result.waterDemandRangeMm.second} mm)"
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                color = c.mint
            )
            if (result.flags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.flags.forEach { f -> StatusBadge(text = f, tone = "warn") }
                }
            }
            Spacer(Modifier.height(8.dp))
            result.reasons.forEach { r ->
                Text("• $r", style = MaterialTheme.typography.bodyMedium, color = c.ink)
            }
        }

        // Candidate Rankings
        GTCard {
            Text(tr(lang, "সকল বিকল্পের স্কোর তুলনা", "RANKED CANDIDATE ACTIONS"), style = MaterialTheme.typography.labelMedium, color = c.lime)
            Spacer(Modifier.height(8.dp))
            result.candidates.forEach { cand ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${cand.action} (${cand.energySource})", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Text(
                            "${cand.pumpMinutes}m • ${cand.waterL} L • ${cand.energyWh} Wh • unmet ${cand.unmetWaterMm} mm",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                            color = c.mute
                        )
                    }
                    Text(
                        "Score ${cand.score}",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont),
                        color = if (cand.action == result.recommendation) c.lime else c.mute
                    )
                }
                HorizontalDivider(color = c.line)
            }
        }

        // Inputs Form (all 15 inputs, grouped & localized)
        GTCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lang, "১. জমি ও মাটির অবস্থা", "1. FIELD & SOIL PARAMETERS"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                if (liveWeather?.forecast?.isNotEmpty() == true) {
                    val today = liveWeather.forecast.first()
                    OutlinedButton(
                        onClick = onApplyLiveRain,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("solar_apply_live_rain")
                    ) {
                        Text(tr(lang, "লাইভ বৃষ্টি (${today.rainMm}mm)", "Use Live Rain (${today.rainMm}mm)"))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            NumRow(
                tr(lang, "মাটির আর্দ্রতা (%)", "Soil Moisture (%)"), moisture, { moisture = it },
                tr(lang, "লক্ষ্য আর্দ্রতা (%)", "Target Moisture (%)"), target, { target = it }
            )
            NumRow(
                tr(lang, "জমির আয়তন (বর্গমি.)", "Area (m²)"), area, { area = it },
                tr(lang, "শিকড় অঞ্চলের পানি (মিমি)", "Root Zone Water (mm)"), rootMm, { rootMm = it }
            )
            NumRow(
                tr(lang, "দৈনিক ফসলের পানি (মিমি)", "Daily Crop Water (mm)"), dailyWater, { dailyWater = it },
                tr(lang, "পূর্বাভাস বৃষ্টি (মিমি)", "Forecast Rain (mm)"), rainMm, { rainMm = it }
            )
            NumRow(
                tr(lang, "বৃষ্টির সম্ভাবনা (০–১)", "Rain Prob (0–1)"), rainProb, { rainProb = it },
                tr(lang, "পরিকল্পনা সময় (ঘণ্টা)", "Horizon (hours)"), horizonH, { horizonH = it }
            )

            HorizontalDivider(color = c.line, modifier = Modifier.padding(vertical = 8.dp))
            Text(tr(lang, "২. সৌরশক্তি, ব্যাটারি ও ট্যাংক", "2. SOLAR, BATTERY & TANK HARDWARE"), style = MaterialTheme.typography.labelMedium, color = c.mint)
            Spacer(Modifier.height(8.dp))

            NumRow(
                tr(lang, "সোলার ক্ষমতা (ওয়াট)", "Solar Power (W)"), solarW, { solarW = it },
                tr(lang, "পাম্প ক্ষমতা (ওয়াট)", "Pump Power (W)"), pumpW, { pumpW = it }
            )
            NumRow(
                tr(lang, "ব্যাটারি ধারণ (Wh)", "Battery Cap (Wh)"), batCap, { batCap = it },
                tr(lang, "বর্তমান চার্জ (%)", "Battery SoC (%)"), batPct, { batPct = it }
            )
            NumRow(
                tr(lang, "রিজার্ভ সীমা (%)", "Battery Reserve (%)"), batRes, { batRes = it },
                tr(lang, "ট্যাংকের পানি (লি.)", "Tank Water (L)"), tankL, { tankL = it }
            )
            OutlinedTextField(
                value = flowRate, onValueChange = { flowRate = it },
                label = { Text(tr(lang, "পাম্প প্রবাহ (লি./মিনিট)", "Pump Flow (L/min)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { submitCurrent() },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("solar_compute_button")
                ) {
                    Text(tr(lang, "হিসাব আপডেট করুন", "Compute Decision"))
                }
                OutlinedButton(
                    onClick = onResetDemo,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("solar_reset_button")
                ) {
                    Text(tr(lang, "ডেমো রিসেট", "Reset Demo"))
                }
            }
        }

        GTCard {
            Text(tr(lang, "স্বচ্ছ অনুমানসমূহ", "MODEL ASSUMPTIONS"), style = MaterialTheme.typography.labelMedium, color = c.mute)
            Spacer(Modifier.height(6.dp))
            result.assumptions.forEach { a ->
                Text("• $a", style = MaterialTheme.typography.bodyMedium, color = c.mute)
            }
        }
    }
}

@Composable
private fun MetricCol(label: String, value: String, modifier: Modifier = Modifier) {
    val c = GTTheme.colors
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.mute)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont), color = c.ink)
    }
}

@Composable
private fun NumRow(
    l1: String, v1: String, onV1: (String) -> Unit,
    l2: String, v2: String, onV2: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        OutlinedTextField(
            value = v1, onValueChange = onV1, label = { Text(l1) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = v2, onValueChange = onV2, label = { Text(l2) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}
