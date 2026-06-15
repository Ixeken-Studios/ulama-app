package com.ixeken.worldcupinfo.notification.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ixeken.worldcupinfo.data.database.MatchDao
import com.ixeken.worldcupinfo.data.mapper.toDomain
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver que responde al reinicio del dispositivo para volver a programar las alarmas activas.
 */
@AndroidEntryPoint
class MatchBootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MatchBootReceiver"
    }

    @Inject
    lateinit var matchDao: MatchDao

    @Inject
    lateinit var alarmScheduler: MatchAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val matchesWithPrediction = matchDao.getMatchesWithPredictions()
                    val currentTimeMs = System.currentTimeMillis()
                    matchesWithPrediction.forEach { matchWithPred ->
                        val match = matchWithPred.toDomain()
                        if (match.isAlarmActive) {
                            val matchTimeMs = match.dateUnixTimestamp * 1000
                            // Solo volver a programar si el partido es futuro
                            if (matchTimeMs > currentTimeMs) {
                                alarmScheduler.scheduleMatchAlarm(match)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
