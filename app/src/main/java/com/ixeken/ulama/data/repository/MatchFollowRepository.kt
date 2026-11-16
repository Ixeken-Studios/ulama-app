package com.ixeken.ulama.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.ixeken.ulama.core.model.FollowedMatch
import com.ixeken.ulama.core.model.MatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repositorio local de partidos seguidos por el usuario.
 *
 * Persiste los partidos en SharedPreferences serializados en formato JSON para garantizar
 * recuperacion instantanea sin necesidad de bases de datos pesadas ni conexiones externas.
 */
class MatchFollowRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _followedMatches = MutableStateFlow<Map<String, FollowedMatch>>(emptyMap())
    val followedMatches: StateFlow<Map<String, FollowedMatch>> = _followedMatches.asStateFlow()

    private val _followedMatchIds = MutableStateFlow<Set<String>>(emptySet())
    val followedMatchIds: StateFlow<Set<String>> = _followedMatchIds.asStateFlow()

    init {
        loadFromPreferences()
    }

    /**
     * Carga todos los partidos guardados previamente en disco al inicializar el repositorio.
     */
    private fun loadFromPreferences() {
        val storedMap = mutableMapOf<String, FollowedMatch>()
        runCatching {
            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                if (key.startsWith(KEY_PREFIX) && value is String) {
                    val matchId = key.removePrefix(KEY_PREFIX)
                    runCatching {
                        val match = json.decodeFromString<FollowedMatch>(value)
                        storedMap[matchId] = match
                    }
                }
            }
        }
        _followedMatches.value = storedMap
        _followedMatchIds.value = storedMap.keys.toSet()
        pruneFinishedOlderThan()
    }

    /**
     * Depura partidos finalizados con antiguedad superior al umbral para evitar acumulacion en disco.
     */
    fun pruneFinishedOlderThan(
        maxAgeMillis: Long = DEFAULT_FINISHED_PRUNE_AGE_MS,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val current = _followedMatches.value
        if (current.isEmpty()) return

        val expiredIds = current.values
            .filter { match ->
                match.state == MatchState.FINISHED &&
                    match.startTimeUtcMillis > 0L &&
                    (nowMillis - match.startTimeUtcMillis) > maxAgeMillis
            }
            .map { it.matchId }

        if (expiredIds.isEmpty()) return

        val updated = current.toMutableMap()
        val editor = prefs.edit()
        for (expiredId in expiredIds) {
            updated.remove(expiredId)
            editor.remove(KEY_PREFIX + expiredId)
        }
        runCatching { editor.apply() }
        _followedMatches.value = updated
        _followedMatchIds.value = updated.keys.toSet()
    }

    /**
     * Guarda o actualiza un partido seguido y emite el nuevo estado a los observadores.
     */
    fun follow(match: FollowedMatch) {
        val updated = _followedMatches.value.toMutableMap()
        updated[match.matchId] = match
        _followedMatches.value = updated
        _followedMatchIds.value = updated.keys.toSet()

        runCatching {
            val serialized = json.encodeToString(match)
            prefs.edit().putString(KEY_PREFIX + match.matchId, serialized).apply()
        }
    }

    /**
     * Elimina el partido del seguimiento y persiste la eliminacion en disco.
     */
    fun unfollow(matchId: String) {
        val updated = _followedMatches.value.toMutableMap()
        updated.remove(matchId)
        _followedMatches.value = updated
        _followedMatchIds.value = updated.keys.toSet()

        runCatching {
            prefs.edit().remove(KEY_PREFIX + matchId).apply()
        }
    }

    /**
     * Consulta rapida para saber si un partido se encuentra bajo seguimiento activo.
     */
    fun isFollowing(matchId: String): Boolean {
        return _followedMatchIds.value.contains(matchId)
    }

    /**
     * Obtiene el modelo del partido seguido o nulo si no existe en la lista.
     */
    fun getFollowed(matchId: String): FollowedMatch? {
        return _followedMatches.value[matchId]
    }

    /**
     * Actualiza el tanteador, el reloj y el estado de juego de un partido en seguimiento.
     */
    fun updateScoreAndStatus(
        matchId: String,
        homeScore: Int,
        awayScore: Int,
        state: MatchState,
        clockDisplay: String,
        periodDisplay: String
    ) {
        val current = _followedMatches.value[matchId] ?: return
        val updated = current.copy(
            homeScore = homeScore,
            awayScore = awayScore,
            state = state,
            clockDisplay = clockDisplay,
            periodDisplay = periodDisplay
        )
        follow(updated)
    }

    companion object {
        private const val PREFS_NAME = "ulama_followed_matches_prefs"
        private const val KEY_PREFIX = "match_"
        private const val DEFAULT_FINISHED_PRUNE_AGE_MS = 24L * 60L * 60L * 1000L

        @Volatile
        private var instance: MatchFollowRepository? = null

        /**
         * Proveedor singleton para acceso uniforme desde componentes de UI y servicios en segundo plano.
         */
        fun getInstance(context: Context): MatchFollowRepository {
            return instance ?: synchronized(this) {
                instance ?: MatchFollowRepository(context).also { instance = it }
            }
        }
    }
}
