package com.ixeken.ulama.ui.matches

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.LeagueCategory
import com.ixeken.ulama.core.model.sanitizeAggregateScores
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.RadialBracketBuilder
import com.ixeken.ulama.core.model.TemporalFilter
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.core.model.toFollowedMatch
import com.ixeken.ulama.core.model.toMatch
import com.ixeken.ulama.core.util.DateTimeUtils
import com.ixeken.ulama.data.repository.AppSettingsRepository
import com.ixeken.ulama.data.repository.AppSettingsRepositoryImpl
import com.ixeken.ulama.data.repository.FavoriteTeamsRepository
import com.ixeken.ulama.data.repository.FavoriteTeamsRepositoryImpl
import com.ixeken.ulama.data.repository.MatchFollowRepository
import com.ixeken.ulama.data.repository.MatchRepository
import com.ixeken.ulama.data.repository.MatchRepositoryImpl
import com.ixeken.ulama.service.FavoriteAutoFollowJobService
import com.ixeken.ulama.service.FavoriteAutoFollowSync
import com.ixeken.ulama.service.MatchAlarmScheduler
import com.ixeken.ulama.service.MatchLiveNotificationService
import com.ixeken.ulama.data.repository.MyLeaguesRepository
import com.ixeken.ulama.data.repository.MyLeaguesRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel encargado del flujo de partidos y clasificaciones.
 * Mantiene la reactividad desacoplada mediante StateFlow inmutable, gestiona
 * el filtrado temporal en memoria, la agregacion multiliga de favoritos y persiste en SharedPreferences.
 */
