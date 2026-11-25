package com.ixeken.ulama.core.model

import androidx.compose.runtime.Immutable

/**
 * Modelo inmutable para un equipo dentro de un cruce eliminatorio radial.
 *
 * @param id Identificador unico del club o seleccion.
 * @param name Nombre oficial completo del equipo.
 * @param shortName Abreviatura o acronimo de 3 caracteres para visualizacion compacta.
 * @param crestUrl Direccion remota o local del escudo vectorial o mapa de bits.
 * @param score Marcador o goles anotados en el partido de la llave.
 * @param isWinner Indica si este equipo avanzo a la siguiente ronda del torneo.
 */
@Immutable
data class RadialTeam(
    val id: String,
    val name: String,
    val shortName: String,
    val crestUrl: String,
    val score: String? = null,
    val isWinner: Boolean = false
) {
    /**
     * Resuelve la URL del escudo adaptada al modo oscuro sustituyendo /500/ por /500-dark/ en ESPN.
     */
    fun getCrestUrl(isDark: Boolean): String {
        if (crestUrl.isBlank()) return crestUrl
        return if (isDark) crestUrl.replace("/500/", "/500-dark/") else crestUrl
    }
}

/**
 * Representa un enfrentamiento o partido individual dentro de una ronda eliminatoria.
 *
 * @param id Identificador unico del partido.
 * @param matchIndex Posicion ordinal del cruce dentro de su ronda actual.
 * @param teamA Primer equipo participante en la llave.
 * @param teamB Segundo equipo participante en la llave.
 * @param winnerTeamId Identificador del equipo que gano la serie.
 * @param statusText Estado del partido como resultado final o penales.
 */
@Immutable
data class RadialMatch(
    val id: String,
    val matchIndex: Int,
    val teamA: RadialTeam? = null,
    val teamB: RadialTeam? = null,
    val winnerTeamId: String? = null,
    val statusText: String? = null
) {
    /**
     * Verifica si un equipo con el identificador provisto compite en este cruce.
     * Permite enlazar de forma determinista la serie previa con la serie sucesora en el arbol eliminatorio.
     */
    fun containsTeam(teamId: String): Boolean = teamA?.id == teamId || teamB?.id == teamId
}

/**
 * Agrupacion de partidos correspondientes a una fase del torneo (ej. Cuartos o Semifinales).
 *
 * @param roundIndex Indice de la ronda donde 0 es la fase exterior inicial y la ultima es la final.
 * @param name Nombre legible de la fase competitiva.
 * @param matches Lista ordenada de enfrentamientos pertenecientes a esta fase.
 */
@Immutable
data class RadialRound(
    val roundIndex: Int,
    val name: String,
    val matches: List<RadialMatch>
)

/**
 * Estructura completa de arbol eliminatorio radial para torneos de 8, 16 o 32 equipos.
 *
 * @param totalTeams Numero total de equipos participantes en el cuadro eliminatorio.
 * @param rounds Fases del torneo ordenadas de exterior a interior.
 * @param champion Equipo coronado como campeon ubicado en el nodo central.
 * @param thirdPlaceMatch Partido opcional por el tercer lugar entre los semifinalistas perdedores.
 */
@Immutable
data class RadialBracketTree(
    val totalTeams: Int,
    val rounds: List<RadialRound>,
    val champion: RadialTeam? = null,
    val thirdPlaceMatch: RadialMatch? = null
) {
    val totalRounds: Int get() = rounds.size

    /**
     * Determina si el arbol contiene al menos un cruce con equipos programados.
     * Si es falso, el cuadro esta completamente vacio y en espera de clasificados.
     */
    val hasAnyScheduledMatch: Boolean
        get() = champion != null || rounds.any { round ->
            round.matches.any { match -> match.teamA != null || match.teamB != null }
        }

    /**
     * Resuelve el partido padre en la ronda subsecuente hacia el centro.
     * En un arbol binario simetrico el cruce padre siempre corresponde a matchIndex dividido entre dos.
     */
    fun getParentMatch(roundIndex: Int, matchIndex: Int): RadialMatch? {
        val nextRound = rounds.getOrNull(roundIndex + 1) ?: return null
        val parentIndex = RadialGeometry.getParentMatchIndex(matchIndex)
        return nextRound.matches.getOrNull(parentIndex)
    }
}

