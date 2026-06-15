package com.ixeken.worldcupinfo.domain.model

/**
 * Modelo de negocio puro que representa la predicción (quiniela) de goles de un partido.
 *
 * @property matchId Identificador único del partido asociado a la quiniela.
 * @property goalsA Goles pronosticados para el equipo A.
 * @property goalsB Goles pronosticados para el equipo B.
 */
data class Prediction(
    val matchId: String,
    val goalsA: Int,
    val goalsB: Int,
    val penaltyWinner: String? = null
)
