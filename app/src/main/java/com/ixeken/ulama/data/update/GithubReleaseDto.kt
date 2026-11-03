package com.ixeken.ulama.data.update

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos inmutable para deserializar la informacion esencial de una publicacion en GitHub Releases.
 * Mantiene unicamente los campos indispensables para comparar versiones y enlazar la descarga sin dependencias pesadas.
 */
@Immutable
@Serializable
data class GithubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val name: String? = null,
    val body: String? = null
)
