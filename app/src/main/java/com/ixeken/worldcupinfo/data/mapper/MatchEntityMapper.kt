package com.ixeken.worldcupinfo.data.mapper

import com.ixeken.worldcupinfo.data.database.entities.MatchEntity
import com.ixeken.worldcupinfo.data.database.entities.MatchWithPrediction
import com.ixeken.worldcupinfo.data.database.entities.PredictionEntity
import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.model.MatchStage
import com.ixeken.worldcupinfo.domain.model.MatchStatus
import com.ixeken.worldcupinfo.domain.model.Prediction

/**
 * Convierte un Match de dominio a MatchEntity de Room.
 */
fun Match.toEntity(): MatchEntity {
    val gson = com.google.gson.Gson()
    return MatchEntity(
        id = id,
        dateUnixTimestamp = dateUnixTimestamp,
        teamA = teamA,
        teamB = teamB,
        stadium = stadium,
        status = status.name,
        stage = stage.name,
        isAlarmActive = isAlarmActive,
        goalsA = goalsA,
        goalsB = goalsB,
        group = group,
        eventsDetailsA = gson.toJson(eventsDetailsA),
        eventsDetailsB = gson.toJson(eventsDetailsB),
        liveMinute = liveMinute,
        attendance = attendance,
        teamAColor = teamAColor,
        teamBColor = teamBColor,
        stats = stats?.let { gson.toJson(it) }
    )
}

/**
 * Convierte un Prediction de dominio a PredictionEntity de Room.
 */
fun Prediction.toEntity(): PredictionEntity {
    return PredictionEntity(
        matchId = matchId,
        goalsA = goalsA,
        goalsB = goalsB,
        penaltyWinner = penaltyWinner
    )
}

/**
 * Convierte un MatchWithPrediction de base de datos a un Match de dominio.
 */
fun MatchWithPrediction.toDomain(): Match {
    val statusEnum = try {
        MatchStatus.valueOf(match.status)
    } catch (e: IllegalArgumentException) {
        MatchStatus.SCHEDULED
    }

    val stageEnum = try {
        MatchStage.valueOf(match.stage)
    } catch (e: IllegalArgumentException) {
        MatchStage.GROUPS
    }

    val domainPrediction = prediction?.let {
        Prediction(
            matchId = it.matchId,
            goalsA = it.goalsA,
            goalsB = it.goalsB,
            penaltyWinner = it.penaltyWinner
        )
    }

    val gson = com.google.gson.Gson()
    val type = object : com.google.gson.reflect.TypeToken<List<com.ixeken.worldcupinfo.domain.model.MatchEvent>>() {}.type
    val eventsDetailsAList: List<com.ixeken.worldcupinfo.domain.model.MatchEvent> = try {
        if (!match.eventsDetailsA.isNullOrEmpty()) gson.fromJson(match.eventsDetailsA, type) else emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    val eventsDetailsBList: List<com.ixeken.worldcupinfo.domain.model.MatchEvent> = try {
        if (!match.eventsDetailsB.isNullOrEmpty()) gson.fromJson(match.eventsDetailsB, type) else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    val statsObj: com.ixeken.worldcupinfo.domain.model.MatchStats? = try {
        if (!match.stats.isNullOrEmpty()) {
            gson.fromJson(match.stats, com.ixeken.worldcupinfo.domain.model.MatchStats::class.java)
        } else null
    } catch (e: Exception) {
        null
    }

    return Match(
        id = match.id,
        dateUnixTimestamp = match.dateUnixTimestamp,
        teamA = match.teamA,
        teamB = match.teamB,
        stadium = match.stadium,
        status = statusEnum,
        stage = stageEnum,
        prediction = domainPrediction,
        isAlarmActive = match.isAlarmActive,
        goalsA = match.goalsA,
        goalsB = match.goalsB,
        group = match.group,
        eventsDetailsA = eventsDetailsAList,
        eventsDetailsB = eventsDetailsBList,
        liveMinute = match.liveMinute,
        attendance = match.attendance,
        teamAColor = match.teamAColor,
        teamBColor = match.teamBColor,
        stats = statsObj
    )
}
