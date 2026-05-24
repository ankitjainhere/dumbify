package com.dumbify.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Forest-green dark scheme ─────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary                = Color(0xFF88D993),
    onPrimary              = Color(0xFF003912),
    primaryContainer       = Color(0xFF1F5026),
    onPrimaryContainer     = Color(0xFFA4F5AE),
    secondary              = Color(0xFFB5CCB5),
    onSecondary            = Color(0xFF213524),
    secondaryContainer     = Color(0xFF374B39),
    onSecondaryContainer   = Color(0xFFD1E8D0),
    error                  = Color(0xFFFFB4AB),
    onError                = Color(0xFF690005),
    errorContainer         = Color(0xFF93000A),
    onErrorContainer       = Color(0xFFFFDAD6),
    background             = Color(0xFF0F140F),
    onBackground           = Color(0xFFE0E4DC),
    surface                = Color(0xFF0F140F),
    onSurface              = Color(0xFFE0E4DC),
    surfaceContainerLowest = Color(0xFF0A0F0A),
    surfaceContainerLow    = Color(0xFF171C17),
    surfaceContainer       = Color(0xFF1B201B),
    surfaceContainerHigh   = Color(0xFF262B25),
    surfaceContainerHighest= Color(0xFF303530),
    onSurfaceVariant       = Color(0xFFC0C9BD),
    outline                = Color(0xFF8A9388),
    outlineVariant         = Color(0xFF404941),
    inverseSurface         = Color(0xFFE0E4DC),
    inverseOnSurface       = Color(0xFF2D322C),
)

// ── Forest-green light scheme ────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary                = Color(0xFF2D6A39),
    onPrimary              = Color(0xFFFFFFFF),
    primaryContainer       = Color(0xFFAEF2B8),
    onPrimaryContainer     = Color(0xFF002109),
    secondary              = Color(0xFF506352),
    onSecondary            = Color(0xFFFFFFFF),
    secondaryContainer     = Color(0xFFD2E8D2),
    onSecondaryContainer   = Color(0xFF0E1F12),
    error                  = Color(0xFFBA1A1A),
    onError                = Color(0xFFFFFFFF),
    errorContainer         = Color(0xFFFFDAD6),
    onErrorContainer       = Color(0xFF410002),
    background             = Color(0xFFF7FBF2),
    onBackground           = Color(0xFF181D17),
    surface                = Color(0xFFF7FBF2),
    onSurface              = Color(0xFF181D17),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow    = Color(0xFFF1F5EC),
    surfaceContainer       = Color(0xFFEBEFE6),
    surfaceContainerHigh   = Color(0xFFE5E9E0),
    onSurfaceVariant       = Color(0xFF414941),
    outline                = Color(0xFF717971),
    outlineVariant         = Color(0xFFC1C9BF),
)

// ── Custom tokens not in M3 scheme ───────────────────────────────────────────
data class DumbifyColors(
    val amber: Color,
    val amberContainer: Color,
    val onAmberContainer: Color,
    val successDot: Color,
    val dangerDot: Color,
)

val DumbifyColorsDark = DumbifyColors(
    amber           = Color(0xFFFFD68A),
    amberContainer  = Color(0xFF4D3E13),
    onAmberContainer= Color(0xFFFFE3A8),
    successDot      = Color(0xFF73DD80),
    dangerDot       = Color(0xFFFF8A80),
)

val DumbifyColorsLight = DumbifyColors(
    amber           = Color(0xFF7A5A00),
    amberContainer  = Color(0xFFFFE08A),
    onAmberContainer= Color(0xFF261A00),
    successDot      = Color(0xFF2E7D32),
    dangerDot       = Color(0xFFC62828),
)

val LocalDumbifyColors = staticCompositionLocalOf { DumbifyColorsDark }

// ── Theme entry point ────────────────────────────────────────────────────────
@Composable
fun DumbifyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(LocalContext.current)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }
    val dumbifyColors = if (darkTheme) DumbifyColorsDark else DumbifyColorsLight

    CompositionLocalProvider(LocalDumbifyColors provides dumbifyColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
