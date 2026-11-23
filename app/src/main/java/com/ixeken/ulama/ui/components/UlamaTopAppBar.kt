package com.ixeken.ulama.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Barra superior principal de Ulama construida con Material 3 Expressive.
 * Alinea a la izquierda el acceso a configuracion y a la derecha el selector desplegable de ligas activas,
 * manteniendo el centro despejado conforme al diseno minimalista.
 * El boton selector implementa la regla Clip Before Click para limitar la onda de choque tactil.
 *
 * @param selectedLeague Liga activa actualmente seleccionada.
 * @param onLeagueSelected Accion disparada al seleccionar una nueva liga en el menu desplegable.
 * @param onSettingsClick Accion disparada al pulsar el boton de ajustes.
 * @param favoriteCount Cantidad global de clubes favoritos seleccionados.
 * @param onManageFavorites Callback para abrir el selector de favoritos.
 * @param modifier Modificador base de Compose para el contenedor superior.
 */
@Composable
fun UlamaTopAppBar(
    selectedLeague: League,
    onLeagueSelected: (League) -> Unit,
    onSettingsClick: () -> Unit = {},
    favoriteCount: Int = 0,
    myLeaguesCount: Int = 0,
    onManageFavorites: () -> Unit = {},
    onManageMyLeagues: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    val isDark = UlamaTheme.isDark

    CenterAlignedTopAppBar(
        navigationIcon = {
            Box(modifier = Modifier.padding(start = UlamaTheme.spacing.normal)) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val settingsShape = rememberAnimatedShape(
                    isPressed = isPressed,
                    isSelected = false
                )

                Surface(
                    shape = settingsShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .bounceClick(enabled = true, interactionSource = interactionSource)
                        .clip(settingsShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = onSettingsClick
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(all = UlamaTheme.spacing.selectorPaddingVertical)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.cd_settings_button),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
                        )
                    }
                }
            }
        },
        title = {},
        actions = {
            Box(modifier = Modifier.padding(end = UlamaTheme.spacing.normal)) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val selectorShape = rememberAnimatedShape(
                    isPressed = isPressed,
                    isSelected = isSheetOpen
                )

                Surface(
                    shape = selectorShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .bounceClick(enabled = true, interactionSource = interactionSource)
                        .clip(selectorShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = { isSheetOpen = true }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = UlamaTheme.spacing.selectorPaddingHorizontal,
                            vertical = UlamaTheme.spacing.selectorPaddingVertical
                        )
                    ) {
                        when (selectedLeague) {
                            League.FAVORITES -> {
                                Icon(
                                    painter = painterResource(R.drawable.ic_star),
                                    contentDescription = stringResource(selectedLeague.nameRes),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
                                )
                            }
                            League.MY_LEAGUES -> {
                                Icon(
                                    painter = painterResource(R.drawable.ic_trophy),
                                    contentDescription = stringResource(selectedLeague.nameRes),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
                                )
                            }
                            else -> {
                                AsyncImage(
                                    model = selectedLeague.getLogoUrl(isDark),
                                    contentDescription = stringResource(
                                        R.string.cd_league_logo,
                                        stringResource(selectedLeague.nameRes)
                                    ),
                                    modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                        Text(
                            text = stringResource(selectedLeague.nameRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isSheetOpen) {
                LeaguePickerBottomSheet(
                    selectedLeague = selectedLeague,
                    onLeagueSelected = onLeagueSelected,
                    onDismissRequest = { isSheetOpen = false },
                    favoriteCount = favoriteCount,
                    myLeaguesCount = myLeaguesCount,
                    onManageFavorites = onManageFavorites,
                    onManageMyLeagues = onManageMyLeagues
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

@Preview(name = "UlamaTopAppBar - Dark Theme", showBackground = true)
@Composable
private fun UlamaTopAppBarDarkPreview() {
    UlamaTheme(darkTheme = true) {
        UlamaTopAppBar(
            selectedLeague = League.LIGA_MX,
            onLeagueSelected = {},
            onSettingsClick = {}
        )
    }
}

@Preview(name = "UlamaTopAppBar - Light Theme", showBackground = true)
@Composable
private fun UlamaTopAppBarLightPreview() {
    UlamaTheme(darkTheme = false) {
        UlamaTopAppBar(
            selectedLeague = League.PREMIER_LEAGUE,
            onLeagueSelected = {},
            onSettingsClick = {}
        )
    }
}
