package com.ixeken.worldcupinfo.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Estructura de relación de uno a uno para obtener la información del encuentro
 * combinada con la predicción de la quiniela correspondiente si es que ha sido guardada.
 *
 * @property match La entidad del partido embebida.
 * @property prediction La entidad de predicción relacionada si existe.
 */
data class MatchWithPrediction(
    @Embedded val match: MatchEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "matchId"
    )
    val prediction: PredictionEntity?
)
