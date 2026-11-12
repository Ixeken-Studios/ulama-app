package com.ixeken.ulama.data.espn.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Estructuras DTO serializables para la respuesta de marcadores de ESPN.
 * Procesadas de forma estatica con Kotlinx Serialization sin reflexion en tiempo de ejecucion.
 */

/**
 * Metadatos de la temporada activa reportados a nivel de liga por la API de ESPN.
 * Permite identificar los limites temporales del certamen vigente para descartar
 * encuentros historicos de temporadas previas en torneos multietapa o de eliminacion.
 */
@Serializable
data class EspnLeagueSeasonDto(
    val year: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val displayName: String? = null
)

@Serializable
data class EspnLeagueCalendarDto(
    val calendar: List<JsonElement> = emptyList(),
    val season: EspnLeagueSeasonDto? = null
) {
    /**
     * Extrae de forma recursiva las cadenas de fechas ISO del calendario, soportando tanto
     * arreglos planos de cadenas como estructuras jerarquicas de objetos con entries.
     */
    fun extractDateStrings(): List<String> {
        val result = mutableListOf<String>()
        fun extractFrom(element: JsonElement) {
            when (element) {
                is JsonPrimitive -> if (element.isString && element.content.isNotBlank()) {
                    result.add(element.content)
                }
                is JsonArray -> element.forEach { extractFrom(it) }
                is JsonObject -> {
                    val entries = element["entries"]
                    if (entries != null) {
                        extractFrom(entries)
                    } else {
                        val startVal = (element["startDate"] as? JsonPrimitive)?.content
                        val endVal = (element["endDate"] as? JsonPrimitive)?.content
                        val valueVal = (element["value"] as? JsonPrimitive)?.content
                        val isSingleDay = endVal == null || (startVal != null && startVal.take(10) == endVal.take(10))
                        val resolved = when {
                            !valueVal.isNullOrBlank() && valueVal.length >= 10 && valueVal[0].isDigit() -> valueVal
                            isSingleDay && !startVal.isNullOrBlank() && startVal.length >= 10 && startVal[0].isDigit() -> startVal
                            else -> null
                        }
                        if (resolved != null) {
                            result.add(resolved)
                        }
                    }
                }
            }
        }
        calendar.forEach { extractFrom(it) }
        return result
    }
}

@Serializable
data class EspnScoreboardResponse(
    val leagues: List<EspnLeagueCalendarDto> = emptyList(),
    val events: List<EspnEventDto> = emptyList()
)

@Serializable
data class EspnSeasonDto(
    val year: Int? = null,
    val type: Int? = null,
    val slug: String? = null
)

@Serializable
data class EspnEventDto(
    val id: String = "",
    val date: String = "",
    val name: String = "",
    val season: EspnSeasonDto? = null,
    val competitions: List<EspnCompetitionDto> = emptyList(),
    val status: EspnStatusDto? = null
)

@Serializable
data class EspnCompetitionDto(
    val id: String = "",
    val date: String = "",
    val competitors: List<EspnCompetitorDto> = emptyList(),
    val details: List<EspnSummaryScoringPlayDto> = emptyList(),
    val status: EspnStatusDto? = null,
    val venue: EspnVenueDto? = null,
    val leg: EspnLegDto? = null,
    val series: EspnSeriesDto? = null,
    val notes: List<EspnNoteDto> = emptyList()
)

/**
 * Representa la vuelta o partido dentro de una serie eliminatoria de ida y vuelta.
 * Provee el numero de encuentro y la etiqueta oficial devuelta por la API de ESPN.
 */
@Serializable
data class EspnLegDto(
    val value: Int? = null,
    val displayValue: String? = null
)

/**
 * Estructura de eliminatoria directa para torneos por fases como Champions League o Liguilla.
 * Agrupa el estado de la llave, el titulo de la ronda y los marcadores agregados acumulados.
 */
@Serializable
data class EspnSeriesDto(
    val title: String? = null,
    val completed: Boolean = false,
    val totalCompetitions: Int? = null,
    val competitors: List<EspnSeriesCompetitorDto> = emptyList()
)

/**
 * Informacion de puntaje acumulado de un club en una serie de eliminacion directa.
 */
@Serializable
data class EspnSeriesCompetitorDto(
    val id: String = "",
    val aggregateScore: Double? = null,
    val winner: Boolean = false
)

/**
 * Notas descriptivas del evento emitidas por ESPN como el resultado global o definicion de llave.
 */
@Serializable
data class EspnNoteDto(
    val text: String? = null,
    val headline: String? = null
)

@Serializable
data class EspnLinescoreDto(
    val value: Double? = null,
    val displayValue: String? = null,
    val period: Int? = null
)

@Serializable
data class EspnCompetitorDto(
    val id: String = "",
    val homeAway: String = "home",
    val winner: Boolean = false,
    val score: String? = null,
    val shootoutScore: Int? = null,
    val linescores: List<EspnLinescoreDto> = emptyList(),
    val team: EspnTeamDto = EspnTeamDto(),
    val records: List<EspnRecordDto> = emptyList()
)

@Serializable
data class EspnRecordDto(
    val name: String? = null,
    val type: String? = null,
    val summary: String? = null
)

@Serializable
data class EspnTeamDto(
    val id: String = "",
    val name: String = "",
    val displayName: String = "",
    val shortDisplayName: String? = null,
    val abbreviation: String? = null,
    val logo: String? = null,
    val color: String? = null,
    val alternateColor: String? = null
)

@Serializable
data class EspnStatusDto(
    val clock: Double? = 0.0,
    val displayClock: String? = "",
    val period: Int? = 1,
    val type: EspnStatusTypeDto? = null
)

@Serializable
data class EspnStatusTypeDto(
    val id: String = "1",
    val name: String = "",
    val state: String = "pre",
    val completed: Boolean = false,
    val description: String? = "",
    val detail: String? = "",
    val shortDetail: String? = ""
)

@Serializable
data class EspnVenueDto(
    val fullName: String? = null
)
