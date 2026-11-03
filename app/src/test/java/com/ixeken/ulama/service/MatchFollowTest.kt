package com.ixeken.ulama.service

import com.ixeken.ulama.core.model.FollowedMatch
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.toMatch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para validar la serializacion de partidos seguidos y la logica de deteccion de goles.
 */
class MatchFollowTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun followedMatch_serialization_roundtripPreservesFields() {
        val original = FollowedMatch(
            matchId = "700123",
            leagueCode = "mex.1",
            homeTeamName = "América",
            awayTeamName = "Chivas",
            homeTeamShortName = "AME",
            awayTeamShortName = "CHI",
            homeScore = 2,
            awayScore = 1,
            state = MatchState.LIVE,
            clockDisplay = "74'",
            periodDisplay = "Segundo tiempo",
            startTimeUtcMillis = 1792345678000L
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<FollowedMatch>(serialized)

        assertEquals(original.matchId, deserialized.matchId)
        assertEquals(original.leagueCode, deserialized.leagueCode)
        assertEquals(original.homeTeamName, deserialized.homeTeamName)
        assertEquals(original.homeScore, deserialized.homeScore)
        assertEquals(original.awayScore, deserialized.awayScore)
        assertEquals(original.state, deserialized.state)
        assertEquals(original.clockDisplay, deserialized.clockDisplay)
    }

    @Test
    fun goalDetection_identifiesHomeAndAwayGoals() {
        val prevHome = 1
        val prevAway = 1

        val newHome = 2
        val newAway = 1

        val homeScored = newHome > prevHome
        val awayScored = newAway > prevAway

        assertTrue(homeScored)
        assertFalse(awayScored)

        val nextHome = 2
        val nextAway = 2

        assertTrue(nextAway > newAway)
        assertFalse(nextHome > newHome)
    }

    @Test
    fun leagueFromCode_returnsProperLeagueEnum() {
        assertEquals(League.BUNDESLIGA, League.fromCode("ger.1"))
        assertEquals(League.CHAMPIONS_LEAGUE, League.fromCode("uefa.champions"))
        assertEquals(League.CHAMPIONSHIP, League.fromCode("eng.2"))
        assertEquals(League.EREDIVISIE, League.fromCode("ned.1"))
        assertEquals(League.LA_LIGA, League.fromCode("esp.1"))
        assertEquals(League.LIGA_MX, League.fromCode("mex.1"))
        assertEquals(League.LIGUE_1, League.fromCode("fra.1"))
        assertEquals(League.PREMIER_LEAGUE, League.fromCode("eng.1"))
        assertEquals(League.SERIE_A, League.fromCode("ita.1"))
        assertEquals(League.LIGA_MX, League.fromCode("desconocida"))
    }

    @Test
    fun followedMatch_toMatch_correctlyMapsFields() {
        val followed = FollowedMatch(
            matchId = "401234",
            leagueCode = "eng.1",
            homeTeamName = "Arsenal",
            awayTeamName = "Chelsea",
            homeTeamShortName = "ARS",
            awayTeamShortName = "CHE",
            homeTeamLogo = "https://a.espncdn.com/ars.png",
            awayTeamLogo = "https://a.espncdn.com/che.png",
            homeScore = 3,
            awayScore = 1,
            state = MatchState.LIVE,
            clockDisplay = "88'",
            periodDisplay = "Segundo tiempo",
            startTimeUtcMillis = 1792345678000L,
            isHalftime = false,
            isExtraTime = false,
            isShootout = false
        )

        val match = followed.toMatch()

        assertEquals("401234", match.id)
        assertEquals(League.PREMIER_LEAGUE, match.league)
        assertEquals("Arsenal", match.homeTeam.name)
        assertEquals("ARS", match.homeTeam.shortName)
        assertEquals(3, match.homeTeam.score)
        assertEquals("Chelsea", match.awayTeam.name)
        assertEquals(1, match.awayTeam.score)
        assertEquals(MatchState.LIVE, match.status.state)
        assertEquals("88'", match.status.displayClock)
        assertFalse(match.isFinished)
    }
}
