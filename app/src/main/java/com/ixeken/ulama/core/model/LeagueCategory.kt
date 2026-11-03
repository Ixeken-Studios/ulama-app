package com.ixeken.ulama.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.R

/**
 * Categoria organizativa de competiciones de futbol en Ulama.
 * Permite segmentar limpiamente entre torneos de clubes domesticos/continentales
 * y campeonatos de selecciones nacionales internacionales.
 *
 * @param titleRes Recurso de texto localizado para la visualizacion de la categoria.
 */
@Immutable
enum class LeagueCategory(@param:StringRes val titleRes: Int) {
    CLUBS(titleRes = R.string.league_category_clubs),
    INTERNATIONAL(titleRes = R.string.league_category_international)
}
