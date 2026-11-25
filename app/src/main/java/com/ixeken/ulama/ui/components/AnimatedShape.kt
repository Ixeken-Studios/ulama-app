package com.ixeken.ulama.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.ixeken.ulama.ui.theme.LocalUlamaMotion
import com.ixeken.ulama.ui.theme.UlamaMotion
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Provee una forma dinamica que transiciona suavemente de rectangular a circular.
 * Delega la fisica de animacion a la clase desacoplada UlamaMotion, evitando logica monolitica.
 *
 * @param isPressed Indica si el usuario mantiene pulsado el elemento tactil.
 * @param isSelected Indica si el elemento representa un estado activo o seleccionado.
 * @param restRadius Radio en reposo para un aspecto rectangular (12dp).
 * @param morphedRadius Radio comprimido para un aspecto circular (36dp).
 * @param motion Instancia de UlamaMotion a consultar, consumiendo por defecto el proveedor del tema.
 */
@Composable
fun rememberAnimatedShape(
    isPressed: Boolean = false,
    isSelected: Boolean = false,
    restRadius: Dp = UlamaTheme.shapes.buttonRestRadius,
    morphedRadius: Dp = UlamaTheme.shapes.buttonSelectedRadius,
    motion: UlamaMotion = LocalUlamaMotion.current
): Shape {
    val targetRadius = if (isPressed || isSelected) morphedRadius else restRadius
    val animatedRadius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = motion.spatialSpec(),
        label = "AnimatedShapeCorner"
    )
    return RoundedCornerShape(animatedRadius)
}

/**
 * Provee una forma segmentada con radios independientes por esquina que anima su metamorfosis
 * leyendo una unica interpolacion escalar en fase de dibujado para evitar recomposiciones por cuadro.
 */
@Composable
fun rememberMorphingSegmentShape(
    isMorphed: Boolean,
    topStartRest: Dp,
    topEndRest: Dp,
    bottomEndRest: Dp,
    bottomStartRest: Dp,
    topStartMorphed: Dp,
    topEndMorphed: Dp = topStartMorphed,
    bottomEndMorphed: Dp = topStartMorphed,
    bottomStartMorphed: Dp = topStartMorphed,
    motion: UlamaMotion = LocalUlamaMotion.current
): Shape {
    val progressState: State<Float> = animateFloatAsState(
        targetValue = if (isMorphed) 1f else 0f,
        animationSpec = motion.spatialSpec(),
        label = "MorphingSegmentProgress"
    )
    return remember(
        topStartRest,
        topEndRest,
        bottomEndRest,
        bottomStartRest,
        topStartMorphed,
        topEndMorphed,
        bottomEndMorphed,
        bottomStartMorphed,
        progressState
    ) {
        MorphingSegmentShape(
            progress = { progressState.value },
            topStartRest = topStartRest,
            topEndRest = topEndRest,
            bottomEndRest = bottomEndRest,
            bottomStartRest = bottomStartRest,
            topStartMorphed = topStartMorphed,
            topEndMorphed = topEndMorphed,
            bottomEndMorphed = bottomEndMorphed,
            bottomStartMorphed = bottomStartMorphed
        )
    }
}

private class MorphingSegmentShape(
    private val progress: () -> Float,
    private val topStartRest: Dp,
    private val topEndRest: Dp,
    private val bottomEndRest: Dp,
    private val bottomStartRest: Dp,
    private val topStartMorphed: Dp,
    private val topEndMorphed: Dp,
    private val bottomEndMorphed: Dp,
    private val bottomStartMorphed: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val t = progress()
        // El rebote elastico puede sobrepasar 0f..1f, por lo que se acotan radios negativos
        return RoundedCornerShape(
            topStart = lerp(topStartRest, topStartMorphed, t).coerceAtLeast(0.dp),
            topEnd = lerp(topEndRest, topEndMorphed, t).coerceAtLeast(0.dp),
            bottomEnd = lerp(bottomEndRest, bottomEndMorphed, t).coerceAtLeast(0.dp),
            bottomStart = lerp(bottomStartRest, bottomStartMorphed, t).coerceAtLeast(0.dp)
        ).createOutline(size, layoutDirection, density)
    }
}
