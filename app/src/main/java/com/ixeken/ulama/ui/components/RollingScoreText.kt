package com.ixeken.ulama.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.ixeken.ulama.ui.theme.UlamaMotion
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Componente modular que anima la transicion numerica de anotaciones deportivas mediante desplazamiento vertical.
 *
 * Se aisla esta microinteraccion para desacoplar el efecto de odometro de los contenedores de tarjetas y cabeceras.
 * Utiliza [AnimatedContent] acoplado a la fisica de resortes de [UlamaMotion.spatialSpec] para asegurar coherencia
 * con el estilo activo: rebote reactivo en modo Expressive y traslacion seca en modo Standard.
 *
 * @param score Cifra numerica de goles a desplegar y animar ante variaciones.
 * @param modifier Modificador base de Compose.
 * @param color Tono de color para el texto, permitiendo modular opacidad en clubes derrotados.
 * @param style Estilo tipografico a aplicar, por defecto headlineLarge.
 * @param fontWeight Grosor de la tipografia deportiva, por defecto Black.
 * @param motion Configuracion de movimiento activa, enlazada por defecto a [UlamaTheme.motion].
 */
@Composable
fun RollingScoreText(
    score: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    fontWeight: FontWeight = FontWeight.Black,
    motion: UlamaMotion = UlamaTheme.motion
) {
    AnimatedContent(
        targetState = score,
        transitionSpec = {
            val isIncreasing = targetState > initialState
            val enterOffsetY = if (isIncreasing) { height: Int -> height } else { height: Int -> -height }
            val exitOffsetY = if (isIncreasing) { height: Int -> -height } else { height: Int -> height }
            slideInVertically(
                animationSpec = motion.spatialSpec(),
                initialOffsetY = enterOffsetY
            ) togetherWith slideOutVertically(
                animationSpec = motion.spatialSpec(),
                targetOffsetY = exitOffsetY
            )
        },
        label = "RollingScoreTransition",
        modifier = modifier
    ) { currentScore ->
        Text(
            text = currentScore.toString(),
            style = style,
            fontWeight = fontWeight,
            color = color
        )
    }
}
