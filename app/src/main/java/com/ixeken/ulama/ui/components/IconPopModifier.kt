package com.ixeken.ulama.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.ixeken.ulama.ui.theme.UlamaMotion
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Modificador centralizado que aplica un rebote elastico sobre iconos al conmutar su estado de seleccion.
 *
 * Se utiliza para confirmar de forma reactiva interacciones clave como seguir partidos o marcar equipos favoritos.
 * Omite la ejecucion durante la composicion inicial por defecto para evitar saltos visuales al abrir pantallas,
 * pero permite habilitar animateOnAttach para insignias o emblemas que entran dinamicamente al seleccionarse.
 * La aplicacion de la escala se difiere exclusivamente a la fase de dibujado en [Modifier.graphicsLayer].
 *
 * @param trigger Clave o valor booleano cuyo cambio dispara la microinteraccion de rebote.
 * @param animateOnAttach Si es verdadero, ejecuta el rebote elastico inmediatamente al entrar en composicion.
 * @param motion Configuracion de movimiento activa, enlazada por defecto a [UlamaTheme.motion].
 */
@Composable
fun Modifier.iconPop(
    trigger: Any?,
    animateOnAttach: Boolean = false,
    motion: UlamaMotion = UlamaTheme.motion
): Modifier {
    val initialScale = if (motion.isExpressive) 0.72f else 0.86f
    val scale = remember { Animatable(if (animateOnAttach) initialScale else 1f) }
    var isInitial by remember { mutableStateOf(true) }

    LaunchedEffect(trigger) {
        if (isInitial) {
            isInitial = false
            if (!animateOnAttach) return@LaunchedEffect
        }
        scale.snapTo(initialScale)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = motion.spatialSpec()
        )
    }

    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
