package com.ixeken.ulama.ui.competitions

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.LeagueRegion
import com.ixeken.ulama.ui.components.bounceClick
import com.ixeken.ulama.ui.theme.UlamaTheme
import kotlin.math.roundToInt

/**
 * Mapa mundial vectorial interactivo flotando directamente sobre el fondo de superficie
 * sin tarjetas contenedoras ni bordes. Renderiza rutas SVG simplificadas en cinco bloques
 * continentales con transiciones elasticas y hit-testing preciso sobre cada masa terrestre.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorldContinentMap(
    selectedRegion: LeagueRegion,
    onRegionToggle: (LeagueRegion) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val motion = UlamaTheme.motion
    val colorAnimSpec = remember(motion) { motion.effectsSpec<androidx.compose.ui.graphics.Color>() }
    val parsedContinents = remember { WorldMapPaths.parseContinents() }

    val continentColors = parsedContinents.map { parsed ->
        val region = parsed.spec.region
        val isSelected = selectedRegion == region
        val isDimmed = selectedRegion != LeagueRegion.ALL && !isSelected
        val targetColor = when {
            isSelected -> colorScheme.primaryContainer
            isDimmed -> colorScheme.surfaceContainerHighest.copy(alpha = 0.42f)
            else -> colorScheme.surfaceContainerHighest
        }
        animateColorAsState(
            targetValue = targetColor,
            animationSpec = colorAnimSpec,
            label = "continentColor_${region.name}"
        )
    }

    val isGlobalSelected = selectedRegion == LeagueRegion.GLOBAL

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = UlamaTheme.spacing.extraSmall)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
                .fillMaxWidth()
                .heightIn(max = UlamaTheme.dimensions.worldMapHeight)
                .aspectRatio(
                    ratio = WorldMapPaths.VIEWBOX_WIDTH / WorldMapPaths.VIEWBOX_HEIGHT,
                    matchHeightConstraintsFirst = false
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(parsedContinents) {
                        detectTapGestures { tapOffset ->
                            val sx = WorldMapPaths.VIEWBOX_WIDTH / size.width.toFloat()
                            val sy = WorldMapPaths.VIEWBOX_HEIGHT / size.height.toFloat()
                            val viewBoxPoint = Offset(tapOffset.x * sx, tapOffset.y * sy)
                            WorldMapPaths.findRegionAt(parsedContinents, viewBoxPoint)?.let(onRegionToggle)
                        }
                    }
            ) {
                scale(
                    scaleX = size.width / WorldMapPaths.VIEWBOX_WIDTH,
                    scaleY = size.height / WorldMapPaths.VIEWBOX_HEIGHT,
                    pivot = Offset.Zero
                ) {
                    parsedContinents.forEachIndexed { index, parsed ->
                        drawPath(
                            path = parsed.path,
                            color = continentColors[index].value
                        )
                    }
                }
            }

            // Etiquetas compactas centradas en cada continente con area tactil minima de 48dp.
            parsedContinents.forEach { parsed ->
                val spec = parsed.spec
                val isSelected = selectedRegion == spec.region
                val regionLabel = stringResource(spec.region.labelRes)
                Layout(
                    content = {
                        Surface(
                            shape = UlamaTheme.shapes.chip,
                            color = if (isSelected) {
                                colorScheme.primary
                            } else {
                                colorScheme.surface.copy(alpha = 0.66f)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(UlamaTheme.shapes.chip)
                                .clickable(
                                    onClick = { onRegionToggle(spec.region) }
                                )
                                .semantics {
                                    role = Role.Button
                                    selected = isSelected
                                    contentDescription = regionLabel
                                }
                        ) {
                            Text(
                                text = regionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(
                                    horizontal = UlamaTheme.spacing.small,
                                    vertical = UlamaTheme.spacing.none
                                )
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                    val centerX = (constraints.maxWidth * spec.labelFraction.x).roundToInt()
                    val centerY = (constraints.maxHeight * spec.labelFraction.y).roundToInt()
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(
                            x = (centerX - placeable.width / 2).coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0)),
                            y = (centerY - placeable.height / 2).coerceIn(0, (constraints.maxHeight - placeable.height).coerceAtLeast(0))
                        )
                    }
                }
            }

            // Boton Global destacado en esquina inferior izquierda sobre espacio oceanico libre.
            val globalInteraction = remember { MutableInteractionSource() }
            Surface(
                shape = UlamaTheme.shapes.chip,
                color = if (isGlobalSelected) colorScheme.primary else colorScheme.secondaryContainer,
                contentColor = if (isGlobalSelected) colorScheme.onPrimary else colorScheme.onSecondaryContainer,
                tonalElevation = if (isGlobalSelected) UlamaTheme.spacing.none else UlamaTheme.spacing.extraSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = UlamaTheme.spacing.extraSmall, bottom = UlamaTheme.spacing.extraSmall)
                    .bounceClick(enabled = true, interactionSource = globalInteraction)
                    .clip(UlamaTheme.shapes.chip)
                    .clickable(
                        interactionSource = globalInteraction,
                        indication = ripple(),
                        onClick = { onRegionToggle(LeagueRegion.GLOBAL) }
                    )
                    .semantics {
                        role = Role.Button
                        selected = isGlobalSelected
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.extraSmall),
                    modifier = Modifier.padding(
                        horizontal = UlamaTheme.spacing.medium,
                        vertical = UlamaTheme.spacing.small
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_public),
                        contentDescription = null,
                        modifier = Modifier.size(UlamaTheme.dimensions.iconSmall)
                    )
                    Text(
                        text = stringResource(R.string.region_global),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
