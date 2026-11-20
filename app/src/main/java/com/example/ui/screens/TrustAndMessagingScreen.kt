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
import com.example.data.UserPrefs
import com.example.domain.CropCatalog
import com.example.domain.TrustReport
import com.example.ui.components.GTCard
import com.example.ui.components.NoticeBanner
import com.example.ui.components.ProfitHistogramChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.ui.viewmodel.ChannelSimMessage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrustAndMessagingScreen(
    lang: AppLang,
    prefs: UserPrefs,
    trustCropId: String,
    trustReport: TrustReport,
    channelMessages: List<ChannelSimMessage> = emptyList(),
    onSelectTrustCrop: (String) -> Unit,
    onGenerateLinkCode: () -> Unit,
    onSimulateWhatsappLink: (String) -> Unit,
    onUnlinkWhatsapp: () -> Unit,
    onSendCommand: (String) -> Unit = {}
) {
    val c = GTTheme.colors
    var phoneInput by remember { mutableStateOf("01712345678") }
    var cmdInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 4: Farmers' Trust
        SectionHeader(
            kicker = "SECTION 04 · FARMERS' TRUST",
            title = tr(lang, "কৃষকের ভরসা — বেনামী তুলনামূলক ফলাফল", "Farmers' Trust — Cohort Benchmark"),
            subtitle = tr(
                lang,
                "একই ফসলে অন্য কৃষকদের ফলাফল (ক্ষতিগ্রস্ত প্লটসহ সৎ পরিসংখ্যান)।",
                "Failure-inclusive profit & yield distribution across anonymous cohort records."
            )
        )

        NoticeBanner(
            title = tr(lang, "সিমুলেশন ও সৎ পরিসংখ্যান নীতি", "Honest Simulation Label"),
            body = tr(
                lang,
                "এই চার্টের বেসলাইন রেকর্ডগুলো সিমুলেশন ('source = simulation') এবং ব্যর্থ/মৃত প্লটও এতে অন্তর্ভুক্ত থাকে যাতে লাভ ফুলিয়ে দেখানো না হয়।",
                "Baseline cohort rows carry source='simulation' and include dead/failed plots alongside your local harvested twins."
            ),
            tone = "info"
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CropCatalog.CROPS.forEach { crop ->
                FilterChip(
                    selected = crop.id == trustCropId,
                    onClick = { onSelectTrustCrop(crop.id) },
                    label = { Text(crop.displayName(lang)) },
                    modifier = Modifier.testTag("trust_crop_${crop.id}")
                )
            }
        }

        GTCard(accentBorder = c.lime.copy(alpha = 0.5f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(tr(lang, "মোট কৃষক / প্লট", "Farmers / Plots"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                    Text("${BnFormat.num(trustReport.nFarmers, lang)} / ${BnFormat.num(trustReport.nPlots, lang)}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFont), color = c.ink)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr(lang, "লাভ করেছেন", "Profitable"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                    Text("${BnFormat.num(trustReport.profitShare * 100, lang)}%", style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFont), color = c.mint)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(tr(lang, "ব্যর্থ প্লট", "Failed"), style = MaterialTheme.typography.labelSmall, color = c.mute)
                    Text("${BnFormat.num(trustReport.failedCount, lang)}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFont), color = c.bad)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    tr(
                        lang,
                        "গড় (Median) লাভ/শতাংশ: ${BnFormat.signedMoney(trustReport.medianProfitPerDecimal, lang)}",
                        "Median profit/dec: ${BnFormat.signedMoney(trustReport.medianProfitPerDecimal, lang)}"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = c.lime
                )
                trustReport.medianYieldPerDecimal?.let { y ->
                    Text(
                        tr(lang, "${BnFormat.num(y, 1, lang)} কেজি/শতক", "$y kg/dec"),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont),
                        color = c.ink
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                tr(
                    lang,
                    "লাভ বণ্টন হিস্টোগ্রাম (${BnFormat.num(trustReport.hist.lo, lang)} ৳ থেকে ${BnFormat.num(trustReport.hist.hi, lang)} ৳ / শতাংশ)",
                    "8-Bin Profit Histogram (${trustReport.hist.lo} to ${trustReport.hist.hi} BDT/dec)"
                ),
                style = MaterialTheme.typography.labelSmall,
                color = c.mute
            )
            Spacer(Modifier.height(6.dp))
            ProfitHistogramChart(hist = trustReport.hist)
            Spacer(Modifier.height(8.dp))
            Text(
                tr(
                    lang,
                    "২৫–৭৫ পার্সেন্টাইল: ${BnFormat.money(trustReport.p25, lang)} – ${BnFormat.money(trustReport.p75, lang)} • সক্রিয় প্লট: ${BnFormat.num(trustReport.growingNow, lang)}",
                    "IQR (P25–P75): ${trustReport.p25}–${trustReport.p75} BDT • Growing now: ${trustReport.growingNow}"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = c.mute
            )
        }

        if (trustReport.mine.isNotEmpty()) {
            GTCard(accentBorder = c.mint.copy(alpha = 0.5f)) {
                Text(tr(lang, "আমার শেষ করা প্লটের অবস্থান", "YOUR COMPLETED PLOTS"), style = MaterialTheme.typography.labelMedium, color = c.mint)
                Spacer(Modifier.height(8.dp))
                trustReport.mine.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${m.startDate} • ${m.outcome}", color = c.ink, fontWeight = FontWeight.SemiBold)
                            Text("${BnFormat.signedMoney(m.profitPerDecimal, lang)}/dec (${m.yieldPerDecimal} kg/dec)", color = c.mute, style = MaterialTheme.typography.bodyMedium)
                        }
                        m.betterThanShare?.let { bt ->
                            StatusBadge(
                                text = tr(lang, "${(bt * 100).toInt()}% কৃষকের চেয়ে ভালো", "Better than ${(bt * 100).toInt()}%"),
                                tone = if (bt >= 0.5) "go" else "caution"
                            )
                        }
                    }
                    HorizontalDivider(color = c.line)
                }
            }
        }

        HorizontalDivider(color = c.line)

        // SECTION 3: WhatsApp & SMS Offline Guide
        SectionHeader(
            kicker = "SECTION 03 · WHATSAPP & SMS",
            title = tr(lang, "হোয়াটসঅ্যাপ ও এসএমএস সংযোগ গাইড", "WhatsApp & SMS Channel Guide"),
            subtitle = tr(
                lang,
                "কম নেটওয়ার্ক এলাকায় টেক্সট ও পাতার ছবি পাঠিয়ে পরামর্শ পাওয়ার কমান্ড রেফারেন্স (অফলাইন সিমুলেশন)।",
                "Offline channel preview & command reference for WhatsApp/SMS farmer check-ins."
            )
        )

        GTCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lang, "সংযোগ অবস্থা (ডেমো)", "Link Status (Local Demo)"), style = MaterialTheme.typography.titleMedium, color = c.ink)
                StatusBadge(
                    text = if (prefs.whatsappDemoLinked) tr(lang, "সংযুক্ত (${prefs.whatsappPhone})", "Linked (${prefs.whatsappPhone})") else tr(lang, "সংযুক্ত নয়", "Unlinked"),
                    tone = if (prefs.whatsappDemoLinked) "go" else "caution"
                )
            }
            Spacer(Modifier.height(10.dp))
            if (prefs.whatsappLinkCode.isNotEmpty()) {
                Text(
                    "LINK ${prefs.whatsappLinkCode}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFont),
                    color = c.lime
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onGenerateLinkCode,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("msg_gen_code")
                ) { Text(tr(lang, "লিংক কোড তৈরি", "Generate Code")) }
                if (prefs.whatsappDemoLinked) {
                    OutlinedButton(
                        onClick = onUnlinkWhatsapp,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("msg_unlink")
                    ) { Text(tr(lang, "সংযোগ বিচ্ছিন্ন", "Unlink")) }
                }
            }
            if (!prefs.whatsappDemoLinked) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = phoneInput, onValueChange = { phoneInput = it },
                        label = { Text(tr(lang, "মোবাইল নম্বর", "Mobile Number")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("msg_phone_input")
                    )
                    OutlinedButton(
                        onClick = { onSimulateWhatsappLink(phoneInput) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("msg_demo_link")
                    ) { Text(tr(lang, "ডেমো সংযোগ", "Link Demo")) }
                }
            }
        }

        GTCard {
            Text(tr(lang, "সমর্থিত হোয়াটসঅ্যাপ ও এসএমএস কমান্ড", "SUPPORTED COMMANDS"), style = MaterialTheme.typography.labelMedium, color = c.lime)
            Spacer(Modifier.height(8.dp))
            val cmds = listOf(
                "LINK <code>" to tr(lang, "ফোন নম্বর একাউন্টের সাথে যুক্ত করতে", "Link phone number to farmer account"),
                "অবস্থা / STATUS" to tr(lang, "সক্রিয় প্লটের আজকের অবস্থা জানতে", "Get active plot health, moisture & stage"),
                "সেচ / IRRIGATE" to tr(lang, "সেচের পরামর্শ বা স্ট্যাটাস দেখতে", "Check irrigation rule & pump state"),
                "[পাতার ছবি]" to tr(lang, "হোয়াটসঅ্যাপে পাতার ছবি পাঠিয়ে রোগের প্রাথমিক ধারণা পেতে", "Send leaf photo for advisory disease triage")
            )
            cmds.forEach { (cmd, desc) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cmd, style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont), color = c.lime)
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = c.mute, modifier = Modifier.padding(start = 12.dp))
                }
                HorizontalDivider(color = c.line)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                tr(
                    lang,
                    "সীমা: জরুরি SMS সর্বোচ্চ ২০০ অক্ষর (ইউনিকোড) ও ঘণ্টায় সর্বোচ্চ ৩০ বার্তা। অফলাইন মোডে বাস্তব SMS পাঠানো হয় না।",
                    "Limits: Bangla SMS capped at 200 Unicode chars, rate-limited at 30 msgs/hr. Offline Android build simulates messaging locally."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = c.mute
            )
        }

        // Interactive WhatsApp / SMS Command Simulator (offline)
        GTCard(accentBorder = c.mint.copy(alpha = 0.5f)) {
            Text(tr(lang, "হোয়াটসঅ্যাপ / এসএমএস কমান্ড সিমুলেটর", "CHANNEL BOT SIMULATOR"), style = MaterialTheme.typography.labelMedium, color = c.mint)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                channelMessages.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (m.fromFarmer) Arrangement.End else Arrangement.Start
                    ) {
                        Text(
                            text = m.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.ink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (m.fromFarmer) c.limeSoft else c.surfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    tr(lang, "অবস্থা", "STATUS"),
                    tr(lang, "সেচ", "IRRIGATE"),
                    tr(lang, "দাম আলু", "PRICE potato")
                ).forEach { quick ->
                    FilterChip(
                        selected = false,
                        onClick = { onSendCommand(quick) },
                        label = { Text(quick) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = cmdInput,
                    onValueChange = { cmdInput = it },
                    label = { Text(tr(lang, "কমান্ড লিখুন", "Type command")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("msg_cmd_input")
                )
                Button(
                    onClick = {
                        onSendCommand(cmdInput)
                        cmdInput = ""
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("msg_cmd_send")
                ) {
                    Text(tr(lang, "পাঠান", "Send"))
                }
            }
        }
    }
}
