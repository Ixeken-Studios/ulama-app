package com.ixeken.ulama.ui.components.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de resolucion de colisiones cromaticas en DetailColorUtils.
 * Verifica la conmutacion automatica al uniforme alternativo cuando ambos clubes visten el mismo color base.
 */
class DetailColorUtilsTest {

    @Test
    fun calculateColorDistance_detectsClashBetweenReds() {
        // Dos tonos de rojo similares (como Tijuana y Atlas)
        val redTijuana = Color(0xFFCE1126)
        val redAtlas = Color(0xFFD00000)

        val distance = DetailColorUtils.calculateColorDistance(redTijuana, redAtlas)

        // La distancia entre dos rojos debe ser muy baja (por debajo del umbral de colision)
        assertTrue(distance < DetailColorUtils.COLOR_CLASH_THRESHOLD)
    }

    @Test
    fun calculateColorDistance_distinguishesDifferentColors() {
        val red = Color(0xFFCE1126)
        val blue = Color(0xFF0032A8)

        val distance = DetailColorUtils.calculateColorDistance(red, blue)

        assertTrue(distance >= DetailColorUtils.COLOR_CLASH_THRESHOLD)
    }

    @Test
    fun resolveMatchColors_whenColorsClash_switchesAwayToAlternateColor() {
        // Caso real Tijuana (rojo) vs Atlas (rojo con alternativo blanco o negro)
        val tijuanaRed = "ce1126"
        val atlasRed = "d00000"
        val atlasAlternateWhite = "ffffff"

        val resolved = DetailColorUtils.resolveMatchColors(
            homePrimaryHex = tijuanaRed,
            homeAlternateHex = "000000",
            awayPrimaryHex = atlasRed,
            awayAlternateHex = atlasAlternateWhite,
            homeFallback = Color.Red,
            awayFallback = Color.Blue
        )

        // El local mantiene su rojo titular
        assertEquals(DetailColorUtils.parseHexColor(tijuanaRed, Color.Red), resolved.homeColor)
        // El visitante debe haber conmutado a su blanco alternativo para evitar barras indistinguibles
        assertEquals(DetailColorUtils.parseHexColor(atlasAlternateWhite, Color.Blue), resolved.awayColor)
    }

    @Test
    fun resolveMatchColors_whenColorsAreDistinct_keepsPrimaryKits() {
        val cruzAzulBlue = "0032a8"
        val americaYellow = "ffee00"

        val resolved = DetailColorUtils.resolveMatchColors(
            homePrimaryHex = cruzAzulBlue,
            homeAlternateHex = "ffffff",
            awayPrimaryHex = americaYellow,
            awayAlternateHex = "001a4d",
            homeFallback = Color.Blue,
            awayFallback = Color.Yellow
        )

        assertEquals(DetailColorUtils.parseHexColor(cruzAzulBlue, Color.Blue), resolved.homeColor)
        assertEquals(DetailColorUtils.parseHexColor(americaYellow, Color.Yellow), resolved.awayColor)
    }

    @Test
    fun getAccessibleTeamColor_inDarkMode_preservesLuminousColor() {
        val americaYellow = Color(0xFFFFEE00)
        val adjusted = DetailColorUtils.getAccessibleTeamColor(americaYellow, isDark = true)
        assertEquals(americaYellow, adjusted)
    }

    @Test
    fun getAccessibleTeamColor_inDarkMode_lightensDarkKit() {
        val darkNavy = Color(0xFF001993)
        val adjusted = DetailColorUtils.getAccessibleTeamColor(darkNavy, isDark = true)
        assertTrue(adjusted != darkNavy)
        // La luminancia debe elevarse significativamente respecto al tono sombrío original
        assertTrue(adjusted.luminance() > darkNavy.luminance())
        assertTrue(adjusted.luminance() >= 0.10f)
    }

    @Test
    fun getAccessibleTeamColor_inLightMode_darkensPaleKit() {
        val pureWhite = Color(0xFFFFFFFF)
        val adjusted = DetailColorUtils.getAccessibleTeamColor(pureWhite, isDark = false)
        assertTrue(adjusted != pureWhite)
        // La luminancia debe reducirse para no desvanecerse en fondo claro
        assertTrue(adjusted.luminance() <= 0.40f)
    }
}
