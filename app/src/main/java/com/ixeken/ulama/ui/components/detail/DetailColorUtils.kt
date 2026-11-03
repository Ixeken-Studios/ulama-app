package com.ixeken.ulama.ui.components.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.sqrt

/**
 * Par de colores definitivos resueltos para representar a ambos rivales en un partido.
 */
data class ResolvedMatchColors(
    val homeColor: Color,
    val awayColor: Color
)

/**
 * Utilidades de conversion, contraste y resolucion de colisiones para colores de clubes deportivos.
 * Convierte cadenas hexadecimales provenientes del API de ESPN a Color de Compose
 * con recuperacion resiliente ante formatos invalidos, calcula contrastes accesibles y resuelve
 * emparejamientos cromaticos cuando dos clubes visten tonos similares (por ejemplo, rojo vs rojo).
 */
object DetailColorUtils {

    /**
     * Umbral perceptivo por debajo del cual dos colores se consideran en colision visual.
     * En una escala normalizada de 0.0 a 1.0 mediante redmean, distancias inferiores a 0.30
     * resultan dificiles de distinguir visualmente en barras comparativas adyacentes.
     */
    const val COLOR_CLASH_THRESHOLD: Float = 0.30f

    /**
     * Parsea una cadena hexadecimal (con o sin prefijo '#') a un Color de Jetpack Compose.
     * Si la cadena es nula, vacia o no conforma un formato de color valido, devuelve el color de respaldo.
     *
     * @param hex Cadena hexadecimal del club enviada por ESPN (ejemplo: "ffee00" o "#386641").
     * @param fallback Color semantico de respaldo en caso de error o ausencia.
     */
    fun parseHexColor(hex: String?, fallback: Color): Color {
        if (hex.isNullOrBlank()) return fallback
        return runCatching {
            val formattedHex = when {
                hex.startsWith("#") -> hex
                hex.length in 3..8 -> "#$hex"
                else -> return fallback
            }
            Color(android.graphics.Color.parseColor(formattedHex))
        }.getOrDefault(fallback)
    }

