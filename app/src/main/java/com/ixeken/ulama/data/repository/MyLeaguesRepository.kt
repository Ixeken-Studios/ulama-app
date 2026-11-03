package com.ixeken.ulama.data.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de repositorio local para persistir y consultar las ligas personalizadas por el usuario.
 * Permite marcar y desmarcar competiciones deportivas por su codigo de ESPN.
 */
interface MyLeaguesRepository {
    /**
     * Flujo reactivo con el conjunto inmutable de codigos de ligas seleccionadas para 'Mis Ligas'.
     */
    val myLeagueCodes: StateFlow<Set<String>>

    /**
     * Alterna la seleccion de una competicion (agrega si no existe, elimina si existe).
     */
    fun toggleLeague(leagueCode: String)

    /**
     * Consulta inmediata sobre si una liga forma parte del conjunto de 'Mis Ligas'.
     */
    fun isLeagueSelected(leagueCode: String): Boolean

    /**
     * Establece explicitamente el estado de seleccion de una liga.
     */
    fun setLeagueSelected(leagueCode: String, isSelected: Boolean)

    /**
     * Elimina todas las ligas seleccionadas.
     */
    fun clearAll()
}
