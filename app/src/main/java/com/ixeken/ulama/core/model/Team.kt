package com.ixeken.ulama.core.model

import androidx.compose.runtime.Immutable

/**
 * Modelo inmutable de un club deportivo en un encuentro.
 * Facilita el renderizado con escudos y evita recomposiciones innecesarias en la UI.
 */
@Immutable
data class Team(
    val id: String,
    val name: String,
    val shortName: String,
    val abbreviation: String,
    val logoUrl: String?,
    val isHome: Boolean,
    val score: Int?,
    val isWinner: Boolean,
    val record: String? = null,
    val color: String? = null,
    val alternateColor: String? = null,
    val shootoutScore: Int? = null
) {
    /**
     * Resuelve la URL del escudo adaptada al modo oscuro sustituyendo el tamano base por 500-dark en ESPN.
     */
    fun getLogoUrl(isDark: Boolean): String? {
        if (logoUrl == null) return null
        return if (isDark) logoUrl.replace("/500/", "/500-dark/") else logoUrl
    }
}
