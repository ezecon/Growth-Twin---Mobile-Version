package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GrowthTwinColors(
    val isDark: Boolean,
    val bg: Color,
    val bgDeep: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val lime: Color,
    val limeBright: Color,
    val limeSoft: Color,
    val onLime: Color,
    val mint: Color,
    val mintSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val bad: Color,
    val badSoft: Color,
    val blue: Color,
    val ink: Color,
    val mute: Color,
    val line: Color,
    val lineBright: Color
)

val DarkGrowthTwinColors = GrowthTwinColors(
    isDark = true,
    bg = Color(0xFF07100A),
    bgDeep = Color(0xFF030704),
    surface = Color(0xFF0A140D),
    surfaceCard = Color(0xFF0D1B11),
    surfaceElevated = Color(0xFF122819),
    lime = Color(0xFFA3F20A),
    limeBright = Color(0xFFC2FF44),
    limeSoft = Color(0x24A3F20A),
    onLime = Color(0xFF0B1A06),
    mint = Color(0xFF3DE0A0),
    mintSoft = Color(0x223DE0A0),
    warn = Color(0xFFF0C46A),
    warnSoft = Color(0x26F0C46A),
    bad = Color(0xFFF19A92),
    badSoft = Color(0x24F19A92),
    blue = Color(0xFF6FA8FF),
    ink = Color(0xFFF2F6EE),
    mute = Color(0xFFA9B8A6),
    line = Color(0x33A3F20A),
    lineBright = Color(0x66A3F20A)
)

val LightGrowthTwinColors = GrowthTwinColors(
    isDark = false,
    bg = Color(0xFFF3F7EE),
    bgDeep = Color(0xFFE6EEDF),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEAF2E1),
    lime = Color(0xFF3F8A00),
    limeBright = Color(0xFF2D6800),
    limeSoft = Color(0x1E3F8A00),
    onLime = Color(0xFFFFFFFF),
    mint = Color(0xFF0B8A5F),
    mintSoft = Color(0x1E0B8A5F),
    warn = Color(0xFF8F6200),
    warnSoft = Color(0x208F6200),
    bad = Color(0xFFB3362A),
    badSoft = Color(0x1EB3362A),
    blue = Color(0xFF2B6CB0),
    ink = Color(0xFF16240F),
    mute = Color(0xFF4D5F46),
    line = Color(0x2E286414),
    lineBright = Color(0x55286414)
)

val LocalGrowthTwinColors = staticCompositionLocalOf { DarkGrowthTwinColors }
