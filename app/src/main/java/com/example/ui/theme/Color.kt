package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------------------------
// SecureMind palette
//
// Obsidian depth + an electric mint-cyan "signal" + warm amber highlight. The theme is meant to
// read as a private, encrypted vault for reading and thinking: dark, calm, high-contrast, and
// deliberately free of the generic purple/teal template colours.
//
// Every token name below is intentionally kept identical to the previous palette so the whole app
// re-skins from this one file.
// ---------------------------------------------------------------------------------------------

// Surfaces: obsidian, layered by elevation.
val NaturalDarkBackground = Color(0xFF080B10)
val NaturalDarkSurface = Color(0xFF0F141C)
val NaturalDarkSurfaceVariant = Color(0xFF172029)
val NaturalDarkSurfaceElevated = Color(0xFF212C38)
val NaturalDarkBorder = Color(0xFF2E3B49)

// The SecureMind signal colour: a mint-cyan that stays legible as text on obsidian.
val NaturalPrimary = Color(0xFF5CE1C6)
val NaturalOnPrimary = Color(0xFF00201B)
val NaturalPrimaryContainer = Color(0xFF0C4F44)
val NaturalSecondary = Color(0xFF8FC7E8)     // Cool steel blue
val NaturalTertiary = Color(0xFFF2C879)      // Warm amber

val NaturalDarkText = Color(0xFFE6ECF2)
val NaturalDarkTextMuted = Color(0xFF8A98A8)

// Accent families (kept for compatibility with existing screens, re-tuned to the new identity).
val NaturalWarmOchre = Color(0xFFF2A93B)
val NaturalSageBg = Color(0xFF101F1B)
val NaturalSageBorder = Color(0xFF1F3A32)
val NaturalSageAccent = Color(0xFF7EE0C0)
val NaturalSageMuted = Color(0xFF5FA890)
val NaturalSageSuccess = Color(0xFF5FE0A8)
val NaturalForestAccent = Color(0xFF3ED9A5)

val NaturalOchreBg = Color(0xFF211B10)
val NaturalOchreBorder = Color(0xFF3D3220)
val NaturalOchreAccent = Color(0xFFF2C879)
val NaturalOchreMuted = Color(0xFFA08B63)

// Light theme equivalents.
val NaturalLightBackground = Color(0xFFF6F8FA)
val NaturalLightSurface = Color(0xFFFFFFFF)
val NaturalLightSurfaceVariant = Color(0xFFE9EEF3)
val NaturalLightPrimary = Color(0xFF0E7C6B)
val NaturalLightOnPrimary = Color(0xFFFFFFFF)
val NaturalLightText = Color(0xFF10151B)
val NaturalLightTextSecondary = Color(0xFF5A6672)
