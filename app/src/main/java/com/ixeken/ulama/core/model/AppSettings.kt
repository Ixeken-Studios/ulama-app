package com.ixeken.ulama.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.R

/**
 * Modos de visualizacion para el tema oscuro de la aplicacion.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Paleta de colores aplicada a los componentes visuales.
 */
enum class ColorPalette {
    DYNAMIC,
    ULAMA,
    MONOCHROME,
    PURPLE,
    BLUE,
    AMBER
}

/**
 * Estilo de transicion fisica para componentes animados.
 */
enum class MotionStyle {
    EXPRESSIVE,
    STANDARD
}

/**
 * Idioma configurado para los textos de la aplicacion.
 * Permite alternar entre el idioma del sistema o forzar espanol o ingles de forma explicita.
 *
 * @param nativeName Nombre del idioma en su lengua original para encabezados directos.
 * @param localizedNameRes Recurso de cadena que traduce el nombre del idioma al idioma activo.
 * @param descriptionRes Recurso de cadena explicativo opcional para opciones como la del sistema.
 */
enum class AppLanguage(
    val nativeName: String? = null,
    @param:StringRes val localizedNameRes: Int,
    @param:StringRes val descriptionRes: Int? = null
) {
    SYSTEM(
        nativeName = null,
        localizedNameRes = R.string.settings_language_system,
        descriptionRes = R.string.settings_language_system_desc
    ),
    SPANISH(
        nativeName = "Español",
        localizedNameRes = R.string.settings_language_es
    ),
    ENGLISH(
        nativeName = "English",
        localizedNameRes = R.string.settings_language_en
    );
}

/**
 * Estado inmutable de las preferencias de usuario de la aplicacion.
 */
@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPalette: ColorPalette = ColorPalette.DYNAMIC,
    val motionStyle: MotionStyle = MotionStyle.EXPRESSIVE,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val notificationLeadMinutes: Int = DEFAULT_NOTIFICATION_LEAD_MINUTES,
    val autoFollowFavorites: Boolean = false,
    val checkUpdatesOnStart: Boolean = false,
    val lastKnownVersion: String = "",
    val hasCompletedOnboarding: Boolean = false
) {
    companion object {
        const val DEFAULT_NOTIFICATION_LEAD_MINUTES = 5
    }
}
