package com.ixeken.worldcupinfo.data.remote.api

import com.ixeken.worldcupinfo.data.remote.dto.RemoteCupResponse
import com.ixeken.worldcupinfo.data.remote.dto.EspnScoreboardResponse
import retrofit2.http.GET

interface MatchApiService {
    /**
     * Obtiene los datos del fixture desde el repositorio remoto de GitHub.
     */
    @GET("openfootball/worldcup.json/master/2026/worldcup.json")
    suspend fun fetchWorldCupFixture(): RemoteCupResponse

    /**
     * Obtiene los marcadores en vivo de ESPN.
     */
    @GET("https://site.api.espn.com/apis/site/v2/sports/soccer/fifa.world/scoreboard")
    suspend fun fetchEspnScoreboard(
        @retrofit2.http.Query("dates") dates: String? = null
    ): EspnScoreboardResponse
}