/**
 * Motor matematico universal para calcular la distribucion polar de nodos en cualquier arbol eliminatorio.
 * Soporta de forma determinista llaves de 8, 16 y 32 equipos divididos en dos cuadros simetricos.
 */
object RadialGeometry {

    /**
     * Calcula el angulo polar en grados sexagesimales para un cruce en su ronda respectiva.
     * Los cruces de la primera mitad se ubican en el arco izquierdo y los restantes en el arco derecho.
     */
    fun calculateAngleDegrees(matchIndex: Int, totalMatchesInRound: Int): Float {
        if (totalMatchesInRound <= 1) return 180f
        val halfMatches = totalMatchesInRound / 2
        val isLeftBracket = matchIndex < halfMatches

        return if (isLeftBracket) {
            if (halfMatches == 1) {
                180f
            } else {
                val step = 140f / (halfMatches - 1)
                110f + matchIndex * step
            }
        } else {
            val rightIndex = matchIndex - halfMatches
            if (halfMatches == 1) {
                0f
            } else {
                val step = 140f / (halfMatches - 1)
                -70f + rightIndex * step
            }
        }
    }

    /**
     * Calcula la proporcion radial entre 0.0 y 1.0 donde 1.0 es el borde exterior y el minimo es la antesala central.
     */
    fun calculateRadiusFraction(roundIndex: Int, totalRounds: Int): Float {
        if (totalRounds <= 1) return 0.5f
        val step = 0.75f / (totalRounds - 1)
        return 1.0f - (roundIndex * step)
    }

    /**
     * Obtiene el indice del cruce padre inmediato en la siguiente ronda.
     */
    fun getParentMatchIndex(matchIndex: Int): Int = matchIndex / 2
}

/**
 * Constructor modular de arboles eliminatorios radiales a partir de partidos reales o cuadros vacios.
 */
object RadialBracketBuilder {

    /**
     * Crea un arbol eliminatorio completamente vacio sin ningun equipo sembrado.
     * Todas las casillas exhiben el estado no asignado (—) para previsualizacion de llaves en espera.
     *
     * @param totalTeams Numero total de contendientes en el cuadro (8, 16 o 32).
     * @param hasThirdPlace Indica si se debe reservar el nodo inferior para el partido de 3er lugar.
     */
    fun createEmptyTree(totalTeams: Int, hasThirdPlace: Boolean = false): RadialBracketTree {
        val rounds = mutableListOf<RadialRound>()
        var matchesInRound = totalTeams / 2
        var roundIdx = 0

        while (matchesInRound >= 1) {
            val roundName = when (matchesInRound) {
                1 -> "Final"
                2 -> "Semifinales"
                4 -> "Cuartos de Final"
                8 -> "Octavos de Final"
                16 -> "Dieciseisavos"
                else -> "Ronda $roundIdx"
            }
            val matches = (0 until matchesInRound).map { idx ->
                RadialMatch(
                    id = "empty_${roundIdx}_$idx",
                    matchIndex = idx,
                    teamA = null,
                    teamB = null,
                    winnerTeamId = null,
                    statusText = null
                )
            }
            rounds.add(RadialRound(roundIndex = roundIdx, name = roundName, matches = matches))
            roundIdx++
            matchesInRound /= 2
        }

        val thirdPlace = if (hasThirdPlace) {
            RadialMatch(
                id = "empty_third_0",
                matchIndex = 0,
                teamA = null,
                teamB = null,
                winnerTeamId = null,
                statusText = null
            )
        } else null

        return RadialBracketTree(
            totalTeams = totalTeams,
            rounds = rounds,
            champion = null,
            thirdPlaceMatch = thirdPlace
        )
    }

