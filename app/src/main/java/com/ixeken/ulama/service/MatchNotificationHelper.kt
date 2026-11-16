package com.ixeken.ulama.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.RingtoneManager
import android.os.Build
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.ixeken.ulama.MainActivity
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.FollowedMatch
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.resolveExtraTimeSuffixRes
import com.ixeken.ulama.core.network.ImageFallbackInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Gestor y constructor de notificaciones del sistema para Ulama.
 *
 * Configura los canales oficiales, construye las tarjetas en curso para la cortina
 * y Samsung Now Bar con soporte de escudos y textos localizados sin cadenas hardcodeadas.
 */
object MatchNotificationHelper {

    const val CHANNEL_LIVE_ID = "ulama_live_matches_v2"
    const val CHANNEL_GOAL_ID = "ulama_goal_alerts"

    const val EXTRA_OPEN_MATCH_ID = "extra_open_match_id"

    private val logoMemoryCache = LruCache<String, Bitmap>(30)
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ImageFallbackInterceptor())
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Descarga y almacena en memoria el escudo oficial del club para su visualizacion en la tarjeta expandida.
     * Utiliza el interceptor de Combiner para bajar directamente la miniatura de 128px en lugar del archivo crudo de 500px.
     */
    suspend fun loadTeamLogoBitmap(url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        logoMemoryCache.get(url)?.let { return@withContext it }

        runCatching {
            val request = Request.Builder().url(ImageFallbackInterceptor.toCombinerUrl(url, 128)).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes() ?: return@use null
                    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@use null
                    val scaled = if (original.width == 128 && original.height == 128) {
                        original
                    } else {
                        Bitmap.createScaledBitmap(original, 128, 128, true)
                    }
                    logoMemoryCache.put(url, scaled)
                    scaled
                } else null
            }
        }.getOrNull()
    }

    /**
     * Genera un escudo dual en Canvas combinando los logotipos de local y visitante para la tarjeta expandida.
     * Implementa la composicion limpia lado a lado sobre fondo transparente para integrarse de forma natural
     * con la superficie del sistema en modo claro u oscuro, eliminando cajas oscuras y escalando los escudos
     * de forma proporcional preservando su relacion de aspecto original.
     */
    fun createCombinedScoreboardBitmap(homeBitmap: Bitmap?, awayBitmap: Bitmap?): Bitmap? {
        if (homeBitmap == null && awayBitmap == null) return null
        if (homeBitmap != null && awayBitmap == null) return homeBitmap
        if (homeBitmap == null && awayBitmap != null) return awayBitmap

        val size = 128
        val combined = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combined)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val targetLogoSize = 56f
        val gap = 6f
        val startX = (size - (targetLogoSize * 2f + gap)) / 2f
        val centerY = size / 2f

        homeBitmap?.let {
            drawScaledLogo(canvas, it, startX, centerY - targetLogoSize / 2f, targetLogoSize, paint)
        }

        awayBitmap?.let {
            drawScaledLogo(canvas, it, startX + targetLogoSize + gap, centerY - targetLogoSize / 2f, targetLogoSize, paint)
        }

        return combined
    }

    /**
     * Dibuja un logotipo centrando su contenido dentro de un cuadro delimitador y manteniendo
     * estrictamente su proporcion original para evitar distorsiones en escudos no cuadrados.
     */
    private fun drawScaledLogo(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        maxSize: Float,
        paint: Paint
    ) {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        if (srcWidth <= 0f || srcHeight <= 0f) return

        val scale = minOf(maxSize / srcWidth, maxSize / srcHeight)
        val dstWidth = srcWidth * scale
        val dstHeight = srcHeight * scale
        val dx = left + (maxSize - dstWidth) / 2f
        val dy = top + (maxSize - dstHeight) / 2f
        val dstRect = RectF(dx, dy, dx + dstWidth, dy + dstHeight)
        canvas.drawBitmap(bitmap, null, dstRect, paint)
    }


    /**
     * Registra los canales oficiales en el NotificationManager del sistema.
     * Es idempotente y seguro de llamar en cualquier ciclo de vida.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService<NotificationManager>() ?: return

        runCatching { manager.deleteNotificationChannel("ulama_live_matches") }

        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_ID,
            context.getString(R.string.notif_channel_live_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_live_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val goalChannel = NotificationChannel(
            CHANNEL_GOAL_ID,
            context.getString(R.string.notif_channel_goals_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_goals_desc)
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(defaultSound, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(liveChannel)
        manager.createNotificationChannel(goalChannel)
    }

    /**
     * Construye la notificacion permanente de seguimiento para la cortina y la pastilla de Now Bar.
     * Utiliza codigos cortos para el titulo colapsado evitando truncado de texto en la Now Bar de Samsung,
     * y presenta titulos completos e imagenes de escudos en la vista expandida con cero cadenas hardcodeadas.
     */
    fun buildLiveNotification(
        context: Context,
        match: FollowedMatch,
        goalTeamName: String? = null,
        largeIconBitmap: Bitmap? = null,
        homeLogoBitmap: Bitmap? = null,
        awayLogoBitmap: Bitmap? = null
    ): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MATCH_ID, match.matchId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            match.matchId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, MatchLiveNotificationService::class.java).apply {
            action = MatchLiveNotificationService.ACTION_STOP
            putExtra(MatchLiveNotificationService.EXTRA_MATCH_ID, match.matchId)
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            match.matchId.hashCode() + 1000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isFinished = match.state == MatchState.FINISHED
        val hasGoalAlert = !goalTeamName.isNullOrBlank()

        val channelId = CHANNEL_LIVE_ID
        val brandColor = ContextCompat.getColor(context, R.color.ulama_brand_primary)

        val homeCode = match.homeTeamShortName.ifBlank {
            match.homeTeamName.take(3).uppercase()
        }
        val awayCode = match.awayTeamShortName.ifBlank {
            match.awayTeamName.take(3).uppercase()
        }

        // Titulo corto para la vista colapsada y la pastilla de Samsung Now Bar que evita el corte con puntos suspensivos
        val collapsedTitle = when {
            hasGoalAlert -> context.getString(
                R.string.notif_collapsed_goal_format,
                homeCode,
                match.homeScore,
                match.awayScore,
                awayCode
            )
            match.isFinishedPenalties && match.homeShootoutScore != null && match.awayShootoutScore != null -> {
                context.getString(
                    R.string.notif_collapsed_final_penalties_format,
                    homeCode,
                    match.homeScore,
                    match.homeShootoutScore,
                    match.awayShootoutScore,
                    match.awayScore,
                    awayCode
                )
            }
            match.isFinishedAet -> {
                context.getString(
                    R.string.notif_collapsed_final_aet_format,
                    homeCode,
                    match.homeScore,
                    match.awayScore,
                    awayCode
                )
            }
            isFinished -> context.getString(
                R.string.notif_collapsed_final_format,
                homeCode,
                match.homeScore,
                match.awayScore,
                awayCode
            )
            match.state == MatchState.SCHEDULED -> context.getString(
                R.string.notif_collapsed_vs_format,
                homeCode,
                awayCode
            )
            else -> context.getString(
                R.string.notif_collapsed_score_format,
                homeCode,
                match.homeScore,
                match.awayScore,
                awayCode
            )
        }

        val isHalftime = match.isHalftime ||
            match.periodDisplay.equals("Medio tiempo", ignoreCase = true) ||
            match.periodDisplay.equals("Descanso", ignoreCase = true) ||
            match.periodDisplay.equals("Halftime", ignoreCase = true) ||
            match.periodDisplay.equals("Half Time", ignoreCase = true) ||
            match.periodDisplay.equals("HT", ignoreCase = true) ||
            match.periodDisplay.equals("MT", ignoreCase = true) ||
            match.clockDisplay.equals("HT", ignoreCase = true) ||
            match.clockDisplay.equals("MT", ignoreCase = true)

        val text = when {
            match.isFinishedPenalties -> context.getString(R.string.match_status_finished_penalties)
            match.isFinishedAet -> context.getString(R.string.match_status_finished_aet)
            isFinished -> context.getString(R.string.notif_match_finished_text)
            hasGoalAlert -> {
                val clock = match.clockDisplay.trim()
                if (!goalTeamName.isNullOrBlank()) {
                    if (clock.isNotBlank()) {
                        context.getString(R.string.notif_live_goal_text, goalTeamName, clock)
                    } else {
                        context.getString(R.string.notif_live_goal_text_no_clock, goalTeamName)
                    }
                } else {
                    if (clock.isNotBlank()) {
                        context.getString(R.string.notif_live_goal_text_generic, clock)
                    } else {
                        context.getString(R.string.notif_goal_clock_default)
                    }
                }
            }
            isHalftime -> context.getString(R.string.notif_period_halftime)
            match.isShootout -> context.getString(R.string.match_status_live_penalties)
            match.isExtraTime -> {
                val clock = match.clockDisplay.ifBlank { context.getString(R.string.match_status_live_extra_time) }
                val suffix = resolveExtraTimeSuffixRes(clock)?.let { context.getString(it) }.orEmpty()
                "$clock$suffix"
            }
            match.state == MatchState.LIVE -> {
                match.clockDisplay.ifBlank { context.getString(R.string.match_status_live) }
            }
            match.state == MatchState.SCHEDULED -> {
                if (match.periodDisplay.isNotBlank()) match.periodDisplay else context.getString(R.string.match_status_upcoming)
            }
            else -> ""
        }

        val shortCriticalText = when {
            hasGoalAlert -> context.getString(
                R.string.notif_short_critical_goal_score_format,
                match.homeScore,
                match.awayScore
            )
            match.isFinishedPenalties && match.homeShootoutScore != null && match.awayShootoutScore != null -> context.getString(
                R.string.notif_short_critical_penalties_format,
                match.homeShootoutScore,
                match.awayShootoutScore
            )
            isFinished -> context.getString(
                R.string.notif_short_critical_finished_format,
                match.homeScore,
                match.awayScore
            )
            isHalftime -> context.getString(
                R.string.notif_short_critical_halftime_format,
                match.homeScore,
                match.awayScore
            )
            match.isShootout && match.homeShootoutScore != null && match.awayShootoutScore != null -> context.getString(
                R.string.notif_short_critical_penalties_format,
                match.homeShootoutScore,
                match.awayShootoutScore
            )
            match.isShootout -> context.getString(R.string.match_status_live_penalties)
            match.isExtraTime -> context.getString(
                R.string.notif_short_critical_extra_time_format,
                match.homeScore,
                match.awayScore
            )
            match.state == MatchState.LIVE -> context.getString(
                R.string.notif_short_critical_live_format,
                match.homeScore,
                match.awayScore
            )
            match.state == MatchState.SCHEDULED -> context.getString(R.string.notif_short_critical_vs)
            else -> ""
        }

        val leagueName = League.entries.firstOrNull { it.code.equals(match.leagueCode, ignoreCase = true) }
            ?.let { context.getString(it.nameRes) }
            ?: match.leagueCode.uppercase()

        val combinedIcon = largeIconBitmap ?: createCombinedScoreboardBitmap(homeLogoBitmap, awayLogoBitmap)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_soccer_ball)
            .setContentTitle(collapsedTitle)
            .setContentText(text)
            .setSubText(leagueName)
            .setColor(brandColor)
            .setColorized(false)
            .setOngoing(!isFinished)
            .setAutoCancel(isFinished)
            .setOnlyAlertOnce(!hasGoalAlert && !isFinished)
            .setShowWhen(false)
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroupSummary(false)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(collapsedTitle)
                    .bigText(text)
            )

        if (combinedIcon != null) {
            builder.setLargeIcon(combinedIcon)
        }

        if (hasGoalAlert || isFinished) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        }

        if (!isFinished) {
            builder.addAction(
                0,
                context.getString(R.string.notif_action_unfollow),
                stopPendingIntent
            )
            builder.addAction(
                0,
                context.getString(R.string.notif_action_details),
                openPendingIntent
            )
        }

        val baseNotification = builder.build()

        if (isFinished) return baseNotification

        val platformPromotedNotification = runCatching {
            val platformBuilder = Notification.Builder.recoverBuilder(context, baseNotification)
            val methodPromote = platformBuilder.javaClass.getMethod(
                "setRequestPromotedOngoing",
                Boolean::class.javaPrimitiveType
            )
            methodPromote.invoke(platformBuilder, true)

            val methodShort = platformBuilder.javaClass.getMethod(
                "setShortCriticalText",
                CharSequence::class.java
            )
            methodShort.invoke(platformBuilder, shortCriticalText)
            platformBuilder.build()
        }.getOrNull()

        val finalNotification = platformPromotedNotification ?: baseNotification

        finalNotification.extras.putBoolean("android.requestPromotedOngoing", true)
        finalNotification.extras.putBoolean("androidx.core.app.extra.REQUEST_PROMOTED_ONGOING", true)
        finalNotification.extras.putBoolean("com.samsung.android.support.ongoing_activity", true)
        finalNotification.extras.putCharSequence("android.shortCriticalText", shortCriticalText)
        finalNotification.extras.putCharSequence("com.samsung.android.extra.short_critical_text", shortCriticalText)
        finalNotification.extras.putString("android.substName", context.getString(R.string.notif_app_subst_name))

        return finalNotification
    }

    /**
     * Construye una notificacion emergente de alta prioridad en CHANNEL_GOAL_ID con sonido y vibracion.
     * Se auto-cancela despues de 15 segundos y no interfiere con la tarjeta permanente en primer plano.
     */
    fun buildGoalAlertNotification(
        context: Context,
        match: FollowedMatch,
        goalTeamName: String,
        scoringTeamLogo: Bitmap? = null
    ): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MATCH_ID, match.matchId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            match.matchId.hashCode() + 2000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val brandColor = ContextCompat.getColor(context, R.color.ulama_brand_primary)
        val homeCode = match.homeTeamShortName.ifBlank { match.homeTeamName.take(3).uppercase() }
        val awayCode = match.awayTeamShortName.ifBlank { match.awayTeamName.take(3).uppercase() }

        val title = context.getString(R.string.notif_goal_alert_title, goalTeamName)
        val clock = match.clockDisplay.trim()
        val content = if (clock.isNotBlank()) {
            context.getString(
                R.string.notif_goal_alert_expanded_clock_format,
                homeCode,
                match.homeScore,
                match.awayScore,
                awayCode,
                clock
            )
        } else {
            context.getString(
                R.string.notif_goal_alert_expanded_format,
                homeCode,
                match.homeScore,
                match.awayScore,
                awayCode
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_GOAL_ID)
            .setSmallIcon(R.drawable.ic_soccer_ball)
            .setContentTitle(title)
            .setContentText(content)
            .setColor(brandColor)
            .setAutoCancel(true)
            .setTimeoutAfter(15000L)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)

        if (scoringTeamLogo != null) {
            builder.setLargeIcon(scoringTeamLogo)
        }

        return builder.build()
    }
}

