package com.ixeken.ulama.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias para validar la resolucion dinamica de URLs de escudos
 * en modo claro y modo oscuro para ligas y clubes.
 */
class LogoUrlResolutionTest {

    @Test
    fun team_getLogoUrl_replacesPathInDarkMode() {
        val lightUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/223.png"
        val team = Team(
            id = "223",
            name = "Toluca",
            shortName = "Toluca",
            abbreviation = "TOL",
            logoUrl = lightUrl,
            isHome = true,
            score = 2,
            isWinner = true
        )

        assertEquals(lightUrl, team.getLogoUrl(isDark = false))
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500-dark/223.png", team.getLogoUrl(isDark = true))
    }

    @Test
    fun team_getLogoUrl_returnsNullWhenLogoUrlIsNull() {
        val team = Team(
            id = "0",
            name = "Sin Escudo",
            shortName = "SE",
            abbreviation = "SE",
            logoUrl = null,
            isHome = false,
            score = 0,
            isWinner = false
        )

        assertNull(team.getLogoUrl(isDark = false))
        assertNull(team.getLogoUrl(isDark = true))
    }

    @Test
    fun standingEntry_getLogoUrl_replacesPathInDarkMode() {
        val lightUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/227.png"
        val entry = StandingEntry(
            rank = 1,
            teamName = "América",
            teamShortName = "América",
            teamAbbreviation = "AME",
            logoUrl = lightUrl,
            gamesPlayed = 10,
            wins = 7,
            draws = 2,
            losses = 1,
            goalsFor = 20,
            goalsAgainst = 8,
            goalDifference = 12,
            points = 23,
            teamId = "227"
        )

        assertEquals(lightUrl, entry.getLogoUrl(isDark = false))
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500-dark/227.png", entry.getLogoUrl(isDark = true))
    }

    @Test
    fun standingEntry_getLogoUrl_returnsNullWhenLogoUrlIsNull() {
        val entry = StandingEntry(
            rank = 1,
            teamName = "Sin Escudo",
            teamShortName = "SE",
            teamAbbreviation = "SE",
            logoUrl = null,
            gamesPlayed = 0,
            wins = 0,
            draws = 0,
            losses = 0,
            goalsFor = 0,
            goalsAgainst = 0,
            goalDifference = 0,
            points = 0
        )

        assertNull(entry.getLogoUrl(isDark = false))
        assertNull(entry.getLogoUrl(isDark = true))
    }

    @Test
    fun league_getLogoUrl_returnsLightAndDarkVariants() {
        val league = League.LIGA_MX
        assertEquals(league.lightLogoUrl, league.getLogoUrl(isDark = false))
        assertEquals(league.darkLogoUrl, league.getLogoUrl(isDark = true))
    }
}
