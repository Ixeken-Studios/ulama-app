package com.ixeken.ulama.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.ixeken.ulama.core.model.FollowedMatch
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.data.repository.AppSettingsRepositoryImpl
import com.ixeken.ulama.data.repository.MatchFollowRepository

/**
 * Programador de alarmas exactas para activar el seguimiento de partidos al iniciar el encuentro.
 *
 * Emplea AlarmManager con setExactAndAllowWhileIdle para despertar el sistema aun en modo Doze,
 * asegurando que la notificacion de Now Bar aparezca justo cuando rueda el balon.
 */
object MatchAlarmScheduler {

    const val ACTION_START_MATCH_TRACKING = "com.ixeken.ulama.ACTION_START_MATCH_TRACKING"
    const val EXTRA_MATCH_ID = "extra_match_id"

    /**
     * Verifica si la aplicacion cuenta con permiso del sistema para agendar alarmas exactas.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService<AlarmManager>() ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Agenda el inicio del servicio para la marca temporal previa al silbatazo del partido.
     * Si no se especifica un margen, toma la anticipacion configurada en las preferencias del usuario.
     */
    fun scheduleKickoff(
        context: Context,
        match: FollowedMatch,
        leadMinutes: Int = AppSettingsRepositoryImpl.provide(context).settings.value.notificationLeadMinutes
    ) {
        if (match.startTimeUtcMillis <= 0L) return

        val leadMillis = leadMinutes.coerceAtLeast(0) * 60 * 1000L
        val triggerTime = match.startTimeUtcMillis - leadMillis
        val now = System.currentTimeMillis()

        if (triggerTime <= now) {
            val isReasonablyRecent = now < match.startTimeUtcMillis + THREE_HOURS_MS
            if (isReasonablyRecent) {
                MatchLiveNotificationService.start(context, match.matchId)
            }
            return
        }

        scheduleAlarmAt(context, match.matchId, triggerTime)
    }

    /**
     * Dispara una alarma exacta inmediata a 1 segundo vista cuando un JobService en segundo plano
     * detecta un encuentro en vivo y requiere la exencion temporal del receptor para arrancar el servicio.
     */
    fun scheduleImmediateTrigger(context: Context, matchId: String) {
        if (!canScheduleExactAlarms(context)) {
            MatchLiveNotificationService.start(context, matchId)
            return
        }
        scheduleAlarmAt(context, matchId, System.currentTimeMillis() + 1_000L)
    }

    private fun scheduleAlarmAt(context: Context, matchId: String, triggerTime: Long) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val intent = Intent(context, MatchAlarmReceiver::class.java).apply {
            action = ACTION_START_MATCH_TRACKING
            putExtra(EXTRA_MATCH_ID, matchId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            matchId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
    }

    /**
     * Reprograma las alarmas de todos los partidos seguidos activos y reactiva
     * el seguimiento en primer plano si alguno ya se encuentra en juego o dentro del margen.
     */
    fun rescheduleAllFollowed(
        context: Context,
        leadMinutes: Int = AppSettingsRepositoryImpl.provide(context).settings.value.notificationLeadMinutes
    ) {
        val followRepository = MatchFollowRepository.getInstance(context)
        followRepository.pruneFinishedOlderThan()
        val followedMatches = followRepository.followedMatches.value.values
        for (match in followedMatches) {
            when (match.state) {
                MatchState.LIVE -> MatchLiveNotificationService.start(context, match.matchId)
                MatchState.SCHEDULED -> scheduleKickoff(context, match, leadMinutes)
                MatchState.FINISHED -> Unit
            }
        }
    }

    private const val THREE_HOURS_MS = 3L * 60L * 60L * 1000L

    /**
     * Cancela cualquier alarma previamente programada para este partido.
     */
    fun cancelKickoff(context: Context, matchId: String) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val intent = Intent(context, MatchAlarmReceiver::class.java).apply {
            action = ACTION_START_MATCH_TRACKING
            putExtra(EXTRA_MATCH_ID, matchId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            matchId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
