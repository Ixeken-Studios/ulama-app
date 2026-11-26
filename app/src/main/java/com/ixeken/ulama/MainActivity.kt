package com.ixeken.ulama

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixeken.ulama.core.model.ColorPalette
import com.ixeken.ulama.core.util.LocaleHelper
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.MotionStyle
import com.ixeken.ulama.core.model.TemporalFilter
import com.ixeken.ulama.core.model.ThemeMode
import kotlinx.coroutines.delay
import com.ixeken.ulama.core.util.DateTimeUtils
import com.ixeken.ulama.core.util.ImagePreloadHelper
import com.ixeken.ulama.data.repository.AppSettingsRepositoryImpl
import com.ixeken.ulama.service.FavoriteAutoFollowJobService
import com.ixeken.ulama.service.MatchAlarmScheduler
import com.ixeken.ulama.service.MatchNotificationHelper
import com.ixeken.ulama.ui.competitions.CompetitionsScreen
import com.ixeken.ulama.ui.components.EmptyFavoritesCard
import com.ixeken.ulama.ui.components.EmptyMatchesCard
import com.ixeken.ulama.ui.components.EmptyMyLeaguesCard
import com.ixeken.ulama.ui.components.MatchesGroupCard
import com.ixeken.ulama.ui.components.PullToRefreshWithLoadingIndicator
import com.ixeken.ulama.ui.components.RadialKnockoutBracket
import com.ixeken.ulama.ui.components.StandingsTable
import com.ixeken.ulama.ui.components.TemporalFilterTabs
import com.ixeken.ulama.ui.components.UlamaTopAppBar
import com.ixeken.ulama.ui.components.detail.MatchDetailBottomSheet
import com.ixeken.ulama.ui.matches.MatchesUiState
import com.ixeken.ulama.ui.matches.MatchesViewModel
import com.ixeken.ulama.ui.matches.resolveQuickSwitchLeagues
import com.ixeken.ulama.ui.settings.SettingsScreen
import com.ixeken.ulama.ui.theme.UlamaTheme
import com.ixeken.ulama.data.update.GithubUpdateChecker
import com.ixeken.ulama.data.update.UpdateCheckResult
import com.ixeken.ulama.ui.components.OnboardingBottomSheet
import com.ixeken.ulama.ui.components.UpdateAvailableBottomSheet
import com.ixeken.ulama.ui.components.WhatsNewBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val pendingNotificationMatchId = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(MatchNotificationHelper.EXTRA_OPEN_MATCH_ID)?.let { matchId ->
            pendingNotificationMatchId.value = matchId
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.getStringExtra(MatchNotificationHelper.EXTRA_OPEN_MATCH_ID)?.let { matchId ->
            pendingNotificationMatchId.value = matchId
        }
        setContent {
            val context = LocalContext.current
            val settingsRepository = remember { AppSettingsRepositoryImpl.provide(context) }
            val appSettings by settingsRepository.settings.collectAsStateWithLifecycle()

            var isCompetitionsOpen by rememberSaveable { mutableStateOf(false) }
            var competitionsInitialTeamsCode by rememberSaveable { mutableStateOf<String?>(null) }
            var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
            var isWhatsNewSheetOpen by remember { mutableStateOf(false) }
            var isOnboardingSheetOpen by remember { mutableStateOf(false) }
            var updateAvailableData by remember { mutableStateOf<UpdateCheckResult.NewVersionAvailable?>(null) }

            val openCompetitions: (League?) -> Unit = { initialLeague ->
                competitionsInitialTeamsCode = initialLeague?.code
                isCompetitionsOpen = true
            }

            BackHandler(enabled = isSettingsOpen) {
                isSettingsOpen = false
            }

            val darkTheme = when (appSettings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val isExpressive = appSettings.motionStyle == MotionStyle.EXPRESSIVE

            val view = LocalView.current
            if (!view.isInEditMode) {
                DisposableEffect(darkTheme) {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                    onDispose {}
                }
            }

            UlamaTheme(
                darkTheme = darkTheme,
                colorPalette = appSettings.colorPalette,
                isExpressiveMotion = isExpressive,
                dataSaver = appSettings.dataSaverMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    val viewModel: MatchesViewModel = viewModel(factory = MatchesViewModel.provideFactory(context))
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_notification_permission_needed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(appSettings.hasCompletedOnboarding) {
                    if (!appSettings.hasCompletedOnboarding) {
                        isOnboardingSheetOpen = true
                    }
                }

                LaunchedEffect(Unit) {
                    if (appSettings.hasCompletedOnboarding &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val currentVersion = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.2.1"
                    } catch (_: Exception) {
                        "2.2.1"
                    }

                    val savedVersion = settingsRepository.settings.value.lastKnownVersion

                    when {
                        !appSettings.hasCompletedOnboarding -> settingsRepository.setLastKnownVersion(currentVersion)
                        savedVersion.isNotEmpty() && savedVersion != currentVersion -> {
                            isWhatsNewSheetOpen = true
                            settingsRepository.setLastKnownVersion(currentVersion)
                        }
                        savedVersion.isEmpty() -> settingsRepository.setLastKnownVersion(currentVersion)
                    }

                    if (appSettings.checkUpdatesOnStart) {
                        val checker = GithubUpdateChecker(context)
                        val result = checker.checkForUpdates()
                        if (result is UpdateCheckResult.NewVersionAvailable) {
                            updateAvailableData = result
                        }
                    }
                }

                val openMatchId by pendingNotificationMatchId
                LaunchedEffect(openMatchId) {
                    val targetId = openMatchId
                    if (!targetId.isNullOrBlank()) {
                        isSettingsOpen = false
                        isCompetitionsOpen = false
                        competitionsInitialTeamsCode = null
                        viewModel.selectMatchById(targetId)
                        pendingNotificationMatchId.value = null
                    }
                }

                LifecycleResumeEffect(Unit) {
                    MatchAlarmScheduler.rescheduleAllFollowed(context)
                    FavoriteAutoFollowJobService.syncSchedule(context)
                    viewModel.silentRefresh()
                    onPauseOrDispose {}
                }

                // Sondeo automatico cada 30s exclusivamente en primer plano (RESUMED) y con partidos en vivo
                val hasLiveMatches = remember(uiState.matches, uiState.selectedMatch) {
                    uiState.matches.any { it.status.state == MatchState.LIVE } ||
                        uiState.selectedMatch?.status?.state == MatchState.LIVE
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner, hasLiveMatches) {
                    if (!hasLiveMatches) return@LaunchedEffect
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        while (true) {
                            delay(30_000L)
                            viewModel.silentRefresh()
                        }
                    }
                }

                // Precargamos los escudos cuando el centro de competiciones tiene clubes resueltos
                // exclusivamente para la variante de tema activa y solo si el ahorro de datos esta inactivo.
                LaunchedEffect(isCompetitionsOpen, uiState.availableTeamsByLeague, darkTheme, appSettings.dataSaverMode) {
                    if (!appSettings.dataSaverMode && isCompetitionsOpen && uiState.availableTeamsByLeague.isNotEmpty()) {
                        val urls = uiState.availableTeamsByLeague.values.flatten().mapNotNull { entry ->
                            entry.getLogoUrl(darkTheme)
                        }
                        ImagePreloadHelper.preloadImages(
                            context = context,
                            urls = urls,
                            dataSaverEnabled = appSettings.dataSaverMode
                        )
                    }
                }

                // Precargamos unicamente los escudos de la pestana visible tras pintar los partidos,
                // evitando competir con el JSON inicial ni descargar escudos fuera de pantalla.
                LaunchedEffect(uiState.matches, uiState.isLoading, darkTheme, appSettings.dataSaverMode) {
                    if (!appSettings.dataSaverMode && !uiState.isLoading && uiState.matches.isNotEmpty()) {
                        val urls = uiState.matches.flatMap { match ->
                            listOfNotNull(
                                match.homeTeam.getLogoUrl(darkTheme),
                                match.awayTeam.getLogoUrl(darkTheme)
                            )
                        }
                        ImagePreloadHelper.preloadImages(
                            context = context,
                            urls = urls,
                            dataSaverEnabled = appSettings.dataSaverMode
                        )
                    }
                }

                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val horizontalMargin = if (screenWidth >= UlamaTheme.dimensions.maxContentWidth) {
                    UlamaTheme.spacing.screenHorizontalExpanded
                } else {
                    UlamaTheme.spacing.screenHorizontalCompact
                }
                val standardLeagueNames = League.standardLeagues.associateWith { stringResource(it.nameRes) }
                val quickLeagues = remember(uiState.myLeagueCodes, standardLeagueNames) {
                    resolveQuickSwitchLeagues(uiState.myLeagueCodes) { league ->
                        standardLeagueNames[league].orEmpty()
                    }
                }
                var leagueTransitionDirection by remember { mutableIntStateOf(0) }
                // Muestra el mini paginado al entrar a la app y al interactuar con la pildora, ocultandolo tras una pausa breve
                var showLeaguePagerIndicator by rememberSaveable { mutableStateOf(true) }
                var isLeaguePillDragging by remember { mutableStateOf(false) }
                LaunchedEffect(
                    showLeaguePagerIndicator,
                    isLeaguePillDragging,
                    uiState.selectedLeague,
                    isOnboardingSheetOpen,
                    quickLeagues.size
                ) {
                    if (showLeaguePagerIndicator && !isLeaguePillDragging && !isOnboardingSheetOpen && quickLeagues.size > 1) {
                        delay(2_500L)
                        showLeaguePagerIndicator = false
                    }
                }

                if (isSettingsOpen) {
                    SettingsScreen(
                        appSettings = appSettings,
                        currentLanguage = LocaleHelper.getCurrentLanguage(context),
                        onThemeModeChanged = settingsRepository::setThemeMode,
                        onColorPaletteChanged = settingsRepository::setColorPalette,
                        onMotionStyleChanged = settingsRepository::setMotionStyle,
                        onAppLanguageChanged = { language ->
                            settingsRepository.setAppLanguage(language)
                            LocaleHelper.applyLanguage(context, language)
                        },
                        onNotificationLeadMinutesChanged = { minutes ->
                            settingsRepository.setNotificationLeadMinutes(minutes)
                            MatchAlarmScheduler.rescheduleAllFollowed(context, minutes)
                        },
                        onAutoFollowFavoritesChanged = settingsRepository::setAutoFollowFavorites,
                        onDataSaverModeChanged = settingsRepository::setDataSaverMode,
                        onCheckUpdatesOnStartChanged = settingsRepository::setCheckUpdatesOnStart,
                        onShowWalkthrough = { isOnboardingSheetOpen = true },
                        onBackClick = { isSettingsOpen = false }
                    )
                } else if (isCompetitionsOpen) {
                    CompetitionsScreen(
                        selectedLeague = uiState.selectedLeague,
                        favoriteTeamIds = uiState.favoriteTeamIds,
                        myLeagueCodes = uiState.myLeagueCodes,
                        availableTeamsByLeague = uiState.availableTeamsByLeague,
                        loadingTeamLeagues = uiState.loadingTeamLeagues,
                        initialTeamsLeague = competitionsInitialTeamsCode?.let { League.fromCode(it) },
                        onSelectLeague = { league ->
                            leagueTransitionDirection = 0
                            viewModel.selectLeague(league)
                        },
                        onToggleMyLeague = viewModel::toggleMyLeague,
                        onToggleFavoriteTeam = viewModel::toggleFavoriteTeam,
                        onEnsureLeagueTeamsLoaded = viewModel::ensureLeagueTeamsLoaded,
                        onEnsureAllTeamsLoaded = viewModel::ensureAvailableTeamsLoaded,
                        onClose = {
                            isCompetitionsOpen = false
                            competitionsInitialTeamsCode = null
                        }
                    )
                } else {
                    val motion = UlamaTheme.motion
                    val coroutineScope = rememberCoroutineScope()
                    val pagerState = rememberPagerState(
                        initialPage = uiState.selectedFilter.ordinal,
                        pageCount = { TemporalFilter.entries.size }
                    )
                    // Sin rebote lateral para evitar dejar bordes vecinos visibles al detenerse
                    val pagerSnapSpec = remember(motion.mode) {
                        spring<Float>(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = if (motion.isExpressive) Spring.StiffnessMediumLow else Spring.StiffnessMedium
                        )
                    }
                    val activeTabFilter by remember(pagerState) {
                        derivedStateOf {
                            TemporalFilter.entries.getOrElse(pagerState.targetPage) { TemporalFilter.TODAY }
                        }
                    }
                    val currentSelectedFilter by rememberUpdatedState(uiState.selectedFilter)

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.settledPage }
                            .distinctUntilChanged()
                            .collect { settled ->
                                val settledFilter = TemporalFilter.entries.getOrElse(settled) { TemporalFilter.TODAY }
                                if (currentSelectedFilter != settledFilter) {
                                    viewModel.selectFilter(settledFilter)
                                }
                            }
                    }

                    LaunchedEffect(uiState.selectedFilter) {
                        val targetPage = uiState.selectedFilter.ordinal
                        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
                            pagerState.animateScrollToPage(
                                page = targetPage,
                                animationSpec = pagerSnapSpec
                            )
                        }
                    }

                    LaunchedEffect(uiState.selectedLeague) {
                        if (leagueTransitionDirection != 0) {
                            leagueTransitionDirection = 0
                        }
                    }

                    Scaffold(
                        topBar = {
                            UlamaTopAppBar(
                                selectedLeague = uiState.selectedLeague,
                                onLeaguePickerClick = { openCompetitions(null) },
                                onSettingsClick = { isSettingsOpen = true },
                                quickLeagues = quickLeagues,
                                onQuickLeagueSelected = { targetLeague, direction ->
                                    leagueTransitionDirection = direction
                                    showLeaguePagerIndicator = true
                                    viewModel.selectLeague(targetLeague)
                                },
                                showPagerIndicator = showLeaguePagerIndicator,
                                onLeaguePillDragChange = { dragging ->
                                    isLeaguePillDragging = dragging
                                    if (dragging) {
                                        showLeaguePagerIndicator = true
                                    }
                                }
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = innerPadding.calculateTopPadding())
                                .padding(horizontal = horizontalMargin)
                                .padding(top = UlamaTheme.spacing.screenVertical)
                        ) {
                            TemporalFilterTabs(
                                selectedFilter = activeTabFilter,
                                onFilterSelected = { filter ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = filter.ordinal,
                                            animationSpec = pagerSnapSpec
                                        )
                                    }
                                    viewModel.selectFilter(filter)
                                },
                                modifier = Modifier.widthIn(max = UlamaTheme.dimensions.maxContentWidth)
                            )

                            Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))

                            AnimatedContent(
                                targetState = uiState.selectedLeague,
                                transitionSpec = {
                                    val dir = leagueTransitionDirection
                                    if (dir != 0) {
                                        (slideInHorizontally(animationSpec = motion.spatialSpec()) { fullWidth ->
                                            dir * (fullWidth / 4)
                                        } + fadeIn(animationSpec = motion.effectsSpec())) togetherWith
                                            (slideOutHorizontally(animationSpec = motion.spatialSpec()) { fullWidth ->
                                                -dir * (fullWidth / 4)
                                            } + fadeOut(animationSpec = motion.effectsSpec()))
                                    } else {
                                        fadeIn(animationSpec = motion.effectsSpec()) togetherWith
                                            fadeOut(animationSpec = motion.effectsSpec())
                                    }
                                },
                                label = "MainLeagueContentTransition",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
                            ) { currentLeague ->
                                if (uiState.isLoading || currentLeague != uiState.selectedLeague) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ContainedLoadingIndicator(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            indicatorColor = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    PullToRefreshWithLoadingIndicator(
                                        isRefreshing = uiState.isRefreshing,
                                        onRefresh = viewModel::refresh,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        HorizontalPager(
                                            state = pagerState,
                                            beyondViewportPageCount = 0,
                                            key = { TemporalFilter.entries[it].name },
                                            flingBehavior = PagerDefaults.flingBehavior(
                                                state = pagerState,
                                                snapAnimationSpec = pagerSnapSpec
                                            ),
                                            modifier = Modifier.fillMaxSize()
                                        ) { page ->
                                            val pageFilter = TemporalFilter.entries[page]
                                            val pageMatches = uiState.matchesByFilter[pageFilter]
                                                ?: if (pageFilter == uiState.selectedFilter) uiState.matches else emptyList()

                                            MatchesLeaguePageContent(
                                                league = currentLeague,
                                                pageFilter = pageFilter,
                                                matches = pageMatches,
                                                uiState = uiState,
                                                bottomContentPadding = innerPadding.calculateBottomPadding(),
                                                onMatchClick = viewModel::selectMatch,
                                                onManageFavorites = { openCompetitions(League.FAVORITES) },
                                                onManageMyLeagues = { openCompetitions(null) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            val selectedMatch = uiState.selectedMatch
            if (selectedMatch != null) {
                MatchDetailBottomSheet(
                        match = selectedMatch,
                        matchDetail = uiState.matchDetail,
                        isLoadingDetail = uiState.isLoadingDetail,
                        standings = uiState.resolvedDetailStandings,
                        cutoffs = when {
                            selectedMatch.league.tableCutoffs.isNotEmpty() -> selectedMatch.league.tableCutoffs
                            selectedMatch.league.groupCutoffs.isNotEmpty() -> selectedMatch.league.groupCutoffs
                            uiState.groups.isNotEmpty() -> uiState.groups.firstOrNull()?.cutoffs.orEmpty()
                            else -> emptyList()
                        },
                        legend = when {
                            selectedMatch.league.tableLegend.isNotEmpty() -> selectedMatch.league.tableLegend
                            selectedMatch.league.groupLegend.isNotEmpty() -> selectedMatch.league.groupLegend
                            uiState.groups.isNotEmpty() -> uiState.groups.firstOrNull()?.legend.orEmpty()
                            else -> emptyList()
                        },
                        isFollowing = uiState.followedMatchIds.contains(selectedMatch.id),
                        onFollowToggle = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.toggleFollow(context, selectedMatch, uiState.matchDetail)
                        },
                        onDismissRequest = viewModel::dismissMatchDetail
                    )
                }

                if (isWhatsNewSheetOpen) {
                    WhatsNewBottomSheet(
                        onDismissRequest = { isWhatsNewSheetOpen = false }
                    )
                }

                updateAvailableData?.let { data ->
                    UpdateAvailableBottomSheet(
                        version = data.version,
                        releaseUrl = data.releaseUrl,
                        releaseNotes = data.releaseNotes,
                        onDismissRequest = { updateAvailableData = null }
                    )
                }

                if (isOnboardingSheetOpen) {
                    OnboardingBottomSheet(
                        appSettings = appSettings,
                        selectedLeague = uiState.selectedLeague,
                        onThemeModeChanged = settingsRepository::setThemeMode,
                        onColorPaletteChanged = settingsRepository::setColorPalette,
                        onLeagueSelected = { league ->
                            leagueTransitionDirection = 0
                            viewModel.selectLeague(league)
                        },
                        onNotificationLeadMinutesChanged = { minutes ->
                            settingsRepository.setNotificationLeadMinutes(minutes)
                            MatchAlarmScheduler.rescheduleAllFollowed(context, minutes)
                        },
                        onCompleteOnboarding = {
                            settingsRepository.setOnboardingCompleted(true)
                            isOnboardingSheetOpen = false
                        },
                        onDismissRequest = {
                            settingsRepository.setOnboardingCompleted(true)
                            isOnboardingSheetOpen = false
                        }
                    )
                }
                }
            }
        }
    }
}

