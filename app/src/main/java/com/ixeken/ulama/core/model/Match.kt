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
    val seriesSummary: String? = null
) {
    /**
     * Determina si el encuentro corresponde a una serie eliminatoria directa o fase de liguilla.
     * Excluye etiquetas de fase de grupos o jornadas regulares para prevenir clasificaciones erroneas.
     */
    val isKnockout: Boolean
        get() {
            val title = seriesTitle?.trim()
            if (!title.isNullOrBlank()) {
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
     */
    val isAggregateActive: Boolean
        get() = aggregateHomeScore != null && aggregateAwayScore != null

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

