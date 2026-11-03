package com.ixeken.ulama.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receptor de difusion que captura el silbatazo inicial agendado y arranca el servicio de notificaciones.
 *
 * El sistema operativo despierta este componente de manera garantizada mediante AlarmManager,
 * permitiendo iniciar el Foreground Service con la excepcion temporal otorgada a las alarmas exactas.
 */
class MatchAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val matchId = intent.getStringExtra(MatchAlarmScheduler.EXTRA_MATCH_ID) ?: return
        MatchLiveNotificationService.start(context, matchId)
    }
}
