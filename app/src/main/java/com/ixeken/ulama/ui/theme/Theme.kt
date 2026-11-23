package com.ixeken.ulama.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.core.model.ColorPalette

/**
 * Nivel 2: Tokens semanticos para el esquema de color oscuro.
 * Mapea exclusivamente tokens primitivos de Color.kt sin literales de color locales.
 */
private val UlamaDarkColorScheme = darkColorScheme(
    primary = LightGreen,
    onPrimary = DeepForestGreen,
    primaryContainer = Green,
    onPrimaryContainer = CrispWhite,
    inversePrimary = DarkGreen,
    secondary = LightGreen,
    onSecondary = DeepForestGreen,
    secondaryContainer = DeepSageContainer,
    onSecondaryContainer = LightSageText,
    tertiary = PaleOlive,
    onTertiary = DeepOliveContainer,
    tertiaryContainer = DeepOliveContainer,
    onTertiaryContainer = LightOliveText,
    background = DarkBackground,
    onBackground = LightVanilla,
    surface = DarkBackground,
    onSurface = LightVanilla,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = LightGreen,
    inverseSurface = VanillaWhite,
    inverseOnSurface = CharcoalBlack,
    error = LightRed,
    onError = DeepRed,
    errorContainer = DarkRedContainer,
    onErrorContainer = PaleRedContainer,
    outline = Green,
    outlineVariant = DarkGreenBorder,
    scrim = ScrimBlack,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest
)

/**
 * Nivel 2: Tokens semanticos para el esquema de color claro.
 * Mapea exclusivamente tokens primitivos de Color.kt sin literales de color locales.
 */
private val UlamaLightColorScheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = CrispWhite,
    primaryContainer = DarkGreen,
    onPrimaryContainer = CrispWhite,
    inversePrimary = LightGreen,
    secondary = Green,
    onSecondary = CrispWhite,
    secondaryContainer = PaleSageContainer,
    onSecondaryContainer = DarkSageText,
    tertiary = WarmOlive,
    onTertiary = CrispWhite,
    tertiaryContainer = PaleOliveContainer,
    onTertiaryContainer = DarkOliveText,
    background = VanillaWhite,
    onBackground = CharcoalBlack,
    surface = VanillaWhite,
    onSurface = CharcoalBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = DarkGreen,
    inverseSurface = CharcoalBlack,
    inverseOnSurface = LightVanilla,
    error = Red,
    onError = CrispWhite,
    errorContainer = PaleRedContainer,
    onErrorContainer = DeepRed,
    outline = Green,
    outlineVariant = LightGreenBorder,
    scrim = ScrimBlack,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightCardSurface,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest
)

/**
 * Nivel 2: Tokens semanticos para el esquema Monocromatico Oscuro (M3 Expressive Monochrome).
 * Escala pura de grises y negros profundos sin tinte cromatico.
 */
private val MonochromeDarkColorScheme = darkColorScheme(
    primary = CrispWhite,
    onPrimary = ScrimBlack,
    primaryContainer = MonoDarkSurfaceContainerHighest,
    onPrimaryContainer = CrispWhite,
    inversePrimary = CharcoalBlack,
    secondary = MonoLightGrayText,
    onSecondary = ScrimBlack,
    secondaryContainer = MonoDarkSurfaceContainer,
    onSecondaryContainer = CrispWhite,
    tertiary = MonoMidGray,
    onTertiary = ScrimBlack,
    tertiaryContainer = MonoDarkSurface,
    onTertiaryContainer = MonoLightGrayText,
    background = MonoDeepBlack,
    onBackground = CrispWhite,
    surface = MonoDeepBlack,
    onSurface = CrispWhite,
    surfaceVariant = MonoDarkSurfaceContainer,
    onSurfaceVariant = MonoLightGrayText,
    surfaceTint = CrispWhite,
    inverseSurface = CrispWhite,
    inverseOnSurface = ScrimBlack,
    error = LightRed,
    onError = DeepRed,
    errorContainer = DarkRedContainer,
    onErrorContainer = PaleRedContainer,
    outline = MonoMidGray,
    outlineVariant = MonoDarkOutline,
    scrim = ScrimBlack,
    surfaceDim = ScrimBlack,
    surfaceBright = MonoDarkSurfaceContainerHighest,
    surfaceContainerLowest = ScrimBlack,
    surfaceContainerLow = MonoDarkSurface,
    surfaceContainer = MonoDarkSurfaceContainer,
    surfaceContainerHigh = MonoDarkSurfaceContainerHigh,
    surfaceContainerHighest = MonoDarkSurfaceContainerHighest
)

