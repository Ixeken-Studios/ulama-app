package com.ixeken.ulama.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.TemporalFilter
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Tarjeta de presentacion limpia y concisa para estados sin partidos.
 * Muestra directamente el estado temporal sin subtitulos redundantes,
 * permitiendo una lectura inmediata y priorizando la tabla general adyacente.
 * Emplea estrictamente tokens de nivel 3 para formas, dimensiones y espaciados.
 *
 * @param filter Filtro temporal actualmente seleccionado para personalizar el mensaje.
 * @param modifier Modificador base de Compose para el contenedor.
 */
@Composable
fun EmptyMatchesCard(
    filter: TemporalFilter,
    modifier: Modifier = Modifier
) {
    val titleRes = when (filter) {
        TemporalFilter.TODAY -> R.string.empty_events_today
        TemporalFilter.PAST -> R.string.empty_events_past
        TemporalFilter.UPCOMING -> R.string.empty_events_upcoming
    }

    Surface(
        shape = UlamaTheme.shapes.container,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = UlamaTheme.spacing.emptyCardPaddingHorizontal,
                    vertical = UlamaTheme.spacing.emptyCardPaddingVertical
                )
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Tarjeta de invitacion cuando la competicion Favoritos no cuenta con clubes registrados.
 * Ofrece explicacion directa y un boton para abrir de inmediato el selector de equipos.
 *
 * @param onManageFavorites Callback ejecutado al pulsar el boton de accion.
 * @param modifier Modificador base de Compose para el contenedor exterior.
 */
@Composable
fun EmptyFavoritesCard(
    onManageFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = UlamaTheme.shapes.container,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = UlamaTheme.spacing.emptyCardPaddingHorizontal,
                    vertical = UlamaTheme.spacing.cardPadding
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))
            Text(
                text = stringResource(R.string.fav_teams_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.normal))
            OutlinedButton(
                onClick = onManageFavorites,
                shape = RoundedCornerShape(UlamaTheme.shapes.buttonRestRadius)
            ) {
                Text(
                    text = stringResource(R.string.fav_teams_edit_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Tarjeta informativa mostrada cuando la vista virtual Mis Ligas no tiene competiciones seleccionadas.
 * Provee acceso directo para abrir la hoja modal de seleccion de torneos.
 *
 * @param onManageMyLeagues Callback ejecutado al presionar el boton de configuracion.
 * @param modifier Modificador base de Compose para el contenedor exterior.
 */
@Composable
fun EmptyMyLeaguesCard(
    onManageMyLeagues: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = UlamaTheme.shapes.container,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = UlamaTheme.spacing.emptyCardPaddingHorizontal,
                    vertical = UlamaTheme.spacing.cardPadding
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trophy),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))
            Text(
                text = stringResource(R.string.my_leagues_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.normal))
            OutlinedButton(
                onClick = onManageMyLeagues,
                shape = RoundedCornerShape(UlamaTheme.shapes.buttonRestRadius)
            ) {
                Text(
                    text = stringResource(R.string.my_leagues_empty_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyMatchesCardPreview() {
    UlamaTheme {
        EmptyMatchesCard(filter = TemporalFilter.TODAY)
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyFavoritesCardPreview() {
    UlamaTheme {
        EmptyFavoritesCard(onManageFavorites = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyMyLeaguesCardPreview() {
    UlamaTheme {
        EmptyMyLeaguesCard(onManageMyLeagues = {})
    }
}
