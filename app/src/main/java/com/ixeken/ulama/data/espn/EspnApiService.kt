package com.ixeken.ulama.data.espn

import com.ixeken.ulama.data.espn.model.EspnMatchSummaryResponse
import com.ixeken.ulama.data.espn.model.EspnScoreboardResponse
import com.ixeken.ulama.data.espn.model.EspnStandingsResponse
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato de red para consultar los endpoints publicos de futbol de ESPN.
 * Consume endpoints libres sin autenticacion, tokens ni cuotas de uso restrictivas.
 */
interface EspnApiService {

    @GET("site/v2/sports/soccer/{league}/scoreboard")
    suspend fun getScoreboard(
        @Path("league") league: String,
        @Query("dates") dates: String? = null,
        @Query("limit") limit: Int? = null,
        @Header("Cache-Control") cacheControl: String? = null
    ): EspnScoreboardResponse

    @GET("v2/sports/soccer/{league}/standings")
    suspend fun getStandings(
        @Path("league") league: String
    ): EspnStandingsResponse

    @GET("site/v2/sports/soccer/{league}/summary")
    suspend fun getMatchSummary(
        @Path("league") league: String,
        @Query("event") eventId: String
    ): EspnMatchSummaryResponse
}
