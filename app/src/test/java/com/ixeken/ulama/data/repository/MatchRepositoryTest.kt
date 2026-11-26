package com.ixeken.ulama.data.repository

import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.data.espn.EspnApiService
import com.ixeken.ulama.data.espn.model.EspnCompetitionDto
import com.ixeken.ulama.data.espn.model.EspnCompetitorDto
import com.ixeken.ulama.data.espn.model.EspnEventDto
import com.ixeken.ulama.data.espn.model.EspnLeagueCalendarDto
import com.ixeken.ulama.data.espn.model.EspnLeagueSeasonDto
import com.ixeken.ulama.data.espn.model.EspnSeasonDto
import com.ixeken.ulama.data.espn.model.EspnScoreboardResponse
import com.ixeken.ulama.data.espn.model.EspnStandingsChildDto
import com.ixeken.ulama.data.espn.model.EspnStandingsEntriesDto
import com.ixeken.ulama.data.espn.model.EspnStandingsEntryDto
import com.ixeken.ulama.data.espn.model.EspnStandingsResponse
import com.ixeken.ulama.data.espn.model.EspnStandingsTeamDto
import com.ixeken.ulama.data.espn.model.EspnStatDto
import com.ixeken.ulama.data.espn.model.EspnStatusDto
import com.ixeken.ulama.data.espn.model.EspnStatusTypeDto
import com.ixeken.ulama.data.espn.model.EspnTeamDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Prueba unitaria para validar la transformacion y robustez de MatchRepositoryImpl.
 */
class MatchRepositoryTest {

