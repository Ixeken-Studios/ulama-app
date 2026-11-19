package com.ixeken.ulama.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * Grupo de botones conectados oficial de Material 3 Expressive para seleccion unica.
 * Reemplaza selectores ad-hoc con ToggleButtons conectados empleando formas asimetricas
 * adaptativas (connectedLeadingButtonShapes, connectedMiddleButtonShapes y connectedTrailingButtonShapes)
 * y separacion estandarizada ConnectedSpaceBetween.
 *
 * @param options Coleccion inmutable de opciones disponibles.
 * @param selectedOption Opcion actualmente seleccionada.
 * @param onOptionSelected Callback invocado al conmutar una opcion.
 * @param labelProvider Funcion que resuelve la cadena de texto de la opcion.
 * @param modifier Modificador base para la fila contenedora.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ExpressiveConnectedButtonGroup(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selectedOption
            val interactionSource = remember { MutableInteractionSource() }
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onOptionSelected(option) },
                shapes = shapes,
                interactionSource = interactionSource,
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .bounceClick(enabled = true, interactionSource = interactionSource)
            ) {
                Text(
                    text = labelProvider(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
