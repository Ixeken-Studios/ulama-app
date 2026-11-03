package com.ixeken.ulama.data.debug

import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.CutoffLineStyle
import com.ixeken.ulama.core.model.GoalScorer
import com.ixeken.ulama.core.model.GroupStandings
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.LineupPlayer
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchDetail
import com.ixeken.ulama.core.model.MatchStatComparison
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.MatchStatus
import com.ixeken.ulama.core.model.RadialBracketBuilder
import com.ixeken.ulama.core.model.RadialBracketTree
import com.ixeken.ulama.core.model.RadialMatch
import com.ixeken.ulama.core.model.RadialRound
import com.ixeken.ulama.core.model.RadialTeam
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.core.model.TableCutoff
import com.ixeken.ulama.core.model.TableLegendEntry
import com.ixeken.ulama.core.model.TableZoneType
import com.ixeken.ulama.core.model.TacticalLine
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.core.model.TeamLineup
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Proveedor de datos local y completamente encapsulado para la liga de pruebas (League.DEBUG).
 * Genera partidos con metadatos reales de fases eliminatorias, idas y vueltas, marcadores globales,
 * tiempos extras, tandas de penales y fases de grupos para probar componentes visuales y el arbol radial.
 */
object DebugLeagueData {

    private fun teamRealMadrid(isHome: Boolean, score: Int? = null) = Team(
        id = "86",
        name = "Real Madrid",
        shortName = "Real Madrid",
        abbreviation = "RMA",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/86.png",
        isHome = isHome,
        score = score,
        isWinner = (score ?: 0) > 1,
        color = "00529F"
    )

    private fun teamBayernMunich(isHome: Boolean, score: Int? = null) = Team(
        id = "132",
        name = "Bayern Munich",
        shortName = "Bayern",
        abbreviation = "BAY",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/132.png",
        isHome = isHome,
        score = score,
        isWinner = false,
        color = "DC052D"
    )

    private fun teamBarcelona(isHome: Boolean, score: Int? = null) = Team(
        id = "83",
        name = "Barcelona",
        shortName = "Barça",
        abbreviation = "BAR",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/83.png",
        isHome = isHome,
        score = score,
        isWinner = false,
        color = "A50044"
    )

    private fun teamInter(isHome: Boolean, score: Int? = null) = Team(
        id = "110",
        name = "Inter Milan",
        shortName = "Inter",
        abbreviation = "INT",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/110.png",
        isHome = isHome,
        score = score,
        isWinner = false,
        color = "0068A8"
    )

    private fun teamArsenal(isHome: Boolean, score: Int? = null) = Team(
        id = "359",
        name = "Arsenal",
        shortName = "Arsenal",
        abbreviation = "ARS",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/359.png",
        isHome = isHome,
        score = score,
        isWinner = true,
        color = "EF0107"
    )

    private fun teamPSG(isHome: Boolean, score: Int? = null) = Team(
        id = "160",
        name = "Paris Saint-Germain",
        shortName = "PSG",
        abbreviation = "PSG",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/160.png",
        isHome = isHome,
        score = score,
        isWinner = false,
        color = "004170"
    )

    private fun teamDortmund(isHome: Boolean, score: Int? = null, shootout: Int? = null) = Team(
        id = "124",
        name = "Borussia Dortmund",
        shortName = "Dortmund",
        abbreviation = "BVB",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/124.png",
        isHome = isHome,
        score = score,
        isWinner = true,
        shootoutScore = shootout,
        color = "FDE100"
    )

    private fun teamAtletico(isHome: Boolean, score: Int? = null, shootout: Int? = null) = Team(
        id = "1068",
        name = "Atlético de Madrid",
        shortName = "Atleti",
        abbreviation = "ATM",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/1068.png",
        isHome = isHome,
        score = score,
        isWinner = false,
        shootoutScore = shootout,
        color = "CB3524"
    )

    private fun teamManCity(isHome: Boolean, score: Int? = null) = Team(
        id = "382",
        name = "Manchester City",
        shortName = "Man City",
        abbreviation = "MCI",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/382.png",
        isHome = isHome,
        score = score,
        isWinner = false,
        color = "6CABDD"
    )

    private fun teamJuventus(isHome: Boolean, score: Int? = null) = Team(
        id = "111",
        name = "Juventus",
        shortName = "Juve",
        abbreviation = "JUV",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/111.png",
        isHome = isHome,
        score = score,
        isWinner = false,
        color = "000000"
    )

    private fun teamMilan(isHome: Boolean, score: Int? = null) = Team(
        id = "103",
        name = "AC Milan",
        shortName = "Milan",
        abbreviation = "MIL",
        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/103.png",
        isHome = isHome,
        score = score,
        isWinner = false,
        color = "FB090B"
    )