    private val fakeApiService = object : EspnApiService {
        override suspend fun getScoreboard(
            league: String,
            dates: String?,
            limit: Int?,
            cacheControl: String?
        ): EspnScoreboardResponse {
            return EspnScoreboardResponse(
                events = listOf(
                    EspnEventDto(
                        id = "101",
                        date = "2026-10-10T01:00Z",
                        name = "Puebla vs Leon",
                        competitions = listOf(
                            EspnCompetitionDto(
                                id = "101",
                                date = "2026-10-10T01:00Z",
                                competitors = listOf(
                                    EspnCompetitorDto(
                                        id = "231",
                                        homeAway = "home",
                                        score = "2",
                                        winner = true,
                                        team = EspnTeamDto(
                                            id = "231",
                                            name = "Puebla",
                                            displayName = "Puebla",
                                            abbreviation = "PUE"
                                        )
                                    ),
                                    EspnCompetitorDto(
                                        id = "228",
                                        homeAway = "away",
                                        score = "1",
                                        winner = false,
                                        team = EspnTeamDto(
                                            id = "228",
                                            name = "Leon",
                                            displayName = "Leon",
                                            abbreviation = "LEO"
                                        )
                                    )
                                ),
                                status = EspnStatusDto(
                                    displayClock = "90'",
                                    type = EspnStatusTypeDto(
                                        state = "post",
                                        completed = true,
                                        detail = "Final"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse {
            if (league == League.WORLD_CUP.code) {
                return EspnStandingsResponse(
                    children = listOf(
                        EspnStandingsChildDto(
                            name = "Group A",
                            standings = EspnStandingsEntriesDto(
                                entries = listOf(
                                    EspnStandingsEntryDto(
                                        team = EspnStandingsTeamDto(id = "1", name = "Czechia", displayName = "Czechia"),
                                        stats = listOf(
                                            EspnStatDto(name = "rank", value = 4.0),
                                            EspnStatDto(name = "points", value = 1.0)
                                        )
                                    ),
                                    EspnStandingsEntryDto(
                                        team = EspnStandingsTeamDto(id = "2", name = "Mexico", displayName = "Mexico"),
                                        stats = listOf(
                                            EspnStatDto(name = "rank", value = 1.0),
                                            EspnStatDto(name = "points", value = 9.0)
                                        )
                                    )
                                )
                            )
                        ),
                        EspnStandingsChildDto(
                            name = "Group B",
                            standings = EspnStandingsEntriesDto(
                                entries = listOf(
                                    EspnStandingsEntryDto(
                                        team = EspnStandingsTeamDto(id = "3", name = "Canada", displayName = "Canada"),
                                        stats = listOf(
                                            EspnStatDto(name = "rank", value = 2.0),
                                            EspnStatDto(name = "points", value = 4.0)
                                        )
                                    ),
                                    EspnStandingsEntryDto(
                                        team = EspnStandingsTeamDto(id = "4", name = "Switzerland", displayName = "Switzerland"),
                                        stats = listOf(
                                            EspnStatDto(name = "rank", value = 1.0),
                                            EspnStatDto(name = "points", value = 7.0)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }
            return EspnStandingsResponse(
                children = listOf(
                    EspnStandingsChildDto(
                        standings = EspnStandingsEntriesDto(
                            entries = listOf(
                                EspnStandingsEntryDto(
                                    team = EspnStandingsTeamDto(
                                        id = "223",
                                        name = "Toluca",
                                        displayName = "Toluca",
                                        abbreviation = "TOL"
                                    ),
                                    stats = listOf(
                                        EspnStatDto(name = "rank", value = 1.0),
                                        EspnStatDto(name = "points", value = 25.0),
                                        EspnStatDto(name = "gamesPlayed", value = 10.0),
                                        EspnStatDto(name = "wins", value = 8.0),
                                        EspnStatDto(name = "ties", value = 1.0),
                                        EspnStatDto(name = "losses", value = 1.0),
                                        EspnStatDto(name = "pointsFor", value = 20.0),
                                        EspnStatDto(name = "pointsAgainst", value = 8.0),
                                        EspnStatDto(name = "pointDifferential", value = 12.0)
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?): com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse {
            return com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                boxscore = com.ixeken.ulama.data.espn.model.EspnBoxscoreDto(
                    teams = listOf(
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(
                            team = EspnTeamDto(id = "231", name = "Puebla", displayName = "Puebla"),
                            statistics = listOf(
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "possessionPct", displayValue = "48.0"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "totalShots", displayValue = "12")
                            )
                        ),
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(
                            team = EspnTeamDto(id = "228", name = "Leon", displayName = "Leon"),
                            statistics = listOf(
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "possessionPct", displayValue = "52.0"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "totalShots", displayValue = "14")
                            )
                        )
                    )
                ),
                rosters = listOf(
                    com.ixeken.ulama.data.espn.model.EspnRosterTeamDto(
                        team = EspnTeamDto(id = "231", name = "Puebla", displayName = "Puebla"),
                        formation = "4-4-2",
                        roster = listOf(
                            com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                starter = true,
                                jersey = "1",
                                formationPlace = 1,
                                position = com.ixeken.ulama.data.espn.model.EspnPlayerPositionDto(name = "Goalkeeper"),
                                athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(displayName = "J. Rodríguez", shortName = "J. Rodríguez")
                            ),
                            com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                starter = false,
                                jersey = "12",
                                subbedIn = true,
                                subbedInFor = com.ixeken.ulama.data.espn.model.EspnSubbedInForDto(
                                    jersey = "1",
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(displayName = "J. Rodríguez", shortName = "J. Rodríguez")
                                ),
                                plays = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(
                                        substitution = true,
                                        clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(displayValue = "65'")
                                    )
                                ),
                                athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(displayName = "M. Fraga")
                            )
                        )
                    )
                ),
                header = com.ixeken.ulama.data.espn.model.EspnSummaryHeaderDto(
                    competitions = listOf(
                        com.ixeken.ulama.data.espn.model.EspnSummaryCompetitionDto(
                            details = listOf(
                                com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto(
                                    scoringPlay = true,
                                    clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(displayValue = "34'"),
                                    team = com.ixeken.ulama.data.espn.model.EspnSummaryTeamRefDto(id = "231"),
                                    participants = listOf(
                                        com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                            athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(displayName = "L. Cavallini")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    private val repository = MatchRepositoryImpl(
        apiService = fakeApiService,
        ioDispatcher = Dispatchers.Unconfined
    )

    @Test
    fun getMatches_mapsEspnEventToImmutableMatch() = runBlocking {
        val result = repository.getMatches(League.LIGA_MX)
        assertTrue(result.isSuccess)
        val matches = result.getOrThrow()
        assertEquals(1, matches.size)

        val match = matches.first()
        assertEquals("101", match.id)
        assertEquals("Puebla", match.homeTeam.name)
        assertEquals(2, match.homeTeam.score)
        assertTrue(match.homeTeam.isWinner)
        assertEquals("Leon", match.awayTeam.name)
        assertEquals(1, match.awayTeam.score)
        assertEquals(MatchState.FINISHED, match.status.state)
        assertTrue(match.status.isCompleted)
    }

    @Test
    fun getStandings_mapsEspnStandingsToImmutableStandingEntry() = runBlocking {
        val result = repository.getStandings(League.LIGA_MX)
        assertTrue(result.isSuccess)
        val standings = result.getOrThrow()
        assertEquals(1, standings.size)

        val entry = standings.first()
        assertEquals(1, entry.rank)
        assertEquals("Toluca", entry.teamName)
        assertEquals("223", entry.teamId)
        assertEquals(25, entry.points)
        assertEquals(10, entry.gamesPlayed)
        assertEquals(8, entry.wins)
        assertEquals(1, entry.draws)
        assertEquals(1, entry.losses)
        assertEquals(12, entry.goalDifference)
    }

    @Test
    fun getGroups_parsesAllGroupsAndOrdersByPointsAndRank() = runBlocking {
        val result = repository.getGroups(League.WORLD_CUP)
        assertTrue(result.isSuccess)
        val groups = result.getOrThrow()
        assertEquals(2, groups.size)

        // Grupo A
        val groupA = groups[0]
        assertEquals("A", groupA.name)
        assertEquals(2, groupA.standings.size)
        // Verificacion de ordenamiento: Mexico (9 pts, rank 1) debe quedar antes que Czechia (1 pt, rank 4)
        assertEquals("Mexico", groupA.standings[0].teamName)
        assertEquals(1, groupA.standings[0].rank)
        assertEquals(9, groupA.standings[0].points)
        assertEquals("Czechia", groupA.standings[1].teamName)
        assertEquals(4, groupA.standings[1].rank)
        assertEquals(1, groupA.standings[1].points)
        assertEquals(1, groupA.cutoffs.size)
        assertEquals(2, groupA.cutoffs[0].rankAfter)

        // Grupo B
        val groupB = groups[1]
        assertEquals("B", groupB.name)
        assertEquals(2, groupB.standings.size)
        // Verificacion de ordenamiento: Switzerland (7 pts, rank 1) antes que Canada (4 pts, rank 2)
        assertEquals("Switzerland", groupB.standings[0].teamName)
        assertEquals(1, groupB.standings[0].rank)
        assertEquals(7, groupB.standings[0].points)
        assertEquals("Canada", groupB.standings[1].teamName)
        assertEquals(2, groupB.standings[1].rank)
        assertEquals(4, groupB.standings[1].points)
    }

    @Test
    fun getStandings_forMultiGroupTournament_returnsAllTeamsFlattened() = runBlocking {
        val result = repository.getStandings(League.WORLD_CUP)
        assertTrue(result.isSuccess)
        val standings = result.getOrThrow()
        assertEquals(4, standings.size)
        assertTrue(standings.any { it.teamName == "Mexico" })
        assertTrue(standings.any { it.teamName == "Switzerland" })
    }

    @Test
    fun resolveMatchdayDates_groupsWeekendMatchdayProperly() {
        val calendar = listOf(
            "2026-10-09T07:00Z",
            "2026-10-10T07:00Z",
            "2026-10-11T07:00Z",
            "2026-10-16T07:00Z"
        )
        val today = java.time.LocalDate.of(2026, 10, 6)
        val dates = MatchRepositoryImpl.resolveMatchdayDates(calendar, today)

        assertEquals(listOf("20261009", "20261010", "20261011"), dates)
    }

    @Test
    fun resolveMatchdayDates_groupsMidweekDoubleMatchdayProperly() {
        val calendar = listOf(
            "2026-10-16T07:00Z",
            "2026-10-17T07:00Z",
            "2026-10-18T07:00Z",
            "2026-10-20T07:00Z",
            "2026-10-21T07:00Z",
            "2026-10-23T07:00Z"
        )
        val today = java.time.LocalDate.of(2026, 10, 19)
        val dates = MatchRepositoryImpl.resolveMatchdayDates(calendar, today)

        assertTrue(dates.contains("20261020"))
        assertTrue(dates.contains("20261021"))
        // No debe mezclar con la siguiente jornada del fin de semana (23 de octubre)
        assertTrue(!dates.contains("20261023"))
    }

    @Test
    fun resolveMatchdayDates_emptyCalendar_returnsFallbackClusterAroundToday() {
        val calendar = emptyList<String>()
        val today = java.time.LocalDate.of(2026, 10, 13)
        val dates = MatchRepositoryImpl.resolveMatchdayDates(calendar, today)

        assertEquals(7, dates.size)
        assertTrue(dates.contains("20261013"))
        assertTrue(dates.contains("20261014"))
        assertTrue(dates.contains("20261010"))
        assertTrue(dates.contains("20261016"))
    }

    @Test
    fun matchModel_correctlyIdentifiesWinnerAndLoserWithOpacityStates() {
        val homeWinnerMatch = com.ixeken.ulama.core.model.Match(
            id = "1",
            dateUtc = "2026-10-06T00:00:00Z",
            homeTeam = com.ixeken.ulama.core.model.Team("1", "Necaxa", "Necaxa", "NEC", null, true, 2, false),
            awayTeam = com.ixeken.ulama.core.model.Team("2", "América", "América", "AME", null, false, 4, true),
            status = com.ixeken.ulama.core.model.MatchStatus(MatchState.FINISHED, "FT", "Final", true),
            venueName = "Estadio Victoria"
        )

        assertTrue(homeWinnerMatch.isFinished)
        assertTrue(homeWinnerMatch.isAwayWinner)
        assertTrue(homeWinnerMatch.isHomeLoser)
        org.junit.Assert.assertFalse(homeWinnerMatch.isAwayLoser)
        org.junit.Assert.assertFalse(homeWinnerMatch.isDraw)

        val drawMatch = homeWinnerMatch.copy(
            homeTeam = homeWinnerMatch.homeTeam.copy(score = 2, isWinner = false),
            awayTeam = homeWinnerMatch.awayTeam.copy(score = 2, isWinner = false)
        )
        assertTrue(drawMatch.isDraw)
        org.junit.Assert.assertFalse(drawMatch.isHomeLoser)
        org.junit.Assert.assertFalse(drawMatch.isAwayLoser)

        val liveMatch = homeWinnerMatch.copy(
            status = com.ixeken.ulama.core.model.MatchStatus(MatchState.LIVE, "45'", "1T", false)
        )
        org.junit.Assert.assertFalse(liveMatch.isFinished)
        org.junit.Assert.assertFalse(liveMatch.isHomeLoser)
        org.junit.Assert.assertFalse(liveMatch.isAwayLoser)
    }

    @Test
    fun getMatchDetail_mapsSummaryCorrectly() = runBlocking {
        val result = repository.getMatchDetail(League.LIGA_MX, "101")
        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()

        assertEquals("101", detail.matchId)
        assertEquals(1, detail.homeScorers.size)
        assertEquals("L. Cavallini", detail.homeScorers.first().playerName)
        assertEquals("34'", detail.homeScorers.first().minutesDisplay)
        assertTrue(detail.awayScorers.isEmpty())

        assertEquals(2, detail.stats.size)
        assertEquals("48%", detail.stats.first().homeDisplayValue)
        assertEquals("52%", detail.stats.first().awayDisplayValue)

        val homeLineup = detail.homeLineup
        org.junit.Assert.assertNotNull(homeLineup)
        assertEquals("4-4-2", homeLineup?.formation)
        assertEquals(1, homeLineup?.starters?.size)
        assertEquals(1, homeLineup?.bench?.size)
        assertEquals(com.ixeken.ulama.core.model.TacticalLine.GOALKEEPER, homeLineup?.starters?.first()?.line)

        val benchPlayer = homeLineup?.bench?.first()
        org.junit.Assert.assertNotNull(benchPlayer)
        assertEquals("M. Fraga", benchPlayer?.name)
        assertTrue(benchPlayer?.subbedIn == true)
        assertEquals("65'", benchPlayer?.subInMinute)
        assertEquals("J. Rodríguez", benchPlayer?.subbedInForName)
        assertEquals("1", benchPlayer?.subbedInForJersey)
    }

    @Test
    fun scoreboardResponse_deserializesCalendarWithObjectsGracefully() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val rawJson = """
            {
                "leagues": [
                    {
                        "calendar": [
                            {
                                "label": "UEFA Champions League",
                                "startDate": "2026-07-01T04:00Z",
                                "endDate": "2027-07-01T03:59Z"
                            }
                        ]
                    }
                ],
                "events": []
            }
        """.trimIndent()

        val parsed = json.decodeFromString<EspnScoreboardResponse>(rawJson)
        assertEquals(1, parsed.leagues.size)
        assertTrue(parsed.leagues.first().extractDateStrings().isEmpty())
    }

    @Test
    fun getMatches_distinguishesHalftimeFromSecondHalfAndFullTime() = runBlocking {
        val liveSecondHalfApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse {
                return EspnScoreboardResponse(
                    events = listOf(
                        EspnEventDto(
                            id = "201",
                            date = "2026-10-12T20:00Z",
                            name = "Levante vs Sevilla",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "201",
                                    date = "2026-10-12T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "1", homeAway = "home", score = "3", team = EspnTeamDto(id = "1", name = "Levante")),
                                        EspnCompetitorDto(id = "2", homeAway = "away", score = "3", team = EspnTeamDto(id = "2", name = "Sevilla"))
                                    ),
                                    status = EspnStatusDto(
                                        displayClock = "90'+10'",
                                        type = EspnStatusTypeDto(
                                            state = "in",
                                            name = "STATUS_SECOND_HALF",
                                            description = "Second Half",
                                            detail = "2nd Half",
                                            completed = false
                                        )
                                    )
                                )
                            )
                        ),
                        EspnEventDto(
                            id = "202",
                            date = "2026-10-12T20:00Z",
                            name = "Betis vs Celta",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "202",
                                    date = "2026-10-12T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "3", homeAway = "home", score = "1", team = EspnTeamDto(id = "3", name = "Betis")),
                                        EspnCompetitorDto(id = "4", homeAway = "away", score = "0", team = EspnTeamDto(id = "4", name = "Celta"))
                                    ),
                                    status = EspnStatusDto(
                                        displayClock = "HT",
                                        type = EspnStatusTypeDto(
                                            state = "in",
                                            name = "STATUS_HALFTIME",
                                            description = "Halftime",
                                            detail = "HT",
                                            completed = false
                                        )
                                    )
                                )
                            )
                        ),
                        EspnEventDto(
                            id = "203",
                            date = "2026-10-12T20:00Z",
                            name = "Valencia vs Villarreal",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "203",
                                    date = "2026-10-12T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "5", homeAway = "home", score = "2", team = EspnTeamDto(id = "5", name = "Valencia")),
                                        EspnCompetitorDto(id = "6", homeAway = "away", score = "1", team = EspnTeamDto(id = "6", name = "Villarreal"))
                                    ),
                                    status = EspnStatusDto(
                                        displayClock = "90'",
                                        type = EspnStatusTypeDto(
                                            state = "in",
                                            name = "STATUS_FULL_TIME",
                                            description = "Full Time",
                                            detail = "FT",
                                            completed = true
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?): com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val testRepo = MatchRepositoryImpl(liveSecondHalfApi, Dispatchers.Unconfined)
        val matches = testRepo.getMatches(League.LA_LIGA, forceRefresh = true).getOrThrow()

        val secondHalfMatch = matches.first { it.id == "201" }
        assertEquals(MatchState.LIVE, secondHalfMatch.status.state)
        org.junit.Assert.assertFalse(secondHalfMatch.isHalftime)
        assertEquals("90'+10'", secondHalfMatch.status.displayClock)

        val halftimeMatch = matches.first { it.id == "202" }
        assertEquals(MatchState.LIVE, halftimeMatch.status.state)
        assertTrue(halftimeMatch.isHalftime)

        val fullTimeMatch = matches.first { it.id == "203" }
        assertEquals(MatchState.FINISHED, fullTimeMatch.status.state)
        org.junit.Assert.assertFalse(fullTimeMatch.isHalftime)
    }

    @Test
    fun getMatches_resolvesExtraTimeAndPenaltyShootoutsCorrectly() = runBlocking {
        val etAndPenaltiesApi = object : EspnApiService {
            override suspend fun getScoreboard(
                league: String,
                dates: String?,
                limit: Int?,
                cacheControl: String?
            ): EspnScoreboardResponse {
                return EspnScoreboardResponse(
                    events = listOf(
                        // 1. Tiempos extras en vivo
                        EspnEventDto(
                            id = "301",
                            date = "2026-10-12T20:00Z",
                            name = "Arsenal vs Chelsea",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "301",
                                    date = "2026-10-12T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "1", homeAway = "home", score = "2", team = EspnTeamDto(id = "1", name = "Arsenal")),
                                        EspnCompetitorDto(id = "2", homeAway = "away", score = "2", team = EspnTeamDto(id = "2", name = "Chelsea"))
                                    ),
                                    status = EspnStatusDto(
                                        clock = 114.0,
                                        displayClock = "114'",
                                        period = 4,
                                        type = EspnStatusTypeDto(
                                            state = "in",
                                            name = "STATUS_SECOND_HALF_EXTRA_TIME",
                                            description = "Second Half Extra Time",
                                            detail = "2ET 114'",
                                            completed = false
                                        )
                                    )
                                )
                            )
                        ),
                        // 2. Finalizado tras tiempos extras
                        EspnEventDto(
                            id = "302",
                            date = "2026-10-12T20:00Z",
                            name = "Real Madrid vs Atletico",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "302",
                                    date = "2026-10-12T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "3", homeAway = "home", score = "2", winner = true, team = EspnTeamDto(id = "3", name = "Real Madrid")),
                                        EspnCompetitorDto(id = "4", homeAway = "away", score = "1", winner = false, team = EspnTeamDto(id = "4", name = "Atletico"))
                                    ),
                                    status = EspnStatusDto(
                                        clock = 120.0,
                                        displayClock = "120'",
                                        period = 4,
                                        type = EspnStatusTypeDto(
                                            state = "post",
                                            name = "STATUS_FINAL_AET",
                                            description = "Full Time (AET)",
                                            detail = "AET",
                                            completed = true
                                        )
                                    )
                                )
                            )
                        ),
                        // 3. Finalizado en penales con shootoutScore en competidores
                        EspnEventDto(
                            id = "303",
                            date = "2026-10-12T20:00Z",
                            name = "Levante vs Sevilla",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "303",
                                    date = "2026-10-12T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "5", homeAway = "home", score = "3", shootoutScore = 4, winner = true, team = EspnTeamDto(id = "5", name = "Levante")),
                                        EspnCompetitorDto(id = "6", homeAway = "away", score = "3", shootoutScore = 2, winner = false, team = EspnTeamDto(id = "6", name = "Sevilla"))
                                    ),
                                    status = EspnStatusDto(
                                        clock = 120.0,
                                        displayClock = "",
                                        period = 5,
                                        type = EspnStatusTypeDto(
                                            state = "post",
                                            name = "STATUS_FINAL_PEN",
                                            description = "Shootout",
                                            detail = "FT-Pens",
                                            completed = true
                                        )
                                    )
                                )
                            )
                        ),
                        // 4. Finalizado en penales con fallback de shootout en detail
                        EspnEventDto(
                            id = "304",
                            date = "2026-10-12T20:00Z",
                            name = "Liverpool vs Man City",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "304",
                                    date = "2026-10-12T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "7", homeAway = "home", score = "1", winner = false, team = EspnTeamDto(id = "7", name = "Liverpool")),
                                        EspnCompetitorDto(id = "8", homeAway = "away", score = "1", winner = true, team = EspnTeamDto(id = "8", name = "Man City"))
                                    ),
                                    status = EspnStatusDto(
                                        clock = 120.0,
                                        displayClock = "",
                                        period = 5,
                                        type = EspnStatusTypeDto(
                                            state = "post",
                                            name = "STATUS_FINAL_PEN",
                                            description = "Shootout",
                                            detail = "FT-Pens (3-5)",
                                            completed = true
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?): com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(etAndPenaltiesApi, Dispatchers.Unconfined)
        val matches = repo.getMatches(League.PREMIER_LEAGUE, forceRefresh = true).getOrThrow()

        // 1. Validar tiempos extras en vivo
        val liveEtMatch = matches.first { it.id == "301" }
        assertEquals(MatchState.LIVE, liveEtMatch.status.state)
        assertTrue(liveEtMatch.status.isExtraTime)
        org.junit.Assert.assertFalse(liveEtMatch.status.isShootout)
        org.junit.Assert.assertFalse(liveEtMatch.status.isFinishedAet)

        // 2. Validar finalizado tras tiempos extras
        val finishedAetMatch = matches.first { it.id == "302" }
        assertEquals(MatchState.FINISHED, finishedAetMatch.status.state)
        assertTrue(finishedAetMatch.status.isFinishedAet)
        org.junit.Assert.assertFalse(finishedAetMatch.status.isFinishedPenalties)
        assertTrue(finishedAetMatch.isHomeWinner)

        // 3. Validar penales con shootoutScore explicito
        val shootoutMatch = matches.first { it.id == "303" }
        assertEquals(MatchState.FINISHED, shootoutMatch.status.state)
        assertTrue(shootoutMatch.status.isFinishedPenalties)
        org.junit.Assert.assertFalse(shootoutMatch.status.isFinishedAet)
        assertEquals(4, shootoutMatch.homeTeam.shootoutScore)
        assertEquals(2, shootoutMatch.awayTeam.shootoutScore)
        assertTrue(shootoutMatch.isHomeWinner)
        org.junit.Assert.assertFalse(shootoutMatch.isDraw)

        // 4. Validar penales con fallback desde detail "FT-Pens (3-5)"
        val fallbackShootoutMatch = matches.first { it.id == "304" }
        assertEquals(MatchState.FINISHED, fallbackShootoutMatch.status.state)
        assertTrue(fallbackShootoutMatch.status.isFinishedPenalties)
        assertEquals(3, fallbackShootoutMatch.homeTeam.shootoutScore)
        assertEquals(5, fallbackShootoutMatch.awayTeam.shootoutScore)
        assertTrue(fallbackShootoutMatch.isAwayWinner)
        org.junit.Assert.assertFalse(fallbackShootoutMatch.isDraw)
    }

    @Test
    fun espnScoreboardDto_deserializesKnockoutSeriesAndLegDetails() {
        val jsonParser = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val jsonString = """
            {
              "events": [
                {
                  "id": "701985",
                  "date": "2024-05-07T19:00Z",
                  "name": "Borussia Dortmund at Paris Saint-Germain",
                  "competitions": [
                    {
                      "id": "701985",
                      "date": "2024-05-07T19:00Z",
                      "leg": {
                        "value": 2,
                        "displayValue": "2nd Leg"
                      },
                      "series": {
                        "title": "Semifinals",
                        "completed": true,
                        "totalCompetitions": 2,
                        "competitors": [
                          {
                            "id": "160",
                            "winner": false,
                            "aggregateScore": 0.0
                          },
                          {
                            "id": "124",
                            "winner": true,
                            "aggregateScore": 2.0
                          }
                        ]
                      },
                      "notes": [
                        {
                          "text": "2nd Leg - Borussia Dortmund advance 2-0 on aggregate"
                        }
                      ],
                      "competitors": [
                        {
                          "id": "160",
                          "homeAway": "home",
                          "winner": false,
                          "score": "0",
                          "team": {
                            "id": "160",
                            "name": "Paris Saint-Germain",
                            "displayName": "Paris Saint-Germain"
                          }
                        },
                        {
                          "id": "124",
                          "homeAway": "away",
                          "winner": true,
                          "score": "1",
                          "team": {
                            "id": "124",
                            "name": "Borussia Dortmund",
                            "displayName": "Borussia Dortmund"
                          }
                        }
                      ],
                      "status": {
                        "type": {
                          "state": "post",
                          "completed": true
                        }
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = jsonParser.decodeFromString<EspnScoreboardResponse>(jsonString)
        val competition = parsed.events.first().competitions.first()

        assertEquals(2, competition.leg?.value)
        assertEquals("2nd Leg", competition.leg?.displayValue)
        assertEquals("Semifinals", competition.series?.title)
        assertTrue(competition.series?.completed == true)
        assertEquals(2, competition.series?.totalCompetitions)

        val dortmundSeries = competition.series?.competitors?.find { it.id == "124" }
        val psgSeries = competition.series?.competitors?.find { it.id == "160" }

        assertEquals(2.0, dortmundSeries?.aggregateScore)
        assertTrue(dortmundSeries?.winner == true)
        assertEquals(0.0, psgSeries?.aggregateScore)
        org.junit.Assert.assertFalse(psgSeries?.winner == true)

        assertEquals("2nd Leg - Borussia Dortmund advance 2-0 on aggregate", competition.notes.firstOrNull()?.text)
    }

    @Test
    fun getMatches_knockoutEvent_correctlyMapsSeriesLegAndAggregateScores() = runBlocking {
        val knockoutJson = """
            {
              "events": [
                {
                  "id": "701985",
                  "date": "2024-05-07T19:00Z",
                  "name": "Borussia Dortmund at Paris Saint-Germain",
                  "competitions": [
                    {
                      "id": "701985",
                      "date": "2024-05-07T19:00Z",
                      "leg": {
                        "value": 2,
                        "displayValue": "2nd Leg"
                      },
                      "series": {
                        "title": "Semifinals",
                        "completed": true,
                        "totalCompetitions": 2,
                        "competitors": [
                          {
                            "id": "160",
                            "winner": false,
                            "aggregateScore": 0.0
                          },
                          {
                            "id": "124",
                            "winner": true,
                            "aggregateScore": 2.0
                          }
                        ]
                      },
                      "notes": [
                        {
                          "text": "2nd Leg - Borussia Dortmund advance 2-0 on aggregate"
                        }
                      ],
                      "competitors": [
                        {
                          "id": "160",
                          "homeAway": "home",
                          "winner": false,
                          "score": "0",
                          "team": {
                            "id": "160",
                            "name": "Paris Saint-Germain",
                            "displayName": "Paris Saint-Germain"
                          }
                        },
                        {
                          "id": "124",
                          "homeAway": "away",
                          "winner": true,
                          "score": "1",
                          "team": {
                            "id": "124",
                            "name": "Borussia Dortmund",
                            "displayName": "Borussia Dortmund"
                          }
                        }
                      ],
                      "status": {
                        "type": {
                          "state": "post",
                          "completed": true
                        }
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val fakeScoreboard = jsonParser.decodeFromString<EspnScoreboardResponse>(knockoutJson)

        val api = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = fakeScoreboard
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?): com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repository = MatchRepositoryImpl(api, Dispatchers.Unconfined)
        val matches = repository.getMatches(League.CHAMPIONS_LEAGUE, dates = "20240507", forceRefresh = true).getOrThrow()

        assertEquals(1, matches.size)
        val match = matches.first()

        assertTrue(match.isKnockout)
        assertTrue(match.isAggregateActive)
        assertEquals("Semifinals", match.seriesTitle)
        assertEquals(2, match.legNumber)
        assertEquals("2nd Leg", match.legText)
        assertEquals(0, match.aggregateHomeScore)
        assertEquals(2, match.aggregateAwayScore)
        assertEquals("2nd Leg - Borussia Dortmund advance 2-0 on aggregate", match.seriesSummary)
        assertTrue(match.isFinished)
        assertTrue(match.isAwayWinner)
        org.junit.Assert.assertFalse(match.isHomeWinner)
    }

    @Test
    fun getMatches_cleansPre2027MatchesForGoldCupAndAfcon() = runBlocking {
        val staleApi = object : EspnApiService {
            override suspend fun getScoreboard(
                league: String,
                dates: String?,
                limit: Int?,
                cacheControl: String?
            ): EspnScoreboardResponse {
                return EspnScoreboardResponse(
                    events = listOf(
                        EspnEventDto(
                            id = "gc_2025",
                            date = "2025-07-06T23:00Z",
                            name = "Mexico vs United States",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "gc_2025",
                                    date = "2025-07-06T23:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "1", homeAway = "home", score = "2", winner = true, team = EspnTeamDto(id = "1", name = "Mexico")),
                                        EspnCompetitorDto(id = "2", homeAway = "away", score = "1", winner = false, team = EspnTeamDto(id = "2", name = "United States"))
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repository = MatchRepositoryImpl(staleApi, Dispatchers.Unconfined)
        val goldCupMatches = repository.getMatches(League.GOLD_CUP, forceRefresh = true).getOrThrow()
        assertTrue(goldCupMatches.isEmpty())

        val afconMatches = repository.getMatches(League.AFCON, forceRefresh = true).getOrThrow()
        assertTrue(afconMatches.isEmpty())

        val goldCupGroups = repository.getGroups(League.GOLD_CUP, forceRefresh = true).getOrThrow()
        assertTrue(goldCupGroups.isEmpty())
    }

    @Test
    fun getMatches_tournamentLeaguesQueryFullSeasonAndIncludeUpcomingMatches() = runBlocking {
        var capturedDates: String? = null
        var capturedLimit: Int? = null

        val tournamentApi = object : EspnApiService {
            override suspend fun getScoreboard(
                league: String,
                dates: String?,
                limit: Int?,
                cacheControl: String?
            ): EspnScoreboardResponse {
                capturedDates = dates
                capturedLimit = limit
                return EspnScoreboardResponse(
                    events = listOf(
                        EspnEventDto(
                            id = "ucl_past",
                            date = "2026-10-14T19:00Z",
                            name = "Real Madrid at AS Roma",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "ucl_past",
                                    date = "2026-10-14T19:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "1", homeAway = "home", score = "1", team = EspnTeamDto(id = "1", name = "AS Roma")),
                                        EspnCompetitorDto(id = "2", homeAway = "away", score = "2", team = EspnTeamDto(id = "2", name = "Real Madrid"))
                                    )
                                )
                            )
                        ),
                        EspnEventDto(
                            id = "ucl_upcoming",
                            date = "2026-10-20T19:00Z",
                            name = "Barcelona at Paris Saint-Germain",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "ucl_upcoming",
                                    date = "2026-10-20T19:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "3", homeAway = "home", team = EspnTeamDto(id = "3", name = "Paris Saint-Germain")),
                                        EspnCompetitorDto(id = "4", homeAway = "away", team = EspnTeamDto(id = "4", name = "Barcelona"))
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repository = MatchRepositoryImpl(tournamentApi, Dispatchers.Unconfined)
        val matches = repository.getMatches(League.CHAMPIONS_LEAGUE, forceRefresh = true).getOrThrow()

        assertEquals(2, matches.size)
        assertEquals(java.time.LocalDate.now().year.toString(), capturedDates)
        assertEquals(200, capturedLimit)
        assertTrue(matches.any { it.id == "ucl_upcoming" && it.dateUtc.contains("2026-10-20") })
    }

    @Test
    fun getMatches_tournamentLeaguesSanitizeHistoricalEventsFromPriorSeason() = runBlocking {
        val mixedApi = object : EspnApiService {
            override suspend fun getScoreboard(
                league: String,
                dates: String?,
                limit: Int?,
                cacheControl: String?
            ): EspnScoreboardResponse {
                return EspnScoreboardResponse(
                    leagues = listOf(
                        EspnLeagueCalendarDto(
                            season = EspnLeagueSeasonDto(
                                year = 2026,
                                startDate = "2026-07-01T04:00Z",
                                endDate = "2027-07-01T03:59Z",
                                displayName = "2026-27 UEFA Champions League"
                            )
                        )
                    ),
                    events = listOf(
                        // Partido de eliminatoria de la edicion anterior concluida en mayo
                        EspnEventDto(
                            id = "ucl_old_final",
                            date = "2026-05-30T19:00Z",
                            name = "PSG vs Arsenal",
                            season = EspnSeasonDto(year = 2025, slug = "final"),
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "ucl_old_final",
                                    date = "2026-05-30T19:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "1", homeAway = "home", score = "2", winner = true, team = EspnTeamDto(id = "1", name = "PSG")),
                                        EspnCompetitorDto(id = "2", homeAway = "away", score = "1", winner = false, team = EspnTeamDto(id = "2", name = "Arsenal"))
                                    )
                                )
                            )
                        ),
                        // Partido de fase de liga de la nueva edicion en octubre
                        EspnEventDto(
                            id = "ucl_new_league_phase",
                            date = "2026-10-21T19:00Z",
                            name = "Bayern vs Arsenal",
                            season = EspnSeasonDto(year = 2026, slug = "league-phase"),
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "ucl_new_league_phase",
                                    date = "2026-10-21T19:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "3", homeAway = "home", score = "1", team = EspnTeamDto(id = "3", name = "Bayern")),
                                        EspnCompetitorDto(id = "2", homeAway = "away", score = "0", team = EspnTeamDto(id = "2", name = "Arsenal"))
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repository = MatchRepositoryImpl(mixedApi, Dispatchers.Unconfined)
        val matches = repository.getMatches(League.CHAMPIONS_LEAGUE, forceRefresh = true).getOrThrow()

        // El partido de mayo de la temporada 2025 debe ser descartado, conservando solo el de octubre de 2026
        assertEquals(1, matches.size)
        assertEquals("ucl_new_league_phase", matches.first().id)
    }

    @Test
    fun getMatches_friendlies_usesRollingCalendarClustersWithoutQueryDatesYear() = runBlocking {
        val today = LocalDate.now(ZoneId.systemDefault())
        val pastDate = today.minusDays(30)
        val futureDate = today.plusDays(5)
        val pastIso = pastDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val futureIso = futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val pastBasic = pastDate.format(DateTimeFormatter.BASIC_ISO_DATE)
        val futureBasic = futureDate.format(DateTimeFormatter.BASIC_ISO_DATE)

        var initialQueryDates: String? = "NOT_CALLED"
        var initialQueryLimit: Int? = -1
        val queriedDates = mutableListOf<String?>()

        val friendliesApi = object : EspnApiService {
            override suspend fun getScoreboard(
                league: String,
                dates: String?,
                limit: Int?,
                cacheControl: String?
            ): EspnScoreboardResponse {
                queriedDates.add(dates)
                if (dates == null) {
                    initialQueryDates = dates
                    initialQueryLimit = limit
                    return EspnScoreboardResponse(
                        leagues = listOf(
                            com.ixeken.ulama.data.espn.model.EspnLeagueCalendarDto(
                                calendar = listOf(
                                    kotlinx.serialization.json.JsonPrimitive("${pastIso}T00:00Z"),
                                    kotlinx.serialization.json.JsonPrimitive("${futureIso}T00:00Z")
                                )
                            )
                        ),
                        events = emptyList()
                    )
                }

                val matchId = "friendly_$dates"
                val isPastBatch = dates == pastBasic
                return EspnScoreboardResponse(
                    events = listOf(
                        EspnEventDto(
                            id = matchId,
                            date = if (isPastBatch) "${pastIso}T20:00Z" else "${futureIso}T20:00Z",
                            name = if (isPastBatch) "Mexico vs USA" else "Brazil vs Argentina",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = matchId,
                                    date = if (isPastBatch) "${pastIso}T20:00Z" else "${futureIso}T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "1", homeAway = "home", score = "2", team = EspnTeamDto(id = "1", name = "Team A")),
                                        EspnCompetitorDto(id = "2", homeAway = "away", score = "1", team = EspnTeamDto(id = "2", name = "Team B"))
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repository = MatchRepositoryImpl(friendliesApi, Dispatchers.Unconfined)
        val matches = repository.getMatches(League.FRIENDLIES, forceRefresh = true).getOrThrow()

        // Verifica que la consulta inicial a FRIENDLIES no use ano fijo ni limite de torneo
        assertEquals(null, initialQueryDates)
        assertEquals(null, initialQueryLimit)

        // Verifica que se capturen partidos pasados y futuros a traves del calendario
        assertEquals(2, matches.size)
        assertTrue(matches.any { it.id == "friendly_$pastBasic" && it.dateUtc.contains(pastIso) })
        assertTrue(matches.any { it.id == "friendly_$futureBasic" && it.dateUtc.contains(futureIso) })
    }

    @Test
    fun getMatchDetail_mapsDefensiveStatsAndMetadataCorrectly() = runBlocking {
        val summaryApi = object : com.ixeken.ulama.data.espn.EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                boxscore = com.ixeken.ulama.data.espn.model.EspnBoxscoreDto(
                    teams = listOf(
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(
                            team = EspnTeamDto(id = "1", name = "Team A"),
                            statistics = listOf(
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "effectiveTackles", displayValue = "15"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "interceptions", displayValue = "8"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "totalClearance", displayValue = "22"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "blockedShots", displayValue = "4"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "saves", displayValue = "3")
                            )
                        ),
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(
                            team = EspnTeamDto(id = "2", name = "Team B"),
                            statistics = listOf(
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "effectiveTackles", displayValue = "10"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "interceptions", displayValue = "12"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "totalClearance", displayValue = "18"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "blockedShots", displayValue = "6"),
                                com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "saves", displayValue = "5")
                            )
                        )
                    )
                ),
                broadcasts = listOf(
                    com.ixeken.ulama.data.espn.model.EspnBroadcastDto(
                        media = com.ixeken.ulama.data.espn.model.EspnBroadcastMediaDto(shortName = "ViX", name = "ViX Premium")
                    ),
                    com.ixeken.ulama.data.espn.model.EspnBroadcastDto(
                        media = com.ixeken.ulama.data.espn.model.EspnBroadcastMediaDto(shortName = "TUDN", name = "TUDN")
                    )
                ),
                gameInfo = com.ixeken.ulama.data.espn.model.EspnGameInfoDto(
                    attendance = 45200,
                    officials = listOf(
                        com.ixeken.ulama.data.espn.model.EspnOfficialDto(displayName = "Cesar Ramos")
                    ),
                    weather = com.ixeken.ulama.data.espn.model.EspnWeatherDto(displayValue = "Despejado", temperature = 24)
                )
            )
        }

        val repository = MatchRepositoryImpl(summaryApi, Dispatchers.Unconfined)
        val detail = repository.getMatchDetail(League.LIGA_MX, "test_match", forceRefresh = true).getOrThrow()

        assertEquals(listOf("ViX", "TUDN"), detail.broadcasts)
        assertEquals(45200, detail.attendance)
        assertEquals("Cesar Ramos", detail.referee)
        assertEquals("Despejado \u00B7 24\u00B0", detail.weather)

        assertEquals(5, detail.stats.size)
        assertEquals("15", detail.stats[0].homeDisplayValue)
        assertEquals("10", detail.stats[0].awayDisplayValue)
        assertEquals(com.ixeken.ulama.R.string.stat_effective_tackles, detail.stats[0].labelRes)

        assertEquals("8", detail.stats[1].homeDisplayValue)
        assertEquals("12", detail.stats[1].awayDisplayValue)
        assertEquals(com.ixeken.ulama.R.string.stat_interceptions, detail.stats[1].labelRes)

        assertEquals("22", detail.stats[2].homeDisplayValue)
        assertEquals("18", detail.stats[2].awayDisplayValue)
        assertEquals(com.ixeken.ulama.R.string.stat_clearances, detail.stats[2].labelRes)

        assertEquals("4", detail.stats[3].homeDisplayValue)
        assertEquals("6", detail.stats[3].awayDisplayValue)
        assertEquals(com.ixeken.ulama.R.string.stat_blocked_shots, detail.stats[3].labelRes)

        assertEquals("3", detail.stats[4].homeDisplayValue)
        assertEquals("5", detail.stats[4].awayDisplayValue)
        assertEquals(com.ixeken.ulama.R.string.stat_saves, detail.stats[4].labelRes)
    }

    @Test
    fun getMatchDetail_mapsRecentFormAndHeadToHeadCorrectly() = runBlocking {
        val summaryApi = object : com.ixeken.ulama.data.espn.EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                boxscore = com.ixeken.ulama.data.espn.model.EspnBoxscoreDto(
                    teams = listOf(
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(
                            team = EspnTeamDto(id = "10", name = "Club America")
                        ),
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(
                            team = EspnTeamDto(id = "20", name = "Chivas")
                        )
                    )
                ),
                lastFiveGames = listOf(
                    com.ixeken.ulama.data.espn.model.EspnLastFiveGamesDto(
                        team = EspnTeamDto(id = "10", name = "Club America"),
                        events = listOf(
                            com.ixeken.ulama.data.espn.model.EspnFormEventDto(gameResult = "W", score = "2-1"),
                            com.ixeken.ulama.data.espn.model.EspnFormEventDto(gameResult = "D", score = "1-1"),
                            com.ixeken.ulama.data.espn.model.EspnFormEventDto(gameResult = "L", score = "0-1")
                        )
                    ),
                    com.ixeken.ulama.data.espn.model.EspnLastFiveGamesDto(
                        team = EspnTeamDto(id = "20", name = "Chivas"),
                        events = listOf(
                            com.ixeken.ulama.data.espn.model.EspnFormEventDto(gameResult = "L", score = "0-2"),
                            com.ixeken.ulama.data.espn.model.EspnFormEventDto(gameResult = "W", score = "3-0")
                        )
                    )
                ),
                seasonseries = listOf(
                    com.ixeken.ulama.data.espn.model.EspnSeasonSeriesDto(
                        summary = "America leads series 2-1-0",
                        events = listOf(
                            com.ixeken.ulama.data.espn.model.EspnSeasonSeriesEventDto(
                                id = "ev1",
                                date = "2026-03-01T20:00:00Z",
                                competitors = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnSeasonSeriesCompetitorDto(
                                        score = "2",
                                        winner = true,
                                        team = EspnTeamDto(id = "10", name = "Club America")
                                    ),
                                    com.ixeken.ulama.data.espn.model.EspnSeasonSeriesCompetitorDto(
                                        score = "1",
                                        winner = false,
                                        team = EspnTeamDto(id = "20", name = "Chivas")
                                    )
                                )
                            ),
                            com.ixeken.ulama.data.espn.model.EspnSeasonSeriesEventDto(
                                id = "ev2",
                                date = "2025-10-15T20:00:00Z",
                                competitors = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnSeasonSeriesCompetitorDto(
                                        score = "0",
                                        winner = false,
                                        team = EspnTeamDto(id = "10", name = "Club America")
                                    ),
                                    com.ixeken.ulama.data.espn.model.EspnSeasonSeriesCompetitorDto(
                                        score = "0",
                                        winner = false,
                                        team = EspnTeamDto(id = "20", name = "Chivas")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        val repository = MatchRepositoryImpl(summaryApi, Dispatchers.Unconfined)
        val detail = repository.getMatchDetail(League.LIGA_MX, "test_fase2", forceRefresh = true).getOrThrow()

        // Verifica racha reciente
        assertEquals(3, detail.homeRecentForm.size)
        assertEquals(com.ixeken.ulama.core.model.FormResult.WIN, detail.homeRecentForm[0].result)
        assertEquals(com.ixeken.ulama.core.model.FormResult.DRAW, detail.homeRecentForm[1].result)
        assertEquals(com.ixeken.ulama.core.model.FormResult.LOSS, detail.homeRecentForm[2].result)

        assertEquals(2, detail.awayRecentForm.size)
        assertEquals(com.ixeken.ulama.core.model.FormResult.LOSS, detail.awayRecentForm[0].result)
        assertEquals(com.ixeken.ulama.core.model.FormResult.WIN, detail.awayRecentForm[1].result)

        // Verifica historial cara a cara (H2H)
        val h2h = detail.headToHead
        org.junit.Assert.assertNotNull(h2h)
        assertEquals(1, h2h?.homeWins)
        assertEquals(1, h2h?.draws)
        assertEquals(0, h2h?.awayWins)
        assertEquals(2, h2h?.matches?.size)
        assertEquals("2", h2h?.matches?.first()?.homeScore)
        assertEquals("1", h2h?.matches?.first()?.awayScore)
    }

    @Test
    fun espnMatchSummaryResponse_deserializesSeasonseriesArrayCorrectly() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val rawJson = """
            {
                "seasonseries": [
                    {
                        "type": "head-to-head",
                        "title": "Club America vs. Chivas",
                        "summary": "America leads series 2-1-0",
                        "completed": false,
                        "events": [
                            {
                                "id": "123",
                                "date": "2026-03-01T20:00:00Z",
                                "competitors": [
                                    { "homeAway": "home", "winner": true, "score": "2", "team": { "id": "10", "displayName": "America" } },
                                    { "homeAway": "away", "winner": false, "score": "1", "team": { "id": "20", "displayName": "Chivas" } }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse>(rawJson)
        assertEquals(1, parsed.seasonseries.size)
        assertEquals("America leads series 2-1-0", parsed.seasonseries[0].summary)
        assertEquals(1, parsed.seasonseries[0].events.size)
    }

    @Test
    fun getMatchDetail_mapsLeadersCorrectly() = runBlocking {
        val summaryApi = object : com.ixeken.ulama.data.espn.EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                header = com.ixeken.ulama.data.espn.model.EspnSummaryHeaderDto(
                    competitions = listOf(
                        com.ixeken.ulama.data.espn.model.EspnSummaryCompetitionDto(
                            status = EspnStatusDto(
                                type = com.ixeken.ulama.data.espn.model.EspnStatusTypeDto(
                                    state = "post",
                                    completed = true
                                )
                            )
                        )
                    )
                ),
                boxscore = com.ixeken.ulama.data.espn.model.EspnBoxscoreDto(
                    teams = listOf(
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(team = EspnTeamDto(id = "10", displayName = "Arsenal")),
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(team = EspnTeamDto(id = "20", displayName = "Chelsea"))
                    )
                ),
                leaders = listOf(
                    com.ixeken.ulama.data.espn.model.EspnTeamLeadersDto(
                        team = EspnTeamDto(id = "10", displayName = "Arsenal"),
                        leaders = listOf(
                            com.ixeken.ulama.data.espn.model.EspnLeaderCategoryDto(
                                name = "totalShots",
                                displayName = "Total Shots",
                                leaders = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnLeaderEntryDto(
                                        displayValue = "4",
                                        athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(
                                            id = "1",
                                            displayName = "Bukayo Saka",
                                            shortName = "B. Saka",
                                            jersey = "7"
                                        ),
                                        summary = "1 SOG, 1 G"
                                    )
                                )
                            ),
                            com.ixeken.ulama.data.espn.model.EspnLeaderCategoryDto(
                                name = "saves",
                                displayName = "Saves",
                                leaders = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnLeaderEntryDto(
                                        displayValue = "3",
                                        athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(
                                            id = "2",
                                            displayName = "David Raya",
                                            shortName = "D. Raya",
                                            jersey = "22"
                                        )
                                    )
                                )
                            ),
                            com.ixeken.ulama.data.espn.model.EspnLeaderCategoryDto(
                                name = "goals",
                                displayName = "Goals",
                                leaders = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnLeaderEntryDto(
                                        displayValue = "Matches: 5, Goals: 3",
                                        athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(
                                            id = "1",
                                            displayName = "Bukayo Saka",
                                            shortName = "B. Saka"
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    com.ixeken.ulama.data.espn.model.EspnTeamLeadersDto(
                        team = EspnTeamDto(id = "20", displayName = "Chelsea"),
                        leaders = listOf(
                            com.ixeken.ulama.data.espn.model.EspnLeaderCategoryDto(
                                name = "totalShots",
                                displayName = "Total Shots",
                                leaders = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnLeaderEntryDto(
                                        displayValue = "5",
                                        athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(
                                            id = "3",
                                            displayName = "Cole Palmer",
                                            shortName = "C. Palmer",
                                            jersey = "20"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        val repository = MatchRepositoryImpl(summaryApi, Dispatchers.Unconfined)
        val detail = repository.getMatchDetail(League.PREMIER_LEAGUE, "test_leaders", forceRefresh = true).getOrThrow()

        assertEquals(2, detail.leaders.size)

        // totalShots
        val shotsCategory = detail.leaders.first { it.categoryKey == "totalShots" }
        assertEquals("B. Saka", shotsCategory.homeLeader?.athleteName)
        assertEquals("7", shotsCategory.homeLeader?.jersey)
        assertEquals("4", shotsCategory.homeLeader?.valueDisplay)
        assertEquals("C. Palmer", shotsCategory.awayLeader?.athleteName)
        assertEquals("20", shotsCategory.awayLeader?.jersey)
        assertEquals("5", shotsCategory.awayLeader?.valueDisplay)

        // saves
        val savesCategory = detail.leaders.first { it.categoryKey == "saves" }
        assertEquals("D. Raya", savesCategory.homeLeader?.athleteName)
        assertEquals("3", savesCategory.homeLeader?.valueDisplay)
        org.junit.Assert.assertNull(savesCategory.awayLeader)
    }

    @Test
    fun getMatchDetail_mapsIsCompletedCorrectly() = runBlocking {
        val summaryApi = object : com.ixeken.ulama.data.espn.EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                header = com.ixeken.ulama.data.espn.model.EspnSummaryHeaderDto(
                    competitions = listOf(
                        com.ixeken.ulama.data.espn.model.EspnSummaryCompetitionDto(
                            status = EspnStatusDto(
                                type = com.ixeken.ulama.data.espn.model.EspnStatusTypeDto(
                                    state = "post",
                                    completed = true
                                )
                            )
                        )
                    )
                )
            )
        }

        val repo = MatchRepositoryImpl(summaryApi, Dispatchers.Unconfined)
        val detail = repo.getMatchDetail(League.LIGA_MX, "test_completed", forceRefresh = true).getOrThrow()

        assertTrue(detail.isCompleted)
    }

    @Test
    fun getCachedMatchDetail_returnsStoredDetailWithinTTL() = runBlocking {
        var networkCalls = 0
        val summaryApi = object : com.ixeken.ulama.data.espn.EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?): com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse {
                networkCalls++
                return com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                    header = com.ixeken.ulama.data.espn.model.EspnSummaryHeaderDto(
                        competitions = listOf(
                            com.ixeken.ulama.data.espn.model.EspnSummaryCompetitionDto(
                                status = EspnStatusDto(
                                    type = com.ixeken.ulama.data.espn.model.EspnStatusTypeDto(
                                        state = "post",
                                        completed = true
                                    )
                                )
                            )
                        )
                    )
                )
            }
        }

        val repo = MatchRepositoryImpl(summaryApi, Dispatchers.Unconfined)

        // Antes de consultar, la cache en memoria esta vacia
        org.junit.Assert.assertNull(repo.getCachedMatchDetail("match_cached_test"))

        // Primera consulta: va a red
        val detail = repo.getMatchDetail(League.LIGA_MX, "match_cached_test", forceRefresh = false).getOrThrow()
        assertEquals(1, networkCalls)
        assertTrue(detail.isCompleted)

        // getCachedMatchDetail retorna inmediatamente el objeto en memoria
        val cached = repo.getCachedMatchDetail("match_cached_test")
        org.junit.Assert.assertNotNull(cached)
        assertEquals(detail.matchId, cached?.matchId)
        assertTrue(cached?.isCompleted == true)

        // Segunda consulta getMatchDetail sin forceRefresh: no incrementa llamadas de red
        val secondCallResult = repo.getMatchDetail(League.LIGA_MX, "match_cached_test", forceRefresh = false).getOrThrow()
        assertEquals(1, networkCalls)
        assertEquals(detail.matchId, secondCallResult.matchId)
    }

    @Test
    fun getStandings_and_getGroups_concurrentForceRefresh_deduplicatesNetworkCall() = runBlocking {
        var standingsCalls = 0
        val api = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse {
                standingsCalls++
                return fakeApiService.getStandings(league)
            }
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(api, Dispatchers.Unconfined)
        val standings = repo.getStandings(League.WORLD_CUP, forceRefresh = true).getOrThrow()
        val groups = repo.getGroups(League.WORLD_CUP, forceRefresh = true).getOrThrow()

        assertEquals(1, standingsCalls)
        assertEquals(4, standings.size)
        assertEquals(2, groups.size)
    }

    @Test
    fun getMatches_cachesConcludedPastDatesAcrossForceRefresh() = runBlocking {
        val queriedDates = mutableListOf<String?>()
        val pastCalendarApi = object : EspnApiService {
            override suspend fun getScoreboard(
                league: String,
                dates: String?,
                limit: Int?,
                cacheControl: String?
            ): EspnScoreboardResponse {
                queriedDates.add(dates)
                if (dates == null) {
                    return EspnScoreboardResponse(
                        leagues = listOf(
                            EspnLeagueCalendarDto(
                                calendar = listOf(
                                    kotlinx.serialization.json.JsonPrimitive("2026-01-10T00:00Z")
                                )
                            )
                        )
                    )
                }
                return EspnScoreboardResponse(
                    events = listOf(
                        EspnEventDto(
                            id = "past_finished_1",
                            date = "2026-01-10T20:00Z",
                            name = "Team A vs Team B",
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "past_finished_1",
                                    date = "2026-01-10T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "1", homeAway = "home", score = "1", winner = true, team = EspnTeamDto(id = "1", name = "Team A")),
                                        EspnCompetitorDto(id = "2", homeAway = "away", score = "0", winner = false, team = EspnTeamDto(id = "2", name = "Team B"))
                                    ),
                                    status = EspnStatusDto(
                                        type = EspnStatusTypeDto(state = "post", completed = true, detail = "FT")
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(pastCalendarApi, Dispatchers.Unconfined)
        val firstLoad = repo.getMatches(League.LIGA_MX, forceRefresh = true).getOrThrow()
        assertEquals(1, firstLoad.size)
        assertEquals(1, queriedDates.count { it == "20260110" })

        // Segunda llamada con forceRefresh = true no vuelve a pedir la fecha pasada ya concluida
        val secondLoad = repo.getMatches(League.LIGA_MX, forceRefresh = true).getOrThrow()
        assertEquals(1, secondLoad.size)
        assertEquals(1, queriedDates.count { it == "20260110" })
    }

    @Test
    fun extractDateStrings_parsesNestedEntriesAndResolveMatchdayDates_includesInternationalWindow() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val rawJson = """
            {
                "leagues": [
                    {
                        "calendar": [
                            {
                                "label": "Window",
                                "entries": [
                                    { "value": "2026-10-08T07:00Z" },
                                    { "startDate": "2026-10-13T07:00Z", "endDate": "2026-10-13T23:59Z" }
                                ]
                            }
                        ]
                    }
                ],
                "events": []
            }
        """.trimIndent()

        val parsed = json.decodeFromString<EspnScoreboardResponse>(rawJson)
        val extracted = parsed.leagues.first().extractDateStrings()
        assertEquals(listOf("2026-10-08T07:00Z", "2026-10-13T07:00Z"), extracted)

        val resolved = MatchRepositoryImpl.resolveMatchdayDates(
            calendarDates = extracted,
            today = java.time.LocalDate.of(2026, 10, 7)
        )
        assertEquals(listOf("20261008", "20261013"), resolved)
    }

    @Test
    fun getGroups_flattensTwoLevelNestedStandingsChildren() = runBlocking {
        val nestedApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse {
                return EspnStandingsResponse(
                    children = listOf(
                        EspnStandingsChildDto(
                            name = "League A",
                            children = listOf(
                                EspnStandingsChildDto(
                                    name = "Group 1",
                                    standings = EspnStandingsEntriesDto(
                                        entries = listOf(
                                            EspnStandingsEntryDto(
                                                team = EspnStandingsTeamDto(id = "10", name = "Mexico", displayName = "Mexico"),
                                                stats = listOf(EspnStatDto(name = "rank", value = 1.0), EspnStatDto(name = "points", value = 6.0))
                                            )
                                        )
                                    )
                                ),
                                EspnStandingsChildDto(
                                    name = "Group 2",
                                    standings = EspnStandingsEntriesDto(
                                        entries = listOf(
                                            EspnStandingsEntryDto(
                                                team = EspnStandingsTeamDto(id = "20", name = "USA", displayName = "USA"),
                                                stats = listOf(EspnStatDto(name = "rank", value = 1.0), EspnStatDto(name = "points", value = 4.0))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(nestedApi, Dispatchers.Unconfined)
        val groups = repo.getGroups(League.CONCACAF_NATIONS_LEAGUE, forceRefresh = true).getOrThrow()
        val standings = repo.getStandings(League.CONCACAF_NATIONS_LEAGUE, forceRefresh = false).getOrThrow()

        assertEquals(2, groups.size)
        assertEquals("Liga A - Grupo 1", groups[0].name)
        assertEquals("Liga A - Grupo 2", groups[1].name)
        assertEquals(2, standings.size)
    }

    @Test
    fun getMatchDetail_handlesInvertedBoxscoreTeamsKeyEventsScorersAndStarterFalseLineups() = runBlocking {
        val internationalApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                header = com.ixeken.ulama.data.espn.model.EspnSummaryHeaderDto(
                    competitions = listOf(
                        com.ixeken.ulama.data.espn.model.EspnSummaryCompetitionDto(
                            competitors = listOf(
                                EspnCompetitorDto(id = "100", homeAway = "home", team = EspnTeamDto(id = "100", name = "Mexico", displayName = "Mexico")),
                                EspnCompetitorDto(id = "200", homeAway = "away", team = EspnTeamDto(id = "200", name = "Brazil", displayName = "Brazil"))
                            ),
                            status = EspnStatusDto(type = EspnStatusTypeDto(state = "in", completed = false))
                        )
                    )
                ),
                // Boxscore con el equipo visitante en el indice 0 y el local en el indice 1
                boxscore = com.ixeken.ulama.data.espn.model.EspnBoxscoreDto(
                    teams = listOf(
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(
                            team = EspnTeamDto(id = "200", name = "Brazil"),
                            homeAway = "away",
                            statistics = listOf(com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "totalShots", displayValue = "9"))
                        ),
                        com.ixeken.ulama.data.espn.model.EspnBoxscoreTeamDto(
                            team = EspnTeamDto(id = "100", name = "Mexico"),
                            homeAway = "home",
                            statistics = listOf(com.ixeken.ulama.data.espn.model.EspnStatisticItemDto(name = "totalShots", displayValue = "11"))
                        )
                    )
                ),
                keyEvents = listOf(
                    com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto(
                        scoringPlay = false,
                        type = com.ixeken.ulama.data.espn.model.EspnPlayTypeDto(text = "Goal - Header"),
                        clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(displayValue = "52'"),
                        team = com.ixeken.ulama.data.espn.model.EspnSummaryTeamRefDto(id = "100"),
                        athletesInvolved = listOf(
                            com.ixeken.ulama.data.espn.model.EspnAthleteDto(displayName = "S. Giménez")
                        )
                    )
                ),
                rosters = listOf(
                    com.ixeken.ulama.data.espn.model.EspnRosterTeamDto(
                        team = EspnTeamDto(id = "100", name = "Mexico"),
                        homeAway = "home",
                        roster = (1..12).map { idx ->
                            com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                starter = false,
                                formationPlace = 0,
                                athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(
                                    id = "p_$idx",
                                    displayName = "Player $idx",
                                    jersey = "$idx",
                                    position = kotlinx.serialization.json.JsonPrimitive(
                                        if (idx == 1) "GK" else if (idx in 2..5) "DF" else "MF"
                                    )
                                )
                            )
                        }
                    )
                ),
                leaders = listOf(
                    com.ixeken.ulama.data.espn.model.EspnTeamLeadersDto(
                        team = EspnTeamDto(id = "100", name = "Mexico"),
                        leaders = listOf(
                            com.ixeken.ulama.data.espn.model.EspnLeaderCategoryDto(
                                name = "totalShots",
                                leaders = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnLeaderEntryDto(
                                        displayValue = "3",
                                        athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(displayName = "S. Giménez")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }

        val repo = MatchRepositoryImpl(internationalApi, Dispatchers.Unconfined)
        val detail = repo.getMatchDetail(League.FRIENDLIES, "intl_1", forceRefresh = true).getOrThrow()

        // Goleador extraido desde keyEvents + athletesInvolved y asignado correctamente al local (id 100)
        assertEquals(1, detail.homeScorers.size)
        assertEquals("S. Giménez", detail.homeScorers.first().playerName)
        assertEquals("52'", detail.homeScorers.first().minutesDisplay)
        assertTrue(detail.awayScorers.isEmpty())

        // Estadisticas alineadas con home (11) y away (9) pese al orden invertido en boxscore.teams
        assertEquals("11", detail.stats.first().homeDisplayValue)
        assertEquals("9", detail.stats.first().awayDisplayValue)

        // Alineacion reconstruida aunque starter = false y formationPlace = 0
        assertEquals(11, detail.homeLineup?.starters?.size)
        assertEquals(1, detail.homeLineup?.bench?.size)
        assertEquals(com.ixeken.ulama.core.model.TacticalLine.GOALKEEPER, detail.homeLineup?.starters?.first()?.line)

        // Como el partido sigue en vivo (completed = false), no debe exponer Match Leaders
        assertTrue(detail.leaders.isEmpty())
    }

    @Test
    fun scoreboardDetails_withStringAthletePosition_deserializesAndMapsCleanly() = runBlocking {
        val rawJson = """
            {
              "leagues": [
                {
                  "calendar": ["2026-04-12T07:00Z"]
                }
              ],
              "events": [
                {
                  "id": "401800999",
                  "date": "2026-04-12T20:00Z",
                  "status": {
                    "displayClock": "90'",
                    "period": 2,
                    "type": {
                      "name": "STATUS_FULL_TIME",
                      "state": "post",
                      "completed": true,
                      "detail": "FT",
                      "shortDetail": "FT"
                    }
                  },
                  "competitions": [
                    {
                      "id": "401800999",
                      "competitors": [
                        {
                          "id": "10",
                          "homeAway": "home",
                          "score": "1",
                          "team": { "id": "10", "name": "America", "abbreviation": "AME" }
                        },
                        {
                          "id": "20",
                          "homeAway": "away",
                          "score": "0",
                          "team": { "id": "20", "name": "Cruz Azul", "abbreviation": "CAZ" }
                        }
                      ],
                      "details": [
                        {
                          "type": { "id": "98", "text": "Goal - Header" },
                          "clock": { "value": 4200.0, "displayValue": "70'" },
                          "team": { "id": "10" },
                          "scoreValue": 1,
                          "scoringPlay": true,
                          "redCard": false,
                          "yellowCard": false,
                          "penaltyKick": false,
                          "ownGoal": false,
                          "shootout": false,
                          "athletesInvolved": [
                            {
                              "id": "285936",
                              "displayName": "Henry Martin",
                              "shortName": "H. Martin",
                              "fullName": "Henry Martin",
                              "jersey": "21",
                              "position": "CD-R"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val jsonParser = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val dto = jsonParser.decodeFromString(EspnScoreboardResponse.serializer(), rawJson)
        val detailAthletes = dto.events.first().competitions.first().details.first().athletesInvolved
        assertEquals("CD", detailAthletes.first().extractPosition()?.abbreviation)
        assertEquals("CD-R", detailAthletes.first().extractPosition()?.name)
    }

    @Test
    fun getMatches_futurePostponedAndPlaceholderMatches_mappedToScheduledAndFilteredCleanly() = runBlocking {
        val afcApi = object : EspnApiService {
            override suspend fun getScoreboard(
                league: String,
                dates: String?,
                limit: Int?,
                cacheControl: String?
            ): EspnScoreboardResponse {
                return EspnScoreboardResponse(
                    events = listOf(
                        // Partido de Fase de Grupos en Enero 2027 con STATUS_POSTPONED (state = post, completed = false)
                        EspnEventDto(
                            id = "afc_group_2027",
                            date = "2027-01-20T10:30Z",
                            name = "Japan vs Thailand",
                            season = EspnSeasonDto(year = 2027, slug = "group-stage"),
                            status = EspnStatusDto(
                                type = EspnStatusTypeDto(
                                    name = "STATUS_POSTPONED",
                                    state = "post",
                                    completed = false,
                                    detail = "Postponed",
                                    shortDetail = "Postponed"
                                )
                            ),
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "afc_group_2027",
                                    date = "2027-01-20T10:30Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "627", homeAway = "home", team = EspnTeamDto(id = "627", name = "Japan", displayName = "Japan", logo = "https://logo/jpn.png")),
                                        EspnCompetitorDto(id = "4396", homeAway = "away", team = EspnTeamDto(id = "4396", name = "Thailand", displayName = "Thailand", logo = "https://logo/tha.png"))
                                    )
                                )
                            )
                        ),
                        // Partido plantilla de Octavos con dos placeholders sin seleccion real
                        EspnEventDto(
                            id = "afc_placeholder_r16",
                            date = "2027-01-25T20:00Z",
                            name = "Group C Winner vs 3rd Place Group A",
                            season = EspnSeasonDto(year = 2027, slug = "round-of-16"),
                            status = EspnStatusDto(
                                type = EspnStatusTypeDto(
                                    name = "STATUS_SCHEDULED",
                                    state = "pre",
                                    completed = false,
                                    detail = "TBD",
                                    shortDetail = "TBD"
                                )
                            ),
                            competitions = listOf(
                                EspnCompetitionDto(
                                    id = "afc_placeholder_r16",
                                    date = "2027-01-25T20:00Z",
                                    competitors = listOf(
                                        EspnCompetitorDto(id = "5927", homeAway = "home", team = EspnTeamDto(id = "5927", name = "Group C Winner", displayName = "Group C Winner", shortDisplayName = "1C", abbreviation = "1C", logo = "")),
                                        EspnCompetitorDto(id = "16304", homeAway = "away", team = EspnTeamDto(id = "16304", name = "3rd Place Group A", displayName = "3rd Place Group A", shortDisplayName = "3 A", abbreviation = "3ABF", logo = ""))
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(afcApi, Dispatchers.Unconfined)
        val matches = repo.getMatches(League.ASIAN_CUP, forceRefresh = true).getOrThrow()

        // El cruce placeholder (1C vs 3 A) se descarta y el partido de grupo en 2027 con STATUS_POSTPONED queda como SCHEDULED
        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals("afc_group_2027", match.id)
        assertEquals(MatchState.SCHEDULED, match.status.state)
        org.junit.Assert.assertFalse(match.isFinished)
    }

    @Test
    fun getGroups_futureTournamentWithPublishedDraw_returns2027GroupsAndRejects2025Groups() = runBlocking {
        val standingsApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?) = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse {
                val seasonYear = if (league == League.ASIAN_CUP.code) 2027 else 2025
                return EspnStandingsResponse(
                    children = listOf(
                        EspnStandingsChildDto(
                            name = "Group A",
                            standings = EspnStandingsEntriesDto(
                                season = seasonYear,
                                entries = listOf(
                                    EspnStandingsEntryDto(
                                        team = EspnStandingsTeamDto(id = "1", name = "Saudi Arabia", displayName = "Saudi Arabia"),
                                        stats = listOf(EspnStatDto(name = "rank", value = 1.0), EspnStatDto(name = "points", value = 0.0))
                                    ),
                                    EspnStandingsEntryDto(
                                        team = EspnStandingsTeamDto(id = "999", name = "TBD", displayName = "TBD"),
                                        stats = listOf(EspnStatDto(name = "rank", value = 2.0), EspnStatDto(name = "points", value = 0.0))
                                    )
                                )
                            )
                        ),
                        EspnStandingsChildDto(
                            name = "Group B",
                            standings = EspnStandingsEntriesDto(
                                season = seasonYear,
                                entries = listOf(
                                    EspnStandingsEntryDto(
                                        team = EspnStandingsTeamDto(id = "2", name = "Japan", displayName = "Japan"),
                                        stats = listOf(EspnStatDto(name = "rank", value = 1.0), EspnStatDto(name = "points", value = 0.0))
                                    )
                                )
                            )
                        )
                    )
                )
            }
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) = com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(standingsApi, Dispatchers.Unconfined)
        val afcGroups = repo.getGroups(League.ASIAN_CUP, forceRefresh = true).getOrThrow()
        assertEquals(2, afcGroups.size)
        // Verifica que la entrada "TBD" haya sido descartada de Group A
        assertEquals(1, afcGroups.first().standings.size)
        assertEquals("Saudi Arabia", afcGroups.first().standings.first().teamName)

        // Gold Cup reporta season = 2025 (< tournamentYear 2027), por lo que debe rechazarse
        val goldCupGroups = repo.getGroups(League.GOLD_CUP, forceRefresh = true).getOrThrow()
        assertTrue(goldCupGroups.isEmpty())
    }

    @Test
    fun getMatchDetail_mapsYellowDoubleYellowAndRedCardsInLineups() = runBlocking {
        val cardsApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?) = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?) = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                    rosters = listOf(
                        com.ixeken.ulama.data.espn.model.EspnRosterTeamDto(
                            homeAway = "home",
                            team = EspnTeamDto(id = "10", name = "Local", displayName = "Local"),
                            formation = "4-4-2",
                            roster = listOf(
                                com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                    starter = true,
                                    jersey = "4",
                                    formationPlace = 4,
                                    plays = listOf(com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(yellowCard = true)),
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p1", displayName = "Amarillo")
                                ),
                                com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                    starter = true,
                                    jersey = "6",
                                    formationPlace = 6,
                                    plays = listOf(
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(yellowCard = true),
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(yellowCard = true)
                                    ),
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p2", displayName = "DobleAmarilla")
                                ),
                                com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                    starter = false,
                                    jersey = "14",
                                    subbedIn = true,
                                    plays = listOf(
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(substitution = true),
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(yellowCard = true),
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(redCard = true)
                                    ),
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p3", displayName = "SuplenteExpulsado")
                                ),
                                com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                    starter = false,
                                    jersey = "22",
                                    plays = listOf(com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(redCard = true)),
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p4", displayName = "RojaDirecta")
                                )
                            )
                        )
                    )
                )
        }

        val repo = MatchRepositoryImpl(cardsApi, Dispatchers.Unconfined)
        val detail = repo.getMatchDetail(League.LIGA_MX, "cards_match", forceRefresh = true).getOrThrow()
        val starters = detail.homeLineup?.starters.orEmpty()
        val bench = detail.homeLineup?.bench.orEmpty()

        assertEquals(1, starters[0].yellowCards)
        org.junit.Assert.assertFalse(starters[0].hasRedCard)
        org.junit.Assert.assertFalse(starters[0].isSentOff)

        assertEquals(2, starters[1].yellowCards)
        assertTrue(starters[1].isSentOff)

        assertEquals(1, bench[0].yellowCards)
        assertTrue(bench[0].hasRedCard)
        assertTrue(bench[0].isSentOff)

        assertEquals(0, bench[1].yellowCards)
        assertTrue(bench[1].hasRedCard)
        assertTrue(bench[1].isSentOff)
    }

    @Test
    fun getMatchDetail_mapsTimelineEventsFilteringDelaysAndPromotingSecondYellow() = runBlocking {
        val timelineApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?) = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?) = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                    header = com.ixeken.ulama.data.espn.model.EspnSummaryHeaderDto(
                        competitions = listOf(
                            com.ixeken.ulama.data.espn.model.EspnSummaryCompetitionDto(
                                competitors = listOf(
                                    EspnCompetitorDto(id = "219", homeAway = "home", team = EspnTeamDto(id = "219", name = "Guadalajara")),
                                    EspnCompetitorDto(id = "218", homeAway = "away", team = EspnTeamDto(id = "218", name = "Cruz Azul"))
                                )
                            )
                        )
                    ),
                    keyEvents = listOf(
                        com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto(
                            id = "delay_1",
                            type = com.ixeken.ulama.data.espn.model.EspnPlayTypeDto(text = "Start Delay", type = "start-delay"),
                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(value = 600.0, displayValue = "10'"),
                            team = com.ixeken.ulama.data.espn.model.EspnSummaryTeamRefDto(id = "219")
                        ),
                        com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto(
                            id = "goal_1",
                            scoringPlay = true,
                            type = com.ixeken.ulama.data.espn.model.EspnPlayTypeDto(text = "Goal - Volley", type = "goal---volley"),
                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(value = 1234.0, displayValue = "21'"),
                            team = com.ixeken.ulama.data.espn.model.EspnSummaryTeamRefDto(id = "219"),
                            participants = listOf(
                                com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "a1", displayName = "Efraín Álvarez")
                                ),
                                com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "a2", displayName = "Richard Ledezma")
                                )
                            )
                        ),
                        com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto(
                            id = "yc_1",
                            type = com.ixeken.ulama.data.espn.model.EspnPlayTypeDto(text = "Yellow Card", type = "yellow-card"),
                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(value = 2100.0, displayValue = "35'"),
                            team = com.ixeken.ulama.data.espn.model.EspnSummaryTeamRefDto(id = "218"),
                            participants = listOf(
                                com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "b1", displayName = "Carlos Rodríguez")
                                )
                            )
                        ),
                        com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto(
                            id = "sub_1",
                            type = com.ixeken.ulama.data.espn.model.EspnPlayTypeDto(text = "Substitution", type = "substitution"),
                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(value = 3491.0, displayValue = "59'"),
                            team = com.ixeken.ulama.data.espn.model.EspnSummaryTeamRefDto(id = "219"),
                            participants = listOf(
                                com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "a3", displayName = "Luis Gabriel Rey")
                                ),
                                com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "a4", displayName = "José Castillo")
                                )
                            )
                        ),
                        com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto(
                            id = "yc_2",
                            type = com.ixeken.ulama.data.espn.model.EspnPlayTypeDto(text = "Yellow Card", type = "yellow-card"),
                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(value = 4500.0, displayValue = "75'"),
                            team = com.ixeken.ulama.data.espn.model.EspnSummaryTeamRefDto(id = "218"),
                            participants = listOf(
                                com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "b1", displayName = "Carlos Rodríguez")
                                )
                            )
                        )
                    )
                )
        }

        val repo = MatchRepositoryImpl(timelineApi, Dispatchers.Unconfined)
        val detail = repo.getMatchDetail(League.LIGA_MX, "timeline_match", forceRefresh = true).getOrThrow()
        val events = detail.timelineEvents

        // Start Delay debe descartarse y quedar 4 eventos ordenados del mas reciente al primero
        assertEquals(4, events.size)
        assertEquals("75'", events[0].minute)
        assertEquals(com.ixeken.ulama.core.model.TimelineEventType.SECOND_YELLOW, events[0].type)
        assertEquals(false, events[0].isHomeTeam)

        assertEquals("59'", events[1].minute)
        assertEquals(com.ixeken.ulama.core.model.TimelineEventType.SUBSTITUTION, events[1].type)
        assertEquals("Luis Gabriel Rey", events[1].primaryPlayerName)
        assertEquals("José Castillo", events[1].secondaryPlayerName)

        assertEquals("35'", events[2].minute)
        assertEquals(com.ixeken.ulama.core.model.TimelineEventType.YELLOW_CARD, events[2].type)

        assertEquals("21'", events[3].minute)
        assertEquals(com.ixeken.ulama.core.model.TimelineEventType.GOAL, events[3].type)
        assertEquals("Efraín Álvarez", events[3].primaryPlayerName)
        assertEquals("Richard Ledezma", events[3].secondaryPlayerName)
        assertEquals(true, events[3].isHomeTeam)
    }

    @Test
    fun getMatchDetail_mapsGoalCountAndResolvesFinalLineupSubstitutions() = runBlocking {
        val lineupApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?) = EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?) = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse(
                    keyEvents = listOf(
                        com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto(
                            id = "g1",
                            scoringPlay = true,
                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(value = 1260.0, displayValue = "21'"),
                            team = com.ixeken.ulama.data.espn.model.EspnSummaryTeamRefDto(id = "10"),
                            participants = listOf(
                                com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p_star", displayName = "Efraín Álvarez")
                                ),
                                com.ixeken.ulama.data.espn.model.EspnScoringParticipantDto(
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p_assist", displayName = "Richard Ledezma")
                                )
                            )
                        )
                    ),
                    rosters = listOf(
                        com.ixeken.ulama.data.espn.model.EspnRosterTeamDto(
                            homeAway = "home",
                            team = EspnTeamDto(id = "10", name = "Local", displayName = "Local"),
                            formation = "4-2-3-1",
                            roster = listOf(
                                com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                    starter = true,
                                    jersey = "11",
                                    formationPlace = 11,
                                    position = com.ixeken.ulama.data.espn.model.EspnPlayerPositionDto(
                                        name = "Attacking Midfielder Left",
                                        abbreviation = "AM-L"
                                    ),
                                    subbedOut = true,
                                    subbedOutFor = com.ixeken.ulama.data.espn.model.EspnSubbedInForDto(
                                        jersey = "14",
                                        athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p_sub", displayName = "Ricardo Marín")
                                    ),
                                    plays = listOf(
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(
                                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(displayValue = "67'"),
                                            substitution = true
                                        )
                                    ),
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p_star", displayName = "Efraín Álvarez")
                                ),
                                com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                    starter = true,
                                    jersey = "7",
                                    formationPlace = 7,
                                    plays = listOf(
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(
                                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(displayValue = "21'"),
                                            scoringPlay = true,
                                            didAssist = true
                                        )
                                    ),
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p_assist", displayName = "Richard Ledezma")
                                ),
                                com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto(
                                    starter = false,
                                    jersey = "14",
                                    position = com.ixeken.ulama.data.espn.model.EspnPlayerPositionDto(
                                        name = "Substitute",
                                        abbreviation = "SUB"
                                    ),
                                    subbedIn = true,
                                    subbedInFor = com.ixeken.ulama.data.espn.model.EspnSubbedInForDto(
                                        jersey = "11",
                                        athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p_star", displayName = "Efraín Álvarez")
                                    ),
                                    plays = listOf(
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(
                                            clock = com.ixeken.ulama.data.espn.model.EspnClockDisplayDto(displayValue = "67'"),
                                            substitution = true
                                        ),
                                        com.ixeken.ulama.data.espn.model.EspnPlayerPlayDto(scoringPlay = true, didScore = true)
                                    ),
                                    athlete = com.ixeken.ulama.data.espn.model.EspnAthleteDto(id = "p_sub", displayName = "Ricardo Marín")
                                )
                            )
                        )
                    )
                )
        }

        val repo = MatchRepositoryImpl(lineupApi, Dispatchers.Unconfined)
        val detail = repo.getMatchDetail(League.LIGA_MX, "lineup_sub_match", forceRefresh = true).getOrThrow()
        val homeLineup = detail.homeLineup!!

        assertEquals(1, homeLineup.starters[0].goalsCount)
        assertEquals(0, homeLineup.starters[0].assistsCount)
        // Quien solo dio asistencia con scoringPlay=true y didAssist=true no debe contar como gol
        assertEquals(0, homeLineup.starters[1].goalsCount)
        assertEquals(1, homeLineup.starters[1].assistsCount)
        assertEquals(1, homeLineup.bench.first().goalsCount)
        assertTrue(homeLineup.hasSubstitutions)

        val finalStarters = homeLineup.resolveFinalStarters()
        assertEquals(2, finalStarters.size)
        assertEquals("Ricardo Marín", finalStarters[0].name)
        assertEquals(11, finalStarters[0].formationPlace)
        assertEquals("Attacking Midfielder Left", finalStarters[0].position)
        assertEquals("Richard Ledezma", finalStarters[1].name)
    }

    @Test
    fun resolveKnockoutMonths_limitsWindowUsingLatestRegularDateAndCoversMultiMonthPlayoffs() {
        val calendar = listOf(
            "2026-10-31T07:00Z",
            "2026-11-22T08:00Z",
            "2026-11-25T08:00Z",
            "2026-11-29T08:00Z",
            "2026-12-03T08:00Z",
            "2026-12-13T08:00Z"
        )
        // Durante Cuartos con Jornada regular el 22 nov, no debe consultar octubre
        val duringQf = MatchRepositoryImpl.resolveKnockoutMonths(
            calendarDates = calendar,
            latestRegularDate = java.time.LocalDate.of(2026, 11, 22),
            today = java.time.LocalDate.of(2026, 11, 24)
        )
        assertEquals(listOf("202611", "202612"), duringQf)

        // Durante la Final sin partidos regulares en la ventana, debe cubrir nov y dic completos
        val duringFinal = MatchRepositoryImpl.resolveKnockoutMonths(
            calendarDates = calendar,
            latestRegularDate = null,
            today = java.time.LocalDate.of(2026, 12, 10)
        )
        assertEquals(listOf("202611", "202612"), duringFinal)
    }

    @Test
    fun getMatches_domesticKnockoutLeague_mergesMonthlyKnockoutLegsWithoutAddingOldRegularMatches() = runBlocking {
        val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        val leg1Date = today.plusDays(1)
        val leg2Date = today.plusDays(4)
        val oldRegularDate = today.minusDays(10)
        val leg1Iso = "${leg1Date}T01:00Z"
        val leg2Iso = "${leg2Date}T01:00Z"
        val oldRegIso = "${oldRegularDate}T01:00Z"

        fun makeEvent(
            id: String,
            dateIso: String,
            slug: String,
            seriesTitle: String?,
            leg: Int? = null,
            seriesCompleted: Boolean = false,
            winnerId: String? = null
        ): EspnEventDto {
            val comps = listOf(
                EspnCompetitorDto(
                    id = "223",
                    homeAway = "home",
                    score = "0",
                    team = EspnTeamDto(id = "223", name = "Toluca", displayName = "Toluca")
                ),
                EspnCompetitorDto(
                    id = "216",
                    homeAway = "away",
                    score = "0",
                    team = EspnTeamDto(id = "216", name = "Atlas", displayName = "Atlas")
                )
            )
            return EspnEventDto(
                id = id,
                date = dateIso,
                name = "Atlas at Toluca",
                season = EspnSeasonDto(year = today.year, slug = slug),
                competitions = listOf(
                    EspnCompetitionDto(
                        id = id,
                        date = dateIso,
                        competitors = comps,
                        status = EspnStatusDto(type = EspnStatusTypeDto(state = "pre", completed = false)),
                        leg = leg?.let { com.ixeken.ulama.data.espn.model.EspnLegDto(value = it) },
                        series = seriesTitle?.let {
                            com.ixeken.ulama.data.espn.model.EspnSeriesDto(
                                title = it,
                                completed = seriesCompleted,
                                competitors = listOf(
                                    com.ixeken.ulama.data.espn.model.EspnSeriesCompetitorDto(id = "223", winner = winnerId == "223"),
                                    com.ixeken.ulama.data.espn.model.EspnSeriesCompetitorDto(id = "216", winner = winnerId == "216")
                                )
                            )
                        }
                    )
                )
            )
        }

        val leg1Event = makeEvent("ko_leg1", leg1Iso, "apertura---quarterfinals", "Apertura - Quarterfinals", leg = 1)
        val leg2Event = makeEvent("ko_leg2", leg2Iso, "apertura---quarterfinals", "Apertura - Quarterfinals", leg = 2, seriesCompleted = true, winnerId = "223")
        val oldRegularEvent = makeEvent("old_reg", oldRegIso, "torneo-apertura", null, leg = null)

        val fakeApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?): EspnScoreboardResponse {
                return when {
                    dates == null -> EspnScoreboardResponse(
                        leagues = listOf(
                            EspnLeagueCalendarDto(
                                calendar = listOf(
                                    kotlinx.serialization.json.JsonPrimitive(leg1Iso),
                                    kotlinx.serialization.json.JsonPrimitive(leg2Iso)
                                )
                            )
                        ),
                        events = listOf(leg1Event)
                    )
                    dates.length == 6 -> EspnScoreboardResponse(events = listOf(oldRegularEvent, leg1Event, leg2Event))
                    else -> EspnScoreboardResponse(events = listOf(leg1Event))
                }
            }
            override suspend fun getStandings(league: String, cacheControl: String?): EspnStandingsResponse = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?): com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(fakeApi, Dispatchers.Unconfined)
        val matches = repo.getMatches(League.LIGA_MX, forceRefresh = true).getOrThrow()

        assertEquals(2, matches.size)
        assertTrue(matches.any { it.id == "ko_leg1" && it.isKnockout })
        val mappedLeg2 = matches.first { it.id == "ko_leg2" }
        assertTrue(mappedLeg2.isKnockout)
        assertTrue(mappedLeg2.isSeriesCompleted)
        assertEquals("223", mappedLeg2.seriesWinnerTeamId)
        assertTrue(matches.none { it.id == "old_reg" })
    }

    @Test
    fun getStandings_mergeStandingsGroups_unifiesAndReranksByPoints() = runBlocking {
        fun makeEntry(id: String, name: String, rank: Double, pts: Double, gd: Double) =
            com.ixeken.ulama.data.espn.model.EspnStandingsEntryDto(
                team = com.ixeken.ulama.data.espn.model.EspnStandingsTeamDto(
                    id = id,
                    name = name,
                    displayName = name
                ),
                stats = listOf(
                    com.ixeken.ulama.data.espn.model.EspnStatDto(name = "rank", value = rank),
                    com.ixeken.ulama.data.espn.model.EspnStatDto(name = "points", value = pts),
                    com.ixeken.ulama.data.espn.model.EspnStatDto(name = "pointDifferential", value = gd)
                )
            )

        val fakeApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?) =
                EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?) = EspnStandingsResponse(
                children = listOf(
                    com.ixeken.ulama.data.espn.model.EspnStandingsChildDto(
                        name = "Group 1",
                        standings = com.ixeken.ulama.data.espn.model.EspnStandingsEntriesDto(
                            season = 2026,
                            entries = listOf(
                                makeEntry("1", "Monterrey", 1.0, 37.0, 20.0),
                                makeEntry("4", "Toluca", 4.0, 27.0, 10.0)
                            )
                        )
                    ),
                    com.ixeken.ulama.data.espn.model.EspnStandingsChildDto(
                        name = "Group 2",
                        standings = com.ixeken.ulama.data.espn.model.EspnStandingsEntriesDto(
                            season = 2026,
                            entries = listOf(
                                makeEntry("2", "America", 1.0, 37.0, 25.0),
                                makeEntry("3", "UNAM", 3.0, 20.0, 5.0)
                            )
                        )
                    )
                )
            )
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(fakeApi, Dispatchers.Unconfined)
        val standings = repo.getStandings(League.LIGA_MX_FEMENIL, forceRefresh = true).getOrThrow()
        val groups = repo.getGroups(League.LIGA_MX_FEMENIL, forceRefresh = false).getOrThrow()

        assertTrue(groups.isEmpty())
        assertEquals(listOf("2", "1", "4", "3"), standings.map { it.teamId })
        assertEquals(listOf(1, 2, 3, 4), standings.map { it.rank })
    }

    @Test
    fun getGroups_appliesLeagueGroupCutoffsLegendAndStripsTournamentPrefix() = runBlocking {
        fun makeEntry(id: String, name: String, rank: Double, pts: Double) =
            com.ixeken.ulama.data.espn.model.EspnStandingsEntryDto(
                team = com.ixeken.ulama.data.espn.model.EspnStandingsTeamDto(
                    id = id,
                    name = name,
                    displayName = name
                ),
                stats = listOf(
                    com.ixeken.ulama.data.espn.model.EspnStatDto(name = "rank", value = rank),
                    com.ixeken.ulama.data.espn.model.EspnStatDto(name = "points", value = pts)
                )
            )

        val fakeApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?) =
                EspnScoreboardResponse()
            override suspend fun getStandings(league: String, cacheControl: String?) = when (league) {
                League.CAF_CHAMPIONS.code -> EspnStandingsResponse(
                    children = listOf(
                        com.ixeken.ulama.data.espn.model.EspnStandingsChildDto(
                            name = "2026-27 CAF Champions League - Group A",
                            standings = com.ixeken.ulama.data.espn.model.EspnStandingsEntriesDto(
                                entries = listOf(makeEntry("1", "Al Ahly", 1.0, 9.0), makeEntry("2", "FAR", 2.0, 6.0))
                            )
                        ),
                        com.ixeken.ulama.data.espn.model.EspnStandingsChildDto(
                            name = "2026-27 CAF Champions League - Group B",
                            standings = com.ixeken.ulama.data.espn.model.EspnStandingsEntriesDto(
                                entries = listOf(makeEntry("3", "Sundowns", 1.0, 9.0), makeEntry("4", "Esperance", 2.0, 6.0))
                            )
                        )
                    )
                )
                else -> EspnStandingsResponse(
                    children = listOf(
                        com.ixeken.ulama.data.espn.model.EspnStandingsChildDto(
                            name = "Group A",
                            standings = com.ixeken.ulama.data.espn.model.EspnStandingsEntriesDto(
                                entries = (1..4).map { makeEntry("a$it", "Club A$it", it.toDouble(), (12 - it * 2).toDouble()) }
                            )
                        ),
                        com.ixeken.ulama.data.espn.model.EspnStandingsChildDto(
                            name = "Group B",
                            standings = com.ixeken.ulama.data.espn.model.EspnStandingsEntriesDto(
                                entries = (1..4).map { makeEntry("b$it", "Club B$it", it.toDouble(), (12 - it * 2).toDouble()) }
                            )
                        )
                    )
                )
            }
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(fakeApi, Dispatchers.Unconfined)

        val cafGroups = repo.getGroups(League.CAF_CHAMPIONS, forceRefresh = true).getOrThrow()
        assertEquals(listOf("A", "B"), cafGroups.map { it.name })
        assertEquals(listOf(2), cafGroups.first().cutoffs.map { it.rankAfter })

        val libGroups = repo.getGroups(League.LIBERTADORES, forceRefresh = true).getOrThrow()
        assertEquals(listOf(2, 3), libGroups.first().cutoffs.map { it.rankAfter })
        assertEquals(2, libGroups.first().legend.size)
    }

    @Test
    fun getMatches_mapsSeriesTotalGamesAndConferenceEliminationSlugs() = runBlocking {
        val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        val todayIso = "${today}T20:00:00Z"

        fun makeEvent(id: String, slug: String, seriesTitle: String?, totalGames: Int?) =
            com.ixeken.ulama.data.espn.model.EspnEventDto(
                id = id,
                date = todayIso,
                season = com.ixeken.ulama.data.espn.model.EspnSeasonDto(slug = slug),
                competitions = listOf(
                    com.ixeken.ulama.data.espn.model.EspnCompetitionDto(
                        id = id,
                        date = todayIso,
                        competitors = listOf(
                            EspnCompetitorDto(id = "1", homeAway = "home", team = EspnTeamDto(id = "1", name = "Home")),
                            EspnCompetitorDto(id = "2", homeAway = "away", team = EspnTeamDto(id = "2", name = "Away"))
                        ),
                        status = EspnStatusDto(type = EspnStatusTypeDto(state = "pre", completed = false)),
                        series = seriesTitle?.let {
                            com.ixeken.ulama.data.espn.model.EspnSeriesDto(title = it, totalCompetitions = totalGames)
                        }
                    )
                )
            )

        val fakeApi = object : EspnApiService {
            override suspend fun getScoreboard(league: String, dates: String?, limit: Int?, cacheControl: String?) =
                when (league) {
                    League.MLS.code -> EspnScoreboardResponse(
                        events = listOf(
                            makeEvent("m1", "eastern-conference-playoffs---round-one", "Playoff Series", 3),
                            makeEvent("m2", "eastern-conference-playoffs---semifinals", null, null),
                            makeEvent("m3", "mls-cup", null, null)
                        )
                    )
                    else -> EspnScoreboardResponse(
                        events = listOf(
                            makeEvent("a1", "elimination-finals", null, null),
                            makeEvent("a2", "grand-final", null, null)
                        )
                    )
                }
            override suspend fun getStandings(league: String, cacheControl: String?) = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(fakeApi, Dispatchers.Unconfined)
        val mlsMatches = repo.getMatches(League.MLS, forceRefresh = true).getOrThrow()
        assertEquals(3, mlsMatches.first { it.id == "m1" }.seriesTotalGames)
        assertTrue(mlsMatches.first { it.id == "m1" }.isBestOfSeries)
        assertEquals("conference-semifinals", mlsMatches.first { it.id == "m2" }.seriesTitle)
        assertEquals("mls-cup", mlsMatches.first { it.id == "m3" }.seriesTitle)
        assertTrue(mlsMatches.all { it.isKnockout })

        val ausMatches = repo.getMatches(League.A_LEAGUE_MEN, forceRefresh = true).getOrThrow()
        assertEquals("elimination-finals", ausMatches.first { it.id == "a1" }.seriesTitle)
        assertEquals("grand-final", ausMatches.first { it.id == "a2" }.seriesTitle)
    }

    @Test
    fun getMatches_emitsPreliminaryMatchesBeforeClusterDatesAndSupportsCacheOnlySeed() = runBlocking {
        val today = LocalDate.now(ZoneId.systemDefault())
        val tomorrow = today.plusDays(1)
        val todayIso = "${today}T20:00:00Z"
        val tomorrowIso = "${tomorrow}T20:00:00Z"
        val tomorrowBasic = tomorrow.format(DateTimeFormatter.BASIC_ISO_DATE)

        val callOrder = mutableListOf<String>()
        val capturedHeaders = mutableListOf<String?>()

        fun buildEvent(id: String, dateIso: String) = EspnEventDto(
            id = id,
            date = dateIso,
            competitions = listOf(
                EspnCompetitionDto(
                    id = id,
                    date = dateIso,
                    competitors = listOf(
                        EspnCompetitorDto(id = "1", homeAway = "home", team = EspnTeamDto(id = "1", name = "America")),
                        EspnCompetitorDto(id = "2", homeAway = "away", team = EspnTeamDto(id = "2", name = "Chivas"))
                    ),
                    status = EspnStatusDto(type = EspnStatusTypeDto(state = "pre", completed = false))
                )
            )
        )

        val fakeApi = object : EspnApiService {
            override suspend fun getScoreboard(
                league: String,
                dates: String?,
                limit: Int?,
                cacheControl: String?
            ): EspnScoreboardResponse {
                capturedHeaders.add(cacheControl)
                callOrder.add(dates ?: "base")
                return if (dates == null) {
                    EspnScoreboardResponse(
                        leagues = listOf(
                            EspnLeagueCalendarDto(
                                calendar = listOf(
                                    kotlinx.serialization.json.JsonPrimitive(todayIso),
                                    kotlinx.serialization.json.JsonPrimitive(tomorrowIso)
                                )
                            )
                        ),
                        events = listOf(buildEvent("today_match", todayIso))
                    )
                } else if (dates == tomorrowBasic) {
                    EspnScoreboardResponse(events = listOf(buildEvent("tomorrow_match", tomorrowIso)))
                } else {
                    EspnScoreboardResponse(events = emptyList())
                }
            }

            override suspend fun getStandings(league: String, cacheControl: String?) = EspnStandingsResponse()
            override suspend fun getMatchSummary(league: String, eventId: String, cacheControl: String?) =
                com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse()
        }

        val repo = MatchRepositoryImpl(fakeApi, Dispatchers.Unconfined)
        var preliminarySnapshotIds: List<String> = emptyList()
        var callOrderAtPreliminary: List<String> = emptyList()

        val finalMatches = repo.getMatches(
            league = League.LIGA_MX,
            forceRefresh = true,
            onPreliminaryMatches = { prelim ->
                preliminarySnapshotIds = prelim.map { it.id }
                callOrderAtPreliminary = callOrder.toList()
            }
        ).getOrThrow()

        // Fase 1 emite de inmediato tras la llamada base antes de consultar las fechas secundarias del calendario
        assertEquals(listOf("today_match"), preliminarySnapshotIds)
        assertEquals(listOf("base"), callOrderAtPreliminary)
        assertEquals(listOf("today_match", "tomorrow_match"), finalMatches.map { it.id })

        // Semilla cacheOnly inyecta only-if-cached cuando no existe cache previa en memoria
        capturedHeaders.clear()
        val freshRepo = MatchRepositoryImpl(fakeApi, Dispatchers.Unconfined)
        freshRepo.getMatches(league = League.PREMIER_LEAGUE, cacheOnly = true)
        assertTrue(capturedHeaders.all { it == com.ixeken.ulama.core.network.CacheInterceptor.CACHE_CONTROL_DISK_SEED })
    }
}


