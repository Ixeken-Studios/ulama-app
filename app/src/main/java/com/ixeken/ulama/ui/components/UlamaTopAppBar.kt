package com.ixeken.ulama.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.ui.theme.UlamaMotion
import com.ixeken.ulama.ui.theme.UlamaTheme
import kotlin.math.abs
import kotlinx.coroutines.launch

private const val MAX_VISIBLE_PAGER_DOTS = 7

/**
 * Barra superior principal de Ulama construida con Material 3 Expressive.
 * Alinea a la izquierda el acceso a configuracion y a la derecha la pildora selectora de competiciones
 * con soporte de deslizamiento horizontal directo y un micro paginado inferior para alternar entre
 * Mis equipos, Mis ligas y las competiciones guardadas en orden alfabetico.
 *
 * @param selectedLeague Liga activa actualmente seleccionada.
 * @param onLeaguePickerClick Accion disparada al pulsar la pildora para abrir el centro de competiciones.
 * @param onSettingsClick Accion disparada al pulsar el boton de ajustes.
 * @param quickLeagues Lista ordenada de vistas rapidas accesibles mediante gesto sobre la pildora.
 * @param onQuickLeagueSelected Accion disparada al completar un deslizamiento sobre la pildora con su direccion (-1 o +1).
 * @param showPagerIndicator Controla la visibilidad temporal del indicador de paginado debajo de la pildora.
 * @param onLeaguePillDragChange Notifica cuando inicia o termina un gesto horizontal sobre la pildora.
 * @param modifier Modificador base de Compose para el contenedor superior.
 */
