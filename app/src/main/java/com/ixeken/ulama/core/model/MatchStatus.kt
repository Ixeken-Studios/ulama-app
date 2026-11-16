package com.ixeken.ulama.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.R
import kotlinx.serialization.Serializable

/**
 * Estados canonicos de un encuentro deportivo.
 */
@Serializable
enum class MatchState {
    SCHEDULED,
    LIVE,
    FINISHED
}

/**
 * Modelo inmutable del reloj y estado de un partido.
 * Garantiza Strong Skipping en Jetpack Compose al evitar mutabilidad interna.
 */
@Immutable
data class MatchStatus(
    val state: MatchState,
    val displayClock: String,
    val detail: String,
    val isCompleted: Boolean,
    val isHalftime: Boolean = false,
    val isExtraTime: Boolean = false,
    val isShootout: Boolean = false,
    val isFinishedAet: Boolean = false,
    val isFinishedPenalties: Boolean = false
)

/**
 * Determina el sufijo localizado de tiempo extra cuando el reloj remoto no incluye indicador.
 */
@StringRes
fun resolveExtraTimeSuffixRes(clock: String): Int? {
    if (clock.contains("TE", ignoreCase = true) || clock.contains("ET", ignoreCase = true)) {
        return null
    }
    val mins = clock.filter { it.isDigit() }.toIntOrNull() ?: 90
    return if (mins > 105) R.string.match_extra_time_second_suffix else R.string.match_extra_time_first_suffix
}
