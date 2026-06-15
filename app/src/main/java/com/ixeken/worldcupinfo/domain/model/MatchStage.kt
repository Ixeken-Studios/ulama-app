package com.ixeken.worldcupinfo.domain.model

import com.ixeken.worldcupinfo.R

/**
 * Representa las diferentes fases o etapas de un partido en el torneo.
 */
enum class MatchStage(val displayNameResId: Int) {
    GROUPS(R.string.stage_groups),
    ROUND_OF_32(R.string.stage_dieciseisavos),
    ROUND_OF_16(R.string.stage_octavos),
    QUARTERFINALS(R.string.stage_cuartos),
    SEMIFINAL(R.string.stage_semis),
    FINAL(R.string.stage_final),
    THIRD_PLACE(R.string.stage_third_place)
}
