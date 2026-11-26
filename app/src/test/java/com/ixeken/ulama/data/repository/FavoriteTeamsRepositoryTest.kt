package com.ixeken.ulama.data.repository

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para validar la persistencia, consulta y modificaciones
 * en el repositorio ligero de clubes favoritos.
 */
class FavoriteTeamsRepositoryTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var repository: FavoriteTeamsRepositoryImpl

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        repository = FavoriteTeamsRepositoryImpl(fakePrefs)
    }

    @Test
    fun initialState_isEmpty() {
        assertTrue(repository.favoriteTeamIds.value.isEmpty())
        assertFalse(repository.isFavorite("227"))
    }

    @Test
    fun toggleFavorite_addsAndRemovesClub() {
        repository.toggleFavorite("227")
        assertTrue(repository.isFavorite("227"))
        assertTrue(repository.favoriteTeamIds.value.contains("227"))

        repository.toggleFavorite("227")
        assertFalse(repository.isFavorite("227"))
        assertFalse(repository.favoriteTeamIds.value.contains("227"))
    }

    @Test
    fun setFavorite_explicitValues() {
        repository.setFavorite("86", true)
        assertTrue(repository.isFavorite("86"))

        repository.setFavorite("86", true)
        assertEquals(1, repository.favoriteTeamIds.value.size)

        repository.setFavorite("86", false)
        assertFalse(repository.isFavorite("86"))
    }

    @Test
    fun toggleFavorite_ignoresBlank() {
        repository.toggleFavorite("")
        repository.toggleFavorite("   ")
        assertTrue(repository.favoriteTeamIds.value.isEmpty())
    }

    @Test
    fun clearAll_removesAllEntries() {
        repository.toggleFavorite("227")
        repository.toggleFavorite("86")
        repository.toggleFavorite("364")
        assertEquals(3, repository.favoriteTeamIds.value.size)

        repository.clearAll()
        assertTrue(repository.favoriteTeamIds.value.isEmpty())
    }

    @Test
    fun preloadedPreferences_areLoadedOnInit() {
        fakePrefs.putStringSet(FavoriteTeamsRepositoryImpl.KEY_FAVORITES, setOf("227", "83"))
        val newRepo = FavoriteTeamsRepositoryImpl(fakePrefs)
        assertEquals(setOf("227", "83"), newRepo.favoriteTeamIds.value)
        assertTrue(newRepo.isFavorite("227"))
        assertTrue(newRepo.isFavorite("83"))
    }

    @Test
    fun recordTeamLeagues_persistsAndCleansUpWhenUnfollowed() {
        repository.toggleFavorite("86")
        assertFalse(repository.hasMappedLeaguesForAllFavorites())

        repository.recordTeamLeagues("86", listOf("esp.1", "uefa.champions"))
        assertTrue(repository.hasMappedLeaguesForAllFavorites())
        assertEquals(setOf("esp.1", "uefa.champions"), repository.favoriteLeagueCodes.value)

        val reloaded = FavoriteTeamsRepositoryImpl(fakePrefs)
        assertTrue(reloaded.hasMappedLeaguesForAllFavorites())
        assertEquals(setOf("esp.1", "uefa.champions"), reloaded.favoriteLeagueCodes.value)

        reloaded.toggleFavorite("86")
        assertTrue(reloaded.favoriteLeagueCodes.value.isEmpty())
    }
}

/**
 * Doble de prueba en memoria para SharedPreferences que evita dependencias externas.
 */
private class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    fun putStringSet(key: String, values: Set<String>?) {
        data[key] = values?.toSet()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        return (data[key] as? Set<String>)?.toMutableSet() ?: defValues
    }

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
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
        private val removed = mutableSetOf<String>()

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values?.toSet()
            return this
        }

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removed.add(key)
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
            removed.forEach { storage.remove(it) }
            storage.putAll(temp)
        }
    }
}
