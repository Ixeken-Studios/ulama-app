package com.ixeken.ulama.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Hoja modal inferior no intrusiva que notifica al usuario cuando se detecta una nueva version disponible.
 * Se despliega al arrancar la app en lugar de un cuadro de dialogo rigido para permitir descarte organico
 * mediante deslizamiento tactil y facilitar la descarga directa desde GitHub.
 *
 * @param version Cadena que identifica la nueva version remota lista para instalar.
 * @param releaseUrl Enlace web oficial para acceder a la descarga de la publicacion.
 * @param releaseNotes Resumen opcional con los cambios o notas de version del repositorio.
 * @param onDismissRequest Accion ejecutada al cerrar o descartar la hoja modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableBottomSheet(
    version: String,
    releaseUrl: String,
    releaseNotes: String?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                // Cabecera con icono distintivo, titulo y version
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_system_update),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.sheet_update_available_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "v$version",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = UlamaTheme.spacing.small,
                                        vertical = UlamaTheme.spacing.extraSmall
                                    )
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.sheet_update_available_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.medium))

                // Notas de la version si se encuentran disponibles
                if (!releaseNotes.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.sheet_update_available_notes_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = UlamaTheme.spacing.extraSmall)
                    )

                    Surface(
                        shape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = UlamaTheme.borders.cardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(UlamaTheme.spacing.medium)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = releaseNotes.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.medium))
                }

                // Boton de accion principal: descargar actualizacion
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(releaseUrl)
                        } catch (_: Exception) {
                        }
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UlamaTheme.dimensions.minTouchTarget)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_system_update),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.sheet_update_available_download),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

                // Boton secundario: posponer aviso
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UlamaTheme.dimensions.minTouchTarget)
                ) {
                    Text(
                        text = stringResource(R.string.sheet_update_available_later),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