@Composable
fun UlamaTopAppBar(
    selectedLeague: League,
    onLeaguePickerClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    quickLeagues: List<League> = emptyList(),
    onQuickLeagueSelected: (League, Int) -> Unit = { _, _ -> },
    showPagerIndicator: Boolean = true,
    onLeaguePillDragChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = UlamaTheme.isDark
    val motion = UlamaTheme.motion
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = UlamaTheme.spacing.normal)
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                var isDragging by remember { mutableStateOf(false) }
                var lastDirection by remember { mutableIntStateOf(0) }
                var hasTriggeredThreshold by remember { mutableStateOf(false) }
                val dragOffsetPx = remember { Animatable(0f) }

                val currentLeague by rememberUpdatedState(selectedLeague)
                val currentQuickLeagues by rememberUpdatedState(quickLeagues)
                val currentOnQuickSelected by rememberUpdatedState(onQuickLeagueSelected)
                val currentOnPillDragChange by rememberUpdatedState(onLeaguePillDragChange)

                val thresholdPx = with(density) { UlamaTheme.dimensions.pillSwipeThreshold.toPx() }
                val maxDragPx = with(density) { UlamaTheme.dimensions.pillMaxDragOffset.toPx() }

                val selectorShape = rememberAnimatedShape(
                    isPressed = isPressed || isDragging,
                    isSelected = false
                )

                val activeIndex = quickLeagues.indexOf(selectedLeague)
                val leagueName = stringResource(selectedLeague.nameRes)
                val pagerAccessibilityLabel = if (activeIndex >= 0 && quickLeagues.size > 1) {
                    stringResource(
                        R.string.cd_league_pager_position,
                        leagueName,
                        activeIndex + 1,
                        quickLeagues.size
                    )
                } else {
                    stringResource(R.string.cd_league_selector)
                }

                // Restablece la direccion tras completar una transicion para evitar direcciones obsoletas al volver desde CompetitionsScreen
                LaunchedEffect(selectedLeague) {
                    if (!isDragging && lastDirection != 0) {
                        lastDirection = 0
                    }
                }

                val draggableState = rememberDraggableState { delta ->
                    if (currentQuickLeagues.size <= 1) return@rememberDraggableState
                    val currentIdx = currentQuickLeagues.indexOf(currentLeague)
                    val proposedOffset = dragOffsetPx.value + delta
                    // Deslizar a la izquierda (-offset) avanza (+1); deslizar a la derecha (+offset) retrocede (-1)
                    val direction = if (proposedOffset < 0f) 1 else -1
                    val targetLeague = resolveTargetQuickLeague(
                        currentIndex = currentIdx,
                        direction = direction,
                        quickLeagues = currentQuickLeagues
                    )
                    // Aplicamos mayor resistencia cuando el usuario empuja contra un extremo bloqueado
                    val damping = when {
                        targetLeague == null -> 0.16f
                        abs(dragOffsetPx.value) >= thresholdPx -> 0.24f
                        else -> 0.52f
                    }
                    val nextOffset = (dragOffsetPx.value + delta * damping)
                        .coerceIn(-maxDragPx, maxDragPx)

                    coroutineScope.launch {
                        dragOffsetPx.snapTo(nextOffset)
                    }

                    val crossed = abs(nextOffset) >= thresholdPx && targetLeague != null
                    if (crossed && !hasTriggeredThreshold) {
                        hasTriggeredThreshold = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    } else if (!crossed && abs(nextOffset) < thresholdPx * 0.7f) {
                        hasTriggeredThreshold = false
                    }
                }

                Surface(
                    shape = selectorShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .semantics { contentDescription = pagerAccessibilityLabel }
                        .bounceClick(enabled = true, interactionSource = interactionSource)
                        .graphicsLayer {
                            val offset = dragOffsetPx.value
                            translationX = offset
                            if (motion.isExpressive && offset != 0f && maxDragPx > 0f) {
                                val dragRatio = (abs(offset) / maxDragPx).coerceIn(0f, 1f)
                                scaleX = 1f + dragRatio * 0.045f
                                scaleY = 1f - dragRatio * 0.04f
                            }
                        }
                        .clip(selectorShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = {
                                lastDirection = 0
                                onLeaguePickerClick()
                            }
                        )
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            enabled = quickLeagues.size > 1,
                            onDragStarted = {
                                isDragging = true
                                hasTriggeredThreshold = false
                                currentOnPillDragChange(true)
                            },
                            onDragStopped = { velocity ->
                                isDragging = false
                                currentOnPillDragChange(false)
                                val offset = dragOffsetPx.value
                                val velocityDirection = when {
                                    velocity <= -550f -> 1
                                    velocity >= 550f -> -1
                                    else -> 0
                                }
                                val dragDirection = when {
                                    offset <= -thresholdPx * 0.85f -> 1
                                    offset >= thresholdPx * 0.85f -> -1
                                    velocityDirection != 0 && abs(offset) > thresholdPx * 0.3f -> velocityDirection
                                    else -> 0
                                }
                                if (dragDirection != 0) {
                                    val currentIdx = currentQuickLeagues.indexOf(currentLeague)
                                    val targetLeague = resolveTargetQuickLeague(
                                        currentIndex = currentIdx,
                                        direction = dragDirection,
                                        quickLeagues = currentQuickLeagues
                                    )
                                    if (targetLeague != null && targetLeague != currentLeague) {
                                        if (!hasTriggeredThreshold) {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.GestureThresholdActivate
                                            )
                                        }
                                        lastDirection = dragDirection
                                        currentOnQuickSelected(targetLeague, dragDirection)
                                    }
                                }
                                hasTriggeredThreshold = false
                                coroutineScope.launch {
                                    dragOffsetPx.animateTo(
                                        targetValue = 0f,
                                        animationSpec = motion.spatialSpec()
                                    )
                                }
                            }
                        )
                ) {
                    AnimatedContent(
                        targetState = selectedLeague,
                        transitionSpec = {
                            val dir = lastDirection
                            val contentTransition: ContentTransform = if (dir != 0) {
                                (slideInHorizontally(animationSpec = motion.spatialSpec()) { fullWidth ->
                                    dir * (fullWidth / 3)
                                } + fadeIn(animationSpec = motion.effectsSpec())) togetherWith
                                    (slideOutHorizontally(animationSpec = motion.spatialSpec()) { fullWidth ->
                                        -dir * (fullWidth / 3)
                                    } + fadeOut(animationSpec = motion.effectsSpec()))
                            } else {
                                fadeIn(animationSpec = motion.effectsSpec()) togetherWith
                                    fadeOut(animationSpec = motion.effectsSpec())
                            }
                            contentTransition.using(
                                SizeTransform(clip = true) { _, _ -> motion.spatialSpec() }
                            )
                        },
                        label = "TopBarLeaguePillContent"
                    ) { league ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = UlamaTheme.spacing.selectorPaddingHorizontal,
                                vertical = UlamaTheme.spacing.selectorPaddingVertical
                            )
                        ) {
                            LeaguePillIcon(league = league, isDark = isDark)
                            Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                            Text(
                                text = stringResource(league.nameRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = quickLeagues.size > 1 && showPagerIndicator,
                    enter = fadeIn(animationSpec = motion.effectsSpec()) +
                        expandVertically(animationSpec = motion.spatialSpec()),
                    exit = fadeOut(animationSpec = motion.effectsSpec()) +
                        shrinkVertically(animationSpec = motion.spatialSpec())
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(UlamaTheme.spacing.pillIndicatorTopSpacing))
                        LeagueMiniPagerIndicator(
                            totalCount = quickLeagues.size,
                            activeIndex = activeIndex,
                            dragProgressProvider = {
                                if (maxDragPx > 0f) (-dragOffsetPx.value / maxDragPx).coerceIn(-0.45f, 0.45f)
                                else 0f
                            },
                            motion = motion
                        )
                    }
                }
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

@Composable
private fun LeaguePillIcon(
    league: League,
    isDark: Boolean
) {
    when (league) {
        League.FAVORITES -> {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = stringResource(league.nameRes),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
            )
        }
        League.MY_LEAGUES -> {
            Icon(
                painter = painterResource(R.drawable.ic_trophy),
                contentDescription = stringResource(league.nameRes),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
            )
        }
        else -> {
            AsyncImage(
                model = league.getLogoUrl(isDark),
                contentDescription = stringResource(
                    R.string.cd_league_logo,
                    stringResource(league.nameRes)
                ),
                modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
            )
        }
    }
}

