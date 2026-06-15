package com.ixeken.worldcupinfo.domain.usecase

import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener todos los partidos del mundial de forma reactiva.
 */
class GetMatchesUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    /**
     * Ejecuta el caso de uso y retorna un flujo de la lista de partidos actualizados.
     */
    operator fun invoke(): Flow<List<Match>> {
        return matchRepository.getMatchesFlow()
    }
}
