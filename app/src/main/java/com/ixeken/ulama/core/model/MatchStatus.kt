package com.ixeken.ulama.core.model

import androidx.compose.runtime.Immutable

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
