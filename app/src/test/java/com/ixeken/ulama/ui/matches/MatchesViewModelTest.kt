package com.ixeken.ulama.ui.matches

import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.MatchStatus
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.core.model.TemporalFilter
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias para validar la particion simultanea de filtros temporales
 * requerida por HorizontalPager sin parpadeos en MatchesViewModel.
 */
class MatchesViewModelTest {

    @Test
    fun partitionMatchesByFilter_separatesPastTodayAndUpcomingSimultaneously() {
        val now = Instant.now()
        val pastMatch = sampleMatch(
            id = "past_1",
            dateUtc = now.minus(2, ChronoUnit.DAYS).toString(),
            state = MatchState.FINISHED
        )
        val liveTodayMatch = sampleMatch(
            id = "live_1",
            dateUtc = now.toString(),
            state = MatchState.LIVE
        )
        val upcomingMatch = sampleMatch(
            id = "upcoming_1",
            dateUtc = now.plus(2, ChronoUnit.DAYS).toString(),
            state = MatchState.SCHEDULED
        )

        val partitioned = partitionMatchesByFilter(
            matches = listOf(pastMatch, liveTodayMatch, upcomingMatch),
            isFavoritesLeague = false,
            favoriteIds = emptySet()
        )

        assertEquals(listOf("past_1"), partitioned[TemporalFilter.PAST]?.map { it.id })
        assertEquals(listOf("live_1"), partitioned[TemporalFilter.TODAY]?.map { it.id })
        assertEquals(listOf("upcoming_1"), partitioned[TemporalFilter.UPCOMING]?.map { it.id })
    }

    @Test
    fun partitionMatchesByFilter_filtersOnlyFavoriteTeamsInFavoritesView() {
        val now = Instant.now()
        val favMatch = sampleMatch(
            id = "fav_upcoming",
            dateUtc = now.plus(1, ChronoUnit.DAYS).toString(),
            state = MatchState.SCHEDULED,
            homeId = "fav_team"
        )
        val otherMatch = sampleMatch(
            id = "other_upcoming",
            dateUtc = now.plus(1, ChronoUnit.DAYS).toString(),
            state = MatchState.SCHEDULED,
            homeId = "other_team"
        )

        val partitioned = partitionMatchesByFilter(
            matches = listOf(favMatch, otherMatch),
            isFavoritesLeague = true,
            favoriteIds = setOf("fav_team")
        )

        assertEquals(listOf("fav_upcoming"), partitioned[TemporalFilter.UPCOMING]?.map { it.id })
        assertEquals(emptyList<Match>(), partitioned[TemporalFilter.TODAY])
        assertEquals(emptyList<Match>(), partitioned[TemporalFilter.PAST])
    }

    private fun sampleMatch(
        id: String,
        dateUtc: String,
        state: MatchState,
        homeId: String = "home_1"
    ): Match = Match(
        id = id,
        dateUtc = dateUtc,
        homeTeam = Team(
            id = homeId,
            name = "Home",
            shortName = "HOM",
            abbreviation = "HOM",
            logoUrl = null,
            isHome = true,
            score = null,
            isWinner = false
        ),
        awayTeam = Team(
            id = "away_1",
            name = "Away",
            shortName = "AWY",
            abbreviation = "AWY",
            logoUrl = null,
            isHome = false,
            score = null,
            isWinner = false
        ),
        status = MatchStatus(
            state = state,
            displayClock = "0'",
            detail = "",
            isCompleted = state == MatchState.FINISHED
        ),
        venueName = null,
        league = League.LIGA_MX
    )
}
