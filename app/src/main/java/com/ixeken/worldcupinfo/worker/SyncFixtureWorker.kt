package com.ixeken.worldcupinfo.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ixeken.worldcupinfo.domain.repository.MatchRepository
import com.ixeken.worldcupinfo.data.database.MatchDao
import com.ixeken.worldcupinfo.data.mapper.toDomain
import com.ixeken.worldcupinfo.notification.alarm.MatchAlarmScheduler
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker para sincronizar el fixture del mundial con la fuente remota en segundo plano y re-agendar alarmas activas (Auto-curación).
 */
@HiltWorker
class SyncFixtureWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val matchRepository: MatchRepository,
    private val matchDao: MatchDao,
    private val alarmScheduler: MatchAlarmScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Sincronizar fixture
                matchRepository.syncFixture()
                
                // 2. Rutina de auto-curación de alarmas
                val matchesWithPrediction = matchDao.getMatchesWithPredictions()
                val currentTimeMs = System.currentTimeMillis()
                matchesWithPrediction.forEach { matchWithPred ->
                    val match = matchWithPred.toDomain()
                    if (match.isAlarmActive) {
                        val matchTimeMs = match.dateUnixTimestamp * 1000
                        if (matchTimeMs > currentTimeMs) {
                            alarmScheduler.scheduleMatchAlarm(match)
                        }
                    }
                }
                
                Result.success()
            } catch (e: Exception) {
                // Si falla la sincronización, intentamos de nuevo más tarde si es necesario,
                // pero devolvemos retry para que WorkManager intente de nuevo bajo sus constraints
                Result.retry()
            }
        }
    }
}