    /**
     * Determina el color optimo de texto o icono (blanco vainilla o negro carbon) para garantizar
     * contraste WCAG sobre un fondo de color especifico segun su luminancia relativa.
     */
    fun getContrastingTextColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.45f) {
            Color(0xFF1F1F1F)
        } else {
            Color(0xFFF9F2E1)
        }
    }

    /**
     * Adapta el color institucional de un equipo para garantizar legibilidad y contraste WCAG
     * como tipografia plana sobre superficies claras u oscuras sin distorsionar su identidad visual.
     *
     * Si el color ya cuenta con luminancia adecuada (por ejemplo, amarillo o rojo claro en modo oscuro,
     * o azul marino en modo claro), se mantiene intacto. Si un club viste un tono excesivamente
     * oscuro en modo oscuro (como azul noche o negro), eleva su luminosidad en el espacio HSL
     * al 58% para que resulte nitido y vibrante. En modo claro, atenua tonos excesivamente palidos
     * limitando su luminosidad al 38% para evitar que se desvanezcan contra el fondo.
     *
     * @param color Tono original resuelto del club.
     * @param isDark Indicador de si la superficie opera bajo modo oscuro.
     * @return Color adaptado para lectura de texto.
     */
    fun getAccessibleTeamColor(color: Color, isDark: Boolean): Color {
        val lum = color.luminance()
        if (isDark) {
            if (lum >= 0.22f) return color
            val hsl = rgbToHsl(color.red, color.green, color.blue)
            val adjustedL = hsl[2].coerceAtLeast(0.58f)
            return hslToColor(hsl[0], hsl[1], adjustedL)
        } else {
            if (lum <= 0.45f) return color
            val hsl = rgbToHsl(color.red, color.green, color.blue)
            val adjustedL = hsl[2].coerceAtMost(0.38f)
            return hslToColor(hsl[0], hsl[1], adjustedL)
        }
    }

    private fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val l = (max + min) / 2f
        var h = 0f
        var s = 0f

        if (delta > 0.0001f) {
            s = if (l <= 0.5f) delta / (max + min) else delta / (2f - max - min)
            h = when (max) {
                r -> ((g - b) / delta + (if (g < b) 6f else 0f)) * 60f
                g -> ((b - r) / delta + 2f) * 60f
                else -> ((r - g) / delta + 4f) * 60f
            }
        }
        return floatArrayOf(h, s, l)
    }

    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (rPrime, gPrime, bPrime) = when (((h / 60f) % 6f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    /**
     * Calcula la distancia perceptiva ponderada (algoritmo redmean) entre dos colores.
     * Pondera las diferencias de rojo, verde y azul segun la sensibilidad del ojo humano,
     * devolviendo un valor normalizado entre 0.0 (colores identicos) y 1.0 (maxima diferencia).
     */
    fun calculateColorDistance(c1: Color, c2: Color): Float {
        val rDiff = c1.red - c2.red
        val gDiff = c1.green - c2.green
        val bDiff = c1.blue - c2.blue
        val rMean = (c1.red + c2.red) / 2f
        val weightR = 2f + rMean
        val weightG = 4f
        val weightB = 2f + (1f - rMean)
        val distanceSquared = weightR * rDiff * rDiff + weightG * gDiff * gDiff + weightB * bDiff * bDiff
        return (sqrt(distanceSquared) / 3f).coerceIn(0f, 1f)
    }

    /**
     * Resuelve los colores a mostrar para un encuentro evitando colisiones cromaticas.
     * Siguiendo el protocolo oficial de vestimenta deportiva:
     * 1. El club local conserva su color titular (homeColor).
     * 2. Si el color titular del club visitante colisiona con el del local, el visitante conmuta
     *    a su color alternativo (alternateColor).
     * 3. Si el visitante carece de alternativo o persiste la colision, el local conmuta a su alternativo.
     * 4. Si ambos alternativos colisionan, se emplea el color semantico de respaldo asegurando legibilidad.
     */
    fun resolveMatchColors(
        homePrimaryHex: String?,
        homeAlternateHex: String?,
        awayPrimaryHex: String?,
        awayAlternateHex: String?,
        homeFallback: Color,
        awayFallback: Color,
        threshold: Float = COLOR_CLASH_THRESHOLD
    ): ResolvedMatchColors {
        val initialHome = parseHexColor(homePrimaryHex, homeFallback)
        val initialAway = parseHexColor(awayPrimaryHex, awayFallback)

        // Escenario 1: Distancia suficiente, sin colision visual
        if (calculateColorDistance(initialHome, initialAway) >= threshold) {
            return ResolvedMatchColors(homeColor = initialHome, awayColor = initialAway)
        }

        // Escenario 2: Colision detectada. El visitante conmuta a su uniforme alternativo
        if (!awayAlternateHex.isNullOrBlank()) {
            val awayAlternate = parseHexColor(awayAlternateHex, awayFallback)
            if (calculateColorDistance(initialHome, awayAlternate) >= threshold) {
                return ResolvedMatchColors(homeColor = initialHome, awayColor = awayAlternate)
            }
        }

        // Escenario 3: El alternativo visitante tambien colisiona. Se prueba alternativo local vs titular visitante
        if (!homeAlternateHex.isNullOrBlank()) {
            val homeAlternate = parseHexColor(homeAlternateHex, homeFallback)
            if (calculateColorDistance(homeAlternate, initialAway) >= threshold) {
                return ResolvedMatchColors(homeColor = homeAlternate, awayColor = initialAway)
            }
        }

        // Escenario 4: Ambos alternativos colisionan o estan ausentes. Se usa el respaldo con contraste garantizado
        val safeAway = if (calculateColorDistance(initialHome, awayFallback) >= threshold) {
            awayFallback
        } else {
            if (initialHome.luminance() > 0.5f) Color(0xFF1F1F1F) else Color(0xFFF9F2E1)
        }

        return ResolvedMatchColors(homeColor = initialHome, awayColor = safeAway)
    }
}
