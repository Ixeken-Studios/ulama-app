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

    /**
     * Determina si el participante corresponde a una casilla plantilla aun por definir en un sorteo o llave
     * eliminatoria (ej. "Group C Winner", "1C", "3rd Place Group A", "RD16 W1", "TBD").
     * Se evalua de forma declarativa para impedir que llaves futuras sin clasificados reales se muestren
     * como equipos sin bandera en el cuadro eliminatorio o en la lista de proximos partidos.
     */
    val isPlaceholder: Boolean
        get() {
            val n = name.trim()
            val s = shortName.trim()
            if (n.isEmpty()) return true
            if (n.equals("TBD", ignoreCase = true) || n.equals("TBA", ignoreCase = true) ||
                s.equals("TBD", ignoreCase = true) || s.equals("TBA", ignoreCase = true) ||
                n.contains("Por definir", ignoreCase = true)
            ) return true
            val hasPlaceholderKeyword = n.contains("Winner", ignoreCase = true) ||
                n.contains("Loser", ignoreCase = true) ||
                n.contains("Ganador", ignoreCase = true) ||
                n.contains("Perdedor", ignoreCase = true) ||
                n.contains("Runner-up", ignoreCase = true) ||
                n.contains("Group ", ignoreCase = true) ||
                n.contains("Grupo ", ignoreCase = true) ||
                n.contains("Place ", ignoreCase = true) ||
                n.contains("Round of ", ignoreCase = true) ||
                n.contains("Quarterfinal", ignoreCase = true) ||
                n.contains("Semifinal", ignoreCase = true)
            if (hasPlaceholderKeyword) return true
            return logoUrl.isNullOrBlank() && PLACEHOLDER_CODE_REGEX.matches(s)
        }

    companion object {
        private val PLACEHOLDER_CODE_REGEX = Regex(
            pattern = "^([1-4][A-L]|3\\s*[A-L]{1,4}|(RD16|R16|R32|QF|SF|W|L)\\s*[W|L]?\\d+)$",
            option = RegexOption.IGNORE_CASE
        )
    }
}
