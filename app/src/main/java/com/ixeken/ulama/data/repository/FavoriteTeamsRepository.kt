package com.ixeken.ulama.data.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de repositorio local para persistir y consultar clubes favoritos.
 * Permite marcar y desmarcar clubes por su identificador unico universal de ESPN.
 */
interface FavoriteTeamsRepository {
    /**
     * Flujo reactivo con el conjunto inmutable de identificadores de clubes favoritos.
     */
    val favoriteTeamIds: StateFlow<Set<String>>

    /**
     * Alterna la condicion de favorito de un equipo (agrega si no existe, elimina si existe).
     */
    fun toggleFavorite(teamId: String)

    /**
     * Consulta inmediata sobre si un equipo forma parte del conjunto de favoritos.
     */
    fun isFavorite(teamId: String): Boolean

    /**
     * Establece explicitamente el estado de favorito de un club.
     */
    fun setFavorite(teamId: String, isFavorite: Boolean)

    /**
     * Elimina todos los clubes favoritos almacenados.
     */
    fun clearAll()
}
