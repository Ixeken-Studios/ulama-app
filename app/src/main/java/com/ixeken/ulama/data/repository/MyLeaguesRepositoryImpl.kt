package com.ixeken.ulama.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementacion ligera del repositorio de Mis Ligas basada en SharedPreferences.
 * Respeta la filosofia Ponytail al evitar bases de datos pesadas para un conjunto de cadenas.
 *
 * @param prefs Instancia de SharedPreferences donde se persiste el conjunto de codigos de torneo.
 */
class MyLeaguesRepositoryImpl(
    private val prefs: SharedPreferences
) : MyLeaguesRepository {

    private val _myLeagueCodes = MutableStateFlow<Set<String>>(emptySet())
    override val myLeagueCodes: StateFlow<Set<String>> = _myLeagueCodes.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val stored = prefs.getStringSet(KEY_MY_LEAGUES, emptySet()).orEmpty()
        _myLeagueCodes.value = stored.toSet()
    }

    override fun toggleLeague(leagueCode: String) {
        if (leagueCode.isBlank()) return
        val current = _myLeagueCodes.value.toMutableSet()
        if (current.contains(leagueCode)) {
            current.remove(leagueCode)
        } else {
            current.add(leagueCode)
        }
        updateAndPersist(current)
    }

    override fun isLeagueSelected(leagueCode: String): Boolean {
        return _myLeagueCodes.value.contains(leagueCode)
    }

    override fun setLeagueSelected(leagueCode: String, isSelected: Boolean) {
        if (leagueCode.isBlank()) return
        val current = _myLeagueCodes.value.toMutableSet()
        val changed = if (isSelected) current.add(leagueCode) else current.remove(leagueCode)
        if (changed) {
            updateAndPersist(current)
        }
    }

    override fun clearAll() {
        updateAndPersist(emptySet())
    }

    private fun updateAndPersist(newSet: Set<String>) {
        _myLeagueCodes.value = newSet
        prefs.edit().putStringSet(KEY_MY_LEAGUES, newSet).apply()
    }

    companion object {
        private const val PREFS_NAME = "ulama_user_preferences"
        internal const val KEY_MY_LEAGUES = "my_leagues_codes"

        @Volatile
        private var instance: MyLeaguesRepositoryImpl? = null

        fun getInstance(context: Context): MyLeaguesRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: MyLeaguesRepositoryImpl(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
