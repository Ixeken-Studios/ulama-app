package com.ixeken.ulama.data.espn.model

import kotlinx.serialization.Serializable

/**
 * Estructuras DTO serializables para la tabla general de posiciones de ESPN.
 */
@Serializable
data class EspnStandingsResponse(
    val children: List<EspnStandingsChildDto> = emptyList()
)

@Serializable
data class EspnStandingsChildDto(
    val name: String = "",
    val abbreviation: String? = null,
    val standings: EspnStandingsEntriesDto? = null
)

@Serializable
data class EspnStandingsEntriesDto(
    val entries: List<EspnStandingsEntryDto> = emptyList()
)

@Serializable
data class EspnStandingsEntryDto(
    val team: EspnStandingsTeamDto,
    val stats: List<EspnStatDto> = emptyList()
)

@Serializable
data class EspnStandingsTeamDto(
    val id: String,
    val name: String = "",
    val displayName: String = "",
    val shortDisplayName: String? = null,
    val abbreviation: String? = null,
    val logos: List<EspnLogoDto> = emptyList()
)

@Serializable
data class EspnLogoDto(
    val href: String
)

@Serializable
data class EspnStatDto(
    val name: String = "",
    val value: Double? = null,
    val displayValue: String? = null
)
