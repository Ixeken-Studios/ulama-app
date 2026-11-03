package com.ixeken.ulama.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nivel 1: Tokens primitivos de espaciado y margenes de la reticula visual.
 * Escala fija base para dimensiones espaciales.
 */
val SpaceNone: Dp = 0.dp
val SpaceExtraSmall: Dp = 4.dp
val SpaceSmall: Dp = 8.dp
val SpaceMedium: Dp = 12.dp
val SpaceNormal: Dp = 16.dp
val SpaceLarge: Dp = 20.dp
val SpaceExtraLarge: Dp = 24.dp
val SpaceHuge: Dp = 32.dp
val SpaceConnectedGap: Dp = 3.dp

/**
 * Nivel 2: Tokens semanticos de espaciado para componentes y vistas.
 * Conectan la reticula de diseno con la intencion de uso en pantallas compactas y expandidas.
 */
@Immutable
data class UlamaSpacing(
    // Escala base
    val none: Dp = SpaceNone,
    val extraSmall: Dp = SpaceExtraSmall,
    val small: Dp = SpaceSmall,
    val medium: Dp = SpaceMedium,
    val normal: Dp = SpaceNormal,
    val large: Dp = SpaceLarge,
    val extraLarge: Dp = SpaceExtraLarge,
    val huge: Dp = SpaceHuge,

    // Roles semanticos especificos para pantallas y contenedores
    val screenHorizontalCompact: Dp = SpaceNormal,
    val screenHorizontalExpanded: Dp = SpaceExtraLarge,
    val screenVertical: Dp = SpaceSmall,
    val contentSpacing: Dp = SpaceMedium,
    val listSpacing: Dp = SpaceMedium,
    val listTopPadding: Dp = SpaceExtraSmall,
    val listBottomPadding: Dp = SpaceExtraLarge,

    // Roles semanticos de tarjeta
    val cardPadding: Dp = SpaceNormal,
    val cardInnerSpacing: Dp = SpaceSmall,
    val cardBottomRowSpacing: Dp = SpaceNormal,

    // Roles semanticos para la tabla de posiciones
    val tablePaddingHorizontal: Dp = SpaceMedium,
    val tablePaddingVertical: Dp = SpaceNormal,
    val tableHeaderMarginBottom: Dp = SpaceMedium,
    val tableHeaderPaddingVertical: Dp = SpaceExtraSmall,
    val tableRowPaddingVertical: Dp = SpaceSmall,
    val tableRowPaddingHorizontal: Dp = SpaceExtraSmall,
    val tableDividerPaddingVertical: Dp = SpaceExtraSmall,

    // Roles semanticos para pestanas y selectores
    val tabSpacing: Dp = SpaceSmall,
    val tabPaddingVertical: Dp = SpaceSmall,
    val tabContentPaddingHorizontal: Dp = SpaceNormal,
    val selectorPaddingHorizontal: Dp = SpaceNormal,
    val selectorPaddingVertical: Dp = SpaceSmall,
    val emptyCardPaddingVertical: Dp = SpaceExtraLarge,
    val emptyCardPaddingHorizontal: Dp = SpaceLarge,

    // Separacion entre tarjetas agrupadas conectadas
    val connectedCardSpacing: Dp = SpaceConnectedGap
)

val LocalUlamaSpacing = staticCompositionLocalOf { UlamaSpacing() }
