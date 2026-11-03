package com.ixeken.ulama.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementacion ligera del repositorio de equipos favoritos basada en SharedPreferences.
 * Respeta la filosofia Ponytail al evitar la complejidad de bases de datos para un conjunto simple de identificadores.
 *
 * @param prefs Instancia de SharedPreferences donde se persiste el conjunto de claves.
 */
class FavoriteTeamsRepositoryImpl(
    private val prefs: SharedPreferences
) : FavoriteTeamsRepository {

    private val _favoriteTeamIds = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteTeamIds: StateFlow<Set<String>> = _favoriteTeamIds.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val stored = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty()
        _favoriteTeamIds.value = stored.toSet()
    }

    override fun toggleFavorite(teamId: String) {
        if (teamId.isBlank()) return
        val current = _favoriteTeamIds.value.toMutableSet()
        if (current.contains(teamId)) {
            current.remove(teamId)
        } else {
            current.add(teamId)
        }
        updateAndPersist(current)
    }

    override fun isFavorite(teamId: String): Boolean {
        return _favoriteTeamIds.value.contains(teamId)
    }

    override fun setFavorite(teamId: String, isFavorite: Boolean) {
        if (teamId.isBlank()) return
        val current = _favoriteTeamIds.value.toMutableSet()
        val changed = if (isFavorite) current.add(teamId) else current.remove(teamId)
        if (changed) {
            updateAndPersist(current)
        }
    }

    override fun clearAll() {
        updateAndPersist(emptySet())
    }

    private fun updateAndPersist(newSet: Set<String>) {
        _favoriteTeamIds.value = newSet
        prefs.edit().putStringSet(KEY_FAVORITES, newSet).apply()
    }

    companion object {
        const val PREFS_NAME = "ulama_user_preferences"
        const val KEY_FAVORITES = "favorite_team_ids"

        @Volatile
        private var instance: FavoriteTeamsRepositoryImpl? = null

        /**
         * Proveedor singleton para acceso uniforme desde componentes y factorias.
         */
        fun getInstance(context: Context): FavoriteTeamsRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: FavoriteTeamsRepositoryImpl(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