/**
 * Indicador de paginado compacto dibujado en un unico nodo en fase de dibujado.
 * Evita recomposiciones durante el arrastre y acota la ventana visible si hay muchas ligas guardadas.
 */
@Composable
private fun LeagueMiniPagerIndicator(
    totalCount: Int,
    activeIndex: Int,
    dragProgressProvider: () -> Float,
    motion: UlamaMotion,
    modifier: Modifier = Modifier
) {
    val visibleCount = totalCount.coerceAtMost(MAX_VISIBLE_PAGER_DOTS)
    val windowStart = if (totalCount <= MAX_VISIBLE_PAGER_DOTS) {
        0
    } else {
        val anchor = if (activeIndex >= 0) activeIndex else 0
        (anchor - MAX_VISIBLE_PAGER_DOTS / 2).coerceIn(0, totalCount - MAX_VISIBLE_PAGER_DOTS)
    }
    val localActiveIndex = if (activeIndex >= 0) (activeIndex - windowStart).toFloat() else -1f

    val animatedLocalIndex by animateFloatAsState(
        targetValue = if (localActiveIndex >= 0f) localActiveIndex else 0f,
        animationSpec = motion.spatialSpec(),
        label = "LeaguePagerDotIndex"
    )
    val hasActiveSelection = localActiveIndex >= 0f

    val dotSize = UlamaTheme.dimensions.pagerDotSize
    val activeDotWidth = UlamaTheme.dimensions.pagerActiveDotWidth
    val dotGap = UlamaTheme.spacing.pagerDotGap
    val activeColor: Color = MaterialTheme.colorScheme.primary
    val inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)

    val totalWidth = if (hasActiveSelection) {
        activeDotWidth + dotSize * (visibleCount - 1) + dotGap * (visibleCount - 1)
    } else {
        dotSize * visibleCount + dotGap * (visibleCount - 1)
    }

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(dotSize)
            .drawBehind {
                val dotPx = dotSize.toPx()
                val activeWidthPx = activeDotWidth.toPx()
                val gapPx = dotGap.toPx()
                val dragShift = dragProgressProvider() * (dotPx + gapPx) * 0.45f
                val corner = CornerRadius(dotPx / 2f, dotPx / 2f)

                var cursorX = 0f
                for (i in 0 until visibleCount) {
                    val isEdgeClipped = (i == 0 && windowStart > 0) ||
                        (i == visibleCount - 1 && windowStart + visibleCount < totalCount)

                    val distanceFromActive = if (hasActiveSelection) {
                        abs(animatedLocalIndex - i)
                    } else {
                        1f
                    }
                    val activeFraction = (1f - distanceFromActive).coerceIn(0f, 1f)
                    val scale = if (isEdgeClipped && activeFraction < 0.5f) 0.65f else 1f

                    val widthPx = (dotPx + (activeWidthPx - dotPx) * activeFraction) * scale
                    val heightPx = dotPx * scale
                    val topY = (size.height - heightPx) / 2f
                    val drawX = (cursorX + dragShift * activeFraction)
                        .coerceIn(0f, (size.width - widthPx).coerceAtLeast(0f))

                    val dotColor = if (activeFraction > 0.05f) {
                        androidx.compose.ui.graphics.lerp(inactiveColor, activeColor, activeFraction)
                    } else {
                        inactiveColor
                    }

                    drawRoundRect(
                        color = dotColor,
                        topLeft = Offset(drawX, topY),
                        size = Size(widthPx, heightPx),
                        cornerRadius = corner
                    )
                    cursorX += widthPx + gapPx
                }
            }
    )
}

private fun resolveTargetQuickLeague(
    currentIndex: Int,
    direction: Int,
    quickLeagues: List<League>
): League? {
    if (quickLeagues.isEmpty() || direction == 0) return null
    val targetIndex = when {
        currentIndex == -1 && direction > 0 -> 0
        currentIndex == -1 && direction < 0 -> quickLeagues.lastIndex
        else -> currentIndex + direction
    }
    return quickLeagues.getOrNull(targetIndex)
}

@Preview(name = "UlamaTopAppBar - Dark Theme", showBackground = true)
@Composable
private fun UlamaTopAppBarDarkPreview() {
    UlamaTheme(darkTheme = true) {
        UlamaTopAppBar(
            selectedLeague = League.LIGA_MX,
            onLeaguePickerClick = {},
            onSettingsClick = {},
            quickLeagues = listOf(League.FAVORITES, League.MY_LEAGUES, League.LIGA_MX, League.PREMIER_LEAGUE)
        )
    }
}

@Preview(name = "UlamaTopAppBar - Light Theme", showBackground = true)
@Composable
private fun UlamaTopAppBarLightPreview() {
    UlamaTheme(darkTheme = false) {
        UlamaTopAppBar(
            selectedLeague = League.PREMIER_LEAGUE,
            onLeaguePickerClick = {},
            onSettingsClick = {},
            quickLeagues = listOf(League.FAVORITES, League.MY_LEAGUES, League.LIGA_MX, League.PREMIER_LEAGUE)
        )
    }
}
