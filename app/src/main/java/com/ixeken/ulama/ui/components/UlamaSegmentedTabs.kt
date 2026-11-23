package com.ixeken.ulama.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.ui.theme.UlamaTheme
import kotlinx.coroutines.launch

/**
 * Componente generico de pestanas segmentadas con estetica Material 3 Expressive.
 * Reutilizable tanto para filtros temporales principales como para selectores de modales.
 * Cada pestana aplica transicion de esquinas reactivas mediante rememberAnimatedShape,
 * respeta la regla Clip Before Click delimitando la ondulacion tactil y fondo al contorno,
 * y consume exclusivamente tokens semanticos de nivel 3 (primaryContainer / surfaceContainer).
 */
@Composable
fun <T> UlamaSegmentedTabs(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    iconRes: ((T) -> Int?)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.tabSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val animatedShape = rememberAnimatedShape(
                isPressed = isPressed,
                isSelected = isSelected,
                restRadius = UlamaTheme.shapes.tabRestRadius,
                morphedRadius = UlamaTheme.shapes.tabMorphedRadius
            )

            val motion = UlamaTheme.motion
            val spatialSpec = motion.spatialSpec<Float>()
            val isExpressive = motion.isExpressive
            val squashX = remember { Animatable(1f) }
            val squashY = remember { Animatable(1f) }

            LaunchedEffect(isSelected) {
                if (isExpressive && isSelected) {
                    val stretchFactor = 1.08f
                    val squashFactor = 0.92f
                    squashX.snapTo(stretchFactor)
                    squashY.snapTo(squashFactor)
                    launch { squashX.animateTo(1f, spatialSpec) }
                    launch { squashY.animateTo(1f, spatialSpec) }
                }
            }

            val containerColorState = animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                label = "SegmentedTabContainerColor"
            )

            val contentColorState = animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "SegmentedTabContentColor"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = UlamaTheme.dimensions.minTouchTarget)
                    .bounceClick(
                        enabled = true,
                        interactionSource = interactionSource,
                        motion = motion
                    )
                    .graphicsLayer {
                        if (isExpressive) {
                            scaleX = squashX.value
                            scaleY = squashY.value
                        }
                    }
                    .clip(animatedShape)
                    // Lectura diferida a fase de dibujado para evitar recomposiciones durante la transicion de color
                    .drawBehind { drawRect(containerColorState.value) }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = { onItemSelected(item) }
                    )
                    .padding(vertical = UlamaTheme.spacing.tabPaddingVertical)
            ) {
                val itemIconRes = iconRes?.invoke(item)
                if (itemIconRes != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = UlamaTheme.spacing.extraSmall)
                    ) {
                        Icon(
                            painter = painterResource(itemIconRes),
                            contentDescription = null,
                            tint = { contentColorState.value },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label(item),
                            color = { contentColorState.value },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = label(item),
                        color = { contentColorState.value },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
