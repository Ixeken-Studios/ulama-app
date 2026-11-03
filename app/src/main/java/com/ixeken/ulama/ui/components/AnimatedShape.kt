package com.ixeken.ulama.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