/**
 * Nivel 2: Tokens semanticos para el esquema Monocromatico Claro (M3 Expressive Monochrome).
 * Escala pura de blancos, grises y negros elegantes.
 */
private val MonochromeLightColorScheme = lightColorScheme(
    primary = CharcoalBlack,
    onPrimary = CrispWhite,
    primaryContainer = MonoLightGrayContainer,
    onPrimaryContainer = CharcoalBlack,
    inversePrimary = CrispWhite,
    secondary = MonoDarkGrayText,
    onSecondary = CrispWhite,
    secondaryContainer = MonoNearWhite,
    onSecondaryContainer = CharcoalBlack,
    tertiary = MonoMidGray,
    onTertiary = CrispWhite,
    tertiaryContainer = MonoLightGraySurface,
    onTertiaryContainer = CharcoalBlack,
    background = CrispWhite,
    onBackground = CharcoalBlack,
    surface = CrispWhite,
    onSurface = CharcoalBlack,
    surfaceVariant = MonoLightGraySurface,
    onSurfaceVariant = MonoDarkGrayText,
    surfaceTint = CharcoalBlack,
    inverseSurface = MonoDeepBlack,
    inverseOnSurface = MonoNearWhite,
    error = Red,
    onError = CrispWhite,
    errorContainer = PaleRedContainer,
    onErrorContainer = DeepRed,
    outline = MonoMidGray,
    outlineVariant = MonoLightGrayOutline,
    scrim = ScrimBlack,
    surfaceDim = MonoLightGraySurface,
    surfaceBright = CrispWhite,
    surfaceContainerLowest = CrispWhite,
    surfaceContainerLow = MonoNearWhite,
    surfaceContainer = MonoLightGrayContainer,
    surfaceContainerHigh = MonoLightGraySurface,
    surfaceContainerHighest = MonoLightGrayOutline
)

/**
 * Nivel 2: Tokens semanticos para el esquema Morado Claro (M3 Expressive Purple).
 */
private val PurpleLightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,
    inversePrimary = PurpleDarkPrimary,
    secondary = PurpleSecondary,
    onSecondary = PurpleOnSecondary,
    secondaryContainer = PurpleSecondaryContainer,
    onSecondaryContainer = PurpleOnSecondaryContainer,
    tertiary = PurpleTertiary,
    onTertiary = PurpleOnTertiary,
    tertiaryContainer = PurpleTertiaryContainer,
    onTertiaryContainer = PurpleOnTertiaryContainer,
    background = PurpleBackground,
    onBackground = PurpleOnBackground,
    surface = PurpleSurface,
    onSurface = PurpleOnSurface,
    surfaceVariant = PurpleSurfaceVariant,
    onSurfaceVariant = PurpleOnSurfaceVariant,
    surfaceTint = PurplePrimary,
    inverseSurface = PurpleDarkSurface,
    inverseOnSurface = PurpleDarkOnSurface,
    error = Red,
    onError = CrispWhite,
    errorContainer = PaleRedContainer,
    onErrorContainer = DeepRed,
    outline = PurpleOutline,
    outlineVariant = PurpleOutlineVariant,
    scrim = ScrimBlack,
    surfaceDim = PurpleSurfaceDim,
    surfaceBright = PurpleSurfaceBright,
    surfaceContainerLowest = PurpleSurfaceContainerLowest,
    surfaceContainerLow = PurpleSurfaceContainerLow,
    surfaceContainer = PurpleSurfaceContainer,
    surfaceContainerHigh = PurpleSurfaceContainerHigh,
    surfaceContainerHighest = PurpleSurfaceContainerHighest
)

/**
 * Nivel 2: Tokens semanticos para el esquema Morado Oscuro (M3 Expressive Purple).
 */