    /**
     * Retorna la coleccion de encuentros de prueba distribuidos en los tres filtros temporales.
     * Utiliza tiempos calculados dinamicamente desde Instant.now() para que nunca queden desfasados.
     */
    fun getMatches(): List<Match> {
        val now = Instant.now()
        val todayIso = now.toString()
        val todayFinishedIso = now.minus(3, ChronoUnit.HOURS).toString()
        val yesterdayIso = now.minus(1, ChronoUnit.DAYS).toString()
        val pastTwoDaysIso = now.minus(2, ChronoUnit.DAYS).toString()
        val pastWeekIso = now.minus(7, ChronoUnit.DAYS).toString()
        val tomorrowIso = now.plus(1, ChronoUnit.DAYS).toString()
        val inThreeDaysIso = now.plus(3, ChronoUnit.DAYS).toString()
        val inFiveDaysIso = now.plus(5, ChronoUnit.DAYS).toString()

        return listOf(
            // --- HOY (TODAY) ---
            Match(
                id = "debug_1_live_knockout",
                dateUtc = todayIso,
                homeTeam = teamRealMadrid(isHome = true, score = 2),
                awayTeam = teamBayernMunich(isHome = false, score = 1),
                status = MatchStatus(
                    state = MatchState.LIVE,
                    displayClock = "78'",
                    detail = "78' - 2do Tiempo",
                    isCompleted = false
                ),
                venueName = "Santiago Bernabéu",
                league = League.DEBUG,
                seriesTitle = "Semifinal",
                legNumber = 2,
                legText = "Vuelta",
                aggregateHomeScore = 4,
                aggregateAwayScore = 3,
                seriesSummary = "Vuelta · Real Madrid lidera 4-3 en el global"
            ),
            Match(
                id = "debug_2_live_group",
                dateUtc = todayIso,
                homeTeam = teamBarcelona(isHome = true, score = 1),
                awayTeam = teamInter(isHome = false, score = 1),
                status = MatchStatus(
                    state = MatchState.LIVE,
                    displayClock = "55'",
                    detail = "55' - 2do Tiempo",
                    isCompleted = false
                ),
                venueName = "Spotify Camp Nou",
                league = League.DEBUG,
                seriesTitle = "Fase de Grupos · J5"
            ),
            Match(
                id = "debug_3_finished_today",
                dateUtc = todayFinishedIso,
                homeTeam = teamArsenal(isHome = true, score = 3),
                awayTeam = teamPSG(isHome = false, score = 0),
                status = MatchStatus(
                    state = MatchState.FINISHED,
                    displayClock = "90'",
                    detail = "Finalizado",
                    isCompleted = true
                ),
                venueName = "Emirates Stadium",
                league = League.DEBUG,
                seriesTitle = "Cuartos de Final",
                legNumber = 2,
                legText = "Vuelta",
                aggregateHomeScore = 5,
                aggregateAwayScore = 2,
                seriesSummary = "Vuelta · Arsenal gana 5-2 en el global"
            ),

            // --- PASADOS (PAST) ---
            Match(
                id = "debug_4_past_first_leg",
                dateUtc = pastWeekIso,
                homeTeam = teamBayernMunich(isHome = true, score = 2),
                awayTeam = teamRealMadrid(isHome = false, score = 2),
                status = MatchStatus(
                    state = MatchState.FINISHED,
                    displayClock = "90'",
                    detail = "Finalizado",
                    isCompleted = true
                ),
                venueName = "Allianz Arena",
                league = League.DEBUG,
                seriesTitle = "Semifinal",
                legNumber = 1,
                legText = "Ida"
            ),
            Match(
                id = "debug_5_past_penalties",
                dateUtc = pastTwoDaysIso,
                homeTeam = teamDortmund(isHome = true, score = 1, shootout = 4),
                awayTeam = teamAtletico(isHome = false, score = 1, shootout = 2),
                status = MatchStatus(
                    state = MatchState.FINISHED,
                    displayClock = "120'",
                    detail = "Penales",
                    isCompleted = true,
                    isFinishedPenalties = true
                ),
                venueName = "Signal Iduna Park",
                league = League.DEBUG,
                seriesTitle = "Octavos de Final",
                legNumber = 2,
                legText = "Vuelta",
                aggregateHomeScore = 3,
                aggregateAwayScore = 3,
                seriesSummary = "Dortmund avanza 4-2 en penales tras 3-3 global"
            ),
            Match(
                id = "debug_6_past_group",
                dateUtc = yesterdayIso,
                homeTeam = teamJuventus(isHome = true, score = 2),
                awayTeam = teamMilan(isHome = false, score = 0),
                status = MatchStatus(
                    state = MatchState.FINISHED,
                    displayClock = "90'",
                    detail = "Finalizado",
                    isCompleted = true
                ),
                venueName = "Allianz Stadium",
                league = League.DEBUG,
                seriesTitle = "Fase de Grupos · J4"
            ),

            // --- PRÓXIMOS (UPCOMING) ---
            Match(
                id = "debug_7_upcoming_final",
                dateUtc = tomorrowIso,
                homeTeam = teamManCity(isHome = true),
                awayTeam = teamRealMadrid(isHome = false),
                status = MatchStatus(
                    state = MatchState.SCHEDULED,
                    displayClock = "14:00",
                    detail = "Programado",
                    isCompleted = false
                ),
                venueName = "Wembley Stadium",
                league = League.DEBUG,
                seriesTitle = "Gran Final"
            ),
            Match(
                id = "debug_8_upcoming_first_leg",
                dateUtc = inThreeDaysIso,
                homeTeam = teamAtletico(isHome = true),
                awayTeam = teamPSG(isHome = false),
                status = MatchStatus(
                    state = MatchState.SCHEDULED,
                    displayClock = "19:00",
                    detail = "Programado",
                    isCompleted = false
                ),
                venueName = "Cívitas Metropolitano",
                league = League.DEBUG,
                seriesTitle = "Cuartos de Final",
                legNumber = 1,
                legText = "Ida"
            ),
            Match(
                id = "debug_9_upcoming_group",
                dateUtc = inFiveDaysIso,
                homeTeam = teamInter(isHome = true),
                awayTeam = teamBarcelona(isHome = false),
                status = MatchStatus(
                    state = MatchState.SCHEDULED,
                    displayClock = "20:45",
                    detail = "Programado",
                    isCompleted = false
                ),
                venueName = "San Siro",
                league = League.DEBUG,
                seriesTitle = "Fase de Grupos · J6"
            )
        )
    }

