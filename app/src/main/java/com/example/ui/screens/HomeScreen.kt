package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import com.example.R
import com.example.data.LiveWeatherSnapshot
import com.example.domain.CropCatalog
import com.example.domain.PlotState
import com.example.domain.PlotTwinEngine
import com.example.domain.SentinelEngine
import com.example.ui.components.GTCard
import com.example.ui.components.NoticeBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StageBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import com.example.ui.theme.tr
import kotlinx.coroutines.delay

object Routes {
    const val HOME = "home"
    const val ADVISOR = "advisor"
    const val TWIN = "twin"
    const val IRRIGATION = "irrigation"
    const val SENTINEL = "sentinel"
    const val SOLARFLOW = "solarflow"
    const val MARKET = "market"
    const val TRUST = "trust"
    const val EXPERT = "expert"
}

private data class HeroSlide(
    val kicker: String,
    val titleBn: String,
    val titleEn: String,
    val bodyBn: String,
    val bodyEn: String,
    val locationBn: String,
    val locationEn: String,
    val route: String
)

private data class QuickTile(
    val numTag: String,
    val route: String,
    val icon: ImageVector,
    val titleBn: String,
    val titleEn: String,
    val subBn: String,
    val subEn: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    lang: AppLang,
    activePlot: PlotState?,
    userRole: String,
    sentinelScenarioId: String = "fresh",
    telemetryCount: Int = 0,
    liveWeather: LiveWeatherSnapshot? = null,
    serverConnected: Boolean = false,
    onRefreshWeather: () -> Unit = {},
    onSelectSentinelScenario: (String) -> Unit = {},
    onNavigate: (String) -> Unit
) {
    val c = GTTheme.colors
    var slideIdx by remember { mutableIntStateOf(0) }
    var homeSentinelMode by remember { mutableStateOf("sim") }
    val scenarios = remember { SentinelEngine.demoScenarios() }
    val activeScenario = scenarios.firstOrNull { it.id == sentinelScenarioId } ?: scenarios.first()

    val slides = remember {
        listOf(
            HeroSlide(
                "01 · DIGITAL PLOT TWIN",
                "প্রতিটি জমির জন্য কৃষি বুদ্ধিমত্তা",
                "Living digital twin for every field",
                "আবহাওয়া, মাটির আর্দ্রতা, pH ও রোগের ঝুঁকি বুঝে প্রতিদিনের সিদ্ধান্ত নিন।",
                "Track soil water, N/P/K balance, heat/rain stress, and harvest profits daily.",
                "বাংলাদেশ · ডিজিটাল ফিল্ড", "Bangladesh · Digital Field",
                Routes.TWIN
            ),
            HeroSlide(
                "02 · SENTINEL + IRRIGATION",
                "স্বয়ংক্রিয় সেচ ও সেন্সর যাচাই",
                "Trust-aware sensor telemetry",
                "বাস্তব ও সিমুলেটেড আর্দ্রতা আলাদা রাখুন — বৃষ্টির পূর্বাভাস ও দৈনিক পাম্প সীমাসহ।",
                "Reality Gap filters stale/flatlined probes and pauses pumps before rainstorms.",
                "সেচ ও সেন্টিনেল মনিটর", "Irrigation & Sentinel Monitor",
                Routes.SENTINEL
            ),
            HeroSlide(
                "03 · SOLARFLOW & DAMSATHI",
                "সৌর সেচ ও ন্যায্য বাজার দাম",
                "Solar irrigation & fair market prices",
                "সোলার বনাম ব্যাটারি পাম্পিং ও দালালের অফার বনাম জেলার ন্যায্য দাম হিসাব করুন।",
                "Compare solar/battery irrigation runs and audit buyer offers across Bangladesh districts.",
                "সোলারফ্লো · দামসাথী", "SolarFlow · Fair Market",
                Routes.MARKET
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L)
            slideIdx = (slideIdx + 1) % slides.size
        }
    }

    val slide = slides[slideIdx]
    val tiles = remember(userRole) {
        buildList {
            add(QuickTile("01", Routes.ADVISOR, Icons.Default.Lightbulb, "চাষের পরামর্শ", "Crop Advisor", "৩ দৃশ্যপটে লাভ-ক্ষতি ও ফসল বদল", "3-scenario profit & rotation checks"))
            add(QuickTile("02", Routes.TWIN, Icons.Default.Eco, "প্লট টুইন", "Plot Twin", "দৈনিক সিমুলেশন, পরীক্ষাগার ও চেক-ইন", "Daily crop twin, Experiment Lab & check-in"))
            add(QuickTile("2B", Routes.IRRIGATION, Icons.Default.WaterDrop, "সেচ কন্ট্রোল", "Irrigation Control", "লাইভ গেজ, পাম্প ও ৯০মি সীমা", "Live wave gauge, pump & auto-schedule"))
            add(QuickTile("S1", Routes.SENTINEL, Icons.Default.Sensors, "সেন্টিনেল মনিটর", "Sentinel Monitor", "ফ্রেশ/স্টেল/সাসপেক্ট ও রিয়েলিটি গ্যাপ", "Probe calibration & Reality Gap telemetry"))
            add(QuickTile("SF", Routes.SOLARFLOW, Icons.Default.WbSunny, "সোলারফ্লো AI", "SolarFlow AI", "সোলার বনাম ব্যাটারি সেচ র‍্যাঙ্কিং", "Water-energy pump decision optimizer"))
            add(QuickTile("FM", Routes.MARKET, Icons.Default.Storefront, "দামসাথী বাজার", "Fair Market", "ন্যায্য দাম, পরিবহন লাভ ও অধিকার", "District fair prices & trading rights"))
            add(QuickTile("03", Routes.TRUST, Icons.AutoMirrored.Filled.Chat, "WhatsApp ও ভরসা", "Trust & Messaging", "বেনামী কোহর্ট ও অফলাইন SMS কমান্ড", "Honest crop cohort & SMS/WhatsApp bot"))
            if (userRole == "expert" || userRole == "admin") {
                add(QuickTile("EX", Routes.EXPERT, Icons.Default.Shield, "বিশেষজ্ঞ / অ্যাডমিন", "Expert / Admin", "পরামর্শ ও পাতার ছবির রিভিউ কিউ", "Advisor & leaf photo review queues"))
            } else {
                add(QuickTile("EX", Routes.EXPERT, Icons.Default.Groups, "বিশেষজ্ঞ কনসোল", "Expert Console", "রিভিউ কিউ দেখতে ভূমিকা বদলান", "Switch role to inspect expert review queue"))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner matching GitHub index.html (.hero + .foot + #dots)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = c.surfaceCard,
            border = BorderStroke(1.dp, c.lineBright),
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 256.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_field),
                    contentDescription = tr(lang, "সবুজ ধানক্ষেতের ডিজিটাল টুইন ব্যানার", "Lush paddy field banner"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x9907100A),
                                Color(0xEC07100A)
                            )
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x2EA3F20A), Color.Transparent),
                            center = Offset(size.width * 0.86f, size.height * 0.18f),
                            radius = size.width * 0.5f
                        )
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = slide.kicker,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                        color = Color(0xFFA3F20A)
                    )
                    Text(
                        text = tr(lang, slide.titleBn, slide.titleEn),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFFF2F6EE)
                    )
                    Text(
                        text = tr(lang, slide.bodyBn, slide.bodyEn),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD2DDD0)
                    )
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = { onNavigate(slide.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA3F20A),
                            contentColor = Color(0xFF0B1A06)
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("hero_cta_button")
                    ) {
                        Text(tr(lang, "মডিউল খুলুন", "Open Module"), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Color(0x33A3F20A))
                    // GitHub index.html .foot row: LOCATION + slide dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LOCATION",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFont),
                                color = Color(0xFFA9B8A6)
                            )
                            Text(
                                text = tr(lang, slide.locationBn, slide.locationEn),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFF2F6EE)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            slides.indices.forEach { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(if (idx == slideIdx) 12.dp else 9.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == slideIdx) Color(0xFFA3F20A) else Color(0x66FFFFFF))
                                        .clickable { slideIdx = idx }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Weather & Real API Telemetry Strip (Open-Meteo + Backend status)
        GTCard(accentBorder = c.mint.copy(alpha = 0.45f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = c.mint)
                    Column {
                        Text(
                            text = "LIVE WEATHER & API",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.mint
                        )
                        Text(
                            text = if (liveWeather != null) {
                                "${liveWeather.locationName} • ${BnFormat.num(liveWeather.currentTempC, 1, lang)}°C • RH ${liveWeather.currentRh}% • Rain ${BnFormat.num(liveWeather.currentRainMm, 1, lang)}mm"
                            } else {
                                tr(lang, "লাইভ আবহাওয়া API লোড হচ্ছে...", "Tap refresh for live Bangladesh weather API")
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFont),
                            color = c.ink
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusBadge(
                        text = if (serverConnected) "SERVER API" else if (liveWeather != null) "LIVE API" else "LOCAL",
                        tone = if (serverConnected || liveWeather != null) "go" else "info"
                    )
                    TextButton(
                        onClick = onRefreshWeather,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("home_refresh_weather")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = tr(lang, "রিফ্রেশ", "Refresh"))
                    }
                }
            }
        }

        // Inline Sentinel Monitor Preview (matching #sentinel-monitor right under .hero in GitHub index.html)
        GTCard(accentBorder = c.lineBright) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "SENTINEL · DATA SOURCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.lime
                    )
                    Text(
                        text = tr(lang, "বিশ্বাসযোগ্য সেন্সর মনিটর", "Trust-aware sensor monitor"),
                        style = MaterialTheme.typography.titleLarge,
                        color = c.ink
                    )
                }
                TextButton(
                    onClick = { onNavigate(Routes.SENTINEL) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("home_open_sentinel")
                ) {
                    Text(tr(lang, "পূর্ণ প্যানেল →", "Full Panel →"))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = homeSentinelMode == "sim",
                    onClick = { homeSentinelMode = "sim" },
                    label = { Text(tr(lang, "সিমুলেশন", "Simulation")) },
                    modifier = Modifier.testTag("home_sentinel_sim")
                )
                FilterChip(
                    selected = homeSentinelMode == "real",
                    onClick = { homeSentinelMode = "real" },
                    label = { Text(tr(lang, "রিয়েল সেন্সর", "Real sensor")) },
                    modifier = Modifier.testTag("home_sentinel_real")
                )
            }
            Spacer(Modifier.height(8.dp))
            if (homeSentinelMode == "sim") {
                NoticeBanner(
                    title = tr(lang, "ডেমো মোড", "Demo mode"),
                    body = tr(
                        lang,
                        "নিচের Fresh, Stale ও Suspect উদাহরণগুলো সিমুলেটেড। এগুলো হার্ডওয়্যার পাম্প চালায় না।",
                        "Fresh, Stale, and Suspect scenarios below are simulated examples and do not control hardware."
                    ),
                    tone = "warn"
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    scenarios.forEach { sc ->
                        FilterChip(
                            selected = sc.id == activeScenario.id,
                            onClick = { onSelectSentinelScenario(sc.id) },
                            label = { Text(tr(lang, sc.labelBn, sc.labelEn)) }
                        )
                    }
                }
                val pred = activePlot?.awPct() ?: 40.0
                val est = SentinelEngine.correctState(pred, activeScenario.calibratedPct, quality = activeScenario.quality)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tr(lang, activeScenario.summaryBn, activeScenario.summaryEn),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.mute
                        )
                        Text(
                            "ADC ${activeScenario.rawAdc} · ${activeScenario.calibratedPct}% → ${BnFormat.num(est.estimatedValue, 1, lang)}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont),
                            color = c.ink
                        )
                    }
                    StatusBadge(text = activeScenario.quality.uppercase(), tone = activeScenario.quality)
                }
            } else {
                NoticeBanner(
                    title = tr(lang, "রিয়েল সেন্সর মোড", "Real sensor mode"),
                    body = tr(
                        lang,
                        "সংরক্ষিত ডিভাইস টেলিমেট্রি: ${BnFormat.num(telemetryCount, lang)}টি রিডিং। ক্যালিব্রেশন বা টেস্ট প্যাকেট পাঠাতে পূর্ণ প্যানেলে যান।",
                        "Stored device telemetry packets: $telemetryCount. Open Sentinel panel to calibrate ADC or ingest readings."
                    ),
                    tone = "ok"
                )
            }
        }

        // Live Twin Quick Card
        if (activePlot != null) {
            val crop = CropCatalog.BY_ID[activePlot.cropId]
            val stages = CropCatalog.stagesOf(activePlot.cropId)
            val (_, stage) = CropCatalog.stageAt(activePlot.cropId, activePlot.day, crop?.durationDays ?: 120)
            val advice = PlotTwinEngine.buildAdvice(activePlot).firstOrNull()
            GTCard(accentBorder = c.lime.copy(alpha = 0.55f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(lang, "02 · সক্রিয় প্লট টুইন", "02 · ACTIVE PLOT TWIN"),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.lime
                        )
                        Text(
                            text = "${activePlot.name} • ${crop?.displayName(lang) ?: activePlot.cropId}",
                            style = MaterialTheme.typography.titleLarge,
                            color = c.ink
                        )
                    }
                    StatusBadge(
                        text = tr(
                            lang,
                            "দিন ${BnFormat.num(activePlot.day, lang)}/${BnFormat.num(crop?.durationDays ?: 120, lang)}",
                            "Day ${activePlot.day}/${crop?.durationDays ?: 120}"
                        ),
                        tone = if (activePlot.status == "dead") "bad" else "go"
                    )
                }
                Spacer(Modifier.height(10.dp))
                StageBar(stages, activePlot.day, crop?.durationDays ?: 120, lang)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatPill(
                        label = tr(lang, "পর্যায়", "Stage"),
                        value = stage.displayName(lang),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatPill(
                        label = tr(lang, "স্বাস্থ্য", "Health"),
                        value = "${BnFormat.num(activePlot.health, lang)}%",
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatPill(
                        label = tr(lang, "আর্দ্রতা", "Moisture"),
                        value = "${BnFormat.num(activePlot.awPct(), lang)}%",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (advice != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${advice.icon} ${tr(lang, advice.titleBn, advice.titleEn)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.mint,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onNavigate(Routes.TWIN) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("home_open_twin")
                    ) {
                        Text(tr(lang, "টুইন খুলুন", "Open Twin"))
                    }
                    OutlinedButton(
                        onClick = { onNavigate(Routes.IRRIGATION) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("home_open_irrigation")
                    ) {
                        Text(tr(lang, "সেচ দেখুন", "Irrigation"))
                    }
                }
            }
        }

        SectionHeader(
            kicker = "MODULES · 01 — 04",
            title = tr(lang, "সবগুলো কৃষি সেবা", "All GrowthTwin Modules"),
            subtitle = tr(lang, "চাষের পরিকল্পনা থেকে বাজারদর — প্রতিটি ধাপে স্বচ্ছ হিসাব", "Transparent rule engines and simulations across every farming decision")
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tiles.forEach { tile ->
                Surface(
                    onClick = { onNavigate(tile.route) },
                    shape = MaterialTheme.shapes.large,
                    color = c.surfaceCard,
                    border = BorderStroke(1.dp, c.line),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 128.dp)
                        .testTag("home_tile_${tile.route}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c.limeSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(tile.icon, contentDescription = tr(lang, tile.titleBn, tile.titleEn), tint = c.lime)
                            }
                            Text(
                                text = tile.numTag,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFont),
                                color = c.lime
                            )
                        }
                        Text(
                            text = tr(lang, tile.titleBn, tile.titleEn),
                            style = MaterialTheme.typography.titleMedium,
                            color = c.ink
                        )
                        Text(
                            text = tr(lang, tile.subBn, tile.subEn),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.mute
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MiniStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    val c = GTTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.mute)
        Text(value, style = MaterialTheme.typography.labelLarge, color = c.ink, maxLines = 1)
    }
}
