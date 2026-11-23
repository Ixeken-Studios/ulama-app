package com.ixeken.ulama.ui.components

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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.LeagueCategory
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Modal Bottom Sheet para la seleccion de competiciones de futbol en Ulama.
 * Reemplaza el antiguo menu desplegable con una cuadricula circular interactiva
 * construida con Material 3 Expressive, soporte de Dynamic Color y la regla Clip Before Click.
 *
 * @param selectedLeague Competicion actualmente activa.
 * @param onLeagueSelected Callback invocado al seleccionar una nueva liga.
 * @param onDismissRequest Accion ejecutada para cerrar la hoja modal.
 * @param modifier Modificador base para el contenedor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguePickerBottomSheet(
    selectedLeague: League,
    onLeagueSelected: (League) -> Unit,
    onDismissRequest: () -> Unit,
    favoriteCount: Int = 0,
    myLeaguesCount: Int = 0,
    onManageFavorites: () -> Unit = {},
    onManageMyLeagues: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val isDark = UlamaTheme.isDark

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UlamaTheme.spacing.normal),
                contentAlignment = Alignment.Center
            ) {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
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
            var selectedCategory by remember(selectedLeague) {
                mutableStateOf(
                    if (selectedLeague.isInternational) LeagueCategory.INTERNATIONAL else LeagueCategory.CLUBS
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = UlamaTheme.spacing.small, bottom = UlamaTheme.spacing.extraSmall),
                horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.normal),
                verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "virtual_leagues_row"
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            UlamaTheme.spacing.large,
                            Alignment.CenterHorizontally
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LeagueCircleItem(
                            league = League.FAVORITES,
                            isSelected = selectedLeague == League.FAVORITES,
                            badgeCount = favoriteCount,
                            isDark = isDark,
                            onClick = {
                                onLeagueSelected(League.FAVORITES)
                                if (favoriteCount == 0) onManageFavorites()
                                onDismissRequest()
                            },
                            circleSize = 50.dp,
                            iconSize = 24.dp,
                            modifier = Modifier.width(84.dp)
                        )
                        LeagueCircleItem(
                            league = League.MY_LEAGUES,
                            isSelected = selectedLeague == League.MY_LEAGUES,
                            badgeCount = myLeaguesCount,
                            isDark = isDark,
                            onClick = {
                                onLeagueSelected(League.MY_LEAGUES)
                                if (myLeaguesCount == 0) onManageMyLeagues()
                                onDismissRequest()
                            },
                            circleSize = 50.dp,
                            iconSize = 24.dp,
                            modifier = Modifier.width(84.dp)
                        )
                    }
                }

                item(
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "category_tabs_row"
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = UlamaTheme.spacing.small,
                                bottom = UlamaTheme.spacing.extraSmall
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveConnectedButtonGroup(
                            options = LeagueCategory.entries,
                            selectedOption = selectedCategory,
                            onOptionSelected = { selectedCategory = it },
                            labelProvider = { stringResource(it.titleRes) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                val currentLeagues = when (selectedCategory) {
                    LeagueCategory.CLUBS -> League.clubLeagues
                    LeagueCategory.INTERNATIONAL -> League.internationalLeagues
                }

                items(
                    items = currentLeagues,
                    key = { it.code },
                    contentType = { "league_circle_item" }
                ) { league ->
                    LeagueCircleItem(
                        league = league,
                        isSelected = league == selectedLeague,
                        badgeCount = 0,
                        isDark = isDark,
                        onClick = {
                            onLeagueSelected(league)
                            onDismissRequest()
                        },
                        circleSize = 57.dp,
                        logoSize = 34.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

            Row(
                horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismissRequest()
                        onManageFavorites()
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                    Text(
                        text = stringResource(R.string.fav_teams_picker_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDismissRequest()
                        onManageMyLeagues()
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                    Text(
                        text = stringResource(R.string.my_leagues_edit_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Elemento visual circular interactivo para representar una competicion o vista agrupada.
 * Aplica la regla Clip Before Click para limitar la ondulacion tactil al circulo
 * y presenta la insignia de conteo reactiva sobre el borde superior derecho.
 */
@Composable
internal fun LeagueCircleItem(
    league: League,
    isSelected: Boolean,
    badgeCount: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circleSize: Dp = 57.dp,
    iconSize: Dp = 26.dp,
    logoSize: Dp = 34.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
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
                    .size(circleSize)
                    .bounceClick(enabled = true, interactionSource = interactionSource)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    when (league) {
                        League.FAVORITES -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = stringResource(league.nameRes),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        League.MY_LEAGUES -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_trophy),
                                contentDescription = stringResource(league.nameRes),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        League.DEBUG -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_code),
                                contentDescription = stringResource(league.nameRes),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        else -> {
                            AsyncImage(
                                model = league.getLogoUrl(isDark),
                                contentDescription = stringResource(
                                    R.string.cd_league_logo,
                                    stringResource(league.nameRes)
                                ),
                                modifier = Modifier.size(logoSize)
                            )
                        }
                    }
                }
            }

            if (badgeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

        val leagueName = stringResource(league.nameRes)
        Text(
            text = if (isSelected) "★ $leagueName" else leagueName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
