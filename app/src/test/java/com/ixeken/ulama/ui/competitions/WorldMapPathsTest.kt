package com.ixeken.ulama.ui.competitions

import androidx.compose.ui.graphics.vector.PathParser
import com.ixeken.ulama.core.model.LeagueRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para verificar la integridad de las cinco rutas SVG continentales,
 * la validez de su sintaxis en PathParser y los anclajes proporcionales dentro del viewBox.
 */
class WorldMapPathsTest {

    @Test
    fun continents_parseCleanlyAndCoverAllInteractiveRegions() {
        assertEquals(LeagueRegion.continents.size, WorldMapPaths.continents.size)
        assertEquals(LeagueRegion.continents.toSet(), WorldMapPaths.continents.map { it.region }.toSet())

        val parser = PathParser()
        WorldMapPaths.continents.forEach { spec ->
            assertTrue(spec.pathStrings.isNotEmpty())
            assertTrue(spec.labelFraction.x in 0f..1f)
            assertTrue(spec.labelFraction.y in 0f..1f)
            spec.pathStrings.forEach { d ->
                parser.clear()
                val nodes = parser.parsePathString(d).toNodes()
                assertTrue(nodes.size > 5)
            }
        }
    }
}