    /**
     * Construye dinamicamente el arbol eliminatorio consolidando partidos de ida y vuelta.
     * Si no se registran partidos eliminatorios en la lista provista, retorna la estructura vacia en espera.
     *
     * @param matches Coleccion completa de partidos de la competicion.
     * @param league Competicion deportiva activa.
     */
    fun build(matches: List<Match>, league: League): RadialBracketTree? {
        if (!league.hasKnockoutBracket) return null
        val totalTeams = league.knockoutBracketTeams
        if (totalTeams < 2) return null

        val allKnockoutMatches = matches.sanitizeAggregateScores().filter { it.isKnockout }
        if (allKnockoutMatches.isEmpty()) {
            return createEmptyTree(totalTeams, league.hasThirdPlaceMatch)
        }

        // Si existen partidos de fase regular o grupos muy posteriores (mas de 45 dias) a eliminatorias concluidas,
        // dichas eliminatorias corresponden a una edicion previa y no al certamen activo en curso.
        // Se usa un margen de 45 dias porque en torneos multinivel (Concacaf/UEFA Nations League) los grupos de
        // Ligas B y C se disputan en la misma ventana FIFA que los Cuartos de Final de la Liga A.
        val regularPhaseMatches = matches.filter { !it.isKnockout }
        val latestRegularDate = regularPhaseMatches.maxOfOrNull { it.dateUtc }
        val knockoutMatches = if (latestRegularDate != null) {
            allKnockoutMatches.filter { match ->
                !match.isFinished || !isPriorSeasonKnockout(match.dateUtc, latestRegularDate)
            }
        } else {
            allKnockoutMatches
        }

        if (knockoutMatches.isEmpty()) {
            return createEmptyTree(totalTeams, league.hasThirdPlaceMatch)
        }

        val emptyTree = createEmptyTree(totalTeams, league.hasThirdPlaceMatch)
        val populatedRounds = emptyTree.rounds.map { it.copy(matches = it.matches.toMutableList()) }.toMutableList()

        val groupedByRound = knockoutMatches
            .mapNotNull { match ->
                val rIdx = normalizeRoundIndex(match.seriesTitle, totalTeams) ?: return@mapNotNull null
                rIdx to match
            }
            .groupBy({ it.first }, { it.second })

        var resolvedChampion: RadialTeam? = null
        var resolvedThirdPlace: RadialMatch? = emptyTree.thirdPlaceMatch

        val thirdPlaceMatches = groupedByRound[-1]
        if (!thirdPlaceMatches.isNullOrEmpty()) {
            resolvedThirdPlace = consolidateMatch(thirdPlaceMatches, 0, "third_0")
        }

        val rawMatchesByRound = mutableMapOf<Int, List<RadialMatch>>()
        populatedRounds.indices.forEach { roundIdx ->
            val matchesInRound = groupedByRound[roundIdx].orEmpty()
            if (matchesInRound.isNotEmpty()) {
                val sorted = matchesInRound.sortedBy { it.dateUtc }
                val consolidatedList = sorted
                    .groupBy { setOf(it.homeTeam.id, it.awayTeam.id) }
                    .values
                    .mapIndexed { idx, pairMatches ->
                        consolidateMatch(pairMatches, idx, "${populatedRounds[roundIdx].name}_$idx")
                    }
                rawMatchesByRound[roundIdx] = consolidatedList
            }
        }

        val totalRounds = populatedRounds.size
        val slottedRounds = Array(totalRounds) { r ->
            arrayOfNulls<RadialMatch>(populatedRounds[r].matches.size)
        }

        val topRoundIdx = (totalRounds - 1 downTo 0).firstOrNull { rawMatchesByRound[it]?.isNotEmpty() == true }

        if (topRoundIdx != null) {
            val topMatches = rawMatchesByRound[topRoundIdx].orEmpty()
            topMatches.forEachIndexed { idx, match ->
                if (idx < slottedRounds[topRoundIdx].size) {
                    slottedRounds[topRoundIdx][idx] = match
                }
            }

            for (r in topRoundIdx - 1 downTo 0) {
                val unassigned = rawMatchesByRound[r]?.toMutableList() ?: mutableListOf()
                val parentRound = r + 1
                val parentSlots = slottedRounds[parentRound].size

                for (p in 0 until parentSlots) {
                    val parentMatch = slottedRounds[parentRound][p] ?: continue
                    val f0 = 2 * p
                    val f1 = (2 * p) + 1

                    val teamAId = parentMatch.teamA?.id
                    val matchForA = if (teamAId != null) {
                        unassigned.firstOrNull { it.containsTeam(teamAId) }
                    } else null

                    if (matchForA != null && f0 in slottedRounds[r].indices) {
                        slottedRounds[r][f0] = matchForA
                        unassigned.remove(matchForA)
                    }

                    val teamBId = parentMatch.teamB?.id
                    val matchForB = if (teamBId != null) {
                        unassigned.firstOrNull { it.containsTeam(teamBId) }
                    } else null

                    if (matchForB != null && f1 in slottedRounds[r].indices) {
                        slottedRounds[r][f1] = matchForB
                        unassigned.remove(matchForB)
                    }
                }

                for (s in slottedRounds[r].indices) {
                    if (slottedRounds[r][s] == null && unassigned.isNotEmpty()) {
                        slottedRounds[r][s] = unassigned.removeAt(0)
                    }
                }
            }

            for (r in 0 until totalRounds) {
                val roundMatches = populatedRounds[r].matches.toMutableList()
                for (s in roundMatches.indices) {
                    val match = slottedRounds[r][s]
                    if (match != null) {
                        roundMatches[s] = match.copy(matchIndex = s)
                    }
                }
                populatedRounds[r] = populatedRounds[r].copy(matches = roundMatches)
            }

            val finalRoundMatches = populatedRounds.last().matches
            if (finalRoundMatches.isNotEmpty()) {
                val finalMatch = finalRoundMatches.first()
                val winnerId = finalMatch.winnerTeamId
                if (winnerId != null) {
                    resolvedChampion = if (finalMatch.teamA?.id == winnerId) finalMatch.teamA else finalMatch.teamB
                }
            }
        }

        return RadialBracketTree(
            totalTeams = totalTeams,
            rounds = populatedRounds,
            champion = resolvedChampion,
            thirdPlaceMatch = resolvedThirdPlace
        )
    }

