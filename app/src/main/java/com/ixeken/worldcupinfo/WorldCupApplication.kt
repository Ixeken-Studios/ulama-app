package com.ixeken.worldcupinfo

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.hilt.work.HiltWorkerFactory
import com.ixeken.worldcupinfo.worker.SyncFixtureWorker
import com.ixeken.worldcupinfo.data.database.MatchDao
import com.ixeken.worldcupinfo.data.mapper.toDomain
import com.ixeken.worldcupinfo.notification.alarm.MatchAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Clase de aplicación principal que actúa como el punto de entrada para Dagger Hilt.
 */
@HiltAndroidApp
class WorldCupApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "WorldCupApplication"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var matchDao: MatchDao

    @Inject
    lateinit var alarmScheduler: MatchAlarmScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleSyncTask()
        rescheduleAlarmsAtStartup()
    }

    private fun scheduleSyncTask() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncFixtureWorker>(8, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncFixtureWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun rescheduleAlarmsAtStartup() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms on boot", e)
            }
        }
    }
}
