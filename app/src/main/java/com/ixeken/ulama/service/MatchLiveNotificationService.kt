package com.ixeken.ulama.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.util.DateTimeUtils
import com.ixeken.ulama.data.repository.AppSettingsRepositoryImpl
import com.ixeken.ulama.data.repository.MatchFollowRepository
import com.ixeken.ulama.data.repository.MatchRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Servicio en primer plano de tipo dataSync para actualizacion en vivo de marcadores y Now Bar.
 *
 * Mantiene la corrutina de sondeo activa con pantalla apagada sin recurrir a servicios externos,
 * comparando marcadores locales para disparar alertas inmediatas de gol.
 */
class MatchLiveNotificationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var followRepository: MatchFollowRepository
    private lateinit var matchRepository: MatchRepositoryImpl
    private var isPollingActive = false

    private val previousScores = mutableMapOf<String, Pair<Int, Int>>()

    private var currentForegroundMatchId: String? = null

    override fun onCreate() {
        super.onCreate()
        followRepository = MatchFollowRepository.getInstance(applicationContext)
        matchRepository = MatchRepositoryImpl.getInstance(applicationContext)
        MatchNotificationHelper.createNotificationChannels(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val matchId = intent?.getStringExtra(EXTRA_MATCH_ID)

        when (action) {
            ACTION_STOP -> {
                if (matchId != null) {
                    followRepository.unfollow(matchId)
                    notificationManager().cancel(matchId.hashCode())
                    notificationManager().cancel(matchId.hashCode() + GOAL_ALERT_ID_OFFSET)
                    notificationManager().cancel(matchId.hashCode() + FINISHED_ALERT_OFFSET)
                    if (matchId == currentForegroundMatchId) {
                        currentForegroundMatchId = null
                    }
                }
                checkAndStopIfNoActiveMatches()
                return START_NOT_STICKY
            }
            ACTION_START_TEST -> {
                if (matchId != null) {
                    val match = followRepository.getFollowed(matchId)
                    if (match != null) {
                        val notif = MatchNotificationHelper.buildLiveNotification(this, match)
                        startForegroundCompat(match.matchId.hashCode(), notif)
                        serviceScope.launch {
                            val homeLogo = MatchNotificationHelper.loadTeamLogoBitmap(this@MatchLiveNotificationService, match.homeTeamLogo)
                            val awayLogo = MatchNotificationHelper.loadTeamLogoBitmap(this@MatchLiveNotificationService, match.awayTeamLogo)
                            val richNotif = MatchNotificationHelper.buildLiveNotification(
                                this@MatchLiveNotificationService,
                                match,
                                homeLogoBitmap = homeLogo,
                                awayLogoBitmap = awayLogo
                            )
                            notificationManager().notify(match.matchId.hashCode(), richNotif)
                        }
                    }
                }
                return START_STICKY
            }
            ACTION_TEST_GOAL -> {
                if (matchId != null) {
                    val match = followRepository.getFollowed(matchId)
                    val team = intent.getStringExtra(EXTRA_SCORING_TEAM) ?: "Equipo"
                    if (match != null) {
                        val liveNotif = MatchNotificationHelper.buildLiveNotification(this, match, goalTeamName = team)
                        startForegroundCompat(match.matchId.hashCode(), liveNotif)
                        serviceScope.launch {
                            val homeLogo = MatchNotificationHelper.loadTeamLogoBitmap(this@MatchLiveNotificationService, match.homeTeamLogo)
                            val awayLogo = MatchNotificationHelper.loadTeamLogoBitmap(this@MatchLiveNotificationService, match.awayTeamLogo)
                            val richNotif = MatchNotificationHelper.buildLiveNotification(
                                this@MatchLiveNotificationService,
                                match,
                                goalTeamName = team,
                                homeLogoBitmap = homeLogo,
                                awayLogoBitmap = awayLogo
                            )
                            notificationManager().notify(match.matchId.hashCode(), richNotif)
                        }
                    }
                }
                return START_STICKY
            }
            ACTION_TEST_FINISH -> {
                if (matchId != null) {
                    val match = followRepository.getFollowed(matchId)
                    if (match != null) {
                        val finishedLive = MatchNotificationHelper.buildLiveNotification(this, match)
                        notificationManager().notify(match.matchId.hashCode(), finishedLive)
                        runCatching { stopForeground(STOP_FOREGROUND_DETACH) }
                    }
                }
                checkAndStopIfNoActiveMatches()
                return START_NOT_STICKY
            }
        }

        val allFollowed = followRepository.followedMatches.value.values.toList()
        val requestedMatch = if (matchId != null) followRepository.getFollowed(matchId) else null
        val alreadyTracked = matchId != null &&
            isPollingActive &&
            (matchId == currentForegroundMatchId || previousScores.containsKey(matchId))

        if (requestedMatch != null && isEligibleForTracking(requestedMatch)) {
            if (!alreadyTracked) {
                val notif = MatchNotificationHelper.buildLiveNotification(this, requestedMatch)
                if (currentForegroundMatchId == null) {
                    currentForegroundMatchId = requestedMatch.matchId
                    startForegroundCompat(requestedMatch.matchId.hashCode(), notif)
                } else {
                    notificationManager().notify(requestedMatch.matchId.hashCode(), notif)
                }
                serviceScope.launch {
                    val homeLogo = MatchNotificationHelper.loadTeamLogoBitmap(this@MatchLiveNotificationService, requestedMatch.homeTeamLogo)
                    val awayLogo = MatchNotificationHelper.loadTeamLogoBitmap(this@MatchLiveNotificationService, requestedMatch.awayTeamLogo)
                    val richNotif = MatchNotificationHelper.buildLiveNotification(
                        this@MatchLiveNotificationService,
                        requestedMatch,
                        homeLogoBitmap = homeLogo,
                        awayLogoBitmap = awayLogo
                    )
                    notificationManager().notify(requestedMatch.matchId.hashCode(), richNotif)
                }
            }
        } else if (currentForegroundMatchId == null) {
            val firstActive = allFollowed.firstOrNull { isEligibleForTracking(it) }
            if (firstActive != null) {
                currentForegroundMatchId = firstActive.matchId
                val initialNotif = MatchNotificationHelper.buildLiveNotification(this, firstActive)
                startForegroundCompat(firstActive.matchId.hashCode(), initialNotif)
            } else {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!isPollingActive) {
            isPollingActive = true
            serviceScope.launch {
                pollFollowedMatchesLoop()
            }
        }

        return START_STICKY
    }

    /**
     * Evalua si un partido seguido califica para monitoreo activo y notificaciones en primer plano.
     * Los encuentros agendados solo califican si el tiempo actual alcanza el margen de anticipacion configurado.
     */
    private fun isEligibleForTracking(match: com.ixeken.ulama.core.model.FollowedMatch): Boolean {
        return when (match.state) {
            MatchState.LIVE -> true
            MatchState.SCHEDULED -> {
                if (match.startTimeUtcMillis <= 0L) return false
                val leadMinutes = AppSettingsRepositoryImpl.provide(applicationContext).settings.value.notificationLeadMinutes
                val leadMillis = leadMinutes.coerceAtLeast(0) * 60 * 1000L
                val now = System.currentTimeMillis()
                now >= (match.startTimeUtcMillis - leadMillis)
            }
            MatchState.FINISHED -> false
        }
    }

    /**
     * Bucle continuo de sondeo con intervalos adaptados segun el periodo del partido.
     * Agrupa los partidos seguidos por liga y fecha para realizar una sola peticion HTTP por competicion.
     */
    private suspend fun pollFollowedMatchesLoop() {
        try {
            while (serviceScope.isActive) {
                val activeMatches = followRepository.followedMatches.value.values.filter {
                    isEligibleForTracking(it)
                }

                if (activeMatches.isEmpty()) {
                    currentForegroundMatchId = null
                    runCatching { stopForeground(STOP_FOREGROUND_DETACH) }
                    stopSelf()
                    break
                }

                val groupedByLeagueAndDate = activeMatches.groupBy { followed ->
                    Pair(League.fromCode(followed.leagueCode), resolvePollDateKey(followed))
                }

                for ((leagueAndDate, matchesInGroup) in groupedByLeagueAndDate) {
                    val (league, dateKey) = leagueAndDate
                    val dayMatches = runCatching {
                        matchRepository.getMatches(league = league, dates = dateKey, forceRefresh = true).getOrNull()
                    }.getOrNull().orEmpty()

                    for (followed in matchesInGroup) {
                        val match = dayMatches.firstOrNull { it.id == followed.matchId }
                            ?: runCatching {
                                matchRepository.getMatches(league = league, forceRefresh = true)
                                    .getOrNull()
                                    ?.firstOrNull { it.id == followed.matchId }
                            }.getOrNull()
                            ?: continue
                        processPolledMatch(followed, match)
                    }
                }

                val hasLiveRunning = activeMatches.any { it.state == MatchState.LIVE }
                val delayMillis = if (hasLiveRunning) POLL_INTERVAL_LIVE_MS else POLL_INTERVAL_REST_MS
                delay(delayMillis)
            }
        } finally {
            isPollingActive = false
        }
    }

    /**
     * Resuelve la clave de fecha YYYYMMDD en la zona horaria del dispositivo para consultar
     * exclusivamente el dia del encuentro en lugar de descargar toda la jornada semanal.
     */
    private fun resolvePollDateKey(followed: com.ixeken.ulama.core.model.FollowedMatch): String {
        val instant = if (followed.startTimeUtcMillis > 0L) {
            Instant.ofEpochMilli(followed.startTimeUtcMillis)
        } else {
            Instant.now()
        }
        return instant.atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.BASIC_ISO_DATE)
    }

    /**
     * Evalua el partido actualizado desde ESPN y dispara alertas de gol o cierre.
     */
    private suspend fun processPolledMatch(
        followed: com.ixeken.ulama.core.model.FollowedMatch,
        match: Match
    ) {
        val newHomeScore = match.homeTeam.score ?: followed.homeScore
        val newAwayScore = match.awayTeam.score ?: followed.awayScore
        val newState = match.status.state
        val newClock = match.status.displayClock
        val newPeriod = match.status.detail

        val prevScores = previousScores[followed.matchId] ?: Pair(followed.homeScore, followed.awayScore)
        val homeScored = newHomeScore > prevScores.first
        val awayScored = newAwayScore > prevScores.second

        val updatedStartMillis = DateTimeUtils.parseUtcEpochMillis(match.dateUtc)
            .takeIf { it > 0L } ?: followed.startTimeUtcMillis
        val updatedMatch = followed.copy(
            homeScore = newHomeScore,
            awayScore = newAwayScore,
            state = newState,
            clockDisplay = newClock,
            periodDisplay = newPeriod,
            startTimeUtcMillis = updatedStartMillis,
            isHalftime = match.isHalftime,
            isExtraTime = match.status.isExtraTime,
            isShootout = match.status.isShootout,
            isFinishedAet = match.status.isFinishedAet,
            isFinishedPenalties = match.status.isFinishedPenalties,
            homeShootoutScore = match.homeTeam.shootoutScore,
            awayShootoutScore = match.awayTeam.shootoutScore
        )

        previousScores[followed.matchId] = Pair(newHomeScore, newAwayScore)
        followRepository.follow(updatedMatch)

        val notifId = updatedMatch.matchId.hashCode()

        val homeLogo = MatchNotificationHelper.loadTeamLogoBitmap(this, updatedMatch.homeTeamLogo)
        val awayLogo = MatchNotificationHelper.loadTeamLogoBitmap(this, updatedMatch.awayTeamLogo)

        if (homeScored || awayScored) {
            val scoringClub = if (homeScored) updatedMatch.homeTeamName else updatedMatch.awayTeamName
            val scoringLogo = if (homeScored) homeLogo else awayLogo

            // Alerta Heads-Up sonora y vibrante en CHANNEL_GOAL_ID
            val goalAlertNotif = MatchNotificationHelper.buildGoalAlertNotification(
                this,
                updatedMatch,
                scoringClub,
                scoringLogo
            )
            notificationManager().notify(notifId + GOAL_ALERT_ID_OFFSET, goalAlertNotif)

            // Actualizacion visual de la tarjeta persistente en CHANNEL_LIVE_ID
            val liveNotifWithGoal = MatchNotificationHelper.buildLiveNotification(
                this,
                updatedMatch,
                goalTeamName = scoringClub,
                homeLogoBitmap = homeLogo,
                awayLogoBitmap = awayLogo
            )
            notificationManager().notify(notifId, liveNotifWithGoal)
        } else if (newState == MatchState.FINISHED) {
            val finishedLiveNotif = MatchNotificationHelper.buildLiveNotification(
                this,
                updatedMatch,
                homeLogoBitmap = homeLogo,
                awayLogoBitmap = awayLogo
            )
            notificationManager().notify(notifId, finishedLiveNotif)

            if (updatedMatch.matchId == currentForegroundMatchId) {
                val nextActive = followRepository.followedMatches.value.values.firstOrNull {
                    it.matchId != updatedMatch.matchId && isEligibleForTracking(it)
                }
                if (nextActive != null) {
                    currentForegroundMatchId = nextActive.matchId
                    val nextNotif = MatchNotificationHelper.buildLiveNotification(this, nextActive)
                    startForegroundCompat(nextActive.matchId.hashCode(), nextNotif)
                } else {
                    currentForegroundMatchId = null
                    runCatching { stopForeground(STOP_FOREGROUND_DETACH) }
                }
            }
        } else {
            val updatedNotif = MatchNotificationHelper.buildLiveNotification(
                this,
                updatedMatch,
                homeLogoBitmap = homeLogo,
                awayLogoBitmap = awayLogo
            )
            notificationManager().notify(notifId, updatedNotif)
        }
    }

    /**
     * Detiene el servicio y libera el primer plano si no restan partidos con seguimiento activo.
     */
    private fun checkAndStopIfNoActiveMatches() {
        val remainingActive = followRepository.followedMatches.value.values.any {
            isEligibleForTracking(it)
        }
        if (!remainingActive) {
            currentForegroundMatchId = null
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
        }
    }

    /**
     * Eleva el servicio al estado Foreground compatible con todas las versiones del sistema operativo.
     */
    private fun startForegroundCompat(notificationId: Int, notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(notificationId, notification)
            }
        }
    }

    private fun notificationManager(): NotificationManager = getSystemService()!!

    override fun onDestroy() {
        isPollingActive = false
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.ixeken.ulama.ACTION_STOP"
        const val ACTION_START_TEST = "com.ixeken.ulama.ACTION_START_TEST"
        const val ACTION_TEST_GOAL = "com.ixeken.ulama.ACTION_TEST_GOAL"
        const val ACTION_TEST_FINISH = "com.ixeken.ulama.ACTION_TEST_FINISH"

        const val EXTRA_MATCH_ID = "extra_match_id"
        const val EXTRA_SCORING_TEAM = "extra_scoring_team"

        private const val GOAL_ALERT_ID_OFFSET = 50000
        private const val FINISHED_ALERT_OFFSET = 60000
        private const val POLL_INTERVAL_LIVE_MS = 35_000L
        private const val POLL_INTERVAL_REST_MS = 60_000L

        /**
         * Metodo seguro para iniciar el servicio en primer plano desde cualquier contexto.
         * Si el sistema restringe el arranque en segundo plano, publica una notificacion estandar
         * de respaldo para evitar que el usuario pierda el aviso del encuentro.
         */
        fun start(context: Context, matchId: String) {
            runCatching {
                val intent = Intent(context, MatchLiveNotificationService::class.java).apply {
                    putExtra(EXTRA_MATCH_ID, matchId)
                }
                ContextCompat.startForegroundService(context, intent)
            }.onFailure {
                MatchFollowRepository.getInstance(context).getFollowed(matchId)?.let { followed ->
                    MatchNotificationHelper.postFallbackKickoffNotification(context, followed)
                }
            }
        }

        /**
         * Solicita la detencion del seguimiento para un partido determinado.
         */
        fun stop(context: Context, matchId: String) {
            runCatching {
                val intent = Intent(context, MatchLiveNotificationService::class.java).apply {
                    action = ACTION_STOP
                    putExtra(EXTRA_MATCH_ID, matchId)
                }
                context.startService(intent)
            }
        }

        /**
         * Inicia inmediatamente una notificacion en vivo de prueba respaldada por Foreground Service.
         */
        fun startTestLive(context: Context, match: com.ixeken.ulama.core.model.FollowedMatch) {
            runCatching {
                val intent = Intent(context, MatchLiveNotificationService::class.java).apply {
                    action = ACTION_START_TEST
                    putExtra(EXTRA_MATCH_ID, match.matchId)
                }
                ContextCompat.startForegroundService(context, intent)
            }
        }

        /**
         * Dispara una alerta de gol y actualiza la tarjeta en primer plano para pruebas.
         */
        fun triggerTestGoal(context: Context, match: com.ixeken.ulama.core.model.FollowedMatch, scoringTeam: String) {
            runCatching {
                val intent = Intent(context, MatchLiveNotificationService::class.java).apply {
                    action = ACTION_TEST_GOAL
                    putExtra(EXTRA_MATCH_ID, match.matchId)
                    putExtra(EXTRA_SCORING_TEAM, scoringTeam)
                }
                ContextCompat.startForegroundService(context, intent)
            }
        }

        /**
         * Dispara la alerta de fin de encuentro y actualiza la notificacion.
         */
        fun triggerTestFinish(context: Context, match: com.ixeken.ulama.core.model.FollowedMatch) {
            runCatching {
                val intent = Intent(context, MatchLiveNotificationService::class.java).apply {
                    action = ACTION_TEST_FINISH
                    putExtra(EXTRA_MATCH_ID, match.matchId)
                }
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }
}
