package com.ixeken.ulama.data.repository

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para validar la persistencia, modificacion y consulta
 * de ligas activas en MyLeaguesRepositoryImpl sin dependencias de Android framework.
 */
class MyLeaguesRepositoryTest {

    private lateinit var fakePrefs: FakeMyLeaguesSharedPreferences
    private lateinit var repository: MyLeaguesRepositoryImpl

    @Before
    fun setUp() {
        fakePrefs = FakeMyLeaguesSharedPreferences()
        repository = MyLeaguesRepositoryImpl(fakePrefs)
    }

    @Test
    fun initialState_isEmpty() {
        assertTrue(repository.myLeagueCodes.value.isEmpty())
        assertFalse(repository.isLeagueSelected("mex.1"))
    }

    @Test
    fun toggleLeague_addsAndRemovesLeague() {
        repository.toggleLeague("mex.1")
        assertTrue(repository.isLeagueSelected("mex.1"))
        assertTrue(repository.myLeagueCodes.value.contains("mex.1"))

        repository.toggleLeague("mex.1")
        assertFalse(repository.isLeagueSelected("mex.1"))
        assertFalse(repository.myLeagueCodes.value.contains("mex.1"))
    }

    @Test
    fun setLeagueSelected_explicitValues() {
        repository.setLeagueSelected("eng.1", true)
        assertTrue(repository.isLeagueSelected("eng.1"))

        repository.setLeagueSelected("eng.1", true)
        assertEquals(1, repository.myLeagueCodes.value.size)

        repository.setLeagueSelected("eng.1", false)
        assertFalse(repository.isLeagueSelected("eng.1"))
    }

    @Test
    fun toggleLeague_ignoresBlank() {
        repository.toggleLeague("")
        repository.toggleLeague("   ")
        assertTrue(repository.myLeagueCodes.value.isEmpty())
    }

    @Test
    fun clearAll_removesAllEntries() {
        repository.toggleLeague("mex.1")
        repository.toggleLeague("eng.1")
        repository.toggleLeague("esp.1")
        assertEquals(3, repository.myLeagueCodes.value.size)

        repository.clearAll()
        assertTrue(repository.myLeagueCodes.value.isEmpty())
    }

    @Test
    fun preloadedPreferences_areLoadedOnInit() {
        fakePrefs.putStringSet(MyLeaguesRepositoryImpl.KEY_MY_LEAGUES, setOf("mex.1", "esp.1"))
        val newRepo = MyLeaguesRepositoryImpl(fakePrefs)
        assertEquals(setOf("mex.1", "esp.1"), newRepo.myLeagueCodes.value)
        assertTrue(newRepo.isLeagueSelected("mex.1"))
        assertTrue(newRepo.isLeagueSelected("esp.1"))
    }
}

/**
 * Doble de prueba en memoria para SharedPreferences que simula el almacenamiento de conjuntos.
 */
private class FakeMyLeaguesSharedPreferences : SharedPreferences {
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
