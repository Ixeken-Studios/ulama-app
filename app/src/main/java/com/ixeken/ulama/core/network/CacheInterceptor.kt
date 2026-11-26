package com.ixeken.ulama.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Interceptor de red para almacenamiento en disco y soporte fuera de linea.
 * Aplica el principio Ponytail aprovechando el cache nativo de OkHttp sin sobrecargar el sistema con SQLite o Room.
 */
class CacheInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // Las sondas explicitas a disco omiten la consulta de red del sistema y responden al instante.
        if (request.cacheControl.onlyIfCached) {
            return chain.proceed(request)
        }

        if (!isOnline(context)) {
            val cacheControl = CacheControl.Builder()
                .onlyIfCached()
                .maxStale(7, TimeUnit.DAYS)
                .build()
            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        }

        return chain.proceed(request)
    }

    companion object {
        /**
         * Directiva para semilla instantanea en arranque en frio desde el cache HTTP de OkHttp sin abrir red.
         */
        const val CACHE_CONTROL_DISK_SEED = "only-if-cached, max-stale=604800"
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Interceptor de nivel de red que sobreescribe las cabeceras de respuesta del servidor de ESPN.
 * Se asignan ventanas de frescura diferenciadas por endpoint porque las tablas de posiciones solo cambian al terminar
 * un partido y los resumenes concluidos son inmutables, evitando re-descargar cientos de kilobytes cada minuto.
 */
class ResponseCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val path = request.url.encodedPath
        val cacheControlHeader = when {
            path.contains("/standings") -> "public, max-age=900, stale-while-revalidate=3600"
            else -> "public, max-age=60, stale-while-revalidate=300"
        }
        return response.newBuilder()
            .removeHeader("Pragma")
            .removeHeader("Cache-Control")
            .header("Cache-Control", cacheControlHeader)
            .build()
    }
}
