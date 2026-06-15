package com.ixeken.worldcupinfo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RemoteCupResponse(
    @SerializedName("matches")
    val matches: List<RemoteMatchDto>
)

data class RemoteMatchDto(
    @SerializedName("round") val round: String,
    @SerializedName("num") val num: Int,
    @SerializedName("date") val date: String,        // Ej: "2026-07-11"
    @SerializedName("time") val time: String,         // Ej: "18:00" or "13:00 UTC-6"
    @SerializedName("team1") val team1: String,
    @SerializedName("team2") val team2: String,
    @SerializedName("score") val score: RemoteScoreDto? = null,
    @SerializedName("goals1") val goals1: List<RemoteGoalDto>? = null,
    @SerializedName("goals2") val goals2: List<RemoteGoalDto>? = null,
    @SerializedName("ground") val ground: String?,
    @SerializedName("group") val group: String?
)

data class RemoteGoalDto(
    @SerializedName("name") val name: String,
    @SerializedName("minute") val minute: String
)

data class RemoteScoreDto(
    @SerializedName("ft") val ft: List<Int>? = null
)
