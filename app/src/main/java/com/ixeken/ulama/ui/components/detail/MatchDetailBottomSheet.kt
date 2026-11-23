package com.ixeken.ulama.ui.components.detail

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * ModalBottomSheet de detalle de partido de grado industrial.
 * Respeta la arquitectura edge to edge usando contentWindowInsets = WindowInsets(0, 0, 0, 0) y navigationBarsPadding interno,
 * delimitando la altura maxima al 86% de la pantalla para preservar el fondo/scrim superior visible.
 * En partidos proximos muestra exclusivamente la tabla de posiciones con los clubes rivales;
 * en partidos pasados o en vivo ofrece conmutacion entre estadisticas, alineaciones y tabla.
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
    var selectedTab by remember { mutableStateOf(MatchDetailTab.STATS) }

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
                .verticalScroll(rememberScrollState())
                .padding(bottom = UlamaTheme.spacing.contentSpacing)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
                    .padding(horizontal = UlamaTheme.spacing.cardPadding)
            ) {
                // Cabecera Apple Sports: Nombres, marcadores, anotadores e icono de seguimiento
                MatchDetailHeader(
                    match = match,
                    matchDetail = matchDetail,
                    isFollowing = isFollowing,
                    onFollowToggle = onFollowToggle,
                    modifier = Modifier.animateListEntry(index = 0, key = match.id)
                )

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))

                // Estado PROXIMO: Mostrar historial directo si existe y tabla de posiciones con clubes rivales destacados
                if (match.status.state == MatchState.SCHEDULED) {
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
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                ContainedLoadingIndicator(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    indicatorColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    // Estados FINALIZADO y EN VIVO: Barra de pestanas reutilizable con iconos nativos
                    UlamaSegmentedTabs(
                        items = availableTabs,
                        selectedItem = if (selectedTab in availableTabs) selectedTab else MatchDetailTab.STATS,
                        onItemSelected = { selectedTab = it },
                        label = { stringResource(it.titleRes) },
                        iconRes = { it.iconRes },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateListEntry(index = 1, key = match.id)
                    )

                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))

                    if (isLoadingDetail && matchDetail == null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            ContainedLoadingIndicator(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        when (selectedTab) {
                            MatchDetailTab.STATS -> {
                                val showMatchLeaders = match.isFinished && matchDetail?.leaders?.isNotEmpty() == true
                                if (matchDetail != null && matchDetail.stats.isNotEmpty()) {
                                    MatchStatsView(
                                        stats = matchDetail.stats,
                                        homeColor = resolvedColors.homeColor,
                                        awayColor = resolvedColors.awayColor,
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
                                        homeColor = resolvedColors.homeColor,
                                        awayColor = resolvedColors.awayColor,
                                        modifier = Modifier.animateListEntry(index = 3, key = "${match.id}_leaders")
                                    )
                                }

                                if (matchDetail?.headToHead != null) {
                                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
                                    HeadToHeadCard(
                                        h2h = matchDetail.headToHead,
                                        homeTeam = match.homeTeam,
                                        awayTeam = match.awayTeam,
                                        homeColor = resolvedColors.homeColor,
                                        awayColor = resolvedColors.awayColor,
                                        modifier = Modifier.animateListEntry(
                                            index = if (showMatchLeaders) 4 else 3,
                                            key = "${match.id}_h2h"
                                        )
                                    )
                                }
                            }
                            MatchDetailTab.LINEUPS -> {
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
                                        homeColor = resolvedColors.homeColor,
                                        awayColor = resolvedColors.awayColor,
                                        modifier = Modifier.animateListEntry(index = 2, key = "${match.id}_lineups")
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
                            MatchDetailTab.EVENTS -> {
                                if (matchDetail != null && matchDetail.timelineEvents.isNotEmpty()) {
                                    MatchTimelineCard(
                                        events = matchDetail.timelineEvents,
                                        matchId = match.id,
                                        homeTeam = match.homeTeam,
                                        awayTeam = match.awayTeam,
                                        homeColor = resolvedColors.homeColor,
                                        awayColor = resolvedColors.awayColor
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
                            MatchDetailTab.TABLE -> {
                                if (standings.isNotEmpty()) {
                                    StandingsTable(
                                        standings = standings,
                                        cutoffs = cutoffs,
                                        legend = legend,
                                        highlightedTeamIds = highlightedTeamIds,
                                        modifier = Modifier.animateListEntry(index = 2, key = "${match.id}_table")
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    ) {
                                        ContainedLoadingIndicator(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            indicatorColor = MaterialTheme.colorScheme.primary
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
