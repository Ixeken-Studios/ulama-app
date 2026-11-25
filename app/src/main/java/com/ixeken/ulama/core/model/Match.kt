package com.ixeken.ulama.core.model

import androidx.compose.runtime.Immutable

/**
 * Modelo inmutable principal de un partido de futbol.
 * Representa los contendientes, el estado del cronometro y la sede.
 */
@Immutable
data class Match(
    val id: String,
    val dateUtc: String,
    val homeTeam: Team,
    val awayTeam: Team,
    val status: MatchStatus,
    val venueName: String?,
    val league: League = League.LIGA_MX,
    val seriesTitle: String? = null,
    val legNumber: Int? = null,
    val legText: String? = null,
    val aggregateHomeScore: Int? = null,
    val aggregateAwayScore: Int? = null,
    val seriesSummary: String? = null,
    val isSeriesCompleted: Boolean = false,
    val seriesWinnerTeamId: String? = null,
    val homeScorers: List<GoalScorer> = emptyList(),
    val awayScorers: List<GoalScorer> = emptyList()
) {
    /**
     * Determina si el encuentro corresponde a una serie eliminatoria directa o fase de liguilla.
     * Evalua primero indicadores explicitos de ronda eliminatoria para no descartar titulos con
     * prefijos de torneo estacional (ej. "Apertura - Quarterfinals" o "Clausura - Finals").
     */
    val isKnockout: Boolean
        get() {
            val title = seriesTitle?.trim()
            if (!title.isNullOrBlank()) {
                val hasKnockoutKeyword = title.contains("Quarter", ignoreCase = true) ||
                    title.contains("Cuart", ignoreCase = true) ||
                    title.contains("Semi", ignoreCase = true) ||
                    title.contains("Final", ignoreCase = true) ||
                    title.contains("Octav", ignoreCase = true) ||
                    title.contains("Dieciseis", ignoreCase = true) ||
                    title.contains("Round of ", ignoreCase = true) ||
                    title.contains("Round-of-", ignoreCase = true) ||
                    title.contains("3rd", ignoreCase = true) ||
                    title.contains("Third", ignoreCase = true) ||
                    title.contains("Tercer", ignoreCase = true) ||
                    title.contains("Playoff", ignoreCase = true) ||
                    title.contains("Play-In", ignoreCase = true) ||
                    title.contains("Play In", ignoreCase = true) ||
                    title.contains("Prelim", ignoreCase = true) ||
                    title.contains("Liguilla", ignoreCase = true)
                if (hasKnockoutKeyword) return true

                val isGroupOrRegular = title.contains("Grupo", ignoreCase = true) ||
                    title.contains("Group", ignoreCase = true) ||
                    title.contains("Jornada", ignoreCase = true) ||
                    title.contains("Apertura", ignoreCase = true) ||
                    title.contains("Clausura", ignoreCase = true) ||
                    title.equals("group-stage", ignoreCase = true) ||
                    title.equals("league-phase", ignoreCase = true) ||
                    title.equals("regular-season", ignoreCase = true)
                if (isGroupOrRegular) return false
                return true
            }
            return isAggregateActive || legNumber != null
        }

    /**
     * Identifica si existe un marcador global valido registrado para ambos contendientes.
     * Un partido de ida (legNumber == 1) no posee marcador global por definicion.
     */
    val isAggregateActive: Boolean
        get() = aggregateHomeScore != null && aggregateAwayScore != null && legNumber != 1

    /**
     * Determina si el encuentro deportivo ha finalizado de forma definitiva.
     */
    val isFinished: Boolean
        get() = status.state == MatchState.FINISHED

    /**
     * Determina si el encuentro se encuentra en el descanso de medio tiempo.
     */
    val isHalftime: Boolean
        get() = status.isHalftime

    /**
     * Identifica si el equipo local es el ganador del encuentro considerando penales si existieron.
     */
    val isHomeWinner: Boolean
        get() {
            if (!isFinished) return false
            if (homeTeam.isWinner) return true
            if (awayTeam.isWinner) return false
            val homeShootout = homeTeam.shootoutScore
            val awayShootout = awayTeam.shootoutScore
            if (homeShootout != null && awayShootout != null && homeShootout != awayShootout) {
                return homeShootout > awayShootout
            }
            return (homeTeam.score ?: 0) > (awayTeam.score ?: 0)
        }

    /**
     * Identifica si el equipo visitante es el ganador del encuentro considerando penales si existieron.
     */
    val isAwayWinner: Boolean
        get() {
            if (!isFinished) return false
            if (awayTeam.isWinner) return true
            if (homeTeam.isWinner) return false
            val homeShootout = homeTeam.shootoutScore
            val awayShootout = awayTeam.shootoutScore
            if (homeShootout != null && awayShootout != null && homeShootout != awayShootout) {
                return awayShootout > homeShootout
            }
            return (awayTeam.score ?: 0) > (homeTeam.score ?: 0)
        }

    /**
     * Identifica si el partido concluyo en empate sin vencedor definido.
     */
    val isDraw: Boolean
        get() = isFinished && !isHomeWinner && !isAwayWinner

    /**
     * Identifica si el equipo local resulto derrotado para atenuar su peso visual en la interfaz.
     */
    val isHomeLoser: Boolean
        get() = isFinished && !isDraw && isAwayWinner

    /**
     * Identifica si el equipo visitante resulto derrotado para atenuar su peso visual en la interfaz.
     */
    val isAwayLoser: Boolean
        get() = isFinished && !isDraw && isHomeWinner
}

/**
 * Normaliza los marcadores globales en una coleccion de partidos eliminatorios a dos vueltas.
 * Garantiza que ningun partido de vuelta programado o en curso exhiba marcador global
 * si el partido de ida correspondiente aun no ha concluido en su totalidad.
 */
fun List<Match>.sanitizeAggregateScores(): List<Match> {
    if (this.isEmpty()) return this
    val matchesByTeams = this.groupBy { match ->
        setOf(match.homeTeam.id, match.awayTeam.id)
    }

    return this.map { match ->
        if (match.aggregateHomeScore == null || match.aggregateAwayScore == null) return@map match

        // El partido de ida de una serie eliminatoria nunca posee marcador global acumulado
        if (match.legNumber == 1) {
            return@map match.copy(aggregateHomeScore = null, aggregateAwayScore = null)
        }

        // Buscamos el partido de ida de la misma serie eliminatoria entre ambos contendientes
        val pairMatches = matchesByTeams[setOf(match.homeTeam.id, match.awayTeam.id)].orEmpty()
        val firstLeg = pairMatches.firstOrNull { other ->
            other.id != match.id &&
                (!match.isKnockout || other.isKnockout) &&
                (
                    (other.legNumber != null && match.legNumber != null && other.legNumber < match.legNumber) ||
                    other.dateUtc < match.dateUtc
                )
        }

        // Si existe el partido de ida y aun no ha concluido, se anula el global en el partido de vuelta
        if (firstLeg != null && !firstLeg.isFinished) {
            match.copy(aggregateHomeScore = null, aggregateAwayScore = null)
        } else {
            match
        }
    }
}


