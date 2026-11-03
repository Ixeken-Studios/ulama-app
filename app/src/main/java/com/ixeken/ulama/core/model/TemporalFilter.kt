package com.ixeken.ulama.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.R

/**
 * Modelo inmutable para los tres filtros temporales de partidos.
 * Vincula identificadores de strings.xml para erradicar texto hardcodeado.
 */
@Immutable
enum class TemporalFilter(@param:StringRes val titleRes: Int) {
    PAST(R.string.tab_past),
    TODAY(R.string.tab_today),
    UPCOMING(R.string.tab_upcoming)
}
