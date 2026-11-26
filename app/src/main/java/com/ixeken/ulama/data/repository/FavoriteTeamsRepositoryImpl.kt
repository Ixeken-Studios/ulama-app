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

    private val _teamLeagueEntries = MutableStateFlow<Set<String>>(emptySet())
    private val _favoriteLeagueCodes = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteLeagueCodes: StateFlow<Set<String>> = _favoriteLeagueCodes.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val storedIds = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet()
        val rawPairs = prefs.getStringSet(KEY_FAVORITE_TEAM_LEAGUES, emptySet()).orEmpty()
        val validPairs = rawPairs.filter { pair ->
            val teamId = pair.substringBefore(':', "")
            val leagueCode = pair.substringAfter(':', "")
            teamId in storedIds && leagueCode.isNotBlank()
        }.toSet()
        _favoriteTeamIds.value = storedIds
        _teamLeagueEntries.value = validPairs
        _favoriteLeagueCodes.value = extractLeagueCodes(validPairs)
    }

    override fun toggleFavorite(teamId: String) {
        if (teamId.isBlank()) return
        val current = _favoriteTeamIds.value.toMutableSet()
        val removed = if (current.contains(teamId)) {
            current.remove(teamId)
            true
        } else {
            current.add(teamId)
            false
        }
        updateAndPersist(current, removedTeamId = if (removed) teamId else null)
    }

    override fun isFavorite(teamId: String): Boolean {
        return _favoriteTeamIds.value.contains(teamId)
    }

    override fun setFavorite(teamId: String, isFavorite: Boolean) {
        if (teamId.isBlank()) return
        val current = _favoriteTeamIds.value.toMutableSet()
        val changed = if (isFavorite) current.add(teamId) else current.remove(teamId)
        if (changed) {
            updateAndPersist(current, removedTeamId = if (!isFavorite) teamId else null)
        }
    }

    override fun recordTeamLeagues(teamId: String, leagueCodes: Collection<String>) {
        if (teamId.isBlank() || leagueCodes.isEmpty() || teamId !in _favoriteTeamIds.value) return
        val additions = leagueCodes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { "$teamId:$it" }
        if (additions.isEmpty()) return

        val updatedPairs = _teamLeagueEntries.value + additions
        if (updatedPairs.size == _teamLeagueEntries.value.size) return

        _teamLeagueEntries.value = updatedPairs
        _favoriteLeagueCodes.value = extractLeagueCodes(updatedPairs)
        prefs.edit().putStringSet(KEY_FAVORITE_TEAM_LEAGUES, updatedPairs).apply()
    }

    override fun hasMappedLeaguesForAllFavorites(): Boolean {
        val favs = _favoriteTeamIds.value
        if (favs.isEmpty()) return true
        val pairs = _teamLeagueEntries.value
        return favs.all { id -> pairs.any { it.startsWith("$id:") } }
    }

    override fun clearAll() {
        _favoriteTeamIds.value = emptySet()
        _teamLeagueEntries.value = emptySet()
        _favoriteLeagueCodes.value = emptySet()
        prefs.edit()
            .putStringSet(KEY_FAVORITES, emptySet())
            .putStringSet(KEY_FAVORITE_TEAM_LEAGUES, emptySet())
            .apply()
    }

    private fun updateAndPersist(newSet: Set<String>, removedTeamId: String? = null) {
        _favoriteTeamIds.value = newSet
        val editor = prefs.edit().putStringSet(KEY_FAVORITES, newSet)
        if (removedTeamId != null) {
            val filteredPairs = _teamLeagueEntries.value
                .filterNot { it.startsWith("$removedTeamId:") }
                .toSet()
            _teamLeagueEntries.value = filteredPairs
            _favoriteLeagueCodes.value = extractLeagueCodes(filteredPairs)
            editor.putStringSet(KEY_FAVORITE_TEAM_LEAGUES, filteredPairs)
        }
        editor.apply()
    }

    private fun extractLeagueCodes(pairs: Set<String>): Set<String> =
        pairs.mapNotNull { it.substringAfter(':', "").takeIf { code -> code.isNotBlank() } }.toSet()

    companion object {
        const val PREFS_NAME = "ulama_user_preferences"
        const val KEY_FAVORITES = "favorite_team_ids"
        const val KEY_FAVORITE_TEAM_LEAGUES = "favorite_team_leagues"

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
