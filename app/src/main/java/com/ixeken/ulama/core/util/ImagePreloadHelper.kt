package com.ixeken.ulama.core.util

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import java.util.concurrent.ConcurrentHashMap

/**
 * Utilidad de precarga anticipada de escudos de clubes en Coil 3.
 * Se registra cada URL en un conjunto en memoria para evitar reconstruir y encolar peticiones
 * repetidas cada vez que la lista de partidos se filtra o actualiza en segundo plano.
 */
object ImagePreloadHelper {

    private val preloadedUrls = ConcurrentHashMap.newKeySet<String>()

    /**
     * Encola la precarga de una coleccion de URLs en el cargador singleton omitiendo las ya procesadas en la sesion.
     */
    fun preloadImages(context: Context, urls: Collection<String?>) {
        val pendingUrls = urls.asSequence()
            .filterNotNull()
            .filter { it.isNotBlank() && preloadedUrls.add(it) }
            .toList()
        if (pendingUrls.isEmpty()) return

        val imageLoader = SingletonImageLoader.get(context)
        for (url in pendingUrls) {
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(request)
        }
    }
}
