package com.ixeken.ulama.data.repository

import android.content.Context
import com.ixeken.ulama.R
import com.ixeken.ulama.core.network.NetworkClient
import com.ixeken.ulama.core.model.CutoffLineStyle
import com.ixeken.ulama.core.model.FormResult
import com.ixeken.ulama.core.model.GoalScorer
import com.ixeken.ulama.core.model.GroupStandings
import com.ixeken.ulama.core.model.HeadToHeadMatch
import com.ixeken.ulama.core.model.HeadToHeadSummary
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.LineupPlayer
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchDetail
import com.ixeken.ulama.core.model.MatchLeaderCategory
import com.ixeken.ulama.core.model.MatchPlayerLeader
import com.ixeken.ulama.core.model.MatchStatComparison
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.MatchStatus
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.core.model.TableCutoff
import com.ixeken.ulama.core.model.TableZoneType
import com.ixeken.ulama.core.model.TacticalLine
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.core.model.TeamFormMatch
import com.ixeken.ulama.core.model.TeamLineup
import com.ixeken.ulama.data.espn.EspnApiService
import com.ixeken.ulama.data.espn.model.EspnCompetitorDto
import com.ixeken.ulama.data.espn.model.EspnEventDto
import com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse
import com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto
import com.ixeken.ulama.data.espn.model.EspnRosterTeamDto
import com.ixeken.ulama.data.espn.model.EspnStandingsEntryDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import java.util.concurrent.ConcurrentHashMap

/**
 * Implementacion del repositorio que consulta ESPN y transforma DTOs a modelos de dominio inmutables.
 * Ejecuta el parseo y mapeo estrictamente en Dispatchers.IO para evitar sobrecargar el hilo principal.
 * Mantiene una cache en memoria concurrente para las tablas de posiciones reduciendo la latencia de red a 0ms.
 */
