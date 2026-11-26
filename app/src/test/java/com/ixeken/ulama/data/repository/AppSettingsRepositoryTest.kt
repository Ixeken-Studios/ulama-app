package com.ixeken.ulama.data.repository

import android.content.SharedPreferences
import com.ixeken.ulama.core.model.ColorPalette
import com.ixeken.ulama.core.model.MotionStyle
import com.ixeken.ulama.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para AppSettingsRepositoryImpl que verifican la persistencia
 * y reactividad de los ajustes visuales y de ahorro de datos de la aplicacion.
 */
class AppSettingsRepositoryTest {

    private lateinit var fakePrefs: FakeSettingsPreferences
    private lateinit var repository: AppSettingsRepositoryImpl

    @Before
    fun setUp() {
        fakePrefs = FakeSettingsPreferences()
        repository = AppSettingsRepositoryImpl(fakePrefs)
    }

    @Test
    fun defaultSettings_haveExpectedValues() {
        val initial = repository.settings.value
        assertEquals(ThemeMode.SYSTEM, initial.themeMode)
        assertEquals(ColorPalette.DYNAMIC, initial.colorPalette)
        assertEquals(MotionStyle.EXPRESSIVE, initial.motionStyle)
        assertEquals(5, initial.notificationLeadMinutes)
        assertEquals(false, initial.autoFollowFavorites)
        assertEquals(false, initial.dataSaverMode)
        assertEquals(false, initial.checkUpdatesOnStart)
        assertEquals("", initial.lastKnownVersion)
    }

    @Test
    fun setThemeMode_updatesStateAndPersists() {
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.settings.value.themeMode)
        assertEquals(ThemeMode.DARK.name, fakePrefs.getString("settings_theme_mode", null))

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.settings.value.themeMode)
        assertEquals(ThemeMode.LIGHT.name, fakePrefs.getString("settings_theme_mode", null))
    }

    @Test
    fun setColorPalette_updatesStateAndPersists() {
        repository.setColorPalette(ColorPalette.ULAMA)
        assertEquals(ColorPalette.ULAMA, repository.settings.value.colorPalette)
        assertEquals(ColorPalette.ULAMA.name, fakePrefs.getString("settings_color_palette", null))

        repository.setColorPalette(ColorPalette.MONOCHROME)
        assertEquals(ColorPalette.MONOCHROME, repository.settings.value.colorPalette)
        assertEquals(ColorPalette.MONOCHROME.name, fakePrefs.getString("settings_color_palette", null))

        repository.setColorPalette(ColorPalette.DYNAMIC)
        assertEquals(ColorPalette.DYNAMIC, repository.settings.value.colorPalette)
    }

    @Test
    fun setMotionStyle_updatesStateAndPersists() {
        repository.setMotionStyle(MotionStyle.STANDARD)
        assertEquals(MotionStyle.STANDARD, repository.settings.value.motionStyle)
        assertEquals(MotionStyle.STANDARD.name, fakePrefs.getString("settings_motion_style", null))

        repository.setMotionStyle(MotionStyle.EXPRESSIVE)
        assertEquals(MotionStyle.EXPRESSIVE, repository.settings.value.motionStyle)
    }

    @Test
    fun setNotificationLeadMinutes_updatesStateAndPersists() {
        repository.setNotificationLeadMinutes(15)
        assertEquals(15, repository.settings.value.notificationLeadMinutes)
        assertEquals(15, fakePrefs.getInt("settings_notification_lead_minutes", 0))

        repository.setNotificationLeadMinutes(10)
        assertEquals(10, repository.settings.value.notificationLeadMinutes)
        assertEquals(10, fakePrefs.getInt("settings_notification_lead_minutes", 0))
    }

    @Test
    fun setAutoFollowFavorites_updatesStateAndPersists() {
        repository.setAutoFollowFavorites(true)
        assertEquals(true, repository.settings.value.autoFollowFavorites)
        assertEquals(true, fakePrefs.getBoolean("settings_auto_follow_favorites", false))

        repository.setAutoFollowFavorites(false)
        assertEquals(false, repository.settings.value.autoFollowFavorites)
        assertEquals(false, fakePrefs.getBoolean("settings_auto_follow_favorites", true))
    }

    @Test
    fun setDataSaverMode_updatesStateAndPersists() {
        repository.setDataSaverMode(true)
        assertEquals(true, repository.settings.value.dataSaverMode)
        assertEquals(true, fakePrefs.getBoolean("settings_data_saver_mode", false))

        repository.setDataSaverMode(false)
        assertEquals(false, repository.settings.value.dataSaverMode)
        assertEquals(false, fakePrefs.getBoolean("settings_data_saver_mode", true))
    }

    @Test
    fun setCheckUpdatesOnStart_updatesStateAndPersists() {
        repository.setCheckUpdatesOnStart(true)
        assertEquals(true, repository.settings.value.checkUpdatesOnStart)
        assertEquals(true, fakePrefs.getBoolean("settings_check_updates_on_start", false))

        repository.setCheckUpdatesOnStart(false)
        assertEquals(false, repository.settings.value.checkUpdatesOnStart)
        assertEquals(false, fakePrefs.getBoolean("settings_check_updates_on_start", true))
    }

    @Test
    fun setLastKnownVersion_updatesStateAndPersists() {
        repository.setLastKnownVersion("2.0")
        assertEquals("2.0", repository.settings.value.lastKnownVersion)
        assertEquals("2.0", fakePrefs.getString("settings_last_known_version", null))
    }

    @Test
    fun preloadedPreferences_areLoadedCorrectly() {
        fakePrefs.putString("settings_theme_mode", ThemeMode.DARK.name)
        fakePrefs.putString("settings_color_palette", ColorPalette.ULAMA.name)
        fakePrefs.putString("settings_motion_style", MotionStyle.STANDARD.name)
        fakePrefs.putInt("settings_notification_lead_minutes", 10)
        fakePrefs.putBoolean("settings_auto_follow_favorites", true)
        fakePrefs.putBoolean("settings_data_saver_mode", true)
        fakePrefs.putBoolean("settings_check_updates_on_start", true)
        fakePrefs.putString("settings_last_known_version", "1.0")

        val newRepo = AppSettingsRepositoryImpl(fakePrefs)
        val loaded = newRepo.settings.value
        assertEquals(ThemeMode.DARK, loaded.themeMode)
        assertEquals(ColorPalette.ULAMA, loaded.colorPalette)
        assertEquals(MotionStyle.STANDARD, loaded.motionStyle)
        assertEquals(10, loaded.notificationLeadMinutes)
        assertEquals(true, loaded.autoFollowFavorites)
        assertEquals(true, loaded.dataSaverMode)
        assertEquals(true, loaded.checkUpdatesOnStart)
        assertEquals("1.0", loaded.lastKnownVersion)
    }
}

/**
 * Doble de prueba en memoria para SharedPreferences que evita mocks pesados.
 */
private class FakeSettingsPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    fun putString(key: String, value: String?) {
        data[key] = value
    }

    fun putInt(key: String, value: Int) {
        data[key] = value
    }

    fun putBoolean(key: String, value: Boolean) {
        data[key] = value
    }

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(data)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = null
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            storage.clear()
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            storage.putAll(temp)
        }
    }
}
