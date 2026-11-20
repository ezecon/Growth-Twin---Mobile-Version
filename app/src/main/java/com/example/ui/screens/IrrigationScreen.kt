package com.example.ui.screens

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.IrrigationView
import com.example.domain.PlotState
import com.example.ui.components.GTCard
import com.example.ui.components.MetricGaugeBar
import com.example.ui.components.MoistureLiquidGauge
import com.example.ui.components.MoistureTimeSeriesChart
import com.example.ui.components.NoticeBanner
import com.example.ui.components.PumpFlowPipe
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IrrigationScreen(
    lang: AppLang,
    plot: PlotState?,
    view: IrrigationView?,
    onStartPump: (Int, Boolean) -> Unit,
    onStopPump: () -> Unit,
    onSetDemoMoisture: (Double) -> Unit,
    onUpdateSchedule: (Boolean, Int, Int, Boolean) -> Unit
) {
    val c = GTTheme.colors
    var durationMin by remember { mutableIntStateOf(5) }
    var forceStart by remember { mutableStateOf(false) }
    var schEnabled by remember(view?.schedule?.enabled) { mutableStateOf(view?.schedule?.enabled ?: false) }
    var schEveryMin by remember(view?.schedule?.everyMinutes) { mutableStateOf((view?.schedule?.everyMinutes ?: 1440).toString()) }
    var schRunMin by remember(view?.schedule?.minutes) { mutableStateOf((view?.schedule?.minutes ?: 10).toString()) }
    var schOnlyDry by remember(view?.schedule?.onlyIfDry) { mutableStateOf(view?.schedule?.onlyIfDry ?: true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            kicker = "SECTION 2B · IRRIGATION",
            title = tr(lang, "লাইভ সেচ ও পাম্প কন্ট্রোল", "Live Irrigation & Pump Control"),
            subtitle = tr(
                lang,
                "দৈনিক ৯০ মিনিট নিরাপত্তা সীমা, ৫ সেকেন্ড কুলডাউন ও স্বয়ংক্রিয় সময়সূচী।",
                "Real-time moisture gauge, 90 min/24h safety cap, cooldown & rain-aware auto-schedule."
            )
        )

        if (plot == null || view == null) {
            NoticeBanner(
                title = tr(lang, "প্লট নির্বাচন করুন", "Select a plot first"),
                body = tr(lang, "প্লট টুইন ট্যাব থেকে একটি প্লট বেছে নিন।", "Choose or create a plot in Plot Twin."),
                tone = "info"
            )
            return
        }

        // Simulation vs Real Sensor transparency notice
        NoticeBanner(
            title = if (view.mode == "sensor") tr(lang, "আসল ESP32 সেন্সর মোড", "LIVE SENSOR MODE") else tr(lang, "র‍্যান্ডম-ওয়াক ডেমো সিমুলেশন", "SIMULATION MODE"),
            body = if (view.mode == "sensor") {
                tr(lang, "সেন্টিনেল থেকে আসা ক্যালিব্রেটেড সেন্সরের মান দেখানো হচ্ছে।", "Displaying calibrated readings ingested from your device.")
            } else {
                tr(lang, "হাতে-কলমে পরীক্ষার জন্য র‍্যান্ডম-ওয়াক সিমুলেশন চলছে; বাস্তব হার্ডওয়্যার পাম্প নয়।", "Running simulated random-walk soil drift for UI testing; no physical pump is driven.")
            },
            tone = if (view.mode == "sensor") "ok" else "warn"
        )

        // Moisture Wave Gauge + Demo Moisture Test Buttons
        GTCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${plot.name} • ${tr(lang, "মাটির আর্দ্রতা", "Soil Moisture")}", style = MaterialTheme.typography.titleMedium, color = c.ink)
                StatusBadge(
                    text = if (view.pump != null) tr(lang, "পাম্প চালু", "PUMP ON") else tr(lang, "পাম্প বন্ধ", "PUMP OFF"),
                    tone = if (view.pump != null) "go" else "info"
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                MoistureLiquidGauge(
                    moisturePct = view.moisturePct,
                    emergencyBelow = view.emergencyBelow,
                    irrigateBelow = view.irrigateBelow,
                    stopAt = view.stopAt,
                    lang = lang
                )
            }
            Spacer(Modifier.height(12.dp))
            PumpFlowPipe(pumpOn = view.pump != null)
            Spacer(Modifier.height(10.dp))

            if (view.mode == "sim") {
                Text(tr(lang, "ডেমো পরীক্ষা: আর্দ্রতা সেট করুন", "Demo Simulation Presets"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onSetDemoMoisture(18.0) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("irr_demo_dry")
                    ) { Text(tr(lang, "খুব শুষ্ক (১৮%)", "Dry 18%")) }
                    OutlinedButton(
                        onClick = { onSetDemoMoisture(42.0) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("irr_demo_normal")
                    ) { Text(tr(lang, "স্বাভাবিক (৪২%)", "Normal 42%")) }
                    OutlinedButton(
                        onClick = { onSetDemoMoisture(68.0) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("irr_demo_wet")
                    ) { Text(tr(lang, "ভেজা (৬৮%)", "Wet 68%")) }
                }
            }
        }

        // Manual Pump Controls
        GTCard {
            Text(tr(lang, "ম্যানুয়াল পাম্প নিয়ন্ত্রণ", "MANUAL PUMP CONTROL"), style = MaterialTheme.typography.labelMedium, color = c.lime)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 5, 10, 20, 30).forEach { m ->
                    FilterChip(
                        selected = durationMin == m,
                        onClick = { durationMin = m },
                        label = { Text("${BnFormat.num(m, lang)} ${tr(lang, "মি.", "m")}") },
                        modifier = Modifier.testTag("irr_dur_$m")
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lang, "৬০%+ আর্দ্রতায়ও Force চালু", "Force start even above 60%"), style = MaterialTheme.typography.bodyMedium, color = c.mute)
                Switch(checked = forceStart, onCheckedChange = { forceStart = it }, modifier = Modifier.testTag("irr_force_switch"))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onStartPump(durationMin, forceStart) },
                    enabled = view.pump == null,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("irr_pump_start")
                ) {
                    Text(tr(lang, "পাম্প চালু করুন", "Start Pump"))
                }
                Button(
                    onClick = onStopPump,
                    enabled = view.pump != null,
                    colors = ButtonDefaults.buttonColors(containerColor = c.bad, contentColor = c.bg),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("irr_pump_stop")
                ) {
                    Text(tr(lang, "পাম্প বন্ধ করুন", "Stop Pump"))
                }
            }
            Spacer(Modifier.height(10.dp))
            MetricGaugeBar(
                label = tr(lang, "২৪ ঘণ্টার নিরাপত্তা সীমা", "24h Safety Cap (90 min)"),
                valueText = "${BnFormat.num(view.used24hMin, 1, lang)} / ${BnFormat.num(view.dailyCapMin, lang)} ${tr(lang, "মি.", "m")}",
                fraction = (view.used24hMin / view.dailyCapMin).toFloat(),
                color = if (view.left24hMin < 10) c.bad else c.lime
            )
        }

        // Live Time-Series Chart
        GTCard {
            Text(tr(lang, "আর্দ্রতার লাইভ চার্ট", "LIVE MOISTURE SERIES"), style = MaterialTheme.typography.labelMedium, color = c.lime)
            Spacer(Modifier.height(8.dp))
            MoistureTimeSeriesChart(samples = view.series)
        }

        // Auto Schedule Card
        GTCard {
            Text(tr(lang, "স্বয়ংক্রিয় সেচ সময়সূচী", "AUTO SCHEDULE"), style = MaterialTheme.typography.labelMedium, color = c.lime)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lang, "সময়সূচী সক্রিয়", "Schedule Enabled"), color = c.ink)
                Switch(checked = schEnabled, onCheckedChange = { schEnabled = it }, modifier = Modifier.testTag("irr_sch_enabled"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = schEveryMin, onValueChange = { schEveryMin = it },
                    label = { Text(tr(lang, "বিরতি (মিনিট)", "Every (min)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = schRunMin, onValueChange = { schRunMin = it },
                    label = { Text(tr(lang, "সময়কাল (মি.)", "Run (min)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lang, "শুধু মাটি শুষ্ক হলে সেচ দিন (<৩৫%)", "Only irrigate if dry (<35%)"), style = MaterialTheme.typography.bodyMedium, color = c.mute)
                Switch(checked = schOnlyDry, onCheckedChange = { schOnlyDry = it })
            }
            view.schedule.lastSkipReasonBn?.let { skipBn ->
                Spacer(Modifier.height(4.dp))
                Text(
                    tr(lang, "সর্বশেষ স্কিপ: $skipBn", "Last skipped: ${view.schedule.lastSkipReasonEn ?: skipBn}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.warn
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onUpdateSchedule(
                        schEnabled,
                        schEveryMin.toIntOrNull() ?: 1440,
                        schRunMin.toIntOrNull() ?: 10,
                        schOnlyDry
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("irr_sch_save")
            ) {
                Text(tr(lang, "সময়সূচী সংরক্ষণ", "Save Schedule"))
            }
        }

        // Pump History
        if (view.history.isNotEmpty()) {
            GTCard {
                Text(tr(lang, "সেচের ইতিহাস", "IRRIGATION HISTORY"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(8.dp))
                view.history.forEach { h ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${h.source.uppercase()} • ${h.actualSeconds}s (${h.stopReason})", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        Text("${h.moistureBefore}% → ${h.moistureAfter}%", style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont), color = c.mint)
                    }
                    HorizontalDivider(color = c.line)
                }
            }
        }
    }
}
