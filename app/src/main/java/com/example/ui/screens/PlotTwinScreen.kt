package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.CropCatalog
import com.example.domain.DailyLogEntry
import com.example.domain.PlotAlert
import com.example.domain.PlotState
import com.example.domain.PlotTwinEngine
import com.example.domain.TwinActionSpec
import com.example.domain.TwinEvent
import com.example.domain.WeatherSimulator
import com.example.ui.components.GTCard
import com.example.ui.components.MetricGaugeBar
import com.example.ui.components.NoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StageBar
import com.example.ui.components.StatusBadge
import com.example.ui.components.TwinSparklineChart
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlotTwinScreen(
    lang: AppLang,
    plots: List<PlotState>,
    selectedPlot: PlotState?,
    logs: List<DailyLogEntry>,
    events: List<TwinEvent>,
    ttsSpeaking: Boolean,
    onSelectPlot: (String) -> Unit,
    onCreatePlot: (String, String, String, Double, Double?, Double?, Boolean, Boolean) -> Unit,
    onAdvance: (Int, Boolean, Boolean) -> Unit,
    onAction: (TwinActionSpec) -> Unit,
    onAnswerQuestion: (String, String) -> Unit,
    onManualReading: (Double?, Double?, Double?) -> Unit,
    onInspectPhoto: (Uri) -> Unit,
    onRunExperiment: (String, Int, String, Boolean?, String?) -> Unit,
    onUpdateConfig: (Boolean, Boolean) -> Unit,
    onDeletePlot: () -> Unit,
    onSpeakSummary: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    val c = GTTheme.colors
    var showNewDialog by remember { mutableStateOf(false) }
    var whyAlert by remember { mutableStateOf<PlotAlert?>(null) }

    // Manual reading inputs
    var phText by remember { mutableStateOf("") }
    var ecText by remember { mutableStateOf("") }
    var moistureText by remember { mutableStateOf("") }

    // Experiment Lab states
    var expHorizon by remember { mutableStateOf("14") }
    var expType by remember { mutableStateOf("irrigation") }
    var expAutoIrr by remember { mutableStateOf(false) }
    var expNutrient by remember { mutableStateOf("N") }

    // Zero-permission Photo Picker
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onInspectPhoto(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            kicker = "SECTION 02 · PLOT TWIN",
            title = tr(lang, "জীবন্ত ডিজিটাল প্লট টুইন", "Living Plot Digital Twin"),
            subtitle = tr(
                lang,
                "দৈনিক পানি, পুষ্টি, রোগের ঝুঁকি, ল্যাব পরীক্ষা ও ৩-ট্যাপ চেক-ইন।",
                "Day-by-day crop physiology, What-If Experiment Lab, alerts & daily check-ins."
            )
        )

        // Plot selector & Create button
        GTCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lang, "আপনার প্লটসমূহ", "Your Plots"), style = MaterialTheme.typography.titleMedium, color = c.ink)
                Button(
                    onClick = { showNewDialog = true },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("twin_new_plot_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.padding(2.dp))
                    Text(tr(lang, "নতুন প্লট", "New Plot"))
                }
            }
            if (plots.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    plots.forEach { p ->
                        FilterChip(
                            selected = selectedPlot?.id == p.id,
                            onClick = { onSelectPlot(p.id) },
                            label = { Text("${p.name} (d${p.day})") },
                            modifier = Modifier.testTag("twin_select_${p.id}")
                        )
                    }
                }
            }
        }

        if (selectedPlot == null) {
            NoticeBanner(
                title = tr(lang, "কোনো প্লট নেই", "No plot selected"),
                body = tr(lang, "উপরের 'নতুন প্লট' বোতামে চাপ দিয়ে একটি জমি যোগ করুন।", "Tap 'New Plot' above to initialize a digital twin."),
                tone = "info"
            )
        } else {
            val crop = CropCatalog.BY_ID.getValue(selectedPlot.cropId)
            val prof = CropCatalog.profile(crop.id, crop.waterNeed)
            val stages = CropCatalog.stagesOf(crop.id)
            val (_, curStage) = CropCatalog.stageAt(crop.id, selectedPlot.day, crop.durationDays)
            val advice = PlotTwinEngine.buildAdvice(selectedPlot)
            val questions = PlotTwinEngine.openQuestionViews(selectedPlot)
            val forecast = WeatherSimulator.forecast(selectedPlot.seed, selectedPlot.currentDate().plusDays(1), 7)
            val est = if (selectedPlot.status in setOf("growing", "ready")) {
                PlotTwinEngine.estimateYield(selectedPlot, crop)
            } else selectedPlot.result

            // Crop hero + Stage Bar
            GTCard(accentBorder = c.lime.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedPlot.name, style = MaterialTheme.typography.headlineMedium, color = c.ink)
                        Text(
                            "${crop.displayName(lang)} • ${BnFormat.num(selectedPlot.area, 0, lang)} ${tr(lang, "শতাংশ", "dec")} • ${CropCatalog.soilName(selectedPlot.soil, lang)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.mute
                        )
                    }
                    StatusBadge(
                        text = when (selectedPlot.status) {
                            "ready" -> tr(lang, "কাটার সময়", "Ready")
                            "harvested" -> tr(lang, "কাটা হয়েছে", "Harvested")
                            "dead" -> tr(lang, "মারা গেছে", "Failed")
                            else -> tr(lang, "দিন ${BnFormat.num(selectedPlot.day, lang)}/${BnFormat.num(crop.durationDays, lang)}", "Day ${selectedPlot.day}/${crop.durationDays}")
                        },
                        tone = if (selectedPlot.status == "dead") "bad" else if (selectedPlot.status == "ready") "warn" else "go"
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    tr(lang, "পর্যায়: ${curStage.nameBn}", "Stage: ${curStage.nameEn}"),
                    style = MaterialTheme.typography.labelLarge,
                    color = c.lime
                )
                Spacer(Modifier.height(6.dp))
                StageBar(stages, selectedPlot.day, crop.durationDays, lang)

                if (est != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            tr(lang, "স্বাস্থ্য: ${BnFormat.num(selectedPlot.health, lang)}%", "Health: ${selectedPlot.health.roundToInt()}%"),
                            style = MaterialTheme.typography.labelLarge,
                            color = c.mint
                        )
                        Text(
                            tr(lang, "সম্ভাব্য ফলন: ${BnFormat.num(est.yieldKg, lang)} কেজি (${BnFormat.signedMoney(est.profit, lang)})",
                                "Est: ${est.yieldKg} kg (${BnFormat.signedMoney(est.profit, lang)})"),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont),
                            color = c.ink
                        )
                    }
                }
                val warns = if (lang == AppLang.BN) selectedPlot.warningsBn else selectedPlot.warningsEn
                if (warns.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    warns.forEach { w ->
                        Text("⚠ $w", style = MaterialTheme.typography.bodyMedium, color = c.warn)
                    }
                }
                selectedPlot.lastWx?.let { wx ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr(
                            lang,
                            "আজকের আবহাওয়া: ${BnFormat.num(wx.tmin, 1, lang)}–${BnFormat.num(wx.tmax, 1, lang)}°C • বৃষ্টি ${BnFormat.num(wx.rainMm, 1, lang)} মিমি • আর্দ্রতা ${wx.rh}%",
                            "Today's weather: ${wx.tmin}–${wx.tmax}°C • Rain ${wx.rainMm} mm • RH ${wx.rh}%"
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                        color = c.mute
                    )
                }
            }

            // Harvest / End-of-Season Outcome Card (when ready, harvested, or dead)
            if (selectedPlot.status == "ready") {
                GTCard(accentBorder = c.lime) {
                    Text(
                        tr(lang, "ফসল কাটার সময় হয়েছে!", "Ready for Harvest"),
                        style = MaterialTheme.typography.titleMedium,
                        color = c.lime
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr(lang, "আপনার ফসল পরিপক্ব পর্যায়ে পৌঁছেছে। এখনই ফসল তুলে চূড়ান্ত লাভের হিসাব করুন।", "Crop reached harvest maturity. Harvest now to record yield & cohort rank."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onAction(TwinActionSpec("harvest")) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("twin_harvest_now")
                    ) {
                        Text(tr(lang, "ফসল কাটুন ও লাভ হিসাব করুন", "Harvest & Record Profit"))
                    }
                }
            }
            selectedPlot.result?.let { res ->
                GTCard(accentBorder = if (res.outcome == "dead") c.bad else c.mint) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tr(lang, "মৌসুম সমাপ্তি ফলাফল", "FINAL HARVEST OUTCOME"),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (res.outcome == "dead") c.bad else c.mint
                        )
                        StatusBadge(
                            text = res.outcome.uppercase(),
                            tone = if (res.outcome == "dead") "bad" else "go"
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(tr(lang, "মোট ফলন", "Yield"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                            Text("${BnFormat.num(res.yieldKg, lang)} kg", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont), color = c.ink)
                        }
                        Column {
                            Text(tr(lang, "মোট আয়", "Revenue"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                            Text(BnFormat.money(res.revenue, lang), style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont), color = c.ink)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(tr(lang, "নিট লাভ/ক্ষতি", "Net Profit"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                            Text(
                                BnFormat.signedMoney(res.profit, lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont),
                                color = if (res.profit >= 0) c.mint else c.bad
                            )
                        }
                    }
                }
            }

            // Time Advance Controls
            if (selectedPlot.status in setOf("growing", "ready")) {
                GTCard {
                    Text(tr(lang, "সময় এগিয়ে নিন (সিমুলেশন)", "SIMULATE TIME"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAdvance(1, true, false) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("twin_step_1d")
                        ) { Text(tr(lang, "+১ দিন", "+1 Day")) }
                        Button(
                            onClick = { onAdvance(7, true, false) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("twin_step_7d")
                        ) { Text(tr(lang, "+৭ দিন", "+7 Days")) }
                        OutlinedButton(
                            onClick = { onAdvance(crop.durationDays + 5, false, true) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("twin_step_harvest")
                        ) { Text(tr(lang, "কর্তন পর্যন্ত", "To Harvest")) }
                    }
                }
            }

            // What to do today (Advice cards)
            GTCard {
                Text(tr(lang, "আজকের করণীয় পরামর্শ", "WHAT TO DO TODAY"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(8.dp))
                advice.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${item.icon} ${tr(lang, item.titleBn, item.titleEn)}", style = MaterialTheme.typography.titleMedium, color = c.ink)
                            Text(tr(lang, item.textBn, item.textEn), style = MaterialTheme.typography.bodyMedium, color = c.mute)
                        }
                        if (item.action != null) {
                            Button(
                                onClick = { onAction(item.action) },
                                modifier = Modifier.padding(start = 8.dp).heightIn(min = 48.dp).testTag("twin_advice_${item.id}")
                            ) {
                                Text(tr(lang, item.btnBn ?: "করুন", item.btnEn ?: "Apply"))
                            }
                        }
                    }
                    HorizontalDivider(color = c.line)
                }
            }

            // Soil Sensors & Sparkline
            GTCard {
                Text(tr(lang, "মাটি ও ফসলের অবস্থা", "SOIL & CROP METRICS"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(8.dp))
                MetricGaugeBar(
                    label = tr(lang, "মাটির আর্দ্রতা (সীমা ${prof.trigger.toInt()}%)", "Soil Water (trigger ${prof.trigger.toInt()}%)"),
                    valueText = "${BnFormat.num(selectedPlot.awPct(), lang)}%",
                    fraction = (selectedPlot.awPct() / 140.0).toFloat(),
                    color = if (selectedPlot.awPct() < prof.emergency) c.bad else if (selectedPlot.awPct() < prof.trigger) c.warn else c.mint
                )
                Spacer(Modifier.height(8.dp))
                MetricGaugeBar(tr(lang, "নাইট্রোজেন (N)", "Nitrogen (N)"), "${BnFormat.num(selectedPlot.n, lang)}%", (selectedPlot.n / 100.0).toFloat(), c.lime)
                Spacer(Modifier.height(6.dp))
                MetricGaugeBar(tr(lang, "ফসফরাস (P)", "Phosphorus (P)"), "${BnFormat.num(selectedPlot.p, lang)}%", (selectedPlot.p / 100.0).toFloat(), c.mint)
                Spacer(Modifier.height(6.dp))
                MetricGaugeBar(tr(lang, "পটাশিয়াম (K)", "Potassium (K)"), "${BnFormat.num(selectedPlot.k, lang)}%", (selectedPlot.k / 100.0).toFloat(), c.blue)
                Spacer(Modifier.height(6.dp))
                MetricGaugeBar(tr(lang, "রোগের ঝুঁকি", "Disease Risk"), "${BnFormat.num(selectedPlot.dis, lang)}%", (selectedPlot.dis / 100.0).toFloat(), if (selectedPlot.dis >= 60) c.bad else c.warn)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("pH ${BnFormat.num(selectedPlot.ph, 2, lang)} (ideal ${crop.phMin}–${crop.phMax})", style = MaterialTheme.typography.labelLarge, color = c.ink)
                    Text("EC ${BnFormat.num(selectedPlot.ec, 2, lang)} dS/m", style = MaterialTheme.typography.labelLarge, color = c.ink)
                }
                if (logs.size >= 2) {
                    Spacer(Modifier.height(10.dp))
                    Text(tr(lang, "স্বাস্থ্য (সবুজ) ও আর্দ্রতা (মিন্ট) ধারা", "Health (lime) & Moisture (mint) trend"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                    Spacer(Modifier.height(4.dp))
                    TwinSparklineChart(logs)
                }
            }

            // Daily Check-in: Photo Advisory + 3-tap Questions + Manual Reading
            GTCard {
                Text(tr(lang, "দৈনিক চেক-ইন (ছবি, প্রশ্ন ও মাপা মান)", "DAILY CHECK-IN"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("twin_pick_leaf_photo")
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text(tr(lang, "পাতার ছবি বিশ্লেষণ (শুধু পরামর্শ)", "Inspect Leaf Photo (Advisory Only)"))
                }

                if (questions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    questions.forEach { q ->
                        Text(tr(lang, q.textBn, q.textEn), style = MaterialTheme.typography.bodyLarge, color = c.ink)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            q.options.forEach { opt ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onAnswerQuestion(q.id, opt.value) },
                                    label = { Text(tr(lang, opt.labelBn, opt.labelEn)) },
                                    modifier = Modifier.testTag("twin_q_${q.id}_${opt.value}")
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(tr(lang, "হাতে মাপা মান দিন (ঐচ্ছিক)", "Enter Manual Probe Readings"), style = MaterialTheme.typography.labelLarge, color = c.mute)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = phText, onValueChange = { phText = it },
                        label = { Text("pH") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("twin_read_ph")
                    )
                    OutlinedTextField(
                        value = ecText, onValueChange = { ecText = it },
                        label = { Text("EC") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("twin_read_ec")
                    )
                    OutlinedTextField(
                        value = moistureText, onValueChange = { moistureText = it },
                        label = { Text("Water %") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("twin_read_aw")
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onManualReading(phText.toDoubleOrNull(), ecText.toDoubleOrNull(), moistureText.toDoubleOrNull())
                        phText = ""; ecText = ""; moistureText = ""
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("twin_submit_reading")
                ) {
                    Text(tr(lang, "মাপা মান টুইনে যুক্ত করুন", "Update Twin State"))
                }
            }

            // Farm Experiment Lab (What-If)
            GTCard(accentBorder = c.mint.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = c.mint)
                    Text(tr(lang, "খামার পরীক্ষাগার (What-If ল্যাব)", "FARM EXPERIMENT LAB"), style = MaterialTheme.typography.labelMedium, color = c.mint)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    tr(lang, "মূল প্লট পরিবর্তন না করে ক্লোন কপিতে সেচ বা সারের প্রভাব যাচাই করুন।", "Run counterfactual simulations on an isolated clone without changing live state."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.mute
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = expType == "irrigation",
                        onClick = { expType = "irrigation" },
                        label = { Text(tr(lang, "সেচ পরীক্ষা", "Irrigation")) },
                        modifier = Modifier.testTag("lab_type_irrigation")
                    )
                    FilterChip(
                        selected = expType == "fertilizer",
                        onClick = { expType = "fertilizer" },
                        label = { Text(tr(lang, "সার পরীক্ষা", "Fertilizer")) },
                        modifier = Modifier.testTag("lab_type_fertilizer")
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (expType == "irrigation") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !expAutoIrr, onClick = { expAutoIrr = false }, label = { Text(tr(lang, "অটো সেচ বন্ধ", "Auto Irr OFF")) })
                        FilterChip(selected = expAutoIrr, onClick = { expAutoIrr = true }, label = { Text(tr(lang, "অটো সেচ চালু", "Auto Irr ON")) })
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("N", "P", "K").forEach { n ->
                            FilterChip(selected = expNutrient == n, onClick = { expNutrient = n }, label = { Text("+$n") })
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = expHorizon, onValueChange = { expHorizon = it },
                        label = { Text(tr(lang, "দিন", "Horizon (days)")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("lab_horizon_input")
                    )
                    Button(
                        onClick = {
                            val h = (expHorizon.toIntOrNull() ?: 14).coerceIn(1, 120)
                            onRunExperiment("", h, expType, expAutoIrr, expNutrient)
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("lab_run_button")
                    ) {
                        Text(tr(lang, "তুলনা চালান", "Run Experiment"))
                    }
                }
                selectedPlot.experiments.reversed().take(3).forEach { ex ->
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = c.line)
                    Spacer(Modifier.height(6.dp))
                    Text("${ex.name} (${ex.horizonDays}d)", style = MaterialTheme.typography.titleMedium, color = c.ink)
                    Text(
                        tr(
                            lang,
                            "ফলন পার্থক্য: ${ex.deltaYieldKg} কেজি • লাভ: ${BnFormat.signedMoney(ex.deltaProfitBdt, lang)} • পানি: ${ex.deltaWaterL} লি.",
                            "Δ Yield: ${ex.deltaYieldKg} kg • Δ Profit: ${BnFormat.signedMoney(ex.deltaProfitBdt, lang)} • Δ Water: ${ex.deltaWaterL} L"
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                        color = c.lime
                    )
                }
            }

            // 7-day weather outlook
            GTCard {
                Text(tr(lang, "আগামী ৭ দিনের আবহাওয়ার পূর্বাভাস", "7-DAY WEATHER OUTLOOK"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(8.dp))
                forecast.forEach { f ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(f.date.substring(5), style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont), color = c.ink)
                        Text("${BnFormat.num(f.tmax, 1, lang)}°C", style = MaterialTheme.typography.bodyMedium, color = c.warn)
                        Text("${BnFormat.num(f.rainMm, 1, lang)} mm (${f.prob}%)", style = MaterialTheme.typography.bodyMedium, color = c.mint)
                    }
                }
            }

            // Alerts with "Why?" modal
            if (selectedPlot.alerts.isNotEmpty()) {
                GTCard {
                    Text(tr(lang, "সতর্কতা ও কারণ ('কেন?')", "ALERTS & EXPLANATIONS"), style = MaterialTheme.typography.labelMedium, color = c.warn)
                    Spacer(Modifier.height(8.dp))
                    selectedPlot.alerts.reversed().take(6).forEach { a ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(tr(lang, a.titleBn, a.titleEn), style = MaterialTheme.typography.titleMedium, color = c.ink)
                                Text(tr(lang, a.textBn, a.textEn), style = MaterialTheme.typography.bodyMedium, color = c.mute)
                            }
                            if (a.why != null) {
                                TextButton(onClick = { whyAlert = a }, modifier = Modifier.testTag("twin_why_${a.key}")) {
                                    Text(tr(lang, "কেন?", "Why?"))
                                }
                            }
                        }
                    }
                }
            }

            // Event Timeline & Daily Logs History (from original web Plot Twin)
            if (events.isNotEmpty() || logs.isNotEmpty()) {
                var historyTab by remember { mutableStateOf("events") }
                GTCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tr(lang, "কার্যক্রম ও দৈনিক লগ", "TIMELINE & DAILY LOGS"),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.lime
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = historyTab == "events",
                                onClick = { historyTab = "events" },
                                label = { Text(tr(lang, "ইভেন্ট", "Events")) },
                                modifier = Modifier.testTag("twin_tab_events")
                            )
                            FilterChip(
                                selected = historyTab == "logs",
                                onClick = { historyTab = "logs" },
                                label = { Text(tr(lang, "দৈনিক লগ", "Daily Logs")) },
                                modifier = Modifier.testTag("twin_tab_logs")
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (historyTab == "events") {
                        events.reversed().take(10).forEach { ev ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "D${ev.day}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont),
                                    color = c.lime,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    tr(lang, ev.textBn, ev.textEn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.ink,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(color = c.line)
                        }
                    } else {
                        logs.reversed().take(10).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "D${log.day} • ${tr(lang, log.stageBn, log.stageEn)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = c.ink
                                    )
                                    Text(
                                        "AW ${BnFormat.num(log.aw, lang)}% • H ${BnFormat.num(log.health, lang)}% • Rain ${log.rainMm}mm",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                                        color = c.mute
                                    )
                                }
                                Text(
                                    "${log.estYieldKg} kg",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont),
                                    color = c.mint
                                )
                            }
                            HorizontalDivider(color = c.line)
                        }
                    }
                }
            }

            // Settings + TTS Listen + Delete
            GTCard {
                Text(tr(lang, "প্লট সেটিংস ও বাংলা সারাংশ", "SETTINGS & AUDIO SUMMARY"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr(lang, "স্বয়ংক্রিয় সেচ চালু", "Auto-Irrigation Controller"), color = c.ink)
                    Switch(
                        checked = selectedPlot.config.autoIrrigation,
                        onCheckedChange = { onUpdateConfig(it, selectedPlot.config.autoInputs) },
                        modifier = Modifier.testTag("twin_cfg_auto_irr")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr(lang, "স্বয়ংক্রিয় সার/স্প্রে", "Auto Fertilize / Spray"), color = c.ink)
                    Switch(
                        checked = selectedPlot.config.autoInputs,
                        onCheckedChange = { onUpdateConfig(selectedPlot.config.autoIrrigation, it) },
                        modifier = Modifier.testTag("twin_cfg_auto_inputs")
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(PlotTwinEngine.buildSummaryText(selectedPlot, lang), style = MaterialTheme.typography.bodyMedium, color = c.mute)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { if (ttsSpeaking) onStopSpeaking() else onSpeakSummary() },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("twin_tts_button")
                    ) {
                        Icon(if (ttsSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text(tr(lang, if (ttsSpeaking) "থামুন" else "শুনুন", if (ttsSpeaking) "Stop" else "Listen"))
                    }
                    OutlinedButton(
                        onClick = onDeletePlot,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("twin_delete_plot")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = tr(lang, "মুছুন", "Delete"))
                    }
                }
            }
        }
    }

    if (whyAlert != null) {
        val alert = whyAlert!!
        val w = alert.why!!
        AlertDialog(
            onDismissRequest = { whyAlert = null },
            title = { Text(tr(lang, alert.titleBn, alert.titleEn)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr(lang, w.ruleBn, w.ruleEn), style = MaterialTheme.typography.bodyLarge)
                    HorizontalDivider()
                    w.facts.forEach { f ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr(lang, f.labelBn, f.labelEn), color = c.mute)
                            Text(f.value, fontWeight = FontWeight.Bold, fontFamily = MonoFont)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { whyAlert = null }) { Text(tr(lang, "বুঝেছি", "Close")) }
            }
        )
    }

    if (showNewDialog) {
        var name by remember { mutableStateOf("") }
        var cropId by remember { mutableStateOf("boro") }
        var soil by remember { mutableStateOf("doash") }
        var areaText by remember { mutableStateOf("33") }
        var phTextDlg by remember { mutableStateOf("6.3") }
        var autoIrr by remember { mutableStateOf(true) }
        var autoInp by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text(tr(lang, "নতুন প্লট তৈরি", "Create New Plot")) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text(tr(lang, "প্লটের নাম", "Plot name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_plot_name")
                    )
                    Text(tr(lang, "ফসল", "Crop"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CropCatalog.CROPS.forEach { cr ->
                            FilterChip(
                                selected = cropId == cr.id,
                                onClick = { cropId = cr.id },
                                label = { Text(cr.displayName(lang)) }
                            )
                        }
                    }
                    Text(tr(lang, "মাটি", "Soil"), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("doash", "entel", "beley").forEach { sId ->
                            FilterChip(
                                selected = soil == sId,
                                onClick = { soil = sId },
                                label = { Text(CropCatalog.soilName(sId, lang)) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = areaText, onValueChange = { areaText = it },
                            label = { Text(tr(lang, "শতাংশ", "Decimal")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phTextDlg, onValueChange = { phTextDlg = it },
                            label = { Text("pH") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autoIrr, onCheckedChange = { autoIrr = it })
                        Text(tr(lang, "স্বয়ংক্রিয় সেচ", "Auto irrigation"))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autoInp, onCheckedChange = { autoInp = it })
                        Text(tr(lang, "স্বয়ংক্রিয় সার/স্প্রে", "Auto inputs"))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePlot(
                            name, cropId, soil,
                            areaText.toDoubleOrNull() ?: 33.0,
                            phTextDlg.toDoubleOrNull(), 1.0, autoIrr, autoInp
                        )
                        showNewDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_plot")
                ) {
                    Text(tr(lang, "তৈরি করুন", "Create"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text(tr(lang, "বাতিল", "Cancel")) }
            }
        )
    }
}
