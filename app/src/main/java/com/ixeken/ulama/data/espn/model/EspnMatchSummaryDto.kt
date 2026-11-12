package com.ixeken.ulama.data.espn.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Estructuras DTO para el detalle del partido proveniente del endpoint summary de ESPN.
 * Se mapean exclusivamente los nodos necesarios para estadisticas, alineaciones y anotaciones,
 * ignorando campos especulativos segun el principio YAGNI.
 */
@Serializable
data class EspnMatchSummaryResponse(
    val boxscore: EspnBoxscoreDto? = null,
    val rosters: List<EspnRosterTeamDto> = emptyList(),
    val header: EspnSummaryHeaderDto? = null,
    val keyEvents: List<EspnSummaryScoringPlayDto> = emptyList(),
    val broadcasts: List<EspnBroadcastDto> = emptyList(),
    val gameInfo: EspnGameInfoDto? = null,
    val lastFiveGames: List<EspnLastFiveGamesDto> = emptyList(),
    val seasonseries: List<EspnSeasonSeriesDto> = emptyList(),
    val leaders: List<EspnTeamLeadersDto> = emptyList()
)

@Serializable
data class EspnBoxscoreDto(
    val teams: List<EspnBoxscoreTeamDto> = emptyList()
)

@Serializable
data class EspnBoxscoreTeamDto(
    val team: EspnTeamDto = EspnTeamDto(),
    val homeAway: String? = null,
    val statistics: List<EspnStatisticItemDto> = emptyList()
)

@Serializable
data class EspnStatisticItemDto(
    val name: String = "",
    val displayValue: String = "",
    val label: String = ""
)

@Serializable
data class EspnRosterTeamDto(
    val team: EspnTeamDto = EspnTeamDto(),
    val homeAway: String? = null,
    val formation: String? = null,
    val roster: List<EspnRosterPlayerItemDto> = emptyList()
)

@Serializable
data class EspnRosterPlayerItemDto(
    val starter: Boolean = false,
    val jersey: String? = null,
    val formationPlace: Int? = null,
    val position: EspnPlayerPositionDto? = null,
    val athlete: EspnAthleteDto? = null,
    val subbedIn: Boolean = false,
    val subbedInFor: EspnSubbedInForDto? = null,
    val plays: List<EspnPlayerPlayDto> = emptyList()
)

@Serializable
data class EspnSubbedInForDto(
    val jersey: String? = null,
    val athlete: EspnAthleteDto? = null
)

@Serializable
data class EspnPlayerPlayDto(
    val clock: EspnClockDisplayDto? = null,
    val substitution: Boolean = false,
    val scoringPlay: Boolean = false,
    val redCard: Boolean = false,
    val yellowCard: Boolean = false
)

@Serializable
data class EspnPlayerPositionDto(
    val name: String? = null,
    val displayName: String? = null,
    val abbreviation: String? = null
)

@Serializable
data class EspnAthleteDto(
    val id: String? = null,
    val displayName: String = "",
    val shortName: String? = null,
    val jersey: String? = null,
    val position: JsonElement? = null
) {
    /**
     * Extrae la posicion del futbolista tolerando tanto cadenas planas ("CD-R", "GK")
     * en jugadas y lideres como objetos estructurados en plantillas de alineacion.
     */
    fun extractPosition(): EspnPlayerPositionDto? {
        return when (val pos = position) {
            is JsonPrimitive -> if (pos.isString && pos.content.isNotBlank()) {
                EspnPlayerPositionDto(
                    name = pos.content,
                    abbreviation = pos.content.substringBefore("-").trim()
                )
            } else null
            is JsonObject -> EspnPlayerPositionDto(
                name = (pos["name"] as? JsonPrimitive)?.content,
                displayName = (pos["displayName"] as? JsonPrimitive)?.content,
                abbreviation = (pos["abbreviation"] as? JsonPrimitive)?.content
            )
            else -> null
        }
    }
}

@Serializable
data class EspnSummaryHeaderDto(
    val competitions: List<EspnSummaryCompetitionDto> = emptyList()
)

