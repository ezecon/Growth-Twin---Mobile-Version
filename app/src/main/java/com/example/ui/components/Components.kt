package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TelemetryReadingEntity
import com.example.domain.CropStage
import com.example.domain.DailyLogEntry
import com.example.domain.IrrigationSample
import com.example.domain.TrustHistogram
import com.example.ui.theme.AppLang
import com.example.ui.theme.BnFormat
import com.example.ui.theme.GTTheme
import com.example.ui.theme.MonoFont
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun GTCard(
    modifier: Modifier = Modifier,
    accentBorder: Color? = null,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = GTTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = c.surfaceCard,
        border = BorderStroke(1.dp, accentBorder ?: c.line),
        tonalElevation = if (c.isDark) 0.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding.dp), content = content)
    }
}

@Composable
fun SectionHeader(
    kicker: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = kicker.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = c.lime
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = c.ink
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.mute
            )
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    tone: String = "ok", // go | ok | caution | warn | no | bad | info
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    val (bg, fg) = when (tone) {
        "go", "ok", "valid" -> c.mintSoft to c.mint
        "caution", "warn", "stale", "action" -> c.warnSoft to c.warn
        "no", "bad", "critical", "suspect", "dead" -> c.badSoft to c.bad
        else -> c.limeSoft to c.lime
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NoticeBanner(
    title: String,
    body: String,
    tone: String = "warn",
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    val (bg, border, fg) = when (tone) {
        "ok" -> Triple(c.mintSoft, c.mint.copy(alpha = 0.4f), c.mint)
        "bad" -> Triple(c.badSoft, c.bad.copy(alpha = 0.4f), c.bad)
        else -> Triple(c.warnSoft, c.warn.copy(alpha = 0.4f), c.warn)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.ink)
        }
    }
}

