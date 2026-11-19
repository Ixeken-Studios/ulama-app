package com.ixeken.ulama.core.network

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Pruebas unitarias para validar el redimensionado automatico a 128px en el Combiner de ESPN,
 * la recuperacion ante respuestas 404 en rutas oscuras y la memoria en cache de fallbacks.
 */
class ImageFallbackInterceptorTest {

    @Before
    fun setUp() {
        ImageFallbackInterceptor.clearMissingDarkCacheForTest()
    }

    private fun fakeChain(
        initialUrl: String,
        requestedUrls: MutableList<String>,
        respond: (Request) -> Response
    ): Interceptor.Chain {
        var currentRequest = Request.Builder().url(initialUrl).build()
        return Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java)
        ) { _, method, args ->
            when (method.name) {
                "request" -> currentRequest
                "proceed" -> {
                    currentRequest = args[0] as Request
                    requestedUrls.add(currentRequest.url.toString())
                    respond(currentRequest)
                }
                else -> throw UnsupportedOperationException("Metodo no soportado en prueba: ${method.name}")
            }
        } as Interceptor.Chain
    }

    private fun buildResponse(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Not Found")
            .body(body.toResponseBody())
            .build()

    @Test
    fun interceptor_retriesWithDefaultPathWhenDarkUrlReturns404_andMemoizesMissingDarkUrl() {
        val interceptor = ImageFallbackInterceptor()
        val requestedUrls = mutableListOf<String>()
        val respond: (Request) -> Response = { request ->
            if (request.url.toString().contains("/500-dark/")) {
                buildResponse(request, 404, "")
            } else {
                buildResponse(request, 200, "image-data")
            }
        }

        val response1 = interceptor.intercept(
            fakeChain("https://a.espncdn.com/i/teamlogos/soccer/500-dark/388.png", requestedUrls, respond)
        )

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
        val response2 = interceptor.intercept(
            fakeChain("https://a.espncdn.com/i/teamlogos/soccer/500-dark/388.png", requestedUrls, respond)
        )
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

        val response = interceptor.intercept(
            fakeChain("https://a.espncdn.com/i/teamlogos/soccer/500-dark/223.png", requestedUrls) { request ->
                buildResponse(request, 200, "image-data")
            }
        )

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

        val response = interceptor.intercept(
            fakeChain("https://a.espncdn.com/i/teamlogos/soccer/500/99999.png", requestedUrls) { request ->
                buildResponse(request, 404, "")
            }
        )

        assertEquals(404, response.code)
        assertEquals(1, requestedUrls.size)
        assertEquals(
            "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/99999.png&w=128&h=128",
            requestedUrls[0]
        )
    }
}
