package com.ixeken.ulama.data.repository

import com.ixeken.ulama.core.model.GroupStandings
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchDetail
import com.ixeken.ulama.core.model.StandingEntry

/**
 * Contrato de repositorio para obtencion de partidos, tabla de posiciones y clasificacion por grupos.
 */
interface MatchRepository {
    suspend fun getMatches(
        league: League,
        dates: String? = null,
        forceRefresh: Boolean = false,
        cacheOnly: Boolean = false,
        onPreliminaryMatches: ((List<Match>) -> Unit)? = null
    ): Result<List<Match>>

    suspend fun getStandings(league: League, forceRefresh: Boolean = false): Result<List<StandingEntry>>
    suspend fun getGroups(league: League, forceRefresh: Boolean = false): Result<List<GroupStandings>>

    /**
     * Obtiene en una sola operacion atomica tanto la tabla general como los grupos de una competicion,
     * evitando adquirir bloqueos duplicados y permitiendo lectura instantanea desde cache en disco.
     */
    suspend fun getStandingsAndGroups(
        league: League,
        forceRefresh: Boolean = false,
        cacheOnly: Boolean = false
    ): Result<Pair<List<StandingEntry>, List<GroupStandings>>> {
        val standings = getStandings(league, forceRefresh)
        val groups = getGroups(league, forceRefresh)
        return standings.map { Pair(it, groups.getOrDefault(emptyList())) }
    }

    suspend fun getMatchDetail(league: League, matchId: String, forceRefresh: Boolean = false): Result<MatchDetail>

    /**
     * Obtiene el detalle de un partido desde la cache en memoria de forma inmediata.
     * Permite a la interfaz de usuario presentar datos al instante aplicando Stale-While-Revalidate sin bloquear hilos.
     * Retorna nulo si el partido no se ha consultado o si expiro su tiempo de vida util.
     */
    fun getCachedMatchDetail(matchId: String): MatchDetail?
}