private val PurpleDarkColorScheme = darkColorScheme(
    primary = PurpleDarkPrimary,
    onPrimary = PurpleDarkOnPrimary,
    primaryContainer = PurpleDarkPrimaryContainer,
    onPrimaryContainer = PurpleDarkOnPrimaryContainer,
    inversePrimary = PurplePrimary,
    secondary = PurpleDarkSecondary,
    onSecondary = PurpleDarkOnSecondary,
    secondaryContainer = PurpleDarkSecondaryContainer,
    onSecondaryContainer = PurpleDarkOnSecondaryContainer,
    tertiary = PurpleDarkTertiary,
    onTertiary = PurpleDarkOnTertiary,
    tertiaryContainer = PurpleDarkTertiaryContainer,
    onTertiaryContainer = PurpleDarkOnTertiaryContainer,
    background = PurpleDarkBackground,
    onBackground = PurpleDarkOnBackground,
    surface = PurpleDarkSurface,
    onSurface = PurpleDarkOnSurface,
    surfaceVariant = PurpleDarkSurfaceVariant,
    onSurfaceVariant = PurpleDarkOnSurfaceVariant,
    surfaceTint = PurpleDarkPrimary,
    inverseSurface = PurpleSurface,
    inverseOnSurface = PurpleOnSurface,
    error = LightRed,
    onError = DeepRed,
    errorContainer = DarkRedContainer,
    onErrorContainer = PaleRedContainer,
    outline = PurpleDarkOutline,
    outlineVariant = PurpleDarkOutlineVariant,
    scrim = ScrimBlack,
    surfaceDim = PurpleDarkSurfaceDim,
    surfaceBright = PurpleDarkSurfaceBright,
    surfaceContainerLowest = PurpleDarkSurfaceContainerLowest,
    surfaceContainerLow = PurpleDarkSurfaceContainerLow,
    surfaceContainer = PurpleDarkSurfaceContainer,
    surfaceContainerHigh = PurpleDarkSurfaceContainerHigh,
    surfaceContainerHighest = PurpleDarkSurfaceContainerHighest
)

/**
 * Nivel 2: Tokens semanticos para el esquema Azul Claro (M3 Expressive Blue).
 */
private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    inversePrimary = BlueDarkPrimary,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    tertiary = BlueTertiary,
    onTertiary = BlueOnTertiary,
    tertiaryContainer = BlueTertiaryContainer,
    onTertiaryContainer = BlueOnTertiaryContainer,
    background = BlueBackground,
    onBackground = BlueOnBackground,
    surface = BlueSurface,
    onSurface = BlueOnSurface,
    surfaceVariant = BlueSurfaceVariant,
    onSurfaceVariant = BlueOnSurfaceVariant,
    surfaceTint = BluePrimary,
    inverseSurface = BlueDarkSurface,
    inverseOnSurface = BlueDarkOnSurface,
    error = Red,
    onError = CrispWhite,
    errorContainer = PaleRedContainer,
    onErrorContainer = DeepRed,
    outline = BlueOutline,
    outlineVariant = BlueOutlineVariant,
    scrim = ScrimBlack,
    surfaceDim = BlueSurfaceDim,
    surfaceBright = BlueSurfaceBright,
    surfaceContainerLowest = BlueSurfaceContainerLowest,
    surfaceContainerLow = BlueSurfaceContainerLow,
    surfaceContainer = BlueSurfaceContainer,
    surfaceContainerHigh = BlueSurfaceContainerHigh,
    surfaceContainerHighest = BlueSurfaceContainerHighest
)

/**
 * Nivel 2: Tokens semanticos para el esquema Azul Oscuro (M3 Expressive Blue).
 */
