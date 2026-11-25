package com.ixeken.ulama.ui.matches

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.core.model.GroupStandings
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchDetail
import com.ixeken.ulama.core.model.RadialBracketTree
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.core.model.TemporalFilter
import java.text.Collator

/**
 * Estado inmutable de la pantalla de partidos de Ulama.
 * Disenado bajo Strong Skipping Mode para prevenir recomposiciones innecesarias.
 */
@Immutable
data class MatchesUiState(
    val selectedLeague: League = League.LIGA_MX,
    val selectedFilter: TemporalFilter = TemporalFilter.TODAY,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val matches: List<Match> = emptyList(),
    val matchesByFilter: Map<TemporalFilter, List<Match>> = emptyMap(),
    val standings: List<StandingEntry> = emptyList(),
    val groups: List<GroupStandings> = emptyList(),
    val radialBracket: RadialBracketTree? = null,
    val radialBracketSemis: RadialBracketTree? = null,
    val radialBracketEmpty: RadialBracketTree? = null,
    val radialBracket32: RadialBracketTree? = null,
    val selectedMatch: Match? = null,
    val matchDetail: MatchDetail? = null,
    val detailStandings: List<StandingEntry> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val followedMatchIds: Set<String> = emptySet(),
    val favoriteTeamIds: Set<String> = emptySet(),
    val myLeagueCodes: Set<String> = emptySet(),
    val availableTeamsByLeague: Map<League, List<StandingEntry>> = emptyMap(),
    val loadingTeamLeagues: Set<League> = emptySet(),
    val errorMessage: String? = null,
    @param:StringRes val errorMessageRes: Int? = null
) {
    /**
     * Resuelve la tabla de posiciones correspondiente para el visor de detalle,
     * priorizando detailStandings si fue poblada para ligas virtuales o grupos,
     * o recurriendo a la clasificacion general si esta disponible.
     */
    val resolvedDetailStandings: List<StandingEntry>
        get() = when {
            detailStandings.isNotEmpty() -> detailStandings
            standings.isNotEmpty() && (selectedMatch == null || selectedMatch.league == selectedLeague || selectedLeague == League.DEBUG) -> standings
            else -> emptyList()
        }
}

/**
 * Construye la secuencia ordenada para rotar rapidamente desde la pildora superior:
 * primero Mis equipos y Mis ligas, seguidos por las competiciones fijadas en orden alfabetico localizado.
 */
fun resolveQuickSwitchLeagues(
    myLeagueCodes: Set<String>,
    nameProvider: (League) -> String
): List<League> {
    val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
    val pinned = myLeagueCodes
        .asSequence()
        .mapNotNull { code -> League.entries.firstOrNull { it.code == code } }
        .filter { !it.isVirtual && it != League.DEBUG }
        .distinct()
        .sortedWith { a, b -> collator.compare(nameProvider(a), nameProvider(b)) }
        .toList()
    return listOf(League.FAVORITES, League.MY_LEAGUES) + pinned
}

