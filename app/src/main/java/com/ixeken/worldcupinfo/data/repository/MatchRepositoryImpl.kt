package com.ixeken.worldcupinfo.data.repository

import android.util.Log

import com.ixeken.worldcupinfo.data.database.MatchDao
import com.ixeken.worldcupinfo.data.mapper.toDomain
import com.ixeken.worldcupinfo.data.mapper.toEntity
import com.ixeken.worldcupinfo.data.mapper.MatchDtoMapper
import com.ixeken.worldcupinfo.data.remote.api.MatchApiService
import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.model.Prediction
import com.ixeken.worldcupinfo.domain.model.MatchStatus
import com.ixeken.worldcupinfo.domain.model.MatchStage
import com.ixeken.worldcupinfo.data.database.entities.MatchEntity
import com.ixeken.worldcupinfo.domain.repository.MatchRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación concreta del repositorio de partidos y quinielas.
 */
@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val matchDao: MatchDao,
    private val apiService: MatchApiService
) : MatchRepository {

    companion object {
        private const val TAG = "MatchRepositoryImpl"
    }

    override fun getMatchesFlow(): Flow<List<Match>> {
        return matchDao.getMatchesWithPredictionsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveMatches(matches: List<Match>) {
        val entities = matches.map { it.toEntity() }
        matchDao.insertMatches(entities)
    }

    /**
     * Sincroniza los datos del fixture con la fuente remota y actualiza el almacenamiento local.
     */
    override suspend fun syncFixture() {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.fetchWorldCupFixture()
                val activeAlarmIds = matchDao.getActiveAlarmMatchIds().toSet()
                val localMatches = matchDao.getMatches().associateBy { it.id }

                val entities = MatchDtoMapper.mapList(response.matches).map { remoteEntity ->
                    val local = localMatches[remoteEntity.id]
                    var finalEntity = remoteEntity

                    if (remoteEntity.id in activeAlarmIds) {
                        finalEntity = finalEntity.copy(isAlarmActive = true)
                    }

                    if (local != null) {

                        val finalStatus = when {
                            remoteEntity.status == "FINISHED" -> "FINISHED"
                            local.status == "FINISHED" -> "FINISHED"
                            local.status == "LIVE" || local.status == "HALFTIME" || local.status == "PENALTIES" -> local.status
                            else -> remoteEntity.status
                        }

                        val finalGoalsA = if (remoteEntity.status == "FINISHED") (remoteEntity.goalsA ?: local.goalsA) else (local.goalsA ?: remoteEntity.goalsA)
                        val finalGoalsB = if (remoteEntity.status == "FINISHED") (remoteEntity.goalsB ?: local.goalsB) else (local.goalsB ?: remoteEntity.goalsB)
                        val finalEventsDetailsA = if (remoteEntity.status == "FINISHED") (remoteEntity.eventsDetailsA ?: local.eventsDetailsA) else (local.eventsDetailsA ?: remoteEntity.eventsDetailsA)
                        val finalEventsDetailsB = if (remoteEntity.status == "FINISHED") (remoteEntity.eventsDetailsB ?: local.eventsDetailsB) else (local.eventsDetailsB ?: remoteEntity.eventsDetailsB)

                        finalEntity = finalEntity.copy(
                            status = finalStatus,
                            goalsA = finalGoalsA,
                            goalsB = finalGoalsB,
                            eventsDetailsA = finalEventsDetailsA,
                            eventsDetailsB = finalEventsDetailsB,
                            liveMinute = if (finalStatus == "FINISHED") null else (local.liveMinute ?: finalEntity.liveMinute),
                            attendance = local.attendance ?: remoteEntity.attendance,
                            teamAColor = local.teamAColor ?: remoteEntity.teamAColor,
                            teamBColor = local.teamBColor ?: remoteEntity.teamBColor,
                            stats = local.stats ?: remoteEntity.stats
                        )
                    }
                    finalEntity
                }
                matchDao.insertMatches(entities)
                
                // Intentar sincronizar los marcadores y eventos en vivo de hoy desde ESPN para pre-cachearlos
                try {
                    syncLiveScores(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error pre-caching today's live scores", e)
                }
            } catch (e: Exception) {
                // Fallback silencioso si no hay conexión o falla la red
                Log.e(TAG, "Error syncing fixture", e)
            }
        }
    }

    override suspend fun syncLiveScores(dateString: String?) {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.fetchEspnScoreboard(dateString)
                val events = response.events ?: return@withContext
                val localMatches = matchDao.getMatches().toMutableList()
                val updatedEntities = mutableListOf<MatchEntity>()
                val gson = Gson()

                // Helper to extract a statistic value
                fun getStatValue(stats: List<com.ixeken.worldcupinfo.data.remote.dto.EspnStatistic>?, nameKey: String): Int? {
                    val stat = stats?.find {
                        it.name?.equals(nameKey, ignoreCase = true) == true ||
                        it.abbreviation?.equals(nameKey, ignoreCase = true) == true
                    }
                    val cleanValue = stat?.displayValue?.replace("%", "")?.trim()
                    return cleanValue?.toDoubleOrNull()?.toInt() ?: cleanValue?.toIntOrNull()
                }

                for (event in events) {
                    val comps = event.competitions ?: continue
                    if (comps.isEmpty()) continue
                    val comp = comps[0]
                    val competitors = comp.competitors ?: continue
                    if (competitors.size < 2) continue

                    val competitorHome = competitors.find { it.homeAway == "home" } ?: competitors[0]
                    val competitorAway = competitors.find { it.homeAway == "away" } ?: competitors[1]

                    val abbrHome = competitorHome.team?.abbreviation?.trim() ?: ""
                    val abbrAway = competitorAway.team?.abbreviation?.trim() ?: ""
                    if (abbrHome.isEmpty() || abbrAway.isEmpty()) continue

                    val localMatch = localMatches.find {
                        (it.teamA.equals(abbrHome, ignoreCase = true) && it.teamB.equals(abbrAway, ignoreCase = true)) ||
                        (it.teamA.equals(abbrAway, ignoreCase = true) && it.teamB.equals(abbrHome, ignoreCase = true))
                    }

                    if (localMatch != null) {
                        val isSwapped = localMatch.teamA.equals(abbrAway, ignoreCase = true)
                        val espnGoalsA = if (isSwapped) competitorAway.score else competitorHome.score
                        val espnGoalsB = if (isSwapped) competitorHome.score else competitorAway.score

                        val goalsAInt = espnGoalsA?.toIntOrNull()
                        val goalsBInt = espnGoalsB?.toIntOrNull()

                        val details = comp.details ?: emptyList()
                        val teamAEventsList = mutableListOf<com.ixeken.worldcupinfo.domain.model.MatchEvent>()
                        val teamBEventsList = mutableListOf<com.ixeken.worldcupinfo.domain.model.MatchEvent>()

                        val homeTeamId = competitorHome.team?.id ?: ""
                        val awayTeamId = competitorAway.team?.id ?: ""

                        for (detail in details) {
                            val athleteName = detail.athletesInvolved?.firstOrNull()?.displayName ?: ""
                            val minute = detail.clock?.displayValue?.replace("'", "") ?: ""
                            if (athleteName.isNotEmpty()) {
                                val eventType = when {
                                    detail.yellowCard == true || detail.type?.text?.lowercase()?.contains("yellow card") == true ->
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.YELLOW_CARD
                                    detail.redCard == true || detail.type?.text?.lowercase()?.contains("red card") == true ->
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.RED_CARD
                                    detail.penaltyKick == true || detail.type?.text?.lowercase()?.contains("penalty") == true ->
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.PENALTY
                                    detail.ownGoal == true || detail.type?.text?.lowercase()?.contains("own goal") == true ->
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.OWN_GOAL
                                    detail.scoringPlay == true ->
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.GOAL
                                    else -> null
                                }
                                if (eventType != null) {
                                    val eventObj = com.ixeken.worldcupinfo.domain.model.MatchEvent(athleteName, minute, eventType)
                                    val detailTeamId = detail.team?.id ?: ""

                                    if (isSwapped) {
                                        if (detailTeamId == homeTeamId) {
                                            teamBEventsList.add(eventObj)
                                        } else if (detailTeamId == awayTeamId) {
                                            teamAEventsList.add(eventObj)
                                        }
                                    } else {
                                        if (detailTeamId == homeTeamId) {
                                            teamAEventsList.add(eventObj)
                                        } else if (detailTeamId == awayTeamId) {
                                            teamBEventsList.add(eventObj)
                                        }
                                    }
                                }
                            }
                        }

                        // Extract team colors
                        val rawColorHome = competitorHome.team?.color
                        val rawColorAway = competitorAway.team?.color
                        val colorHome = rawColorHome?.let { if (it.startsWith("#")) it else "#$it" }
                        val colorAway = rawColorAway?.let { if (it.startsWith("#")) it else "#$it" }

                        val teamAColor = if (isSwapped) colorAway else colorHome
                        val teamBColor = if (isSwapped) colorHome else colorAway

                        // Extract attendance
                        val attendance = comp.attendance

                        // Extract stats
                        val homeStatsList = competitorHome.statistics
                        val awayStatsList = competitorAway.statistics

                        val possessionHome = getStatValue(homeStatsList, "possessionPct")
                        val shotsHome = getStatValue(homeStatsList, "totalShots")
                        val shotsOnTargetHome = getStatValue(homeStatsList, "shotsOnTarget")
                        val foulsHome = getStatValue(homeStatsList, "foulsCommitted")
                        val cornersHome = getStatValue(homeStatsList, "wonCorners")

                        val possessionAway = getStatValue(awayStatsList, "possessionPct")
                        val shotsAway = getStatValue(awayStatsList, "totalShots")
                        val shotsOnTargetAway = getStatValue(awayStatsList, "shotsOnTarget")
                        val foulsAway = getStatValue(awayStatsList, "foulsCommitted")
                        val cornersAway = getStatValue(awayStatsList, "wonCorners")

                        val possessionA = if (isSwapped) possessionAway else possessionHome
                        val possessionB = if (isSwapped) possessionHome else possessionAway

                        val shotsA = if (isSwapped) shotsAway else shotsHome
                        val shotsB = if (isSwapped) shotsHome else shotsAway

                        val shotsOnTargetA = if (isSwapped) shotsOnTargetAway else shotsOnTargetHome
                        val shotsOnTargetB = if (isSwapped) shotsOnTargetHome else shotsOnTargetAway

                        val foulsA = if (isSwapped) foulsAway else foulsHome
                        val foulsB = if (isSwapped) foulsHome else foulsAway

                        val cornersA = if (isSwapped) cornersAway else cornersHome
                        val cornersB = if (isSwapped) cornersHome else cornersAway

                        val matchStats = if (possessionA != null || possessionB != null || shotsA != null || shotsB != null) {
                            com.ixeken.worldcupinfo.domain.model.MatchStats(
                                possessionA = possessionA,
                                possessionB = possessionB,
                                shotsA = shotsA,
                                shotsB = shotsB,
                                shotsOnTargetA = shotsOnTargetA,
                                shotsOnTargetB = shotsOnTargetB,
                                foulsA = foulsA,
                                foulsB = foulsB,
                                cornersA = cornersA,
                                cornersB = cornersB
                            )
                        } else {
                            null
                        }

                        if (goalsAInt != null && goalsBInt != null) {
                            val espnState = comp.status?.type?.state ?: "pre"
                            val espnStatusName = comp.status?.type?.name ?: ""
                            val espnStatusDetail = comp.status?.type?.detail ?: ""

                            val updatedStatus = when {
                                espnStatusName == "STATUS_HALFTIME" || espnStatusDetail == "HT" -> MatchStatus.HALFTIME.name
                                espnStatusName == "STATUS_SHOOTOUT" || espnStatusName == "STATUS_PENALTY" || espnStatusDetail == "PEN" || espnStatusDetail == "SO" -> MatchStatus.PENALTIES.name
                                espnState == "post" -> MatchStatus.FINISHED.name
                                espnState == "in" -> MatchStatus.LIVE.name
                                else -> localMatch.status
                            }

                            val displayClock = comp.status?.displayClock

                            val updatedMatch = localMatch.copy(
                                goalsA = goalsAInt,
                                goalsB = goalsBInt,
                                status = updatedStatus,
                                eventsDetailsA = gson.toJson(teamAEventsList),
                                eventsDetailsB = gson.toJson(teamBEventsList),
                                liveMinute = if (updatedStatus == MatchStatus.FINISHED.name) null else displayClock,
                                attendance = attendance,
                                teamAColor = teamAColor,
                                teamBColor = teamBColor,
                                stats = matchStats?.let { gson.toJson(it) }
                            )
                            updatedEntities.add(updatedMatch)

                            // Resolver de forma dinámica los placeholders de la siguiente ronda si el partido finalizó
                            if (updatedStatus == MatchStatus.FINISHED.name && localMatch.stage != MatchStage.GROUPS.name) {
                                val isHomeWinner = competitorHome.winner == true
                                val isAwayWinner = competitorAway.winner == true
                                var winner: String? = null
                                var loser: String? = null

                                if (isHomeWinner) {
                                    winner = if (isSwapped) localMatch.teamB else localMatch.teamA
                                    loser = if (isSwapped) localMatch.teamA else localMatch.teamB
                                } else if (isAwayWinner) {
                                    winner = if (isSwapped) localMatch.teamA else localMatch.teamB
                                    loser = if (isSwapped) localMatch.teamB else localMatch.teamA
                                } else {
                                    if (goalsAInt > goalsBInt) {
                                        winner = localMatch.teamA
                                        loser = localMatch.teamB
                                    } else if (goalsBInt > goalsAInt) {
                                        winner = localMatch.teamB
                                        loser = localMatch.teamA
                                    }
                                }

                                if (winner != null && loser != null) {
                                    val matchNum = localMatch.id.replace("match_", "")
                                    val winnerPlaceholder = "W$matchNum"
                                    val loserPlaceholder = "L$matchNum"
                                    val runnerUpPlaceholder = "RU$matchNum"

                                    for (i in localMatches.indices) {
                                        val m = localMatches[i]
                                        var updatedM = m
                                        var changed = false
                                        if (m.teamA == winnerPlaceholder) {
                                            updatedM = updatedM.copy(teamA = winner)
                                            changed = true
                                        } else if (m.teamA == loserPlaceholder || m.teamA == runnerUpPlaceholder) {
                                            updatedM = updatedM.copy(teamA = loser)
                                            changed = true
                                        }
                                        if (m.teamB == winnerPlaceholder) {
                                            updatedM = updatedM.copy(teamB = winner)
                                            changed = true
                                        } else if (m.teamB == loserPlaceholder || m.teamB == runnerUpPlaceholder) {
                                            updatedM = updatedM.copy(teamB = loser)
                                            changed = true
                                        }

                                        if (changed) {
                                            localMatches[i] = updatedM
                                            val existingIndex = updatedEntities.indexOfFirst { it.id == updatedM.id }
                                            if (existingIndex != -1) {
                                                updatedEntities[existingIndex] = updatedM
                                            } else {
                                                updatedEntities.add(updatedM)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (updatedEntities.isNotEmpty()) {
                    matchDao.insertMatches(updatedEntities)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing live scores from ESPN", e)
            }
        }
    }

    override suspend fun savePrediction(prediction: Prediction) {
        matchDao.upsertPrediction(prediction.toEntity())
    }

    override suspend fun deletePrediction(matchId: String) {
        matchDao.deletePrediction(matchId)
    }

    override suspend fun updateAlarmState(matchId: String, isAlarmActive: Boolean) {
        matchDao.updateAlarmState(matchId, isAlarmActive)
    }

    override suspend fun deleteKnockoutMatches() {
        matchDao.deleteKnockoutMatches()
    }

    override fun getMatchesWithPredictionsOnlyFlow(): Flow<List<Match>> {
        return matchDao.getMatchesWithPredictionsOnlyFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
