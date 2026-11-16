package com.ixeken.ulama.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receptor del sistema encargado de restaurar alarmas de partidos y el escaneo de favoritos
 * tras reinicios del dispositivo, actualizaciones de la app o concesion de alarmas exactas.
 */
class MatchSystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                MatchAlarmScheduler.rescheduleAllFollowed(context)
                FavoriteAutoFollowJobService.syncSchedule(context)
            }
        }
    }
}
