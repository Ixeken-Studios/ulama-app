package com.ixeken.ulama.ui.components.detail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchDetail
import com.ixeken.ulama.core.model.MatchDetailTab
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.core.model.TableCutoff
import com.ixeken.ulama.core.model.TableLegendEntry
import com.ixeken.ulama.ui.components.StandingsTable
import com.ixeken.ulama.ui.components.UlamaSegmentedTabs
import com.ixeken.ulama.ui.components.animateListEntry
import com.ixeken.ulama.ui.theme.UlamaTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * ModalBottomSheet de detalle de partido de grado industrial.
 * Respeta la arquitectura edge to edge usando contentWindowInsets = WindowInsets(0, 0, 0, 0) y navigationBarsPadding interno,
 * delimitando la altura maxima al 86% de la pantalla para preservar el fondo/scrim superior visible.
 * En partidos proximos muestra exclusivamente historial H2H y tabla de posiciones;
 * en partidos pasados o en vivo ofrece navegacion por pestanas sincronizada con gestos horizontales
 * mediante HorizontalPager y scroll vertical independiente por pestana para evitar saltos de altura.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailBottomSheet(
    match: Match,
    matchDetail: MatchDetail?,
    isLoadingDetail: Boolean,
    standings: List<StandingEntry>,
    cutoffs: List<TableCutoff>,
    legend: List<TableLegendEntry>,
    isFollowing: Boolean = false,
    onFollowToggle: () -> Unit = {},
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = configuration.screenHeightDp.dp * 0.86f

    val highlightedTeamIds = remember(match) {
        setOfNotNull(
            match.homeTeam.id.takeIf { it.isNotBlank() },
            match.awayTeam.id.takeIf { it.isNotBlank() },
            match.homeTeam.name.takeIf { it.isNotBlank() },
            match.awayTeam.name.takeIf { it.isNotBlank() },
            match.homeTeam.shortName.takeIf { it.isNotBlank() },
            match.awayTeam.shortName.takeIf { it.isNotBlank() }
        )
    }

    val availableTabs = remember(match.league, standings) {
        if (!match.league.hasStandings || standings.isEmpty()) {
            listOf(MatchDetailTab.STATS, MatchDetailTab.LINEUPS, MatchDetailTab.EVENTS)
        } else {
            MatchDetailTab.entries
        }
    }

    val defaultHomeFallback = MaterialTheme.colorScheme.primary
    val defaultAwayFallback = MaterialTheme.colorScheme.tertiary

    val resolvedColors = remember(match, matchDetail, defaultHomeFallback, defaultAwayFallback) {
        DetailColorUtils.resolveMatchColors(
            homePrimaryHex = matchDetail?.homeColor ?: match.homeTeam.color,
            homeAlternateHex = matchDetail?.homeAlternateColor ?: match.homeTeam.alternateColor,
            awayPrimaryHex = matchDetail?.awayColor ?: match.awayTeam.color,
            awayAlternateHex = matchDetail?.awayAlternateColor ?: match.awayTeam.alternateColor,
            homeFallback = defaultHomeFallback,
            awayFallback = defaultAwayFallback
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // Edge to edge estricto: sin insets artificiales para que el fondo fluya hasta el borde inferior
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding() // Protege la interaccion tactil sin cortar el fondo continuo
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                // Estado PROXIMO: Scroll unico con cabecera, historial directo y tabla de posiciones
                if (match.status.state == MatchState.SCHEDULED) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = UlamaTheme.spacing.cardPadding)
                            .padding(bottom = UlamaTheme.spacing.contentSpacing)
                    ) {
                        MatchDetailHeader(
                            match = match,
                            matchDetail = matchDetail,
                            isFollowing = isFollowing,
                            onFollowToggle = onFollowToggle,
                            modifier = Modifier.animateListEntry(index = 0, key = match.id)
                        )

                        Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))

                        if (matchDetail?.headToHead != null) {
                            HeadToHeadCard(
                                h2h = matchDetail.headToHead,
                                homeTeam = match.homeTeam,
                                awayTeam = match.awayTeam,
                                homeColor = resolvedColors.homeColor,
                                awayColor = resolvedColors.awayColor,
                                modifier = Modifier.animateListEntry(index = 1, key = match.id)
                            )
                            Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
                        }
                        if (match.league.hasStandings) {
                            if (standings.isNotEmpty()) {
                                StandingsTable(
                                    standings = standings,
                                    cutoffs = cutoffs,
                                    legend = legend,
                                    highlightedTeamIds = highlightedTeamIds,
                                    modifier = Modifier.animateListEntry(
                                        index = if (matchDetail?.headToHead != null) 2 else 1,
                                        key = match.id
                                    )
                                )
                            } else {
                                DetailLoadingIndicatorBox()
                            }
                        }
                    }
                } else {
                    var headerHeightPx by remember { mutableFloatStateOf(0f) }
                    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { availableTabs.size }
                    )
                    val coroutineScope = rememberCoroutineScope()
                    val motion = UlamaTheme.motion
                    // Sin rebote lateral para no dejar asomar el borde de la pagina vecina al frenar
                    val pagerSnapSpec = remember(motion.mode) {
                        spring<Float>(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = if (motion.isExpressive) Spring.StiffnessMediumLow else Spring.StiffnessMedium
                        )
                    }
                    // Lectura derivada de targetPage para actualizar la pildora activa sin recomponer durante el arrastre
                    val selectedTab by remember(pagerState, availableTabs) {
                        derivedStateOf {
                            availableTabs.getOrElse(pagerState.targetPage) { MatchDetailTab.STATS }
                        }
                    }

                    // Coordina el colapso antes del scroll interno y expande antes de arrastrar para cerrar el sheet
                    val headerNestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                if (available.y < 0f && headerHeightPx > 0f && headerOffsetPx > -headerHeightPx) {
                                    val newOffset = (headerOffsetPx + available.y).coerceIn(-headerHeightPx, 0f)
                                    val consumedY = newOffset - headerOffsetPx
                                    headerOffsetPx = newOffset
                                    return Offset(0f, consumedY)
                                }
                                return Offset.Zero
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (available.y > 0f && headerHeightPx > 0f && headerOffsetPx < 0f) {
                                    val newOffset = (headerOffsetPx + available.y).coerceIn(-headerHeightPx, 0f)
                                    val consumedY = newOffset - headerOffsetPx
                                    headerOffsetPx = newOffset
                                    return Offset(0f, consumedY)
                                }
                                return Offset.Zero
                            }
                        }
                    }

                    LaunchedEffect(match.id) {
                        headerOffsetPx = 0f
                        if (pagerState.currentPage != 0) {
                            pagerState.scrollToPage(0)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .nestedScroll(headerNestedScrollConnection)
                    ) {
                        // Colapso aislado en fases de layout y dibujado para no recomponer ni cabecera ni pestanas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clipToBounds()
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints.copy(minHeight = 0))
                                    val offset = headerOffsetPx.roundToInt().coerceIn(-placeable.height, 0)
                                    val visibleHeight = (placeable.height + offset).coerceAtLeast(0)
                                    layout(placeable.width, visibleHeight) {
                                        placeable.placeRelative(0, offset)
                                    }
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        val newHeight = size.height.toFloat()
                                        val wasCollapsed = headerHeightPx > 0f && headerOffsetPx <= -headerHeightPx + 1f
                                        headerHeightPx = newHeight
                                        headerOffsetPx = if (wasCollapsed) {
                                            -newHeight
                                        } else {
                                            headerOffsetPx.coerceIn(-newHeight, 0f)
                                        }
                                    }
                                    .graphicsLayer {
                                        val progress = if (headerHeightPx > 0f) {
                                            1f + (headerOffsetPx / headerHeightPx)
                                        } else {
                                            1f
                                        }
                                        alpha = progress.coerceIn(0f, 1f)
                                    }
                            ) {
                                MatchDetailHeader(
                                    match = match,
                                    matchDetail = matchDetail,
                                    isFollowing = isFollowing,
                                    onFollowToggle = onFollowToggle,
                                    modifier = Modifier
                                        .padding(horizontal = UlamaTheme.spacing.cardPadding)
                                        .animateListEntry(index = 0, key = match.id)
                                )
                                Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
                            }
                        }

                        // Estados FINALIZADO y EN VIVO: Barra de pestanas fija arriba tras colapsar la cabecera
                        UlamaSegmentedTabs(
                            items = availableTabs,
                            selectedItem = selectedTab,
                            onItemSelected = { tab ->
                                val targetIndex = availableTabs.indexOf(tab)
                                if (targetIndex >= 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = targetIndex,
                                            animationSpec = pagerSnapSpec
                                        )
                                    }
                                }
                            },
                            label = { stringResource(it.titleRes) },
                            iconRes = { it.iconRes },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = UlamaTheme.spacing.cardPadding)
                                .animateListEntry(index = 1, key = match.id)
                        )

                        Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))

                        if (isLoadingDetail && matchDetail == null) {
                            DetailLoadingIndicatorBox(
                                modifier = Modifier
                                    .padding(horizontal = UlamaTheme.spacing.cardPadding)
                                    .padding(bottom = UlamaTheme.spacing.contentSpacing)
                            )
                        } else {
                            // Pre-compone la pestana vecina para evitar tirones al iniciar el gesto lateral
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 1,
                                key = { page -> availableTabs.getOrNull(page)?.name ?: "empty_$page" },
                                flingBehavior = PagerDefaults.flingBehavior(
                                    state = pagerState,
                                    snapAnimationSpec = pagerSnapSpec
                                ),
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                            ) { page ->
                                val tab = availableTabs.getOrNull(page) ?: return@HorizontalPager
                                // Scroll vertical aislado por pagina para preservar posicion y evitar saltos de altura
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = UlamaTheme.spacing.cardPadding)
                                        .padding(bottom = UlamaTheme.spacing.contentSpacing)
                                ) {
                                    when (tab) {
                                        MatchDetailTab.STATS -> MatchStatsTabContent(
                                            match = match,
                                            matchDetail = matchDetail,
                                            homeColor = resolvedColors.homeColor,
                                            awayColor = resolvedColors.awayColor
                                        )
                                        MatchDetailTab.LINEUPS -> MatchLineupsTabContent(
                                            matchId = match.id,
                                            matchDetail = matchDetail,
                                            homeColor = resolvedColors.homeColor,
                                            awayColor = resolvedColors.awayColor
                                        )
                                        MatchDetailTab.EVENTS -> MatchEventsTabContent(
                                            match = match,
                                            matchDetail = matchDetail,
                                            homeColor = resolvedColors.homeColor,
                                            awayColor = resolvedColors.awayColor
                                        )
                                        MatchDetailTab.TABLE -> MatchTableTabContent(
                                            matchId = match.id,
                                            standings = standings,
                                            cutoffs = cutoffs,
                                            legend = legend,
                                            highlightedTeamIds = highlightedTeamIds
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailLoadingIndicatorBox(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        ContainedLoadingIndicator(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            indicatorColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MatchStatsTabContent(
    match: Match,
    matchDetail: MatchDetail?,
    homeColor: Color,
    awayColor: Color
) {
    val showMatchLeaders = match.isFinished && matchDetail?.leaders?.isNotEmpty() == true
    if (matchDetail != null && matchDetail.stats.isNotEmpty()) {
        MatchStatsView(
            stats = matchDetail.stats,
            homeColor = homeColor,
            awayColor = awayColor,
            modifier = Modifier.animateListEntry(index = 2, key = "${match.id}_stats")
        )
    } else if (matchDetail == null || (matchDetail.headToHead == null && !showMatchLeaders)) {
        Text(
            text = stringResource(R.string.stats_not_available),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 32.dp)
        )
    }

    if (showMatchLeaders) {
        Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
        MatchLeadersCard(
            leaders = matchDetail.leaders,
            homeTeam = match.homeTeam,
            awayTeam = match.awayTeam,
            homeColor = homeColor,
            awayColor = awayColor,
            modifier = Modifier.animateListEntry(index = 3, key = "${match.id}_leaders")
        )
    }

    if (matchDetail?.headToHead != null) {
        Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
        HeadToHeadCard(
            h2h = matchDetail.headToHead,
            homeTeam = match.homeTeam,
            awayTeam = match.awayTeam,
            homeColor = homeColor,
            awayColor = awayColor,
            modifier = Modifier.animateListEntry(
                index = if (showMatchLeaders) 4 else 3,
                key = "${match.id}_h2h"
            )
        )
    }
}

@Composable
private fun MatchLineupsTabContent(
    matchId: String,
    matchDetail: MatchDetail?,
    homeColor: Color,
    awayColor: Color
) {
    val hasLineupData = matchDetail != null && (
        (matchDetail.homeLineup?.starters?.isNotEmpty() == true) ||
            (matchDetail.awayLineup?.starters?.isNotEmpty() == true) ||
            (matchDetail.homeLineup?.bench?.isNotEmpty() == true) ||
            (matchDetail.awayLineup?.bench?.isNotEmpty() == true)
        )
    if (hasLineupData) {
        SoccerPitch(
            homeLineup = matchDetail.homeLineup,
            awayLineup = matchDetail.awayLineup,
            homeColor = homeColor,
            awayColor = awayColor,
            modifier = Modifier.animateListEntry(index = 2, key = "${matchId}_lineups")
        )
    } else {
        Text(
            text = stringResource(R.string.lineups_not_available),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 32.dp)
        )
    }
}

@Composable
private fun MatchEventsTabContent(
    match: Match,
    matchDetail: MatchDetail?,
    homeColor: Color,
    awayColor: Color
) {
    if (matchDetail != null && matchDetail.timelineEvents.isNotEmpty()) {
        MatchTimelineCard(
            events = matchDetail.timelineEvents,
            matchId = match.id,
            homeTeam = match.homeTeam,
            awayTeam = match.awayTeam,
            homeColor = homeColor,
            awayColor = awayColor
        )
    } else {
        Text(
            text = stringResource(R.string.events_not_available),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 32.dp)
        )
    }
}

@Composable
private fun MatchTableTabContent(
    matchId: String,
    standings: List<StandingEntry>,
    cutoffs: List<TableCutoff>,
    legend: List<TableLegendEntry>,
    highlightedTeamIds: Set<String>
) {
    if (standings.isNotEmpty()) {
        StandingsTable(
            standings = standings,
            cutoffs = cutoffs,
            legend = legend,
            highlightedTeamIds = highlightedTeamIds,
            modifier = Modifier.animateListEntry(index = 2, key = "${matchId}_table")
        )
    } else {
        DetailLoadingIndicatorBox()
    }
}
