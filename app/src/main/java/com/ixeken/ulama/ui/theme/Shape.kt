package com.ixeken.ulama.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nivel 1: Tokens primitivos de radios de esquina para la geometria del diseno.
 * Valores fijos e inmutables que representan las magnitudes base.
 */
val RadiusNone: Dp = 0.dp
val RadiusMicro: Dp = 2.dp
val RadiusExtraSmall: Dp = 4.dp
val RadiusSmall: Dp = 8.dp
val RadiusMediumSmall: Dp = 12.dp
val RadiusMedium: Dp = 16.dp
val RadiusLarge: Dp = 20.dp
val RadiusExtraLarge: Dp = 28.dp
val RadiusSuperLarge: Dp = 36.dp
val RadiusPill: Dp = 999.dp

/**
 * Nivel 2: Mapeo de tokens de formas oficiales de Material Design 3.
 */
val UlamaMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusExtraSmall),
    small = RoundedCornerShape(RadiusSmall),
    medium = RoundedCornerShape(RadiusMedium),
    large = RoundedCornerShape(RadiusLarge),
    extraLarge = RoundedCornerShape(RadiusExtraLarge)
)

/**
 * Nivel 2: Tokens semanticos de formas y radios para Ulama.
 * Proveen abstracciones contextuales (tarjeta, contenedor, selector, pildora)
 * eliminando valores literales en los composables.
 */
@Immutable
data class UlamaShapes(
    val card: Shape = RoundedCornerShape(RadiusExtraLarge),
    val cardPressed: Shape = RoundedCornerShape(RadiusMedium),
    val container: Shape = RoundedCornerShape(RadiusLarge),
    val disciplineCard: Shape = RoundedCornerShape(RadiusMicro),
    val chip: Shape = RoundedCornerShape(RadiusPill),
    val dropdown: Shape = RoundedCornerShape(RadiusMedium),
    val bottomSheet: Shape = RoundedCornerShape(
        topStart = RadiusExtraLarge,
        topEnd = RadiusExtraLarge,
        bottomStart = RadiusNone,
        bottomEnd = RadiusNone
    ),
    // Radios escalares para transiciones y animaciones reactivas
    val cardRestRadius: Dp = RadiusExtraLarge,
    val cardPressedRadius: Dp = RadiusMedium,
    val tabRestRadius: Dp = RadiusMediumSmall,
    val tabMorphedRadius: Dp = RadiusSuperLarge,
    val buttonRestRadius: Dp = RadiusMediumSmall,
    val buttonSelectedRadius: Dp = RadiusSuperLarge,
    val cardConnectedInnerRadius: Dp = RadiusExtraSmall
)

val LocalUlamaShapes = staticCompositionLocalOf { UlamaShapes() }
