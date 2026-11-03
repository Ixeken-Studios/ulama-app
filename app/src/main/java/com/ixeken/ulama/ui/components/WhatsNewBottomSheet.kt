package com.ixeken.ulama.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Hoja modal inferior de bienvenida post-actualizacion inspirada en Material 3 Expressive.
 * Se despliega una sola vez tras instalar o migrar de version para dar a conocer las caracteristicas
 * principales del sistema estructuradas mediante una lista segmentada conectada.
 *
 * @param onDismissRequest Accion para cerrar la hoja y registrar la version como consumida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val topConnectedShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardRestRadius,
        topEnd = UlamaTheme.shapes.cardRestRadius,
        bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
    )
    val middleConnectedShape = RoundedCornerShape(UlamaTheme.shapes.cardConnectedInnerRadius)
    val bottomConnectedShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomStart = UlamaTheme.shapes.cardRestRadius,
        bottomEnd = UlamaTheme.shapes.cardRestRadius
    )

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
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera con distintivo circular e informacion de la version
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_soccer_ball),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.sheet_whats_new_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.sheet_whats_new_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.large))

                // Lista segmentada conectada con las novedades de la version 2.0
                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WhatsNewFeatureCard(
                        title = stringResource(R.string.settings_about_changelog_feat1_title),
                        description = stringResource(R.string.settings_about_changelog_feat1_desc),
                        icon = painterResource(R.drawable.ic_soccer_ball),
                        shape = topConnectedShape
                    )

                    WhatsNewFeatureCard(
                        title = stringResource(R.string.settings_about_changelog_feat2_title),
                        description = stringResource(R.string.settings_about_changelog_feat2_desc),
                        icon = painterResource(R.drawable.ic_palette),
                        shape = middleConnectedShape
                    )

                    WhatsNewFeatureCard(
                        title = stringResource(R.string.settings_about_changelog_feat3_title),
                        description = stringResource(R.string.settings_about_changelog_feat3_desc),
                        icon = painterResource(R.drawable.ic_notifications_active),
                        shape = bottomConnectedShape
                    )
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.large))

                // Boton principal de cierre para iniciar exploracion
                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UlamaTheme.dimensions.minTouchTarget)
                ) {
                    Text(
                        text = stringResource(R.string.sheet_whats_new_action_explore),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual para presentar un pilar de funcionalidad dentro de la lista segmentada de novedades.
 */
@Composable
private fun WhatsNewFeatureCard(
    title: String,
    description: String,
    icon: Painter,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(UlamaTheme.spacing.medium)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