    /**
     * Retorna la tabla general de posiciones de prueba con 8 clubes y cortes de clasificacion.
     */
    fun getStandings(): List<StandingEntry> {
        return listOf(
            StandingEntry(
                rank = 1,
                teamName = "Real Madrid",
                teamShortName = "Real Madrid",
                teamAbbreviation = "RMA",
                logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/86.png",
                gamesPlayed = 6,
                wins = 5,
                draws = 1,
                losses = 0,
                goalsFor = 16,
                goalsAgainst = 5,
                goalDifference = 11,
                points = 16,
                teamId = "86"
            ),
            StandingEntry(
                rank = 2,
                teamName = "Bayern Munich",
                teamShortName = "Bayern",
                teamAbbreviation = "BAY",
                logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/132.png",
                gamesPlayed = 6,
                wins = 4,
                draws = 1,
                losses = 1,
                goalsFor = 14,
                goalsAgainst = 7,
                goalDifference = 7,
                points = 13,
                teamId = "132"
            ),
            StandingEntry(
                rank = 3,
                teamName = "Arsenal",
                teamShortName = "Arsenal",
                teamAbbreviation = "ARS",
                logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/359.png",
                gamesPlayed = 6,
                wins = 3,
                draws = 2,
                losses = 1,
                goalsFor = 11,
                goalsAgainst = 6,
                goalDifference = 5,
                points = 11,
                teamId = "359"
            ),
            StandingEntry(
                rank = 4,
                teamName = "Barcelona",
                teamShortName = "Barça",
                teamAbbreviation = "BAR",
                logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/83.png",
                gamesPlayed = 6,
                wins = 3,
                draws = 1,
                losses = 2,
                goalsFor = 12,
                goalsAgainst = 9,
                goalDifference = 3,
                points = 10,
                teamId = "83"
            ),
            StandingEntry(
                rank = 5,
                teamName = "Paris Saint-Germain",
                teamShortName = "PSG",
                teamAbbreviation = "PSG",
                logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/160.png",
                gamesPlayed = 6,
                wins = 2,
                draws = 2,
                losses = 2,
                goalsFor = 8,
                goalsAgainst = 9,
                goalDifference = -1,
                points = 8,
                teamId = "160"
            ),
            StandingEntry(
                rank = 6,
                teamName = "Borussia Dortmund",
                teamShortName = "Dortmund",
                teamAbbreviation = "BVB",
                logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/124.png",
                gamesPlayed = 6,
                wins = 1,
                draws = 1,
                losses = 4,
                goalsFor = 6,
                goalsAgainst = 12,
                goalDifference = -6,
                points = 4,
                teamId = "124"
            ),
            StandingEntry(
                rank = 7,
                teamName = "Inter Milan",
                teamShortName = "Inter",
                teamAbbreviation = "INT",
                logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/110.png",
                gamesPlayed = 6,
                wins = 1,
                draws = 0,
                losses = 5,
                goalsFor = 4,
                goalsAgainst = 13,
                goalDifference = -9,
                points = 3,
                teamId = "110"
            ),
            StandingEntry(
                rank = 8,
                teamName = "Atlético de Madrid",
                teamShortName = "Atleti",
                teamAbbreviation = "ATM",
                logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/1068.png",
                gamesPlayed = 6,
                wins = 0,
                draws = 2,
                losses = 4,
                goalsFor = 3,
                goalsAgainst = 13,
                goalDifference = -10,
                points = 2,
                teamId = "1068"
            )
        )
    }

    /**
     * Provee dos grupos de prueba (Grupo A y Grupo B) con cuatro equipos cada uno,
     * configurados con cortes de clasificacion para los dos primeros puestos y leyendas explicativas.
     */
    fun getGroups(): List<GroupStandings> {
        val groupCutoffs = listOf(
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID)
        )
        val groupLegend = listOf(
            TableLegendEntry(rangeText = "1-2", labelRes = R.string.legend_group_advance)
        )

