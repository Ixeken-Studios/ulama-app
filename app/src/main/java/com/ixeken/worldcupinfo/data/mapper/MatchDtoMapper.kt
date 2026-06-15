package com.ixeken.worldcupinfo.data.mapper

import com.ixeken.worldcupinfo.data.database.entities.MatchEntity
import com.ixeken.worldcupinfo.data.remote.dto.RemoteMatchDto
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object MatchDtoMapper {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val mexicoCityZone = ZoneId.of("America/Mexico_City")
    private val timeOffsetRegex = Regex("""(\d{2}:\d{2})\s+UTC([+-]\d+)""")

    fun mapToEntity(dto: RemoteMatchDto, index: Int): MatchEntity {
        // Analizar la hora y su zona horaria (ej: "18:00 UTC-7" o "13:00 UTC-6")
        val match = timeOffsetRegex.find(dto.time)
        val unixTimestamp = if (match != null) {
            val timePart = match.groupValues[1]
            val offsetPart = match.groupValues[2].toInt()
            val sign = if (offsetPart >= 0) "+" else "-"
            val absOffset = Math.abs(offsetPart)
            val offsetString = String.format("%s%02d:00", sign, absOffset)
            val isoString = "${dto.date}T${timePart}:00${offsetString}"
            java.time.OffsetDateTime.parse(isoString).toInstant().epochSecond
        } else {
            // Fallback usando America/Mexico_City si no coincide la regex
            val cleanTime = if (dto.time.length >= 5) dto.time.substring(0, 5) else dto.time
            val dateTimeString = "${dto.date} $cleanTime"
            val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
            val zdt = localDateTime.atZone(mexicoCityZone)
            zdt.toInstant().epochSecond
        }

        val stageMapped = when (dto.round) {
            "Round of 32" -> "ROUND_OF_32"
            "Round of 16" -> "ROUND_OF_16"
            "Quarter-final", "Quarter-finals" -> "QUARTERFINALS"
            "Semi-final", "Semi-finals" -> "SEMIFINAL"
            "Match for third place" -> "THIRD_PLACE"
            "Final" -> "FINAL"
            else -> "GROUPS"
        }

        val isFinished = dto.score?.ft != null && dto.score.ft.size >= 2
        val statusVal = if (isFinished) "FINISHED" else "SCHEDULED"

        val goalsA = if (isFinished) dto.score?.ft?.getOrNull(0) else null
        val goalsB = if (isFinished) dto.score?.ft?.getOrNull(1) else null

        val teamACode = getTeamCode(dto.team1)
        val teamBCode = getTeamCode(dto.team2)

        val eventsA = dto.goals1?.map {
            com.ixeken.worldcupinfo.domain.model.MatchEvent(
                name = it.name,
                minute = it.minute,
                type = com.ixeken.worldcupinfo.domain.model.MatchEventType.GOAL
            )
        } ?: emptyList()
        val eventsB = dto.goals2?.map {
            com.ixeken.worldcupinfo.domain.model.MatchEvent(
                name = it.name,
                minute = it.minute,
                type = com.ixeken.worldcupinfo.domain.model.MatchEventType.GOAL
            )
        } ?: emptyList()

        val gson = com.google.gson.Gson()
        val eventsDetailsA = gson.toJson(eventsA)
        val eventsDetailsB = gson.toJson(eventsB)

        return MatchEntity(
            id = "match_${index + 1}",
            dateUnixTimestamp = unixTimestamp,
            teamA = teamACode,
            teamB = teamBCode,
            stadium = dto.ground ?: "",
            status = statusVal,
            stage = stageMapped,
            isAlarmActive = false,
            goalsA = goalsA,
            goalsB = goalsB,
            group = dto.group,
            eventsDetailsA = eventsDetailsA,
            eventsDetailsB = eventsDetailsB
        )
    }

    fun mapList(dtos: List<RemoteMatchDto>): List<MatchEntity> {
        return dtos.mapIndexed { index, dto -> mapToEntity(dto, index) }
    }

    private fun getTeamCode(teamName: String): String {
        val normalized = teamName.trim().lowercase()
        return when (normalized) {
            "mexico" -> "MEX"
            "south africa" -> "RSA"
            "south korea", "korea republic" -> "KOR"
            "czech republic", "czechia" -> "CZE"
            "canada" -> "CAN"
            "bosnia & herzegovina", "bosnia and herzegovina" -> "BIH"
            "united states", "united states of america", "usa" -> "USA"
            "paraguay" -> "PAR"
            "qatar" -> "QAT"
            "switzerland" -> "SUI"
            "brazil" -> "BRA"
            "morocco" -> "MAR"
            "haiti" -> "HAI"
            "scotland" -> "SCO"
            "australia" -> "AUS"
            "turkey", "türkiye" -> "TUR"
            "germany" -> "GER"
            "curacao", "curaçao" -> "CUW"
            "netherlands" -> "NED"
            "japan" -> "JPN"
            "argentina" -> "ARG"
            "france" -> "FRA"
            "spain" -> "ESP"
            "england" -> "ENG"
            "wales" -> "WLS"
            "colombia" -> "COL"
            "costa rica" -> "CRC"
            "ecuador" -> "ECU"
            "uruguay" -> "URY"
            "chile" -> "CHL"
            "peru" -> "PER"
            "venezuela" -> "VEN"
            "bolivia" -> "BOL"
            "italy" -> "ITA"
            "sweden" -> "SWE"
            "ukr", "ukraine" -> "UKR"
            "cmr", "cameroon" -> "CMR"
            "sen", "senegal" -> "SEN"
            "gha", "ghana" -> "GHA"
            "tun", "tunisia" -> "TUN"
            "algeria" -> "DZA"
            "egypt" -> "EGY"
            "nigeria" -> "NGA"
            "civ", "ivory coast", "cote d'ivoire" -> "CIV"
            "bel", "belgium" -> "BEL"
            "portugal" -> "POR"
            "croatia" -> "CRO"
            "iran" -> "IRN"
            "saudi arabia" -> "SAU"
            "denmark" -> "DEN"
            "poland" -> "POL"
            "austria" -> "AUT"
            "cape verde" -> "CPV"
            "dr congo" -> "COD"
            "iraq" -> "IRQ"
            "jordan" -> "JOR"
            "new zealand" -> "NZL"
            "norway" -> "NOR"
            "panama" -> "PAN"
            "uzbekistan" -> "UZB"
            else -> teamName.uppercase().take(3)
        }
    }
}
