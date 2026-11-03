package com.ixeken.ulama.ui.components.detail

import com.ixeken.ulama.core.model.LineupPlayer
import com.ixeken.ulama.core.model.TacticalLine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias para validar la resolucion y ordenamiento dinamico de formaciones tacticas.
 */
class TacticalFormationUtilsTest {

    private fun createPlayer(
        id: String,
        name: String,
        position: String,
        formationPlace: Int,
        line: TacticalLine
    ) = LineupPlayer(
        id = id,
        name = name,
        jersey = formationPlace.toString(),
        position = position,
        formationPlace = formationPlace,
        line = line
    )

    @Test
    fun resolveTacticalRows_4231_producesFiveExactTacticalRowsWithProperHorizontalOrder() {
        val starters = listOf(
            createPlayer("1", "Nahuel Guzmán", "Goalkeeper", 1, TacticalLine.GOALKEEPER),
            createPlayer("2", "Javier Aquino", "Right Back", 2, TacticalLine.DEFENSE),
            createPlayer("3", "Jesús Angulo", "Left Back", 3, TacticalLine.DEFENSE),
            createPlayer("4", "Fernando Gorriarán", "Left Midfielder", 4, TacticalLine.MIDFIELD),
            createPlayer("5", "Guido Pizarro", "Center Right Defender", 5, TacticalLine.DEFENSE),
            createPlayer("6", "Joaquim", "Center Left Defender", 6, TacticalLine.DEFENSE),
            createPlayer("7", "Sebastián Córdova", "Attacking Midfielder Right", 7, TacticalLine.MIDFIELD),
            createPlayer("8", "Rafael Carioca", "Right Midfielder", 8, TacticalLine.MIDFIELD),
            createPlayer("9", "André-Pierre Gignac", "Forward", 9, TacticalLine.FORWARD),
            createPlayer("10", "Juan Brunetta", "Attacking Midfielder", 10, TacticalLine.MIDFIELD),
            createPlayer("11", "Diego Lainez", "Attacking Midfielder Left", 11, TacticalLine.MIDFIELD)
        )

        val rows = TacticalFormationUtils.resolveTacticalRows("4-2-3-1", starters)

        // 4-2-3-1 debe generar 5 filas: 1 delantero, 3 mediapuntas, 2 pivotes, 4 defensas y 1 portero
        assertEquals(5, rows.size)

        // Fila 1: Delantero centro
        assertEquals(1, rows[0].size)
        assertEquals("André-Pierre Gignac", rows[0][0].name)

        // Fila 2: 3 mediapuntas ordenados de izquierda a derecha (Lainez -> Brunetta -> Córdova)
        assertEquals(3, rows[1].size)
        assertEquals("Diego Lainez", rows[1][0].name)
        assertEquals("Juan Brunetta", rows[1][1].name)
        assertEquals("Sebastián Córdova", rows[1][2].name)

        // Fila 3: 2 contenciones ordenados (Gorriarán izquierda -> Carioca derecha)
        assertEquals(2, rows[2].size)
        assertEquals("Fernando Gorriarán", rows[2][0].name)
        assertEquals("Rafael Carioca", rows[2][1].name)

        // Fila 4: 4 defensores ordenados de izquierda a derecha (Angulo -> Joaquim -> Pizarro -> Aquino)
        assertEquals(4, rows[3].size)
        assertEquals("Jesús Angulo", rows[3][0].name)
        assertEquals("Joaquim", rows[3][1].name)
        assertEquals("Guido Pizarro", rows[3][2].name)
        assertEquals("Javier Aquino", rows[3][3].name)

        // Fila 5: Portero
        assertEquals(1, rows[4].size)
        assertEquals("Nahuel Guzmán", rows[4][0].name)
    }

    @Test
    fun resolveTacticalRows_3421_producesFiveRows() {
        val starters = listOf(
            createPlayer("1", "Kevin Mier", "Goalkeeper", 1, TacticalLine.GOALKEEPER),
            createPlayer("2", "Ignacio Rivero", "Right Midfielder", 2, TacticalLine.MIDFIELD),
            createPlayer("3", "Rodolfo Rotondi", "Left Midfielder", 3, TacticalLine.MIDFIELD),
            createPlayer("4", "Gonzalo Piovi", "Center Left Defender", 4, TacticalLine.DEFENSE),
            createPlayer("5", "Érik Lira", "Center Defender", 5, TacticalLine.DEFENSE),
            createPlayer("6", "Willer Ditta", "Center Right Defender", 6, TacticalLine.DEFENSE),
            createPlayer("7", "Lorenzo Faravelli", "Center Right Midfielder", 7, TacticalLine.MIDFIELD),
            createPlayer("8", "Carlos Rodríguez", "Center Left Midfielder", 8, TacticalLine.MIDFIELD),
            createPlayer("9", "Angel Sepúlveda", "Forward", 9, TacticalLine.FORWARD),
            createPlayer("10", "Luis Romo", "Center Right Forward", 10, TacticalLine.FORWARD),
            createPlayer("11", "Alexis Gutierrez", "Center Left Forward", 11, TacticalLine.FORWARD)
        )

        val rows = TacticalFormationUtils.resolveTacticalRows("3-4-2-1", starters)

        assertEquals(5, rows.size)
        assertEquals(1, rows[0].size) // 1 delantero centro
        assertEquals("Angel Sepúlveda", rows[0][0].name)
        assertEquals(2, rows[1].size) // 2 atacantes secundarios
        assertEquals(4, rows[2].size) // 4 mediocampistas
        assertEquals(3, rows[3].size) // 3 defensores
        assertEquals(1, rows[4].size) // 1 portero
    }

    @Test
    fun resolveTacticalRows_fallbackToCanonicalLinesWhenFormationIsMalformed() {
        val starters = listOf(
            createPlayer("1", "Portero", "Goalkeeper", 1, TacticalLine.GOALKEEPER),
            createPlayer("2", "Defensa", "Defender", 2, TacticalLine.DEFENSE),
            createPlayer("3", "Medio", "Midfielder", 3, TacticalLine.MIDFIELD),
            createPlayer("4", "Delantero", "Forward", 4, TacticalLine.FORWARD)
        )

        val rows = TacticalFormationUtils.resolveTacticalRows("INVALID", starters)

        assertEquals(4, rows.size)
        assertEquals("Delantero", rows[0][0].name)
        assertEquals("Medio", rows[1][0].name)
        assertEquals("Defensa", rows[2][0].name)
        assertEquals("Portero", rows[3][0].name)
    }
}
