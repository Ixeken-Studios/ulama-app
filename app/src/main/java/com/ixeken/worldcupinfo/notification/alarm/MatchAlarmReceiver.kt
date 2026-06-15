package com.ixeken.worldcupinfo.notification.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ixeken.worldcupinfo.MainActivity
import com.ixeken.worldcupinfo.R
import com.ixeken.worldcupinfo.ui.common.getTeamDisplayName

/**
 * BroadcastReceiver encargado de recibir las alarmas del sistema y disparar notificaciones locales.
 */
class MatchAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val matchId = intent.getStringExtra(EXTRA_MATCH_ID) ?: return
        val fallbackA = context.getString(R.string.notification_fallback_team_a)
        val fallbackB = context.getString(R.string.notification_fallback_team_b)
        val teamA = intent.getStringExtra(EXTRA_TEAM_A) ?: fallbackA
        val teamB = intent.getStringExtra(EXTRA_TEAM_B) ?: fallbackB
        val isStartTime = intent.getBooleanExtra(EXTRA_IS_START_TIME, false)

        showNotification(context, matchId, teamA, teamB, isStartTime)
    }

    private fun showNotification(context: Context, matchId: String, teamA: String, teamB: String, isStartTime: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "match_alerts_channel"
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("match_id", matchId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            matchId.hashCode() + (if (isStartTime) 1 else 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayNameA = getTeamDisplayName(teamA, context)
        val displayNameB = getTeamDisplayName(teamB, context)

        val warningMinutes = intent.getIntExtra(EXTRA_WARNING_MINUTES, 5)
        val title = if (isStartTime) {
            context.getString(R.string.notification_title_start)
        } else {
            context.getString(R.string.notification_title_upcoming)
        }
        val text = if (isStartTime) {
            context.getString(R.string.notification_text_start, displayNameA, displayNameB)
        } else {
            context.getString(R.string.notification_text_upcoming, displayNameA, displayNameB, warningMinutes)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = if (isStartTime) matchId.hashCode() + 1 else matchId.hashCode()
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_MATCH_ID = "extra_match_id"
        const val EXTRA_TEAM_A = "extra_team_a"
        const val EXTRA_TEAM_B = "extra_team_b"
        const val EXTRA_IS_START_TIME = "extra_is_start_time"
        const val EXTRA_WARNING_MINUTES = "extra_warning_minutes"
    }
}