    /**
     * Determina si un partido de eliminatoria concluido pertenece a una edicion anterior al comparar
     * su fecha ISO contra la fecha mas reciente de fase regular o de grupos (umbral de 45 dias).
     */
    private fun isPriorSeasonKnockout(knockoutDateUtc: String, latestRegularDateUtc: String): Boolean {
        if (knockoutDateUtc >= latestRegularDateUtc) return false
        return runCatching {
            val koDate = java.time.LocalDate.parse(knockoutDateUtc.take(10))
            val regDate = java.time.LocalDate.parse(latestRegularDateUtc.take(10))
            java.time.temporal.ChronoUnit.DAYS.between(koDate, regDate) > 45L
        }.getOrDefault(true)
    }

    internal fun normalizeRoundIndex(seriesTitle: String?, totalTeams: Int): Int? {
        val title = seriesTitle?.lowercase().orEmpty()
        if (title.isBlank()) return null
        if (title.contains("prelim") ||
            title.contains("play-in") ||
            title.contains("play in") ||
            title.contains("relegation") ||
            title.contains("qualif") ||
            title.contains("repechaje") ||
            title.contains("clasific")
        ) {
            return null
        }

        return when {
            title.contains("tercer") || title.contains("third") || title.contains("3rd") || title.contains("3er") || title.contains("3°") || title.contains("3ro") -> -1
            title.contains("dieciseis") || title.contains("round-of-32") || title.contains("round of 32") -> if (totalTeams >= 32) 0 else null
            title.contains("octav") || title.contains("round-of-16") || title.contains("round of 16") -> when (totalTeams) {
                32 -> 1
                16 -> 0
                else -> null
            }
            title.contains("cuart") || title.contains("quarter") -> when (totalTeams) {
                32 -> 2
                16 -> 1
                else -> 0
            }
            title.contains("semi") -> when (totalTeams) {
                32 -> 3
                16 -> 2
                else -> 1
            }
            title.contains("final") -> when (totalTeams) {
                32 -> 4
                16 -> 3
                else -> 2
            }
            else -> null
        }
    }

