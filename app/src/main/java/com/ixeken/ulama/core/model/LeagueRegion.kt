package com.ixeken.ulama.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.R

/**
 * Define las regiones geograficas para filtrar competiciones de forma declarativa.
 * Permite segmentar torneos por continente o alcance mundial sin condiciones por codigo.
 */
@Immutable
enum class LeagueRegion(
    @param:StringRes val labelRes: Int
) {
    ALL(R.string.region_all),
    GLOBAL(R.string.region_global),
    AMERICAS(R.string.region_americas),
    EUROPE(R.string.region_europe),
    AFRICA(R.string.region_africa),
    ASIA(R.string.region_asia),
    OCEANIA(R.string.region_oceania);

    companion object {
        /**
         * Regiones continentales interactivas representadas directamente en el mapa vectorial.
         */
        val continents: List<LeagueRegion> = listOf(AMERICAS, EUROPE, AFRICA, ASIA, OCEANIA)
    }
}
