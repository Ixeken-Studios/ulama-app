package com.ixeken.ulama.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadialBracketTreeTest {

    @Test
    fun parentMatchLinkage_8Teams() {
        // En 8 equipos: Ronda 0 tiene 4 cruces, Ronda 1 tiene 2 cruces, Ronda 2 tiene 1 final
        val tree = createMockTree(totalTeams = 8)

        assertEquals(3, tree.totalRounds)

        // Cuartos 0 y 1 deben alimentar Semifinal 0 (0 / 2 = 0, 1 / 2 = 0)
        assertEquals("semi-0", tree.getParentMatch(roundIndex = 0, matchIndex = 0)?.id)
        assertEquals("semi-0", tree.getParentMatch(roundIndex = 0, matchIndex = 1)?.id)

        // Cuartos 2 y 3 deben alimentar Semifinal 1 (2 / 2 = 1, 3 / 2 = 1)
        assertEquals("semi-1", tree.getParentMatch(roundIndex = 0, matchIndex = 2)?.id)
        assertEquals("semi-1", tree.getParentMatch(roundIndex = 0, matchIndex = 3)?.id)

        // Semifinales 0 y 1 deben alimentar Final 0 (0 / 2 = 0, 1 / 2 = 0)
        assertEquals("final-0", tree.getParentMatch(roundIndex = 1, matchIndex = 0)?.id)
        assertEquals("final-0", tree.getParentMatch(roundIndex = 1, matchIndex = 1)?.id)

        // La Final no tiene padre en el arbol de rondas porque conecta al centro
        assertNull(tree.getParentMatch(roundIndex = 2, matchIndex = 0))
    }

    @Test
    fun parentMatchLinkage_16Teams() {
        // En 16 equipos: 4 rondas (Octavos con 8 cruces, Cuartos con 4, Semis con 2, Final con 1)
        val tree = createMockTree(totalTeams = 16)

        assertEquals(4, tree.totalRounds)

        // Octavos 0 y 1 alimentan Cuartos 0
        assertEquals("qf-0", tree.getParentMatch(roundIndex = 0, matchIndex = 0)?.id)
        assertEquals("qf-0", tree.getParentMatch(roundIndex = 0, matchIndex = 1)?.id)

        // Octavos 6 y 7 alimentan Cuartos 3 (6 / 2 = 3, 7 / 2 = 3)
        assertEquals("qf-3", tree.getParentMatch(roundIndex = 0, matchIndex = 6)?.id)
        assertEquals("qf-3", tree.getParentMatch(roundIndex = 0, matchIndex = 7)?.id)
    }

    @Test
    fun parentMatchLinkage_32Teams() {
        // En 32 equipos: 5 rondas (Dieciseisavos con 16 cruces)
        val tree = createMockTree(totalTeams = 32)

        assertEquals(5, tree.totalRounds)

        // Dieciseisavos 14 y 15 alimentan Octavos 7 (14 / 2 = 7, 15 / 2 = 7)
        assertEquals("r16-7", tree.getParentMatch(roundIndex = 0, matchIndex = 14)?.id)
        assertEquals("r16-7", tree.getParentMatch(roundIndex = 0, matchIndex = 15)?.id)
    }

    @Test
    fun radialGeometry_angularDistributionSymmetry() {
        // Verificacion en ronda con 4 partidos (2 izquierda, 2 derecha)
        val left0 = RadialGeometry.calculateAngleDegrees(matchIndex = 0, totalMatchesInRound = 4)
        val left1 = RadialGeometry.calculateAngleDegrees(matchIndex = 1, totalMatchesInRound = 4)
        val right0 = RadialGeometry.calculateAngleDegrees(matchIndex = 2, totalMatchesInRound = 4)
        val right1 = RadialGeometry.calculateAngleDegrees(matchIndex = 3, totalMatchesInRound = 4)

        // Cuadro izquierdo entre 110 y 250 grados
        assertEquals(110f, left0, 0.01f)
        assertEquals(250f, left1, 0.01f)

        // Cuadro derecho entre -70 y 70 grados
        assertEquals(-70f, right0, 0.01f)
        assertEquals(70f, right1, 0.01f)
    }

    @Test
    fun radialGeometry_radiusFractions() {
        // En 4 rondas: 1.0 (exterior), 0.75, 0.5, 0.25 (final)
        val r0 = RadialGeometry.calculateRadiusFraction(roundIndex = 0, totalRounds = 4)
        val r1 = RadialGeometry.calculateRadiusFraction(roundIndex = 1, totalRounds = 4)
        val r2 = RadialGeometry.calculateRadiusFraction(roundIndex = 2, totalRounds = 4)
        val r3 = RadialGeometry.calculateRadiusFraction(roundIndex = 3, totalRounds = 4)

        assertEquals(1.0f, r0, 0.01f)
        assertEquals(0.75f, r1, 0.01f)
        assertEquals(0.50f, r2, 0.01f)
        assertEquals(0.25f, r3, 0.01f)
    }

    @Test
    fun debugLeagueData_getRadialBracketTree_validation() {
        val tree16 = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTree(16)
        assertEquals(16, tree16.totalTeams)
        assertEquals(4, tree16.totalRounds)
        assertEquals("Octavos de Final", tree16.rounds[0].name)
        assertEquals(8, tree16.rounds[0].matches.size)
        assertEquals("Cuartos de Final", tree16.rounds[1].name)
        assertEquals(4, tree16.rounds[1].matches.size)
        assertEquals("Semifinales", tree16.rounds[2].name)
        assertEquals(2, tree16.rounds[2].matches.size)
        assertEquals("Final", tree16.rounds[3].name)
        assertEquals(1, tree16.rounds[3].matches.size)
        assertNotNull(tree16.champion)
        assertEquals("86", tree16.champion?.id)
        assertNotNull(tree16.thirdPlaceMatch)
        assertEquals("124", tree16.thirdPlaceMatch?.winnerTeamId)
        assertEquals("3-2", tree16.thirdPlaceMatch?.statusText)

        val tree8 = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTree(8)
        assertEquals(8, tree8.totalTeams)
        assertEquals(3, tree8.totalRounds)
        assertEquals("Cuartos de Final", tree8.rounds[0].name)
        assertEquals(4, tree8.rounds[0].matches.size)
        assertNotNull(tree8.thirdPlaceMatch)
    }

    @Test
    fun debugLeagueData_getRadialBracketTree32_validation() {
        val tree32 = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTree(32)
        assertEquals(32, tree32.totalTeams)
        assertEquals(5, tree32.totalRounds)
        assertEquals("Dieciseisavos de Final", tree32.rounds[0].name)
        assertEquals(16, tree32.rounds[0].matches.size)
        assertEquals("Octavos de Final", tree32.rounds[1].name)
        assertEquals(8, tree32.rounds[1].matches.size)
        assertEquals("Cuartos de Final", tree32.rounds[2].name)
        assertEquals(4, tree32.rounds[2].matches.size)
        assertEquals("Semifinales", tree32.rounds[3].name)
        assertEquals(2, tree32.rounds[3].matches.size)
        assertEquals("Final", tree32.rounds[4].name)
        assertEquals(1, tree32.rounds[4].matches.size)
        assertNotNull(tree32.champion)
        assertEquals("86", tree32.champion?.id)
        assertNotNull(tree32.thirdPlaceMatch)
        assertTrue(tree32.hasAnyScheduledMatch)
    }

    @Test
    fun debugLeagueData_getRadialBracketTreeSemis_validation() {
        val treeSemis16 = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTreeSemis(16)
        assertEquals(16, treeSemis16.totalTeams)
        assertEquals(4, treeSemis16.totalRounds)
        assertNull(treeSemis16.champion)
        assertNotNull(treeSemis16.thirdPlaceMatch)
        assertNull(treeSemis16.thirdPlaceMatch?.teamA)
        assertNull(treeSemis16.thirdPlaceMatch?.teamB)

        // Semifinales tienen equipos definidos pero marcador nulo
        val semiRound = treeSemis16.rounds[2]
        assertEquals("Semifinales", semiRound.name)
        assertNotNull(semiRound.matches[0].teamA)
        assertNotNull(semiRound.matches[0].teamB)
        assertNull(semiRound.matches[0].winnerTeamId)

        // Final con equipos por definirse
        val finalRound = treeSemis16.rounds[3]
        assertEquals("Final", finalRound.name)
        assertNull(finalRound.matches[0].teamA)
        assertNull(finalRound.matches[0].teamB)
    }

    @Test
    fun debugLeagueData_getRadialBracketTreeEmpty_validation() {
        val treeEmpty16 = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTreeEmpty(16)
        assertEquals(16, treeEmpty16.totalTeams)
        assertEquals(4, treeEmpty16.totalRounds)
        assertNull(treeEmpty16.champion)
        assertNotNull(treeEmpty16.thirdPlaceMatch)
        assertNull(treeEmpty16.thirdPlaceMatch?.teamA)
        org.junit.Assert.assertFalse(treeEmpty16.hasAnyScheduledMatch)

        // Todos los cruces de todas las rondas permanecen sin equipos asignados (—)
        treeEmpty16.rounds.forEach { round ->
            round.matches.forEach { match ->
                assertNull(match.teamA)
                assertNull(match.teamB)
                assertNull(match.winnerTeamId)
            }
        }
    }

    @Test
    fun hasAnyScheduledMatch_stateValidation() {
        val completedTree = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTree(16)
        val semisTree = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTreeSemis(16)
        val emptyTree = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTreeEmpty(16)

        assertTrue(completedTree.hasAnyScheduledMatch)
        assertTrue(semisTree.hasAnyScheduledMatch)
        org.junit.Assert.assertFalse(emptyTree.hasAnyScheduledMatch)
    }

    @Test
    fun radialBracketBuilder_createEmptyTree() {
        val empty8 = RadialBracketBuilder.createEmptyTree(totalTeams = 8, hasThirdPlace = false)
        assertEquals(8, empty8.totalTeams)
        assertEquals(3, empty8.totalRounds)
        assertEquals("Cuartos de Final", empty8.rounds[0].name)
        assertEquals(4, empty8.rounds[0].matches.size)
        assertEquals("Semifinales", empty8.rounds[1].name)
        assertEquals(2, empty8.rounds[1].matches.size)
        assertEquals("Final", empty8.rounds[2].name)
        assertEquals(1, empty8.rounds[2].matches.size)
        assertNull(empty8.thirdPlaceMatch)
        org.junit.Assert.assertFalse(empty8.hasAnyScheduledMatch)

        val empty16 = RadialBracketBuilder.createEmptyTree(totalTeams = 16, hasThirdPlace = true)
        assertEquals(16, empty16.totalTeams)
        assertEquals(4, empty16.totalRounds)
        assertEquals("Octavos de Final", empty16.rounds[0].name)
        assertEquals(8, empty16.rounds[0].matches.size)
        assertNotNull(empty16.thirdPlaceMatch)
        org.junit.Assert.assertFalse(empty16.hasAnyScheduledMatch)
    }

    @Test
    fun radialBracketBuilder_buildWithEmptyMatches() {
        val treeLigaMx = RadialBracketBuilder.build(emptyList(), League.LIGA_MX)
        assertNotNull(treeLigaMx)
        assertEquals(8, treeLigaMx?.totalTeams)
        org.junit.Assert.assertFalse(treeLigaMx?.hasAnyScheduledMatch == true)

        val treeChampions = RadialBracketBuilder.build(emptyList(), League.CHAMPIONS_LEAGUE)
        assertNotNull(treeChampions)
        assertEquals(16, treeChampions?.totalTeams)
        org.junit.Assert.assertFalse(treeChampions?.hasAnyScheduledMatch == true)

        val treePremier = RadialBracketBuilder.build(emptyList(), League.PREMIER_LEAGUE)
        assertNull(treePremier)
    }

    @Test
    fun radialBracketBuilder_ignoresCompletedKnockoutMatchesFromPreviousSeasonWhenRegularPhaseIsActive() {
        val dummyTeamA = Team("1", "PSG", "PSG", "PSG", null, true, 2, false)
        val dummyTeamB = Team("2", "Arsenal", "ARS", "ARS", null, false, 1, false)
        val dummyTeamC = Team("3", "Bayern", "BAY", "BAY", null, true, 1, false)

        val oldCompletedFinal = Match(
            id = "old_final",
            dateUtc = "2026-05-30T19:00Z",
            homeTeam = dummyTeamA,
            awayTeam = dummyTeamB,
            status = MatchStatus(MatchState.FINISHED, "90'", "Final", isCompleted = true, isHalftime = false),
            venueName = "Munich",
            league = League.CHAMPIONS_LEAGUE,
            seriesTitle = "final"
        )

        val currentLeaguePhaseMatch = Match(
            id = "new_league_match",
            dateUtc = "2026-10-21T19:00Z",
            homeTeam = dummyTeamC,
            awayTeam = dummyTeamB,
            status = MatchStatus(MatchState.FINISHED, "90'", "Final", isCompleted = true, isHalftime = false),
            venueName = "Allianz Arena",
            league = League.CHAMPIONS_LEAGUE,
            seriesTitle = null
        )

        // Cuando la competicion tiene partidos de fase de liga vigentes posteriores a eliminatorias viejas,
        // el constructor ignora las eliminatorias concluidas y retorna el arbol vacio en espera.
        val tree = RadialBracketBuilder.build(listOf(oldCompletedFinal, currentLeaguePhaseMatch), League.CHAMPIONS_LEAGUE)
        assertNotNull(tree)
        assertNull(tree?.champion)
        org.junit.Assert.assertFalse(tree?.hasAnyScheduledMatch == true)
    }

    @Test
    fun match_isKnockout_filteringValidation() {
        val dummyHome = Team("1", "Local", "LOC", "LOC", null, true, 2, false)
        val dummyAway = Team("2", "Visitante", "VIS", "VIS", null, false, 1, false)
        val dummyStatus = MatchStatus(MatchState.SCHEDULED, "00:00", "Programado", false, false)

        val groupMatch = Match(
            id = "m1",
            dateUtc = "2026-10-14T20:00:00Z",
            homeTeam = dummyHome,
            awayTeam = dummyAway,
            status = dummyStatus,
            venueName = null,
            seriesTitle = "Fase de Grupos · J5"
        )
        org.junit.Assert.assertFalse(groupMatch.isKnockout)

        val jornadaMatch = Match(
            id = "m2",
            dateUtc = "2026-10-14T20:00:00Z",
            homeTeam = dummyHome,
            awayTeam = dummyAway,
            status = dummyStatus,
            venueName = null,
            seriesTitle = "Jornada 12"
        )
        org.junit.Assert.assertFalse(jornadaMatch.isKnockout)

        val qfMatch = Match(
            id = "m3",
            dateUtc = "2026-10-14T20:00:00Z",
            homeTeam = dummyHome,
            awayTeam = dummyAway,
            status = dummyStatus,
            venueName = null,
            seriesTitle = "Cuartos de final - Ida"
        )
        assertTrue(qfMatch.isKnockout)

        val aggregateMatch = Match(
            id = "m4",
            dateUtc = "2026-10-14T20:00:00Z",
            homeTeam = dummyHome,
            awayTeam = dummyAway,
            status = dummyStatus,
            venueName = null,
            seriesTitle = null,
            aggregateHomeScore = 3,
            aggregateAwayScore = 1
        )
        assertTrue(aggregateMatch.isKnockout)
    }

    @Test
    fun isAggregateActive_isFalseForFirstLeg() {
        val dummyHome = Team("1", "Team A", "TMA", "TMA", null, isHome = true, score = null, isWinner = false)
        val dummyAway = Team("2", "Team B", "TMB", "TMB", null, isHome = false, score = null, isWinner = false)
        val dummyStatus = MatchStatus(MatchState.SCHEDULED, "0'", "Scheduled", isCompleted = false)

        val firstLeg = Match(
            id = "leg1",
            dateUtc = "2026-11-15T01:00:00Z",
            homeTeam = dummyHome,
            awayTeam = dummyAway,
            status = dummyStatus,
            venueName = null,
            legNumber = 1,
            aggregateHomeScore = 0,
            aggregateAwayScore = 0
        )
        org.junit.Assert.assertFalse(firstLeg.isAggregateActive)
    }

    @Test
    fun sanitizeAggregateScores_whenFirstLegIsScheduled_clearsAggregateFromSecondLeg() {
        val mexico = Team("203", "Mexico", "MEX", "MEX", null, isHome = true, score = null, isWinner = false)
        val jamaica = Team("1038", "Jamaica", "JAM", "JAM", null, isHome = false, score = null, isWinner = false)

        val leg1Scheduled = Match(
            id = "leg1",
            dateUtc = "2026-11-15T01:00:00Z",
            homeTeam = jamaica,
            awayTeam = mexico,
            status = MatchStatus(MatchState.SCHEDULED, "0'", "Scheduled", isCompleted = false),
            venueName = "Independence Park",
            seriesTitle = "Quarterfinals",
            legNumber = 1
        )

        val leg2ScheduledWithPlaceholderAggregate = Match(
            id = "leg2",
            dateUtc = "2026-11-18T02:30:00Z",
            homeTeam = mexico,
            awayTeam = jamaica,
            status = MatchStatus(MatchState.SCHEDULED, "0'", "Scheduled", isCompleted = false),
            venueName = "Estadio BBVA",
            seriesTitle = "Quarterfinals",
            legNumber = 2,
            aggregateHomeScore = 0,
            aggregateAwayScore = 0
        )

        val sanitized = listOf(leg1Scheduled, leg2ScheduledWithPlaceholderAggregate).sanitizeAggregateScores()
        val sanitizedLeg2 = sanitized.first { it.id == "leg2" }

        org.junit.Assert.assertNull(sanitizedLeg2.aggregateHomeScore)
        org.junit.Assert.assertNull(sanitizedLeg2.aggregateAwayScore)
        org.junit.Assert.assertFalse(sanitizedLeg2.isAggregateActive)
    }

    @Test
    fun sanitizeAggregateScores_whenFirstLegIsFinished_keepsAggregateInSecondLeg() {
        val mexico = Team("203", "Mexico", "MEX", "MEX", null, isHome = true, score = 2, isWinner = true)
        val jamaica = Team("1038", "Jamaica", "JAM", "JAM", null, isHome = false, score = 1, isWinner = false)

        val leg1Finished = Match(
            id = "leg1",
            dateUtc = "2026-11-15T01:00:00Z",
            homeTeam = jamaica,
            awayTeam = mexico,
            status = MatchStatus(MatchState.FINISHED, "90'", "FT", isCompleted = true),
            venueName = "Independence Park",
            seriesTitle = "Quarterfinals",
            legNumber = 1
        )

        val leg2Scheduled = Match(
            id = "leg2",
            dateUtc = "2026-11-18T02:30:00Z",
            homeTeam = mexico,
            awayTeam = jamaica,
            status = MatchStatus(MatchState.SCHEDULED, "0'", "Scheduled", isCompleted = false),
            venueName = "Estadio BBVA",
            seriesTitle = "Quarterfinals",
            legNumber = 2,
            aggregateHomeScore = 2,
            aggregateAwayScore = 1
        )

        val sanitized = listOf(leg1Finished, leg2Scheduled).sanitizeAggregateScores()
        val sanitizedLeg2 = sanitized.first { it.id == "leg2" }

        assertEquals(2, sanitizedLeg2.aggregateHomeScore)
        assertEquals(1, sanitizedLeg2.aggregateAwayScore)
        assertTrue(sanitizedLeg2.isAggregateActive)
    }

    @Test
    fun league_knockoutConfiguration() {
        assertTrue(League.LIGA_MX.hasKnockoutBracket)
        assertEquals(8, League.LIGA_MX.knockoutBracketTeams)

        assertTrue(League.CHAMPIONS_LEAGUE.hasKnockoutBracket)
        assertEquals(16, League.CHAMPIONS_LEAGUE.knockoutBracketTeams)

        assertTrue(League.WORLD_CUP.hasKnockoutBracket)
        assertEquals(32, League.WORLD_CUP.knockoutBracketTeams)
        assertTrue(League.WORLD_CUP.hasThirdPlaceMatch)

        org.junit.Assert.assertFalse(League.PREMIER_LEAGUE.hasKnockoutBracket)
        assertEquals(0, League.PREMIER_LEAGUE.knockoutBracketTeams)
    }

    @Test
    fun normalizeRoundIndex_correctlyDistinguishesAllRounds() {
        // 32 equipos (Copa del Mundo)
        assertEquals(0, RadialBracketBuilder.normalizeRoundIndex("Dieciseisavos", 32))
        assertEquals(0, RadialBracketBuilder.normalizeRoundIndex("round-of-32", 32))
        assertEquals(1, RadialBracketBuilder.normalizeRoundIndex("Octavos de final", 32))
        assertEquals(1, RadialBracketBuilder.normalizeRoundIndex("round-of-16", 32))
        assertEquals(2, RadialBracketBuilder.normalizeRoundIndex("Cuartos de final", 32))
        assertEquals(2, RadialBracketBuilder.normalizeRoundIndex("quarterfinals", 32))
        assertEquals(3, RadialBracketBuilder.normalizeRoundIndex("Semifinales", 32))
        assertEquals(3, RadialBracketBuilder.normalizeRoundIndex("semifinals", 32))
        assertEquals(4, RadialBracketBuilder.normalizeRoundIndex("Final", 32))
        assertEquals(4, RadialBracketBuilder.normalizeRoundIndex("final", 32))
        assertEquals(-1, RadialBracketBuilder.normalizeRoundIndex("3rd-place-match", 32))

        // 16 equipos (Champions League, Copa Africa)
        assertEquals(0, RadialBracketBuilder.normalizeRoundIndex("Octavos de final", 16))
        assertEquals(0, RadialBracketBuilder.normalizeRoundIndex("round-of-16", 16))
        assertEquals(1, RadialBracketBuilder.normalizeRoundIndex("Cuartos de final", 16))
        assertEquals(1, RadialBracketBuilder.normalizeRoundIndex("quarterfinals", 16))
        assertEquals(2, RadialBracketBuilder.normalizeRoundIndex("Semifinales", 16))
        assertEquals(2, RadialBracketBuilder.normalizeRoundIndex("semifinals", 16))
        assertEquals(3, RadialBracketBuilder.normalizeRoundIndex("Final", 16))
        assertEquals(3, RadialBracketBuilder.normalizeRoundIndex("final", 16))
        assertEquals(-1, RadialBracketBuilder.normalizeRoundIndex("3rd-place-match", 16))
        assertEquals(-1, RadialBracketBuilder.normalizeRoundIndex("3er Lugar", 16))
        assertEquals(-1, RadialBracketBuilder.normalizeRoundIndex("Tercer Puesto", 16))

        // 8 equipos (Liga MX, Nations League, Copa Oro)
        assertEquals(0, RadialBracketBuilder.normalizeRoundIndex("Cuartos de final", 8))
        assertEquals(1, RadialBracketBuilder.normalizeRoundIndex("Semifinales", 8))
        assertEquals(2, RadialBracketBuilder.normalizeRoundIndex("Final", 8))
    }

    @Test
    fun radialBracketBuilder_buildWorldCupKnockout_populatesRoundsAndChampion() {
        val spain = Team("1", "Spain", "Spain", "ESP", null, true, 1, true)
        val argentina = Team("2", "Argentina", "Argentina", "ARG", null, false, 0, false)
        val france = Team("3", "France", "France", "FRA", null, true, 2, false)
        val england = Team("4", "England", "England", "ENG", null, false, 0, false)
        val finishedStatus = MatchStatus(MatchState.FINISHED, "FT", "Finalizado", false, false)

        val semi1 = Match(
            id = "semi1",
            dateUtc = "2026-07-14T19:00:00Z",
            homeTeam = spain,
            awayTeam = france,
            status = finishedStatus,
            venueName = null,
            seriesTitle = "semifinals"
        )
        val semi2 = Match(
            id = "semi2",
            dateUtc = "2026-07-15T19:00:00Z",
            homeTeam = argentina,
            awayTeam = england,
            status = finishedStatus,
            venueName = null,
            seriesTitle = "semifinals"
        )
        val thirdPlace = Match(
            id = "third",
            dateUtc = "2026-07-18T19:00:00Z",
            homeTeam = france,
            awayTeam = england,
            status = finishedStatus,
            venueName = null,
            seriesTitle = "3rd-place-match"
        )
        val finalMatch = Match(
            id = "final",
            dateUtc = "2026-07-19T19:00:00Z",
            homeTeam = spain,
            awayTeam = argentina,
            status = finishedStatus,
            venueName = null,
            seriesTitle = "final"
        )

        val tree = RadialBracketBuilder.build(
            listOf(semi1, semi2, thirdPlace, finalMatch),
            League.WORLD_CUP
        )

        assertNotNull(tree)
        assertEquals(32, tree?.totalTeams)
        assertEquals(5, tree?.totalRounds)

        val semiRound = tree?.rounds?.get(3)
        assertEquals("Semifinales", semiRound?.name)
        assertNotNull(semiRound?.matches?.firstOrNull { it.teamA?.name == "Spain" || it.teamB?.name == "Spain" })

        val finalRound = tree?.rounds?.get(4)
        assertEquals("Final", finalRound?.name)
        val finalRadial = finalRound?.matches?.first()
        assertEquals("Spain", finalRadial?.teamA?.name)
        assertEquals("Argentina", finalRadial?.teamB?.name)

        assertEquals("1", tree?.champion?.id)
        assertEquals("Spain", tree?.champion?.name)
        assertNotNull(tree?.thirdPlaceMatch)
        assertEquals("France", tree?.thirdPlaceMatch?.teamA?.name)
    }

    @Test
    fun radialBracketBuilder_treeLineageIntegrity_worldCup2026() {
        val finishedStatus = MatchStatus(MatchState.FINISHED, "FT", "Finalizado", false, false)

        fun createTeam(id: String, name: String) = Team(id, name, name, name.take(3).uppercase(), null, false, 0, false)

        val germany = createTeam("1", "Germany")
        val paraguay = createTeam("2", "Paraguay")
        val france = createTeam("3", "France")
        val sweden = createTeam("4", "Sweden")
        val southAfrica = createTeam("5", "South Africa")
        val canada = createTeam("6", "Canada")
        val netherlands = createTeam("7", "Netherlands")
        val morocco = createTeam("8", "Morocco")
        val portugal = createTeam("9", "Portugal")
        val croatia = createTeam("10", "Croatia")
        val spain = createTeam("11", "Spain")
        val austria = createTeam("12", "Austria")
        val usa = createTeam("13", "United States")
        val bosnia = createTeam("14", "Bosnia")
        val belgium = createTeam("15", "Belgium")
        val senegal = createTeam("16", "Senegal")

        val brazil = createTeam("17", "Brazil")
        val japan = createTeam("18", "Japan")
        val ivoryCoast = createTeam("19", "Ivory Coast")
        val norway = createTeam("20", "Norway")
        val mexico = createTeam("21", "Mexico")
        val ecuador = createTeam("22", "Ecuador")
        val england = createTeam("23", "England")
        val congo = createTeam("24", "Congo DR")
        val argentina = createTeam("25", "Argentina")
        val capeVerde = createTeam("26", "Cape Verde")
        val australia = createTeam("27", "Australia")
        val egypt = createTeam("28", "Egypt")
        val switzerland = createTeam("29", "Switzerland")
        val algeria = createTeam("30", "Algeria")
        val colombia = createTeam("31", "Colombia")
        val ghana = createTeam("32", "Ghana")

        fun match(id: String, round: String, home: Team, away: Team, date: String) = Match(
            id = id,
            dateUtc = date,
            homeTeam = home,
            awayTeam = away,
            status = finishedStatus,
            venueName = null,
            seriesTitle = round
        )

        // Partidos con fechas intencionalmente intercaladas para simular el calendario real
        val r32Matches = listOf(
            match("m32_1", "round-of-32", southAfrica, canada, "2026-06-28T19:00:00Z"),
            match("m32_2", "round-of-32", brazil, japan, "2026-06-29T17:00:00Z"),
            match("m32_3", "round-of-32", germany, paraguay, "2026-06-29T20:30:00Z"),
            match("m32_4", "round-of-32", netherlands, morocco, "2026-06-30T01:00:00Z"),
            match("m32_5", "round-of-32", ivoryCoast, norway, "2026-06-30T17:00:00Z"),
            match("m32_6", "round-of-32", france, sweden, "2026-06-30T21:00:00Z"),
            match("m32_7", "round-of-32", mexico, ecuador, "2026-07-01T02:00:00Z"),
            match("m32_8", "round-of-32", england, congo, "2026-07-01T16:00:00Z"),
            match("m32_9", "round-of-32", belgium, senegal, "2026-07-01T20:00:00Z"),
            match("m32_10", "round-of-32", usa, bosnia, "2026-07-02T00:00:00Z"),
            match("m32_11", "round-of-32", spain, austria, "2026-07-02T19:00:00Z"),
            match("m32_12", "round-of-32", portugal, croatia, "2026-07-02T23:00:00Z"),
            match("m32_13", "round-of-32", switzerland, algeria, "2026-07-03T03:00:00Z"),
            match("m32_14", "round-of-32", australia, egypt, "2026-07-03T18:00:00Z"),
            match("m32_15", "round-of-32", argentina, capeVerde, "2026-07-03T22:00:00Z"),
            match("m32_16", "round-of-32", colombia, ghana, "2026-07-04T01:30:00Z")
        )

        val r16Matches = listOf(
            match("m16_1", "round-of-16", canada, morocco, "2026-07-04T17:00:00Z"),
            match("m16_2", "round-of-16", paraguay, france, "2026-07-04T21:00:00Z"),
            match("m16_3", "round-of-16", brazil, norway, "2026-07-05T20:00:00Z"),
            match("m16_4", "round-of-16", mexico, england, "2026-07-06T01:00:00Z"),
            match("m16_5", "round-of-16", portugal, spain, "2026-07-06T19:00:00Z"),
            match("m16_6", "round-of-16", usa, belgium, "2026-07-07T00:00:00Z"),
            match("m16_7", "round-of-16", argentina, egypt, "2026-07-07T16:00:00Z"),
            match("m16_8", "round-of-16", switzerland, colombia, "2026-07-07T20:00:00Z")
        )

        val qfMatches = listOf(
            match("mqf_1", "quarterfinals", france, morocco, "2026-07-09T20:00:00Z"),
            match("mqf_2", "quarterfinals", spain, belgium, "2026-07-10T19:00:00Z"),
            match("mqf_3", "quarterfinals", norway, england, "2026-07-11T21:00:00Z"),
            match("mqf_4", "quarterfinals", argentina, switzerland, "2026-07-12T01:00:00Z")
        )

        val semiMatches = listOf(
            match("msemi_1", "semifinals", france, spain, "2026-07-14T19:00:00Z"),
            match("msemi_2", "semifinals", england, argentina, "2026-07-15T19:00:00Z")
        )

        val finalMatch = match("mfinal", "final", spain, argentina, "2026-07-19T19:00:00Z")

        val allMatches = r32Matches + r16Matches + qfMatches + semiMatches + finalMatch
        val tree = RadialBracketBuilder.build(allMatches, League.WORLD_CUP)

        assertNotNull(tree)
        assertEquals(32, tree?.totalTeams)
        assertEquals(5, tree?.totalRounds)

        // 1. Verificacion de Semifinales (Ala Izquierda: semi 0, Ala Derecha: semi 1)
        val semi0 = tree?.rounds?.get(3)?.matches?.get(0)
        val semi1 = tree?.rounds?.get(3)?.matches?.get(1)
        assertTrue(semi0?.containsTeam("11") == true) // Spain en semi 0 (Ala Izquierda)
        assertTrue(semi0?.containsTeam("3") == true)  // France en semi 0 (Ala Izquierda)
        assertTrue(semi1?.containsTeam("25") == true) // Argentina en semi 1 (Ala Derecha)
        assertTrue(semi1?.containsTeam("23") == true) // England en semi 1 (Ala Derecha)

        // 2. Verificacion de Cuartos de Final
        val qfRound = tree?.rounds?.get(2)?.matches.orEmpty()
        assertTrue(qfRound[0].containsTeam("3") && qfRound[0].containsTeam("8"))   // France vs Morocco (Ala Izquierda)
        assertTrue(qfRound[1].containsTeam("11") && qfRound[1].containsTeam("15")) // Spain vs Belgium (Ala Izquierda)
        assertTrue(qfRound[2].containsTeam("20") && qfRound[2].containsTeam("23")) // Norway vs England (Ala Derecha)
        assertTrue(qfRound[3].containsTeam("25") && qfRound[3].containsTeam("29")) // Argentina vs Switzerland (Ala Derecha)

        // 3. Verificacion de Octavos de Final
        val r16Round = tree?.rounds?.get(1)?.matches.orEmpty()
        assertTrue(r16Round[0].containsTeam("2") && r16Round[0].containsTeam("3"))   // Paraguay vs France
        assertTrue(r16Round[1].containsTeam("6") && r16Round[1].containsTeam("8"))   // Canada vs Morocco
        assertTrue(r16Round[2].containsTeam("9") && r16Round[2].containsTeam("11"))  // Portugal vs Spain
        assertTrue(r16Round[3].containsTeam("13") && r16Round[3].containsTeam("15")) // USA vs Belgium
        assertTrue(r16Round[4].containsTeam("17") && r16Round[4].containsTeam("20")) // Brazil vs Norway
        assertTrue(r16Round[5].containsTeam("21") && r16Round[5].containsTeam("23")) // Mexico vs England
        assertTrue(r16Round[6].containsTeam("25") && r16Round[6].containsTeam("28")) // Argentina vs Egypt
        assertTrue(r16Round[7].containsTeam("29") && r16Round[7].containsTeam("31")) // Switzerland vs Colombia

        // 4. Verificacion de Dieciseisavos de Final
        val r32Round = tree?.rounds?.get(0)?.matches.orEmpty()
        assertTrue(r32Round[0].containsTeam("1") && r32Round[0].containsTeam("2"))   // Germany vs Paraguay
        assertTrue(r32Round[1].containsTeam("3") && r32Round[1].containsTeam("4"))   // France vs Sweden
        assertTrue(r32Round[2].containsTeam("5") && r32Round[2].containsTeam("6"))   // South Africa vs Canada
        assertTrue(r32Round[3].containsTeam("7") && r32Round[3].containsTeam("8"))   // Netherlands vs Morocco
        assertTrue(r32Round[4].containsTeam("9") && r32Round[4].containsTeam("10"))  // Portugal vs Croatia
        assertTrue(r32Round[5].containsTeam("11") && r32Round[5].containsTeam("12")) // Spain vs Austria
        assertTrue(r32Round[6].containsTeam("13") && r32Round[6].containsTeam("14")) // USA vs Bosnia
        assertTrue(r32Round[7].containsTeam("15") && r32Round[7].containsTeam("16")) // Belgium vs Senegal

        // Ala Derecha en R32 (indices 8 a 15)
        assertTrue(r32Round[8].containsTeam("17") && r32Round[8].containsTeam("18"))  // Brazil vs Japan
        assertTrue(r32Round[9].containsTeam("19") && r32Round[9].containsTeam("20"))  // Ivory Coast vs Norway
        assertTrue(r32Round[10].containsTeam("21") && r32Round[10].containsTeam("22")) // Mexico vs Ecuador
        assertTrue(r32Round[11].containsTeam("23") && r32Round[11].containsTeam("24")) // England vs Congo DR
        assertTrue(r32Round[12].containsTeam("25") && r32Round[12].containsTeam("26")) // Argentina vs Cape Verde
        assertTrue(r32Round[13].containsTeam("27") && r32Round[13].containsTeam("28")) // Australia vs Egypt
        assertTrue(r32Round[14].containsTeam("29") && r32Round[14].containsTeam("30")) // Switzerland vs Algeria
        assertTrue(r32Round[15].containsTeam("31") && r32Round[15].containsTeam("32")) // Colombia vs Ghana

        // 5. Linaje estricto: Todo cruce padre en ronda r tiene sus dos alimentadores en 2k y 2k+1 en ronda r-1
        for (r in 1 until 5) {
            val parentRoundMatches = tree?.rounds?.get(r)?.matches.orEmpty()
            val childRoundMatches = tree?.rounds?.get(r - 1)?.matches.orEmpty()
            parentRoundMatches.forEachIndexed { pIdx, parentMatch ->
                val f0 = 2 * pIdx
                val f1 = (2 * pIdx) + 1
                val child0 = childRoundMatches[f0]
                val child1 = childRoundMatches[f1]

                val parentTeamA = parentMatch.teamA?.id
                val parentTeamB = parentMatch.teamB?.id

                assertTrue(
                    "Parent $pIdx in round $r teamA ($parentTeamA) must be in child $f0",
                    child0.containsTeam(parentTeamA.orEmpty())
                )
                assertTrue(
                    "Parent $pIdx in round $r teamB ($parentTeamB) must be in child $f1",
                    child1.containsTeam(parentTeamB.orEmpty())
                )
            }
        }

        // 6. Validacion de constancia de alas: England siempre en Ala Derecha, Belgium siempre en Ala Izquierda
        val r32EngIdx = r32Round.indexOfFirst { it.containsTeam("23") }
        val r16EngIdx = r16Round.indexOfFirst { it.containsTeam("23") }
        val qfEngIdx = qfRound.indexOfFirst { it.containsTeam("23") }
        assertTrue("England must be in Right Wing in R32", r32EngIdx >= 8)
        assertTrue("England must be in Right Wing in R16", r16EngIdx >= 4)
        assertTrue("England must be in Right Wing in QF", qfEngIdx >= 2)

        val r32BelIdx = r32Round.indexOfFirst { it.containsTeam("15") }
        val r16BelIdx = r16Round.indexOfFirst { it.containsTeam("15") }
        val qfBelIdx = qfRound.indexOfFirst { it.containsTeam("15") }
        assertTrue("Belgium must be in Left Wing in R32", r32BelIdx < 8)
        assertTrue("Belgium must be in Left Wing in R16", r16BelIdx < 4)
        assertTrue("Belgium must be in Left Wing in QF", qfBelIdx < 2)
    }

    @Test
    fun radialBracketBuilder_ignoresGoldCupPrelimsAndRelegationPlayoffs_keepsOnlyQuarterfinals() {
        fun team(id: String, name: String) = Team(id, name, name, name.take(3).uppercase(), "https://logo/$id.png", false, 1, false)
        val finished = MatchStatus(MatchState.FINISHED, "FT", "FT", isCompleted = true, isHalftime = false)
        val scheduled = MatchStatus(MatchState.SCHEDULED, "", "TBD", isCompleted = false, isHalftime = false)

        val prelim1 = Match("p1", "2026-11-12T22:00Z", team("a1", "Aruba"), team("a2", "Martinique"), finished, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = "Gold Cup Prelims", legNumber = 1)
        val prelim2 = Match("p2", "2026-11-12T23:00Z", team("a3", "Antigua"), team("a4", "Trinidad"), finished, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = "Gold Cup Prelims", legNumber = 1)
        val relPlayoff = Match("rp1", "2026-11-12T20:00Z", team("r1", "Malta"), team("r2", "Luxembourg"), finished, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = "Relegation Playoffs", legNumber = 1)
        val leagueBGroupMatchLaterSameWeek = Match("lb1", "2026-11-18T23:00Z", team("b1", "Guyana"), team("b2", "Puerto Rico"), finished, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = null)

        val qf1 = Match("qf1", "2026-11-13T00:30Z", team("c1", "Curaçao"), team("c2", "Panama"), finished, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = "Quarterfinals", legNumber = 1)
        val qf2 = Match("qf2", "2026-11-14T02:00Z", team("c3", "Guatemala"), team("c4", "Canada"), finished, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = "Quarterfinals", legNumber = 1)
        val qf3 = Match("qf3", "2026-11-14T21:30Z", team("c5", "Haiti"), team("c6", "United States"), scheduled, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = "Quarterfinals", legNumber = 1)
        val qf4 = Match("qf4", "2026-11-15T01:00Z", team("c7", "Jamaica"), team("c8", "Mexico"), scheduled, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = "Quarterfinals", legNumber = 1)

        val tree = RadialBracketBuilder.build(
            listOf(prelim1, prelim2, relPlayoff, leagueBGroupMatchLaterSameWeek, qf1, qf2, qf3, qf4),
            League.CONCACAF_NATIONS_LEAGUE
        )

        assertNotNull(tree)
        assertTrue(League.CONCACAF_NATIONS_LEAGUE.hasThirdPlaceMatch)
        assertNotNull(tree?.thirdPlaceMatch)
        val qfMatches = tree?.rounds?.get(0)?.matches.orEmpty()
        assertEquals(4, qfMatches.size)
        org.junit.Assert.assertFalse(qfMatches.any { it.containsTeam("a1") || it.containsTeam("a3") || it.containsTeam("r1") })
        assertTrue(qfMatches.any { it.containsTeam("c2") }) // Panama
        assertTrue(qfMatches.any { it.containsTeam("c4") }) // Canada
        assertTrue(qfMatches.any { it.containsTeam("c6") }) // United States
        assertTrue(qfMatches.any { it.containsTeam("c8") }) // Mexico
    }

    @Test
    fun radialBracketBuilder_treatsPlaceholderTeamsAsEmptySlots() {
        val scheduled = MatchStatus(MatchState.SCHEDULED, "", "TBD", isCompleted = false, isHalftime = false)
        val placeholder1C = Team("5927", "Group C Winner", "1C", "1C", "", true, null, false)
        val placeholder3A = Team("16304", "3rd Place Group A", "3 A", "3ABF", "", false, null, false)
        val placeholderR16W1 = Team("19282", "Round of 16 1 Winner", "RD16 W1", "RD16 W1", "", true, null, false)
        val placeholderR16W3 = Team("19284", "Round of 16 3 Winner", "RD16 W3", "RD16 W3", "", false, null, false)

        assertTrue(placeholder1C.isPlaceholder)
        assertTrue(placeholder3A.isPlaceholder)
        assertTrue(placeholderR16W1.isPlaceholder)

        val r16PlaceholderMatch = Match("afc_r16", "2027-01-25T20:00Z", placeholder1C, placeholder3A, scheduled, null, League.ASIAN_CUP, seriesTitle = "round-of-16")
        val qfPlaceholderMatch = Match("afc_qf", "2027-01-28T20:00Z", placeholderR16W1, placeholderR16W3, scheduled, null, League.ASIAN_CUP, seriesTitle = "quarterfinals")

        val tree = RadialBracketBuilder.build(listOf(r16PlaceholderMatch, qfPlaceholderMatch), League.ASIAN_CUP)
        assertNotNull(tree)
        org.junit.Assert.assertFalse(tree?.hasAnyScheduledMatch == true)
        tree?.rounds?.forEach { round ->
            round.matches.forEach { m ->
                assertNull(m.teamA)
                assertNull(m.teamB)
            }
        }
    }

    @Test
    fun match_isKnockout_recognizesPrefixedSeasonalKnockoutTitles_whileExcludingRegularSeason() {
        val home = Team("223", "Toluca", "Toluca", "TOL", "https://a.espncdn.com/i/teamlogos/soccer/500/223.png", true, 0, false)
        val away = Team("216", "Atlas", "Atlas", "ATS", "https://a.espncdn.com/i/teamlogos/soccer/500/216.png", false, 0, false)
        val scheduled = MatchStatus(MatchState.SCHEDULED, "0'", "Scheduled", isCompleted = false)

        val aperturaQf = Match("qf1", "2026-11-26T01:00Z", home, away, scheduled, null, League.LIGA_MX, seriesTitle = "Apertura - Quarterfinals", legNumber = 1)
        val clausuraSemi = Match("sf1", "2026-05-14T01:00Z", home, away, scheduled, null, League.LIGA_MX, seriesTitle = "Clausura - Semifinals", legNumber = 1)
        val clausuraFinal = Match("f1", "2026-05-24T01:00Z", home, away, scheduled, null, League.LIGA_MX, seriesTitle = "Clausura - Finals", legNumber = 2)
        val regularApertura = Match("r1", "2026-11-21T01:00Z", home, away, scheduled, null, League.LIGA_MX, seriesTitle = "torneo-apertura")
        val regularClausura = Match("r2", "2026-04-21T01:00Z", home, away, scheduled, null, League.LIGA_MX, seriesTitle = "Clausura")

        assertTrue(aperturaQf.isKnockout)
        assertTrue(clausuraSemi.isKnockout)
        assertTrue(clausuraFinal.isKnockout)
        org.junit.Assert.assertFalse(regularApertura.isKnockout)
        org.junit.Assert.assertFalse(regularClausura.isKnockout)
    }

    @Test
    fun radialBracketBuilder_scheduledMatchesWithEspnZeroScores_populateCrestsWithoutZeroScoresOrWinner() {
        fun team(id: String, name: String) = Team(id, name, name, name.take(3).uppercase(), "https://a.espncdn.com/i/teamlogos/soccer/500/$id.png", false, score = 0, isWinner = false)
        val scheduled = MatchStatus(MatchState.SCHEDULED, "0'", "Scheduled", isCompleted = false)

        val qf1 = Match("qf1", "2026-11-26T01:00Z", team("223", "Toluca"), team("216", "Atlas"), scheduled, null, League.LIGA_MX, seriesTitle = "Apertura - Quarterfinals", legNumber = 1)
        val qf2 = Match("qf2", "2026-11-26T03:10Z", team("220", "Monterrey"), team("227", "América"), scheduled, null, League.LIGA_MX, seriesTitle = "Apertura - Quarterfinals", legNumber = 1)
        val qf3 = Match("qf3", "2026-11-27T01:00Z", team("234", "Pachuca"), team("219", "Guadalajara"), scheduled, null, League.LIGA_MX, seriesTitle = "Apertura - Quarterfinals", legNumber = 1)
        val qf4 = Match("qf4", "2026-11-27T03:10Z", team("228", "León"), team("218", "Cruz Azul"), scheduled, null, League.LIGA_MX, seriesTitle = "Apertura - Quarterfinals", legNumber = 1)

        val tree = RadialBracketBuilder.build(listOf(qf1, qf2, qf3, qf4), League.LIGA_MX)
        assertNotNull(tree)
        assertTrue(tree!!.hasAnyScheduledMatch)
        assertNull(tree.champion)

        val qfRound = tree.rounds[0].matches
        assertEquals(4, qfRound.size)
        qfRound.forEach { slot ->
            assertNotNull(slot.teamA)
            assertNotNull(slot.teamB)
            assertNull(slot.teamA?.score)
            assertNull(slot.teamB?.score)
            assertNull(slot.winnerTeamId)
            assertTrue(slot.teamA!!.getCrestUrl(isDark = true).contains("/500-dark/"))
        }
    }

    @Test
    fun radialBracketBuilder_firstLegFinishedAndSecondLegScheduled_doesNotDeclareWinnerYet() {
        val tolucaLeg1 = Team("223", "Toluca", "Toluca", "TOL", null, true, score = 2, isWinner = true)
        val atlasLeg1 = Team("216", "Atlas", "Atlas", "ATS", null, false, score = 1, isWinner = false)
        val atlasLeg2 = Team("216", "Atlas", "Atlas", "ATS", null, true, score = 0, isWinner = false)
        val tolucaLeg2 = Team("223", "Toluca", "Toluca", "TOL", null, false, score = 0, isWinner = false)

        val leg1Finished = Match(
            id = "f_leg1",
            dateUtc = "2026-12-10T02:00Z",
            homeTeam = tolucaLeg1,
            awayTeam = atlasLeg1,
            status = MatchStatus(MatchState.FINISHED, "FT", "FT", isCompleted = true),
            venueName = null,
            league = League.LIGA_MX,
            seriesTitle = "Apertura - Finals",
            legNumber = 1,
            isSeriesCompleted = false
        )
        val leg2Scheduled = Match(
            id = "f_leg2",
            dateUtc = "2026-12-13T02:00Z",
            homeTeam = atlasLeg2,
            awayTeam = tolucaLeg2,
            status = MatchStatus(MatchState.SCHEDULED, "0'", "Scheduled", isCompleted = false),
            venueName = null,
            league = League.LIGA_MX,
            seriesTitle = "Apertura - Finals",
            legNumber = 2,
            aggregateHomeScore = 1,
            aggregateAwayScore = 2,
            isSeriesCompleted = false
        )

        val tree = RadialBracketBuilder.build(listOf(leg1Finished, leg2Scheduled), League.LIGA_MX)
        assertNotNull(tree)
        val finalSlot = tree!!.rounds.last().matches.first()
        assertEquals("2", finalSlot.teamA?.score)
        assertEquals("1", finalSlot.teamB?.score)
        assertNull(finalSlot.winnerTeamId)
        assertNull(tree.champion)
    }

    @Test
    fun radialBracketBuilder_tiedAggregate_usesExplicitSeriesWinnerTeamId() {
        val mexicoLeg1 = Team("203", "Mexico", "MEX", "MEX", null, false, score = 2, isWinner = false)
        val jamaicaLeg1 = Team("1038", "Jamaica", "JAM", "JAM", null, true, score = 3, isWinner = true)
        val mexicoLeg2 = Team("203", "Mexico", "MEX", "MEX", null, true, score = 1, isWinner = true)
        val jamaicaLeg2 = Team("1038", "Jamaica", "JAM", "JAM", null, false, score = 0, isWinner = false)
        val finished = MatchStatus(MatchState.FINISHED, "FT", "FT", isCompleted = true)

        val leg1 = Match("l1", "2026-11-15T01:00Z", jamaicaLeg1, mexicoLeg1, finished, null, League.CONCACAF_NATIONS_LEAGUE, seriesTitle = "Quarterfinals", legNumber = 1)
        val leg2 = Match(
            id = "l2",
            dateUtc = "2026-11-18T02:30Z",
            homeTeam = mexicoLeg2,
            awayTeam = jamaicaLeg2,
            status = finished,
            venueName = null,
            league = League.CONCACAF_NATIONS_LEAGUE,
            seriesTitle = "Quarterfinals",
            legNumber = 2,
            aggregateHomeScore = 3,
            aggregateAwayScore = 3,
            isSeriesCompleted = true,
            seriesWinnerTeamId = "203"
        )

        val tree = RadialBracketBuilder.build(listOf(leg1, leg2), League.CONCACAF_NATIONS_LEAGUE)
        val qfSlot = tree!!.rounds[0].matches.first()
        assertEquals("3", qfSlot.teamA?.score)
        assertEquals("3", qfSlot.teamB?.score)
        assertEquals("203", qfSlot.winnerTeamId)
        assertTrue(qfSlot.teamB?.isWinner == true)
        org.junit.Assert.assertFalse(qfSlot.teamA?.isWinner == true)
    }

    private fun createMockTree(totalTeams: Int): RadialBracketTree {
        val rounds = mutableListOf<RadialRound>()
        var matchesInRound = totalTeams / 2
        var roundIdx = 0

        while (matchesInRound >= 1) {
            val prefix = when (matchesInRound) {
                16 -> "r32"
                8 -> "r16"
                4 -> "qf"
                2 -> "semi"
                else -> "final"
            }
            val matches = (0 until matchesInRound).map { mIdx ->
                RadialMatch(id = "$prefix-$mIdx", matchIndex = mIdx)
            }
            rounds.add(RadialRound(roundIndex = roundIdx, name = prefix, matches = matches))
            matchesInRound /= 2
            roundIdx++
        }

        return RadialBracketTree(totalTeams = totalTeams, rounds = rounds)
    }
}