class MatchRepositoryImpl(
    private val apiService: EspnApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MatchRepository {

    private val standingsCache = ConcurrentHashMap<League, List<StandingEntry>>()
    private val groupsCache = ConcurrentHashMap<League, List<GroupStandings>>()
    private val standingsTimestampCache = ConcurrentHashMap<League, Long>()
    private val standingsLocks = ConcurrentHashMap<League, Mutex>()
    private val matchesCache = ConcurrentHashMap<League, Pair<List<Match>, Long>>()
    private val dateMatchesCache = ConcurrentHashMap<String, Pair<List<Match>, Long>>()
    private val matchDetailCache = ConcurrentHashMap<String, Pair<MatchDetail, Long>>()

    private fun lockForLeague(league: League): Mutex =
        standingsLocks.computeIfAbsent(league) { Mutex() }

    override suspend fun getMatches(
        league: League,
        dates: String?,
        forceRefresh: Boolean
    ): Result<List<Match>> = withContext(ioDispatcher) {
        if (dates == null && !forceRefresh) {
            val cached = matchesCache[league]
            if (cached != null && System.currentTimeMillis() - cached.second < 60_000L) {
                return@withContext Result.success(cached.first)
            }
        }

        runCatching {
            val cacheControlHeader = if (forceRefresh) "no-cache" else null
            if (dates != null) {
                val dateKey = "${league.code}_$dates"
                if (!forceRefresh) {
                    val cachedDate = dateMatchesCache[dateKey]
                    if (cachedDate != null && System.currentTimeMillis() - cachedDate.second < 60_000L) {
                        return@runCatching cachedDate.first
                    }
                }
                val response = apiService.getScoreboard(league = league.code, dates = dates, cacheControl = cacheControlHeader)
                val mapped = response.events.mapNotNull { it.toMatch(league) }
                if (mapped.isNotEmpty()) {
                    dateMatchesCache[dateKey] = Pair(mapped, System.currentTimeMillis())
                }
                return@runCatching mapped
            }

            val currentYear = LocalDate.now(ZoneId.systemDefault()).year
            val queryDates = if (league.isTournament) league.defaultTournamentYear(currentYear) else null
            val queryLimit = if (league.isTournament) 200 else null

            val initialResponse = apiService.getScoreboard(
                league = league.code,
                dates = queryDates,
                limit = queryLimit,
                cacheControl = cacheControlHeader
            )
            val initialMatches = initialResponse.events.mapNotNull { it.toMatch(league) }
            val calendar = initialResponse.leagues.firstOrNull()?.extractDateStrings().orEmpty()
            val targetDates = if (league.isTournament) emptyList() else resolveMatchdayDates(calendar)

            val matchesList = if (targetDates.isEmpty() || league.isTournament) {
                initialMatches
            } else {
                val todayBasicIso = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE)
                val clusterMatches = coroutineScope {
                    targetDates.map { dateStr ->
                        async {
                            val dateKey = "${league.code}_$dateStr"
                            val cachedDateEntry = dateMatchesCache[dateKey]
                            // Las jornadas pasadas donde todos los encuentros finalizaron son inmutables:
                            // evitamos repetir descargas de red incluso durante refrescos manuales o silenciosos.
                            if (cachedDateEntry != null &&
                                System.currentTimeMillis() - cachedDateEntry.second < PAST_DATE_CACHE_TTL_MS &&
                                cachedDateEntry.first.isNotEmpty() &&
                                cachedDateEntry.first.all { it.isFinished }
                            ) {
                                return@async cachedDateEntry.first
                            }

                            val isPastDate = dateStr < todayBasicIso
                            val dateCacheControl = if (isPastDate) null else cacheControlHeader
                            runCatching {
                                val dayMatches = apiService.getScoreboard(
                                    league = league.code,
                                    dates = dateStr,
                                    cacheControl = dateCacheControl
                                ).events.mapNotNull { it.toMatch(league) }
                                if (isPastDate && dayMatches.isNotEmpty() && dayMatches.all { it.isFinished }) {
                                    dateMatchesCache[dateKey] = Pair(dayMatches, System.currentTimeMillis())
                                }
                                dayMatches
                            }.getOrDefault(emptyList())
                        }
                    }.awaitAll()
                }.flatten()

                (initialMatches + clusterMatches)
                    .distinctBy { it.id }
                    .sortedBy { it.dateUtc }
            }

            val activeLeagueSeason = initialResponse.leagues.firstOrNull()?.season
            val activeSeasonStartDate = activeLeagueSeason?.startDate
            val activeSeasonYear = activeLeagueSeason?.year

            val sanitizedList = when {
                league.tournamentYear != null -> {
                    // Filtramos partidos historicos anteriores al certamen oficial futuro declarado.
                    matchesList.filter { it.dateUtc >= "${league.tournamentYear}-01-01T00:00:00Z" }
                }
                activeSeasonStartDate != null -> {
                    // Descartamos encuentros de fases o ediciones concluidas antes del inicio oficial de la temporada activa.
                    matchesList.filter { it.dateUtc >= activeSeasonStartDate }
                }
                activeSeasonYear != null -> {
                    // Respaldo de filtrado si la API solo reporta el ano calendario base del certamen.
                    matchesList.filter { it.dateUtc >= "$activeSeasonYear-01-01T00:00:00Z" }
                }
                else -> matchesList
            }

            if (sanitizedList.isNotEmpty()) {
                matchesCache[league] = Pair(sanitizedList, System.currentTimeMillis())
            }
            sanitizedList
        }
    }

    override suspend fun getStandings(league: League, forceRefresh: Boolean): Result<List<StandingEntry>> = withContext(ioDispatcher) {
        fetchStandingsData(league, forceRefresh).map { it.first }
    }

    override suspend fun getGroups(league: League, forceRefresh: Boolean): Result<List<GroupStandings>> = withContext(ioDispatcher) {
        fetchStandingsData(league, forceRefresh).map { it.second }
    }

    /**
     * Consulta el endpoint de posiciones de ESPN resolviendo clasificacion general de liga unica o particion
     * multiseccion de grupos en torneos internacionales, ordenando las posiciones por puntos y criterios oficiales.
     */
    private suspend fun fetchStandingsData(
        league: League,
        forceRefresh: Boolean
    ): Result<Pair<List<StandingEntry>, List<GroupStandings>>> = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val lastFetch = standingsTimestampCache[league] ?: 0L
        if (!forceRefresh || (now - lastFetch) < STANDINGS_DEDUP_WINDOW_MS) {
            val cachedStandings = standingsCache[league]
            val cachedGroups = groupsCache[league]
            if (cachedStandings != null && cachedGroups != null) {
                return@withContext Result.success(Pair(cachedStandings, cachedGroups))
            }
        }

        lockForLeague(league).withLock {
            val nowInside = System.currentTimeMillis()
            val lastFetchInside = standingsTimestampCache[league] ?: 0L
            // Evitamos que llamadas simultaneas a getStandings y getGroups con forceRefresh = true
            // disparen dos descargas de red consecutivas para la misma competicion.
            if (!forceRefresh || (nowInside - lastFetchInside) < STANDINGS_DEDUP_WINDOW_MS) {
                val cachedStandings = standingsCache[league]
                val cachedGroups = groupsCache[league]
                if (cachedStandings != null && cachedGroups != null) {
                    return@withLock Result.success(Pair(cachedStandings, cachedGroups))
                }
            }

            if (league.tournamentYear != null) {
                // Competiciones internacionales que inician su nuevo certamen en una edicion futura:
                // Retornamos tablas vacias para omitir clasificaciones historicas pasadas.
                return@withLock Result.success(Pair(emptyList(), emptyList()))
            }

            runCatching {
                val response = apiService.getStandings(league = league.code)
                val children = response.children

                if (children.size > 1) {
                    val groupCutoffs = listOf(
                        TableCutoff(
                            rankAfter = 2,
                            zoneType = TableZoneType.QUALIFICATION,
                            lineStyle = CutoffLineStyle.SOLID
                        )
                    )

                    val groupsList = children.mapNotNull { child ->
                        val entries = child.standings?.entries.orEmpty().mapNotNull { it.toStandingEntry() }
                        if (entries.isEmpty()) return@mapNotNull null

                        val sortedEntries = entries.sortedWith(
                            compareBy<StandingEntry> { if (it.rank > 0) it.rank else Int.MAX_VALUE }
                                .thenByDescending { it.points }
                                .thenByDescending { it.goalDifference }
                                .thenByDescending { it.goalsFor }
                                .thenBy { it.teamName }
                        ).mapIndexed { index, entry ->
                            entry.copy(rank = if (entry.rank > 0) entry.rank else index + 1)
                        }

                        val rawName = child.name.ifBlank { child.abbreviation.orEmpty() }
                        val groupName = when {
                            rawName.startsWith("Group ", ignoreCase = true) -> rawName.substring(6).trim()
                            rawName.startsWith("Grupo ", ignoreCase = true) -> rawName.substring(6).trim()
                            rawName.startsWith("League ", ignoreCase = true) ->
                                rawName.replace("League ", "Liga ", ignoreCase = true)
                                    .replace(", Group ", " - Grupo ", ignoreCase = true)
                                    .replace("Group ", "Grupo ", ignoreCase = true)
                            else -> rawName
                        }

                        GroupStandings(
                            name = groupName,
                            standings = sortedEntries,
                            cutoffs = if (sortedEntries.size >= 2) groupCutoffs else emptyList(),
                            legend = emptyList()
                        )
                    }

                    val flattenedStandings = groupsList.flatMap { it.standings }
                    groupsCache[league] = groupsList
                    standingsCache[league] = flattenedStandings
                    standingsTimestampCache[league] = System.currentTimeMillis()
                    Pair(flattenedStandings, groupsList)
                } else {
                    val entries = children.firstOrNull()?.standings?.entries.orEmpty()
                    val mapped = entries.mapNotNull { it.toStandingEntry() }
                    val sortedEntries = mapped.sortedWith(
                        compareBy<StandingEntry> { if (it.rank > 0) it.rank else Int.MAX_VALUE }
                            .thenByDescending { it.points }
                            .thenByDescending { it.goalDifference }
                            .thenByDescending { it.goalsFor }
                            .thenBy { it.teamName }
                    ).mapIndexed { index, entry ->
                        entry.copy(rank = if (entry.rank > 0) entry.rank else index + 1)
                    }

                    groupsCache[league] = emptyList()
                    standingsCache[league] = sortedEntries
                    standingsTimestampCache[league] = System.currentTimeMillis()
                    Pair(sortedEntries, emptyList())
                }
            }
        }
    }

    override fun getCachedMatchDetail(matchId: String): MatchDetail? {
        val cached = matchDetailCache[matchId] ?: return null
        val (detail, timestamp) = cached
        val age = System.currentTimeMillis() - timestamp
        val ttl = when {
            detail.isCompleted -> 24 * 3600 * 1000L // 24 horas para partidos concluidos
            detail.stats.isEmpty() -> 15 * 60 * 1000L // 15 minutos para partidos programados
            else -> 20_000L // 20 segundos para partidos en vivo
        }
        return if (age < ttl) detail else null
    }

    override suspend fun getMatchDetail(
        league: League,
        matchId: String,
        forceRefresh: Boolean
    ): Result<MatchDetail> = withContext(ioDispatcher) {
        if (!forceRefresh) {
            val cached = getCachedMatchDetail(matchId)
            if (cached != null) {
                return@withContext Result.success(cached)
            }
        }
        runCatching {
            val response = apiService.getMatchSummary(league = league.code, eventId = matchId)
            val detail = response.toMatchDetail(matchId)
            matchDetailCache[matchId] = Pair(detail, System.currentTimeMillis())
            detail
        }
    }

    private fun EspnMatchSummaryResponse.toMatchDetail(matchId: String): MatchDetail {
        val competition = header?.competitions?.firstOrNull()
        val isCompleted = competition?.status?.type?.completed == true ||
            competition?.status?.type?.state?.equals("post", ignoreCase = true) == true
        val scoringDetails = competition?.details.orEmpty().filter { it.scoringPlay }

        val homeTeamDto = boxscore?.teams?.getOrNull(0)?.team ?: rosters.getOrNull(0)?.team
        val awayTeamDto = boxscore?.teams?.getOrNull(1)?.team ?: rosters.getOrNull(1)?.team
        val homeTeamId = homeTeamDto?.id.orEmpty()
        val awayTeamId = awayTeamDto?.id.orEmpty()

        val homeScorers = scoringDetails
            .filter { it.team?.id == homeTeamId }
            .groupBy { it.participants.firstOrNull()?.athlete?.displayName.orEmpty() }
            .filterKeys { it.isNotBlank() }
            .map { (name, plays) ->
                val minutes = plays.mapNotNull { it.clock?.displayValue?.takeIf { c -> c.isNotBlank() } }
                    .joinToString(", ")
                GoalScorer(playerName = name, minutesDisplay = minutes)
            }

        val awayScorers = scoringDetails
            .filter { it.team?.id == awayTeamId }
            .groupBy { it.participants.firstOrNull()?.athlete?.displayName.orEmpty() }
            .filterKeys { it.isNotBlank() }
            .map { (name, plays) ->
                val minutes = plays.mapNotNull { it.clock?.displayValue?.takeIf { c -> c.isNotBlank() } }
                    .joinToString(", ")
                GoalScorer(playerName = name, minutesDisplay = minutes)
            }

        val homeStatsMap = boxscore?.teams?.getOrNull(0)?.statistics.orEmpty().associateBy { it.name }
        val awayStatsMap = boxscore?.teams?.getOrNull(1)?.statistics.orEmpty().associateBy { it.name }

        val statsList = buildStatsList(homeStatsMap, awayStatsMap)

        val homeRoster = rosters.firstOrNull { it.team.id == homeTeamId } ?: rosters.getOrNull(0)
        val awayRoster = rosters.firstOrNull { it.team.id == awayTeamId } ?: rosters.getOrNull(1)

        val broadcastsList = broadcasts.mapNotNull { b ->
            b.media?.shortName?.takeIf { it.isNotBlank() }
                ?: b.media?.name?.takeIf { it.isNotBlank() }
                ?: b.names.firstOrNull { it.isNotBlank() }
        }.distinct()

        val attendanceCount = gameInfo?.attendance?.takeIf { it > 0 }
        val refereeName = gameInfo?.officials?.firstOrNull()?.let { official ->
            official.displayName.takeIf { it.isNotBlank() }
                ?: official.fullName?.takeIf { it.isNotBlank() }
        }
        val weatherText = gameInfo?.weather?.let { w ->
            listOfNotNull(
                w.displayValue?.takeIf { it.isNotBlank() },
                w.temperature?.let { "$it°" }
            ).joinToString(" · ").takeIf { it.isNotBlank() }
        }

        // Mapeo de Racha Reciente (lastFiveGames)
        val homeLastFive = lastFiveGames.firstOrNull { it.team?.id == homeTeamId }
            ?: lastFiveGames.getOrNull(0)
        val awayLastFive = lastFiveGames.firstOrNull { it.team?.id == awayTeamId }
            ?: lastFiveGames.getOrNull(1)

        val homeRecentForm = homeLastFive?.events.orEmpty().mapNotNull { ev ->
            val res = when (ev.gameResult?.uppercase()) {
                "W" -> FormResult.WIN
                "D" -> FormResult.DRAW
                "L" -> FormResult.LOSS
                else -> null
            } ?: return@mapNotNull null
            TeamFormMatch(
                result = res,
                score = ev.score,
                opponentName = ev.opponent?.shortDisplayName ?: ev.opponent?.displayName,
                dateUtc = ev.gameDate
            )
        }

        val awayRecentForm = awayLastFive?.events.orEmpty().mapNotNull { ev ->
            val res = when (ev.gameResult?.uppercase()) {
                "W" -> FormResult.WIN
                "D" -> FormResult.DRAW
                "L" -> FormResult.LOSS
                else -> null
            } ?: return@mapNotNull null
            TeamFormMatch(
                result = res,
                score = ev.score,
                opponentName = ev.opponent?.shortDisplayName ?: ev.opponent?.displayName,
                dateUtc = ev.gameDate
            )
        }

        // Mapeo de Historial H2H (seasonseries)
        val h2h = seasonseries.firstOrNull()?.let { series ->
            var homeWins = 0
            var awayWins = 0
            var draws = 0

            val h2hMatches = series.events.mapNotNull { ev ->
                val homeComp = ev.competitors.firstOrNull { it.team?.id == homeTeamId }
                    ?: ev.competitors.firstOrNull { it.homeAway == "home" }
                val awayComp = ev.competitors.firstOrNull { it.team?.id == awayTeamId }
                    ?: ev.competitors.firstOrNull { it.homeAway == "away" }

                val hScore = homeComp?.score?.toIntOrNull()
                val aScore = awayComp?.score?.toIntOrNull()

                if (hScore != null && aScore != null) {
                    when {
                        hScore > aScore -> homeWins++
                        aScore > hScore -> awayWins++
                        else -> draws++
                    }
                } else if (homeComp?.winner == true) {
                    homeWins++
                } else if (awayComp?.winner == true) {
                    awayWins++
                }

                HeadToHeadMatch(
                    dateUtc = ev.date,
                    homeScore = homeComp?.score,
                    awayScore = awayComp?.score,
                    homeTeamName = homeComp?.team?.displayName ?: homeComp?.team?.name,
                    awayTeamName = awayComp?.team?.displayName ?: awayComp?.team?.name
                )
            }

            if (h2hMatches.isNotEmpty() || !series.summary.isNullOrBlank()) {
                HeadToHeadSummary(
                    summary = series.summary,
                    homeWins = homeWins,
                    draws = draws,
                    awayWins = awayWins,
                    matches = h2hMatches
                )
            } else null
        }

        // Mapeo de Líderes Individuales del Partido (summary.leaders)
        val homeLeadersDto = leaders.firstOrNull { it.team?.id == homeTeamId } ?: leaders.getOrNull(0)
        val awayLeadersDto = leaders.firstOrNull { it.team?.id == awayTeamId } ?: leaders.getOrNull(1)

        val homeCatMap = homeLeadersDto?.leaders.orEmpty().associateBy { it.name }
        val awayCatMap = awayLeadersDto?.leaders.orEmpty().associateBy { it.name }

        val orderedCategories = listOf("totalShots", "accuratePasses", "defensiveInterventions", "saves")
        val allCategoryKeys = (homeCatMap.keys + awayCatMap.keys).distinct()
        val availableKeys = orderedCategories.filter { it in allCategoryKeys } + (allCategoryKeys - orderedCategories.toSet())

        val matchLeaders = availableKeys.mapNotNull { catKey ->
            val homeCat = homeCatMap[catKey]
            val awayCat = awayCatMap[catKey]
            val homeEntry = homeCat?.leaders?.firstOrNull()
            val awayEntry = awayCat?.leaders?.firstOrNull()

            if (homeEntry == null && awayEntry == null) return@mapNotNull null

            val homeLeader = homeEntry?.let { entry ->
                MatchPlayerLeader(
                    athleteName = entry.athlete?.shortName?.takeIf { it.isNotBlank() }
                        ?: entry.athlete?.displayName.orEmpty(),
                    jersey = entry.athlete?.jersey,
                    valueDisplay = entry.displayValue,
                    statSummary = entry.summary
                )
            }

            val awayLeader = awayEntry?.let { entry ->
                MatchPlayerLeader(
                    athleteName = entry.athlete?.shortName?.takeIf { it.isNotBlank() }
                        ?: entry.athlete?.displayName.orEmpty(),
                    jersey = entry.athlete?.jersey,
                    valueDisplay = entry.displayValue,
                    statSummary = entry.summary
                )
            }

            MatchLeaderCategory(
                categoryKey = catKey,
                displayNameFallback = homeCat?.displayName?.takeIf { it.isNotBlank() }
                    ?: awayCat?.displayName.orEmpty(),
                homeLeader = homeLeader,
                awayLeader = awayLeader
            )
        }

        return MatchDetail(
            matchId = matchId,
            homeScorers = homeScorers,
            awayScorers = awayScorers,
            stats = statsList,
            homeLineup = homeRoster?.toTeamLineup(),
            awayLineup = awayRoster?.toTeamLineup(),
            homeColor = homeTeamDto?.color,
            awayColor = awayTeamDto?.color,
            homeAlternateColor = homeTeamDto?.alternateColor,
            awayAlternateColor = awayTeamDto?.alternateColor,
            broadcasts = broadcastsList,
            attendance = attendanceCount,
            referee = refereeName,
            weather = weatherText,
            homeRecentForm = homeRecentForm,
            awayRecentForm = awayRecentForm,
            headToHead = h2h,
            leaders = matchLeaders,
            isCompleted = isCompleted
        )
    }

    private fun buildStatsList(
        homeMap: Map<String, com.ixeken.ulama.data.espn.model.EspnStatisticItemDto>,
        awayMap: Map<String, com.ixeken.ulama.data.espn.model.EspnStatisticItemDto>
    ): List<MatchStatComparison> {
        val statConfigs = listOf(
            Triple("possessionPct", R.string.stat_possession, true),
            Triple("totalShots", R.string.stat_shots, false),
            Triple("shotsOnTarget", R.string.stat_shots_on_target, false),
            Triple("wonCorners", R.string.stat_corners, false),
            Triple("totalPasses", R.string.stat_passes, false),
            Triple("passPct", R.string.stat_pass_accuracy, true),
            Triple("effectiveTackles", R.string.stat_effective_tackles, false),
            Triple("interceptions", R.string.stat_interceptions, false),
            Triple("totalClearance", R.string.stat_clearances, false),
            Triple("blockedShots", R.string.stat_blocked_shots, false),
            Triple("saves", R.string.stat_saves, false),
            Triple("offsides", R.string.stat_offsides, false),
            Triple("foulsCommitted", R.string.stat_fouls, false),
            Triple("yellowCards", R.string.stat_yellow_cards, false),
            Triple("redCards", R.string.stat_red_cards, false)
        )

        return statConfigs.mapNotNull { (key, labelRes, isPercent) ->
            val homeItem = homeMap[key]
            val awayItem = awayMap[key]
            if (homeItem == null && awayItem == null) return@mapNotNull null

            val homeRaw = homeItem?.displayValue?.toDoubleOrNull() ?: 0.0
            val awayRaw = awayItem?.displayValue?.toDoubleOrNull() ?: 0.0

            val (homeText, awayText, homeRatio, awayRatio) = if (isPercent) {
                val hVal = if (homeRaw <= 1.0 && homeRaw > 0.0) (homeRaw * 100).toInt() else homeRaw.toInt()
                val aVal = if (awayRaw <= 1.0 && awayRaw > 0.0) (awayRaw * 100).toInt() else awayRaw.toInt()
                val total = (hVal + aVal).coerceAtLeast(1).toFloat()
                Quadruple("$hVal%", "$aVal%", hVal / total, aVal / total)
            } else {
                val total = (homeRaw + awayRaw).coerceAtLeast(1.0).toFloat()
                Quadruple(
                    homeItem?.displayValue ?: homeRaw.toInt().toString(),
                    awayItem?.displayValue ?: awayRaw.toInt().toString(),
                    homeRaw.toFloat() / total,
                    awayRaw.toFloat() / total
                )
            }

            MatchStatComparison(
                labelRes = labelRes,
                homeDisplayValue = homeText,
                awayDisplayValue = awayText,
                homeRatio = homeRatio,
                awayRatio = awayRatio
            )
        }
    }

    private fun EspnRosterTeamDto.toTeamLineup(): TeamLineup {
        val startersList = roster.filter { it.starter }.mapIndexed { index, p -> p.toLineupPlayer(index) }
        val benchList = roster.filter { !it.starter }.mapIndexed { index, p -> p.toLineupPlayer(index) }
        val resolvedShortName = team.shortDisplayName?.takeIf { it.isNotBlank() }
            ?: team.abbreviation?.takeIf { it.isNotBlank() }
            ?: team.name
        return TeamLineup(
            teamId = team.id,
            teamName = team.displayName.ifBlank { team.name },
            shortName = resolvedShortName,
            formation = formation ?: "4-4-2",
            teamColor = team.color,
            teamAlternateColor = team.alternateColor,
            starters = startersList,
            bench = benchList
        )
    }

    private fun EspnRosterPlayerItemDto.toLineupPlayer(index: Int): LineupPlayer {
        val posName = position?.name.orEmpty()
        val slot = formationPlace ?: (index + 1)
        val line = when {
            slot == 1 || posName.contains("Goalkeeper", ignoreCase = true) || posName.contains("Portero", ignoreCase = true) -> TacticalLine.GOALKEEPER
            posName.contains("Defender", ignoreCase = true) || posName.contains("Back", ignoreCase = true) || slot in 2..5 -> TacticalLine.DEFENSE
            posName.contains("Midfield", ignoreCase = true) || slot in 6..9 -> TacticalLine.MIDFIELD
            else -> TacticalLine.FORWARD
        }
        val subInMinute = if (subbedIn) {
            plays.firstOrNull { it.substitution }?.clock?.displayValue?.takeIf { it.isNotBlank() }
        } else null
        val subForName = if (subbedIn) {
            subbedInFor?.athlete?.shortName?.takeIf { it.isNotBlank() }
                ?: subbedInFor?.athlete?.displayName?.takeIf { it.isNotBlank() }
        } else null
        val subForJersey = if (subbedIn) subbedInFor?.jersey else null

        return LineupPlayer(
            id = athlete?.id ?: "$slot",
            name = athlete?.shortName?.takeIf { it.isNotBlank() } ?: athlete?.displayName.orEmpty(),
            jersey = jersey ?: "$slot",
            position = posName,
            formationPlace = slot,
            line = line,
            subbedIn = subbedIn,
            subInMinute = subInMinute,
            subbedInForName = subForName,
            subbedInForJersey = subForJersey
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun EspnEventDto.toMatch(league: League = League.LIGA_MX): Match? {
        val competition = competitions.firstOrNull() ?: return null
        val homeCompetitor = competition.competitors.firstOrNull { it.homeAway == "home" } ?: return null
        val awayCompetitor = competition.competitors.firstOrNull { it.homeAway == "away" } ?: return null

        val homeTeam = homeCompetitor.toTeam(isHome = true)
        val awayTeam = awayCompetitor.toTeam(isHome = false)

        val statusDto = competition.status ?: status
        val period = statusDto?.period ?: 0
        val typeName = statusDto?.type?.name.orEmpty()
        val typeDesc = statusDto?.type?.description.orEmpty()
        val detail = statusDto?.type?.shortDetail.takeIf { !it.isNullOrBlank() }
            ?: statusDto?.type?.detail.orEmpty()
        val displayClock = statusDto?.displayClock ?: ""

        val isCompleted = statusDto?.type?.completed == true ||
            typeName.equals("STATUS_FULL_TIME", ignoreCase = true) ||
            typeName.equals("STATUS_FINAL", ignoreCase = true) ||
            typeName.equals("STATUS_FINAL_AET", ignoreCase = true) ||
            typeName.equals("STATUS_FINAL_PEN", ignoreCase = true) ||
            typeName.equals("STATUS_FULL_TIME_AET", ignoreCase = true) ||
            typeName.equals("STATUS_FULL_TIME_PEN", ignoreCase = true) ||
            statusDto?.type?.state.equals("post", ignoreCase = true) ||
            detail.equals("FT", ignoreCase = true) ||
            detail.equals("AET", ignoreCase = true) ||
            detail.startsWith("FT-", ignoreCase = true)

        val matchState = when {
            isCompleted -> MatchState.FINISHED
            statusDto?.type?.state.equals("pre", ignoreCase = true) -> MatchState.SCHEDULED
            statusDto?.type?.state.equals("in", ignoreCase = true) -> MatchState.LIVE
            else -> MatchState.SCHEDULED
        }

        val isHalftime = matchState == MatchState.LIVE && (
            typeName.equals("STATUS_HALFTIME", ignoreCase = true) ||
            typeName.equals("HALFTIME", ignoreCase = true) ||
            typeDesc.equals("Halftime", ignoreCase = true) ||
            typeDesc.equals("Half Time", ignoreCase = true) ||
            detail.equals("HT", ignoreCase = true) ||
            detail.equals("MT", ignoreCase = true) ||
            detail.equals("Halftime", ignoreCase = true) ||
            detail.equals("Half Time", ignoreCase = true) ||
            detail.equals("Medio tiempo", ignoreCase = true) ||
            detail.equals("Descanso", ignoreCase = true) ||
            displayClock.equals("HT", ignoreCase = true) ||
            displayClock.equals("MT", ignoreCase = true)
        )

        // Tiempos extras en vivo (1TE o 2TE)
        val isLiveExtraTime = matchState == MatchState.LIVE && (
            period in 3..4 ||
            typeName.contains("EXTRA_TIME", ignoreCase = true) ||
            typeDesc.contains("Extra Time", ignoreCase = true) ||
            detail.contains("1ET", ignoreCase = true) ||
            detail.contains("2ET", ignoreCase = true) ||
            detail.contains("AET", ignoreCase = true) ||
            displayClock.contains("ET", ignoreCase = true) ||
            displayClock.contains("TE", ignoreCase = true)
        )

        // Tanda de penales en vivo
        val isLiveShootout = matchState == MatchState.LIVE && (
            period == 5 ||
            typeName.contains("SHOOTOUT", ignoreCase = true) ||
            typeDesc.contains("Shootout", ignoreCase = true) ||
            detail.contains("Pens", ignoreCase = true) ||
            detail.contains("Shootout", ignoreCase = true)
        )

        // Fallback de parseo de penales si el texto en detail incluye tanteo
        val (fallbackHomeShootout, fallbackAwayShootout) = extractShootoutScores(detail, typeDesc)
        val finalHomeShootout = homeTeam.shootoutScore ?: fallbackHomeShootout
        val finalAwayShootout = awayTeam.shootoutScore ?: fallbackAwayShootout

        val resolvedHomeTeam = if (homeTeam.shootoutScore != finalHomeShootout) {
            homeTeam.copy(shootoutScore = finalHomeShootout)
        } else homeTeam

        val resolvedAwayTeam = if (awayTeam.shootoutScore != finalAwayShootout) {
            awayTeam.copy(shootoutScore = finalAwayShootout)
        } else awayTeam

        // Finalizado en tanda de penales
        val isFinishedPenalties = isCompleted && (
            typeName.contains("PEN", ignoreCase = true) ||
            typeName.contains("SHOOTOUT", ignoreCase = true) ||
            typeDesc.contains("Pens", ignoreCase = true) ||
            typeDesc.contains("Penalties", ignoreCase = true) ||
            typeDesc.contains("Shootout", ignoreCase = true) ||
            detail.contains("Pens", ignoreCase = true) ||
            detail.contains("Penalties", ignoreCase = true) ||
            (finalHomeShootout != null && finalAwayShootout != null)
        )

        // Finalizado tras tiempos extras
        val isFinishedAet = isCompleted && !isFinishedPenalties && (
            typeName.contains("AET", ignoreCase = true) ||
            detail.equals("AET", ignoreCase = true) ||
            detail.contains("AET", ignoreCase = true) ||
            typeDesc.contains("AET", ignoreCase = true) ||
            typeDesc.contains("Extra Time", ignoreCase = true)
        )

        // Metadatos de eliminatoria directa, serie de ida y vuelta y marcador global
        val series = competition.series
        val leg = competition.leg
        val rawSeriesTitle = series?.title?.ifBlank { null }
        val seasonSlug = season?.slug?.ifBlank { null }
        val noteHeadline = competition.notes.firstOrNull()?.headline?.ifBlank { null }

        val seriesTitle = when {
            !rawSeriesTitle.isNullOrBlank() &&
                !rawSeriesTitle.equals("regular-season", ignoreCase = true) &&
                !rawSeriesTitle.equals("regular season", ignoreCase = true) &&
                !rawSeriesTitle.equals("group-stage", ignoreCase = true) &&
                !rawSeriesTitle.equals("group stage", ignoreCase = true) &&
                !rawSeriesTitle.equals("league-phase", ignoreCase = true) &&
                !rawSeriesTitle.equals("league phase", ignoreCase = true) -> rawSeriesTitle
            !seasonSlug.isNullOrBlank() -> {
                when (seasonSlug.lowercase()) {
                    "final" -> "final"
                    "3rd-place-match", "third-place-match" -> "3rd-place-match"
                    "semifinals", "semi-finals", "semifinal", "semi-final" -> "semifinals"
                    "quarterfinals", "quarter-finals", "quarterfinal", "quarter-final" -> "quarterfinals"
                    "round-of-16" -> "round-of-16"
                    "round-of-32" -> "round-of-32"
                    "torneo-apertura", "apertura" -> "torneo-apertura"
                    "torneo-clausura", "clausura" -> "torneo-clausura"
                    else -> null
                }
            }
            !noteHeadline.isNullOrBlank() && (noteHeadline.contains("final", ignoreCase = true) ||
                noteHeadline.contains("semi", ignoreCase = true) ||
                noteHeadline.contains("quarter", ignoreCase = true) ||
                noteHeadline.contains("cuart", ignoreCase = true) ||
                noteHeadline.contains("round of", ignoreCase = true)) -> noteHeadline
            else -> null
        }

        val legNumber = leg?.value
        val legText = leg?.displayValue?.ifBlank { null }
        val aggregateHome = series?.competitors?.find { it.id == homeCompetitor.id }?.aggregateScore?.toInt()
        val aggregateAway = series?.competitors?.find { it.id == awayCompetitor.id }?.aggregateScore?.toInt()
        val seriesSummary = competition.notes.firstOrNull()?.text?.ifBlank { null }

        return Match(
            id = id,
            dateUtc = date,
            homeTeam = resolvedHomeTeam,
            awayTeam = resolvedAwayTeam,
            status = MatchStatus(
                state = matchState,
                displayClock = displayClock,
                detail = detail,
                isCompleted = isCompleted,
                isHalftime = isHalftime,
                isExtraTime = isLiveExtraTime,
                isShootout = isLiveShootout,
                isFinishedAet = isFinishedAet,
                isFinishedPenalties = isFinishedPenalties
            ),
            venueName = competition.venue?.fullName,
            league = league,
            seriesTitle = seriesTitle,
            legNumber = legNumber,
            legText = legText,
            aggregateHomeScore = aggregateHome,
            aggregateAwayScore = aggregateAway,
            seriesSummary = seriesSummary
        )
    }

    private fun extractShootoutScores(detail: String, description: String): Pair<Int?, Int?> {
        val combined = "$detail $description"
        val isPenaltyContext = combined.contains("pen", ignoreCase = true) ||
            combined.contains("shootout", ignoreCase = true)

        if (!isPenaltyContext) return Pair(null, null)

        // Patron 1: Formato entre parentesis ej. "FT-Pens (3-5)" o "(4-2 pen.)"
        val parensRegex = Regex("""\(\s*(\d+)\s*[-–]\s*(\d+)\s*(?:pen|pens)?\.?\s*\)""", RegexOption.IGNORE_CASE)
        val parensMatch = parensRegex.find(combined)
        if (parensMatch != null) {
            val h = parensMatch.groupValues[1].toIntOrNull()
            val a = parensMatch.groupValues[2].toIntOrNull()
            return Pair(h, a)
        }

        // Patron 2: Formato descriptivo ej. "4-2 on penalties" o "4-2 pens"
        val trailingRegex = Regex("""(\d+)\s*[-–]\s*(\d+)\s*(?:on\s+)?(?:pens?|penalties)""", RegexOption.IGNORE_CASE)
        val trailingMatch = trailingRegex.find(combined)
        if (trailingMatch != null) {
            val h = trailingMatch.groupValues[1].toIntOrNull()
            val a = trailingMatch.groupValues[2].toIntOrNull()
            return Pair(h, a)
        }

        return Pair(null, null)
    }

    private fun EspnCompetitorDto.toTeam(isHome: Boolean): Team {
        val totalRecord = records.firstOrNull { it.type == "total" }?.summary
            ?: records.firstOrNull()?.summary

        val extractedShootout = shootoutScore
            ?: linescores.firstOrNull { it.period == 5 }?.let { it.displayValue?.toIntOrNull() ?: it.value?.toInt() }

        return Team(
            id = id,
            name = team.name.ifBlank { team.displayName },
            shortName = team.shortDisplayName ?: team.name,
            abbreviation = team.abbreviation ?: team.name.take(3).uppercase(),
            logoUrl = team.logo,
            isHome = isHome,
            score = score?.toIntOrNull(),
            isWinner = winner,
            record = totalRecord,
            color = team.color,
            alternateColor = team.alternateColor,
            shootoutScore = extractedShootout
        )
    }

    private fun EspnStandingsEntryDto.toStandingEntry(): StandingEntry? {
        val statsMap = stats.associateBy { it.name }

        val rank = statsMap["rank"]?.value?.toInt()
            ?: statsMap["rank"]?.displayValue?.toIntOrNull()
            ?: 0

        val points = statsMap["points"]?.value?.toInt()
            ?: statsMap["points"]?.displayValue?.toIntOrNull()
            ?: 0

        val gamesPlayed = statsMap["gamesPlayed"]?.value?.toInt()
            ?: statsMap["gamesPlayed"]?.displayValue?.toIntOrNull()
            ?: 0

        val wins = statsMap["wins"]?.value?.toInt()
            ?: statsMap["wins"]?.displayValue?.toIntOrNull()
            ?: 0

        val draws = statsMap["ties"]?.value?.toInt()
            ?: statsMap["ties"]?.displayValue?.toIntOrNull()
            ?: 0

        val losses = statsMap["losses"]?.value?.toInt()
            ?: statsMap["losses"]?.displayValue?.toIntOrNull()
            ?: 0

        val goalsFor = statsMap["pointsFor"]?.value?.toInt()
            ?: statsMap["pointsFor"]?.displayValue?.toIntOrNull()
            ?: 0

        val goalsAgainst = statsMap["pointsAgainst"]?.value?.toInt()
            ?: statsMap["pointsAgainst"]?.displayValue?.toIntOrNull()
            ?: 0

        val goalDifference = statsMap["pointDifferential"]?.value?.toInt()
            ?: statsMap["pointDifferential"]?.displayValue?.toIntOrNull()
            ?: (goalsFor - goalsAgainst)

        return StandingEntry(
            rank = rank,
            teamName = team.displayName.ifBlank { team.name },
            teamShortName = team.shortDisplayName ?: team.name,
            teamAbbreviation = team.abbreviation ?: team.name.take(3).uppercase(),
            logoUrl = team.logos.firstOrNull()?.href,
            gamesPlayed = gamesPlayed,
            wins = wins,
            draws = draws,
            losses = losses,
            goalsFor = goalsFor,
            goalsAgainst = goalsAgainst,
            goalDifference = goalDifference,
            points = points,
            teamId = team.id
        )
    }

    companion object {
        private const val STANDINGS_DEDUP_WINDOW_MS = 5_000L
        private const val PAST_DATE_CACHE_TTL_MS = 24 * 3600 * 1000L

        @Volatile
        private var instance: MatchRepositoryImpl? = null

        /**
         * Provee una instancia singleton compartida entre ViewModel y servicios de segundo plano
         * para reutilizar las caches concurrentes en memoria durante toda la sesion del proceso.
         */
        fun getInstance(context: Context): MatchRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: MatchRepositoryImpl(
                    apiService = NetworkClient.createEspnApiService(context.applicationContext)
                ).also { instance = it }
            }
        }

        /**
         * Identifica los bloques o jornadas de partidos (fin de semana o jornada doble)
         * agrupando dias contiguos de juego (con diferencia de a lo sumo 1 dia).
         */
        internal fun resolveMatchdayDates(
            calendarDates: List<String>,
            today: LocalDate = LocalDate.now(ZoneId.systemDefault())
        ): List<String> {
            val dates = calendarDates.mapNotNull { parseCalendarDate(it) }.distinct().sorted()
            if (dates.isEmpty()) {
                // Fallback para torneos como Champions League cuyo endpoint en ESPN no expone un arreglo plano de fechas.
                // Provee una ventana semanal simetrica alrededor de hoy para capturar la jornada completa.
                val fallbackDates = (-3L..3L).map { today.plusDays(it) }
                val formatter = DateTimeFormatter.BASIC_ISO_DATE
                return fallbackDates.map { it.format(formatter) }
            }

            val upcomingDates = dates.filter { it >= today }
            val pastDates = dates.filter { it < today }

            val upcomingCluster = mutableListOf<LocalDate>()
            var prevUpcoming: LocalDate? = null
            for (d in upcomingDates) {
                if (prevUpcoming == null) {
                    upcomingCluster.add(d)
                    prevUpcoming = d
                } else {
                    val gap = ChronoUnit.DAYS.between(prevUpcoming, d)
                    if (gap <= 1L) {
                        upcomingCluster.add(d)
                        prevUpcoming = d
                    } else {
                        break
                    }
                }
            }

            val pastCluster = mutableListOf<LocalDate>()
            var nextPast: LocalDate? = null
            for (d in pastDates.reversed()) {
                if (nextPast == null) {
                    pastCluster.add(d)
                    nextPast = d
                } else {
                    val gap = ChronoUnit.DAYS.between(d, nextPast)
                    if (gap <= 1L) {
                        pastCluster.add(d)
                        nextPast = d
                    } else {
                        break
                    }
                }
            }

            val formatter = DateTimeFormatter.BASIC_ISO_DATE
            return (pastCluster + upcomingCluster).distinct().sorted().map { it.format(formatter) }
        }

        private fun parseCalendarDate(iso: String): LocalDate? {
            return runCatching {
                if (iso.length >= 10) {
                    LocalDate.parse(iso.substring(0, 10))
                } else {
                    null
                }
            }.getOrNull()
        }
    }
}
