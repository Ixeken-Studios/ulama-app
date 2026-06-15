package com.ixeken.worldcupinfo.domain.model

/**
 * Modelo de negocio puro para representar un partido de fútbol del Mundial.
 *
 * @property id Identificador único del partido.
 * @property dateUnixTimestamp Fecha y hora del partido expresado en Unix timestamp (segundos).
 * @property teamA Nombre del primer equipo.
 * @property teamB Nombre del segundo equipo.
 * @property stadium Nombre del estadio donde se jugará el encuentro.
 * @property status Estado actual del partido (programado, en vivo o finalizado).
 * @property prediction El pronóstico de goles guardado por el usuario, si lo hay.
 */
data class Match(
    val id: String,
    val dateUnixTimestamp: Long,
    val teamA: String,
    val teamB: String,
    val stadium: String,
    val status: MatchStatus,
    val stage: MatchStage,
    val prediction: Prediction?,
    val isAlarmActive: Boolean = false,
    val goalsA: Int? = null,
    val goalsB: Int? = null,
    val group: String? = null,
    val eventsDetailsA: List<MatchEvent> = emptyList(),
    val eventsDetailsB: List<MatchEvent> = emptyList(),
    val liveMinute: String? = null,
    val attendance: Int? = null,
    val teamAColor: String? = null,
    val teamBColor: String? = null,
    val stats: MatchStats? = null
)

/**
 * Representa un evento individual (gol, tarjeta, etc.) con el autor, minuto y tipo.
 */
data class MatchEvent(
    val name: String,
    val minute: String,
    val type: MatchEventType
)

/**
 * Posibles tipos de eventos de un partido.
 */
enum class MatchEventType {
    GOAL,
    PENALTY,
    OWN_GOAL,
    YELLOW_CARD,
    RED_CARD
}

/**
 * Estadísticas técnicas comparativas del encuentro.
 */
data class MatchStats(
    val possessionA: Int?,
    val possessionB: Int?,
    val shotsA: Int?,
    val shotsB: Int?,
    val shotsOnTargetA: Int?,
    val shotsOnTargetB: Int?,
    val foulsA: Int?,
    val foulsB: Int?,
    val cornersA: Int?,
    val cornersB: Int?
)

/**
 * Representación de los posibles estados del partido.
 */
enum class MatchStatus {
    SCHEDULED,
    LIVE,
    HALFTIME,
    PENALTIES,
    FINISHED
}
