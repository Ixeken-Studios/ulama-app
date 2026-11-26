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
     * Codigos de competicion conocidos donde participan los clubes favoritos actuales.
     * Evita consultar las 30 tablas de posiciones en cada arranque en frio.
     */
    val favoriteLeagueCodes: StateFlow<Set<String>>

    /**
     * Alterna la condicion de favorito de un equipo (agrega si no existe, elimina si existe).
     */
    fun toggleFavorite(teamId: String)

    /**
     * Registra las ligas asociadas a un club favorito para resolver sus partidos sin escaneo global.
     */
    fun recordTeamLeagues(teamId: String, leagueCodes: Collection<String>)

    /**
     * Indica si todos los clubes favoritos almacenados ya cuentan con al menos una liga vinculada en disco.
     */
    fun hasMappedLeaguesForAllFavorites(): Boolean

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
