package com.ixeken.worldcupinfo.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Entidad de Room que representa la tabla "predictions" para almacenar el pronóstico de la quiniela.
 * Cuenta con una clave foránea que referencia a la tabla "matches", con eliminación en cascada.
 *
 * @property matchId Identificador del partido al que pertenece la predicción, actúa como clave primaria.
 * @property goalsA Goles pronosticados para el equipo A.
 * @property goalsB Goles pronosticados para el equipo B.
 */
@Entity(
    tableName = "predictions",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PredictionEntity(
    @PrimaryKey val matchId: String,
    val goalsA: Int,
    val goalsB: Int,
    val penaltyWinner: String? = null
)
