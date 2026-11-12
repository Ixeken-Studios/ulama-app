package com.ixeken.ulama.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Representa un encuentro deportivo seguido por el usuario para notificaciones y Live Updates on-device.
 *
 * Modelo inmutable y serializable disenhado para persistir en disco el estado basico
 * del partido necesario para el despertar por alarma y la restauracion del servicio.
 */
@Immutable
@Serializable
data class FollowedMatch(
    val matchId: String,
    val leagueCode: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val homeTeamId: String = "",
    val awayTeamId: String = "",
    val homeTeamShortName: String = "",
    val awayTeamShortName: String = "",
    val homeTeamLogo: String = "",
    val awayTeamLogo: String = "",
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val state: MatchState = MatchState.SCHEDULED,
    val clockDisplay: String = "",
    val periodDisplay: String = "",
    val startTimeUtcMillis: Long = 0L,
    val isHalftime: Boolean = false,
    val isExtraTime: Boolean = false,
    val isShootout: Boolean = false,
    val isFinishedAet: Boolean = false,
    val isFinishedPenalties: Boolean = false,
    val homeShootoutScore: Int? = null,
    val awayShootoutScore: Int? = null
)

/**
 * Convierte un partido bajo seguimiento persistido a un modelo inmutable de dominio Match.
 * Permite desplegar la vista de detalle inmediatamente sin esperar una peticion remota completa.
 */
fun FollowedMatch.toMatch(): Match {
    val targetLeague = League.fromCode(leagueCode)
    val resolvedDateUtc = if (startTimeUtcMillis > 0L) {
        java.time.Instant.ofEpochMilli(startTimeUtcMillis).toString()
    } else ""
    return Match(
        id = matchId,
        dateUtc = resolvedDateUtc,
        homeTeam = Team(
            id = homeTeamId.ifBlank { "team_home_$matchId" },
            name = homeTeamName,
            shortName = homeTeamShortName,
            abbreviation = homeTeamShortName.ifBlank { homeTeamName.take(3).uppercase() },
            logoUrl = homeTeamLogo.ifBlank { null },
            isHome = true,
            score = homeScore,
            isWinner = false,
            shootoutScore = homeShootoutScore
        ),
        awayTeam = Team(
            id = awayTeamId.ifBlank { "team_away_$matchId" },
            name = awayTeamName,
            shortName = awayTeamShortName,
            abbreviation = awayTeamShortName.ifBlank { awayTeamName.take(3).uppercase() },
            logoUrl = awayTeamLogo.ifBlank { null },
            isHome = false,
            score = awayScore,
            isWinner = false,
            shootoutScore = awayShootoutScore
        ),
        status = MatchStatus(
            state = state,
            displayClock = clockDisplay,
            detail = periodDisplay,
            isCompleted = state == MatchState.FINISHED,
            isHalftime = isHalftime,
            isExtraTime = isExtraTime,
            isShootout = isShootout,
            isFinishedAet = isFinishedAet,
            isFinishedPenalties = isFinishedPenalties
        ),
        venueName = null,
        league = targetLeague
    )
}
