package com.ixeken.ulama.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * Estilo visual del separador horizontal para delimitar zonas en la tabla.
 */
@Immutable
enum class CutoffLineStyle {
    /** Trazo continuo solido para clasificaciones y fases eliminatorias. */
    SOLID,

    /** Trazo discontinuo segmentado para advertencia de descenso. */
    DASHED
}

/**
 * Clasificacion semantica de las zonas de la tabla de posiciones de futbol.
 */
@Immutable
enum class TableZoneType {
    /** Acceso directo a torneo principal (Champions League o Liguilla directa). */
    QUALIFICATION,

    /** Reclasificacion, copa secundaria o eliminatoria intermedia (Play-in o Europa League). */
    PLAYOFF,

    /** Zona de perdida de categoria o descenso directo. */
    RELEGATION
}

/**
 * Regla de corte visual en la tabla de clasificacion.
 * Modela divisiones de forma modular sin acoplarse a nombres de ligas en los componentes de UI.
 *
 * @param rankAfter Posicion en la tabla tras la cual se dibuja la linea divisoria.
 * @param zoneType Categoria de la zona para determinar la semantica competitiva.
 * @param lineStyle Trazo solido o segmentado para distinguir clasificaciones de descenso.
 */
@Immutable
data class TableCutoff(
    val rankAfter: Int,
    val zoneType: TableZoneType,
    val lineStyle: CutoffLineStyle = CutoffLineStyle.SOLID
)

/**
 * Entrada descriptiva de leyenda al pie de la tabla de posiciones.
 *
 * @param rangeText Rango de puestos asociados (ej. "1-4", "18-20").
 * @param labelRes Recurso de texto traducible para la descripcion de la zona.
 */
@Immutable
data class TableLegendEntry(
    val rangeText: String,
    @param:StringRes val labelRes: Int
)
