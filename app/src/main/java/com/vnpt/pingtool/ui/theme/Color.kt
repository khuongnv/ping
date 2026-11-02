package com.vnpt.pingtool.ui.theme

import androidx.compose.ui.graphics.Color

// Standards from UI_UX_DESIGN_STANDARDS.md
val PrimaryAccent = Color(0xFFFA2D55)     // Apple Red
val SecondaryAccent = Color(0xFFFF375F)
val AccentPressed = Color(0xFFE02449)

val AmoledBlack = Color(0xFF000000)       // 100% AMOLED Black
val GlassSurfaceDark = Color(0xEE1E1E22)  // Liquid Glass fill (~93%)
val GlassSurfaceLight = Color(0xF2FFFFFF)

val CardSurface = Color(0xFF1C1C1E)       // iOS Dark Gray
val CardSurfaceVariant = Color(0xFF2C2C2E)

val LuminousRimBorderDark = Color(0x33FFFFFF) // 0.75dp 20% luminous rim
val LuminousRimBorderLight = Color(0x1F000000)

val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFF8E8E93)  // Gray93

val HairlineDividerDark = Color(0x14FFFFFF) // rgba(255,255,255,0.08)

// Status colors for Ping
val LatencyGood = Color(0xFF34C759)        // iOS Green (< 50ms)
val LatencyMedium = Color(0xFFFF9500)      // iOS Orange (50ms - 150ms)
val LatencyPoor = Color(0xFFFF3B30)        // iOS Red (> 150ms)
val PacketLossColor = Color(0xFFFF453A)
