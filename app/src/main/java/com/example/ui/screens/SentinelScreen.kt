package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import com.example.data.TelemetryReadingEntity
import com.example.domain.MoistureCalibration
import com.example.domain.PlotState
import com.example.domain.SentinelEngine
import com.example.ui.components.GTCard
import com.example.ui.components.NoticeBanner
import com.example.ui.components.RealityGapHistoryChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentinelScreen(
    lang: AppLang,
    plot: PlotState?,
    telemetry: List<TelemetryReadingEntity>,
    selectedScenarioId: String,
    checklistDone: Set<String>,
    onSelectScenario: (String) -> Unit,
    onToggleChecklist: (String) -> Unit,
    onResetChecklist: () -> Unit,
    onGenerateSimulation: () -> Unit,
    onSaveCalibration: (String, Int, Int) -> Unit,
    onIngestPacket: (String, Int, Double?, Double?) -> Unit
) {
    val c = GTTheme.colors
    var mode by remember { mutableStateOf("sim") } // sim | real
    val scenarios = remember { SentinelEngine.demoScenarios() }
    val activeScenario = scenarios.firstOrNull { it.id == selectedScenarioId } ?: scenarios.first()

    val existingCal = plot?.sensorConfig ?: MoistureCalibration()
    var deviceIdText by remember(existingCal) { mutableStateOf(existingCal.deviceId) }
    var wpAdcText by remember(existingCal) { mutableStateOf(existingCal.adcAtWiltingPoint.toString()) }
    var fcAdcText by remember(existingCal) { mutableStateOf(existingCal.adcAtFieldCapacity.toString()) }
    var testAdcText by remember { mutableStateOf("2250") }
    var testTempText by remember { mutableStateOf("28.4") }

    val checklistItems = remember {
        listOf(
            "probe_calib" to ("মাটির ধরন অনুযায়ী শুষ্ক ও ভেজা ADC মাপা হয়েছে" to "Measured wilting-point & field-capacity ADC in target soil"),
            "token_saved" to ("ডিভাইস টোকেন ও আইডি নিরাপদে সংরক্ষণ করা হয়েছে" to "Provisioned device ID & calibration endpoints"),
            "flatline_check" to ("টানা এক মান এলে (flatline) সন্দেহজনক হিসেবে চিহ্নিত হয়" to "Verified flatline/out-of-span packets flag as suspect"),
            "alpha_bounds" to ("সংশোধিত মান ০–১৬০% সীমার মধ্যে থাকে" to "Verified Reality Gap state stays bounded within 0–160%"),
            "rain_hold" to ("বৃষ্টির পূর্বাভাসে সেচ বিরতি পরীক্ষা করা হয়েছে" to "Verified rain forecast overrides unnecessary pumping"),
            "dry_safety" to ("শুকনো অবস্থায় নিরাপত্তা সীমা যাচাই সম্পন্ন" to "Confirmed fail-safe shutoff under field test conditions")
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
            kicker = "SENTINEL · TELEMETRY & REALITY GAP",
            title = tr(lang, "সেন্টিনেল সেন্সর মনিটর", "Trust-Aware Sensor Monitor"),
            subtitle = tr(
                lang,
                "সিমুলেশন ও বাস্তব সেন্সর আলাদা রাখুন; সন্দেহজনক বা পুরনো রিডিং মডেলকে নষ্ট করতে পারে না।",
                "Compare field measurements with the twin model. Stale or suspect packets never silently corrupt twin state."
            )
        )

        // Source Toggle: Simulation vs Real Sensor Telemetry
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == "sim",
                onClick = { mode = "sim" },
                label = { Text(tr(lang, "সিমুলেশন ডেমো", "Simulation Demo")) },
                modifier = Modifier.testTag("sentinel_mode_sim")
            )
            FilterChip(
                selected = mode == "real",
                onClick = { mode = "real" },
                label = { Text(tr(lang, "রিয়েলিটি গ্যাপ ও টেলিমেট্রি", "Calibration & Telemetry")) },
                modifier = Modifier.testTag("sentinel_mode_real")
            )
        }

        if (mode == "sim") {
            NoticeBanner(
                title = tr(lang, "ডেমো মোড", "Demo Mode"),
                body = tr(
                    lang,
                    "নিচের Fresh, Stale ও Suspect উদাহরণগুলো সিমুলেটেড। এগুলো কোনো আসল সেন্সর সংযোগ বা পাম্প নিয়ন্ত্রণ নির্দেশ করে না।",
                    "Fresh, Stale, and Suspect scenarios below are simulated examples and do not control hardware."
                ),
                tone = "warn"
            )

            // Scenario selector
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                scenarios.forEach { sc ->
                    FilterChip(
                        selected = sc.id == activeScenario.id,
                        onClick = { onSelectScenario(sc.id) },
                        label = { Text(tr(lang, sc.labelBn, sc.labelEn)) },
                        modifier = Modifier.testTag("sentinel_sc_${sc.id}")
                    )
                }
            }

            val predictedPct = plot?.awPct() ?: 40.0
            val est = SentinelEngine.correctState(predictedPct, activeScenario.calibratedPct, quality = activeScenario.quality)

            GTCard(accentBorder = c.lineBright) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr(lang, activeScenario.labelBn, activeScenario.labelEn), style = MaterialTheme.typography.titleLarge, color = c.ink)
                    StatusBadge(text = activeScenario.quality.uppercase(), tone = activeScenario.quality)
                }
                Spacer(Modifier.height(6.dp))
                Text(tr(lang, activeScenario.summaryBn, activeScenario.summaryEn), style = MaterialTheme.typography.bodyMedium, color = c.mute)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Raw ADC", style = MaterialTheme.typography.labelSmall, color = c.mute)
                        Text("${activeScenario.rawAdc}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont), color = c.ink)
                    }
                    Column {
                        Text(tr(lang, "মাপা %", "Measured"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                        Text("${activeScenario.calibratedPct}%", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont), color = c.lime)
                    }
                    Column {
                        Text(tr(lang, "মডেল পূর্বানুমান", "Predicted"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                        Text("${BnFormat.num(est.predictedValue, 1, lang)}%", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont), color = c.blue)
                    }
                    Column {
                        Text(tr(lang, "সংশোধিত", "Corrected"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                        Text("${BnFormat.num(est.estimatedValue, 1, lang)}%", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont), color = c.mint)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    tr(
                        lang,
                        "মান ব্যবহৃত হয়েছে? ${if (est.measurementUsed) "হ্যাঁ (ব্লেন্ড α=0.65)" else "না (মডেল পূর্বানুমান অপরিবর্তিত)"}",
                        "Measurement accepted? ${if (est.measurementUsed) "Yes (blended α=0.65)" else "No (prediction retained)"}"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (est.measurementUsed) c.mint else c.bad
                )
            }
        } else {
            NoticeBanner(
                title = tr(lang, "টেলিমেট্রি ও ক্যালিব্রেশন মোড", "Calibration & Reality Gap Telemetry"),
                body = tr(
                    lang,
                    "টু-পয়েন্ট ADC ক্যালিব্রেশন সেট করুন এবং রিয়েলিটি গ্যাপ ইঞ্জিনে সিমুলেটেড বা টেস্ট প্যাকেট পাঠিয়ে যাচাই করুন।",
                    "Configure two-point probe ADC calibration and inspect Reality Gap residuals across readings."
                ),
                tone = "info"
            )

            // Generate demo telemetry + packet tester
            if (plot != null) {
                GTCard {
                    Text(tr(lang, "১. প্রোব ক্যালিব্রেশন (Two-Point ADC)", "1. PROBE CALIBRATION"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deviceIdText, onValueChange = { deviceIdText = it },
                        label = { Text(tr(lang, "ডিভাইস আইডি", "Device ID")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sentinel_device_id")
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = wpAdcText, onValueChange = { wpAdcText = it },
                            label = { Text(tr(lang, "শুকনো (Wilting ADC)", "Wilting Point ADC")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("sentinel_wp_adc")
                        )
                        OutlinedTextField(
                            value = fcAdcText, onValueChange = { fcAdcText = it },
                            label = { Text(tr(lang, "ভেজা (Field Cap ADC)", "Field Capacity ADC")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("sentinel_fc_adc")
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onSaveCalibration(
                                deviceIdText,
                                wpAdcText.toIntOrNull() ?: 3000,
                                fcAdcText.toIntOrNull() ?: 1500
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("sentinel_save_cal")
                    ) {
                        Text(tr(lang, "ক্যালিব্রেশন সংরক্ষণ", "Save Calibration"))
                    }
                }

                GTCard {
                    Text(tr(lang, "২. প্যাকেট ও সিমুলেশন টেস্ট", "2. INGEST TEST PACKET OR DEMO SERIES"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = testAdcText, onValueChange = { testAdcText = it },
                            label = { Text("Raw ADC") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("sentinel_test_adc")
                        )
                        OutlinedTextField(
                            value = testTempText, onValueChange = { testTempText = it },
                            label = { Text("Temp °C") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onIngestPacket(
                                    deviceIdText,
                                    testAdcText.toIntOrNull() ?: 2250,
                                    testTempText.toDoubleOrNull(),
                                    72.0
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sentinel_ingest_test")
                        ) { Text(tr(lang, "প্যাকেট পাঠান", "Ingest Packet")) }
                        OutlinedButton(
                            onClick = onGenerateSimulation,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sentinel_gen_sim")
                        ) { Text(tr(lang, "+১০ ডেমো রিডিং", "+10 Demo")) }
                    }
                }

                GTCard {
                    Text(tr(lang, "রিয়েলিটি গ্যাপ ইতিহাস চার্ট", "REALITY GAP CHART"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                    Text(
                        tr(lang, "মাপা (সবুজ-লাইম) • মডেল (নীল) • সংশোধিত (মিন্ট)", "Measured (lime) • Predicted (blue) • Corrected (mint)"),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.mute
                    )
                    Spacer(Modifier.height(8.dp))
                    RealityGapHistoryChart(readings = telemetry)
                    Spacer(Modifier.height(10.dp))
                    if (telemetry.isEmpty()) {
                        Text(tr(lang, "এখনো কোনো রিডিং নেই। উপরের '+১০ ডেমো' বা 'প্যাকেট পাঠান' চাপুন।", "No readings yet. Tap '+10 Demo' or 'Ingest Packet' above."), color = c.mute)
                    } else {
                        telemetry.take(10).forEach { r ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${r.source.uppercase()} • ${r.deviceId} #${r.sequence}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = c.ink
                                    )
                                    Text(
                                        "ADC ${r.rawMoistureAdc} → ${r.calibratedMoisturePct}% (pred ${r.predictedMoisturePct}%, est ${r.correctedMoisturePct}%)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                                        color = c.mute
                                    )
                                }
                                StatusBadge(text = r.quality, tone = r.quality)
                            }
                            HorizontalDivider(color = c.line)
                        }
                    }
                }
            }
        }

        // Field Test Checklist (persisted in DataStore)
        GTCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lang, "ফিল্ড-টেস্ট চেকলিস্ট (${checklistDone.size}/৬)", "FIELD-TEST CHECKLIST (${checklistDone.size}/6)"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                OutlinedButton(onClick = onResetChecklist, modifier = Modifier.testTag("sentinel_reset_checklist")) {
                    Text(tr(lang, "রিসেট", "Reset"))
                }
            }
            Spacer(Modifier.height(6.dp))
            checklistItems.forEach { (id, texts) ->
                val checked = id in checklistDone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleChecklist(id) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onToggleChecklist(id) }, modifier = Modifier.testTag("sentinel_chk_$id"))
                    Text(tr(lang, texts.first, texts.second), style = MaterialTheme.typography.bodyMedium, color = c.ink, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
