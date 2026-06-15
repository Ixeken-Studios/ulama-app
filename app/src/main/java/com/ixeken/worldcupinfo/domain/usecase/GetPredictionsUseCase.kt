package com.ixeken.worldcupinfo.domain.usecase

import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener únicamente los partidos que tienen una predicción guardada.
 */
class GetPredictionsUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    operator fun invoke(): Flow<List<Match>> {
        return matchRepository.getMatchesWithPredictionsOnlyFlow()
    }
}