/**
 * Contenido desplazable verticalmente para una pestana temporal dentro del HorizontalPager principal.
 * Aisla su propio LazyListState por pestana y precalcula los encabezados diarios para scroll fluido.
 */
@Composable
private fun MatchesLeaguePageContent(
    league: League,
    pageFilter: TemporalFilter,
    matches: List<Match>,
    uiState: MatchesUiState,
    bottomContentPadding: Dp,
    onMatchClick: (Match) -> Unit,
    onManageFavorites: () -> Unit,
    onManageMyLeagues: () -> Unit
) {
    val isDebug = league == League.DEBUG
    val bracket = uiState.radialBracket
    val bracketTitle = stringResource(league.bracketTitleRes)
    val isGroupedFilter = pageFilter == TemporalFilter.PAST || pageFilter == TemporalFilter.UPCOMING

    // Precalculamos la agrupacion diaria y etiquetas para evitar formatear fechas durante el scroll
    val groupedMatchesWithHeaders = remember(matches, isGroupedFilter) {
        if (isGroupedFilter) {
            matches
                .groupBy { match -> DateTimeUtils.getLocalDateKey(match.dateUtc) }
                .map { (dateKey, dayMatches) ->
                    val dateLabel = DateTimeUtils.formatDateHeader(
                        dayMatches.firstOrNull()?.dateUtc ?: dateKey
                    )
                    Triple(dateKey, dateLabel, dayMatches)
                }
        } else {
            emptyList()
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.listSpacing),
        contentPadding = PaddingValues(
            top = UlamaTheme.spacing.listTopPadding,
            bottom = UlamaTheme.spacing.listBottomPadding + bottomContentPadding
        )
    ) {
        if (matches.isEmpty()) {
            item(key = "empty_matches_card", contentType = "empty_matches_card") {
                when {
                    league == League.FAVORITES && uiState.favoriteTeamIds.isEmpty() -> {
                        EmptyFavoritesCard(onManageFavorites = onManageFavorites)
                    }
                    league == League.MY_LEAGUES && uiState.myLeagueCodes.isEmpty() -> {
                        EmptyMyLeaguesCard(onManageMyLeagues = onManageMyLeagues)
                    }
                    else -> {
                        EmptyMatchesCard(filter = pageFilter)
                    }
                }
            }
        } else if (isGroupedFilter) {
            groupedMatchesWithHeaders.forEach { (dateKey, dateLabel, dayMatches) ->
                item(key = "date_header_$dateKey", contentType = "date_header") {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = UlamaTheme.spacing.medium,
                                top = UlamaTheme.spacing.small,
                                bottom = UlamaTheme.spacing.extraSmall
                            )
                    )
                }
                item(key = "matches_group_$dateKey", contentType = "matches_group_card") {
                    MatchesGroupCard(
                        matches = dayMatches,
                        onMatchClick = onMatchClick,
                        showLeagueBadge = league.isVirtual
                    )
                }
            }
        } else {
            item(key = "matches_group_card", contentType = "matches_group_card") {
                MatchesGroupCard(
                    matches = matches,
                    onMatchClick = onMatchClick,
                    showLeagueBadge = league.isVirtual
                )
            }
        }

        // Si la competicion real tiene cruces programados en ESPN, el cuadro de eliminatorias
        // se posiciona prioritariamente arriba de la tabla general y los grupos.
        if (!isDebug && bracket != null && bracket.hasAnyScheduledMatch) {
            item(key = "radial_bracket_active", contentType = "radial_bracket") {
                RadialKnockoutBracket(
                    tree = bracket,
                    title = bracketTitle
                )
            }
        }

        if (uiState.standings.isNotEmpty()) {
            item(key = "standings_table", contentType = "standings_table") {
                StandingsTable(
                    standings = uiState.standings,
                    cutoffs = league.tableCutoffs,
                    legend = league.tableLegend
                )
            }
        }

        if (uiState.groups.isNotEmpty()) {
            items(
                items = uiState.groups,
                key = { group -> "group_${group.name}" },
                contentType = { "group_standings" }
            ) { group ->
                val groupTitle = if (group.name.length <= 4) {
                    stringResource(R.string.group_table_title_format, group.name)
                } else {
                    group.name
                }
                StandingsTable(
                    standings = group.standings,
                    title = groupTitle,
                    cutoffs = group.cutoffs,
                    legend = group.legend
                )
            }
        }

        // Si la competicion real no tiene cruces programados aun, el cuadro vacio
        // se despliega al pie de la tabla y los grupos en espera de clasificados oficiales.
        if (!isDebug && bracket != null && !bracket.hasAnyScheduledMatch) {
            item(key = "radial_bracket_empty_wait", contentType = "radial_bracket") {
                RadialKnockoutBracket(
                    tree = bracket,
                    title = bracketTitle
                )
            }
        }

        // En modo DEBUG se exponen los cuadros de desarrollo secuenciales
        if (isDebug) {
            uiState.radialBracket32?.let { bracket32 ->
                item(key = "radial_bracket_32", contentType = "radial_bracket") {
                    RadialKnockoutBracket(
                        tree = bracket32,
                        title = stringResource(R.string.title_radial_bracket_32)
                    )
                }
            }
            bracket?.let { b ->
                item(key = "radial_bracket_completed", contentType = "radial_bracket") {
                    RadialKnockoutBracket(
                        tree = b,
                        title = stringResource(R.string.title_radial_bracket_completed)
                    )
                }
            }
            uiState.radialBracketSemis?.let { bracketSemis ->
                item(key = "radial_bracket_semis", contentType = "radial_bracket") {
                    RadialKnockoutBracket(
                        tree = bracketSemis,
                        title = stringResource(R.string.title_radial_bracket_semis)
                    )
                }
            }
            uiState.radialBracketEmpty?.let { bracketEmpty ->
                item(key = "radial_bracket_empty", contentType = "radial_bracket") {
                    RadialKnockoutBracket(
                        tree = bracketEmpty,
                        title = stringResource(R.string.title_radial_bracket_empty)
                    )
                }
            }
        }
    }
}