private val BlueDarkColorScheme = darkColorScheme(
    primary = BlueDarkPrimary,
    onPrimary = BlueDarkOnPrimary,
    primaryContainer = BlueDarkPrimaryContainer,
    onPrimaryContainer = BlueDarkOnPrimaryContainer,
    inversePrimary = BluePrimary,
    secondary = BlueDarkSecondary,
    onSecondary = BlueDarkOnSecondary,
    secondaryContainer = BlueDarkSecondaryContainer,
    onSecondaryContainer = BlueDarkOnSecondaryContainer,
    tertiary = BlueDarkTertiary,
    onTertiary = BlueDarkOnTertiary,
    tertiaryContainer = BlueDarkTertiaryContainer,
    onTertiaryContainer = BlueDarkOnTertiaryContainer,
    background = BlueDarkBackground,
    onBackground = BlueDarkOnBackground,
    surface = BlueDarkSurface,
    onSurface = BlueDarkOnSurface,
    surfaceVariant = BlueDarkSurfaceVariant,
    onSurfaceVariant = BlueDarkOnSurfaceVariant,
    surfaceTint = BlueDarkPrimary,
    inverseSurface = BlueSurface,
    inverseOnSurface = BlueOnSurface,
    error = LightRed,
    onError = DeepRed,
    errorContainer = DarkRedContainer,
    onErrorContainer = PaleRedContainer,
    outline = BlueDarkOutline,
    outlineVariant = BlueDarkOutlineVariant,
    scrim = ScrimBlack,
    surfaceDim = BlueDarkSurfaceDim,
    surfaceBright = BlueDarkSurfaceBright,
    surfaceContainerLowest = BlueDarkSurfaceContainerLowest,
    surfaceContainerLow = BlueDarkSurfaceContainerLow,
    surfaceContainer = BlueDarkSurfaceContainer,
    surfaceContainerHigh = BlueDarkSurfaceContainerHigh,
    surfaceContainerHighest = BlueDarkSurfaceContainerHighest
)

/**
 * Nivel 2: Tokens semanticos para el esquema Ambar Claro (M3 Expressive Amber).
 */
private val AmberLightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberPrimaryContainer,
    onPrimaryContainer = AmberOnPrimaryContainer,
    inversePrimary = AmberDarkPrimary,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    background = AmberBackground,
    onBackground = AmberOnBackground,
    surface = AmberSurface,
    onSurface = AmberOnSurface,
    surfaceVariant = AmberSurfaceVariant,
    onSurfaceVariant = AmberOnSurfaceVariant,
    surfaceTint = AmberPrimary,
    inverseSurface = AmberDarkSurface,
    inverseOnSurface = AmberDarkOnSurface,
    error = Red,
    onError = CrispWhite,
    errorContainer = PaleRedContainer,
    onErrorContainer = DeepRed,
    outline = AmberOutline,
    outlineVariant = AmberOutlineVariant,
    scrim = ScrimBlack,
    surfaceDim = AmberSurfaceDim,
    surfaceBright = AmberSurfaceBright,
    surfaceContainerLowest = AmberSurfaceContainerLowest,
    surfaceContainerLow = AmberSurfaceContainerLow,
    surfaceContainer = AmberSurfaceContainer,
    surfaceContainerHigh = AmberSurfaceContainerHigh,
    surfaceContainerHighest = AmberSurfaceContainerHighest
)

/**
 * Nivel 2: Tokens semanticos para el esquema Ambar Oscuro (M3 Expressive Amber).
 */
private val AmberDarkColorScheme = darkColorScheme(
    primary = AmberDarkPrimary,
    onPrimary = AmberDarkOnPrimary,
    primaryContainer = AmberDarkPrimaryContainer,
    onPrimaryContainer = AmberDarkOnPrimaryContainer,
    inversePrimary = AmberPrimary,
    secondary = AmberDarkSecondary,
    onSecondary = AmberDarkOnSecondary,
    secondaryContainer = AmberDarkSecondaryContainer,
    onSecondaryContainer = AmberDarkOnSecondaryContainer,
    tertiary = AmberDarkTertiary,
    onTertiary = AmberDarkOnTertiary,
    tertiaryContainer = AmberDarkTertiaryContainer,
    onTertiaryContainer = AmberDarkOnTertiaryContainer,
    background = AmberDarkBackground,
    onBackground = AmberDarkOnBackground,
    surface = AmberDarkSurface,
    onSurface = AmberDarkOnSurface,
    surfaceVariant = AmberDarkSurfaceVariant,
    onSurfaceVariant = AmberDarkOnSurfaceVariant,
    surfaceTint = AmberDarkPrimary,
    inverseSurface = AmberSurface,
    inverseOnSurface = AmberOnSurface,
    error = LightRed,
    onError = DeepRed,
    errorContainer = DarkRedContainer,
    onErrorContainer = PaleRedContainer,
    outline = AmberDarkOutline,
    outlineVariant = AmberDarkOutlineVariant,
    scrim = ScrimBlack,
    surfaceDim = AmberDarkSurfaceDim,
    surfaceBright = AmberDarkSurfaceBright,
    surfaceContainerLowest = AmberDarkSurfaceContainerLowest,
    surfaceContainerLow = AmberDarkSurfaceContainerLow,
    surfaceContainer = AmberDarkSurfaceContainer,
    surfaceContainerHigh = AmberDarkSurfaceContainerHigh,
    surfaceContainerHighest = AmberDarkSurfaceContainerHighest
)

