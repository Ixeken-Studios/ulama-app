package com.ixeken.ulama.core.network

import android.content.Context
import com.ixeken.ulama.data.espn.EspnApiService
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Proveedor de red optimizado para Ulama.
 * Instancia OkHttp con 10MB de cache en disco y Retrofit con serializacion estatica.
 */
object NetworkClient {

    private const val BASE_URL = "https://site.api.espn.com/apis/"
    private const val CACHE_SIZE = 10L * 1024 * 1024 // 10 Megabytes

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    private var okHttpClient: OkHttpClient? = null

    @Volatile
    private var espnApiService: EspnApiService? = null

    fun getOkHttpClient(context: Context): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: createOkHttpClient(context.applicationContext).also { okHttpClient = it }
        }
    }

    private fun createOkHttpClient(context: Context): OkHttpClient {
        val cacheDirectory = File(context.cacheDir, "espn_http_cache")
        val diskCache = Cache(cacheDirectory, CACHE_SIZE)
        // Elevamos el limite por host sobre HTTP/2 para evitar encolamientos hacia site.api.espn.com
        val dispatcher = Dispatcher().apply {
            maxRequestsPerHost = 12
        }

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .fastFallback(true)
            .cache(diskCache)
            .addInterceptor(CacheInterceptor(context.applicationContext))
            .addNetworkInterceptor(ResponseCacheInterceptor())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Inicializa anticipadamente el cliente HTTP/2 y el cache DNS de ESPN en segundo plano
     * durante la creacion del proceso para recortar latencia en la primera consulta de portada.
     */
    fun prewarm(context: Context) {
        createEspnApiService(context)
        runCatching { InetAddress.getAllByName("site.api.espn.com") }
    }

    fun createEspnApiService(context: Context): EspnApiService {
        return espnApiService ?: synchronized(this) {
            espnApiService ?: buildRetrofit(getOkHttpClient(context)).also { espnApiService = it }
        }
    }

    private fun buildRetrofit(client: OkHttpClient): EspnApiService {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(EspnApiService::class.java)
    }
}
