package com.ixeken.ulama.data.repository

import android.content.Context
import com.ixeken.ulama.R
import com.ixeken.ulama.core.network.NetworkClient
import com.ixeken.ulama.core.model.CutoffLineStyle
import com.ixeken.ulama.core.model.FormResult
import com.ixeken.ulama.core.model.GoalEvent
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
import com.ixeken.ulama.core.model.MatchTimelineEvent
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.core.model.TableCutoff
import com.ixeken.ulama.core.model.TableZoneType
import com.ixeken.ulama.core.model.TacticalLine
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.core.model.sanitizeAggregateScores
import com.ixeken.ulama.core.model.TeamFormMatch
import com.ixeken.ulama.core.model.TeamLineup
import com.ixeken.ulama.core.model.TimelineEventType
import com.ixeken.ulama.data.espn.EspnApiService
import com.ixeken.ulama.data.espn.model.EspnCompetitorDto
import com.ixeken.ulama.data.espn.model.EspnEventDto
import com.ixeken.ulama.data.espn.model.EspnLeaderEntryDto
import com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse
import com.ixeken.ulama.data.espn.model.EspnRosterPlayerItemDto
import com.ixeken.ulama.data.espn.model.EspnRosterTeamDto
import com.ixeken.ulama.data.espn.model.EspnStandingsChildDto
import com.ixeken.ulama.data.espn.model.EspnStandingsEntryDto
import com.ixeken.ulama.data.espn.model.EspnSummaryScoringPlayDto
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

        val result = runCatching {
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
                    .filterNot { it.homeTeam.isPlaceholder && it.awayTeam.isPlaceholder }
                    .sanitizeAggregateScores()
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
            // Si un torneo anual alcanza el tope de 200 eventos, complementamos con las fechas activas del calendario
            // para evitar que las jornadas actuales o proximas queden truncadas al final del ano.
            val shouldResolveCalendar = !league.isTournament || (calendar.isNotEmpty() && initialMatches.size >= 180)
            val targetDates = if (shouldResolveCalendar) resolveMatchdayDates(calendar) else emptyList()

            val matchesList = if (targetDates.isEmpty()) {
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
                                if (dayMatches.isNotEmpty()) {
                                    dateMatchesCache[dateKey] = Pair(dayMatches, System.currentTimeMillis())
                                }
                                dayMatches
                            }.getOrElse { cachedDateEntry?.first.orEmpty() }
                        }
                    }.awaitAll()
                }.flatten()

                (initialMatches + clusterMatches)
                    .distinctBy { it.id }
                    .sortedBy { it.dateUtc }
            }

            // En ligas domesticas con fase eliminatoria (!isTournament), una consulta de jornada contigua
            // puede omitir vueltas separadas por 2+ dias o rondas previas al avanzar a Semifinales/Final.
            // Solo cuando ya existe un partido eliminatorio activo complementamos con la ventana mensual
            // filtrando exclusivamente cruces eliminatorios para no contaminar la pestana Pasados.
            val enrichedMatchesList = if (league.hasKnockoutBracket && !league.isTournament && matchesList.any { it.isKnockout }) {
                val today = LocalDate.now(ZoneId.systemDefault())
                val currentYearMonth = today.format(YEAR_MONTH_FORMATTER)
                val latestRegularDate = matchesList
                    .asSequence()
                    .filter { !it.isKnockout }
                    .mapNotNull { parseCalendarDate(it.dateUtc) }
                    .maxOrNull()
                val knockoutMonths = resolveKnockoutMonths(
                    calendarDates = calendar,
                    latestRegularDate = latestRegularDate,
                    today = today
                )
                if (knockoutMonths.isEmpty()) {
                    matchesList
                } else {
                    val monthlyKnockoutMatches = coroutineScope {
                        knockoutMonths.map { ym ->
                            async {
                                val monthKey = "${league.code}_ko_$ym"
                                val cachedMonth = dateMatchesCache[monthKey]
                                val isPastMonth = ym < currentYearMonth
                                if (cachedMonth != null &&
                                    isPastMonth &&
                                    System.currentTimeMillis() - cachedMonth.second < PAST_DATE_CACHE_TTL_MS &&
                                    cachedMonth.first.all { it.isFinished }
                                ) {
                                    return@async cachedMonth.first
                                }
                                val monthCacheControl = if (isPastMonth) null else cacheControlHeader
                                runCatching {
                                    val koEvents = apiService.getScoreboard(
                                        league = league.code,
                                        dates = ym,
                                        cacheControl = monthCacheControl
                                    ).events
                                        .mapNotNull { it.toMatch(league) }
                                        .filter { it.isKnockout }
                                    if (koEvents.isNotEmpty() || isPastMonth) {
                                        dateMatchesCache[monthKey] = Pair(koEvents, System.currentTimeMillis())
                                    }
                                    koEvents
                                }.getOrElse { cachedMonth?.first.orEmpty() }
                            }
                        }.awaitAll()
                    }.flatten()

                    (matchesList + monthlyKnockoutMatches)
                        .distinctBy { it.id }
                        .sortedBy { it.dateUtc }
                }
            } else {
                matchesList
            }

            val activeLeagueSeason = initialResponse.leagues.firstOrNull()?.season
            val activeSeasonStartDate = activeLeagueSeason?.startDate
            val activeSeasonYear = activeLeagueSeason?.year

            val sanitizedList = when {
                league.tournamentYear != null -> {
                    // Filtramos partidos historicos anteriores al certamen oficial futuro declarado.
                    enrichedMatchesList.filter { it.dateUtc >= "${league.tournamentYear}-01-01T00:00:00Z" }
                }
                activeSeasonStartDate != null -> {
                    // Descartamos encuentros de fases o ediciones concluidas antes del inicio oficial de la temporada activa.
                    enrichedMatchesList.filter { it.dateUtc >= activeSeasonStartDate }
                }
                activeSeasonYear != null -> {
                    // Respaldo de filtrado si la API solo reporta el ano calendario base del certamen.
                    enrichedMatchesList.filter { it.dateUtc >= "$activeSeasonYear-01-01T00:00:00Z" }
                }
                else -> enrichedMatchesList
            }.filterNot { it.homeTeam.isPlaceholder && it.awayTeam.isPlaceholder }
            .sanitizeAggregateScores()

            if (sanitizedList.isNotEmpty()) {
                matchesCache[league] = Pair(sanitizedList, System.currentTimeMillis())
            }
            sanitizedList
        }

        if (result.isFailure && dates == null) {
            val fallbackCached = matchesCache[league]?.first
            if (!fallbackCached.isNullOrEmpty()) {
                return@withContext Result.success(fallbackCached)
            }
        }
        result
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

            val cacheControlHeader = if (forceRefresh) "no-cache" else null
            val result = runCatching {
                val response = apiService.getStandings(league = league.code, cacheControl = cacheControlHeader)
                val targetTournamentYear = league.tournamentYear?.toIntOrNull()
                if (targetTournamentYear != null) {
                    val reportedSeason = response.children.firstNotNullOfOrNull { it.standings?.season }
                        ?: response.standings?.season
                    // Si el torneo inicia en un ano futuro pero ESPN aun devuelve la tabla de una edicion anterior
                    // (season < targetTournamentYear o sin metadato de temporada), omitimos la tabla historica.
                    if (reportedSeason == null || reportedSeason < targetTournamentYear) {
                        groupsCache[league] = emptyList()
                        standingsCache[league] = emptyList()
                        standingsTimestampCache[league] = System.currentTimeMillis()
                        return@runCatching Pair(emptyList<StandingEntry>(), emptyList<GroupStandings>())
                    }
                }
                val flattenedChildren = flattenStandingsChildren(response.children)

                if (league.mergeStandingsGroups && flattenedChildren.size > 1) {
                    val mergedEntries = flattenedChildren
                        .flatMap { (_, rawEntries) -> rawEntries }
                        .mapNotNull { it.toStandingEntry() }
                        .distinctBy { it.teamId.ifBlank { it.teamName } }
                        .map { it.copy(rank = 0) }
                    val sortedMerged = sortStandingEntries(mergedEntries)

                    groupsCache[league] = emptyList()
                    standingsCache[league] = sortedMerged
                    standingsTimestampCache[league] = System.currentTimeMillis()
                    Pair(sortedMerged, emptyList())
                } else if (flattenedChildren.size > 1) {
                    val resolvedGroupCutoffs = league.groupCutoffs.ifEmpty {
                        listOf(
                            TableCutoff(
                                rankAfter = 2,
                                zoneType = TableZoneType.QUALIFICATION,
                                lineStyle = CutoffLineStyle.SOLID
                            )
                        )
                    }

                    val groupsList = flattenedChildren.mapNotNull { (rawName, rawEntries) ->
                        val entries = rawEntries.mapNotNull { it.toStandingEntry() }
                        if (entries.isEmpty()) return@mapNotNull null

                        val sortedEntries = sortStandingEntries(entries)
                        val groupName = normalizeGroupName(rawName)

                        GroupStandings(
                            name = groupName,
                            standings = sortedEntries,
                            cutoffs = if (sortedEntries.size >= 2) resolvedGroupCutoffs else emptyList(),
                            legend = league.groupLegend
                        )
                    }

                    val flattenedStandings = groupsList.flatMap { it.standings }
                    groupsCache[league] = groupsList
                    standingsCache[league] = flattenedStandings
                    standingsTimestampCache[league] = System.currentTimeMillis()
                    Pair(flattenedStandings, groupsList)
                } else {
                    val entries = flattenedChildren.firstOrNull()?.second
                        ?: response.standings?.entries.orEmpty()
                    val mapped = entries.mapNotNull { it.toStandingEntry() }
                    val sortedEntries = sortStandingEntries(mapped)

                    groupsCache[league] = emptyList()
                    standingsCache[league] = sortedEntries
                    standingsTimestampCache[league] = System.currentTimeMillis()
                    Pair(sortedEntries, emptyList())
                }
            }

            if (result.isFailure) {
                val cachedStandings = standingsCache[league]
                val cachedGroups = groupsCache[league]
                if (cachedStandings != null && cachedGroups != null) {
                    return@withLock Result.success(Pair(cachedStandings, cachedGroups))
                }
            }
            result
        }
    }

    /**
     * Aplana recursivamente la jerarquia de nodos de clasificacion para soportar torneos
     * con niveles anidados como Liga A -> Grupo 1 sin requerir logica especifica por liga.
     */
    private fun flattenStandingsChildren(
        children: List<EspnStandingsChildDto>,
        parentPrefix: String = ""
    ): List<Pair<String, List<EspnStandingsEntryDto>>> {
        val result = mutableListOf<Pair<String, List<EspnStandingsEntryDto>>>()
        for (child in children) {
            val childName = child.name.ifBlank { child.abbreviation.orEmpty() }
            val combinedName = when {
                parentPrefix.isBlank() -> childName
                childName.isBlank() -> parentPrefix
                childName.startsWith(parentPrefix, ignoreCase = true) -> childName
                else -> "$parentPrefix, $childName"
            }
            val directEntries = child.standings?.entries.orEmpty()
            if (directEntries.isNotEmpty()) {
                result.add(Pair(combinedName, directEntries))
            }
            if (child.children.isNotEmpty()) {
                result.addAll(flattenStandingsChildren(child.children, combinedName))
            }
        }
        return result
    }

    private fun sortStandingEntries(entries: List<StandingEntry>): List<StandingEntry> {
        return entries.sortedWith(
            compareBy<StandingEntry> { if (it.rank > 0) it.rank else Int.MAX_VALUE }
                .thenByDescending { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
                .thenBy { it.teamName }
        ).mapIndexed { index, entry ->
            entry.copy(rank = if (entry.rank > 0) entry.rank else index + 1)
        }
    }

    private fun normalizeGroupName(rawName: String): String {
        val cleanedPrefix = when {
            rawName.contains(" - Group ", ignoreCase = true) ->
                rawName.substringAfter(" - ").trim()
            rawName.contains(" - Grupo ", ignoreCase = true) ->
                rawName.substringAfter(" - ").trim()
            else -> rawName
        }
        return when {
            cleanedPrefix.startsWith("Group ", ignoreCase = true) -> cleanedPrefix.substring(6).trim()
            cleanedPrefix.startsWith("Grupo ", ignoreCase = true) -> cleanedPrefix.substring(6).trim()
            cleanedPrefix.startsWith("League ", ignoreCase = true) ->
                cleanedPrefix.replace("League ", "Liga ", ignoreCase = true)
                    .replace(", Group ", " - Grupo ", ignoreCase = true)
                    .replace("Group ", "Grupo ", ignoreCase = true)
            else -> cleanedPrefix
        }
    }

    override fun getCachedMatchDetail(matchId: String): MatchDetail? {
        val cached = matchDetailCache[matchId] ?: return null
        val (detail, timestamp) = cached
        val age = System.currentTimeMillis() - timestamp
        val ttl = if (detail.isCompleted) {
            24 * 3600 * 1000L // 24 horas para partidos concluidos
        } else {
            20_000L // 20 segundos para partidos en vivo o previos al inicio
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
        val cacheControlHeader = if (forceRefresh) "no-cache" else null
        val result = runCatching {
            val response = apiService.getMatchSummary(
                league = league.code,
                eventId = matchId,
                cacheControl = cacheControlHeader
            )
            val detail = response.toMatchDetail(matchId)
            matchDetailCache[matchId] = Pair(detail, System.currentTimeMillis())
            detail
        }
        if (result.isFailure) {
            val fallback = matchDetailCache[matchId]?.first
            if (fallback != null) {
                return@withContext Result.success(fallback)
            }
        }
        result
    }

    private fun EspnMatchSummaryResponse.toMatchDetail(matchId: String): MatchDetail {
        val competition = header?.competitions?.firstOrNull()
        val isCompleted = competition?.status?.type?.completed == true ||
            competition?.status?.type?.state?.equals("post", ignoreCase = true) == true

        val homeCompetitor = competition?.competitors?.firstOrNull { it.homeAway.equals("home", ignoreCase = true) }
        val awayCompetitor = competition?.competitors?.firstOrNull { it.homeAway.equals("away", ignoreCase = true) }

        val homeTeamDto = homeCompetitor?.team
            ?: boxscore?.teams?.firstOrNull { it.homeAway.equals("home", ignoreCase = true) }?.team
            ?: rosters.firstOrNull { it.homeAway.equals("home", ignoreCase = true) }?.team
            ?: boxscore?.teams?.getOrNull(0)?.team
            ?: rosters.getOrNull(0)?.team

        val awayTeamDto = awayCompetitor?.team
            ?: boxscore?.teams?.firstOrNull { it.homeAway.equals("away", ignoreCase = true) }?.team
            ?: rosters.firstOrNull { it.homeAway.equals("away", ignoreCase = true) }?.team
            ?: boxscore?.teams?.getOrNull(1)?.team
            ?: rosters.getOrNull(1)?.team

        val homeTeamId = homeCompetitor?.id?.takeIf { it.isNotBlank() } ?: homeTeamDto?.id.orEmpty()
        val awayTeamId = awayCompetitor?.id?.takeIf { it.isNotBlank() } ?: awayTeamDto?.id.orEmpty()

        val rawScoringPlays = competition?.details.orEmpty().ifEmpty { keyEvents }
        val homeScorers = extractGoalScorers(rawScoringPlays, homeTeamId)
        val awayScorers = extractGoalScorers(rawScoringPlays, awayTeamId)

        val homeBoxscore = boxscore?.teams?.firstOrNull { it.team.id == homeTeamId && homeTeamId.isNotBlank() }
            ?: boxscore?.teams?.firstOrNull { it.homeAway.equals("home", ignoreCase = true) }
            ?: boxscore?.teams?.getOrNull(0)
        val awayBoxscore = boxscore?.teams?.firstOrNull { it.team.id == awayTeamId && awayTeamId.isNotBlank() }
            ?: boxscore?.teams?.firstOrNull { it.homeAway.equals("away", ignoreCase = true) }
            ?: boxscore?.teams?.getOrNull(1)

        val homeStatsMap = homeBoxscore?.statistics.orEmpty().associateBy { it.name }
        val awayStatsMap = awayBoxscore?.statistics.orEmpty().associateBy { it.name }

        val statsList = buildStatsList(homeStatsMap, awayStatsMap)

        val homeRoster = rosters.firstOrNull { it.team.id == homeTeamId && homeTeamId.isNotBlank() }
            ?: rosters.firstOrNull { it.homeAway.equals("home", ignoreCase = true) }
            ?: rosters.getOrNull(0)
        val awayRoster = rosters.firstOrNull { it.team.id == awayTeamId && awayTeamId.isNotBlank() }
            ?: rosters.firstOrNull { it.homeAway.equals("away", ignoreCase = true) }
            ?: rosters.getOrNull(1)

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
        val homeLastFive = lastFiveGames.firstOrNull { it.team?.id == homeTeamId && homeTeamId.isNotBlank() }
            ?: lastFiveGames.getOrNull(0)
        val awayLastFive = lastFiveGames.firstOrNull { it.team?.id == awayTeamId && awayTeamId.isNotBlank() }
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

        // Mapeo de Lideres Individuales exclusivos del partido concluido (summary.leaders).
        // Se descartan estadisticas acumuladas de ultimos partidos o temporada ("Matches: 5", etc.)
        // y se restringe estrictamente a partidos finalizados y a las categorias de juego unico.
        val matchLeaders = if (!isCompleted) {
            emptyList()
        } else {
            val homeLeadersDto = leaders.firstOrNull { it.team?.id == homeTeamId && homeTeamId.isNotBlank() }
                ?: leaders.getOrNull(0)
            val awayLeadersDto = leaders.firstOrNull { it.team?.id == awayTeamId && awayTeamId.isNotBlank() }
                ?: leaders.getOrNull(1)

            val homeCatMap = homeLeadersDto?.leaders.orEmpty().associateBy { it.name }
            val awayCatMap = awayLeadersDto?.leaders.orEmpty().associateBy { it.name }

            val orderedCategories = listOf("totalShots", "accuratePasses", "defensiveInterventions", "saves")

            orderedCategories.mapNotNull { catKey ->
                val homeCat = homeCatMap[catKey]
                val awayCat = awayCatMap[catKey]
                val homeEntry = homeCat?.leaders?.firstOrNull { isSingleMatchLeaderEntry(it) }
                val awayEntry = awayCat?.leaders?.firstOrNull { isSingleMatchLeaderEntry(it) }

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
        }

        val (goalCountsByAthleteId, assistCountsByAthleteId) = countGoalsAndAssistsByAthleteId(rawScoringPlays)
        val timelineEvents = extractTimelineEvents(
            keyEvents = keyEvents,
            fallbackDetails = competition?.details.orEmpty(),
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId,
            homeRoster = homeRoster,
            awayRoster = awayRoster
        )

        return MatchDetail(
            matchId = matchId,
            homeScorers = homeScorers,
            awayScorers = awayScorers,
            stats = statsList,
            homeLineup = homeRoster?.toTeamLineup(goalCountsByAthleteId, assistCountsByAthleteId),
            awayLineup = awayRoster?.toTeamLineup(goalCountsByAthleteId, assistCountsByAthleteId),
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
            timelineEvents = timelineEvents,
            isCompleted = isCompleted
        )
    }

    /**
     * Construye la cronologia unificada del partido filtrando pausas internas de ESPN y
     * combinando anotaciones, amonestaciones, expulsiones y sustituciones ordenadas por minuto.
     */
    private fun extractTimelineEvents(
        keyEvents: List<EspnSummaryScoringPlayDto>,
        fallbackDetails: List<EspnSummaryScoringPlayDto>,
        homeTeamId: String,
        awayTeamId: String,
        homeRoster: EspnRosterTeamDto?,
        awayRoster: EspnRosterTeamDto?
    ): List<MatchTimelineEvent> {
        val rawEvents = keyEvents.ifEmpty { fallbackDetails }
        val yellowSeenByAthleteId = mutableSetOf<String>()

        val mappedFromFeed = rawEvents.mapIndexedNotNull { index, play ->
            if (play.shootout) return@mapIndexedNotNull null
            val teamId = play.team?.id.orEmpty()
            val isHomeTeam = when {
                teamId.isNotBlank() && teamId == homeTeamId -> true
                teamId.isNotBlank() && teamId == awayTeamId -> false
                else -> return@mapIndexedNotNull null
            }

            val minute = play.clock?.displayValue?.trim().orEmpty().ifBlank { return@mapIndexedNotNull null }
            val typeText = play.type?.text.orEmpty()
            val typeSlug = play.type?.type.orEmpty()

            var eventType = when {
                play.ownGoal || typeSlug.contains("own-goal", true) || typeText.contains("own goal", true) -> TimelineEventType.OWN_GOAL
                play.scoringPlay && (play.penaltyKick || typeText.contains("penalty", true)) -> TimelineEventType.PENALTY_GOAL
                play.scoringPlay || typeSlug.startsWith("goal", true) || typeText.startsWith("Goal", true) -> TimelineEventType.GOAL
                typeSlug.contains("second-yellow", true) || typeSlug.contains("yellow-red", true) ||
                    typeText.contains("second yellow", true) -> TimelineEventType.SECOND_YELLOW
                typeSlug.contains("red-card", true) || typeText.contains("red card", true) -> TimelineEventType.RED_CARD
                typeSlug.contains("yellow-card", true) || typeText.contains("yellow card", true) -> TimelineEventType.YELLOW_CARD
                typeSlug.contains("substitution", true) || typeText.contains("substitution", true) -> TimelineEventType.SUBSTITUTION
                else -> null
            } ?: return@mapIndexedNotNull null

            val athletes = play.participants.mapNotNull { it.athlete }.ifEmpty { play.athletesInvolved }
            val primaryAthlete = athletes.getOrNull(0)
            val secondaryAthlete = athletes.getOrNull(1)

            if (eventType == TimelineEventType.YELLOW_CARD) {
                val athleteKey = primaryAthlete?.id?.takeIf { it.isNotBlank() }
                    ?: primaryAthlete?.displayName?.takeIf { it.isNotBlank() }
                if (athleteKey != null && !yellowSeenByAthleteId.add(athleteKey)) {
                    eventType = TimelineEventType.SECOND_YELLOW
                }
            }

            val primaryName = primaryAthlete?.let { a ->
                a.shortName?.takeIf { it.isNotBlank() } ?: a.displayName.takeIf { it.isNotBlank() }
            } ?: extractFallbackTimelinePlayerName(play, eventType) ?: return@mapIndexedNotNull null

            val secondaryName = secondaryAthlete?.let { a ->
                a.shortName?.takeIf { it.isNotBlank() } ?: a.displayName.takeIf { it.isNotBlank() }
            }

            val sortSec = play.clock?.value?.toInt() ?: parseClockToSeconds(minute)
            val eventId = play.id?.takeIf { it.isNotBlank() } ?: "ev_${index}_${sortSec}_${primaryName}"

            MatchTimelineEvent(
                id = eventId,
                minute = minute,
                sortMinute = sortSec,
                type = eventType,
                isHomeTeam = isHomeTeam,
                primaryPlayerName = primaryName,
                secondaryPlayerName = secondaryName
            )
        }

        val combined = if (mappedFromFeed.isNotEmpty()) {
            mappedFromFeed
        } else {
            buildFallbackTimelineFromRosters(homeRoster, awayRoster)
        }

        return combined.sortedWith(
            compareByDescending<MatchTimelineEvent> { it.sortMinute }
                .thenBy { it.id }
        )
    }

    private fun extractFallbackTimelinePlayerName(
        play: EspnSummaryScoringPlayDto,
        type: TimelineEventType
    ): String? {
        val raw = play.shortText?.takeIf { it.isNotBlank() } ?: play.text?.takeIf { it.isNotBlank() } ?: return null
        val cleaned = when (type) {
            TimelineEventType.YELLOW_CARD -> raw.replace("Yellow Card", "", ignoreCase = true)
            TimelineEventType.SECOND_YELLOW -> raw.replace("Second Yellow Card", "", ignoreCase = true)
            TimelineEventType.RED_CARD -> raw.replace("Red Card", "", ignoreCase = true)
            TimelineEventType.SUBSTITUTION -> raw.replace("Substitution", "", ignoreCase = true)
            TimelineEventType.GOAL,
            TimelineEventType.OWN_GOAL,
            TimelineEventType.PENALTY_GOAL -> raw.substringBefore("Goal").trim()
        }.trim(' ', '-', ',', '.', ':')
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun buildFallbackTimelineFromRosters(
        homeRoster: EspnRosterTeamDto?,
        awayRoster: EspnRosterTeamDto?
    ): List<MatchTimelineEvent> {
        val events = mutableListOf<MatchTimelineEvent>()
        fun collect(rosterDto: EspnRosterTeamDto?, isHome: Boolean) {
            rosterDto?.roster.orEmpty().forEachIndexed { playerIdx, item ->
                val playerName = item.athlete?.shortName?.takeIf { it.isNotBlank() }
                    ?: item.athlete?.displayName?.takeIf { it.isNotBlank() }
                    ?: return@forEachIndexed
                var yellowCount = 0
                item.plays.forEachIndexed { playIdx, p ->
                    val min = p.clock?.displayValue?.trim().orEmpty().ifBlank { return@forEachIndexed }
                    val sortSec = p.clock?.value?.toInt() ?: parseClockToSeconds(min)
                    val evType = when {
                        p.scoringPlay -> TimelineEventType.GOAL
                        p.redCard -> TimelineEventType.RED_CARD
                        p.yellowCard -> {
                            yellowCount++
                            if (yellowCount >= 2) TimelineEventType.SECOND_YELLOW else TimelineEventType.YELLOW_CARD
                        }
                        p.substitution && item.subbedIn -> TimelineEventType.SUBSTITUTION
                        else -> null
                    } ?: return@forEachIndexed

                    val secondary = if (evType == TimelineEventType.SUBSTITUTION) {
                        item.subbedInFor?.athlete?.shortName?.takeIf { it.isNotBlank() }
                            ?: item.subbedInFor?.athlete?.displayName?.takeIf { it.isNotBlank() }
                    } else null

                    events.add(
                        MatchTimelineEvent(
                            id = "roster_${isHome}_${playerIdx}_${playIdx}_$sortSec",
                            minute = min,
                            sortMinute = sortSec,
                            type = evType,
                            isHomeTeam = isHome,
                            primaryPlayerName = playerName,
                            secondaryPlayerName = secondary
                        )
                    )
                }
            }
        }
        collect(homeRoster, isHome = true)
        collect(awayRoster, isHome = false)
        return events
    }

    private fun parseClockToSeconds(clock: String): Int {
        val cleaned = clock.replace("'", "").trim()
        val parts = cleaned.split("+")
        val base = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val added = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        return base * 60 + added
    }

    /**
     * Cuenta goles no autogoles y asistencias por identificador de futbolista desde details/keyEvents
     * como respaldo ante datos incompletos de roster[].plays, evitando confundir asistencias con goles.
     */
    private fun countGoalsAndAssistsByAthleteId(
        plays: List<EspnSummaryScoringPlayDto>
    ): Pair<Map<String, Int>, Map<String, Int>> {
        if (plays.isEmpty()) return emptyMap<String, Int>() to emptyMap()
        val goalCounts = mutableMapOf<String, Int>()
        val assistCounts = mutableMapOf<String, Int>()
        plays.forEach { play ->
            val typeText = play.type?.text.orEmpty()
            val isGoal = play.scoringPlay || typeText.contains("Goal", ignoreCase = true)
            val isOwnGoal = play.ownGoal || typeText.contains("Own Goal", ignoreCase = true)
            if (isGoal && !isOwnGoal && !play.shootout) {
                val athletes = play.participants.mapNotNull { it.athlete }.ifEmpty { play.athletesInvolved }
                val scorerId = athletes.getOrNull(0)?.id?.takeIf { it.isNotBlank() }
                if (scorerId != null) {
                    goalCounts[scorerId] = (goalCounts[scorerId] ?: 0) + 1
                }
                val assistId = athletes.getOrNull(1)?.id?.takeIf { it.isNotBlank() }
                if (assistId != null) {
                    assistCounts[assistId] = (assistCounts[assistId] ?: 0) + 1
                }
            }
        }
        return goalCounts to assistCounts
    }

    /**
     * Verifica que una entrada de lider corresponda exclusivamente al partido actual,
     * descartando cadenas acumuladas de racha o temporada que incluyan conteo de partidos.
     */
    private fun isSingleMatchLeaderEntry(entry: EspnLeaderEntryDto): Boolean {
        val combined = "${entry.displayValue} ${entry.summary.orEmpty()}"
        if (combined.isBlank()) return false
        val multiMatchTokens = listOf("matches", "partidos", "games", "appearances", "apariciones", "pj:")
        return multiMatchTokens.none { token -> combined.contains(token, ignoreCase = true) }
    }

    /**
     * Extrae la lista de goleadores de un equipo unificando todas las variantes de jugadas de ESPN
     * (participants, athletesInvolved y texto corto/largo en details o keyEvents).
     */
    private fun extractGoalScorers(
        plays: List<EspnSummaryScoringPlayDto>,
        teamId: String
    ): List<GoalScorer> {
        if (teamId.isBlank() || plays.isEmpty()) return emptyList()
        val goalPlays = plays.filter { play ->
            play.team?.id == teamId && isGoalPlay(play)
        }
        if (goalPlays.isEmpty()) return emptyList()

        return goalPlays
            .groupBy { resolveScorerName(it) }
            .filterKeys { it.isNotBlank() }
            .map { (name, playerPlays) ->
                val events = playerPlays.mapNotNull { play ->
                    val baseClock = play.clock?.displayValue?.takeIf { c -> c.isNotBlank() } ?: return@mapNotNull null
                    val typeText = play.type?.text.orEmpty()
                    val isOg = play.ownGoal ||
                        typeText.contains("Own Goal", ignoreCase = true) ||
                        typeText.contains("Autogol", ignoreCase = true)
                    val isPen = !isOg && (play.penaltyKick ||
                        typeText.contains("Penalty", ignoreCase = true) ||
                        typeText.contains("Penal", ignoreCase = true))
                    val cleanClock = baseClock
                        .replace(" (EC)", "")
                        .replace(" (OG)", "")
                        .replace(" (P)", "")
                        .trim()
                    GoalEvent(minute = cleanClock, isOwnGoal = isOg, isPenalty = isPen)
                }
                val minutes = events.joinToString(", ") { event ->
                    when {
                        event.isOwnGoal -> "${event.minute} (OG)"
                        event.isPenalty -> "${event.minute} (P)"
                        else -> event.minute
                    }
                }
                GoalScorer(playerName = name, minutesDisplay = minutes, goals = events)
            }
    }

    private fun isGoalPlay(play: EspnSummaryScoringPlayDto): Boolean {
        if (play.scoringPlay) return true
        val typeText = play.type?.text?.takeIf { it.isNotBlank() } ?: return false
        val hasGoalKeyword = typeText.contains("Goal", ignoreCase = true) ||
            typeText.contains("Gol", ignoreCase = true) ||
            typeText.contains("Penalty - Scored", ignoreCase = true)
        val isExcluded = typeText.contains("Kick", ignoreCase = true) && !typeText.contains("Penalty", ignoreCase = true) ||
            typeText.contains("Miss", ignoreCase = true) ||
            typeText.contains("Saved", ignoreCase = true) ||
            typeText.contains("No Goal", ignoreCase = true)
        return hasGoalKeyword && !isExcluded
    }

    private fun resolveScorerName(play: EspnSummaryScoringPlayDto): String {
        val fromParticipant = play.participants.firstOrNull()?.athlete?.let {
            it.displayName.ifBlank { it.shortName.orEmpty() }
        }?.takeIf { it.isNotBlank() }
        if (fromParticipant != null) return fromParticipant

        val fromInvolved = play.athletesInvolved.firstOrNull()?.let {
            it.displayName.ifBlank { it.shortName.orEmpty() }
        }?.takeIf { it.isNotBlank() }
        if (fromInvolved != null) return fromInvolved

        val rawText = play.shortText?.takeIf { it.isNotBlank() } ?: play.text?.takeIf { it.isNotBlank() } ?: return ""
        return rawText
            .replace(Regex("""^(?:Goal!?\s*[-:]?\s*|Gol!?\s*[-:]?\s*|Own Goal by\s*)""", RegexOption.IGNORE_CASE), "")
            .substringBefore(" (")
            .substringBefore(" - ")
            .replace(Regex("""\s+\d+(?:\+\d+)?'.*$"""), "")
            .trim()
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

    private fun EspnRosterTeamDto.toTeamLineup(
        goalCountsByAthleteId: Map<String, Int> = emptyMap(),
        assistCountsByAthleteId: Map<String, Int> = emptyMap()
    ): TeamLineup {
        val (rawStarters, rawBench) = when {
            roster.any { it.starter } -> roster.partition { it.starter }
            roster.any { (it.formationPlace ?: 0) in 1..11 } -> roster.partition { (it.formationPlace ?: 0) in 1..11 }
            roster.size >= 11 -> Pair(roster.take(11), roster.drop(11))
            else -> Pair(roster, emptyList())
        }
        val startersList = rawStarters.mapIndexed { index, p ->
            p.toLineupPlayer(index, goalCountsByAthleteId, assistCountsByAthleteId)
        }
        val benchList = rawBench.mapIndexed { index, p ->
            p.toLineupPlayer(index, goalCountsByAthleteId, assistCountsByAthleteId)
        }
        val resolvedShortName = team.shortDisplayName?.takeIf { it.isNotBlank() }
            ?: team.abbreviation?.takeIf { it.isNotBlank() }
            ?: team.name
        return TeamLineup(
            teamId = team.id,
            teamName = team.displayName.ifBlank { team.name },
            shortName = resolvedShortName,
            formation = formation?.takeIf { it.isNotBlank() } ?: "4-4-2",
            teamColor = team.color,
            teamAlternateColor = team.alternateColor,
            starters = startersList,
            bench = benchList
        )
    }

    private fun EspnRosterPlayerItemDto.toLineupPlayer(
        index: Int,
        goalCountsByAthleteId: Map<String, Int> = emptyMap(),
        assistCountsByAthleteId: Map<String, Int> = emptyMap()
    ): LineupPlayer {
        val resolvedPos = position ?: athlete?.extractPosition()
        val posName = resolvedPos?.name?.takeIf { it.isNotBlank() }
            ?: resolvedPos?.displayName?.takeIf { it.isNotBlank() }
            .orEmpty()
        val posAbbr = resolvedPos?.abbreviation.orEmpty().uppercase()
        val slot = formationPlace?.takeIf { it > 0 } ?: (index + 1)
        val line = when {
            slot == 1 ||
                posAbbr in setOf("G", "GK", "POR") ||
                posName.contains("Goalkeeper", ignoreCase = true) ||
                posName.contains("Goalie", ignoreCase = true) ||
                posName.contains("Portero", ignoreCase = true) ||
                posName.contains("Arquero", ignoreCase = true) -> TacticalLine.GOALKEEPER
            posAbbr in setOf("D", "DF", "CB", "LB", "RB", "LWB", "RWB", "CD", "LD", "LI") ||
                posName.contains("Defender", ignoreCase = true) ||
                posName.contains("Defense", ignoreCase = true) ||
                posName.contains("Back", ignoreCase = true) ||
                posName.contains("Defensa", ignoreCase = true) ||
                posName.contains("Lateral", ignoreCase = true) ||
                slot in 2..5 -> TacticalLine.DEFENSE
            posAbbr in setOf("M", "MF", "CM", "CDM", "CAM", "DM", "AM", "LM", "RM", "MC", "MCD", "MCO", "MI", "MD") ||
                posName.contains("Midfield", ignoreCase = true) ||
                posName.contains("Mediocamp", ignoreCase = true) ||
                posName.contains("Volante", ignoreCase = true) ||
                slot in 6..9 -> TacticalLine.MIDFIELD
            else -> TacticalLine.FORWARD
        }
        val subInMinute = if (subbedIn) {
            plays.firstOrNull { it.substitution }?.clock?.displayValue?.takeIf { it.isNotBlank() }
        } else null
        val subForId = if (subbedIn) subbedInFor?.athlete?.id else null
        val subForName = if (subbedIn) {
            subbedInFor?.athlete?.shortName?.takeIf { it.isNotBlank() }
                ?: subbedInFor?.athlete?.displayName?.takeIf { it.isNotBlank() }
        } else null
        val subForJersey = if (subbedIn) subbedInFor?.jersey else null

        val subOutMinute = if (subbedOut) {
            plays.firstOrNull { it.substitution }?.clock?.displayValue?.takeIf { it.isNotBlank() }
        } else null
        val subOutForId = if (subbedOut) subbedOutFor?.athlete?.id else null
        val subOutForName = if (subbedOut) {
            subbedOutFor?.athlete?.shortName?.takeIf { it.isNotBlank() }
                ?: subbedOutFor?.athlete?.displayName?.takeIf { it.isNotBlank() }
        } else null

        val athleteId = athlete?.id.orEmpty()
        // ESPN marca scoringPlay = true tanto al goleador (didScore) como al asistidor (didAssist).
        val hasExplicitGoalFlags = plays.any { it.didScore || it.didAssist }
        val goalsFromPlays = if (hasExplicitGoalFlags) {
            plays.count { it.didScore && !it.ownGoal }
        } else {
            plays.count { it.scoringPlay && !it.ownGoal }
        }
        val goalsCount = maxOf(
            goalsFromPlays,
            if (athleteId.isNotBlank()) goalCountsByAthleteId[athleteId] ?: 0 else 0
        )
        val assistsCount = maxOf(
            plays.count { it.didAssist },
            if (athleteId.isNotBlank()) assistCountsByAthleteId[athleteId] ?: 0 else 0
        )
        val yellowCardsCount = plays.count { it.yellowCard }
        val hasRedCard = plays.any { it.redCard }
        val resolvedJersey = jersey?.takeIf { it.isNotBlank() }
            ?: athlete?.jersey?.takeIf { it.isNotBlank() }
            ?: "$slot"

        return LineupPlayer(
            id = athleteId.ifBlank { "$slot" },
            name = athlete?.shortName?.takeIf { it.isNotBlank() } ?: athlete?.displayName.orEmpty(),
            jersey = resolvedJersey,
            position = posName.ifBlank { posAbbr },
            formationPlace = slot,
            line = line,
            subbedIn = subbedIn,
            subInMinute = subInMinute,
            subbedInForId = subForId,
            subbedInForName = subForName,
            subbedInForJersey = subForJersey,
            subbedOut = subbedOut,
            subOutMinute = subOutMinute,
            subbedOutForId = subOutForId,
            subbedOutForName = subOutForName,
            goalsCount = goalsCount,
            assistsCount = assistsCount,
            yellowCards = yellowCardsCount,
            hasRedCard = hasRedCard
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

        val isPostponedOrUnplayed = typeName.contains("POSTPONED", ignoreCase = true) ||
            typeName.contains("CANCELED", ignoreCase = true) ||
            typeName.contains("CANCELLED", ignoreCase = true) ||
            typeName.contains("ABANDONED", ignoreCase = true) ||
            typeName.contains("SUSPENDED", ignoreCase = true) ||
            typeName.contains("DELAYED", ignoreCase = true) ||
            typeName.contains("TBD", ignoreCase = true) ||
            detail.equals("Postponed", ignoreCase = true) ||
            detail.equals("Canceled", ignoreCase = true) ||
            detail.equals("TBD", ignoreCase = true)

        val isCompleted = !isPostponedOrUnplayed && (
            statusDto?.type?.completed == true ||
            typeName.equals("STATUS_FULL_TIME", ignoreCase = true) ||
            typeName.equals("STATUS_FINAL", ignoreCase = true) ||
            typeName.equals("STATUS_FINAL_AET", ignoreCase = true) ||
            typeName.equals("STATUS_FINAL_PEN", ignoreCase = true) ||
            typeName.equals("STATUS_FULL_TIME_AET", ignoreCase = true) ||
            typeName.equals("STATUS_FULL_TIME_PEN", ignoreCase = true) ||
            detail.equals("FT", ignoreCase = true) ||
            detail.equals("AET", ignoreCase = true) ||
            detail.startsWith("FT-", ignoreCase = true)
        )

        val matchState = when {
            isCompleted -> MatchState.FINISHED
            isPostponedOrUnplayed -> MatchState.SCHEDULED
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
                val slugLower = seasonSlug.lowercase()
                when {
                    slugLower.contains("wild-card") || slugLower.contains("wild card") -> "wild-card"
                    slugLower.contains("round-one") || slugLower.contains("round one") -> "round-one"
                    slugLower.contains("elimination-final") -> "elimination-finals"
                    slugLower.contains("conference") && slugLower.contains("semi") -> "conference-semifinals"
                    slugLower.contains("conference") && slugLower.contains("final") -> "conference-finals"
                    slugLower.contains("mls-cup") -> "mls-cup"
                    slugLower.contains("grand-final") -> "grand-final"
                    slugLower.contains("3rd-place") || slugLower.contains("third-place") -> "3rd-place-match"
                    slugLower.contains("round-of-32") -> "round-of-32"
                    slugLower.contains("round-of-16") -> "round-of-16"
                    slugLower.contains("quarter") -> "quarterfinals"
                    slugLower.contains("semi") -> "semifinals"
                    slugLower.contains("final") -> "final"
                    slugLower == "torneo-apertura" || slugLower == "apertura" -> "torneo-apertura"
                    slugLower == "torneo-clausura" || slugLower == "clausura" -> "torneo-clausura"
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
        val isSeriesCompleted = series?.completed == true
        val seriesWinnerTeamId = series?.competitors?.firstOrNull { it.winner }?.id?.takeIf { it.isNotBlank() }
        val seriesTotalGames = series?.totalCompetitions
        val homeScorers = extractGoalScorers(competition.details, homeTeam.id)
        val awayScorers = extractGoalScorers(competition.details, awayTeam.id)

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
            seriesSummary = seriesSummary,
            isSeriesCompleted = isSeriesCompleted,
            seriesWinnerTeamId = seriesWinnerTeamId,
            seriesTotalGames = seriesTotalGames,
            homeScorers = homeScorers,
            awayScorers = awayScorers
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

        val resolvedTeamName = team.displayName.ifBlank { team.name }.trim()
        if (resolvedTeamName.isEmpty() ||
            resolvedTeamName.equals("TBD", ignoreCase = true) ||
            resolvedTeamName.equals("TBA", ignoreCase = true)
        ) {
            return null
        }

        return StandingEntry(
            rank = rank,
            teamName = resolvedTeamName,
            teamShortName = team.shortDisplayName ?: resolvedTeamName,
            teamAbbreviation = team.abbreviation ?: resolvedTeamName.take(3).uppercase(),
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
        private val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")

        /**
         * Determina los meses (YYYYMM) del calendario que pueden contener rondas de una fase eliminatoria
         * activa en ligas domesticas con liguilla o playoffs, acotando el inicio a la ultima jornada regular
         * conocida para minimizar peticiones redundantes.
         */
        internal fun resolveKnockoutMonths(
            calendarDates: List<String>,
            latestRegularDate: LocalDate? = null,
            today: LocalDate = LocalDate.now(ZoneId.systemDefault())
        ): List<String> {
            val baseStart = today.minusDays(35L)
            val windowStart = if (latestRegularDate != null && latestRegularDate.isAfter(baseStart)) {
                latestRegularDate
            } else {
                baseStart
            }
            val windowEnd = today.plusDays(21L)
            val inWindow = calendarDates
                .mapNotNull { parseCalendarDate(it) }
                .filter { it in windowStart..windowEnd }
            if (inWindow.isEmpty()) return emptyList()
            return inWindow.map { it.format(YEAR_MONTH_FORMATTER) }.distinct().sorted()
        }

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
                    if (gap <= 1L || (upcomingCluster.size == 1 && gap <= 7L)) {
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
                    if (gap <= 1L || (pastCluster.size == 1 && gap <= 7L)) {
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
