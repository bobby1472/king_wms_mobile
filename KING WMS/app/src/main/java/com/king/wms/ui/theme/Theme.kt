package com.king.wms.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── KING luxe palette — deep midnight navy + champagne gold ─────────────────
// Dark-first, opulent. Reads premium and stays legible in dim warehouse aisles.
val KingMidnight = Color(0xFF0A1A2F) // base background
val KingSurface = Color(0xFF102745) // cards
val KingElevated = Color(0xFF15304F) // raised cards / tiles
val KingScanWell = Color(0xFF06101E) // camera viewport well
val KingGold = Color(0xFFD4A437) // primary accent
val KingGoldSoft = Color(0xFFE7CE8F) // softer gold for text/icons on dark
val KingIvory = Color(0xFFF3EEE1) // warm off-white text
val KingMuted = Color(0xFF93A7BE) // muted cool gray-blue text
val KingLine = Color(0x47D4A437) // gold hairline border (~28% gold)
val KingGreen = Color(0xFF1B7A5A) // refined emerald (success)
val KingRedSoft = Color(0xFFE5736B) // refined error

// Kept for any older references.
val StatusGreen = KingGreen
val StatusRed = KingRedSoft
val StatusAmber = Color(0xFFE0A93B)

// Serif display family (Android's built-in serif) for the wordmark & headings —
// swap to a bundled face (e.g. Playfair Display) later for an even richer feel.
val KingSerif = FontFamily.Serif

private val LuxeColors = darkColorScheme(
    primary = KingGold,
    onPrimary = KingMidnight,
    secondary = KingGoldSoft,
    onSecondary = KingMidnight,
    tertiary = KingGreen,
    onTertiary = KingIvory,
    background = KingMidnight,
    onBackground = KingIvory,
    surface = KingSurface,
    onSurface = KingIvory,
    surfaceVariant = KingElevated,
    onSurfaceVariant = KingMuted,
    outline = KingGold,
    error = KingRedSoft,
    onError = KingMidnight,
)

private val LuxeTypography = Typography(
    displayLarge = TextStyle(fontFamily = KingSerif, fontWeight = FontWeight.Medium, fontSize = 48.sp, letterSpacing = 1.sp),
    headlineLarge = TextStyle(fontFamily = KingSerif, fontWeight = FontWeight.Medium, fontSize = 30.sp, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontFamily = KingSerif, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 2.sp),
)

@Composable
fun KingWmsTheme(
    // Luxe is intentionally dark-first; the parameter is ignored so the premium
    // look is consistent regardless of the device's day/night setting.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LuxeColors,
        typography = LuxeTypography,
        content = content,
    )
}
