package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Trois thèmes au choix dans l'app. Les couleurs de score (mouche, 10, autres) ne changent pas
 * d'un thème à l'autre : elles doivent rester reconnaissables partout.
 */
enum class AppThemeMode(val label: String, val description: String) {
    DARK("Sombre", "Ardoise et rouge, pour le tir de nuit"),
    LIGHT("Clair", "Fond blanc, lisible en plein jour"),
    TACTICAL("Tactique", "Noir et vert foncé désaturé, touches de doré")
}

/** Sombre : le thème historique de l'app. */
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF4D4D),
    onPrimary = Color(0xFF380004),
    primaryContainer = Color(0xFF68000C),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF261900),
    secondaryContainer = Color(0xFF4A3A10),
    onSecondaryContainer = Color(0xFFFDEBC0),
    background = Color(0xFF0F141C),
    surface = Color(0xFF161E29),
    surfaceVariant = Color(0xFF212B3B),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF7C8AA0),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0000)
)

/** Clair : fond blanc cassé, texte ardoise foncé, rouge et ocre assez contrastés sur blanc. */
private val LightScheme = lightColorScheme(
    primary = Color(0xFFB91C1C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410003),
    secondary = Color(0xFF92400E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFDF0C8),
    onSecondaryContainer = Color(0xFF3D2A00),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EEF4),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF)
)

/**
 * Tactique : noir profond, vert foncé très désaturé pour les cartes, doré pour l'action principale
 * et les titres. Pas de rouge vif hors suppression.
 */
private val TacticalScheme = darkColorScheme(
    primary = Color(0xFFD6B25E),
    onPrimary = Color(0xFF1A1305),
    primaryContainer = Color(0xFF3A2F12),
    onPrimaryContainer = Color(0xFFF3E3B0),
    secondary = Color(0xFF9CB38F),
    onSecondary = Color(0xFF0E160F),
    secondaryContainer = Color(0xFF22342A),
    onSecondaryContainer = Color(0xFFD5E3CF),
    background = Color(0xFF080A09),
    surface = Color(0xFF111714),
    surfaceVariant = Color(0xFF1A241E),
    onSurface = Color(0xFFE8ECE6),
    onSurfaceVariant = Color(0xFFA7B5A2),
    outline = Color(0xFF6B7C6E),
    outlineVariant = Color(0xFF2A3A2F),
    error = Color(0xFFE8877E),
    onError = Color(0xFF3A0A06)
)

fun schemeFor(mode: AppThemeMode): ColorScheme = when (mode) {
    AppThemeMode.DARK -> DarkScheme
    AppThemeMode.LIGHT -> LightScheme
    AppThemeMode.TACTICAL -> TacticalScheme
}

/** Mémorise le thème choisi entre deux lancements. */
object ThemePreferences {
    private const val PREFS = "shootr_settings"
    private const val KEY_THEME = "theme_mode"

    fun load(context: Context): AppThemeMode {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
        return AppThemeMode.entries.firstOrNull { it.name == saved } ?: AppThemeMode.DARK
    }

    fun save(context: Context, mode: AppThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()
    }
}
