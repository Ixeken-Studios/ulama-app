package com.ixeken.ulama

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade

import com.ixeken.ulama.core.network.ImageFallbackInterceptor
import com.ixeken.ulama.core.network.NetworkClient
import com.ixeken.ulama.service.MatchNotificationHelper

/**
 * Aplicacion principal de Ulama.
 * Se comparte el pool de conexiones y sockets HTTP/2 de NetworkClient con Coil 3 mediante newBuilder()
 * para ahorrar hilos en segundo plano y reutilizar sesiones TLS abiertas con los servidores de ESPN.
 */
class UlamaApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        MatchNotificationHelper.createNotificationChannels(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = NetworkClient.getOkHttpClient(this)
            .newBuilder()
            .cache(null)
            .addInterceptor(ImageFallbackInterceptor())
            .build()

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(128L * 1024 * 1024) // 128 MB
                    .build()
            }
            .crossfade(true)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
    }
}