        return listOf(
            GroupStandings(
                name = "A",
                standings = listOf(
                    StandingEntry(
                        rank = 1,
                        teamName = "Real Madrid",
                        teamShortName = "Real Madrid",
                        teamAbbreviation = "RMA",
                        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/86.png",
                        gamesPlayed = 6,
                        wins = 5,
                        draws = 0,
                        losses = 1,
                        goalsFor = 14,
                        goalsAgainst = 6,
                        goalDifference = 8,
                        points = 15,
                        teamId = "86"
                    ),
                    StandingEntry(
                        rank = 2,
                        teamName = "Bayern Munich",
                        teamShortName = "Bayern",
                        teamAbbreviation = "BAY",
                        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/132.png",
                        gamesPlayed = 6,
                        wins = 4,
                        draws = 1,
                        losses = 1,
                        goalsFor = 13,
                        goalsAgainst = 7,
                        goalDifference = 6,
                        points = 13,
                        teamId = "132"
                    ),
                    StandingEntry(
                        rank = 3,
                        teamName = "Inter Milan",
                        teamShortName = "Inter",
                        teamAbbreviation = "INT",
                        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/110.png",
                        gamesPlayed = 6,
                        wins = 2,
                        draws = 1,
                        losses = 3,
                        goalsFor = 7,
                        goalsAgainst = 9,
                        goalDifference = -2,
                        points = 7,
                        teamId = "110"
                    ),
                    StandingEntry(
                        rank = 4,
                        teamName = "Atlético de Madrid",
                        teamShortName = "Atleti",
                        teamAbbreviation = "ATM",
                        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/1068.png",
                        gamesPlayed = 6,
                        wins = 0,
                        draws = 2,
                        losses = 4,
                        goalsFor = 3,
                        goalsAgainst = 15,
                        goalDifference = -12,
                        points = 2,
                        teamId = "1068"
                    )
                ),
                cutoffs = groupCutoffs,
                legend = groupLegend
            ),
            GroupStandings(
                name = "B",
                standings = listOf(
                    StandingEntry(
                        rank = 1,
                        teamName = "Arsenal",
                        teamShortName = "Arsenal",
                        teamAbbreviation = "ARS",
                        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/359.png",
                        gamesPlayed = 6,
                        wins = 4,
                        draws = 2,
                        losses = 0,
                        goalsFor = 12,
                        goalsAgainst = 5,
                        goalDifference = 7,
                        points = 14,
                        teamId = "359"
                    ),
                    StandingEntry(
                        rank = 2,
                        teamName = "Borussia Dortmund",
                        teamShortName = "Dortmund",
                        teamAbbreviation = "BVB",
                        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/124.png",
                        gamesPlayed = 6,
                        wins = 3,
                        draws = 2,
                        losses = 1,
                        goalsFor = 10,
                        goalsAgainst = 7,
                        goalDifference = 3,
                        points = 11,
                        teamId = "124"
                    ),
                    StandingEntry(
                        rank = 3,
                        teamName = "Barcelona",
                        teamShortName = "Barça",
                        teamAbbreviation = "BAR",
                        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/83.png",
                        gamesPlayed = 6,
                        wins = 2,
                        draws = 1,
                        losses = 3,
                        goalsFor = 8,
                        goalsAgainst = 9,
                        goalDifference = -1,
                        points = 7,
                        teamId = "83"
                    ),
                    StandingEntry(
                        rank = 4,
                        teamName = "Paris Saint-Germain",
                        teamShortName = "PSG",
                        teamAbbreviation = "PSG",
                        logoUrl = "https://a.espncdn.com/i/teamlogos/soccer/500/160.png",
                        gamesPlayed = 6,
                        wins = 0,
                        draws = 1,
                        losses = 5,
                        goalsFor = 4,
                        goalsAgainst = 13,
                        goalDifference = -9,
                        points = 1,
                        teamId = "160"
                    )
                ),
                cutoffs = groupCutoffs,
                legend = groupLegend
            )
        )
    }

    /**
     * Resuelve la tabla de clasificacion de grupo si el encuentro corresponde a una fase de grupos.
     */
    fun getGroupStandingsForMatch(matchId: String): List<StandingEntry>? {
        if (matchId == "debug_2_live_group") {
            // El partido 2 es Barcelona vs Inter, que en este torneo simulado juegan en el Grupo A o B
            return getGroups().firstOrNull { it.name == "A" }?.standings
        }
        return null
    }

    /**
     * Provee detalles realistas para cualquiera de los encuentros de prueba al abrir el modal inferior.
     */
    fun getMatchDetail(matchId: String): MatchDetail {
        return MatchDetail(
            matchId = matchId,
            homeScorers = listOf(
                GoalScorer(playerName = "Vinícius Jr", minutesDisplay = "23'"),
                GoalScorer(playerName = "Bellingham", minutesDisplay = "68'")
            ),
            awayScorers = listOf(
                GoalScorer(playerName = "Kane", minutesDisplay = "42'")
            ),
            stats = listOf(
                MatchStatComparison(
                    labelRes = R.string.stat_possession,
                    homeDisplayValue = "56%",
                    awayDisplayValue = "44%",
                    homeRatio = 0.56f,
                    awayRatio = 0.44f
                ),
                MatchStatComparison(
                    labelRes = R.string.stat_shots_on_target,
                    homeDisplayValue = "7",
                    awayDisplayValue = "4",
                    homeRatio = 0.63f,
                    awayRatio = 0.37f
                ),
                MatchStatComparison(
                    labelRes = R.string.stat_fouls,
                    homeDisplayValue = "9",
                    awayDisplayValue = "12",
                    homeRatio = 0.42f,
                    awayRatio = 0.58f
                )
            ),
            homeLineup = TeamLineup(
                teamId = "86",
                teamName = "Real Madrid",
                formation = "4-3-3",
                teamColor = "00529F",
                starters = listOf(
                    LineupPlayer("1", "Courtois", "1", "G", 1, TacticalLine.GOALKEEPER),
                    LineupPlayer("2", "Carvajal", "2", "D", 2, TacticalLine.DEFENSE),
                    LineupPlayer("3", "Rüdiger", "22", "D", 3, TacticalLine.DEFENSE),
                    LineupPlayer("4", "Militão", "3", "D", 4, TacticalLine.DEFENSE),
                    LineupPlayer("5", "Mendy", "23", "D", 5, TacticalLine.DEFENSE),
                    LineupPlayer("6", "Valverde", "8", "M", 6, TacticalLine.MIDFIELD),
                    LineupPlayer("7", "Tchouaméni", "14", "M", 7, TacticalLine.MIDFIELD),
                    LineupPlayer("8", "Bellingham", "5", "M", 8, TacticalLine.MIDFIELD),
                    LineupPlayer("9", "Rodrygo", "11", "A", 9, TacticalLine.FORWARD),
                    LineupPlayer("10", "Mbappé", "9", "A", 10, TacticalLine.FORWARD),
                    LineupPlayer("11", "Vinícius Jr", "7", "A", 11, TacticalLine.FORWARD)
                )
            ),
            awayLineup = TeamLineup(
                teamId = "132",
                teamName = "Bayern Munich",
                formation = "4-2-3-1",
                teamColor = "DC052D",
                starters = listOf(
                    LineupPlayer("12", "Neuer", "1", "G", 1, TacticalLine.GOALKEEPER),
                    LineupPlayer("13", "Kimmich", "6", "D", 2, TacticalLine.DEFENSE),
                    LineupPlayer("14", "Upamecano", "2", "D", 3, TacticalLine.DEFENSE),
                    LineupPlayer("15", "Kim", "3", "D", 4, TacticalLine.DEFENSE),
                    LineupPlayer("16", "Davies", "19", "D", 5, TacticalLine.DEFENSE),
                    LineupPlayer("17", "Pavlović", "45", "M", 6, TacticalLine.MIDFIELD),
                    LineupPlayer("18", "Palhinha", "16", "M", 7, TacticalLine.MIDFIELD),
                    LineupPlayer("19", "Olise", "17", "M", 8, TacticalLine.MIDFIELD),
                    LineupPlayer("20", "Musiala", "42", "M", 9, TacticalLine.MIDFIELD),
                    LineupPlayer("21", "Gnabry", "7", "M", 10, TacticalLine.MIDFIELD),
                    LineupPlayer("22", "Kane", "9", "A", 11, TacticalLine.FORWARD)
                )
            ),
            homeColor = "00529F",
            awayColor = "DC052D",
            isCompleted = true
        )
    }

    /**
     * Determina si un encuentro pertenece a la liga de depuracion.
     */
    fun isDebugMatch(matchId: String): Boolean = matchId.startsWith("debug_")

    /**
     * Provee el arbol de eliminatorias radiales para pruebas de visualizacion.
     * Soporta configuracion de 8 o 16 equipos manteniendo jerarquia estricta de cruces.
     *
     * @param totalTeams Numero de participantes en la llave (16 por omision).
     */
    fun getRadialBracketTree(totalTeams: Int = 16): RadialBracketTree {
        val rma = RadialTeam("86", "Real Madrid", "RMA", "https://a.espncdn.com/i/teamlogos/soccer/500/86.png")
        val bay = RadialTeam("132", "Bayern Munich", "BAY", "https://a.espncdn.com/i/teamlogos/soccer/500/132.png")
        val bvb = RadialTeam("124", "Borussia Dortmund", "BVB", "https://a.espncdn.com/i/teamlogos/soccer/500/124.png")
        val psg = RadialTeam("160", "Paris Saint-Germain", "PSG", "https://a.espncdn.com/i/teamlogos/soccer/500/160.png")
        val ars = RadialTeam("359", "Arsenal", "ARS", "https://a.espncdn.com/i/teamlogos/soccer/500/359.png")
        val bar = RadialTeam("83", "Barcelona", "BAR", "https://a.espncdn.com/i/teamlogos/soccer/500/83.png")
        val mci = RadialTeam("382", "Manchester City", "MCI", "https://a.espncdn.com/i/teamlogos/soccer/500/382.png")
        val atm = RadialTeam("1068", "Atlético de Madrid", "ATM", "https://a.espncdn.com/i/teamlogos/soccer/500/1068.png")
        val juv = RadialTeam("111", "Juventus", "JUV", "https://a.espncdn.com/i/teamlogos/soccer/500/111.png")
        val mil = RadialTeam("103", "AC Milan", "MIL", "https://a.espncdn.com/i/teamlogos/soccer/500/103.png")
        val int = RadialTeam("110", "Inter Milan", "INT", "https://a.espncdn.com/i/teamlogos/soccer/500/110.png")
        val liv = RadialTeam("364", "Liverpool", "LIV", "https://a.espncdn.com/i/teamlogos/soccer/500/364.png")
        val che = RadialTeam("363", "Chelsea", "CHE", "https://a.espncdn.com/i/teamlogos/soccer/500/363.png")
        val b04 = RadialTeam("131", "Bayer Leverkusen", "B04", "https://a.espncdn.com/i/teamlogos/soccer/500/131.png")
        val nap = RadialTeam("114", "Napoli", "NAP", "https://a.espncdn.com/i/teamlogos/soccer/500/114.png")
        val scp = RadialTeam("89", "Sporting CP", "SCP", "https://a.espncdn.com/i/teamlogos/soccer/500/89.png")

        val champion = rma.copy(isWinner = true)

        val finalRound = RadialRound(
            roundIndex = if (totalTeams == 8) 2 else 3,
            name = "Final",
            matches = listOf(
                RadialMatch(
                    id = "final_0",
                    matchIndex = 0,
                    teamA = rma.copy(score = "2", isWinner = true),
                    teamB = ars.copy(score = "1", isWinner = false),
                    winnerTeamId = "86",
                    statusText = "Final"
                )
            )
        )

        val semiRound = RadialRound(
            roundIndex = if (totalTeams == 8) 1 else 2,
            name = "Semifinales",
            matches = listOf(
                RadialMatch(
                    id = "semi_0",
                    matchIndex = 0,
                    teamA = rma.copy(score = "4", isWinner = true),
                    teamB = bvb.copy(score = "1", isWinner = false),
                    winnerTeamId = "86",
                    statusText = "4-1"
                ),
                RadialMatch(
                    id = "semi_1",
                    matchIndex = 1,
                    teamA = ars.copy(score = "2", isWinner = true),
                    teamB = mci.copy(score = "1", isWinner = false),
                    winnerTeamId = "359",
                    statusText = "2-1"
                )
            )
        )

        val qfRound = RadialRound(
            roundIndex = if (totalTeams == 8) 0 else 1,
            name = "Cuartos de Final",
            matches = listOf(
                RadialMatch("qf_0", 0, rma.copy(score = "2", isWinner = true), bay.copy(score = "1", isWinner = false), "86", "2-1"),
                RadialMatch("qf_1", 1, bvb.copy(score = "3", isWinner = true), psg.copy(score = "2", isWinner = false), "124", "3-2"),
                RadialMatch("qf_2", 2, ars.copy(score = "2", isWinner = true), bar.copy(score = "0", isWinner = false), "359", "2-0"),
                RadialMatch("qf_3", 3, mci.copy(score = "3", isWinner = true), atm.copy(score = "1", isWinner = false), "382", "3-1")
            )
        )

        val thirdPlaceMatch = RadialMatch(
            id = "third_0",
            matchIndex = 0,
            teamA = bvb.copy(score = "3", isWinner = true),
            teamB = mci.copy(score = "2", isWinner = false),
            winnerTeamId = "124",
            statusText = "3-2"
        )

        return when (totalTeams) {
            8 -> {
                RadialBracketTree(
                    totalTeams = 8,
                    rounds = listOf(qfRound, semiRound, finalRound),
                    champion = champion,
                    thirdPlaceMatch = thirdPlaceMatch
                )
            }
            32 -> {
                val tot = RadialTeam("367", "Tottenham", "TOT", "https://a.espncdn.com/i/teamlogos/soccer/500/367.png")
                val mun = RadialTeam("360", "Manchester United", "MUN", "https://a.espncdn.com/i/teamlogos/soccer/500/360.png")
                val new = RadialTeam("361", "Newcastle", "NEW", "https://a.espncdn.com/i/teamlogos/soccer/500/361.png")
                val vil = RadialTeam("102", "Villarreal", "VIL", "https://a.espncdn.com/i/teamlogos/soccer/500/102.png")
                val sev = RadialTeam("243", "Sevilla", "SEV", "https://a.espncdn.com/i/teamlogos/soccer/500/243.png")
                val rso = RadialTeam("88", "Real Sociedad", "RSO", "https://a.espncdn.com/i/teamlogos/soccer/500/88.png")
                val ath = RadialTeam("93", "Athletic Club", "ATH", "https://a.espncdn.com/i/teamlogos/soccer/500/93.png")
                val rom = RadialTeam("104", "Roma", "ROM", "https://a.espncdn.com/i/teamlogos/soccer/500/104.png")
                val laz = RadialTeam("112", "Lazio", "LAZ", "https://a.espncdn.com/i/teamlogos/soccer/500/112.png")
                val ata = RadialTeam("125", "Atalanta", "ATA", "https://a.espncdn.com/i/teamlogos/soccer/500/125.png")
                val fio = RadialTeam("109", "Fiorentina", "FIO", "https://a.espncdn.com/i/teamlogos/soccer/500/109.png")
                val rbl = RadialTeam("11420", "RB Leipzig", "RBL", "https://a.espncdn.com/i/teamlogos/soccer/500/11420.png")
                val fra = RadialTeam("125", "Frankfurt", "SGE", "https://a.espncdn.com/i/teamlogos/soccer/500/125.png")
                val por = RadialTeam("437", "Porto", "POR", "https://a.espncdn.com/i/teamlogos/soccer/500/437.png")
                val ben = RadialTeam("1929", "Benfica", "BEN", "https://a.espncdn.com/i/teamlogos/soccer/500/1929.png")
                val aja = RadialTeam("139", "Ajax", "AJA", "https://a.espncdn.com/i/teamlogos/soccer/500/139.png")

                val r32Round = RadialRound(
                    roundIndex = 0,
                    name = "Dieciseisavos de Final",
                    matches = listOf(
                        RadialMatch("r32_0", 0, rma.copy(score = "3", isWinner = true), tot.copy(score = "1", isWinner = false), "86", "3-1"),
                        RadialMatch("r32_1", 1, juv.copy(score = "2", isWinner = true), mun.copy(score = "0", isWinner = false), "111", "2-0"),
                        RadialMatch("r32_2", 2, bay.copy(score = "4", isWinner = true), new.copy(score = "1", isWinner = false), "132", "4-1"),
                        RadialMatch("r32_3", 3, mil.copy(score = "2", isWinner = true), vil.copy(score = "1", isWinner = false), "103", "2-1"),
                        RadialMatch("r32_4", 4, bvb.copy(score = "3", isWinner = true), sev.copy(score = "2", isWinner = false), "124", "3-2"),
                        RadialMatch("r32_5", 5, scp.copy(score = "2", isWinner = true), rso.copy(score = "1", isWinner = false), "89", "2-1"),
                        RadialMatch("r32_6", 6, psg.copy(score = "3", isWinner = true), ath.copy(score = "0", isWinner = false), "160", "3-0"),
                        RadialMatch("r32_7", 7, b04.copy(score = "2", isWinner = true), rom.copy(score = "1", isWinner = false), "131", "2-1"),
                        RadialMatch("r32_8", 8, ars.copy(score = "4", isWinner = true), laz.copy(score = "1", isWinner = false), "359", "4-1"),
                        RadialMatch("r32_9", 9, nap.copy(score = "2", isWinner = true), ata.copy(score = "1", isWinner = false), "114", "2-1"),
                        RadialMatch("r32_10", 10, bar.copy(score = "3", isWinner = true), fio.copy(score = "0", isWinner = false), "83", "3-0"),
                        RadialMatch("r32_11", 11, che.copy(score = "2", isWinner = true), rbl.copy(score = "1", isWinner = false), "363", "2-1"),
                        RadialMatch("r32_12", 12, mci.copy(score = "4", isWinner = true), fra.copy(score = "2", isWinner = false), "382", "4-2"),
                        RadialMatch("r32_13", 13, int.copy(score = "2", isWinner = true), por.copy(score = "1", isWinner = false), "110", "2-1"),
                        RadialMatch("r32_14", 14, atm.copy(score = "3", isWinner = true), ben.copy(score = "1", isWinner = false), "1068", "3-1"),
                        RadialMatch("r32_15", 15, liv.copy(score = "2", isWinner = true), aja.copy(score = "0", isWinner = false), "364", "2-0")
                    )
                )

                val r16Round = RadialRound(
                    roundIndex = 1,
                    name = "Octavos de Final",
                    matches = listOf(
                        RadialMatch("r16_0", 0, rma.copy(score = "3", isWinner = true), juv.copy(score = "1", isWinner = false), "86", "3-1"),
                        RadialMatch("r16_1", 1, bay.copy(score = "2", isWinner = true), mil.copy(score = "0", isWinner = false), "132", "2-0"),
                        RadialMatch("r16_2", 2, bvb.copy(score = "4", isWinner = true), scp.copy(score = "2", isWinner = false), "124", "4-2"),
                        RadialMatch("r16_3", 3, psg.copy(score = "2", isWinner = true), b04.copy(score = "1", isWinner = false), "160", "2-1"),
                        RadialMatch("r16_4", 4, ars.copy(score = "3", isWinner = true), nap.copy(score = "1", isWinner = false), "359", "3-1"),
                        RadialMatch("r16_5", 5, bar.copy(score = "2", isWinner = true), che.copy(score = "1", isWinner = false), "83", "2-1"),
                        RadialMatch("r16_6", 6, mci.copy(score = "4", isWinner = true), int.copy(score = "1", isWinner = false), "382", "4-1"),
                        RadialMatch("r16_7", 7, atm.copy(score = "2", isWinner = true), liv.copy(score = "1", isWinner = false), "1068", "2-1")
                    )
                )

                RadialBracketTree(
                    totalTeams = 32,
                    rounds = listOf(
                        r32Round,
                        r16Round,
                        qfRound.copy(roundIndex = 2),
                        semiRound.copy(roundIndex = 3),
                        finalRound.copy(roundIndex = 4)
                    ),
                    champion = champion,
                    thirdPlaceMatch = thirdPlaceMatch
                )
            }
            else -> {
                val r16Round = RadialRound(
                    roundIndex = 0,
                    name = "Octavos de Final",
                    matches = listOf(
                        RadialMatch("r16_0", 0, rma.copy(score = "3", isWinner = true), juv.copy(score = "1", isWinner = false), "86", "3-1"),
                        RadialMatch("r16_1", 1, bay.copy(score = "2", isWinner = true), mil.copy(score = "0", isWinner = false), "132", "2-0"),
                        RadialMatch("r16_2", 2, bvb.copy(score = "4", isWinner = true), scp.copy(score = "2", isWinner = false), "124", "4-2"),
                        RadialMatch("r16_3", 3, psg.copy(score = "2", isWinner = true), b04.copy(score = "1", isWinner = false), "160", "2-1"),
                        RadialMatch("r16_4", 4, ars.copy(score = "3", isWinner = true), nap.copy(score = "1", isWinner = false), "359", "3-1"),
                        RadialMatch("r16_5", 5, bar.copy(score = "2", isWinner = true), che.copy(score = "1", isWinner = false), "83", "2-1"),
                        RadialMatch("r16_6", 6, mci.copy(score = "4", isWinner = true), int.copy(score = "1", isWinner = false), "382", "4-1"),
                        RadialMatch("r16_7", 7, atm.copy(score = "2", isWinner = true), liv.copy(score = "1", isWinner = false), "1068", "2-1")
                    )
                )

                RadialBracketTree(
                    totalTeams = 16,
                    rounds = listOf(r16Round, qfRound, semiRound, finalRound),
                    champion = champion,
                    thirdPlaceMatch = thirdPlaceMatch
                )
            }
        }
    }

    /**
     * Provee el arbol de eliminatorias en estado intermedio hasta semifinales.
     * Los cruces previos ya se resolvieron mientras que las semifinales estan definidas pero sin jugar,
     * y la final junto al tercer lugar esperan a los ganadores respectivos.
     *
     * @param totalTeams Numero de participantes en la llave (16 por omision).
     */
    fun getRadialBracketTreeSemis(totalTeams: Int = 16): RadialBracketTree {
        val rma = RadialTeam("86", "Real Madrid", "RMA", "https://a.espncdn.com/i/teamlogos/soccer/500/86.png")
        val bay = RadialTeam("132", "Bayern Munich", "BAY", "https://a.espncdn.com/i/teamlogos/soccer/500/132.png")
        val bvb = RadialTeam("124", "Borussia Dortmund", "BVB", "https://a.espncdn.com/i/teamlogos/soccer/500/124.png")
        val psg = RadialTeam("160", "Paris Saint-Germain", "PSG", "https://a.espncdn.com/i/teamlogos/soccer/500/160.png")
        val ars = RadialTeam("359", "Arsenal", "ARS", "https://a.espncdn.com/i/teamlogos/soccer/500/359.png")
        val bar = RadialTeam("83", "Barcelona", "BAR", "https://a.espncdn.com/i/teamlogos/soccer/500/83.png")
        val mci = RadialTeam("382", "Manchester City", "MCI", "https://a.espncdn.com/i/teamlogos/soccer/500/382.png")
        val atm = RadialTeam("1068", "Atlético de Madrid", "ATM", "https://a.espncdn.com/i/teamlogos/soccer/500/1068.png")
        val juv = RadialTeam("111", "Juventus", "JUV", "https://a.espncdn.com/i/teamlogos/soccer/500/111.png")
        val mil = RadialTeam("103", "AC Milan", "MIL", "https://a.espncdn.com/i/teamlogos/soccer/500/103.png")
        val int = RadialTeam("110", "Inter Milan", "INT", "https://a.espncdn.com/i/teamlogos/soccer/500/110.png")
        val liv = RadialTeam("364", "Liverpool", "LIV", "https://a.espncdn.com/i/teamlogos/soccer/500/364.png")
        val che = RadialTeam("363", "Chelsea", "CHE", "https://a.espncdn.com/i/teamlogos/soccer/500/363.png")
        val b04 = RadialTeam("131", "Bayer Leverkusen", "B04", "https://a.espncdn.com/i/teamlogos/soccer/500/131.png")
        val nap = RadialTeam("114", "Napoli", "NAP", "https://a.espncdn.com/i/teamlogos/soccer/500/114.png")
        val scp = RadialTeam("89", "Sporting CP", "SCP", "https://a.espncdn.com/i/teamlogos/soccer/500/89.png")

        val finalRound = RadialRound(
            roundIndex = if (totalTeams == 8) 2 else 3,
            name = "Final",
            matches = listOf(
                RadialMatch(
                    id = "final_0",
                    matchIndex = 0,
                    teamA = null,
                    teamB = null,
                    winnerTeamId = null,
                    statusText = null
                )
            )
        )

        val semiRound = RadialRound(
            roundIndex = if (totalTeams == 8) 1 else 2,
            name = "Semifinales",
            matches = listOf(
                RadialMatch(
                    id = "semi_0",
                    matchIndex = 0,
                    teamA = rma,
                    teamB = bvb,
                    winnerTeamId = null,
                    statusText = null
                ),
                RadialMatch(
                    id = "semi_1",
                    matchIndex = 1,
                    teamA = ars,
                    teamB = mci,
                    winnerTeamId = null,
                    statusText = null
                )
            )
        )

        val qfRound = RadialRound(
            roundIndex = if (totalTeams == 8) 0 else 1,
            name = "Cuartos de Final",
            matches = listOf(
                RadialMatch("qf_0", 0, rma.copy(score = "2", isWinner = true), bay.copy(score = "1", isWinner = false), "86", "2-1"),
                RadialMatch("qf_1", 1, bvb.copy(score = "3", isWinner = true), psg.copy(score = "2", isWinner = false), "124", "3-2"),
                RadialMatch("qf_2", 2, ars.copy(score = "2", isWinner = true), bar.copy(score = "0", isWinner = false), "359", "2-0"),
                RadialMatch("qf_3", 3, mci.copy(score = "3", isWinner = true), atm.copy(score = "1", isWinner = false), "382", "3-1")
            )
        )

        val thirdPlaceMatch = RadialMatch(
            id = "third_0",
            matchIndex = 0,
            teamA = null,
            teamB = null,
            winnerTeamId = null,
            statusText = null
        )

        return if (totalTeams == 8) {
            RadialBracketTree(
                totalTeams = 8,
                rounds = listOf(qfRound, semiRound, finalRound),
                champion = null,
                thirdPlaceMatch = thirdPlaceMatch
            )
        } else {
            val r16Round = RadialRound(
                roundIndex = 0,
                name = "Octavos de Final",
                matches = listOf(
                    RadialMatch("r16_0", 0, rma.copy(score = "3", isWinner = true), juv.copy(score = "1", isWinner = false), "86", "3-1"),
                    RadialMatch("r16_1", 1, bay.copy(score = "2", isWinner = true), mil.copy(score = "0", isWinner = false), "132", "2-0"),
                    RadialMatch("r16_2", 2, bvb.copy(score = "4", isWinner = true), scp.copy(score = "2", isWinner = false), "124", "4-2"),
                    RadialMatch("r16_3", 3, psg.copy(score = "2", isWinner = true), b04.copy(score = "1", isWinner = false), "160", "2-1"),
                    RadialMatch("r16_4", 4, ars.copy(score = "3", isWinner = true), nap.copy(score = "1", isWinner = false), "359", "3-1"),
                    RadialMatch("r16_5", 5, bar.copy(score = "2", isWinner = true), che.copy(score = "1", isWinner = false), "83", "2-1"),
                    RadialMatch("r16_6", 6, mci.copy(score = "4", isWinner = true), int.copy(score = "1", isWinner = false), "382", "4-1"),
                    RadialMatch("r16_7", 7, atm.copy(score = "2", isWinner = true), liv.copy(score = "1", isWinner = false), "1068", "2-1")
                )
            )

            RadialBracketTree(
                totalTeams = 16,
                rounds = listOf(r16Round, qfRound, semiRound, finalRound),
                champion = null,
                thirdPlaceMatch = thirdPlaceMatch
            )
        }
    }

    /**
     * Provee el arbol de eliminatorias completamente vacio al inicio del torneo.
     * Todas las casillas, el campeon y el tercer lugar permanecen sin equipos en espera de clasificados.
     *
     * @param totalTeams Numero de participantes en la llave (16 por omision).
     */
    fun getRadialBracketTreeEmpty(totalTeams: Int = 16): RadialBracketTree {
        return RadialBracketBuilder.createEmptyTree(totalTeams = totalTeams, hasThirdPlace = true)
    }
}

