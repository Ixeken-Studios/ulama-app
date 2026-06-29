package com.ixeken.worldcupinfo.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.ixeken.worldcupinfo.R
import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.model.MatchStage
import com.ixeken.worldcupinfo.domain.model.MatchStatus
import com.ixeken.worldcupinfo.ui.calendar.CalendarState
import com.ixeken.worldcupinfo.ui.calendar.WorldCupViewModel
import com.ixeken.worldcupinfo.ui.common.AppHeader
import com.ixeken.worldcupinfo.ui.common.TeamFlagEmoji
import com.ixeken.worldcupinfo.ui.common.getTeamDisplayName

/**
 * Data class representing a team's statistical standing in a group.
 */
data class TeamStanding(
    val teamCode: String,
    val gamesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val points: Int
)

/**
 * Screen displaying the Group Stage standings dynamically calculated from match data.
 */
@Composable
fun GroupsScreen(
    viewModel: WorldCupViewModel,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showAlarmsOnly by viewModel.showAlarmsOnly.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val showFifaCodes by viewModel.showFifaCodes.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color(0xFF386641), // Verde Oscuro de cabecera
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Cabecera superior verde oscuro unificada
            AppHeader(
                showAlarmsOnly = showAlarmsOnly,
                onToggleAlarmsOnly = { viewModel.onToggleShowAlarmsOnly() },
                onSettingsClick = onSettingsClick,
                onFavoritesClick = onFavoritesClick,
                onRefreshClick = {
                    viewModel.onForceRefresh { success ->
                        val msgId = if (success) R.string.toast_sync_success else R.string.toast_sync_failed
                        Toast.makeText(context, context.getString(msgId), Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 2. Contenedor principal de datos (Blanco Vainilla)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFFF2E8CF)) // Blanco Vainilla
            ) {
                when (val state = uiState) {
                    is CalendarState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF386641)
                        )
                    }
                    is CalendarState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    is CalendarState.Success -> {
                        val standingsMap = remember(state.matches) {
                            calculateStandings(state.matches)
                        }
                        val sortedGroupNames = remember(standingsMap) {
                            standingsMap.keys.sorted()
                        }
                        val bestThirdTeams = remember(standingsMap) {
                            standingsMap.values
                                .mapNotNull { list -> list.getOrNull(2) }
                                .sortedWith(
                                    compareByDescending<TeamStanding> { it.points }
                                        .thenByDescending { it.goalDifference }
                                        .thenByDescending { it.goalsFor }
                                )
                                .take(8)
                                .map { it.teamCode }
                                .toSet()
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 24.dp,
                                bottom = 96.dp // Espacio para evitar superposición con la barra de navegación flotante
                            ),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            item {
                                Text(
                                    text = stringResource(id = R.string.title_groups),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF386641),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (sortedGroupNames.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.no_group_matches),
                                            color = Color(0xFF1F1F1F).copy(alpha = 0.6f),
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            } else {
                                items(
                                    items = sortedGroupNames,
                                    key = { it }
                                ) { groupName ->
                                    val groupStandings = standingsMap[groupName].orEmpty()
                                    GroupStandingCard(
                                        groupName = groupName,
                                        standings = groupStandings,
                                        showFifaCodes = showFifaCodes,
                                        bestThirdTeams = bestThirdTeams
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card representing standings for a single group.
 */
@Composable
fun GroupStandingCard(
    groupName: String,
    standings: List<TeamStanding>,
    showFifaCodes: Boolean,
    modifier: Modifier = Modifier,
    bestThirdTeams: Set<String> = emptySet()
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Group Title
            val groupWord = stringResource(id = R.string.label_group_word)
            val displayGroupName = remember(groupName, groupWord) {
                groupName.replace("Group", groupWord, ignoreCase = true)
            }
            Text(
                text = displayGroupName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF386641),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            )

            // Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team header
                Text(
                    text = stringResource(id = R.string.table_header_team),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A994E),
                    modifier = Modifier.weight(3.4f)
                )

                // Stat headers
                val statHeaders = listOf(
                    stringResource(id = R.string.stat_gp),
                    stringResource(id = R.string.stat_w),
                    stringResource(id = R.string.stat_d),
                    stringResource(id = R.string.stat_l),
                    stringResource(id = R.string.stat_gd),
                    stringResource(id = R.string.stat_pts)
                )
                val weights = listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.7f, 0.7f)
                
                statHeaders.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A994E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(weights[index])
                    )
                }
            }

            // Standings Rows
            val context = LocalContext.current
            standings.forEachIndexed { index, standing ->
                val isQualified = index < 2 || (index == 2 && bestThirdTeams.contains(standing.teamCode))
                Row(
                     modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team info (Rank + Flag + Name)
                    Row(
                        modifier = Modifier.weight(3.4f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Number
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(22.dp)
                        ) {
                            if (isQualified) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF386641))
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = (index + 1).toString(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isQualified) Color(0xFF386641) else Color(0xFF6A994E)
                            )
                        }

                        // Flag
                        TeamFlagEmoji(teamCode = standing.teamCode)
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        // Full Name
                        Text(
                            text = if (showFifaCodes) standing.teamCode.uppercase() else getTeamDisplayName(standing.teamCode, context),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Stat values
                    val stats = listOf(
                        standing.gamesPlayed.toString(),
                        standing.wins.toString(),
                        standing.draws.toString(),
                        standing.losses.toString(),
                        if (standing.goalDifference > 0) "+${standing.goalDifference}" else standing.goalDifference.toString(),
                        standing.points.toString()
                    )
                    val weights = listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.7f, 0.7f)

                    stats.forEachIndexed { idx, valStr ->
                        Text(
                            text = valStr,
                            fontSize = 13.sp,
                            fontWeight = if (idx == 5) FontWeight.Black else FontWeight.Bold, // Bold PTS
                            color = Color(0xFF1F1F1F),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(weights[idx])
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculates group stage standings dynamically in-memory from a list of matches.
 */
fun calculateStandings(matches: List<Match>): Map<String, List<TeamStanding>> {
    val groupMatches = matches.filter { it.stage == MatchStage.GROUPS && !it.group.isNullOrBlank() }

    val groupTeams = mutableMapOf<String, MutableSet<String>>()
    for (match in groupMatches) {
        val grp = match.group ?: continue
        groupTeams.getOrPut(grp) { mutableSetOf() }.apply {
            add(match.teamA)
            add(match.teamB)
        }
    }

    val standingsMap = mutableMapOf<String, MutableMap<String, TeamStanding>>()
    for ((groupName, teams) in groupTeams) {
        val groupStandings = standingsMap.getOrPut(groupName) { mutableMapOf() }
        for (team in teams) {
            groupStandings[team] = TeamStanding(
                teamCode = team,
                gamesPlayed = 0,
                wins = 0,
                draws = 0,
                losses = 0,
                goalsFor = 0,
                goalsAgainst = 0,
                goalDifference = 0,
                points = 0
            )
        }
    }

    for (match in groupMatches) {
        val grp = match.group ?: continue
        val isFinished = match.status == MatchStatus.FINISHED
        if (isFinished && match.goalsA != null && match.goalsB != null) {
            val goalsA = match.goalsA
            val goalsB = match.goalsB

            val teamAStats = standingsMap[grp]?.get(match.teamA)
            val teamBStats = standingsMap[grp]?.get(match.teamB)

            if (teamAStats != null && teamBStats != null) {
                val newGpA = teamAStats.gamesPlayed + 1
                val newGfA = teamAStats.goalsFor + goalsA
                val newGaA = teamAStats.goalsAgainst + goalsB

                val newGpB = teamBStats.gamesPlayed + 1
                val newGfB = teamBStats.goalsFor + goalsB
                val newGaB = teamBStats.goalsAgainst + goalsA

                var newWA = teamAStats.wins
                var newDA = teamAStats.draws
                var newLA = teamAStats.losses
                var newPtsA = teamAStats.points

                var newWB = teamBStats.wins
                var newDB = teamBStats.draws
                var newLB = teamBStats.losses
                var newPtsB = teamBStats.points

                if (goalsA > goalsB) {
                    newWA++
                    newPtsA += 3
                    newLB++
                } else if (goalsA < goalsB) {
                    newWB++
                    newPtsB += 3
                    newLA++
                } else {
                    newDA++
                    newPtsA += 1
                    newDB++
                    newPtsB += 1
                }

                standingsMap[grp]?.put(match.teamA, teamAStats.copy(
                    gamesPlayed = newGpA,
                    wins = newWA,
                    draws = newDA,
                    losses = newLA,
                    goalsFor = newGfA,
                    goalsAgainst = newGaA,
                    goalDifference = newGfA - newGaA,
                    points = newPtsA
                ))

                standingsMap[grp]?.put(match.teamB, teamBStats.copy(
                    gamesPlayed = newGpB,
                    wins = newWB,
                    draws = newDB,
                    losses = newLB,
                    goalsFor = newGfB,
                    goalsAgainst = newGaB,
                    goalDifference = newGfB - newGaB,
                    points = newPtsB
                ))
            }
        }
    }

    return standingsMap.mapValues { (_, teamMap) ->
        teamMap.values.sortedWith(
            compareByDescending<TeamStanding> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
                .thenBy { it.teamCode }
        )
    }
}

// Fin de GroupsScreen
