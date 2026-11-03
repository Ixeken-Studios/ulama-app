package com.ixeken.ulama.core.network

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Pruebas unitarias para validar el redimensionado automatico a 128px en el Combiner de ESPN,
 * la recuperacion ante respuestas 404 en rutas oscuras y la memoria en cache de fallbacks.
 */
class ImageFallbackInterceptorTest {

    @Before
    fun setUp() {
        ImageFallbackInterceptor.clearMissingDarkCacheForTest()
    }

    @Test
    fun interceptor_retriesWithDefaultPathWhenDarkUrlReturns404_andMemoizesMissingDarkUrl() {
        val interceptor = ImageFallbackInterceptor()
        val requestedUrls = mutableListOf<String>()

        fun createChain(initialUrl: String) = object : Interceptor.Chain {
            private var currentRequest = Request.Builder()
                .url(initialUrl)
                .build()

            override fun request(): Request = currentRequest

            override fun proceed(request: Request): Response {
                currentRequest = request
                requestedUrls.add(request.url.toString())

                return if (request.url.toString().contains("/500-dark/")) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(404)
                        .message("Not Found")
                        .body("".toResponseBody())
                        .build()
                } else {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("image-data".toResponseBody())
                        .build()
                }
            }

            override fun connection(): Connection? = null
            override fun call(): Call = throw NotImplementedError()
            override fun connectTimeoutMillis(): Int = 0
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 0
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 0
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        val response1 = interceptor.intercept(createChain("https://a.espncdn.com/i/teamlogos/soccer/500-dark/388.png"))

        assertEquals(200, response1.code)
        assertEquals(2, requestedUrls.size)
        assertEquals(
            "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500-dark/388.png&w=128&h=128",
            requestedUrls[0]
        )
        assertEquals(
            "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/388.png&w=128&h=128",
            requestedUrls[1]
        )

        // Segunda llamada al mismo escudo: debe ir directo a /500/ sin pasar por el 404 de /500-dark/
        val response2 = interceptor.intercept(createChain("https://a.espncdn.com/i/teamlogos/soccer/500-dark/388.png"))
        assertEquals(200, response2.code)
        assertEquals(3, requestedUrls.size)
        assertEquals(
            "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/388.png&w=128&h=128",
            requestedUrls[2]
        )
    }

    @Test
    fun interceptor_doesNotRetryWhenDarkUrlSucceeds() {
        val interceptor = ImageFallbackInterceptor()
        val requestedUrls = mutableListOf<String>()

        val fakeChain = object : Interceptor.Chain {
            private var currentRequest = Request.Builder()
                .url("https://a.espncdn.com/i/teamlogos/soccer/500-dark/223.png")
                .build()

            override fun request(): Request = currentRequest

            override fun proceed(request: Request): Response {
                currentRequest = request
                requestedUrls.add(request.url.toString())

                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("image-data".toResponseBody())
                    .build()
            }

            override fun connection(): Connection? = null
            override fun call(): Call = throw NotImplementedError()
            override fun connectTimeoutMillis(): Int = 0
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 0
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 0
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        val response = interceptor.intercept(fakeChain)

        assertEquals(200, response.code)
        assertEquals(1, requestedUrls.size)
        assertEquals(
            "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500-dark/223.png&w=128&h=128",
            requestedUrls[0]
        )
    }

    @Test
    fun interceptor_doesNotRetryNonDarkUrlWhenFailing() {
        val interceptor = ImageFallbackInterceptor()
        val requestedUrls = mutableListOf<String>()

        val fakeChain = object : Interceptor.Chain {
            private var currentRequest = Request.Builder()
                .url("https://a.espncdn.com/i/teamlogos/soccer/500/99999.png")
                .build()

            override fun request(): Request = currentRequest

            override fun proceed(request: Request): Response {
                currentRequest = request
                requestedUrls.add(request.url.toString())

                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("".toResponseBody())
                    .build()
            }

            override fun connection(): Connection? = null
            override fun call(): Call = throw NotImplementedError()
            override fun connectTimeoutMillis(): Int = 0
            override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun readTimeoutMillis(): Int = 0
            override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
            override fun writeTimeoutMillis(): Int = 0
            override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        }

        val response = interceptor.intercept(fakeChain)

        assertEquals(404, response.code)
        assertEquals(1, requestedUrls.size)
        assertEquals(
            "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/99999.png&w=128&h=128",
            requestedUrls[0]
        )
    }
}
