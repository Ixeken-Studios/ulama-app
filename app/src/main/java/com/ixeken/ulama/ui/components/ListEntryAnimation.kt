package com.ixeken.ulama.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.ixeken.ulama.ui.theme.UlamaMotion
import com.ixeken.ulama.ui.theme.UlamaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modificador centralizado que aplica microinteracciones de entrada escalonada a listas y tarjetas segmentadas.
 *
 * Se aisla la logica en este componente reutilizable para cumplir con el principio de responsabilidad unica y
 * evitar clases monoliticas. La lectura de los valores animados se delega estrictamente a la fase de dibujado
 * dentro de [Modifier.graphicsLayer], garantizando que no se disparen recomposiciones innecesarias en los elementos hijos.
 *
 * Se separan la traslacion espacial y la opacidad en canales independientes conforme a Material 3 Expressive:
 * la posicion consume resortes espaciales con soporte de rebote organico en modo Expressive, y la opacidad emplea
 * resortes de efectos con amortiguamiento critico para eliminar destellos visuales.
 *
 * @param index Posicion ordinal del elemento dentro del contenedor para orquestar la cascada visual.
 * @param key Identificador unico o clave de estado para reiniciar la animacion unicamente cuando cambie el elemento.
 * @param motion Configuracion de movimiento a consultar, enlazada por defecto a [UlamaTheme.motion].
 */
@Composable
fun Modifier.animateListEntry(
    index: Int,
    key: Any? = Unit,
    motion: UlamaMotion = UlamaTheme.motion
): Modifier {
    val translationProgress = remember(key) { Animatable(0f) }
    val alphaProgress = remember(key) { Animatable(0f) }
    val density = LocalDensity.current
    val startOffsetYPx = with(density) { motion.listEntryOffsetY.toPx() }
    val delayMs = remember(index, motion.mode) { motion.listEntryStaggerDelay(index) }

    LaunchedEffect(key) {
        if (delayMs > 0) {
            delay(delayMs)
        }
        launch {
            alphaProgress.animateTo(
                targetValue = 1f,
                animationSpec = motion.effectsSpec()
            )
        }
        launch {
            translationProgress.animateTo(
                targetValue = 1f,
                animationSpec = motion.spatialSpec()
            )
        }
    }

    return this.graphicsLayer {
        alpha = alphaProgress.value.coerceIn(0f, 1f)
        translationY = (1f - translationProgress.value) * startOffsetYPx
    }
}