@Serializable
data class EspnSummaryCompetitionDto(
    val competitors: List<EspnCompetitorDto> = emptyList(),
    val details: List<EspnSummaryScoringPlayDto> = emptyList(),
    val status: EspnStatusDto? = null
)

@Serializable
data class EspnPlayTypeDto(
    val id: String? = null,
    val text: String? = null
)

@Serializable
data class EspnSummaryScoringPlayDto(
    val scoringPlay: Boolean = false,
    val penaltyKick: Boolean = false,
    val ownGoal: Boolean = false,
    val text: String? = null,
    val shortText: String? = null,
    val type: EspnPlayTypeDto? = null,
    val clock: EspnClockDisplayDto? = null,
    val team: EspnSummaryTeamRefDto? = null,
    val participants: List<EspnScoringParticipantDto> = emptyList(),
    val athletesInvolved: List<EspnAthleteDto> = emptyList()
)

@Serializable
data class EspnClockDisplayDto(
    val displayValue: String = ""
)

@Serializable
data class EspnSummaryTeamRefDto(
    val id: String = ""
)

@Serializable
data class EspnScoringParticipantDto(
    val athlete: EspnAthleteDto? = null
)

/**
 * Emisoras y canales de transmision asociados al encuentro (TV y Streaming).
 */
@Serializable
data class EspnBroadcastDto(
    val media: EspnBroadcastMediaDto? = null,
    val names: List<String> = emptyList()
)

@Serializable
data class EspnBroadcastMediaDto(
    val shortName: String? = null,
    val name: String? = null
)

/**
 * Metadatos generales del partido: asistencia, sede, arbitraje y estado del tiempo.
 */
@Serializable
data class EspnGameInfoDto(
    val attendance: Int? = null,
    val venue: EspnGameInfoVenueDto? = null,
    val officials: List<EspnOfficialDto> = emptyList(),
    val weather: EspnWeatherDto? = null
)

@Serializable
data class EspnGameInfoVenueDto(
    val fullName: String? = null,
    val address: EspnVenueAddressDto? = null
)

@Serializable
data class EspnVenueAddressDto(
    val city: String? = null,
    val country: String? = null
)

@Serializable
data class EspnOfficialDto(
    val displayName: String = "",
    val fullName: String? = null
)

@Serializable
data class EspnWeatherDto(
    val displayValue: String? = null,
    val temperature: Int? = null
)

/**
 * Racha de los ultimos partidos de cada equipo reportados por ESPN.
 */
@Serializable
data class EspnLastFiveGamesDto(
    val displayOrder: Int = 0,
    val team: EspnTeamDto? = null,
    val events: List<EspnFormEventDto> = emptyList()
)

@Serializable
data class EspnFormEventDto(
    val id: String = "",
    val gameDate: String? = null,
    val score: String? = null,
    val gameResult: String? = null,
    val opponent: EspnTeamDto? = null
)

/**
 * Historial de enfrentamientos directos cara a cara (Head-to-Head / seasonseries).
 */
@Serializable
data class EspnSeasonSeriesDto(
    val title: String? = null,
    val summary: String? = null,
    val completed: Boolean? = false,
    val events: List<EspnSeasonSeriesEventDto> = emptyList()
)

@Serializable
data class EspnSeasonSeriesEventDto(
    val id: String = "",
    val date: String? = null,
    val competitors: List<EspnSeasonSeriesCompetitorDto> = emptyList()
)

@Serializable
data class EspnSeasonSeriesCompetitorDto(
    val homeAway: String? = null,
    val winner: Boolean? = false,
    val score: String? = null,
    val team: EspnTeamDto? = null
)

/**
 * Lideres individuales de cada equipo en el encuentro (tiros, pases, intervenciones defensivas, atajadas).
 */
@Serializable
data class EspnTeamLeadersDto(
    val team: EspnTeamDto? = null,
    val leaders: List<EspnLeaderCategoryDto> = emptyList()
)

@Serializable
data class EspnLeaderCategoryDto(
    val name: String = "",
    val displayName: String = "",
    val leaders: List<EspnLeaderEntryDto> = emptyList()
)

@Serializable
data class EspnLeaderEntryDto(
    val displayValue: String = "",
    val athlete: EspnAthleteDto? = null,
    val summary: String? = null
)
