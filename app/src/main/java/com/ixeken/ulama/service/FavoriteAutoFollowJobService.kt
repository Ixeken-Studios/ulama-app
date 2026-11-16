package com.ixeken.ulama.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.core.content.getSystemService
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.data.repository.AppSettingsRepositoryImpl
import com.ixeken.ulama.data.repository.FavoriteTeamsRepositoryImpl
import com.ixeken.ulama.data.repository.MatchFollowRepository
import com.ixeken.ulama.data.repository.MatchRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Trabajo periodico nativo del sistema operativo para descubrir y agendar partidos
 * de equipos favoritos aunque el usuario no haya abierto la aplicacion recientemente.
 */
class FavoriteAutoFollowJobService : JobService() {

    private val jobScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        val settings = AppSettingsRepositoryImpl.provide(applicationContext).settings.value
        val favIds = FavoriteTeamsRepositoryImpl.getInstance(applicationContext).favoriteTeamIds.value
        if (!settings.autoFollowFavorites || favIds.isEmpty()) {
            cancel(applicationContext)
            return false
        }

        jobScope.launch {
            var needsReschedule = false
            try {
                MatchFollowRepository.getInstance(applicationContext).pruneFinishedOlderThan()
                val repository = MatchRepositoryImpl.getInstance(applicationContext)
                val targetLeagues = League.standardLeagues.filter { it.tournamentYear == null }
                val allMatches = coroutineScope {
                    targetLeagues.map { league ->
                        async {
                            repository.getMatches(league, forceRefresh = false).getOrDefault(emptyList())
                        }
                    }.awaitAll()
                }.flatten()
                FavoriteAutoFollowSync.syncAndSchedule(
                    context = applicationContext,
                    matches = allMatches,
                    fromBackground = true
                )
            } catch (_: Exception) {
                needsReschedule = true
            } finally {
                jobFinished(params, needsReschedule)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        jobScope.cancel()
        return true
    }

    override fun onDestroy() {
        jobScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val JOB_ID = 9401
        private const val PERIODIC_INTERVAL_MS = 12L * 60L * 60L * 1000L

        /**
         * Registra o cancela el escaneo periodico segun el estado de auto-seguimiento y favoritos.
         */
        fun syncSchedule(context: Context) {
            val settings = AppSettingsRepositoryImpl.provide(context).settings.value
            val favIds = FavoriteTeamsRepositoryImpl.getInstance(context).favoriteTeamIds.value
            if (!settings.autoFollowFavorites || favIds.isEmpty()) {
                cancel(context)
                return
            }

            val scheduler = context.getSystemService<JobScheduler>() ?: return
            if (scheduler.getPendingJob(JOB_ID) != null) return

            val component = ComponentName(context, FavoriteAutoFollowJobService::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIODIC_INTERVAL_MS)
                .setPersisted(true)
                .build()

            runCatching { scheduler.schedule(jobInfo) }
        }

        /**
         * Cancela el trabajo periodico cuando el usuario desactiva el seguimiento automatico.
         */
        fun cancel(context: Context) {
            runCatching {
                context.getSystemService<JobScheduler>()?.cancel(JOB_ID)
            }
        }
    }
}
