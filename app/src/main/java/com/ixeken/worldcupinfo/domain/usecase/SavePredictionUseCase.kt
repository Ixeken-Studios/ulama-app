package com.ixeken.worldcupinfo.domain.usecase

import com.ixeken.worldcupinfo.domain.model.Prediction
import com.ixeken.worldcupinfo.domain.repository.MatchRepository
import javax.inject.Inject

/**
 * Caso de uso para guardar o editar la predicción (quiniela) de goles de un partido.
 */
class SavePredictionUseCase @Inject constructor(
    private val matchRepository: MatchRepository
) {
    /**
     * Ejecuta el caso de uso guardando la quiniela en el repositorio.
     */
    suspend operator fun invoke(prediction: Prediction) {
        matchRepository.savePrediction(prediction)
    }
}