@Composable
fun StageBar(
    stages: List<CropStage>,
    currentDay: Int,
    durationDays: Int,
    lang: AppLang,
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    val progress = (currentDay.toFloat() / durationDays.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            stages.forEachIndexed { idx, st ->
                val startFrac = if (idx == 0) 0f else stages[idx - 1].end.toFloat()
                val endFrac = st.end.toFloat()
                val weight = max(0.08f, endFrac - startFrac)
                val filled = ((progress - startFrac) / (endFrac - startFrac).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                val active = progress in startFrac..endFrac
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.line)
                ) {
                    if (filled > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(filled)
                                .height(10.dp)
                                .background(if (active) c.lime else c.mint)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stages.forEachIndexed { idx, st ->
                val startFrac = if (idx == 0) 0f else stages[idx - 1].end.toFloat()
                val isCurrent = progress >= startFrac && (idx == stages.lastIndex || progress <= st.end.toFloat())
                if (idx == 0 || idx == stages.lastIndex || isCurrent) {
                    Text(
                        text = st.displayName(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) c.lime else c.mute,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun MetricGaugeBar(
    label: String,
    valueText: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = c.mute)
            Text(valueText, style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFont), color = c.ink)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.surfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun MoistureLiquidGauge(
    moisturePct: Double,
    emergencyBelow: Int = 20,
    irrigateBelow: Int = 35,
    stopAt: Int = 60,
    lang: AppLang,
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    val inf = rememberInfiniteTransition(label = "wave")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase"
    )
    val fillFrac = (moisturePct / 100.0).toFloat().coerceIn(0.02f, 1f)
    val liquidColor = when {
        moisturePct < emergencyBelow -> c.bad
        moisturePct < irrigateBelow -> c.warn
        else -> c.mint
    }

    Box(modifier = modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(168.dp)) {
            val r = size.minDimension / 2f - 8f
            val center = Offset(size.width / 2f, size.height / 2f)
            val circlePath = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(center.x - r, center.y - r, center.x + r, center.y + r))
            }
            drawCircle(color = c.surfaceElevated, radius = r, center = center)
            clipPath(circlePath) {
                val baseY = center.y + r - 2f * r * fillFrac
                val wavePath = Path().apply {
                    moveTo(center.x - r, center.y + r)
                    var x = center.x - r
                    while (x <= center.x + r) {
                        val normX = (x - (center.x - r)) / (2f * r)
                        val y = baseY + sin(normX * 4f * PI.toFloat() + phase) * 5f
                        lineTo(x, y)
                        x += 4f
                    }
                    lineTo(center.x + r, center.y + r)
                    close()
                }
                drawPath(
                    wavePath,
                    brush = Brush.verticalGradient(
                        listOf(liquidColor.copy(alpha = 0.75f), liquidColor.copy(alpha = 0.35f))
                    )
                )
            }
            // Threshold marker for stopAt & irrigateBelow
            val trigY = center.y + r - 2f * r * (irrigateBelow / 100f)
            drawLine(c.warn.copy(alpha = 0.7f), Offset(center.x - r * 0.7f, trigY), Offset(center.x + r * 0.7f, trigY), strokeWidth = 2f)
            val stopY = center.y + r - 2f * r * (stopAt / 100f)
            drawLine(c.lime.copy(alpha = 0.7f), Offset(center.x - r * 0.7f, stopY), Offset(center.x + r * 0.7f, stopY), strokeWidth = 2f)
            drawCircle(color = liquidColor.copy(alpha = 0.8f), radius = r, center = center, style = Stroke(width = 4f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${BnFormat.num(moisturePct, 1, lang)}%",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFont, fontWeight = FontWeight.Bold),
                color = c.ink
            )
            Text(
                text = when {
                    moisturePct < emergencyBelow -> if (lang == AppLang.BN) "জরুরি" else "CRITICAL"
                    moisturePct < irrigateBelow -> if (lang == AppLang.BN) "শুষ্ক" else "DRY"
                    moisturePct >= stopAt -> if (lang == AppLang.BN) "পর্যাপ্ত ভেজা" else "WET"
                    else -> if (lang == AppLang.BN) "স্বাভাবিক" else "OPTIMAL"
                },
                style = MaterialTheme.typography.labelSmall,
                color = liquidColor
            )
        }
    }
}

@Composable
fun PumpFlowPipe(
    pumpOn: Boolean,
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    val inf = rememberInfiniteTransition(label = "pumpFlow")
    val flowOffset by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "flow"
    )
    val spinDeg by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    Canvas(modifier = modifier.fillMaxWidth().height(54.dp)) {
        val midY = size.height / 2f
        val pipeLeft = 12f
        val pipeRight = size.width - 12f
        val pipeH = 14f
        drawRoundRect(
            color = c.surfaceElevated,
            topLeft = Offset(pipeLeft, midY - pipeH / 2f),
            size = Size(pipeRight - pipeLeft, pipeH),
            cornerRadius = CornerRadius(7f, 7f)
        )
        drawRoundRect(
            color = if (pumpOn) c.mint else c.line,
            topLeft = Offset(pipeLeft, midY - pipeH / 2f),
            size = Size(pipeRight - pipeLeft, pipeH),
            cornerRadius = CornerRadius(7f, 7f),
            style = Stroke(width = 2f)
        )
        if (pumpOn) {
            val spacing = 28f
            val shift = flowOffset * spacing
            var x = pipeLeft + shift
            while (x < pipeRight - 8f) {
                drawCircle(color = c.lime, radius = 4f, center = Offset(x, midY))
                x += spacing
            }
        }
        // Central impeller hub
        val cx = size.width / 2f
        drawCircle(color = c.surfaceCard, radius = 20f, center = Offset(cx, midY))
        drawCircle(color = if (pumpOn) c.lime else c.mute, radius = 20f, center = Offset(cx, midY), style = Stroke(width = 3f))
        val angleBase = if (pumpOn) spinDeg else 0f
        for (b in 0..2) {
            val rad = Math.toRadians((angleBase + b * 120).toDouble())
            val bx = cx + cos(rad).toFloat() * 12f
            val by = midY + sin(rad).toFloat() * 12f
            drawLine(if (pumpOn) c.lime else c.mute, Offset(cx, midY), Offset(bx, by), strokeWidth = 4f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun MoistureTimeSeriesChart(
    samples: List<IrrigationSample>,
    logs: List<DailyLogEntry> = emptyList(),
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceElevated)
            .padding(12.dp)
    ) {
        val values: List<Float> = when {
            samples.size >= 2 -> samples.map { it.valuePct.toFloat() }
            logs.size >= 2 -> logs.takeLast(30).map { it.aw.toFloat() }
            else -> emptyList()
        }
        // Horizontal grid lines at 20, 35, 60, 100
        listOf(20f, 35f, 60f, 100f).forEach { pct ->
            val y = size.height - (pct / 120f) * size.height
            drawLine(c.line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        if (values.size >= 2) {
            val path = Path()
            values.forEachIndexed { idx, v ->
                val x = (idx.toFloat() / (values.size - 1)) * size.width
                val y = size.height - (v.coerceIn(0f, 120f) / 120f) * size.height
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = c.mint, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun TwinSparklineChart(
    logs: List<DailyLogEntry>,
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceElevated)
            .padding(12.dp)
    ) {
        val tail = logs.takeLast(30)
        if (tail.size >= 2) {
            val healthPath = Path()
            val awPath = Path()
            tail.forEachIndexed { idx, l ->
                val x = (idx.toFloat() / (tail.size - 1)) * size.width
                val yh = size.height - (l.health.toFloat().coerceIn(0f, 100f) / 100f) * size.height
                val ya = size.height - (l.aw.toFloat().coerceIn(0f, 130f) / 130f) * size.height
                if (idx == 0) {
                    healthPath.moveTo(x, yh)
                    awPath.moveTo(x, ya)
                } else {
                    healthPath.lineTo(x, yh)
                    awPath.lineTo(x, ya)
                }
            }
            drawPath(awPath, color = c.mint, style = Stroke(width = 3f, cap = StrokeCap.Round))
            drawPath(healthPath, color = c.lime, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun RealityGapHistoryChart(
    readings: List<TelemetryReadingEntity>,
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceElevated)
            .padding(12.dp)
    ) {
        listOf(25f, 50f, 75f, 100f).forEach { pct ->
            val y = size.height - (pct / 110f) * size.height
            drawLine(c.line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val arr = readings.reversed()
        if (arr.size >= 2) {
            val measuredPath = Path()
            val predPath = Path()
            val corrPath = Path()
            arr.forEachIndexed { idx, r ->
                val x = (idx.toFloat() / (arr.size - 1)) * size.width
                fun yFor(v: Double) = size.height - (v.toFloat().coerceIn(0f, 110f) / 110f) * size.height
                if (idx == 0) {
                    measuredPath.moveTo(x, yFor(r.calibratedMoisturePct))
                    predPath.moveTo(x, yFor(r.predictedMoisturePct))
                    corrPath.moveTo(x, yFor(r.correctedMoisturePct))
                } else {
                    measuredPath.lineTo(x, yFor(r.calibratedMoisturePct))
                    predPath.lineTo(x, yFor(r.predictedMoisturePct))
                    corrPath.lineTo(x, yFor(r.correctedMoisturePct))
                }
            }
            drawPath(predPath, color = c.blue, style = Stroke(width = 2.5f))
            drawPath(measuredPath, color = c.lime, style = Stroke(width = 2.5f))
            drawPath(corrPath, color = c.mint, style = Stroke(width = 3.5f))
        }
    }
}

@Composable
fun ProfitHistogramChart(
    hist: TrustHistogram,
    modifier: Modifier = Modifier
) {
    val c = GTTheme.colors
    val maxCount = (hist.counts.maxOrNull() ?: 1).coerceAtLeast(1)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceElevated)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        val barWidth = (size.width / hist.counts.size) * 0.72f
        val gap = (size.width / hist.counts.size) * 0.28f
        val binStep = if (hist.hi > hist.lo) (hist.hi - hist.lo).toFloat() / hist.counts.size else 1f
        hist.counts.forEachIndexed { i, count ->
            val binCenter = hist.lo + (i + 0.5f) * binStep
            val barColor = if (binCenter >= 0f) c.mint else c.bad
            val barH = (count.toFloat() / maxCount) * size.height
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = barColor.copy(alpha = 0.85f),
                topLeft = Offset(x, size.height - barH),
                size = Size(barWidth, max(4f, barH)),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}
