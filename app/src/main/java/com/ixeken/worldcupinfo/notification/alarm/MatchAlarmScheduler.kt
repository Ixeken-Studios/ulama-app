package com.ixeken.worldcupinfo.notification.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ixeken.worldcupinfo.domain.model.Match
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor encargado de programar y cancelar las alarmas exactas del sistema utilizando AlarmManager.
 */
@Singleton
class MatchAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("worldcupinfo_prefs", Context.MODE_PRIVATE)

    private fun getWarningMinutes(): Int {
        return prefs.getInt("alarm_minutes", 5)
    }

    /**
     * Programa una alarma antes del partido (según preferencia del usuario) y al inicio del mismo.
     *
     * @param match Partido para el cual programar la alarma.
     * @return true si la alarma se programó exitosamente; false de lo contrario.
     */
    fun scheduleMatchAlarm(match: Match): Boolean {
        val warningMinutes = getWarningMinutes()
        val warningSeconds = warningMinutes * 60
        val triggerTimeMsWarning = (match.dateUnixTimestamp - warningSeconds) * 1000
        val triggerTimeMsStart = match.dateUnixTimestamp * 1000
        val currentTimeMs = System.currentTimeMillis()

        var scheduledAny = false

        // 1. Alarma de aviso previo
        if (warningMinutes > 0 && triggerTimeMsWarning > currentTimeMs) {
            val intent = Intent(context, MatchAlarmReceiver::class.java).apply {
                putExtra(MatchAlarmReceiver.EXTRA_MATCH_ID, match.id)
                putExtra(MatchAlarmReceiver.EXTRA_TEAM_A, match.teamA)
                putExtra(MatchAlarmReceiver.EXTRA_TEAM_B, match.teamB)
                putExtra(MatchAlarmReceiver.EXTRA_IS_START_TIME, false)
                putExtra(MatchAlarmReceiver.EXTRA_WARNING_MINUTES, warningMinutes)
            }

            val warningRequestCode = (match.id + "_warning_" + warningMinutes).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                warningRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMsWarning,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMsWarning,
                    pendingIntent
                )
            }
            scheduledAny = true
        }

        // 2. Alarma al momento del partido
        if (triggerTimeMsStart > currentTimeMs) {
            val intent = Intent(context, MatchAlarmReceiver::class.java).apply {
                putExtra(MatchAlarmReceiver.EXTRA_MATCH_ID, match.id)
                putExtra(MatchAlarmReceiver.EXTRA_TEAM_A, match.teamA)
                putExtra(MatchAlarmReceiver.EXTRA_TEAM_B, match.teamB)
                putExtra(MatchAlarmReceiver.EXTRA_IS_START_TIME, true)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (match.id + "_start").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMsStart,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMsStart,
                    pendingIntent
                )
            }
            scheduledAny = true
        }

        return scheduledAny
    }

    /**
     * Cancela las dos alarmas asociadas a un partido específico si ya estaban programadas.
     *
     * @param matchId Identificador del partido cuyas alarmas se van a cancelar.
     */
    fun cancelMatchAlarm(matchId: String) {
        // Cancelar alarma de aviso previo (se usa requestCode que incluye minutos de aviso)
        // Intent sin extras ya que solo se busca el PendingIntent existente
        val intent5Min = Intent(context, MatchAlarmReceiver::class.java)
        // Intent extras unknown here; try cancelling any common warning variants
        val possibleRequestCodes = listOf(
            (matchId + "_warning_0").hashCode(),
            (matchId + "_warning_5").hashCode(),
            (matchId + "_warning_10").hashCode(),
            (matchId + "_warning_15").hashCode(),
            (matchId + "_warning_30").hashCode(),
            (matchId + "_warning_45").hashCode(),
            (matchId + "_warning_60").hashCode(),
        )
        for (rc in possibleRequestCodes) {
            val pendingIntent5Min = PendingIntent.getBroadcast(
                context,
                rc,
                intent5Min,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent5Min != null) {
                alarmManager.cancel(pendingIntent5Min)
                pendingIntent5Min.cancel()
            }
        }

        // Cancelar alarma de inicio
        val intentStart = Intent(context, MatchAlarmReceiver::class.java)
        val pendingIntentStart = PendingIntent.getBroadcast(
            context,
            (matchId + "_start").hashCode(),
            intentStart,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntentStart != null) {
            alarmManager.cancel(pendingIntentStart)
            pendingIntentStart.cancel()
        }
    }

    /**
     * Verifica si la aplicación posee permisos del sistema para programar alarmas exactas (Android 12+).
     */
    fun canScheduleExactAlarms(): Boolean {
        return alarmManager.canScheduleExactAlarms()
    }
}
