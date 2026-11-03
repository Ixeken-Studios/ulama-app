package com.ixeken.ulama.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Representa los dos modos de dinamismo soportados en la aplicacion.
 */
enum class MotionMode {
    EXPRESSIVE,
    STANDARD
}

/**
 * Clase dedicada para centralizar las especificaciones de movimiento de Ulama.
 * Aislada del tema visual para evitar clases monoliticas y reutilizar especificaciones de fisica
 * en cualquier componente sin duplicar declaraciones de resortes.
 *
 * @property mode Modo de movimiento activo.
 */
@Immutable
class UlamaMotion(
    val mode: MotionMode = MotionMode.EXPRESSIVE
) {
    val isExpressive: Boolean get() = mode == MotionMode.EXPRESSIVE

    /**
     * Especificacion para transformaciones espaciales como cambios de forma, posicion o tamano.
     * Expressive usa resortes reactivos con rebote; Standard usa resortes rigidos sin rebote.
     */
    fun <T> spatialSpec(): FiniteAnimationSpec<T> {
        return if (isExpressive) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    }

    /**
     * Especificacion para cambios de efectos visuales como opacidad o color.
     */
    fun <T> effectsSpec(): FiniteAnimationSpec<T> {
        return if (isExpressive) {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        }
    }

    /**
     * Desplazamiento inicial en el eje Y para la entrada de elementos en listas y tarjetas.
     * En Expressive se asigna mayor recorrido para permitir que el resorte desarrolle su rebote caracteristico,
     * mientras que en Standard se acota la distancia para priorizar una transicion rapida y directa.
     */
    val listEntryOffsetY: Dp
        get() = if (isExpressive) 24.dp else 14.dp

    /**
     * Retardo escalonado para la aparicion en cascada de elementos segun su indice en la lista.
     * Expressive emplea un intervalo mas pausado para enriquecer la fluidez visual, y Standard un paso
     * compacto para concluir la secuencia en menos tiempo. Ambos limitan el retardo maximo para evitar
     * que elementos en posiciones inferiores demoren en presentarse.
     *
     * @param index Posicion ordinal del elemento en la lista.
     * @param maxDelayMs Cota maxima de tiempo de espera antes de animar.
     */
    fun listEntryStaggerDelay(index: Int, maxDelayMs: Long = if (isExpressive) 140L else 80L): Long {
        val step = if (isExpressive) 35L else 20L
        return (index * step).coerceAtMost(maxDelayMs)
    }

    companion object {
        val Expressive = UlamaMotion(MotionMode.EXPRESSIVE)
        val Standard = UlamaMotion(MotionMode.STANDARD)
    }
}

/**
 * Proveedor de composicion para acceder a la instancia de UlamaMotion activa en el arbol de Compose.
 */
val LocalUlamaMotion = staticCompositionLocalOf { UlamaMotion.Expressive }
