package com.ixeken.worldcupinfo.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room que representa la tabla "matches" en la base de datos local.
 *
 * @property id Identificador único del partido.
 * @property dateUnixTimestamp Fecha y hora del partido expresada en Unix timestamp (segundos).
 * @property teamA Nombre del primer equipo.
 * @property teamB Nombre del segundo equipo.
 * @property stadium Nombre del estadio del encuentro.
 * @property status Estado en formato String (SCHEDULED, LIVE, FINISHED).
 */
@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val dateUnixTimestamp: Long,
    val teamA: String,
    val teamB: String,
    val stadium: String,
    val status: String,
    val stage: String,
    val isAlarmActive: Boolean = false,
    val goalsA: Int? = null,
    val goalsB: Int? = null,
    val group: String? = null,
    val eventsDetailsA: String? = null,
    val eventsDetailsB: String? = null,
    val liveMinute: String? = null,
    val attendance: Int? = null,
    val teamAColor: String? = null,
    val teamBColor: String? = null,
    val stats: String? = null
)
