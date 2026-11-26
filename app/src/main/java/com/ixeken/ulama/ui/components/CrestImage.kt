package com.ixeken.ulama.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Renderiza el escudo de un club con respaldo geometrico basado en MaterialShapes.
 * Asigna de forma determinista una forma distinta segun la identidad del equipo para diferenciar
 * visualmente clubes sin imagen oficial sin parpadeos durante recomposiciones o desplazamientos.
 *
 * @param logoUrl URL del escudo remoto resuelta segun el tema activo.
 * @param teamName Nombre completo del club usado como respaldo de iniciales.
 * @param abbreviation Abreviatura corta del club mostrada dentro del emblema alternativo.
 * @param modifier Modificador de tamano, recorte o posicion aplicado al componente raiz.
 * @param teamId Identificador estable del club utilizado como semilla determinista de forma.
 * @param contentDescription Descripcion accesible para TalkBack.
 * @param showMonogram Indica si el respaldo debe mostrar siglas o un icono compacto para tamanos reducidos.
 * @param contentScale Estrategia de escalado de imagen aplicada sobre AsyncImage.
 */
@Composable
fun TeamCrestImage(
    logoUrl: String?,
    teamName: String,
    abbreviation: String,
    modifier: Modifier = Modifier,
    teamId: String = "",
    contentDescription: String? = teamName,
    showMonogram: Boolean = true,
    contentScale: ContentScale = ContentScale.Fit
) {
    var hasLoadError by remember(logoUrl) { mutableStateOf(false) }

    if (!logoUrl.isNullOrBlank() && !hasLoadError) {
        AsyncImage(
            model = logoUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onError = { hasLoadError = true },
            modifier = modifier
        )
        return
    }

    val fallbackShapes = UlamaTheme.shapes.crestFallbackShapes
    val fallbackShape = remember(teamId, teamName, fallbackShapes) {
        val seed = teamId.ifBlank { teamName }
        val index = (seed.hashCode() and Int.MAX_VALUE) % fallbackShapes.size
        fallbackShapes[index]
    }
    val monogram = remember(abbreviation, teamName) {
        abbreviation.trim().ifBlank { teamName.trim() }.take(3).uppercase()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(fallbackShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = UlamaTheme.dimensions.borderThin,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = fallbackShape
            )
    ) {
        if (showMonogram && monogram.isNotBlank()) {
            Text(
                text = monogram,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = UlamaTheme.spacing.matrixGap)
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_soccer_ball),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(UlamaTheme.spacing.matrixGap)
            )
        }
    }
}

/**
 * Renderiza el emblema oficial de una competicion o un trofeo semantico cuando la API
 * no provee logotipo propio para el torneo o falla la descarga remota.
 *
 * @param logoUrl URL del logotipo de la liga segun el brillo del tema activo.
 * @param contentDescription Descripcion accesible del torneo para TalkBack.
 * @param modifier Modificador de dimensiones o ubicacion aplicado al icono.
 * @param fallbackTint Tinte aplicado sobre el icono de trofeo alternativo.
 */
@Composable
fun LeagueCrestImage(
    logoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var hasLoadError by remember(logoUrl) { mutableStateOf(false) }

    if (!logoUrl.isNullOrBlank() && !hasLoadError) {
        AsyncImage(
            model = logoUrl,
            contentDescription = contentDescription,
            onError = { hasLoadError = true },
            modifier = modifier
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_trophy),
            contentDescription = contentDescription,
            tint = fallbackTint,
            modifier = modifier
        )
    }
}
