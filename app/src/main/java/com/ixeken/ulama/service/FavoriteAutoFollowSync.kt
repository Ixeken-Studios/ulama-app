package com.ixeken.ulama.service

import android.content.Context
import com.ixeken.ulama.core.model.FollowedMatch
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.toFollowedMatch
import com.ixeken.ulama.data.repository.AppSettingsRepositoryImpl
import com.ixeken.ulama.data.repository.FavoriteTeamsRepositoryImpl
import com.ixeken.ulama.data.repository.MatchFollowRepository

/**
 * Sincroniza y reconcilia el seguimiento automatico de partidos para clubes favoritos.
 *
 * Centraliza la logica compartida entre [MatchesViewModel] y [FavoriteAutoFollowJobService]
 * para garantizar que partidos previamente guardados recuperen su alarma o servicio en vivo
 * si fueron reprogramados o si el sistema limpio alarmas anteriores.
 */
object FavoriteAutoFollowSync {

    private const val THREE_HOURS_MS = 3L * 60L * 60L * 1000L

    /**
     * Evalua y sincroniza una lista de partidos con el repositorio de seguimiento y el programador.
     */
    fun syncAndSchedule(
        context: Context,
        matches: List<Match>,
        manuallyUnfollowedIds: Set<String> = emptySet(),
        fromBackground: Boolean = false
    ) {
        val settings = AppSettingsRepositoryImpl.provide(context).settings.value
        val favIds = FavoriteTeamsRepositoryImpl.getInstance(context).favoriteTeamIds.value
        val followRepo = MatchFollowRepository.getInstance(context)

        syncMatches(
            matches = matches,
            favIds = favIds,
            isAutoFollowEnabled = settings.autoFollowFavorites,
            manuallyUnfollowedIds = manuallyUnfollowedIds,
            getExistingFollowed = followRepo::getFollowed,
            saveFollowed = followRepo::follow,
            leadMinutes = settings.notificationLeadMinutes
        ) { followed, shouldStartNow ->
            if (shouldStartNow) {
                if (fromBackground && MatchAlarmScheduler.canScheduleExactAlarms(context)) {
                    MatchAlarmScheduler.scheduleImmediateTrigger(context, followed.matchId)
                } else {
                    MatchLiveNotificationService.start(context, followed.matchId)
                }
            } else if (followed.state == MatchState.SCHEDULED) {
                MatchAlarmScheduler.scheduleKickoff(context, followed, settings.notificationLeadMinutes)
            }
        }
    }

    /**
     * Nucleo puro de reconciliacion desacoplado de Android para verificacion en pruebas unitarias.
     */
    internal fun syncMatches(
        matches: List<Match>,
        favIds: Set<String>,
        isAutoFollowEnabled: Boolean,
        manuallyUnfollowedIds: Set<String>,
        getExistingFollowed: (String) -> FollowedMatch?,
        saveFollowed: (FollowedMatch) -> Unit,
        leadMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
        onActivateTracking: (FollowedMatch, Boolean) -> Unit
    ) {
        if (!isAutoFollowEnabled || favIds.isEmpty()) return

        val leadMillis = leadMinutes.coerceAtLeast(0) * 60L * 1000L
        val candidates = matches.filter { match ->
            !match.isFinished &&
                match.id !in manuallyUnfollowedIds &&
                (match.homeTeam.id in favIds || match.awayTeam.id in favIds)
        }

        for (match in candidates) {
            val updated = match.toFollowedMatch()
            val existing = getExistingFollowed(match.id)

            if (existing == null || hasTrackingMetadataChanged(existing, updated)) {
                saveFollowed(updated)
            }

            when (updated.state) {
                MatchState.LIVE -> onActivateTracking(updated, true)
                MatchState.SCHEDULED -> {
                    val isWithinWindow = updated.startTimeUtcMillis > 0L &&
                        nowMillis >= (updated.startTimeUtcMillis - leadMillis) &&
                        nowMillis < (updated.startTimeUtcMillis + THREE_HOURS_MS)
                    onActivateTracking(updated, isWithinWindow)
                }
                MatchState.FINISHED -> Unit
            }
        }
    }

    private fun hasTrackingMetadataChanged(
        existing: FollowedMatch,
        updated: FollowedMatch
    ): Boolean {
        return existing.state != updated.state ||
            existing.startTimeUtcMillis != updated.startTimeUtcMillis ||
            existing.homeScore != updated.homeScore ||
            existing.awayScore != updated.awayScore ||
            existing.clockDisplay != updated.clockDisplay ||
            existing.periodDisplay != updated.periodDisplay ||
            existing.isHalftime != updated.isHalftime ||
            existing.isExtraTime != updated.isExtraTime ||
            existing.isShootout != updated.isShootout
    }
}
