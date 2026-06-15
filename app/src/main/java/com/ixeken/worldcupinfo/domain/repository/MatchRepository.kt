package com.ixeken.worldcupinfo.domain.repository

import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.model.Prediction
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a datos para la lógica de negocio en la capa de Dominio.
 */
interface MatchRepository {

    /**
     * Retorna la lista de todos los partidos del mundial de manera reactiva, incluyendo su predicción.
     */
    fun getMatchesFlow(): Flow<List<Match>>

    /**
     * Guarda la lista inicial de partidos en el almacenamiento local.
     */
    suspend fun saveMatches(matches: List<Match>)

    /**
     * Sincroniza los datos del fixture con la fuente remota y actualiza el almacenamiento local.
     */
    suspend fun syncFixture()

    /**
     * Sincroniza los marcadores y goleadores en vivo desde la API de ESPN.
     */
    suspend fun syncLiveScores(dateString: String? = null)

    /**
     * Guarda o edita una predicción (quiniela) para un partido específico.
     */
    suspend fun savePrediction(prediction: Prediction)

    /**
     * Elimina el pronóstico guardado para un partido específico.
     */
    suspend fun deletePrediction(matchId: String)

    /**
     * Actualiza el estado de activación de la alarma de un partido específico.
     */
    suspend fun updateAlarmState(matchId: String, isAlarmActive: Boolean)

    /**
     * Elimina todos los partidos de eliminatorias de la base de datos.
     */
    suspend fun deleteKnockoutMatches()

    /**
     * Retorna únicamente los partidos que tienen una predicción de manera reactiva.
     */
    fun getMatchesWithPredictionsOnlyFlow(): Flow<List<Match>>
}
