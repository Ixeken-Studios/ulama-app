package com.ixeken.ulama.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.LeagueCategory
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Hoja modal inferior para seleccion y gestion de torneos incluidos en la vista virtual Mis Ligas.
 * Permite seleccionar o desmarcar torneos para agregarlos en un espacio comun de consulta.
 * Cumple estrictamente con la regla Clip Before Click y el sistema de tokens en tres niveles.
 *
 * @param selectedLeagueCodes Conjunto reactivo con los codigos de liga actualmente seleccionados.
 * @param onToggleLeague Accion invocada para alternar la seleccion de una competicion.
 * @param onDismissRequest Callback para cerrar la hoja modal.
 * @param modifier Modificador base para el contenedor Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLeaguesBottomSheet(
    selectedLeagueCodes: Set<String>,
    onToggleLeague: (leagueCode: String) -> Unit,
    onDismissRequest: () -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UlamaTheme.spacing.extraSmall)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.my_leagues_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.my_leagues_subtitle),
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

            var selectedCategory by remember { mutableStateOf(LeagueCategory.CLUBS) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
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

            val currentLeagues = when (selectedCategory) {
                LeagueCategory.CLUBS -> League.clubLeagues
                LeagueCategory.INTERNATIONAL -> League.internationalLeagues
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = UlamaTheme.spacing.small, bottom = UlamaTheme.spacing.extraSmall),
                horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.normal),
                verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .weight(1f, fill = false)
            ) {
                items(
                    items = currentLeagues,
                    key = { it.code },
                    contentType = { "my_leagues_circle_item" }
                ) { league ->
                    val isSelected = league.code in selectedLeagueCodes
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onToggleLeague(league.code) }
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
                                        onClick = { onToggleLeague(league.code) }
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
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

                        Text(
                            text = stringResource(league.nameRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(UlamaTheme.spacing.normal))

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
                    text = stringResource(R.string.my_leagues_done_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
