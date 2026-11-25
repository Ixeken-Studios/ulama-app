package com.ixeken.ulama.ui.matches

import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.StandingEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias para validar la resolucion condicional de estados
 * y tablas de clasificacion en MatchesUiState bajo Strong Skipping.
 */
class MatchesUiStateTest {

    @Test
    fun resolvedDetailStandings_returnsLeagueStandingsForStandardLeague() {
        val standardStandings = listOf(
            StandingEntry(
                rank = 1,
                teamName = "Cruz Azul",
                teamShortName = "CAZ",
                teamAbbreviation = "CAZ",
                logoUrl = null,
                gamesPlayed = 10,
                wins = 8,
                draws = 1,
                losses = 1,
                goalsFor = 20,
                goalsAgainst = 6,
                goalDifference = 14,
                points = 25,
                teamId = "218"
            )
        )

        val state = MatchesUiState(
            selectedLeague = League.LIGA_MX,
            standings = standardStandings,
            detailStandings = emptyList()
        )

        assertEquals(standardStandings, state.resolvedDetailStandings)
    }

    @Test
    fun resolvedDetailStandings_returnsDetailStandingsForVirtualFavoritesLeague() {
        val specificMatchStandings = listOf(
            StandingEntry(
                rank = 1,
                teamName = "Real Madrid",
                teamShortName = "RMA",
                teamAbbreviation = "RMA",
                logoUrl = null,
                gamesPlayed = 10,
                wins = 8,
                draws = 1,
                losses = 1,
                goalsFor = 22,
                goalsAgainst = 7,
                goalDifference = 15,
                points = 25,
                teamId = "86"
            )
        )

        val state = MatchesUiState(
            selectedLeague = League.FAVORITES,
            standings = emptyList(), // La vista base omite la tabla en Favoritos
            detailStandings = specificMatchStandings
        )

        assertEquals(emptyList<StandingEntry>(), state.standings)
        assertEquals(specificMatchStandings, state.resolvedDetailStandings)
    }

    @Test
    fun resolvedDetailStandings_returnsDetailStandingsForVirtualMyLeagues() {
        val specificMatchStandings = listOf(
            StandingEntry(
                rank = 1,
                teamName = "Arsenal",
                teamShortName = "ARS",
                teamAbbreviation = "ARS",
                logoUrl = null,
                gamesPlayed = 10,
                wins = 8,
                draws = 1,
                losses = 1,
                goalsFor = 22,
                goalsAgainst = 7,
                goalDifference = 15,
                points = 25,
                teamId = "359"
            )
        )

        val state = MatchesUiState(
            selectedLeague = League.MY_LEAGUES,
            standings = emptyList(),
            detailStandings = specificMatchStandings
        )

        assertEquals(emptyList<StandingEntry>(), state.standings)
        assertEquals(specificMatchStandings, state.resolvedDetailStandings)
    }

    @Test
    fun debugLeagueData_providesTwoGroupsWithFourTeamsEach() {
        val groups = com.ixeken.ulama.data.debug.DebugLeagueData.getGroups()
        assertEquals(2, groups.size)
        assertEquals("A", groups[0].name)
        assertEquals(4, groups[0].standings.size)
        assertEquals("B", groups[1].name)
        assertEquals(4, groups[1].standings.size)
    }

    @Test
    fun matchesUiState_radialBracketDefaultAndSetting() {
        val defaultState = MatchesUiState()
        org.junit.Assert.assertNull(defaultState.radialBracket)
        org.junit.Assert.assertNull(defaultState.radialBracketSemis)
        org.junit.Assert.assertNull(defaultState.radialBracketEmpty)
        org.junit.Assert.assertNull(defaultState.radialBracket32)

        val bracket = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTree(8)
        val bracketSemis = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTreeSemis(8)
        val bracketEmpty = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTreeEmpty(8)
        val bracket32 = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTree(32)

        val stateWithBrackets = defaultState.copy(
            radialBracket = bracket,
            radialBracketSemis = bracketSemis,
            radialBracketEmpty = bracketEmpty,
            radialBracket32 = bracket32
        )
        org.junit.Assert.assertNotNull(stateWithBrackets.radialBracket)
        assertEquals(8, stateWithBrackets.radialBracket?.totalTeams)
        org.junit.Assert.assertNotNull(stateWithBrackets.radialBracketSemis)
        org.junit.Assert.assertNotNull(stateWithBrackets.radialBracketEmpty)
        org.junit.Assert.assertNotNull(stateWithBrackets.radialBracket32)
        assertEquals(32, stateWithBrackets.radialBracket32?.totalTeams)
    }

    @Test
    fun matchesUiState_tracksPerLeagueTeamLoadingIndependently() {
        val initial = MatchesUiState()
        org.junit.Assert.assertTrue(initial.loadingTeamLeagues.isEmpty())

        val loading = initial.copy(loadingTeamLeagues = setOf(League.PREMIER_LEAGUE))
        org.junit.Assert.assertTrue(League.PREMIER_LEAGUE in loading.loadingTeamLeagues)
        org.junit.Assert.assertFalse(League.LA_LIGA in loading.loadingTeamLeagues)
    }

    @Test
    fun resolveQuickSwitchLeagues_prependsVirtualCollectionsAndSortsAlphabetically() {
        val myLeagues = setOf(
            League.PREMIER_LEAGUE.code,
            League.BUNDESLIGA.code,
            League.LIGA_MX.code,
            League.FAVORITES.code,
            League.DEBUG.code,
            "unknown.league"
        )
        val names = mapOf(
            League.PREMIER_LEAGUE to "Premier League",
            League.BUNDESLIGA to "Bundesliga",
            League.LIGA_MX to "Liga MX"
        )

        val resolved = resolveQuickSwitchLeagues(myLeagues) { league ->
            names[league] ?: league.name
        }

        assertEquals(
            listOf(
                League.FAVORITES,
                League.MY_LEAGUES,
                League.BUNDESLIGA,
                League.LIGA_MX,
                League.PREMIER_LEAGUE
            ),
            resolved
        )
    }
}


