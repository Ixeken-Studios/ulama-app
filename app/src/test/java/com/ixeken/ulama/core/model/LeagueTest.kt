package com.ixeken.ulama.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la definicion inmutable del enum League y sus agrupaciones.
 * Valida la correcta categorizacion de torneos internacionales, llaves eliminatorias
 * y resolucion de logotipos optimizados para temas claros y oscuros.
 */
class LeagueTest {

    @Test
    fun standardLeagues_containsAllClubAndInternationalLeagues() {
        assertEquals(9, League.clubLeagues.size)
        assertEquals(7, League.internationalLeagues.size)
        assertEquals(16, League.standardLeagues.size)

        League.clubLeagues.forEach { league ->
            assertFalse(league.isInternational)
            assertFalse(league.isVirtual)
            assertTrue(league.code.isNotBlank())
            assertTrue(league.lightLogoUrl.isNotBlank())
            assertTrue(league.darkLogoUrl.isNotBlank())
        }

        League.internationalLeagues.forEach { league ->
            assertTrue(league.isInternational)
            assertFalse(league.isVirtual)
            assertTrue(league.code.isNotBlank())
            assertTrue(league.lightLogoUrl.isNotBlank())
            assertTrue(league.darkLogoUrl.isNotBlank())
        }
    }

    @Test
    fun internationalTournaments_codesMatchEspnEndpoints() {
        assertEquals("fifa.world", League.WORLD_CUP.code)
        assertEquals("uefa.nations", League.UEFA_NATIONS_LEAGUE.code)
        assertEquals("concacaf.nations.league", League.CONCACAF_NATIONS_LEAGUE.code)
        assertEquals("fifa.friendly", League.FRIENDLIES.code)
        assertEquals("concacaf.gold", League.GOLD_CUP.code)
        assertEquals("caf.nations", League.AFCON.code)
        assertEquals("afc.asian.cup", League.ASIAN_CUP.code)
    }

    @Test
    fun knockoutConfiguration_handlesInternationalBracketsCorrectly() {
        assertTrue(League.WORLD_CUP.hasKnockoutBracket)
        assertEquals(32, League.WORLD_CUP.knockoutBracketTeams)
        assertTrue(League.WORLD_CUP.hasThirdPlaceMatch)

        assertTrue(League.AFCON.hasKnockoutBracket)
        assertEquals(16, League.AFCON.knockoutBracketTeams)
        assertTrue(League.AFCON.hasThirdPlaceMatch)

        assertTrue(League.UEFA_NATIONS_LEAGUE.hasKnockoutBracket)
        assertEquals(8, League.UEFA_NATIONS_LEAGUE.knockoutBracketTeams)
        assertTrue(League.UEFA_NATIONS_LEAGUE.hasThirdPlaceMatch)

        assertTrue(League.CONCACAF_NATIONS_LEAGUE.hasKnockoutBracket)
        assertEquals(8, League.CONCACAF_NATIONS_LEAGUE.knockoutBracketTeams)
        assertTrue(League.CONCACAF_NATIONS_LEAGUE.hasThirdPlaceMatch)

        assertTrue(League.GOLD_CUP.hasKnockoutBracket)
        assertEquals(8, League.GOLD_CUP.knockoutBracketTeams)
        assertFalse(League.GOLD_CUP.hasThirdPlaceMatch)

        assertFalse(League.FRIENDLIES.hasKnockoutBracket)
        assertEquals(0, League.FRIENDLIES.knockoutBracketTeams)
    }

    @Test
    fun hasStandings_onlyTrueForStandardCompetitionsExcludingFriendlies() {
        assertTrue(League.LIGA_MX.hasStandings)
        assertTrue(League.PREMIER_LEAGUE.hasStandings)
        assertTrue(League.WORLD_CUP.hasStandings)
        assertFalse(League.FRIENDLIES.hasStandings)
        assertFalse(League.FAVORITES.hasStandings)
        assertFalse(League.MY_LEAGUES.hasStandings)
    }

    @Test
    fun fromCode_resolvesCorrectLeagueOrFallback() {
        assertEquals(League.WORLD_CUP, League.fromCode("fifa.world"))
        assertEquals(League.UEFA_NATIONS_LEAGUE, League.fromCode("uefa.nations"))
        assertEquals(League.LIGA_MX, League.fromCode("unknown.league.code"))
    }

    @Test
    fun tournamentConfiguration_identifiesStagesAndQueryYearsDeclaratively() {
        assertTrue(League.CHAMPIONS_LEAGUE.isTournament)
        assertTrue(League.WORLD_CUP.isTournament)
        assertTrue(League.AFCON.isTournament)
        assertFalse(League.PREMIER_LEAGUE.isTournament)
        assertFalse(League.LA_LIGA.isTournament)
        assertFalse(League.LIGA_MX.isTournament)
        assertFalse(League.FRIENDLIES.isTournament)

        assertEquals("2027", League.GOLD_CUP.defaultTournamentYear(2026))
        assertEquals("2027", League.AFCON.defaultTournamentYear(2026))
        assertEquals("2027", League.ASIAN_CUP.defaultTournamentYear(2026))
        assertEquals("2026", League.CHAMPIONS_LEAGUE.defaultTournamentYear(2026))
        assertEquals("2026", League.WORLD_CUP.defaultTournamentYear(2026))
    }

    @Test
    fun regionsAndSubRegions_coverAllStandardLeaguesDeclaratively() {
        League.standardLeagues.forEach { league ->
            org.junit.Assert.assertNotEquals(LeagueRegion.ALL, league.region)
            org.junit.Assert.assertNotEquals(LeagueRegion.OCEANIA, league.region)
            assertTrue(league.subRegionRes != 0)
        }

        val european = League.standardLeagues.filter { it.region == LeagueRegion.EUROPE }
        val england = european.filter { it.subRegionRes == com.ixeken.ulama.R.string.subregion_england }
        val europeanComps = european.filter {
            it.subRegionRes == com.ixeken.ulama.R.string.subregion_european_competitions
        }

        assertEquals(setOf(League.PREMIER_LEAGUE, League.CHAMPIONSHIP), england.toSet())
        assertEquals(setOf(League.CHAMPIONS_LEAGUE, League.UEFA_NATIONS_LEAGUE), europeanComps.toSet())
        assertEquals(5, LeagueRegion.continents.size)
    }
}
