package com.ixeken.ulama.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ixeken.ulama.ui.theme.UlamaMotion
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Modificador centralizado que aplica respuesta tactil elastica sobre elementos interactivos al presionarse.
 *
 * Implementa el principio de conmutacion Expressive frente a Standard:
 * - En modo Expressive: Aplica deformacion elastica de squash and stretch con compresion vertical (0.93f)
 *   y ensanchamiento horizontal (1.05f) que rebota organicamente con fisica de resortes al soltarse.
 * - En modo Standard: Aplica una contraccion simetrica uniforme (0.96f) con retorno controlado sin sobreoscilacion.
 *
 * Aplica Zero-Logic Gating retornando de inmediato si enabled es falso.
 * La aplicacion de la escala interactiva se ejecuta exclusivamente en la fase de dibujado mediante [Modifier.graphicsLayer],
 * asegurando 60 cuadros por segundo constantes sin disparar recomposiciones en el arbol de Compose.
 *
 * @param enabled Determina si la respuesta tactil elastica esta activa.
 * @param interactionSource Fuente de interacciones vinculada al gesto tactil del componente contenedor.
 * @param motion Configuracion de movimiento activa, enlazada por defecto a [UlamaTheme.motion].
 */
@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    motion: UlamaMotion = UlamaTheme.motion
): Modifier {
    if (!enabled) return this

    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val (targetScaleX, targetScaleY) = when {
        !isPressed -> 1.0f to 1.0f
        motion.isExpressive -> 1.05f to 0.93f
        else -> 0.96f to 0.96f
    }

    val animatedScaleX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = motion.spatialSpec(),
        label = "InteractiveScaleX"
    )
    val animatedScaleY by animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = motion.spatialSpec(),
        label = "InteractiveScaleY"
    )

    return this.graphicsLayer {
        scaleX = animatedScaleX
        scaleY = animatedScaleY
    }
}