class MatchesViewModel(
    private val repository: MatchRepository,
    private val followRepository: MatchFollowRepository,
    private val favoriteTeamsRepository: FavoriteTeamsRepository,
    private val myLeaguesRepository: MyLeaguesRepository,
    private val prefs: SharedPreferences? = null,
    private val appSettingsRepository: AppSettingsRepository? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val initialLeague: League = prefs?.getString(KEY_SELECTED_LEAGUE, null)
        ?.let { League.fromCode(it) } ?: League.LIGA_MX

    private val _uiState = MutableStateFlow(MatchesUiState(selectedLeague = initialLeague))
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    private var cachedAllMatches: List<Match> = emptyList()
    private val manuallyUnfollowedMatchIds = mutableSetOf<String>()
    private var isLoadingAvailableTeams = false
    private var hasLoadedAllAvailableTeams = false
    private var autoFollowScanJob: Job? = null
    private var lastRefreshTimeMillis: Long = 0L
    private var pendingOpenMatchId: String? = null

    init {
        lastRefreshTimeMillis = System.currentTimeMillis()
        loadData(initialLeague)
        if (initialLeague == League.FAVORITES &&
            favoriteTeamsRepository.favoriteTeamIds.value.isNotEmpty() &&
            !favoriteTeamsRepository.hasMappedLeaguesForAllFavorites()
        ) {
            loadAvailableTeamsByLeague(forceRefresh = false)
        }
        followRepository.followedMatchIds
            .onEach { ids ->
                _uiState.update { it.copy(followedMatchIds = ids) }
            }
            .launchIn(viewModelScope)

        favoriteTeamsRepository.favoriteTeamIds
            .onEach { favIds ->
                _uiState.update { it.copy(favoriteTeamIds = favIds) }
                if (favIds.isNotEmpty() &&
                    _uiState.value.selectedLeague == League.FAVORITES &&
                    !favoriteTeamsRepository.hasMappedLeaguesForAllFavorites()
                ) {
                    ensureAvailableTeamsLoaded()
                }
                if (_uiState.value.selectedLeague == League.FAVORITES) {
                    val hasAllFavoritesCached = favIds.all { favId ->
                        cachedAllMatches.any { it.homeTeam.id == favId || it.awayTeam.id == favId }
                    }
                    if (favIds.isNotEmpty() && (cachedAllMatches.isEmpty() || !hasAllFavoritesCached)) {
                        loadFavoriteLeagueData(forceRefresh = false, isSilent = cachedAllMatches.isNotEmpty())
                    } else {
                        val byFilter = buildMatchesByFilter(cachedAllMatches, League.FAVORITES, favIds)
                        _uiState.update { state ->
                            state.copy(
                                matches = byFilter[state.selectedFilter].orEmpty(),
                                matchesByFilter = byFilter
                            )
                        }
                    }
                }
                if (favIds.isNotEmpty() && appSettingsRepository?.settings?.value?.autoFollowFavorites == true) {
                    appContext?.let { FavoriteAutoFollowJobService.syncSchedule(it) }
                    scheduleAutoFollowScan()
                }
            }
            .launchIn(viewModelScope)

        myLeaguesRepository.myLeagueCodes
            .onEach { codes ->
                _uiState.update { it.copy(myLeagueCodes = codes) }
                if (_uiState.value.selectedLeague == League.MY_LEAGUES) {
                    loadMyLeaguesData()
                }
            }
            .launchIn(viewModelScope)

        appSettingsRepository?.settings
            ?.map { it.autoFollowFavorites }
            ?.distinctUntilChanged()
            ?.onEach { enabled ->
                appContext?.let { FavoriteAutoFollowJobService.syncSchedule(it) }
                if (enabled) {
                    scheduleAutoFollowScan()
                } else {
                    autoFollowScanJob?.cancel()
                }
            }
            ?.launchIn(viewModelScope)
    }

    /**
     * Conmuta la competicion activa o revalida la seleccionada, persiste la preferencia en disco
     * y ejecuta un refresco forzado para actualizar calendario, marcadores y eliminatorias.
     */
    fun selectLeague(league: League) {
        val isSameLeague = _uiState.value.selectedLeague == league
        prefs?.edit()?.putString(KEY_SELECTED_LEAGUE, league.code)?.apply()
        if (league == League.FAVORITES && !favoriteTeamsRepository.hasMappedLeaguesForAllFavorites()) {
            ensureAvailableTeamsLoaded()
        }
        if (!isSameLeague) {
            cachedAllMatches = emptyList()
            val initialBracket = if (league.hasKnockoutBracket && league != League.DEBUG) {
                RadialBracketBuilder.createEmptyTree(league.knockoutBracketTeams, league.hasThirdPlaceMatch)
            } else null
            _uiState.update {
                it.copy(
                    selectedLeague = league,
                    isLoading = true,
                    matches = emptyList(),
                    matchesByFilter = emptyMap(),
                    standings = emptyList(),
                    groups = emptyList(),
                    radialBracket = initialBracket,
                    errorMessage = null
                )
            }
        }
        lastRefreshTimeMillis = System.currentTimeMillis()
        loadData(league, forceRefresh = true, isSilent = false)
    }

    /**
     * Aplica el filtro temporal sobre los partidos prefiltrados para cambio instantaneo de pestana
     * y evita descargas redundantes si los datos ya son recientes o si el modo de ahorro de datos esta activo.
     */
    fun selectFilter(filter: TemporalFilter) {
        if (_uiState.value.selectedFilter == filter) return
        val state = _uiState.value
        val currentMatches = state.matchesByFilter[filter] ?: when (state.selectedLeague) {
            League.FAVORITES -> filterFavoriteMatches(cachedAllMatches, state.favoriteTeamIds, filter)
            else -> filterMatches(cachedAllMatches, filter)
        }
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                matches = currentMatches
            )
        }
        if (state.selectedLeague != League.DEBUG) {
            val now = System.currentTimeMillis()
            val isDataSaver = appSettingsRepository?.settings?.value?.dataSaverMode == true
            val skipNetworkRevalidation = cachedAllMatches.isNotEmpty() &&
                (isDataSaver || now - lastRefreshTimeMillis < FILTER_REFRESH_COOLDOWN_MS)
            if (!skipNetworkRevalidation) {
                lastRefreshTimeMillis = now
                loadData(state.selectedLeague, forceRefresh = true, isSilent = true)
            }
        }
    }

    /**
     * Vuelve a consultar los datos para la liga actual forzando la solicitud de red.
     */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        lastRefreshTimeMillis = System.currentTimeMillis()
        loadData(_uiState.value.selectedLeague, forceRefresh = true, isSilent = false)
    }

    /**
     * Ejecuta una actualizacion silenciosa sin indicadores intrusivos en la interfaz de usuario.
     * Consulta la red directamente forzando el refresco de cache con una cota minima de 10 segundos.
     */
    fun silentRefresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTimeMillis < 10_000L) return
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return
        lastRefreshTimeMillis = now
        val state = _uiState.value
        loadData(state.selectedLeague, forceRefresh = true, isSilent = true)

        // Si el visor de detalle esta abierto en un partido en vivo, refrescamos tambien sus eventos y estadisticas
        val activeMatch = state.selectedMatch
        if (activeMatch != null &&
            activeMatch.status.state == MatchState.LIVE &&
            !com.ixeken.ulama.data.debug.DebugLeagueData.isDebugMatch(activeMatch.id)
        ) {
            val targetLeague = if (!activeMatch.league.isVirtual) activeMatch.league else state.selectedLeague
            viewModelScope.launch {
                val detailResult = repository.getMatchDetail(
                    league = targetLeague,
                    matchId = activeMatch.id,
                    forceRefresh = true
                )
                if (_uiState.value.selectedMatch?.id == activeMatch.id) {
                    _uiState.update {
                        it.copy(matchDetail = detailResult.getOrNull() ?: it.matchDetail)
                    }
                }
            }
        }
    }

    /**
     * Garantiza que los clubes oficiales de todas las competiciones se carguen bajo demanda
     * para busquedas globales y la vista de Mis equipos sin repetir llamadas ya resueltas.
     */
    fun ensureAvailableTeamsLoaded() {
        if (hasLoadedAllAvailableTeams || isLoadingAvailableTeams) return
        loadAvailableTeamsByLeague(forceRefresh = false)
    }

    /**
     * Carga bajo demanda los equipos de una sola competicion evitando disparar 16 consultas
     * paralelas al explorar clubes de una liga especifica. Reutiliza la tabla ya cargada
     * en pantalla para respuesta inmediata en cero milisegundos cuando coincide con la activa.
     */
    fun ensureLeagueTeamsLoaded(league: League) {
        if (league.isVirtual || league == League.DEBUG) return
        val currentState = _uiState.value
        if (!currentState.availableTeamsByLeague[league].isNullOrEmpty()) return
        if (league in currentState.loadingTeamLeagues) return

        if (currentState.selectedLeague == league) {
            val activeTeams = if (currentState.groups.isNotEmpty()) {
                currentState.groups.flatMap { it.standings }
            } else {
                currentState.standings
            }.distinctBy { it.teamId.ifBlank { it.teamName } }

            if (activeTeams.isNotEmpty()) {
                _uiState.update {
                    it.copy(availableTeamsByLeague = it.availableTeamsByLeague + (league to activeTeams))
                }
                if (!league.hasKnockoutBracket && league.tournamentYear == null) return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loadingTeamLeagues = it.loadingTeamLeagues + league) }
            try {
                val teams = fetchLeagueTeams(league, forceRefresh = false)
                if (teams.isNotEmpty()) {
                    recordFavoritesInLeagueEntries(league, teams)
                    _uiState.update { state ->
                        state.copy(availableTeamsByLeague = state.availableTeamsByLeague + (league to teams))
                    }
                }
            } finally {
                _uiState.update { state ->
                    state.copy(loadingTeamLeagues = state.loadingTeamLeagues - league)
                }
            }
        }
    }

    /**
     * Busca y abre un partido por su identificador unico, priorizando la cache en memoria
     * o la entidad persistida en el repositorio de seguimiento para apertura instantanea desde notificaciones.
     */
    fun selectMatchById(matchId: String) {
        if (com.ixeken.ulama.data.debug.DebugLeagueData.isDebugMatch(matchId)) {
            val debugMatch = com.ixeken.ulama.data.debug.DebugLeagueData.getMatches().firstOrNull { it.id == matchId }
            if (debugMatch != null) {
                selectMatch(debugMatch)
            }
            return
        }

        val cachedMatch = cachedAllMatches.firstOrNull { it.id == matchId }
        if (cachedMatch != null) {
            selectMatch(cachedMatch)
            return
        }

        val followed = followRepository.getFollowed(matchId)
        if (followed != null) {
            selectMatch(followed.toMatch())
            return
        }

        pendingOpenMatchId = matchId
    }

    /**
     * Selecciona un partido y solicita sus detalles enriquecidos (alineaciones, estadisticas y tabla).
     * Implementa Stale-While-Revalidate: proyecta de inmediato el detalle en cache si existe reduciendo la latencia a 0ms,
     * omite peticiones remotas para partidos concluidos en cache y desacopla la carga de clasificacion.
     */
    fun selectMatch(match: Match) {
        if (com.ixeken.ulama.data.debug.DebugLeagueData.isDebugMatch(match.id)) {
            _uiState.update {
                it.copy(
                    selectedMatch = match,
                    isLoadingDetail = false,
                    matchDetail = com.ixeken.ulama.data.debug.DebugLeagueData.getMatchDetail(match.id),
                    detailStandings = if (_uiState.value.selectedLeague == League.DEBUG) {
                        com.ixeken.ulama.data.debug.DebugLeagueData.getGroupStandingsForMatch(match.id)
                            ?: com.ixeken.ulama.data.debug.DebugLeagueData.getStandings()
                    } else {
                        emptyList()
                    }
                )
            }
            return
        }
        val targetLeague = if (!match.league.isVirtual) match.league else _uiState.value.selectedLeague
        val immediateStandings = if (_uiState.value.selectedLeague == targetLeague) {
            if (_uiState.value.groups.isNotEmpty()) {
                _uiState.value.groups.firstOrNull { group ->
                    group.standings.any { it.teamId == match.homeTeam.id || it.teamId == match.awayTeam.id }
                }?.standings ?: _uiState.value.standings
            } else {
                _uiState.value.standings
            }
        } else {
            _uiState.value.availableTeamsByLeague[targetLeague].orEmpty()
        }
        val cachedDetail = repository.getCachedMatchDetail(match.id)
        val totalGoals = (match.homeTeam.score ?: 0) + (match.awayTeam.score ?: 0)
        val cachedGoals = cachedDetail?.let { it.homeScorers.size + it.awayScorers.size } ?: 0
        val isCacheCompleteAndSynced = cachedDetail != null &&
            cachedDetail.isCompleted &&
            match.isFinished &&
            (totalGoals == 0 || cachedGoals > 0)

        _uiState.update {
            it.copy(
                selectedMatch = match,
                isLoadingDetail = cachedDetail == null,
                matchDetail = cachedDetail,
                detailStandings = immediateStandings
            )
        }

        // Si el encuentro ya concluyo y contamos con los datos completos y sincronizados en cache, evitamos peticiones de red.
        if (!isCacheCompleteAndSynced) {
            val shouldForceDetailRefresh = match.status.state == MatchState.LIVE || cachedDetail != null
            viewModelScope.launch {
                val detailResult = repository.getMatchDetail(
                    league = targetLeague,
                    matchId = match.id,
                    forceRefresh = shouldForceDetailRefresh
                )
                if (_uiState.value.selectedMatch?.id == match.id) {
                    _uiState.update {
                        it.copy(
                            isLoadingDetail = false,
                            matchDetail = detailResult.getOrNull() ?: it.matchDetail
                        )
                    }
                }
            }
        }

        // Consulta desacoplada de posiciones solo si la liga soporta tabla y aun no disponemos de ella.
        if (targetLeague.hasStandings && immediateStandings.isEmpty()) {
            viewModelScope.launch {
                val groupsResult = repository.getGroups(targetLeague)
                val groups = groupsResult.getOrDefault(emptyList())
                val standingsList = if (groups.isNotEmpty()) {
                    groups.firstOrNull { group ->
                        group.standings.any { it.teamId == match.homeTeam.id || it.teamId == match.awayTeam.id }
                    }?.standings ?: repository.getStandings(targetLeague).getOrDefault(emptyList())
                } else {
                    repository.getStandings(targetLeague).getOrDefault(emptyList())
                }
                if (_uiState.value.selectedMatch?.id == match.id) {
                    _uiState.update {
                        it.copy(detailStandings = standingsList)
                    }
                }
            }
        }
    }

    /**
     * Cierra el modal de detalle de partido restableciendo el estado.
     */
    fun dismissMatchDetail() {
        _uiState.update {
            it.copy(
                selectedMatch = null,
                matchDetail = null,
                isLoadingDetail = false,
                detailStandings = emptyList()
            )
        }
    }

    /**
     * Alterna la condicion de favorito de un club deportivo de forma reactiva
     * y vincula sus ligas conocidas para evitar escaneos globales en futuros arranques.
     */
    fun toggleFavoriteTeam(teamId: String) {
        favoriteTeamsRepository.toggleFavorite(teamId)
        if (favoriteTeamsRepository.isFavorite(teamId)) {
            recordDiscoveredLeaguesForTeam(teamId)
        }
    }

    /**
     * Alterna la seleccion de un torneo en el conjunto personalizado de Mis Ligas.
     */
    fun toggleMyLeague(leagueCode: String) {
        myLeaguesRepository.toggleLeague(leagueCode)
    }

    private fun loadData(league: League, forceRefresh: Boolean = false, isSilent: Boolean = false) {
        when (league) {
            League.FAVORITES -> {
                loadFavoriteLeagueData(forceRefresh, isSilent)
                return
            }
            League.MY_LEAGUES -> {
                loadMyLeaguesData(forceRefresh, isSilent)
                return
            }
            League.DEBUG -> {
                loadDebugLeagueData()
                return
            }
            else -> Unit
        }

        viewModelScope.launch {
            if (!isSilent) {
                _uiState.update {
                    it.copy(
                        isLoading = it.matches.isEmpty(),
                        isRefreshing = it.matches.isNotEmpty() && forceRefresh,
                        errorMessage = null
                    )
                }
            }

            // Fase 0: Semilla instantanea desde OkHttp DiskCache en arranque en frio antes de abrir sockets.
            if (!forceRefresh && cachedAllMatches.isEmpty()) {
                val diskMatchesDeferred = async { repository.getMatches(league = league, cacheOnly = true) }
                val diskTablesDeferred = async {
                    if (league.hasStandings) {
                        repository.getStandingsAndGroups(league = league, cacheOnly = true)
                    } else {
                        Result.success(Pair(emptyList(), emptyList()))
                    }
                }
                val diskMatches = diskMatchesDeferred.await().getOrNull().orEmpty()
                val (diskStandingsRaw, diskGroups) = diskTablesDeferred.await()
                    .getOrDefault(Pair(emptyList(), emptyList()))
                if (_uiState.value.selectedLeague != league) return@launch

                if (diskMatches.isNotEmpty() || diskStandingsRaw.isNotEmpty() || diskGroups.isNotEmpty()) {
                    if (diskMatches.isNotEmpty()) {
                        cachedAllMatches = diskMatches
                    }
                    val seededByFilter = buildMatchesByFilter(diskMatches, league, _uiState.value.favoriteTeamIds)
                    val seededStandings = if (diskGroups.isNotEmpty()) emptyList() else diskStandingsRaw
                    val seededBracket = if (league.hasKnockoutBracket && diskMatches.isNotEmpty()) {
                        RadialBracketBuilder.build(diskMatches, league)
                    } else _uiState.value.radialBracket
                    _uiState.update { state ->
                        state.copy(
                            isLoading = diskMatches.isEmpty() && state.isLoading,
                            matches = if (diskMatches.isNotEmpty()) seededByFilter[state.selectedFilter].orEmpty() else state.matches,
                            matchesByFilter = if (diskMatches.isNotEmpty()) seededByFilter else state.matchesByFilter,
                            standings = if (seededStandings.isNotEmpty() || diskGroups.isNotEmpty()) seededStandings else state.standings,
                            groups = if (diskGroups.isNotEmpty()) diskGroups else state.groups,
                            radialBracket = seededBracket
                        )
                    }
                }
            }

            // Fase 2 desacoplada: la tabla se resuelve en su propia corrutina sin retener la aparicion de partidos.
            if (league.hasStandings) {
                launch {
                    val tablesResult = repository.getStandingsAndGroups(league = league, forceRefresh = forceRefresh)
                    if (_uiState.value.selectedLeague != league) return@launch
                    tablesResult.onSuccess { (rawStandings, groups) ->
                        recordFavoritesInLeagueEntries(league, rawStandings)
                        val standings = if (groups.isNotEmpty()) emptyList() else rawStandings
                        _uiState.update { state ->
                            state.copy(
                                standings = standings,
                                groups = groups
                            )
                        }
                    }
                }
            } else {
                _uiState.update { it.copy(standings = emptyList(), groups = emptyList()) }
            }

            // Fase 1 + Fase 2 de partidos: emite la pestana activa tras la primera ronda de /scoreboard
            // mientras completa en segundo plano jornadas vecinas y meses eliminatorios.
            val matchesResult = repository.getMatches(
                league = league,
                forceRefresh = forceRefresh,
                onPreliminaryMatches = { prelimMatches ->
                    if (_uiState.value.selectedLeague == league && cachedAllMatches.isEmpty()) {
                        val prelimByFilter = buildMatchesByFilter(prelimMatches, league, _uiState.value.favoriteTeamIds)
                        val activeMatches = prelimByFilter[_uiState.value.selectedFilter].orEmpty()
                        if (activeMatches.isNotEmpty()) {
                            _uiState.update { state ->
                                if (state.selectedLeague == league && state.isLoading) {
                                    state.copy(
                                        isLoading = false,
                                        matches = activeMatches,
                                        matchesByFilter = prelimByFilter
                                    )
                                } else state
                            }
                        }
                    }
                }
            )

            if (_uiState.value.selectedLeague != league) return@launch

            matchesResult.fold(
                onSuccess = { allMatches ->
                    cachedAllMatches = allMatches
                    recordDiscoveredLeaguesForFavorites(allMatches)
                    syncFavoriteMatchesFollowing(allMatches)
                    val byFilter = buildMatchesByFilter(allMatches, league, _uiState.value.favoriteTeamIds)
                    val radialBracket = if (league.hasKnockoutBracket) {
                        RadialBracketBuilder.build(allMatches, league)
                    } else null

                    val currentSelectedMatch = _uiState.value.selectedMatch
                    val updatedSelectedMatch = if (currentSelectedMatch != null) {
                        allMatches.firstOrNull { it.id == currentSelectedMatch.id } ?: currentSelectedMatch
                    } else null

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            matches = byFilter[it.selectedFilter].orEmpty(),
                            matchesByFilter = byFilter,
                            radialBracket = radialBracket,
                            radialBracketSemis = null,
                            radialBracketEmpty = null,
                            selectedMatch = updatedSelectedMatch,
                            errorMessage = null
                        )
                    }

                    pendingOpenMatchId?.let { pendingId ->
                        val target = allMatches.firstOrNull { it.id == pendingId }
                        if (target != null) {
                            selectMatch(target)
                            pendingOpenMatchId = null
                        }
                    }
                },
                onFailure = { error ->
                    if (!isSilent) {
                        _uiState.update {
                            val hasSeededMatches = it.matches.isNotEmpty()
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                matches = if (hasSeededMatches) it.matches else emptyList(),
                                standings = if (it.standings.isNotEmpty()) it.standings else emptyList(),
                                radialBracket = it.radialBracket ?: if (league.hasKnockoutBracket) {
                                    RadialBracketBuilder.createEmptyTree(league.knockoutBracketTeams, league.hasThirdPlaceMatch)
                                } else null,
                                errorMessage = if (hasSeededMatches) null else error.localizedMessage,
                                errorMessageRes = if (hasSeededMatches) null else com.ixeken.ulama.R.string.error_loading_matches
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Consulta y consolida en paralelo los partidos de las ligas reales para los clubes favoritos.
     */
    private fun loadFavoriteLeagueData(forceRefresh: Boolean = false, isSilent: Boolean = false) {
        viewModelScope.launch {
            val favIds = favoriteTeamsRepository.favoriteTeamIds.value
            if (favIds.isEmpty()) {
                cachedAllMatches = emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        matches = emptyList(),
                        matchesByFilter = emptyMap(),
                        standings = emptyList(),
                        groups = emptyList(),
                        radialBracket = null,
                        errorMessage = null
                    )
                }
                return@launch
            }
            if (!isSilent) {
                _uiState.update {
                    it.copy(
                        isLoading = it.matches.isEmpty(),
                        isRefreshing = it.matches.isNotEmpty() && forceRefresh,
                        errorMessage = null
                    )
                }
            }
            val targetLeagues = resolveFavoriteTargetLeagues(favIds)
            if (!forceRefresh && cachedAllMatches.isEmpty()) {
                val seededFavMatches = coroutineScope {
                    targetLeagues.map { stdLeague ->
                        async {
                            repository.getMatches(stdLeague, cacheOnly = true).getOrDefault(emptyList())
                        }
                    }.awaitAll()
                }.flatten()
                    .distinctBy { it.id }
                    .sortedBy { it.dateUtc }
                    .sanitizeAggregateScores()

                if (_uiState.value.selectedLeague != League.FAVORITES) return@launch
                if (seededFavMatches.isNotEmpty()) {
                    cachedAllMatches = seededFavMatches
                    val seededByFilter = buildMatchesByFilter(seededFavMatches, League.FAVORITES, favIds)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            matches = seededByFilter[it.selectedFilter].orEmpty(),
                            matchesByFilter = seededByFilter
                        )
                    }
                }
            }
            val previousMatchesSnapshot = cachedAllMatches
            val allAggregated = coroutineScope {
                targetLeagues.map { stdLeague ->
                    async {
                        val fallbackForLeague = previousMatchesSnapshot.filter { it.league == stdLeague }
                        repository.getMatches(stdLeague, forceRefresh = forceRefresh).getOrElse { fallbackForLeague }
                    }
                }.awaitAll()
            }.flatten()
                .distinctBy { it.id }
                .sortedBy { it.dateUtc }
                .sanitizeAggregateScores()

            if (_uiState.value.selectedLeague != League.FAVORITES) return@launch
                    cachedAllMatches = allAggregated
                    recordDiscoveredLeaguesForFavorites(allAggregated)
                    syncFavoriteMatchesFollowing(allAggregated)
                    val byFilter = buildMatchesByFilter(allAggregated, League.FAVORITES, favIds)
                    val currentSelectedMatch = _uiState.value.selectedMatch
                    val updatedSelectedMatch = if (currentSelectedMatch != null) {
                        allAggregated.firstOrNull { it.id == currentSelectedMatch.id } ?: currentSelectedMatch
                    } else null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            matches = byFilter[it.selectedFilter].orEmpty(),
                            matchesByFilter = byFilter,
                            standings = emptyList(),
                            groups = emptyList(),
                            radialBracket = null,
                            radialBracketSemis = null,
                            radialBracketEmpty = null,
                            selectedMatch = updatedSelectedMatch,
                            errorMessage = null
                        )
                    }

                    pendingOpenMatchId?.let { pendingId ->
                        val target = allAggregated.firstOrNull { it.id == pendingId }
                        if (target != null) {
                            selectMatch(target)
                            pendingOpenMatchId = null
                        }
                    }
        }
    }

    /**
     * Consulta y consolida en paralelo los partidos de las ligas seleccionadas por el usuario en Mis Ligas.
     * Si no hay ligas seleccionadas, limpia el estado de partidos sin emitir errores de red.
     */
    private fun loadMyLeaguesData(forceRefresh: Boolean = false, isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                _uiState.update {
                    it.copy(
                        isLoading = it.matches.isEmpty(),
                        isRefreshing = it.matches.isNotEmpty() && forceRefresh,
                        errorMessage = null
                    )
                }
            }
            val activeCodes = myLeaguesRepository.myLeagueCodes.value
            val targetLeagues = League.standardLeagues.filter { it.code in activeCodes }
            if (targetLeagues.isEmpty()) {
                cachedAllMatches = emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        matches = emptyList(),
                        matchesByFilter = emptyMap(),
                        standings = emptyList(),
                        groups = emptyList(),
                        radialBracket = null,
                        errorMessage = null
                    )
                }
                return@launch
            }
            if (!forceRefresh && cachedAllMatches.isEmpty()) {
                val seededMyMatches = coroutineScope {
                    targetLeagues.map { stdLeague ->
                        async {
                            repository.getMatches(stdLeague, cacheOnly = true).getOrDefault(emptyList())
                        }
                    }.awaitAll()
                }.flatten()
                    .distinctBy { it.id }
                    .sortedBy { it.dateUtc }
                    .sanitizeAggregateScores()

                if (_uiState.value.selectedLeague != League.MY_LEAGUES) return@launch
                if (seededMyMatches.isNotEmpty()) {
                    cachedAllMatches = seededMyMatches
                    val seededByFilter = buildMatchesByFilter(seededMyMatches, League.MY_LEAGUES, _uiState.value.favoriteTeamIds)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            matches = seededByFilter[it.selectedFilter].orEmpty(),
                            matchesByFilter = seededByFilter
                        )
                    }
                }
            }
            val previousMatchesSnapshot = cachedAllMatches
            val allAggregated = coroutineScope {
                targetLeagues.map { stdLeague ->
                    async {
                        val fallbackForLeague = previousMatchesSnapshot.filter { it.league == stdLeague }
                        repository.getMatches(stdLeague, forceRefresh = forceRefresh).getOrElse { fallbackForLeague }
                    }
                }.awaitAll()
            }.flatten()
                .distinctBy { it.id }
                .sortedBy { it.dateUtc }
                .sanitizeAggregateScores()

            if (_uiState.value.selectedLeague != League.MY_LEAGUES) return@launch

            cachedAllMatches = allAggregated
                    recordDiscoveredLeaguesForFavorites(allAggregated)
                    syncFavoriteMatchesFollowing(allAggregated)
                    val byFilter = buildMatchesByFilter(allAggregated, League.MY_LEAGUES, _uiState.value.favoriteTeamIds)
                    val currentSelectedMatch = _uiState.value.selectedMatch
                    val updatedSelectedMatch = if (currentSelectedMatch != null) {
                        allAggregated.firstOrNull { it.id == currentSelectedMatch.id } ?: currentSelectedMatch
                    } else null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            matches = byFilter[it.selectedFilter].orEmpty(),
                            matchesByFilter = byFilter,
                            standings = emptyList(),
                            groups = emptyList(),
                            radialBracket = null,
                            radialBracketSemis = null,
                            radialBracketEmpty = null,
                            selectedMatch = updatedSelectedMatch,
                            errorMessage = null
                        )
                    }

                    pendingOpenMatchId?.let { pendingId ->
                        val target = allAggregated.firstOrNull { it.id == pendingId }
                        if (target != null) {
                            selectMatch(target)
                            pendingOpenMatchId = null
                        }
                    }
        }
    }

    /**
     * Carga el conjunto local de pruebas para League.DEBUG sin llamadas a la red.
     */
    private fun loadDebugLeagueData() {
        val allMatches = com.ixeken.ulama.data.debug.DebugLeagueData.getMatches()
        cachedAllMatches = allMatches
        val byFilter = buildMatchesByFilter(allMatches, League.DEBUG, _uiState.value.favoriteTeamIds)
        val standings = com.ixeken.ulama.data.debug.DebugLeagueData.getStandings()
        val groups = com.ixeken.ulama.data.debug.DebugLeagueData.getGroups()
        val radialBracket = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTree(16)
        val radialBracketSemis = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTreeSemis(16)
        val radialBracketEmpty = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTreeEmpty(16)
        val radialBracket32 = com.ixeken.ulama.data.debug.DebugLeagueData.getRadialBracketTree(32)
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                matches = byFilter[it.selectedFilter].orEmpty(),
                matchesByFilter = byFilter,
                standings = standings,
                groups = groups,
                radialBracket = radialBracket,
                radialBracketSemis = radialBracketSemis,
                radialBracketEmpty = radialBracketEmpty,
                radialBracket32 = radialBracket32,
                errorMessage = null
            )
        }
    }

    /**
     * Filtra los partidos de la vista agregada donde compite al menos uno de los clubes favoritos.
     */
    private fun filterFavoriteMatches(matches: List<Match>, favoriteIds: Set<String>, filter: TemporalFilter): List<Match> {
        if (favoriteIds.isEmpty()) return emptyList()
        val favMatches = matches.filter { match ->
            match.homeTeam.id in favoriteIds || match.awayTeam.id in favoriteIds
        }
        return filterMatches(favMatches, filter)
    }

    /**
     * Determina las ligas donde participan los equipos favoritos activos cuando las tablas
     * ya se encuentran en memoria, reduciendo las consultas paralelas solo a los torneos relevantes
     * sin excluir ligas sin tabla (como Amistosos) ni competiciones donde el club ya tiene partidos cargados.
     */
    private fun resolveFavoriteTargetLeagues(favIds: Set<String>): List<League> {
        if (favIds.isEmpty()) return emptyList()
        val teamsByLeague = _uiState.value.availableTeamsByLeague
        val persistedCodes = favoriteTeamsRepository.favoriteLeagueCodes.value
        val hasAllMapped = favoriteTeamsRepository.hasMappedLeaguesForAllFavorites()

        if (teamsByLeague.isEmpty() && (!hasAllMapped || persistedCodes.isEmpty())) {
            return League.standardLeagues
        }

        val leaguesFromCachedMatches = cachedAllMatches
            .filter { it.homeTeam.id in favIds || it.awayTeam.id in favIds }
            .map { it.league }
            .toSet()

        if (teamsByLeague.isEmpty()) {
            val mappedLeagues = League.standardLeagues.filter { it.code in persistedCodes }.toSet()
            val hasInternational = mappedLeagues.any { it.category == LeagueCategory.INTERNATIONAL }
            val hasClub = mappedLeagues.any { it.category == LeagueCategory.CLUBS }
            val matchedFromDisk = League.standardLeagues.filter { league ->
                when {
                    league in mappedLeagues || league in leaguesFromCachedMatches -> true
                    hasInternational && league.category == LeagueCategory.INTERNATIONAL && league.tournamentYear == null -> true
                    hasClub && league.category == LeagueCategory.CLUBS && league.isTournament && league.tournamentYear == null -> true
                    else -> false
                }
            }
            return matchedFromDisk.ifEmpty { League.standardLeagues }
        }

        val hasInternationalFavorite = teamsByLeague.any { (league, entries) ->
            league.category == LeagueCategory.INTERNATIONAL && entries.any { it.teamId in favIds }
        }
        val matchedLeagues = League.standardLeagues.filter { league ->
            if (league.code in persistedCodes || league in leaguesFromCachedMatches || !league.hasStandings) {
                true
            } else if (hasInternationalFavorite && league.category == LeagueCategory.INTERNATIONAL && league.tournamentYear == null) {
                true
            } else {
                val entries = teamsByLeague[league]
                if (!entries.isNullOrEmpty()) {
                    entries.any { it.teamId in favIds }
                } else {
                    league.tournamentYear == null
                }
            }
        }
        return matchedLeagues.ifEmpty { League.standardLeagues }
    }

    private fun recordDiscoveredLeaguesForTeam(teamId: String) {
        if (teamId.isBlank() || !favoriteTeamsRepository.isFavorite(teamId)) return
        val fromTables = _uiState.value.availableTeamsByLeague
            .filter { (_, entries) -> entries.any { it.teamId == teamId } }
            .keys
            .map { it.code }
        val fromMatches = cachedAllMatches
            .filter { !it.league.isVirtual && (it.homeTeam.id == teamId || it.awayTeam.id == teamId) }
            .map { it.league.code }
        favoriteTeamsRepository.recordTeamLeagues(teamId, fromTables + fromMatches)
    }

    private fun recordFavoritesInLeagueEntries(league: League, entries: List<StandingEntry>) {
        if (league.isVirtual || entries.isEmpty()) return
        val favIds = favoriteTeamsRepository.favoriteTeamIds.value
        if (favIds.isEmpty()) return
        for (entry in entries) {
            if (entry.teamId in favIds) {
                favoriteTeamsRepository.recordTeamLeagues(entry.teamId, listOf(league.code))
            }
        }
    }

    private fun recordDiscoveredLeaguesForFavorites(matches: List<Match>) {
        val favIds = favoriteTeamsRepository.favoriteTeamIds.value
        if (favIds.isEmpty() || matches.isEmpty()) return
        for (favId in favIds) {
            val codes = matches
                .asSequence()
                .filter { !it.league.isVirtual && (it.homeTeam.id == favId || it.awayTeam.id == favId) }
                .map { it.league.code }
                .toSet()
            if (codes.isNotEmpty()) {
                favoriteTeamsRepository.recordTeamLeagues(favId, codes)
            }
        }
    }

    /**
     * Precarga en paralelo los clubes de competiciones pendientes para busquedas globales
     * y la vista agregada de Mis equipos, preservando en cache las ligas ya cargadas.
     */
    private fun loadAvailableTeamsByLeague(forceRefresh: Boolean = false) {
        if (isLoadingAvailableTeams && !forceRefresh) return
        isLoadingAvailableTeams = true
        viewModelScope.launch {
            try {
                val existing = _uiState.value.availableTeamsByLeague
                val pendingLeagues = if (forceRefresh) {
                    League.standardLeagues
                } else {
                    League.standardLeagues.filter { existing[it].isNullOrEmpty() }
                }
                if (pendingLeagues.isNotEmpty()) {
                    val entries = coroutineScope {
                        pendingLeagues.map { league ->
                            async { league to fetchLeagueTeams(league, forceRefresh = forceRefresh) }
                        }.awaitAll()
                    }
                    val map = entries.filter { it.second.isNotEmpty() }.toMap()
                    if (map.isNotEmpty()) {
                        map.forEach { (league, teams) -> recordFavoritesInLeagueEntries(league, teams) }
                        _uiState.update {
                            it.copy(availableTeamsByLeague = it.availableTeamsByLeague + map)
                        }
                    }
                }
                hasLoadedAllAvailableTeams = true
            } finally {
                isLoadingAvailableTeams = false
            }
        }
    }

    /**
     * Unifica la resolucion de participantes de una liga combinando su tabla de posiciones
     * con clubes de encuentros directos en torneos eliminatorios o competiciones sin clasificacion.
     */
    private suspend fun fetchLeagueTeams(league: League, forceRefresh: Boolean): List<StandingEntry> {
        val standings = if (league.hasStandings) {
            repository.getStandings(league, forceRefresh = forceRefresh).getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val shouldIncludeMatches = (standings.isEmpty() || league.hasKnockoutBracket) &&
            league.tournamentYear == null
        val matchTeams = if (shouldIncludeMatches) {
            val matches = repository.getMatches(league, forceRefresh = false).getOrDefault(emptyList())
            val standingTeamIds = standings.map { it.teamId }.toSet()
            matches.flatMap { listOf(it.homeTeam, it.awayTeam) }
                .filter { it.id.isNotBlank() && !it.isPlaceholder && it.id !in standingTeamIds }
                .distinctBy { it.id }
                .sortedBy { it.name }
                .mapIndexed { idx, team ->
                    StandingEntry(
                        rank = idx + 1,
                        teamName = team.name,
                        teamShortName = team.shortName,
                        teamAbbreviation = team.abbreviation,
                        logoUrl = team.logoUrl,
                        gamesPlayed = 0,
                        wins = 0,
                        draws = 0,
                        losses = 0,
                        goalsFor = 0,
                        goalsAgainst = 0,
                        goalDifference = 0,
                        points = 0,
                        teamId = team.id
                    )
                }
        } else {
            emptyList()
        }
        return (matchTeams + standings).distinctBy { it.teamId.ifBlank { it.teamName } }
    }

    /**
     * Genera en memoria la particion simultanea de los tres filtros temporales
     * para habilitar transiciones fluidas de HorizontalPager sin parpadeos.
     */
    private fun buildMatchesByFilter(
        matches: List<Match>,
        league: League,
        favoriteIds: Set<String>
    ): Map<TemporalFilter, List<Match>> = partitionMatchesByFilter(
        matches = matches,
        isFavoritesLeague = league == League.FAVORITES,
        favoriteIds = favoriteIds
    )

    /**
     * Realiza la particion de partidos segun el filtro temporal y el estado del encuentro.
     * En partidos concluidos (PAST), los ordena descendentemente para mostrar primero los mas recientes,
     * excluyendo los partidos del dia de hoy para que permanezcan exclusivamente en TODAY hasta el dia siguiente.
     */
    private fun filterMatches(matches: List<Match>, filter: TemporalFilter): List<Match> =
        filterMatchesByTemporalFilter(matches, filter)

    /**
     * Alterna el seguimiento de un partido para notificaciones en vivo y Samsung Now Bar.
     */
    fun toggleFollow(context: Context, match: Match, detail: com.ixeken.ulama.core.model.MatchDetail?) {
        val matchId = match.id
        if (followRepository.isFollowing(matchId)) {
            manuallyUnfollowedMatchIds.add(matchId)
            followRepository.unfollow(matchId)
            MatchAlarmScheduler.cancelKickoff(context, matchId)
            MatchLiveNotificationService.stop(context, matchId)
        } else {
            manuallyUnfollowedMatchIds.remove(matchId)
            // Un partido finalizado no debe generar alarmas ni servicios de notificacion en segundo plano
            if (match.isFinished) return
            val followed = match.toFollowedMatch()
            followRepository.follow(followed)
            if (followed.state == MatchState.LIVE) {
                MatchLiveNotificationService.start(context, matchId)
            } else if (followed.state == MatchState.SCHEDULED) {
                MatchAlarmScheduler.scheduleKickoff(context, followed)
            }
        }
    }


    /**
     * Sincroniza y reconcilia el seguimiento de partidos para los clubes favoritos activos si la opcion esta habilitada.
     */
    private fun syncFavoriteMatchesFollowing(matches: List<Match>) {
        val settings = appSettingsRepository?.settings?.value ?: return
        val favIds = favoriteTeamsRepository.favoriteTeamIds.value
        FavoriteAutoFollowSync.syncMatches(
            matches = matches,
            favIds = favIds,
            isAutoFollowEnabled = settings.autoFollowFavorites,
            manuallyUnfollowedIds = manuallyUnfollowedMatchIds,
            getExistingFollowed = followRepository::getFollowed,
            saveFollowed = followRepository::follow,
            leadMinutes = settings.notificationLeadMinutes
        ) { followed, shouldStartNow ->
            appContext?.let { ctx ->
                if (shouldStartNow) {
                    MatchLiveNotificationService.start(ctx, followed.matchId)
                } else if (followed.state == MatchState.SCHEDULED) {
                    MatchAlarmScheduler.scheduleKickoff(ctx, followed, settings.notificationLeadMinutes)
                }
            }
        }
    }

    /**
     * Agenda el escaneo de seguimiento automatico de favoritos tras completar el arranque inicial
     * evitando duplicar rafagas de red durante la apertura en frio.
     */
    private fun scheduleAutoFollowScan(delayMs: Long = AUTO_FOLLOW_STARTUP_DELAY_MS) {
        val isAutoFollow = appSettingsRepository?.settings?.value?.autoFollowFavorites ?: false
        val favIds = favoriteTeamsRepository.favoriteTeamIds.value
        if (!isAutoFollow || favIds.isEmpty()) return

        syncFavoriteMatchesFollowing(cachedAllMatches)
        autoFollowScanJob?.cancel()
        autoFollowScanJob = viewModelScope.launch {
            if (delayMs > 0L) delay(delayMs)
            scanAndAutoFollowAllFavorites()
        }
    }

    /**
     * Escanea en segundo plano las competiciones relevantes para activar el seguimiento de los clubes favoritos.
     */
    private suspend fun scanAndAutoFollowAllFavorites() {
        val isAutoFollow = appSettingsRepository?.settings?.value?.autoFollowFavorites ?: false
        val favIds = favoriteTeamsRepository.favoriteTeamIds.value
        if (!isAutoFollow || favIds.isEmpty()) return

        try {
            val targetLeagues = resolveFavoriteTargetLeagues(favIds)
            val allAggregated = coroutineScope {
                targetLeagues.map { stdLeague ->
                    async {
                        repository.getMatches(stdLeague, forceRefresh = false).getOrDefault(emptyList())
                    }
                }.awaitAll()
            }.flatten()
            recordDiscoveredLeaguesForFavorites(allAggregated)
            syncFavoriteMatchesFollowing(allAggregated)
        } catch (_: Exception) {
            // Silencioso ante fallos de red en segundo plano
        }
    }

    companion object {
        private const val PREFS_NAME = "ulama_user_preferences"
        private const val KEY_SELECTED_LEAGUE = "selected_league_code"
        private const val FILTER_REFRESH_COOLDOWN_MS = 30_000L
        private const val AUTO_FOLLOW_STARTUP_DELAY_MS = 2_500L

        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val repo = MatchRepositoryImpl.getInstance(appContext)
                    val followRepo = MatchFollowRepository.getInstance(appContext)
                    val favRepo = FavoriteTeamsRepositoryImpl.getInstance(appContext)
                    val myLeaguesRepo = MyLeaguesRepositoryImpl.getInstance(appContext)
                    val settingsRepo = AppSettingsRepositoryImpl.provide(appContext)
                    val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    return MatchesViewModel(repo, followRepo, favRepo, myLeaguesRepo, prefs, settingsRepo, appContext) as T
                }
            }
    }
}

