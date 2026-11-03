package com.ixeken.ulama.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ixeken.ulama.ui.theme.UlamaMotion
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Modificador centralizado que aplica un efecto de latido respiratorio continuo sobre indicadores en vivo.
 *
 * Aplica el principio de Zero-Logic Gating retornando de inmediato cuando el partido no se encuentra activo.
 * La lectura de los valores oscilantes se confina a la fase de dibujado dentro de [Modifier.graphicsLayer],
 * asegurando 60 cuadros por segundo constantes sin disparar recomposiciones en el arbol de Compose.
 *
 * En modo Expressive modula simultaneamente escala y opacidad con dinamismo organico, mientras que en modo
 * Standard restringe la animacion a una oscilacion sutil de opacidad sin deformacion espacial.
 *
 * @param isLive Indica si el evento deportivo se encuentra disputandose en tiempo real.
 * @param motion Configuracion de movimiento activa, enlazada por defecto a [UlamaTheme.motion].
 */
@Composable
fun Modifier.livePulse(
    isLive: Boolean,
    motion: UlamaMotion = UlamaTheme.motion
): Modifier {
    if (!isLive) return this

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulseTransition")
    val isExpressive = motion.isExpressive

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isExpressive) 0.96f else 1.0f,
        targetValue = if (isExpressive) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LivePulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isExpressive) 0.70f else 0.80f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LivePulseAlpha"
    )

    return this.graphicsLayer {
        scaleX = pulseScale
        scaleY = pulseScale
        alpha = pulseAlpha
    }
}
