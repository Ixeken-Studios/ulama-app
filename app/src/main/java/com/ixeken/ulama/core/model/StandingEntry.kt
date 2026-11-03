package com.ixeken.ulama.core.model

import androidx.compose.runtime.Immutable

/**
 * Modelo inmutable para un registro en la tabla general de posiciones.
 * Contiene estadisticas de puntos, partidos y diferencia de goles.
 */
@Immutable
data class StandingEntry(
    val rank: Int,
    val teamName: String,
    val teamShortName: String,
    val teamAbbreviation: String,
    val logoUrl: String?,
    val gamesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val points: Int,
    val teamId: String = ""
) {
    /**
     * Resuelve la URL del escudo adaptada al modo oscuro sustituyendo el tamano base por 500-dark en ESPN.
     */
    fun getLogoUrl(isDark: Boolean): String? {
        if (logoUrl == null) return null
        return if (isDark) logoUrl.replace("/500/", "/500-dark/") else logoUrl
    }
}
