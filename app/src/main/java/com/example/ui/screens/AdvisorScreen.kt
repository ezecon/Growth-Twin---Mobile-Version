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
import com.example.domain.AdvisorReport
import com.example.domain.CropAssessment
import com.example.ui.components.GTCard
import com.example.ui.components.NoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvisorScreen(
    lang: AppLang,
    promptText: String,
    report: AdvisorReport?,
    onPromptChange: (String) -> Unit,
    onSubmitPrompt: (String) -> Unit,
    onAnswerSlot: (String, String) -> Unit,
    onUpdatePh: (Double) -> Unit,
    onRequestReview: () -> Unit
) {
    val c = GTTheme.colors
    var phInput by remember { mutableStateOf("6.3") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            kicker = "SECTION 01 · ADVISOR",
            title = tr(lang, "কোন ফসলে লাভ ও ঝুঁকি কেমন?", "Which crop fits your field, season & budget?"),
            subtitle = tr(
                lang,
                "বাংলা বা ইংরেজিতে আপনার জমি, আগের ফসল ও সেচের কথা লিখুন — ৩ দৃশ্যপটে লাভ-ক্ষতির হিসাব দেখুন।",
                "Describe your plot size, previous crop, and irrigation access — get rule checks & 3-scenario economics."
            )
        )

        GTCard {
            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChange,
                label = { Text(tr(lang, "আপনার জমির বিবরণ লিখুন", "Describe your field & crop plan")) },
                placeholder = {
                    Text(
                        tr(
                            lang,
                            "যেমন: ২ বিঘা দোআঁশ জমি, আগে আলু করেছিলাম, এবার রবি মৌসুমে টমেটো করতে চাই, সেচের ব্যবস্থা আছে",
                            "e.g. 2 bigha loam soil, grew potato last year, want tomato in rabi with irrigation"
                        )
                    )
                },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("advisor_prompt_input")
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSubmitPrompt(promptText) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("advisor_submit_button")
                ) {
                    Text(tr(lang, "পরামর্শ দেখুন", "Analyze Plot"))
                }
                OutlinedButton(
                    onClick = {
                        val demo = if (lang == AppLang.BN) {
                            "২ বিঘা দোআঁশ জমি, আগে আলু করেছিলাম, এবার রবি মৌসুমে টমেটো করতে চাই, সেচের ব্যবস্থা আছে"
                        } else {
                            "2 bigha loam soil, grew potato previously, want tomato in rabi with good irrigation"
                        }
                        onSubmitPrompt(demo)
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("advisor_example_button")
                ) {
                    Text(tr(lang, "উদাহরণ", "Example"))
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val quickPrompts = listOf(
                    "১ বিঘা বোরো ধান রবি মৌসুম সেচ ভালো" to "1 bigha boro rice rabi good irrigation",
                    "৩৩ শতাংশ এঁটেল মাটি সরিষা" to "33 decimal clay soil mustard rabi",
                    "আগের ফসল মসুর ডাল, এবার ভুট্টা" to "Previous lentil, want maize now"
                )
                quickPrompts.forEachIndexed { idx, (bnQ, enQ) ->
                    FilterChip(
                        selected = false,
                        onClick = { onSubmitPrompt(tr(lang, bnQ, enQ)) },
                        label = { Text(tr(lang, bnQ, enQ)) },
                        modifier = Modifier.testTag("advisor_quick_chip_$idx")
                    )
                }
            }
        }

        if (report != null) {
            // Follow-up Questions
            if (report.needsFollowup && report.pendingQuestions.isNotEmpty()) {
                GTCard(accentBorder = c.warn.copy(alpha = 0.6f)) {
                    Text(
                        tr(lang, "আরও সঠিক হিসাবের জন্য উত্তর দিন", "Clarify a few details for higher confidence"),
                        style = MaterialTheme.typography.titleMedium,
                        color = c.warn
                    )
                    Spacer(Modifier.height(8.dp))
                    report.pendingQuestions.forEach { q ->
                        Text(tr(lang, q.textBn, q.textEn), style = MaterialTheme.typography.bodyLarge, color = c.ink)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            q.options.forEach { opt ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onAnswerSlot(q.slot, opt.value) },
                                    label = { Text(tr(lang, opt.labelBn, opt.labelEn)) },
                                    modifier = Modifier.testTag("advisor_q_${q.slot}_${opt.value}")
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            // Headline & Confidence
            GTCard(accentBorder = c.lime.copy(alpha = 0.6f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr(lang, "সিদ্ধান্ত সারসংক্ষেপ", "RECOMMENDATION"),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.lime
                    )
                    StatusBadge(
                        text = tr(
                            lang,
                            "নিশ্চয়তা ${BnFormat.num(report.confidence * 100, lang)}%",
                            "Confidence ${(report.confidence * 100).toInt()}%"
                        ),
                        tone = if (report.confidence >= 0.7) "go" else "caution"
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    tr(lang, report.headlineBn, report.headlineEn),
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.ink
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phInput,
                        onValueChange = { phInput = it },
                        label = { Text(tr(lang, "মাটির pH পরীক্ষা", "Measured pH")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("advisor_ph_input")
                    )
                    Button(
                        onClick = { phInput.toDoubleOrNull()?.let(onUpdatePh) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("advisor_ph_apply")
                    ) {
                        Text(tr(lang, "হিসাব করুন", "Apply pH"))
                    }
                }
            }

            // Target crop 3-scenario card
            report.target?.let { AssessmentCard(it, lang, isTarget = true) }

            // Switch vs Stay comparison
            report.comparison?.let { cmp ->
                GTCard(accentBorder = c.mint.copy(alpha = 0.5f)) {
                    Text(
                        tr(lang, "আগের ফসল বনাম নতুন ফসল তুলনা", "SWITCH VS STAY COMPARISON"),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.mint
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr(lang, cmp.verdictBn, cmp.verdictEn),
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.ink,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(tr(lang, "গড় লাভের পার্থক্য", "Base profit delta"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                            Text(
                                BnFormat.signedMoney(cmp.deltaBase, lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont),
                                color = if (cmp.deltaBase >= 0) c.mint else c.bad
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(tr(lang, "খারাপ অবস্থায় পার্থক্য", "Worst-case delta"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                            Text(
                                BnFormat.signedMoney(cmp.deltaWorst, lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont),
                                color = if (cmp.deltaWorst >= 0) c.mint else c.bad
                            )
                        }
                    }
                }
            }

            // Own past yields card
            if (report.ownYields.isNotEmpty()) {
                GTCard {
                    Text(
                        tr(lang, "আপনার নিজের পূর্ববর্তী ফলন বনাম টেবিল গড়", "YOUR PAST HARVESTS VS REFERENCE TABLE"),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.lime
                    )
                    Spacer(Modifier.height(8.dp))
                    report.ownYields.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tr(lang, row.nameBn, row.nameEn), color = c.ink, fontWeight = FontWeight.SemiBold)
                            Text(
                                tr(
                                    lang,
                                    "নিজস্ব গড়: ${BnFormat.num(row.ownAvgKgDec, 1, lang)} কেজি/শতাংশ (গড় ${BnFormat.num(row.tableBaseKgDec, 1, lang)})",
                                    "Own avg: ${row.ownAvgKgDec} kg/dec (ref ${row.tableBaseKgDec})"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.mint
                            )
                        }
                    }
                }
            }

            // Alternative crops
            if (report.alternatives.isNotEmpty()) {
                Text(
                    tr(lang, "বিকল্প ফসল সুপারিশ", "Alternative Crops This Season"),
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink
                )
                report.alternatives.forEach { alt ->
                    AssessmentCard(alt, lang, isTarget = false)
                }
            }

            // Narrative & Expert Review Request
            GTCard {
                Text(tr(lang, "বিস্তারিত প্রতিবেদন", "FULL SUMMARY"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(6.dp))
                Text(tr(lang, report.narrativeBn, report.narrativeEn), style = MaterialTheme.typography.bodyMedium, color = c.ink)
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = c.line)
                Spacer(Modifier.height(10.dp))
                when (report.reviewStatus) {
                    "reviewed" -> NoticeBanner(
                        title = tr(lang, "বিশেষজ্ঞের মতামত: ${report.expertVerdict ?: ""}", "Expert Review: ${report.expertVerdict ?: ""}"),
                        body = report.expertNote ?: "",
                        tone = "ok"
                    )
                    "requested" -> StatusBadge(
                        text = tr(lang, "বিশেষজ্ঞ রিভিউয়ের অপেক্ষায়", "Queued for Expert Review"),
                        tone = "warn"
                    )
                    else -> OutlinedButton(
                        onClick = onRequestReview,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("advisor_request_review")
                    ) {
                        Text(tr(lang, "কৃষি বিশেষজ্ঞের রিভিউ চান", "Send to Expert Review Queue"))
                    }
                }
            }
        }

        NoticeBanner(
            title = tr(lang, "দায়বদ্ধতা নোট", "Disclaimer"),
            body = tr(
                lang,
                "এটি নিয়ম-ভিত্তিক ও আনুমানিক কৃষি পরামর্শ মাত্র। বীজ ও কীটনাশকের চূড়ান্ত মাত্রা স্থানীয় উপ-সহকারী কৃষি কর্মকর্তার সাথে যাচাই করুন।",
                "This is rule-based advisory only. Verify exact seed and pesticide doses with your local agriculture office."
            ),
            tone = "info"
        )
    }
}

@Composable
private fun AssessmentCard(a: CropAssessment, lang: AppLang, isTarget: Boolean) {
    val c = GTTheme.colors
    GTCard(accentBorder = if (isTarget) c.lineBright else c.line) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (lang == AppLang.BN) a.nameBn else a.nameEn,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink
                )
                Text(
                    text = tr(lang, "খরচ: ${BnFormat.money(a.scenarios.cost, lang)}", "Cost: ${BnFormat.money(a.scenarios.cost, lang)}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.mute
                )
            }
            StatusBadge(
                text = if (lang == AppLang.BN) a.ratingBn else a.ratingEn,
                tone = a.rating
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScenarioBox(tr(lang, "খারাপ", "Bad"), a.scenarios.bad.profit, lang, Modifier.weight(1f))
            ScenarioBox(tr(lang, "গড় (মূল)", "Base"), a.scenarios.base.profit, lang, Modifier.weight(1f))
            ScenarioBox(tr(lang, "ভালো", "Good"), a.scenarios.good.profit, lang, Modifier.weight(1f))
        }
        a.scenarios.breakevenPrice?.let { be ->
            Spacer(Modifier.height(8.dp))
            Text(
                tr(
                    lang,
                    "ব্রেক-ইভেন দাম: ${BnFormat.num(be, 1, lang)} ৳/কেজি • মার্জিন: ${BnFormat.num(a.scenarios.marginPct, 1, lang)}%",
                    "Break-even: $be BDT/kg • Base margin: ${a.scenarios.marginPct}%"
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont),
                color = c.mute
            )
        }
        val reasons = if (lang == AppLang.BN) a.reasonsBn else a.reasonsEn
        if (reasons.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            reasons.forEach { r ->
                Text("• $r", style = MaterialTheme.typography.bodyMedium, color = if (a.rating == "no") c.bad else c.warn)
            }
        }
    }
}

@Composable
private fun ScenarioBox(label: String, profit: Int, lang: AppLang, modifier: Modifier = Modifier) {
    val c = GTTheme.colors
    Column(modifier = modifier.padding(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.mute)
        Text(
            text = BnFormat.signedMoney(profit, lang),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont),
            color = if (profit >= 0) c.mint else c.bad
        )
    }
}
