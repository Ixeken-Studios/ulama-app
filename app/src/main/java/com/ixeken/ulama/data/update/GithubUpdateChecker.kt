package com.ixeken.ulama.data.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ixeken.ulama.core.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Servicio de verificacion de versiones contra la API publica de GitHub Releases.
 * Aplica la filosofia Ponytail reutilizando el cliente OkHttp central y serializadores estaticos sin dependencias pesadas.
 *
 * @property context Contexto de la aplicacion utilizado para acceder al gestor de conectividad y al gestor de paquetes.
 * @property clientProvider Factoria para obtener la instancia de OkHttpClient, permitiendo inyeccion en pruebas unitarias.
 * @property installedVersionProvider Proveedor de la version instalada localmente para facilitar pruebas deterministas.
 */
class GithubUpdateChecker(
    private val context: Context? = null,
    private val clientProvider: () -> OkHttpClient = {
        checkNotNull(context) { "Context is required for default OkHttpClient" }
        NetworkClient.getOkHttpClient(context)
    },
    private val installedVersionProvider: () -> String = {
        context?.let { getInstalledVersionName(it) } ?: "2.0"
    }
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Consulta la API de GitHub Releases y evalua la version remota contra la instalada.
     * Ejecuta el trabajo de red y deserializacion en el despachador de E/S.
     *
     * @return [UpdateCheckResult] con uno de los 5 estados posibles.
     */
    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext UpdateCheckResult.NoInternet
        }

        val request = Request.Builder()
            .url(GITHUB_RELEASES_API)
            .header("User-Agent", "Ulama-Android-App")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            val response = clientProvider().newCall(request).execute()
            response.use { resp ->
                when (resp.code) {
                    403, 404 -> UpdateCheckResult.RepoNotFoundOrPrivate
                    in 200..299 -> {
                        val bodyString = resp.body?.string().orEmpty()
                        if (bodyString.isBlank()) {
                            UpdateCheckResult.RepoNotFoundOrPrivate
                        } else {
                            val release = json.decodeFromString<GithubReleaseDto>(bodyString)
                            evaluateRelease(release)
                        }
                    }
                    else -> UpdateCheckResult.RepoNotFoundOrPrivate
                }
            }
        } catch (_: IOException) {
            UpdateCheckResult.NoInternet
        } catch (_: Exception) {
            UpdateCheckResult.RepoNotFoundOrPrivate
        }
    }

    /**
     * Evalua una publicacion remota comparando su etiqueta de version contra la version instalada.
     */
    internal fun evaluateRelease(release: GithubReleaseDto): UpdateCheckResult {
        val installedVersion = installedVersionProvider()
        val releaseTag = release.tagName.ifBlank { release.name.orEmpty() }
        val releaseVersionClean = releaseTag.trim().removePrefix("v").removePrefix("V")

        val comparison = compareSemVer(installedVersion, releaseTag)
        return when {
            comparison < 0 -> UpdateCheckResult.NewVersionAvailable(
                version = releaseVersionClean,
                releaseUrl = release.htmlUrl.ifBlank { GITHUB_REPO_URL },
                releaseNotes = release.body
            )
            comparison > 0 -> UpdateCheckResult.AheadOfRelease(
                installedVersion = installedVersion,
                releaseVersion = releaseVersionClean
            )
            else -> UpdateCheckResult.UpToDate(version = installedVersion)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        const val GITHUB_REPO_URL = "https://github.com/Ixeken-Studios/ulama-app"
        const val GITHUB_RELEASES_API = "https://api.github.com/repos/Ixeken-Studios/ulama-app/releases/latest"

        /**
         * Compara dos cadenas de version bajo la especificacion SemVer simplificada.
         * Desprende prefijos comunes como 'v' o 'V' y evalua numericamente segmento por segmento.
         *
         * @param v1 Version instalada local.
         * @param v2 Version remota o de referencia.
         * @return Entero negativo si v1 < v2, 0 si son iguales, o entero positivo si v1 > v2.
         */
        fun compareSemVer(v1: String, v2: String): Int {
            val clean1 = v1.trim().removePrefix("v").removePrefix("V")
            val clean2 = v2.trim().removePrefix("v").removePrefix("V")

            val parts1 = clean1.split('.').map { segment ->
                segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }
            val parts2 = clean2.split('.').map { segment ->
                segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }

            val maxLength = maxOf(parts1.size, parts2.size)
            for (i in 0 until maxLength) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) {
                    return p1.compareTo(p2)
                }
            }
            return 0
        }

        private fun getInstalledVersionName(context: Context): String {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "2.0"
            } catch (_: Exception) {
                "2.0"
            }
        }
    }
}
