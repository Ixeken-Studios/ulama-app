package com.ixeken.ulama.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

/**
 * Interceptor de red para optimizar la descarga de escudos de ESPN y recuperar variantes oscuras ausentes.
 *
 * Se decidio reescribir en vuelo las rutas base de 500x500 pixeles hacia el servicio Combiner de ESPN a 128x128 pixeles
 * porque la interfaz nunca muestra escudos mayores a 52dp, logrando reducir el peso de cada imagen de 130 KB a menos de 10 KB
 * sin modificar las claves de cache de Coil ni los modelos de dominio. Ademas, memoriza en RAM los clubes que devuelven 404
 * en modo oscuro para evitar repetir llamadas fallidas en el futuro.
 */
class ImageFallbackInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url.toString()

        val resolvedUrl = if (originalUrl.contains(DARK_LOGO_SEGMENT) &&
            missingDarkLogoUrls.contains(originalUrl)
        ) {
            originalUrl.replace(DARK_LOGO_SEGMENT, DEFAULT_LOGO_SEGMENT)
        } else {
            originalUrl
        }

        val optimizedUrl = toCombinerUrl(resolvedUrl)
        val request = if (optimizedUrl != originalUrl) {
            originalRequest.newBuilder().url(optimizedUrl).build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        if (!response.isSuccessful && resolvedUrl.contains(DARK_LOGO_SEGMENT)) {
            response.close()
            missingDarkLogoUrls.add(originalUrl)
            val fallbackRawUrl = resolvedUrl.replace(DARK_LOGO_SEGMENT, DEFAULT_LOGO_SEGMENT)
            val fallbackRequest = originalRequest.newBuilder()
                .url(toCombinerUrl(fallbackRawUrl))
                .build()
            return chain.proceed(fallbackRequest)
        }

        return response
    }

    companion object {
        private const val DARK_LOGO_SEGMENT = "/500-dark/"
        private const val DEFAULT_LOGO_SEGMENT = "/500/"
        private const val ESPN_RAW_PREFIX = "https://a.espncdn.com/i/"
        private const val DEFAULT_LOGO_SIZE_PX = 128

        private val missingDarkLogoUrls = ConcurrentHashMap.newKeySet<String>()

        /**
         * Transforma una URL cruda de 500px del CDN de ESPN a una peticion dimensionada en su servicio Combiner.
         * Se eligio 128px por cubrir con nitidez el tamano maximo de 52dp en pantallas de alta densidad ahorrando mas del 90% de datos.
         */
        fun toCombinerUrl(url: String, sizePx: Int = DEFAULT_LOGO_SIZE_PX): String {
            if (!url.startsWith(ESPN_RAW_PREFIX) || url.contains("/combiner/i?")) return url
            val path = url.removePrefix("https://a.espncdn.com")
            return "https://a.espncdn.com/combiner/i?img=$path&w=$sizePx&h=$sizePx"
        }

        /**
         * Limpia el registro en memoria de URLs oscuras inexistentes para garantizar aislamiento en pruebas unitarias.
         */
        internal fun clearMissingDarkCacheForTest() {
            missingDarkLogoUrls.clear()
        }
    }
}