/**
 * Particiona una coleccion de partidos en las tres pestanas temporales de forma determinista y pura.
 */
internal fun partitionMatchesByFilter(
    matches: List<Match>,
    isFavoritesLeague: Boolean,
    favoriteIds: Set<String>
): Map<TemporalFilter, List<Match>> {
    val relevantMatches = if (isFavoritesLeague) {
        if (favoriteIds.isEmpty()) emptyList()
        else matches.filter { it.homeTeam.id in favoriteIds || it.awayTeam.id in favoriteIds }
    } else {
        matches
    }
    return TemporalFilter.entries.associateWith { filter ->
        filterMatchesByTemporalFilter(relevantMatches, filter)
    }
}

private fun filterMatchesByTemporalFilter(
    matches: List<Match>,
    filter: TemporalFilter
): List<Match> = when (filter) {
    TemporalFilter.PAST -> matches
        .filter {
            it.status.state == MatchState.FINISHED &&
                !DateTimeUtils.isToday(it.dateUtc) &&
                !DateTimeUtils.isAfterToday(it.dateUtc)
        }
        .sortedByDescending { it.dateUtc }
    TemporalFilter.TODAY -> matches
        .filter { DateTimeUtils.isToday(it.dateUtc) || it.status.state == MatchState.LIVE }
        .sortedBy { it.dateUtc }
    TemporalFilter.UPCOMING -> matches
        .filter { it.status.state == MatchState.SCHEDULED && DateTimeUtils.isAfterToday(it.dateUtc) }
        .sortedBy { it.dateUtc }
}
