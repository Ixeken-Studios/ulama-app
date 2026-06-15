package com.ixeken.worldcupinfo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EspnScoreboardResponse(
    @SerializedName("events") val events: List<EspnEvent>?
)

data class EspnEvent(
    @SerializedName("id") val id: String,
    @SerializedName("date") val date: String,
    @SerializedName("competitions") val competitions: List<EspnCompetition>?
)

data class EspnCompetition(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: EspnStatus?,
    @SerializedName("competitors") val competitors: List<EspnCompetitor>?,
    @SerializedName("details") val details: List<EspnDetail>?,
    @SerializedName("attendance") val attendance: Int? = null
)

data class EspnStatus(
    @SerializedName("type") val type: EspnStatusType?,
    @SerializedName("displayClock") val displayClock: String?
)

data class EspnStatusType(
    @SerializedName("name") val name: String,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("state") val state: String, // "pre", "in", "post"
    @SerializedName("detail") val detail: String? = null
)

data class EspnCompetitor(
    @SerializedName("id") val id: String,
    @SerializedName("homeAway") val homeAway: String,
    @SerializedName("score") val score: String?,
    @SerializedName("team") val team: EspnTeam?,
    @SerializedName("statistics") val statistics: List<EspnStatistic>? = null
)

data class EspnStatistic(
    @SerializedName("name") val name: String?,
    @SerializedName("abbreviation") val abbreviation: String?,
    @SerializedName("displayValue") val displayValue: String?
)

data class EspnTeam(
    @SerializedName("id") val id: String,
    @SerializedName("abbreviation") val abbreviation: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("color") val color: String? = null,
    @SerializedName("alternateColor") val alternateColor: String? = null
)

data class EspnDetail(
    @SerializedName("scoringPlay") val scoringPlay: Boolean?,
    @SerializedName("clock") val clock: EspnClock?,
    @SerializedName("team") val team: EspnTeamRef?,
    @SerializedName("athletesInvolved") val athletesInvolved: List<EspnAthlete>?,
    @SerializedName("penaltyKick") val penaltyKick: Boolean? = null,
    @SerializedName("ownGoal") val ownGoal: Boolean? = null,
    @SerializedName("yellowCard") val yellowCard: Boolean? = null,
    @SerializedName("redCard") val redCard: Boolean? = null,
    @SerializedName("type") val type: EspnDetailType? = null
)

data class EspnDetailType(
    @SerializedName("text") val text: String?
)

data class EspnClock(
    @SerializedName("displayValue") val displayValue: String?
)

data class EspnTeamRef(
    @SerializedName("id") val id: String
)

data class EspnAthlete(
    @SerializedName("id") val id: String,
    @SerializedName("displayName") val displayName: String?
)
