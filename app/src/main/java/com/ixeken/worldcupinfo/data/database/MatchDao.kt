package com.ixeken.worldcupinfo.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ixeken.worldcupinfo.data.database.entities.MatchEntity
import com.ixeken.worldcupinfo.data.database.entities.MatchWithPrediction
import com.ixeken.worldcupinfo.data.database.entities.PredictionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) de Room para interactuar con las tablas de partidos y predicciones.
 */
@Dao
interface MatchDao {

    /**
     * Obtiene todos los partidos ordenados cronológicamente junto a sus predicciones de forma reactiva.
     */
    @Transaction
    @Query("SELECT * FROM matches ORDER BY dateUnixTimestamp ASC")
    fun getMatchesWithPredictionsFlow(): Flow<List<MatchWithPrediction>>

    /**
     * Obtiene una lista estática de todos los partidos guardados localmente.
     */
    @Query("SELECT * FROM matches")
    suspend fun getMatches(): List<MatchEntity>

    /**
     * Obtiene todos los partidos de forma estática junto a sus predicciones.
     */
    @Transaction
    @Query("SELECT * FROM matches")
    suspend fun getMatchesWithPredictions(): List<MatchWithPrediction>

    /**
     * Inserta o reemplaza una lista de partidos.
     */
    @Upsert
    suspend fun insertMatches(matches: List<MatchEntity>)

    /**
     * Inserta o actualiza una predicción para un partido.
     */
    @Upsert
    suspend fun upsertPrediction(prediction: PredictionEntity)

    /**
     * Elimina la predicción de la quiniela asociada a un partido.
     */
    @Query("DELETE FROM predictions WHERE matchId = :matchId")
    suspend fun deletePrediction(matchId: String)

    /**
     * Actualiza el estado de activación de la alarma de un partido específico.
     */
    @Query("UPDATE matches SET isAlarmActive = :isAlarmActive WHERE id = :matchId")
    suspend fun updateAlarmState(matchId: String, isAlarmActive: Boolean)

    /**
     * Obtiene los identificadores de los partidos que tienen la alarma activa.
     */
    @Query("SELECT id FROM matches WHERE isAlarmActive = 1")
    suspend fun getActiveAlarmMatchIds(): List<String>

    /**
     * Elimina todos los partidos de eliminatorias (cuyo stage no sea GROUPS) de la base de datos.
     */
    @Query("DELETE FROM matches WHERE stage != 'GROUPS' AND isAlarmActive = 0 AND id NOT IN (SELECT matchId FROM predictions)")
    suspend fun deleteKnockoutMatches()

    /**
     * Obtiene los partidos que tienen una predicción de forma reactiva.
     */
    @Transaction
    @Query("SELECT * FROM matches WHERE id IN (SELECT matchId FROM predictions) ORDER BY dateUnixTimestamp ASC")
    fun getMatchesWithPredictionsOnlyFlow(): Flow<List<MatchWithPrediction>>
}