    private fun consolidateMatch(matches: List<Match>, matchIndex: Int, id: String): RadialMatch {
        val firstMatch = matches.first()
        val teamAInfo = firstMatch.homeTeam
        val teamBInfo = firstMatch.awayTeam

        val latestMatch = matches.maxByOrNull { it.dateUtc } ?: firstMatch
        val startedMatches = matches.filter { it.status.state != MatchState.SCHEDULED }
        val hasAggregate = matches.any { it.isAggregateActive }
        val isMultiLegSeries = matches.any { it.legNumber != null } || matches.size >= 2

        val numericScoreA: Int?
        val numericScoreB: Int?
        val scoreA: String?
        val scoreB: String?

        when {
            startedMatches.isEmpty() -> {
                numericScoreA = null
                numericScoreB = null
                scoreA = null
                scoreB = null
            }
            hasAggregate -> {
                val aggMatch = matches.lastOrNull { it.isAggregateActive } ?: latestMatch
                val isHomeA = aggMatch.homeTeam.id == teamAInfo.id
                numericScoreA = if (isHomeA) aggMatch.aggregateHomeScore else aggMatch.aggregateAwayScore
                numericScoreB = if (isHomeA) aggMatch.aggregateAwayScore else aggMatch.aggregateHomeScore
                scoreA = numericScoreA?.toString()
                scoreB = numericScoreB?.toString()
            }
            else -> {
                var sumA = 0
                var sumB = 0
                var hasAnyScore = false
                startedMatches.forEach { m ->
                    val isHomeA = m.homeTeam.id == teamAInfo.id
                    val sA = if (isHomeA) m.homeTeam.score else m.awayTeam.score
                    val sB = if (isHomeA) m.awayTeam.score else m.homeTeam.score
                    if (sA != null && sB != null) {
                        sumA += sA
                        sumB += sB
                        hasAnyScore = true
                    }
                }
                numericScoreA = if (hasAnyScore) sumA else null
                numericScoreB = if (hasAnyScore) sumB else null
                scoreA = numericScoreA?.toString()
                scoreB = numericScoreB?.toString()
            }
        }

        // En series a dos vueltas nunca se declara vencedor tras concluir unicamente el partido de ida.
        val isSeriesFinished = when {
            matches.any { it.isSeriesCompleted } -> true
            isMultiLegSeries -> (matches.size >= 2 && matches.all { it.isFinished }) ||
                matches.any { (it.legNumber ?: 0) >= 2 && it.isFinished }
            else -> latestMatch.isFinished
        }

        val explicitSeriesWinner = matches.lastOrNull { !it.seriesWinnerTeamId.isNullOrBlank() }?.seriesWinnerTeamId
        val winnerTeamId: String? = when {
            !isSeriesFinished -> null
            explicitSeriesWinner != null -> explicitSeriesWinner
            numericScoreA != null && numericScoreB != null && numericScoreA > numericScoreB -> teamAInfo.id
            numericScoreA != null && numericScoreB != null && numericScoreB > numericScoreA -> teamBInfo.id
            latestMatch.homeTeam.isWinner -> latestMatch.homeTeam.id
            latestMatch.awayTeam.isWinner -> latestMatch.awayTeam.id
            else -> null
        }

        val shortA = teamAInfo.shortName.ifBlank { teamAInfo.abbreviation }
        val shortB = teamBInfo.shortName.ifBlank { teamBInfo.abbreviation }

        val radialA = if (teamAInfo.isPlaceholder) {
            null
        } else {
            RadialTeam(
                id = teamAInfo.id,
                name = teamAInfo.name,
                shortName = shortA,
                crestUrl = teamAInfo.logoUrl.orEmpty(),
                score = scoreA,
                isWinner = winnerTeamId != null && winnerTeamId == teamAInfo.id
            )
        }

        val radialB = if (teamBInfo.isPlaceholder) {
            null
        } else {
            RadialTeam(
                id = teamBInfo.id,
                name = teamBInfo.name,
                shortName = shortB,
                crestUrl = teamBInfo.logoUrl.orEmpty(),
                score = scoreB,
                isWinner = winnerTeamId != null && winnerTeamId == teamBInfo.id
            )
        }

        val resolvedWinnerId = if (radialA == null && radialB == null) null else winnerTeamId

        val statusText = when {
            radialA == null && radialB == null -> null
            scoreA != null && scoreB != null -> "$scoreA-$scoreB"
            latestMatch.status.displayClock.isNotBlank() -> latestMatch.status.displayClock
            else -> null
        }

        return RadialMatch(
            id = id,
            matchIndex = matchIndex,
            teamA = radialA,
            teamB = radialB,
            winnerTeamId = resolvedWinnerId,
            statusText = statusText
        )
    }
}
