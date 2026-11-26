package com.ixeken.ulama.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.ixeken.ulama.core.model.AppLanguage
import com.ixeken.ulama.core.model.AppSettings
import com.ixeken.ulama.core.model.ColorPalette
import com.ixeken.ulama.core.model.MotionStyle
import com.ixeken.ulama.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Contrato de persistencia reactiva para las configuraciones y preferencias visuales de Ulama.
 */
interface AppSettingsRepository {
    val settings: StateFlow<AppSettings>
    fun setThemeMode(mode: ThemeMode)
    fun setColorPalette(palette: ColorPalette)
    fun setMotionStyle(style: MotionStyle)
    fun setNotificationLeadMinutes(minutes: Int)
    fun setAutoFollowFavorites(enabled: Boolean)
    fun setDataSaverMode(enabled: Boolean)
    fun setAppLanguage(language: AppLanguage)
    fun setCheckUpdatesOnStart(enabled: Boolean)
    fun setLastKnownVersion(version: String)
    fun setOnboardingCompleted(completed: Boolean)
}

/**
 * Implementacion ligera basada en SharedPreferences para la persistencia de ajustes.
 * Garantiza reactividad mediante StateFlow cumpliendo el principio Ponytail sin dependencias complejas.
 *
 * @param prefs Instancia de SharedPreferences donde se almacenan las opciones del usuario.
 */
class AppSettingsRepositoryImpl(
    private val prefs: SharedPreferences
) : AppSettingsRepository {

    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val colorPaletteStr = prefs.getString(KEY_COLOR_PALETTE, ColorPalette.DYNAMIC.name)
        val motionStyleStr = prefs.getString(KEY_MOTION_STYLE, MotionStyle.EXPRESSIVE.name)
        val leadMinutes = prefs.getInt(
            KEY_NOTIFICATION_LEAD_MINUTES,
            AppSettings.DEFAULT_NOTIFICATION_LEAD_MINUTES
        )
        val checkUpdatesOnStart = prefs.getBoolean(KEY_CHECK_UPDATES_ON_START, false)
        val autoFollowFavorites = prefs.getBoolean(KEY_AUTO_FOLLOW_FAVORITES, false)
        val dataSaverMode = prefs.getBoolean(KEY_DATA_SAVER_MODE, false)
        val lastKnownVersion = prefs.getString(KEY_LAST_KNOWN_VERSION, "") ?: ""
        val hasCompletedOnboarding = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
        val languageStr = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.name)

        val themeMode = try {
            ThemeMode.valueOf(themeModeStr ?: ThemeMode.SYSTEM.name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }

        val colorPalette = try {
            ColorPalette.valueOf(colorPaletteStr ?: ColorPalette.DYNAMIC.name)
        } catch (_: IllegalArgumentException) {
            ColorPalette.DYNAMIC
        }

        val motionStyle = try {
            MotionStyle.valueOf(motionStyleStr ?: MotionStyle.EXPRESSIVE.name)
        } catch (_: IllegalArgumentException) {
            MotionStyle.EXPRESSIVE
        }

        val appLanguage = try {
            AppLanguage.valueOf(languageStr ?: AppLanguage.SYSTEM.name)
        } catch (_: IllegalArgumentException) {
            AppLanguage.SYSTEM
        }

        return AppSettings(
            themeMode = themeMode,
            colorPalette = colorPalette,
            motionStyle = motionStyle,
            appLanguage = appLanguage,
            notificationLeadMinutes = leadMinutes,
            autoFollowFavorites = autoFollowFavorites,
            dataSaverMode = dataSaverMode,
            checkUpdatesOnStart = checkUpdatesOnStart,
            lastKnownVersion = lastKnownVersion,
            hasCompletedOnboarding = hasCompletedOnboarding
        )
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    override fun setColorPalette(palette: ColorPalette) {
        prefs.edit().putString(KEY_COLOR_PALETTE, palette.name).apply()
        _settings.value = _settings.value.copy(colorPalette = palette)
    }

    override fun setMotionStyle(style: MotionStyle) {
        prefs.edit().putString(KEY_MOTION_STYLE, style.name).apply()
        _settings.value = _settings.value.copy(motionStyle = style)
    }

    override fun setNotificationLeadMinutes(minutes: Int) {
        val sanitized = minutes.coerceIn(0, 60)
        prefs.edit().putInt(KEY_NOTIFICATION_LEAD_MINUTES, sanitized).apply()
        _settings.value = _settings.value.copy(notificationLeadMinutes = sanitized)
    }

    override fun setAutoFollowFavorites(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_FOLLOW_FAVORITES, enabled).apply()
        _settings.value = _settings.value.copy(autoFollowFavorites = enabled)
    }

    override fun setDataSaverMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_SAVER_MODE, enabled).apply()
        _settings.value = _settings.value.copy(dataSaverMode = enabled)
    }

    override fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        _settings.value = _settings.value.copy(appLanguage = language)
    }

    override fun setCheckUpdatesOnStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CHECK_UPDATES_ON_START, enabled).apply()
        _settings.value = _settings.value.copy(checkUpdatesOnStart = enabled)
    }

    override fun setLastKnownVersion(version: String) {
        prefs.edit().putString(KEY_LAST_KNOWN_VERSION, version).apply()
        _settings.value = _settings.value.copy(lastKnownVersion = version)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, completed).apply()
        _settings.value = _settings.value.copy(hasCompletedOnboarding = completed)
    }

    companion object {
        private const val PREFS_NAME = "ulama_user_preferences"
        private const val KEY_THEME_MODE = "settings_theme_mode"
        private const val KEY_COLOR_PALETTE = "settings_color_palette"
        private const val KEY_MOTION_STYLE = "settings_motion_style"
        private const val KEY_APP_LANGUAGE = "settings_app_language"
        private const val KEY_NOTIFICATION_LEAD_MINUTES = "settings_notification_lead_minutes"
        private const val KEY_AUTO_FOLLOW_FAVORITES = "settings_auto_follow_favorites"
        private const val KEY_DATA_SAVER_MODE = "settings_data_saver_mode"
        private const val KEY_CHECK_UPDATES_ON_START = "settings_check_updates_on_start"
        private const val KEY_LAST_KNOWN_VERSION = "settings_last_known_version"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "settings_has_completed_onboarding"

        @Volatile
        private var instance: AppSettingsRepository? = null

        fun provide(context: Context): AppSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: AppSettingsRepositoryImpl(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
