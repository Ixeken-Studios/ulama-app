package com.ixeken.ulama.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Hoja modal inferior para seleccion y gestion jerarquica de clubes favoritos.
 * Permite explorar N ligas de forma escalable sin saturar la pantalla.
 *
 * Nivel 1: Cuadricula circular de ligas con insignia del conteo de clubes seleccionados.
 * Nivel 2: Desglose de clubes con logotipo oficial, seleccion reactiva e interruptor visual.
 * Respeta rigurosamente Material 3 Expressive, tokens en tres niveles y la regla Clip Before Click.
 *
 * @param favoriteTeamIds Conjunto reactivo con los IDs de clubes seleccionados.
 * @param availableTeamsByLeague Mapa de equipos disponibles clasificados por competicion.
 * @param onToggleFavorite Accion invocada al alternar un equipo como favorito.
 * @param onDismissRequest Callback para cerrar la hoja modal.
 * @param initialLeague Liga inicial a mostrar al abrir la hoja modal (opcional).
 * @param modifier Modificador base para el contenedor Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteTeamsBottomSheet(
    favoriteTeamIds: Set<String>,
    availableTeamsByLeague: Map<League, List<StandingEntry>>,
    onToggleFavorite: (teamId: String) -> Unit,
    onDismissRequest: () -> Unit,
    initialLeague: League? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val isDark = UlamaTheme.isDark
    val motion = UlamaTheme.motion
    var activeLeague by remember(initialLeague) { mutableStateOf<League?>(initialLeague) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        },
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UlamaTheme.spacing.normal)
                .padding(bottom = UlamaTheme.spacing.listBottomPadding)
        ) {
            AnimatedContent(
                targetState = activeLeague,
                transitionSpec = {
                    val isForward = targetState != null

                    if (isForward) {
                        (slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = motion.spatialSpec()
                        ) + fadeIn(animationSpec = motion.effectsSpec()))
                            .togetherWith(
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    targetOffset = { fullWidth -> fullWidth / 3 },
                                    animationSpec = motion.spatialSpec()
                                ) + fadeOut(animationSpec = motion.effectsSpec())
                            )
                    } else {
                        (slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            initialOffset = { fullWidth -> -fullWidth / 3 },
                            animationSpec = motion.spatialSpec()
                        ) + fadeIn(animationSpec = motion.effectsSpec()))
                            .togetherWith(
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = motion.spatialSpec()
                                ) + fadeOut(animationSpec = motion.effectsSpec())
                            )
                    }
                },
                label = "favorite_teams_hierarchy_step"
            ) { targetLeague ->
                if (targetLeague == null) {
                    LeaguesSelectionStep(
                        favoriteTeamIds = favoriteTeamIds,
                        availableTeamsByLeague = availableTeamsByLeague,
                        isDark = isDark,
                        onLeagueClick = { activeLeague = it },
                        onDismissRequest = onDismissRequest
                    )
                } else {
                    TeamsSelectionStep(
                        league = targetLeague,
                        favoriteTeamIds = favoriteTeamIds,
                        teams = availableTeamsByLeague[targetLeague].orEmpty(),
                        isDark = isDark,
                        onToggleFavorite = onToggleFavorite,
                        onBackClick = { activeLeague = null },
                        onDismissRequest = onDismissRequest
                    )
                }
            }
        }
    }
}

/**
 * Nivel 1: Despliegue de competiciones con insignias numericas de favoritos.
 */
@Composable
private fun LeaguesSelectionStep(
    favoriteTeamIds: Set<String>,
    availableTeamsByLeague: Map<League, List<StandingEntry>>,
    isDark: Boolean,
    onLeagueClick: (League) -> Unit,
    onDismissRequest: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Cabecera de Nivel 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = UlamaTheme.spacing.normal)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(end = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.fav_teams_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.fav_teams_select_league_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cd_close_league_selector),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Cuadricula circular de ligas escalable
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(top = UlamaTheme.spacing.small, bottom = UlamaTheme.spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.normal),
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            items(
                items = League.standardLeagues,
                key = { it.code },
                contentType = { "fav_league_circle" }
            ) { league ->
                val leagueTeams = availableTeamsByLeague[league].orEmpty()
                val favCount = leagueTeams.count { it.teamId in favoriteTeamIds }
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onLeagueClick(league) }
                        )
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                width = if (favCount > 0) 2.dp else 1.dp,
                                color = if (favCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .size(57.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(),
                                    onClick = { onLeagueClick(league) }
                                )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                AsyncImage(
                                    model = league.getLogoUrl(isDark),
                                    contentDescription = stringResource(
                                        R.string.cd_league_logo,
                                        stringResource(league.nameRes)
                                    ),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        if (favCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = favCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

                    Text(
                        text = stringResource(league.nameRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val countFormat = if (favCount == 1) {
                        R.string.fav_teams_league_count_format
                    } else {
                        R.string.fav_teams_league_count_plural_format
                    }
                    Text(
                        text = stringResource(countFormat, favCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.normal))

        // Barra inferior de confirmacion
        Button(
            onClick = onDismissRequest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .height(48.dp)
                .bounceClick()
        ) {
            Text(
                text = stringResource(R.string.fav_teams_done_action),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Nivel 2: Desglose de los clubes de una competicion particular.
 */
@Composable
private fun TeamsSelectionStep(
    league: League,
    favoriteTeamIds: Set<String>,
    teams: List<StandingEntry>,
    isDark: Boolean,
    onToggleFavorite: (teamId: String) -> Unit,
    onBackClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Cabecera de Nivel 2 con boton de retroceso
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = UlamaTheme.spacing.normal)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.cd_back_to_leagues),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = UlamaTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(league.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.fav_teams_league_clubs_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier.clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cd_close_league_selector),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Cuadricula circular de clubes escalable con regla Clip Before Click
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(top = UlamaTheme.spacing.small, bottom = UlamaTheme.spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.normal),
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .height(360.dp)
        ) {
            items(
                items = teams,
                key = { it.teamId },
                contentType = { "fav_team_circle" }
            ) { team ->
                val isSelected = team.teamId.isNotBlank() && favoriteTeamIds.contains(team.teamId)
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onToggleFavorite(team.teamId) }
                        )
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .size(57.dp)
                                .bounceClick(enabled = true, interactionSource = interactionSource)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(),
                                    onClick = { onToggleFavorite(team.teamId) }
                                )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                AsyncImage(
                                    model = team.getLogoUrl(isDark),
                                    contentDescription = stringResource(R.string.cd_team_crest, team.teamName),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .iconPop(trigger = isSelected, animateOnAttach = true)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

                    Text(
                        text = team.teamShortName.ifBlank { team.teamName },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.normal))

        // Botonera inferior: retroceso a ligas y confirmacion
        Row(
            horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.normal),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.fav_teams_back_to_leagues),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onDismissRequest,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.fav_teams_done_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
