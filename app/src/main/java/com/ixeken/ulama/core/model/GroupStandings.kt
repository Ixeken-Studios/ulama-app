package com.ixeken.ulama.core.model

import androidx.compose.runtime.Immutable

/**
 * Modelo inmutable para la clasificacion de un grupo en torneos con fase de grupos multiseccion.
 * Disenado para respetar la estabilidad del compilador de Compose bajo Strong Skipping Mode.
 *
 * @param name Nombre o identificador del grupo (ej. "A", "B").
 * @param standings Lista de clubes que integran el grupo ordenados por posicion competitiva.
 * @param cutoffs Reglas modulares de division visual para puestos clasificatorios dentro del grupo.
 * @param legend Leyendas explicativas al pie de la tabla para las zonas del grupo.
 */
@Immutable
data class GroupStandings(
    val name: String,
    val standings: List<StandingEntry>,
    val cutoffs: List<TableCutoff> = emptyList(),
    val legend: List<TableLegendEntry> = emptyList()
)
