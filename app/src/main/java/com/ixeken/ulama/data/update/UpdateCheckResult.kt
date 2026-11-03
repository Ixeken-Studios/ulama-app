package com.ixeken.ulama.data.update

import androidx.compose.runtime.Immutable

/**
 * Estados resultantes de la evaluacion de actualizaciones contra GitHub Releases.
 * Implementa una jerarquia inmutable y sellada para aislar recomposiciones y modelar exhaustivamente cada escenario.
 */
@Immutable
sealed interface UpdateCheckResult {

    /**
     * No se detecto conexion a red activa o fallo la comunicacion por socket.
     */
    data object NoInternet : UpdateCheckResult

    /**
     * El repositorio no existe, se encuentra privado o devolvio codigo 404/403.
     */
    data object RepoNotFoundOrPrivate : UpdateCheckResult

    /**
     * La version instalada coincide plenamente con la version publica mas reciente.
     */
    data class UpToDate(val version: String) : UpdateCheckResult

    /**
     * Existe una version superior disponible en GitHub Releases lista para su descarga.
     */
    data class NewVersionAvailable(
        val version: String,
        val releaseUrl: String,
        val releaseNotes: String?
    ) : UpdateCheckResult

    /**
     * La version instalada es mayor a la publicada en GitHub, tipico en entornos de desarrollo local.
     */
    data class AheadOfRelease(
        val installedVersion: String,
        val releaseVersion: String
    ) : UpdateCheckResult
}
