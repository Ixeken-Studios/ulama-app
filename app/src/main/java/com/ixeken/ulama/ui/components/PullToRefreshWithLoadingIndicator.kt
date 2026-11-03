package com.ixeken.ulama.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import com.ixeken.ulama.ui.theme.UlamaTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Contenedor de actualizacion por deslizamiento hacia abajo Pull-to-Refresh
 * configurado con las especificaciones de Material 3 Expressive.
 *
 * Encapsula la orquestacion de gestos mediante PullToRefreshBox, suministra
 * el indicador morfologico con animacion continua LoadingIndicator, desplaza
 * el contenido verticalmente para evitar solapamientos visuales y ejecuta
 * retroalimentacion haptica tanto al cruzar el umbral de disparo como al confirmar la recarga.
 *
 * @param isRefreshing Indica si el refresco de datos se encuentra activo en segundo plano.
 * @param onRefresh Accion ejecutada cuando el usuario completa el recorrido del gesto de arrastre.
 * @param modifier Modificador visual y de diseno aplicado sobre el contenedor exterior.
 * @param state Estado reactivo del progreso de arrastre y rebote elastico del indicador.
 * @param containerColor Color de fondo de la capsula del indicador de carga.
 * @param indicatorColor Color semantico aplicado sobre la geometria morfologica interior.
 * @param displacementThreshold Distancia maxima de desplazamiento vertical para la lista.
 * @param content Contenido desplazable envuelto por la deteccion de gestos anidados.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PullToRefreshWithLoadingIndicator(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    displacementThreshold: Dp = UlamaTheme.dimensions.pullToRefreshThreshold,
    content: @Composable BoxScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    LaunchedEffect(state) {
        var hasTriggeredThreshold = false
        snapshotFlow { state.distanceFraction >= 1f }
            .distinctUntilChanged()
            .collect { crossed ->
                if (crossed && !hasTriggeredThreshold && !isRefreshing) {
                    hasTriggeredThreshold = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                } else if (!crossed) {
                    hasTriggeredThreshold = false
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = isRefreshing,
                containerColor = containerColor,
                color = indicatorColor,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = state.distanceFraction * displacementThreshold.toPx()
                }
        ) {
            content()
        }
    }
}
