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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.ixeken.ulama.R
import com.ixeken.ulama.ui.theme.UlamaTheme
import com.ixeken.ulama.ui.theme.rememberCrestFallbackShapes

/**
 * Renderiza el escudo de un club con respaldo geometrico basado en MaterialShapes.
 * Cuando el modo de ahorro de datos esta activo, deshabilita unicamente el acceso a red en Coil
 * para mostrar al instante cualquier imagen ya presente en RAM o disco y recurrir al monograma
 * expresivo en 0 ms si el escudo no estaba en cache.
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
    val context = LocalContext.current
    val isDataSaver = UlamaTheme.isDataSaver
    var hasLoadError by remember(logoUrl, isDataSaver) { mutableStateOf(false) }

    if (!logoUrl.isNullOrBlank() && !hasLoadError) {
        val imageRequest = remember(context, logoUrl, isDataSaver) {
            ImageRequest.Builder(context)
                .data(logoUrl)
                .networkCachePolicy(if (isDataSaver) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onError = { hasLoadError = true },
            modifier = modifier
        )
        return
    }

    // Solo teselamos las 6 formas expresivas cuando un club realmente requiere el respaldo geometrico.
    val fallbackShapes = rememberCrestFallbackShapes()
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
 * no provee logotipo propio, falla la descarga o el modo de ahorro de datos omite escudos no cacheados.
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
    val context = LocalContext.current
    val isDataSaver = UlamaTheme.isDataSaver
    var hasLoadError by remember(logoUrl, isDataSaver) { mutableStateOf(false) }

    if (!logoUrl.isNullOrBlank() && !hasLoadError) {
        val imageRequest = remember(context, logoUrl, isDataSaver) {
            ImageRequest.Builder(context)
                .data(logoUrl)
                .networkCachePolicy(if (isDataSaver) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .build()
        }
        AsyncImage(
            model = imageRequest,
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
