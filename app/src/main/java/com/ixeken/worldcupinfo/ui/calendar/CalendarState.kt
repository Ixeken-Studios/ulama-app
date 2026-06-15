package com.ixeken.worldcupinfo.ui.calendar

import com.ixeken.worldcupinfo.domain.model.Match

/**
 * Representa el estado inmutable de la pantalla de Calendario y Quiniela.
 */
sealed interface CalendarState {
    
    /**
     * Estado cargando los datos iniciales.
     */
    object Loading : CalendarState

    /**
     * Estado con los partidos cargados satisfactoriamente.
     *
     * @property matches Lista de partidos cargados.
     */
    data class Success(
        val matches: List<Match>
    ) : CalendarState

    /**
     * Estado cuando ocurre un error en la carga de datos.
     *
     * @property message Mensaje descriptivo del error.
     */
    data class Error(
        val message: String
    ) : CalendarState
}
