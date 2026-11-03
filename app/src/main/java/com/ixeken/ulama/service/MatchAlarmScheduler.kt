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
            val isReasonablyRecent = now < match.startTimeUtcMillis + (3 * 60 * 60 * 1000L)
            if (isReasonablyRecent) {
                MatchLiveNotificationService.start(context, match.matchId)
            }
            return
        }

        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val intent = Intent(context, MatchAlarmReceiver::class.java).apply {
            action = ACTION_START_MATCH_TRACKING
            putExtra(EXTRA_MATCH_ID, match.matchId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            match.matchId.hashCode(),
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
     * Reprograma las alarmas de todos los partidos seguidos en estado agendado
     * cuando se modifica la preferencia de anticipacion.
     */
    fun rescheduleAllFollowed(
        context: Context,
        leadMinutes: Int = AppSettingsRepositoryImpl.provide(context).settings.value.notificationLeadMinutes
    ) {
        val followRepository = MatchFollowRepository.getInstance(context)
        val followedMatches = followRepository.followedMatches.value.values
        for (match in followedMatches) {
            if (match.state == MatchState.SCHEDULED) {
                scheduleKickoff(context, match, leadMinutes)
            }
        }
    }

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
