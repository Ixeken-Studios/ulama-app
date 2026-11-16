package com.ixeken.ulama.service

import com.ixeken.ulama.core.model.FollowedMatch
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.MatchStatus
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.core.model.toFollowedMatch
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

    @Test
    fun match_toFollowedMatch_preservesTrackingMetadata() {
        val match = Match(
            id = "9001",
            dateUtc = "2026-11-16T20:00:00Z",
            homeTeam = Team(id = "86", name = "Real Madrid", shortName = "RMA", abbreviation = "RMA", logoUrl = "h.png", isHome = true, score = 2, isWinner = false, shootoutScore = 4),
            awayTeam = Team(id = "83", name = "Barcelona", shortName = "BAR", abbreviation = "BAR", logoUrl = "a.png", isHome = false, score = 2, isWinner = false, shootoutScore = 2),
            status = MatchStatus(
                state = MatchState.LIVE,
                displayClock = "120'",
                detail = "Penales",
                isCompleted = false,
                isShootout = true
            ),
            venueName = "Bernabeu",
            league = League.LA_LIGA
        )

        val followed = match.toFollowedMatch()

        assertEquals("9001", followed.matchId)
        assertEquals("esp.1", followed.leagueCode)
        assertEquals("86", followed.homeTeamId)
        assertEquals("83", followed.awayTeamId)
        assertEquals(2, followed.homeScore)
        assertEquals(2, followed.awayScore)
        assertEquals(MatchState.LIVE, followed.state)
        assertTrue(followed.isShootout)
        assertEquals(4, followed.homeShootoutScore)
        assertEquals(2, followed.awayShootoutScore)
        assertTrue(followed.startTimeUtcMillis > 0L)
    }

    @Test
    fun favoriteAutoFollowSync_reconcilesAlreadyFollowedMatch_whenMatchBecomesLive() {
        val liveMatch = Match(
            id = "701",
            dateUtc = "2026-11-16T18:00:00Z",
            homeTeam = Team(id = "227", name = "América", shortName = "AME", abbreviation = "AME", logoUrl = null, isHome = true, score = 1, isWinner = false),
            awayTeam = Team(id = "219", name = "Chivas", shortName = "CHI", abbreviation = "CHI", logoUrl = null, isHome = false, score = 0, isWinner = false),
            status = MatchStatus(state = MatchState.LIVE, displayClock = "34'", detail = "1er Tiempo", isCompleted = false),
            venueName = null,
            league = League.LIGA_MX
        )
        val staleStored = liveMatch.toFollowedMatch().copy(
            state = MatchState.SCHEDULED,
            homeScore = 0,
            awayScore = 0,
            clockDisplay = ""
        )
        val store = mutableMapOf("701" to staleStored)
        val activations = mutableListOf<Pair<String, Boolean>>()

        FavoriteAutoFollowSync.syncMatches(
            matches = listOf(liveMatch),
            favIds = setOf("227"),
            isAutoFollowEnabled = true,
            manuallyUnfollowedIds = emptySet(),
            getExistingFollowed = { store[it] },
            saveFollowed = { store[it.matchId] = it },
            leadMinutes = 5,
            nowMillis = liveMatch.toFollowedMatch().startTimeUtcMillis + 34 * 60_000L
        ) { followed, shouldStartNow ->
            activations.add(followed.matchId to shouldStartNow)
        }

        assertEquals(MatchState.LIVE, store["701"]?.state)
        assertEquals(1, store["701"]?.homeScore)
        assertEquals(listOf("701" to true), activations)
    }

    @Test
    fun favoriteAutoFollowSync_respectsManuallyUnfollowedAndLeadWindow() {
        val unfollowedMatch = Match(
            id = "702",
            dateUtc = "2026-11-16T20:00:00Z",
            homeTeam = Team(id = "227", name = "América", shortName = "AME", abbreviation = "AME", logoUrl = null, isHome = true, score = null, isWinner = false),
            awayTeam = Team(id = "220", name = "Cruz Azul", shortName = "CAZ", abbreviation = "CAZ", logoUrl = null, isHome = false, score = null, isWinner = false),
            status = MatchStatus(state = MatchState.SCHEDULED, displayClock = "", detail = "Programado", isCompleted = false),
            venueName = null,
            league = League.LIGA_MX
        )
        val imminentMatch = unfollowedMatch.copy(id = "703")
        val futureMatch = unfollowedMatch.copy(id = "704", dateUtc = "2026-11-16T23:00:00Z")

        val store = mutableMapOf<String, FollowedMatch>()
        val activations = mutableListOf<Pair<String, Boolean>>()
        val imminentKickoffMillis = imminentMatch.toFollowedMatch().startTimeUtcMillis

        FavoriteAutoFollowSync.syncMatches(
            matches = listOf(unfollowedMatch, imminentMatch, futureMatch),
            favIds = setOf("227"),
            isAutoFollowEnabled = true,
            manuallyUnfollowedIds = setOf("702"),
            getExistingFollowed = { store[it] },
            saveFollowed = { store[it.matchId] = it },
            leadMinutes = 5,
            nowMillis = imminentKickoffMillis - (3 * 60_000L)
        ) { followed, shouldStartNow ->
            activations.add(followed.matchId to shouldStartNow)
        }

        assertFalse(store.containsKey("702"))
        assertTrue(store.containsKey("703"))
        assertTrue(store.containsKey("704"))
        assertEquals(listOf("703" to true, "704" to false), activations)
    }
}
