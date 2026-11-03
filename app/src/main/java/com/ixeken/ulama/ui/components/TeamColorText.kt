package com.ixeken.ulama.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ixeken.ulama.ui.components.detail.DetailColorUtils
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Componente modular de texto para letras y numeros que adoptan el color del equipo.
 *
 * En lugar de aplicar trazos o bordes artificiales que distorsionan las formas de las letras,
 * este componente evalua dinamicamente el contraste del color institucional contra el fondo
 * mediante [DetailColorUtils.getAccessibleTeamColor]. Si el color resulta excesivamente oscuro
 * en modo oscuro o excesivamente palido en modo claro, ajusta su luminosidad HSL de forma limpia
 * preservando la identidad cromatica original del club y la estetica minimalista de la app.
 *
 * @param text Contenido textual o cifra numerica a desplegar.
 * @param color Tono institucional del club.
 * @param modifier Modificador base de Compose aplicado al texto.
 * @param style Estilo tipografico a aplicar, por defecto [LocalTextStyle].
 * @param fontWeight Grosor de la fuente.
 * @param textAlign Alineacion horizontal del texto.
 * @param maxLines Cantidad maxima de lineas permitidas.
 * @param overflow Comportamiento ante desbordamiento de texto.
 */
@Composable
fun TeamColorText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val isDark = UlamaTheme.isDark
    val readableColor = remember(color, isDark) {
        DetailColorUtils.getAccessibleTeamColor(color, isDark)
    }

    Text(
        text = text,
        color = readableColor,
        style = style,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}
