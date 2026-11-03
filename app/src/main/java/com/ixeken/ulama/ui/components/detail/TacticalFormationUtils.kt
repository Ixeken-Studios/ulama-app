package com.ixeken.ulama.ui.components.detail

import com.ixeken.ulama.core.model.LineupPlayer
import com.ixeken.ulama.core.model.TacticalLine

/**
 * Utilidades de calculo y estructuracion geometrica para alineaciones tacticas en el terreno de juego.
 * Permite descomponer cualquier formacion numerica dinamica de futbol como 4-2-3-1, 3-4-2-1 o 4-1-4-1
 * en filas horizontales ordenadas de ataque a defensa y de izquierda a derecha.
 */
object TacticalFormationUtils {

    /**
     * Resuelve las filas de jugadores titulares a desplegar en la cancha.
     * Si la formacion numerica es valida y su suma coincide con los 10 jugadores de campo,
     * se agrupan en las lineas exactas del sistema tactico; de lo contrario se recurre a las
     * cuatro lineas canonicas tradicionales.
     */
    fun resolveTacticalRows(
        formation: String,
        starters: List<LineupPlayer>
    ): List<List<LineupPlayer>> {
        val (goalkeepers, outfield) = starters.partition {
            it.line == TacticalLine.GOALKEEPER ||
                it.formationPlace == 1 ||
                it.position.contains("Goalkeeper", ignoreCase = true) ||
                it.position.contains("Portero", ignoreCase = true)
        }

        val formationParts = formation.split("-").mapNotNull { it.toIntOrNull() }
        val isDynamicValid = formationParts.isNotEmpty() && formationParts.sum() == outfield.size

        if (isDynamicValid) {
            // En futbol las formaciones se expresan de defensa a ataque (4-2-3-1).
            // En la pantalla el ataque se dibuja arriba y la defensa abajo, por lo que invertimos el orden.
            val rowCountsFromTop = formationParts.reversed()
            val sortedOutfield = outfield.sortedWith(
                compareByDescending<LineupPlayer> { getVerticalDepthScore(it) }
                    .thenByDescending { it.formationPlace }
            )

            var currentIndex = 0
            val tacticalRows = ArrayList<List<LineupPlayer>>(rowCountsFromTop.size + 1)
            for (count in rowCountsFromTop) {
                val nextIndex = (currentIndex + count).coerceAtMost(sortedOutfield.size)
                val rowChunk = sortedOutfield.subList(currentIndex, nextIndex)
                val sortedRow = rowChunk.sortedWith(
                    compareBy<LineupPlayer> { getHorizontalScore(it) }
                        .thenBy { it.formationPlace }
                )
                tacticalRows.add(sortedRow)
                currentIndex = nextIndex
            }

            if (goalkeepers.isNotEmpty()) {
                tacticalRows.add(goalkeepers)
            }
            return tacticalRows
        }

        // Respaldo canonico en caso de formaciones no parseables o datos incompletos
        val forwards = outfield.filter { it.line == TacticalLine.FORWARD }
        val midfielders = outfield.filter { it.line == TacticalLine.MIDFIELD }
        val defenders = outfield.filter { it.line == TacticalLine.DEFENSE }

        return listOfNotNull(
            forwards.ifEmpty { null }?.sortedBy { getHorizontalScore(it) },
            midfielders.ifEmpty { null }?.sortedBy { getHorizontalScore(it) },
            defenders.ifEmpty { null }?.sortedBy { getHorizontalScore(it) },
            goalkeepers.ifEmpty { null }
        )
    }

    /**
     * Asigna una puntuacion de profundidad vertical para ubicar al jugador en el eje ataque-defensa.
     * Valores mayores representan posiciones mas adelantadas en el campo.
     */
    fun getVerticalDepthScore(player: LineupPlayer): Int {
        val pos = player.position.lowercase()
        return when {
            pos.contains("forward") || pos.contains("delantero") ||
                pos.contains("striker") || pos.contains("wing") || pos.contains("extremo") -> {
                when {
                    pos.contains("center left forward") || pos.contains("center right forward") ||
                        pos.contains("second striker") || pos.contains("segundo delantero") -> 4
                    else -> 5
                }
            }

            pos.contains("attacking") || pos.contains("ofensivo") || pos.contains("mediapunta") -> 3

            pos.contains("midfield") || pos.contains("volante") || pos.contains("medio") -> {
                when {
                    pos.contains("defensive") || pos.contains("defensivo") || pos.contains("pivote") -> 1
                    else -> 2
                }
            }

            pos.contains("defender") || pos.contains("defensa") || pos.contains("back") ||
                pos.contains("lateral") || pos.contains("central") || pos.contains("sweeper") -> 0

            pos.contains("goalkeeper") || pos.contains("portero") || pos.contains("arquero") -> -1

            else -> when (player.formationPlace) {
                1 -> -1
                in 2..5 -> 0
                in 6..8 -> 2
                in 9..11 -> 5
                else -> 0
            }
        }
    }

    /**
     * Asigna una puntuacion lateral para ordenar a los futbolistas de izquierda a derecha en su fila tactica.
     * Valores menores corresponden a la banda izquierda y valores mayores a la banda derecha.
     */
    fun getHorizontalScore(player: LineupPlayer): Int {
        val pos = player.position.lowercase()
        return when {
            pos.contains("center left") || pos.contains("central izquierdo") ||
                pos.contains("medio izquierdo") -> 2

            pos.contains("center right") || pos.contains("central derecho") ||
                pos.contains("medio derecho") -> 4

            pos.contains("left") || pos.contains("izquierdo") -> 1

            pos.contains("right") || pos.contains("derecho") -> 5

            else -> 3
        }
    }
}