/**
 * Nivel 2: Colores semanticos deportivos desacoplados del esquema de acentos para mantener
 * legibilidad reglamentaria de tarjetas disciplinarias en cualquier paleta seleccionada.
 */
@Immutable
data class UlamaColors(
    val cardYellow: Color = RefereeYellowCard,
    val cardRed: Color = RefereeRedCard
)

val LocalUlamaColors = staticCompositionLocalOf { UlamaColors() }

/**
 * CompositionLocal que expone si el tema visual activo es oscuro, respetando configuracion manual y de sistema.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Tema principal de Ulama que aplica los tokens semanticos del sistema, formas, espaciado y movimiento desacoplado.
 * Soporta Dynamic Color en Android 12+, la paleta oficial Ulama y el esquema Monocromatico.
 *
 * @param darkTheme Determina si se aplica la variante oscura del tema.
 * @param dynamicColor Habilita la extraccion dinamica de color del sistema en Android 12 o superior.
 * @param colorPalette Paleta de color semantica seleccionada (Dynamic, Ulama o Monochrome).
 * @param isExpressiveMotion Conmuta entre simulacion de fisicas con rebote (Expressive) o curvas estandar (Standard).
 * @param content Contenido composable envuelto bajo la jerarquia del tema.
 */
@Composable
fun UlamaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorPalette: ColorPalette = if (dynamicColor) ColorPalette.DYNAMIC else ColorPalette.ULAMA,
    isExpressiveMotion: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        colorPalette == ColorPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        colorPalette == ColorPalette.MONOCHROME -> {
            if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
        }
        colorPalette == ColorPalette.PURPLE -> {
            if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        }
        colorPalette == ColorPalette.BLUE -> {
            if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        }
        colorPalette == ColorPalette.AMBER -> {
            if (darkTheme) AmberDarkColorScheme else AmberLightColorScheme
        }
        darkTheme -> UlamaDarkColorScheme
        else -> UlamaLightColorScheme
    }
    val motion = if (isExpressiveMotion) UlamaMotion.Expressive else UlamaMotion.Standard
    val motionScheme = if (isExpressiveMotion) MotionScheme.expressive() else MotionScheme.standard()
    val borders = UlamaBorders(cardBorder = null)

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalUlamaColors provides UlamaColors(),
        LocalUlamaMotion provides motion,
        LocalUlamaShapes provides UlamaShapes(),
        LocalUlamaSpacing provides UlamaSpacing(),
        LocalUlamaDimensions provides UlamaDimensions(),
        LocalUlamaBorders provides borders
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            shapes = UlamaMaterialShapes,
            typography = Typography,
            motionScheme = motionScheme,
            content = content
        )
    }
}

/**
 * Punto de acceso centralizado a los tokens de Nivel 2 para consumo directo en componentes (Nivel 3).
 */
object UlamaTheme {
    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalDarkTheme.current

    val colors: UlamaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalUlamaColors.current

    val spacing: UlamaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalUlamaSpacing.current

    val shapes: UlamaShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalUlamaShapes.current

    val dimensions: UlamaDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalUlamaDimensions.current

    val borders: UlamaBorders
        @Composable
        @ReadOnlyComposable
        get() = LocalUlamaBorders.current

    val motion: UlamaMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalUlamaMotion.current
}