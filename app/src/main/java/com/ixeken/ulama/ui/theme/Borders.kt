package com.ixeken.ulama.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Nivel 2: Tokens semanticos de bordes y trazos estructurales.
 * Controla el delineado perimetral de tarjetas y contenedores de datos.
 * Tanto en modo claro como en modo oscuro las tarjetas prescinden de trazo exterior
 * para mantener una estetica limpia y fluida inspirada en Apple Sports.
 *
 * @param cardBorder Trazo perimetral para tarjetas y contenedores de datos.
 */
@Immutable
data class UlamaBorders(
    val cardBorder: BorderStroke? = null
)

val LocalUlamaBorders = staticCompositionLocalOf { UlamaBorders() }
