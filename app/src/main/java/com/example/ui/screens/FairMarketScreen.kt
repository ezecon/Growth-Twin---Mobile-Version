package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.MarketPriceAnalysis
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FairMarketScreen(
    lang: AppLang,
    query: String,
    qtyText: String,
    analysis: MarketPriceAnalysis,
    onQueryChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onAnalyze: (String?) -> Unit
) {
    val c = GTTheme.colors

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onQueryChange(spoken)
                onAnalyze(spoken)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            kicker = "FAIR MARKET · DAMSATHI",
            title = tr(lang, "দামসাথী — ন্যায্য বাজার দাম ও অধিকার", "DamSathi — Fair Market & Buyer Audit"),
            subtitle = tr(
                lang,
                "ফসলের ন্যায্য খামার-দাম, দালালের অফার যাচাই, পরিবহন খরচ বাদে পাশের জেলা ও বাজারের অধিকার।",
                "Check reference farm-gate prices, audit buyer offers per kg, and compare nearby districts net of haulage."
            )
        )

        NoticeBanner(
            title = tr(lang, "রেফারেন্স ডেটা নোট", "Offline Reference Baseline"),
            body = tr(
                lang,
                "DAM ও WFP রেফারেন্স মূল্যের ওপর ভিত্তি করে স্থানীয় অফলাইন হিসাব। আসল দৈনিক হাটের দাম ভিন্ন হতে পারে।",
                "Uses offline DAM/WFP reference baselines (0.01 BDT/kg/km transport deduction). Confirm live daily haat rates locally."
            ),
            tone = "info"
        )

        GTCard {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(tr(lang, "প্রশ্ন বা ফসল/জেলা/অফার লিখুন", "Ask crop price, district, or offer")) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == AppLang.BN) "bn-BD" else "en-US")
                                }
                                voiceLauncher.launch(intent)
                            }
                        },
                        modifier = Modifier.testTag("market_voice_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = tr(lang, "কণ্ঠে বলুন", "Voice input"), tint = c.lime)
                    }
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("market_query_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = onQtyChange,
                    label = { Text(tr(lang, "পরিমাণ (কেজি)", "Quantity (kg)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("market_qty_input")
                )
                Button(
                    onClick = { onAnalyze(null) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("market_analyze_button")
                ) {
                    Text(tr(lang, "যাচাই করুন", "Check Price"))
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val examples = listOf(
                    "বগুড়ায় আলু বিক্রি করব, দালাল ১৮ টাকা কেজি বলছে" to "Potato in Bogura, middleman offers 18 tk/kg",
                    "পাবনায় পেঁয়াজের দাম কত?" to "Onion market prices around Pabna",
                    "ঢাকায় টমেটো ৭৫০ টাকা মণ অফার পেয়েছি" to "Tomato offer 750 tk per maund near Dhaka",
                    "ওজনে কম দিলে আমার অধিকার কী?" to "What are my rights if a buyer cheats on weight?"
                )
                examples.forEachIndexed { i, (bnE, enE) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val chosen = tr(lang, bnE, enE)
                            onQueryChange(chosen)
                            onAnalyze(chosen)
                        },
                        label = { Text(tr(lang, bnE, enE)) },
                        modifier = Modifier.testTag("market_chip_$i")
                    )
                }
            }
        }

        // Answer / Offer Verdict Card
        GTCard(accentBorder = c.lime.copy(alpha = 0.55f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (analysis.mode == "rights") tr(lang, "কৃষকের বিপণন অধিকার", "MARKETING RIGHTS")
                    else "${tr(lang, analysis.cropBn ?: "বাজার তথ্য", analysis.cropEn ?: "Market Info")} ${analysis.districtBn?.let { "• ${tr(lang, it, analysis.districtEn ?: it)}" } ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink
                )
                analysis.verdictBn?.let { vBn ->
                    val tone = when (analysis.verdictEn) {
                        "Unfairly Low" -> "bad"
                        "Slightly Below Fair" -> "caution"
                        else -> "go"
                    }
                    StatusBadge(text = tr(lang, vBn, analysis.verdictEn ?: vBn), tone = tone)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(tr(lang, analysis.answerBn, analysis.answerEn), style = MaterialTheme.typography.bodyLarge, color = c.ink)

            analysis.warningsBn.forEachIndexed { idx, wBn ->
                Spacer(Modifier.height(6.dp))
                Text("⚠ ${tr(lang, wBn, analysis.warningsEn.getOrElse(idx) { wBn })}", color = c.warn, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (analysis.fairFarmGate != null) {
            val fair = analysis.fairFarmGate
            val whole = analysis.wholesalePrice ?: fair
            val retail = analysis.retailPrice ?: whole
            val maxBar = maxOf(fair, whole, retail, analysis.offerPerKg ?: 0.0).coerceAtLeast(1.0)

            GTCard {
                Text(tr(lang, "দাম তুলনা (প্রতি কেজি)", "PRICE LADDER (BDT / KG)"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                Spacer(Modifier.height(10.dp))
                analysis.offerPerKg?.let { offer ->
                    MetricGaugeBar(
                        label = tr(lang, "দালালের অফার", "Middleman Offer"),
                        valueText = "${BnFormat.num(offer, 1, lang)} ৳/kg",
                        fraction = (offer / maxBar).toFloat(),
                        color = if (offer < fair) c.bad else c.mint
                    )
                    Spacer(Modifier.height(6.dp))
                }
                MetricGaugeBar(
                    label = tr(lang, "ন্যায্য খামার-দাম (${analysis.basisBn})", "Fair Farm-Gate (${analysis.basisEn})"),
                    valueText = "${BnFormat.num(fair, 1, lang)} ৳/kg",
                    fraction = (fair / maxBar).toFloat(),
                    color = c.lime
                )
                Spacer(Modifier.height(6.dp))
                MetricGaugeBar(
                    label = tr(lang, "পাইকারি দাম", "Wholesale Ref."),
                    valueText = "${BnFormat.num(whole, 1, lang)} ৳/kg",
                    fraction = (whole / maxBar).toFloat(),
                    color = c.mint
                )
                Spacer(Modifier.height(6.dp))
                MetricGaugeBar(
                    label = tr(lang, "খুচরা দাম", "Retail Ref."),
                    valueText = "${BnFormat.num(retail, 1, lang)} ৳/kg",
                    fraction = (retail / maxBar).toFloat(),
                    color = c.blue
                )

                if (analysis.totalLoss != null && analysis.totalLoss > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        tr(
                            lang,
                            "${BnFormat.num(analysis.quantityKg, lang)} কেজিতে মোট লোকসান ≈ ${BnFormat.money(analysis.totalLoss, lang)} (দালালের মার্জিন ${analysis.middlemanMarginPct}%)",
                            "Total estimated loss on ${analysis.quantityKg.toInt()} kg: ${analysis.totalLoss} BDT (buyer margin ${analysis.middlemanMarginPct}%)"
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFont),
                        color = c.bad,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (analysis.betterMarkets.isNotEmpty()) {
            GTCard {
                Text(tr(lang, "কাছাকাছি ভালো বাজার (পরিবহন বাদে নিট দাম)", "NEARBY MARKETS (NET OF TRANSPORT)"), style = MaterialTheme.typography.labelMedium, color = c.lime)
                if (analysis.routeNoteBn.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(tr(lang, analysis.routeNoteBn, analysis.routeNoteEn), style = MaterialTheme.typography.bodyMedium, color = c.mute)
                }
                Spacer(Modifier.height(8.dp))
                analysis.betterMarkets.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tr(lang, m.districtBn, m.districtEn), style = MaterialTheme.typography.titleMedium, color = c.ink)
                            Text(
                                tr(
                                    lang,
                                    if (m.distanceKm > 0) "দূরত্ব ~${BnFormat.num(m.distanceKm, lang)} কিমি • পাইকারি ${BnFormat.num(m.wholesalePrice, 1, lang)} ৳"
                                    else "পাইকারি রেফারেন্স: ${BnFormat.num(m.wholesalePrice, 1, lang)} ৳",
                                    if (m.distanceKm > 0) "~${m.distanceKm} km away • Wholesale ${m.wholesalePrice} BDT"
                                    else "Wholesale ref: ${m.wholesalePrice} BDT"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.mute
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${BnFormat.num(m.estNetPrice, 1, lang)} ৳/kg", style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont), color = c.mint)
                            Text("+${BnFormat.num(m.gainVsLocal, 1, lang)} ৳", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont), color = c.lime)
                        }
                    }
                    HorizontalDivider(color = c.line)
                }
            }
        }

        // Farmer Rights & Fair Trading Rules (DamSathi)
        GTCard(accentBorder = c.mint.copy(alpha = 0.4f)) {
            Text(tr(lang, "কৃষকের বিপণন অধিকার ও ন্যায্য বাণিজ্য", "FARMER RIGHTS & BUYER RULES"), style = MaterialTheme.typography.labelMedium, color = c.mint)
            Spacer(Modifier.height(6.dp))
            Text(
                tr(
                    lang,
                    "• ডিজিটাল স্কেলে সঠিক ওজন চাওয়া ও মণপ্রতি অবৈধ 'ঢলতা/ধলতা' বাদ দিতে অস্বীকার করা আপনার আইনি অধিকার।\n• কৃষি বিপণন অধিদপ্তর (DAM) ও ভোক্তা অধিকার হটলাইন ১৬১২১-এ যেকোনো অনিয়মের অভিযোগ করা যায়।",
                    "• You have the right to standardized digital weighing and to refuse illegal extra weight deductions (dhalta) per maund.\n• Report unfair market syndicates or price manipulation via local DAM office or hotline 16121."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = c.ink
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val rightsTopics = listOf(
                    "ওজনে কারচুপি করলে কী করব?" to "What if buyer cheats on weight?",
                    "দালালের অতিরিক্ত কমিশন নিয়ম" to "What are rules on middleman commission?",
                    "ন্যায্য সরকারি দাম কোথায় পাব?" to "How to verify official DAM crop rates?"
                )
                rightsTopics.forEach { (rBn, rEn) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            val q = tr(lang, rBn, rEn)
                            onQueryChange(q)
                            onAnalyze(q)
                        },
                        label = { Text(tr(lang, rBn, rEn)) }
                    )
                }
            }
        }
    }
}
