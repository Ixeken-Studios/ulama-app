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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixeken.ulama.core.model.ColorPalette
import com.ixeken.ulama.core.util.LocaleHelper
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.MotionStyle
import com.ixeken.ulama.core.model.TemporalFilter
import com.ixeken.ulama.core.model.ThemeMode
import com.ixeken.ulama.core.util.DateTimeUtils
import com.ixeken.ulama.core.util.ImagePreloadHelper
import com.ixeken.ulama.data.repository.AppSettingsRepositoryImpl
import com.ixeken.ulama.service.FavoriteAutoFollowJobService
import com.ixeken.ulama.service.MatchAlarmScheduler
import com.ixeken.ulama.service.MatchNotificationHelper
import com.ixeken.ulama.ui.components.EmptyFavoritesCard
import com.ixeken.ulama.ui.components.EmptyMatchesCard
import com.ixeken.ulama.ui.components.EmptyMyLeaguesCard
import com.ixeken.ulama.ui.components.FavoriteTeamsBottomSheet
import com.ixeken.ulama.ui.components.MatchesGroupCard
import com.ixeken.ulama.ui.components.MyLeaguesBottomSheet
import com.ixeken.ulama.ui.components.PullToRefreshWithLoadingIndicator
import com.ixeken.ulama.ui.components.RadialKnockoutBracket
import com.ixeken.ulama.ui.components.StandingsTable
import com.ixeken.ulama.ui.components.TemporalFilterTabs
import com.ixeken.ulama.ui.components.UlamaTopAppBar
import com.ixeken.ulama.ui.components.detail.MatchDetailBottomSheet
import com.ixeken.ulama.ui.matches.MatchesViewModel
import com.ixeken.ulama.ui.settings.SettingsScreen
import com.ixeken.ulama.ui.theme.UlamaTheme
import com.ixeken.ulama.data.update.GithubUpdateChecker
import com.ixeken.ulama.data.update.UpdateCheckResult
import com.ixeken.ulama.ui.components.OnboardingBottomSheet
import com.ixeken.ulama.ui.components.UpdateAvailableBottomSheet
import com.ixeken.ulama.ui.components.WhatsNewBottomSheet

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

            var isFavoritesSheetOpen by remember { mutableStateOf(false) }
            var isMyLeaguesSheetOpen by remember { mutableStateOf(false) }
            var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
            var isWhatsNewSheetOpen by remember { mutableStateOf(false) }
            var isOnboardingSheetOpen by remember { mutableStateOf(false) }
            var updateAvailableData by remember { mutableStateOf<UpdateCheckResult.NewVersionAvailable?>(null) }

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
                isExpressiveMotion = isExpressive
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
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.1.0"
                    } catch (_: Exception) {
                        "2.1.0"
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
                        isFavoritesSheetOpen = false
                        isMyLeaguesSheetOpen = false
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

                // Precargamos los escudos del selector de favoritos solo cuando el modal se abre
                // y exclusivamente para la variante de tema activa (claro u oscuro).
                LaunchedEffect(isFavoritesSheetOpen, uiState.availableTeamsByLeague, darkTheme) {
                    if (isFavoritesSheetOpen) {
                        viewModel.ensureAvailableTeamsLoaded()
                        if (uiState.availableTeamsByLeague.isNotEmpty()) {
                            val urls = uiState.availableTeamsByLeague.values.flatten().mapNotNull { entry ->
                                entry.getLogoUrl(darkTheme)
                            }
                            ImagePreloadHelper.preloadImages(context, urls)
                        }
                    }
                }

                LaunchedEffect(uiState.matches, darkTheme) {
                    if (uiState.matches.isNotEmpty()) {
                        val urls = uiState.matches.flatMap { match ->
                            listOfNotNull(
                                match.homeTeam.getLogoUrl(darkTheme),
                                match.awayTeam.getLogoUrl(darkTheme)
                            )
                        }
                        ImagePreloadHelper.preloadImages(context, urls)
                    }
                }

                LaunchedEffect(uiState.standings, darkTheme) {
                    if (uiState.standings.isNotEmpty()) {
                        val urls = uiState.standings.mapNotNull { entry ->
                            entry.getLogoUrl(darkTheme)
                        }
                        ImagePreloadHelper.preloadImages(context, urls)
                    }
                }

                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val horizontalMargin = if (screenWidth >= UlamaTheme.dimensions.maxContentWidth) {
                    UlamaTheme.spacing.screenHorizontalExpanded
                } else {
                    UlamaTheme.spacing.screenHorizontalCompact
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
                        onCheckUpdatesOnStartChanged = settingsRepository::setCheckUpdatesOnStart,
                        onShowWalkthrough = { isOnboardingSheetOpen = true },
                        onBackClick = { isSettingsOpen = false }
                    )
                } else {
                    Scaffold(
                        topBar = {
                            UlamaTopAppBar(
                                selectedLeague = uiState.selectedLeague,
                                favoriteCount = uiState.favoriteTeamIds.size,
                                myLeaguesCount = uiState.myLeagueCodes.size,
                                onLeagueSelected = viewModel::selectLeague,
                                onSettingsClick = { isSettingsOpen = true },
                                onManageFavorites = {
                                    viewModel.ensureAvailableTeamsLoaded()
                                    isFavoritesSheetOpen = true
                                },
                                onManageMyLeagues = { isMyLeaguesSheetOpen = true }
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
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = viewModel::selectFilter,
                            modifier = Modifier.widthIn(max = UlamaTheme.dimensions.maxContentWidth)
                        )

                        Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))

                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ContainedLoadingIndicator(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            else -> {
                                val isDebug = uiState.selectedLeague == League.DEBUG
                                val bracket = uiState.radialBracket
                                val bracketTitle = if (uiState.selectedLeague == League.LIGA_MX) {
                                    stringResource(R.string.legend_liguilla)
                                } else {
                                    stringResource(R.string.title_radial_bracket)
                                }

                                val isGroupedFilter = uiState.selectedFilter == TemporalFilter.PAST || uiState.selectedFilter == TemporalFilter.UPCOMING
                                // Precalculamos tanto la agrupacion por dia como la etiqueta localizada del encabezado
                                // para evitar formatear fechas dentro del bloque DSL de LazyColumn en cada scroll.
                                val groupedMatchesWithHeaders = remember(uiState.matches, isGroupedFilter) {
                                    if (isGroupedFilter) {
                                        uiState.matches
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

                                PullToRefreshWithLoadingIndicator(
                                    isRefreshing = uiState.isRefreshing,
                                    onRefresh = viewModel::refresh,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.listSpacing),
                                        contentPadding = PaddingValues(
                                            top = UlamaTheme.spacing.listTopPadding,
                                            bottom = UlamaTheme.spacing.listBottomPadding + innerPadding.calculateBottomPadding()
                                        )
                                    ) {
                                        if (uiState.matches.isEmpty()) {
                                            item(key = "empty_matches_card", contentType = "empty_matches_card") {
                                                when {
                                                    uiState.selectedLeague == League.FAVORITES && uiState.favoriteTeamIds.isEmpty() -> {
                                                        EmptyFavoritesCard(
                                                            onManageFavorites = {
                                                                viewModel.ensureAvailableTeamsLoaded()
                                                                isFavoritesSheetOpen = true
                                                            }
                                                        )
                                                    }
                                                    uiState.selectedLeague == League.MY_LEAGUES && uiState.myLeagueCodes.isEmpty() -> {
                                                        EmptyMyLeaguesCard(
                                                            onManageMyLeagues = { isMyLeaguesSheetOpen = true }
                                                        )
                                                    }
                                                    else -> {
                                                        EmptyMatchesCard(filter = uiState.selectedFilter)
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
                                                        onMatchClick = viewModel::selectMatch,
                                                        showLeagueBadge = uiState.selectedLeague.isVirtual
                                                    )
                                                }
                                            }
                                        } else {
                                            item(key = "matches_group_card", contentType = "matches_group_card") {
                                                MatchesGroupCard(
                                                    matches = uiState.matches,
                                                    onMatchClick = viewModel::selectMatch,
                                                    showLeagueBadge = uiState.selectedLeague.isVirtual
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
                                                    cutoffs = uiState.selectedLeague.tableCutoffs,
                                                    legend = uiState.selectedLeague.tableLegend
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
                            uiState.groups.isNotEmpty() -> uiState.groups.firstOrNull()?.cutoffs.orEmpty()
                            else -> emptyList()
                        },
                        legend = when {
                            selectedMatch.league.tableLegend.isNotEmpty() -> selectedMatch.league.tableLegend
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

                if (isFavoritesSheetOpen) {
                    FavoriteTeamsBottomSheet(
                        favoriteTeamIds = uiState.favoriteTeamIds,
                        availableTeamsByLeague = uiState.availableTeamsByLeague,
                        onToggleFavorite = viewModel::toggleFavoriteTeam,
                        onDismissRequest = { isFavoritesSheetOpen = false }
                    )
                }

                if (isMyLeaguesSheetOpen) {
                    MyLeaguesBottomSheet(
                        selectedLeagueCodes = uiState.myLeagueCodes,
                        onToggleLeague = viewModel::toggleMyLeague,
                        onDismissRequest = { isMyLeaguesSheetOpen = false }
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
                        onLeagueSelected = viewModel::selectLeague,
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
