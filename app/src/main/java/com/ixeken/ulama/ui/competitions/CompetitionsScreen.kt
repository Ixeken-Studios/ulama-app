package com.ixeken.ulama.ui.competitions

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.LeagueRegion
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.ui.components.bounceClick
import com.ixeken.ulama.ui.theme.UlamaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
private data class IndexedTeamEntry(
    val league: League,
    val entry: StandingEntry
) {
    val stableId: String get() = "${league.code}_${entry.teamId.ifBlank { entry.teamName }}"
    val teamKey: String get() = entry.teamId.ifBlank { entry.teamName }
}

@Immutable
private data class SubRegionGroup(
    @param:StringRes val titleRes: Int,
    val leagues: List<League>
)

/**
 * Pantalla dedicada para explorar competiciones por mapa geografico, gestionar Mis ligas
 * y seguir clubes favoritos en cuadricula adaptativa con componentes Material 3 Expressive.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompetitionsScreen(
    selectedLeague: League,
    favoriteTeamIds: Set<String>,
    myLeagueCodes: Set<String>,
    availableTeamsByLeague: Map<League, List<StandingEntry>>,
    loadingTeamLeagues: Set<League>,
    initialTeamsLeague: League? = null,
    onSelectLeague: (League) -> Unit,
    onToggleMyLeague: (String) -> Unit,
    onToggleFavoriteTeam: (String) -> Unit,
    onEnsureLeagueTeamsLoaded: (League) -> Unit,
    onEnsureAllTeamsLoaded: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = UlamaTheme.isDark
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val isCompact = screenWidth < UlamaTheme.dimensions.maxContentWidth
    val horizontalMargin = if (isCompact) {
        UlamaTheme.spacing.screenHorizontalCompact
    } else {
        UlamaTheme.spacing.screenHorizontalExpanded
    }

    var activeTeamsLeagueCode by rememberSaveable(initialTeamsLeague) {
        mutableStateOf(initialTeamsLeague?.code)
    }
    val activeTeamsLeague = remember(activeTeamsLeagueCode) {
        activeTeamsLeagueCode?.let { League.fromCode(it) }
    }
    var selectedRegion by rememberSaveable { mutableStateOf(LeagueRegion.ALL) }

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val isSearchExpanded = searchBarState.currentValue == SearchBarValue.Expanded
    val searchQuery = textFieldState.text.toString().trim()
    var allowSearchFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(120L)
        allowSearchFocus = true
    }

    LaunchedEffect(searchBarState.targetValue) {
        if (searchBarState.targetValue == SearchBarValue.Collapsed) {
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(activeTeamsLeague) {
        val target = activeTeamsLeague ?: return@LaunchedEffect
        if (target == League.FAVORITES) {
            onEnsureAllTeamsLoaded()
        } else {
            onEnsureLeagueTeamsLoaded(target)
        }
    }

    LaunchedEffect(isSearchExpanded, activeTeamsLeague) {
        if (isSearchExpanded && activeTeamsLeague == null) {
            onEnsureAllTeamsLoaded()
        }
    }

    val collapseSearch: () -> Unit = {
        focusManager.clearFocus(force = true)
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    val navigateBack: () -> Unit = {
        when {
            isSearchExpanded -> collapseSearch()
            activeTeamsLeague != null && initialTeamsLeague == null -> {
                textFieldState.clearText()
                activeTeamsLeagueCode = null
            }
            else -> onClose()
        }
    }

    BackHandler(enabled = !isSearchExpanded, onBack = navigateBack)

    val placeholderText = when (activeTeamsLeague) {
        null -> stringResource(R.string.competitions_search_placeholder)
        League.FAVORITES -> stringResource(R.string.competitions_teams_all_search_placeholder)
        else -> stringResource(
            R.string.competitions_teams_search_placeholder,
            stringResource(activeTeamsLeague.nameRes)
        )
    }

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            modifier = Modifier.focusProperties {
                canFocus = allowSearchFocus || isSearchExpanded
            },
            onSearch = {
                collapseSearch()
            },
            placeholder = {
                Text(
                    text = placeholderText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                if (isSearchExpanded) {
                    IconButton(onClick = collapseSearch) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = { textFieldState.clearText() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            }
        )
    }

    val searchResultsContent: @Composable () -> Unit = {
        SearchOverlayResults(
            query = searchQuery,
            activeTeamsLeague = activeTeamsLeague,
            selectedLeague = selectedLeague,
            myLeagueCodes = myLeagueCodes,
            favoriteTeamIds = favoriteTeamIds,
            availableTeamsByLeague = availableTeamsByLeague,
            isDark = isDark,
            onSelectLeague = { league ->
                collapseSearch()
                onSelectLeague(league)
                onClose()
            },
            onOpenTeams = { league ->
                collapseSearch()
                textFieldState.clearText()
                activeTeamsLeagueCode = league.code
            },
            onToggleMyLeague = onToggleMyLeague,
            onToggleFavoriteTeam = onToggleFavoriteTeam
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AppBarWithSearch(
                state = searchBarState,
                inputField = inputField,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                actions = if (activeTeamsLeague != null && initialTeamsLeague == null) {
                    {
                        FilledTonalIconButton(onClick = onClose) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.cd_confirm_selection)
                            )
                        }
                    }
                } else null,
                colors = SearchBarDefaults.appBarWithSearchColors(
                    scrolledAppBarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    appBarContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (isCompact) {
            ExpandedFullScreenSearchBar(
                state = searchBarState,
                inputField = inputField
            ) {
                searchResultsContent()
            }
        } else {
            ExpandedDockedSearchBar(
                state = searchBarState,
                inputField = inputField
            ) {
                searchResultsContent()
            }
        }

        AnimatedContent(
            targetState = activeTeamsLeague,
            label = "competitionsDestination",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { targetTeamsLeague ->
            if (targetTeamsLeague == null) {
                CompetitionsMainContent(
                    selectedLeague = selectedLeague,
                    selectedRegion = selectedRegion,
                    favoriteTeamIds = favoriteTeamIds,
                    myLeagueCodes = myLeagueCodes,
                    isCompact = isCompact,
                    horizontalMargin = horizontalMargin,
                    isDark = isDark,
                    onRegionToggle = { tapped ->
                        selectedRegion = if (selectedRegion == tapped) LeagueRegion.ALL else tapped
                    },
                    onResetRegion = { selectedRegion = LeagueRegion.ALL },
                    onSelectLeague = { league ->
                        onSelectLeague(league)
                        onClose()
                    },
                    onOpenTeams = { league ->
                        textFieldState.clearText()
                        activeTeamsLeagueCode = league.code
                    },
                    onToggleMyLeague = onToggleMyLeague
                )
            } else {
                val filteredTeams = remember(targetTeamsLeague, availableTeamsByLeague, favoriteTeamIds, searchQuery) {
                    buildTeamsForLeague(
                        league = targetTeamsLeague,
                        availableTeamsByLeague = availableTeamsByLeague,
                        favoriteTeamIds = favoriteTeamIds,
                        query = searchQuery
                    )
                }
                TeamsGridContent(
                    league = targetTeamsLeague,
                    teams = filteredTeams,
                    favoriteTeamIds = favoriteTeamIds,
                    isInMyLeagues = targetTeamsLeague.code in myLeagueCodes,
                    isLoading = targetTeamsLeague in loadingTeamLeagues ||
                        (targetTeamsLeague == League.FAVORITES && availableTeamsByLeague.isEmpty()),
                    searchQuery = searchQuery,
                    horizontalMargin = horizontalMargin,
                    isDark = isDark,
                    onToggleMyLeague = { onToggleMyLeague(targetTeamsLeague.code) },
                    onToggleFavoriteTeam = onToggleFavoriteTeam
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompetitionsMainContent(
    selectedLeague: League,
    selectedRegion: LeagueRegion,
    favoriteTeamIds: Set<String>,
    myLeagueCodes: Set<String>,
    isCompact: Boolean,
    horizontalMargin: androidx.compose.ui.unit.Dp,
    isDark: Boolean,
    onRegionToggle: (LeagueRegion) -> Unit,
    onResetRegion: () -> Unit,
    onSelectLeague: (League) -> Unit,
    onOpenTeams: (League) -> Unit,
    onToggleMyLeague: (String) -> Unit
) {
    val context = LocalContext.current
    val followedLeagues = remember(myLeagueCodes) {
        League.standardLeagues.filter { it.code in myLeagueCodes }
    }
    val remainingLeagues = remember(myLeagueCodes) {
        League.standardLeagues.filter { it.code !in myLeagueCodes }
    }
    val regionSubGroups = remember(selectedRegion, context) {
        if (selectedRegion == LeagueRegion.ALL) {
            emptyList()
        } else {
            val matching = League.standardLeagues.filter { it.region == selectedRegion }
            matching.groupBy { it.subRegionRes }
                .entries
                .sortedWith(
                    compareBy<Map.Entry<Int, List<League>>> { it.key != R.string.subregion_european_competitions }
                        .thenByDescending { it.value.size }
                        .thenBy { context.getString(it.key) }
                )
                .map { SubRegionGroup(titleRes = it.key, leagues = it.value) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = UlamaTheme.spacing.small,
            bottom = UlamaTheme.spacing.listBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.sectionSpacing)
    ) {
        item(key = "world_map", contentType = "world_map") {
            WorldContinentMap(
                selectedRegion = selectedRegion,
                onRegionToggle = onRegionToggle
            )
        }

        if (selectedRegion == LeagueRegion.ALL) {
            item(key = "my_teams_row", contentType = "my_teams_row") {
                MyTeamsSegmentedCard(
                    isSelected = selectedLeague == League.FAVORITES,
                    favoriteCount = favoriteTeamIds.size,
                    onSelect = { onSelectLeague(League.FAVORITES) },
                    onOpenTeams = { onOpenTeams(League.FAVORITES) }
                )
            }

            if (followedLeagues.isNotEmpty()) {
                item(key = "my_leagues_section", contentType = "segmented_group") {
                    Column(verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.league_my_leagues),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val isMyLeaguesActive = selectedLeague == League.MY_LEAGUES
                            if (isMyLeaguesActive) {
                                Button(
                                    onClick = { onSelectLeague(League.MY_LEAGUES) },
                                    contentPadding = PaddingValues(
                                        horizontal = UlamaTheme.spacing.medium,
                                        vertical = UlamaTheme.spacing.extraSmall
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.competitions_view_matches),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { onSelectLeague(League.MY_LEAGUES) },
                                    contentPadding = PaddingValues(
                                        horizontal = UlamaTheme.spacing.medium,
                                        vertical = UlamaTheme.spacing.extraSmall
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.competitions_view_matches),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        SegmentedLeagueGroup(
                            leagues = followedLeagues,
                            selectedLeague = selectedLeague,
                            myLeagueCodes = myLeagueCodes,
                            isDark = isDark,
                            onSelectLeague = onSelectLeague,
                            onOpenTeams = onOpenTeams,
                            onToggleMyLeague = onToggleMyLeague
                        )
                    }
                }
            }

            if (remainingLeagues.isNotEmpty()) {
                item(key = "all_leagues_section", contentType = "segmented_group") {
                    Column(verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small)) {
                        Text(
                            text = stringResource(R.string.competitions_all_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isCompact || remainingLeagues.size <= 3) {
                            SegmentedLeagueGroup(
                                leagues = remainingLeagues,
                                selectedLeague = selectedLeague,
                                myLeagueCodes = myLeagueCodes,
                                isDark = isDark,
                                onSelectLeague = onSelectLeague,
                                onOpenTeams = onOpenTeams,
                                onToggleMyLeague = onToggleMyLeague
                            )
                        } else {
                            val splitIndex = (remainingLeagues.size + 1) / 2
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.medium),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SegmentedLeagueGroup(
                                    leagues = remainingLeagues.subList(0, splitIndex),
                                    selectedLeague = selectedLeague,
                                    myLeagueCodes = myLeagueCodes,
                                    isDark = isDark,
                                    onSelectLeague = onSelectLeague,
                                    onOpenTeams = onOpenTeams,
                                    onToggleMyLeague = onToggleMyLeague,
                                    modifier = Modifier.weight(1f)
                                )
                                SegmentedLeagueGroup(
                                    leagues = remainingLeagues.subList(splitIndex, remainingLeagues.size),
                                    selectedLeague = selectedLeague,
                                    myLeagueCodes = myLeagueCodes,
                                    isDark = isDark,
                                    onSelectLeague = onSelectLeague,
                                    onOpenTeams = onOpenTeams,
                                    onToggleMyLeague = onToggleMyLeague,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        } else if (regionSubGroups.isEmpty()) {
            item(key = "empty_region", contentType = "empty_region") {
                EmptyRegionCard(
                    regionName = stringResource(selectedRegion.labelRes),
                    onReset = onResetRegion
                )
            }
        } else if (isCompact || regionSubGroups.size == 1) {
            items(
                items = regionSubGroups,
                key = { "subregion_${it.titleRes}" },
                contentType = { "subregion_group" }
            ) { group ->
                SubRegionSection(
                    group = group,
                    selectedLeague = selectedLeague,
                    myLeagueCodes = myLeagueCodes,
                    isDark = isDark,
                    onSelectLeague = onSelectLeague,
                    onOpenTeams = onOpenTeams,
                    onToggleMyLeague = onToggleMyLeague
                )
            }
        } else {
            item(key = "subregions_two_column", contentType = "subregions_grid") {
                val (leftGroups, rightGroups) = remember(regionSubGroups) {
                    balanceGroupsIntoTwoColumns(regionSubGroups)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.medium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.sectionSpacing),
                        modifier = Modifier.weight(1f)
                    ) {
                        leftGroups.forEach { group ->
                            SubRegionSection(
                                group = group,
                                selectedLeague = selectedLeague,
                                myLeagueCodes = myLeagueCodes,
                                isDark = isDark,
                                onSelectLeague = onSelectLeague,
                                onOpenTeams = onOpenTeams,
                                onToggleMyLeague = onToggleMyLeague
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.sectionSpacing),
                        modifier = Modifier.weight(1f)
                    ) {
                        rightGroups.forEach { group ->
                            SubRegionSection(
                                group = group,
                                selectedLeague = selectedLeague,
                                myLeagueCodes = myLeagueCodes,
                                isDark = isDark,
                                onSelectLeague = onSelectLeague,
                                onOpenTeams = onOpenTeams,
                                onToggleMyLeague = onToggleMyLeague
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubRegionSection(
    group: SubRegionGroup,
    selectedLeague: League,
    myLeagueCodes: Set<String>,
    isDark: Boolean,
    onSelectLeague: (League) -> Unit,
    onOpenTeams: (League) -> Unit,
    onToggleMyLeague: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small)) {
        Text(
            text = stringResource(group.titleRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SegmentedLeagueGroup(
            leagues = group.leagues,
            selectedLeague = selectedLeague,
            myLeagueCodes = myLeagueCodes,
            isDark = isDark,
            onSelectLeague = onSelectLeague,
            onOpenTeams = onOpenTeams,
            onToggleMyLeague = onToggleMyLeague
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MyTeamsSegmentedCard(
    isSelected: Boolean,
    favoriteCount: Int,
    onSelect: () -> Unit,
    onOpenTeams: () -> Unit
) {
    SegmentedListItem(
        selected = isSelected,
        onClick = onSelect,
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = segmentedItemColors(),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = stringResource(R.string.cd_fav_teams_star),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UlamaTheme.dimensions.iconNormal)
            )
        },
        supportingContent = if (favoriteCount > 0) {
            {
                Text(
                    text = stringResource(R.string.fav_teams_global_count_format, favoriteCount),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else null,
        trailingContent = {
            IconButton(onClick = onOpenTeams) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.cd_open_teams, stringResource(R.string.league_favorites))
                )
            }
        },
        content = {
            Text(
                text = stringResource(R.string.league_favorites),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SegmentedLeagueGroup(
    leagues: List<League>,
    selectedLeague: League,
    myLeagueCodes: Set<String>,
    isDark: Boolean,
    onSelectLeague: (League) -> Unit,
    onOpenTeams: (League) -> Unit,
    onToggleMyLeague: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = leagues.size
    val itemColors = segmentedItemColors()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        leagues.forEachIndexed { index, league ->
            val isActive = selectedLeague == league
            val isInMyLeagues = league.code in myLeagueCodes
            val leagueName = stringResource(league.nameRes)

            SegmentedListItem(
                selected = isActive,
                onClick = { onSelectLeague(league) },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                colors = itemColors,
                leadingContent = {
                    AsyncImage(
                        model = league.getLogoUrl(isDark),
                        contentDescription = stringResource(R.string.cd_league_logo, leagueName),
                        modifier = Modifier.size(UlamaTheme.dimensions.leagueLogoList)
                    )
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.extraSmall)
                    ) {
                        FilledIconToggleButton(
                            checked = isInMyLeagues,
                            onCheckedChange = { onToggleMyLeague(league.code) },
                            shapes = IconButtonDefaults.toggleableShapes()
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isInMyLeagues) R.drawable.ic_star else R.drawable.ic_star_outline
                                ),
                                contentDescription = stringResource(R.string.cd_toggle_my_league, leagueName),
                                modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
                            )
                        }
                        IconButton(onClick = { onOpenTeams(league) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = stringResource(R.string.cd_open_teams, leagueName)
                            )
                        }
                    }
                },
                content = {
                    Text(
                        text = leagueName,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyRegionCard(
    regionName: String,
    onReset: () -> Unit
) {
    Surface(
        shape = UlamaTheme.shapes.container,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = UlamaTheme.spacing.emptyCardPaddingHorizontal,
                    vertical = UlamaTheme.spacing.emptyCardPaddingVertical
                )
        ) {
            Text(
                text = stringResource(R.string.competitions_empty_region_title, regionName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.competitions_empty_region_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
            FilledTonalButton(onClick = onReset) {
                Text(text = stringResource(R.string.competitions_reset_filter))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TeamsGridContent(
    league: League,
    teams: List<IndexedTeamEntry>,
    favoriteTeamIds: Set<String>,
    isInMyLeagues: Boolean,
    isLoading: Boolean,
    searchQuery: String,
    horizontalMargin: androidx.compose.ui.unit.Dp,
    isDark: Boolean,
    onToggleMyLeague: () -> Unit,
    onToggleFavoriteTeam: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = UlamaTheme.dimensions.teamGridCellMin),
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = UlamaTheme.spacing.small,
            bottom = UlamaTheme.spacing.listBottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.gridSpacing),
        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.gridSpacing),
        modifier = Modifier.fillMaxSize()
    ) {
        item(
            key = "teams_header_${league.code}",
            span = { GridItemSpan(maxLineSpan) },
            contentType = "teams_header"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UlamaTheme.spacing.small)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.medium),
                    modifier = Modifier.weight(1f)
                ) {
                    if (league == League.FAVORITES) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star),
                            contentDescription = stringResource(R.string.cd_fav_teams_star),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(UlamaTheme.dimensions.leagueLogoList)
                        )
                    } else {
                        AsyncImage(
                            model = league.getLogoUrl(isDark),
                            contentDescription = stringResource(
                                R.string.cd_league_logo,
                                stringResource(league.nameRes)
                            ),
                            modifier = Modifier.size(UlamaTheme.dimensions.leagueLogoList)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(league.nameRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val countSubtitle = when {
                            league == League.FAVORITES && favoriteTeamIds.isNotEmpty() ->
                                stringResource(R.string.fav_teams_global_count_format, favoriteTeamIds.size)
                            teams.isNotEmpty() ->
                                stringResource(R.string.competitions_teams_count_subtitle, teams.size)
                            else -> null
                        }
                        if (countSubtitle != null) {
                            Text(
                                text = countSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (!league.isVirtual) {
                    ToggleButton(
                        checked = isInMyLeagues,
                        onCheckedChange = { onToggleMyLeague() }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isInMyLeagues) R.drawable.ic_star else R.drawable.ic_star_outline
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(UlamaTheme.dimensions.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                        Text(
                            text = stringResource(R.string.competitions_add_to_my_leagues),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (isLoading && teams.isEmpty()) {
            item(
                key = "teams_loading",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "loading"
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = UlamaTheme.spacing.huge)
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (teams.isEmpty()) {
            item(
                key = "teams_empty",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "empty"
            ) {
                Surface(
                    shape = UlamaTheme.shapes.container,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            searchQuery.isNotBlank() -> stringResource(R.string.competitions_empty_search, searchQuery)
                            league == League.FAVORITES -> stringResource(R.string.competitions_empty_favorites)
                            else -> stringResource(R.string.competitions_empty_teams)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(UlamaTheme.spacing.extraLarge)
                    )
                }
            }
        } else {
            items(
                items = teams,
                key = { it.stableId },
                contentType = { "team_grid_card" }
            ) { item ->
                val teamId = item.teamKey
                TeamGridCard(
                    entry = item.entry,
                    isFavorite = teamId in favoriteTeamIds,
                    isDark = isDark,
                    onToggleFavorite = { onToggleFavoriteTeam(teamId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TeamGridCard(
    entry: StandingEntry,
    isFavorite: Boolean,
    isDark: Boolean,
    onToggleFavorite: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val motion = UlamaTheme.motion
    val shapes = UlamaTheme.shapes

    val radiusSpec = remember(motion) { motion.spatialSpec<androidx.compose.ui.unit.Dp>() }
    val colorSpec = remember(motion) { motion.effectsSpec<androidx.compose.ui.graphics.Color>() }

    val cornerRadius by animateDpAsState(
        targetValue = if (isFavorite) shapes.teamCardSelectedRadius else shapes.teamCardRestRadius,
        animationSpec = radiusSpec,
        label = "teamCardRadius"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFavorite) colorScheme.primaryContainer else colorScheme.surfaceContainer,
        animationSpec = colorSpec,
        label = "teamCardColor"
    )
    val cardShape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val displayName = entry.teamShortName.ifBlank { entry.teamName }

    Surface(
        shape = cardShape,
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(enabled = true, interactionSource = interactionSource)
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onToggleFavorite
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(UlamaTheme.spacing.medium)
        ) {
            val crestUrl = entry.getLogoUrl(isDark)
            if (!crestUrl.isNullOrBlank()) {
                AsyncImage(
                    model = crestUrl,
                    contentDescription = stringResource(R.string.cd_team_crest, displayName),
                    modifier = Modifier.size(UlamaTheme.dimensions.teamGridCrest)
                )
            } else {
                Surface(
                    shape = shapes.chip,
                    color = colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(UlamaTheme.dimensions.teamGridCrest)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entry.teamAbbreviation.ifBlank { displayName.take(3).uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = UlamaTheme.dimensions.teamNameBoxHeight)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFavorite) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ToggleButton(
                checked = isFavorite,
                onCheckedChange = { onToggleFavorite() },
                contentPadding = PaddingValues(
                    horizontal = UlamaTheme.spacing.medium,
                    vertical = UlamaTheme.spacing.extraSmall
                )
            ) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(UlamaTheme.dimensions.iconSmall)
                )
                Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                Text(
                    text = stringResource(
                        if (isFavorite) R.string.competitions_following_team else R.string.competitions_follow_team
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SearchOverlayResults(
    query: String,
    activeTeamsLeague: League?,
    selectedLeague: League,
    myLeagueCodes: Set<String>,
    favoriteTeamIds: Set<String>,
    availableTeamsByLeague: Map<League, List<StandingEntry>>,
    isDark: Boolean,
    onSelectLeague: (League) -> Unit,
    onOpenTeams: (League) -> Unit,
    onToggleMyLeague: (String) -> Unit,
    onToggleFavoriteTeam: (String) -> Unit
) {
    val context = LocalContext.current

    if (activeTeamsLeague != null) {
        val teams = remember(activeTeamsLeague, availableTeamsByLeague, favoriteTeamIds, query) {
            buildTeamsForLeague(
                league = activeTeamsLeague,
                availableTeamsByLeague = availableTeamsByLeague,
                favoriteTeamIds = favoriteTeamIds,
                query = query
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = UlamaTheme.spacing.normal,
                vertical = UlamaTheme.spacing.small
            ),
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.extraSmall)
        ) {
            if (teams.isEmpty() && query.isNotBlank()) {
                item(key = "search_empty") {
                    Text(
                        text = stringResource(R.string.competitions_empty_search, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(UlamaTheme.spacing.normal)
                    )
                }
            } else {
                items(items = teams, key = { "search_team_${it.stableId}" }) { item ->
                    val teamId = item.teamKey
                    TeamSearchResultRow(
                        item = item,
                        isFavorite = teamId in favoriteTeamIds,
                        isDark = isDark,
                        onToggleFavorite = { onToggleFavoriteTeam(teamId) }
                    )
                }
            }
        }
        return
    }

    val matchingLeagues = remember(query, context) {
        if (query.isBlank()) {
            League.standardLeagues
        } else {
            val lower = query.lowercase()
            League.standardLeagues.filter { league ->
                context.getString(league.nameRes).lowercase().contains(lower) ||
                    context.getString(league.subRegionRes).lowercase().contains(lower) ||
                    context.getString(league.region.labelRes).lowercase().contains(lower)
            }
        }
    }

    val matchingTeams = remember(query, availableTeamsByLeague) {
        if (query.isBlank()) {
            emptyList()
        } else {
            val lower = query.lowercase()
            availableTeamsByLeague.entries
                .asSequence()
                .flatMap { (league, entries) ->
                    entries.asSequence().map { IndexedTeamEntry(league = league, entry = it) }
                }
                .filter { item ->
                    item.entry.teamName.lowercase().contains(lower) ||
                        item.entry.teamShortName.lowercase().contains(lower) ||
                        item.entry.teamAbbreviation.lowercase().contains(lower)
                }
                .distinctBy { it.teamKey }
                .take(20)
                .toList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = UlamaTheme.spacing.normal,
            vertical = UlamaTheme.spacing.small
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        if (matchingLeagues.isNotEmpty()) {
            item(key = "search_leagues_header") {
                Text(
                    text = stringResource(R.string.competitions_search_leagues_header),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = UlamaTheme.spacing.extraSmall)
                )
            }
            items(
                count = matchingLeagues.size,
                key = { idx -> "search_league_${matchingLeagues[idx].code}" }
            ) { index ->
                val league = matchingLeagues[index]
                val isInMyLeagues = league.code in myLeagueCodes
                val leagueName = stringResource(league.nameRes)
                SegmentedListItem(
                    selected = selectedLeague == league,
                    onClick = { onSelectLeague(league) },
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = matchingLeagues.size),
                    colors = segmentedItemColors(),
                    supportingContent = { Text(text = stringResource(league.subRegionRes)) },
                    leadingContent = {
                        AsyncImage(
                            model = league.getLogoUrl(isDark),
                            contentDescription = null,
                            modifier = Modifier.size(UlamaTheme.dimensions.leagueLogoList)
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledIconToggleButton(
                                checked = isInMyLeagues,
                                onCheckedChange = { onToggleMyLeague(league.code) }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isInMyLeagues) R.drawable.ic_star else R.drawable.ic_star_outline
                                    ),
                                    contentDescription = stringResource(R.string.cd_toggle_my_league, leagueName),
                                    modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
                                )
                            }
                            IconButton(onClick = { onOpenTeams(league) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chevron_right),
                                    contentDescription = stringResource(R.string.cd_open_teams, leagueName)
                                )
                            }
                        }
                    },
                    content = {
                        Text(
                            text = leagueName,
                            fontWeight = if (selectedLeague == league) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        if (matchingTeams.isNotEmpty()) {
            item(key = "search_teams_header") {
                Text(
                    text = stringResource(R.string.competitions_search_teams_header),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = UlamaTheme.spacing.small, bottom = UlamaTheme.spacing.extraSmall)
                )
            }
            items(
                count = matchingTeams.size,
                key = { idx -> "global_team_${matchingTeams[idx].stableId}" }
            ) { index ->
                val item = matchingTeams[index]
                val teamId = item.teamKey
                TeamSearchResultRow(
                    item = item,
                    isFavorite = teamId in favoriteTeamIds,
                    isDark = isDark,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = matchingTeams.size),
                    onToggleFavorite = { onToggleFavoriteTeam(teamId) }
                )
            }
        }

        if (matchingLeagues.isEmpty() && matchingTeams.isEmpty() && query.isNotBlank()) {
            item(key = "search_no_results") {
                Text(
                    text = stringResource(R.string.competitions_empty_search, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(UlamaTheme.spacing.normal)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TeamSearchResultRow(
    item: IndexedTeamEntry,
    isFavorite: Boolean,
    isDark: Boolean,
    onToggleFavorite: () -> Unit,
    shapes: androidx.compose.material3.ListItemShapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
) {
    SegmentedListItem(
        selected = isFavorite,
        onClick = onToggleFavorite,
        shapes = shapes,
        colors = segmentedItemColors(),
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.competitions_team_league_subtitle,
                    stringResource(item.league.nameRes)
                )
            )
        },
        leadingContent = {
            item.entry.getLogoUrl(isDark)?.let { logo ->
                AsyncImage(
                    model = logo,
                    contentDescription = null,
                    modifier = Modifier.size(UlamaTheme.dimensions.leagueLogoList)
                )
            }
        },
        trailingContent = {
            FilledIconToggleButton(
                checked = isFavorite,
                onCheckedChange = { onToggleFavorite() }
            ) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
                )
            }
        },
        content = {
            Text(text = item.entry.teamShortName.ifBlank { item.entry.teamName })
        }
    )
}

@Composable
private fun segmentedItemColors() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLeadingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTrailingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedSupportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer
)

private fun buildTeamsForLeague(
    league: League,
    availableTeamsByLeague: Map<League, List<StandingEntry>>,
    favoriteTeamIds: Set<String>,
    query: String
): List<IndexedTeamEntry> {
    val baseTeams = if (league == League.FAVORITES) {
        availableTeamsByLeague.entries
            .asSequence()
            .flatMap { (srcLeague, entries) ->
                entries.asSequence().map { IndexedTeamEntry(league = srcLeague, entry = it) }
            }
            .filter { it.teamKey in favoriteTeamIds }
            .distinctBy { it.teamKey }
            .sortedBy { it.entry.teamShortName.ifBlank { it.entry.teamName } }
            .toList()
    } else {
        availableTeamsByLeague[league].orEmpty()
            .map { IndexedTeamEntry(league = league, entry = it) }
            .sortedBy { it.entry.teamShortName.ifBlank { it.entry.teamName } }
    }

    if (query.isBlank()) return baseTeams
    val lower = query.lowercase()
    return baseTeams.filter { item ->
        item.entry.teamName.lowercase().contains(lower) ||
            item.entry.teamShortName.lowercase().contains(lower) ||
            item.entry.teamAbbreviation.lowercase().contains(lower)
    }
}

private fun balanceGroupsIntoTwoColumns(
    groups: List<SubRegionGroup>
): Pair<List<SubRegionGroup>, List<SubRegionGroup>> {
    val left = mutableListOf<SubRegionGroup>()
    val right = mutableListOf<SubRegionGroup>()
    var leftLoad = 0
    var rightLoad = 0
    groups.forEach { group ->
        val cost = group.leagues.size + 1
        if (leftLoad <= rightLoad) {
            left.add(group)
            leftLoad += cost
        } else {
            right.add(group)
            rightLoad += cost
        }
    }
    return left to right
}
