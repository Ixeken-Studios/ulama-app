package com.ixeken.worldcupinfo.ui.predictions

import android.widget.Toast
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ixeken.worldcupinfo.R
import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.model.MatchStage
import com.ixeken.worldcupinfo.domain.model.MatchStatus
import com.ixeken.worldcupinfo.ui.calendar.CalendarState
import com.ixeken.worldcupinfo.ui.calendar.WorldCupViewModel
import com.ixeken.worldcupinfo.ui.calendar.getCleanCityResId
import com.ixeken.worldcupinfo.ui.common.AppHeader
import com.ixeken.worldcupinfo.ui.common.TeamFlagEmoji
import com.ixeken.worldcupinfo.ui.common.TeamFlagEmojiLarge
import com.ixeken.worldcupinfo.ui.common.getTeamDisplayName
import com.ixeken.worldcupinfo.ui.common.getTeamFlagDrawable
import com.ixeken.worldcupinfo.ui.common.isPlaceholderTeam

private val PredictionCorrectColor = Color(0xFF386641)
private val PredictionIncorrectColor = Color(0xFFBC4749)

/**
 * Returns a [BorderStroke] for a finished-match prediction card:
 * - Green if the predicted score matches the actual result.
 * - Red if it does not match.
 * - `null` when the match is not finished, has no prediction, or the actual scores are unavailable.
 */
@Composable
private fun predictionBorder(match: Match): BorderStroke? {
    val prediction = match.prediction ?: return null
    if (match.status != MatchStatus.FINISHED) return null
    val actualA = match.goalsA ?: return null
    val actualB = match.goalsB ?: return null
    val isCorrect = prediction.goalsA == actualA && prediction.goalsB == actualB
    val color = if (isCorrect) PredictionCorrectColor else PredictionIncorrectColor
    return BorderStroke(3.dp, color)
}

/**
 * Pantalla que muestra el listado o bento de todas las predicciones de la quiniela
 * registradas por el usuario de manera interactiva.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionsScreen(
    viewModel: WorldCupViewModel,
    onSettingsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val predictionsState by viewModel.predictionsState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showAlarmsOnly by viewModel.showAlarmsOnly.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val showFifaCodes by viewModel.showFifaCodes.collectAsStateWithLifecycle()

    var isBentoView by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var selectedMatchForEdit by remember { mutableStateOf<Match?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentMatches = (predictionsState as? CalendarState.Success)?.matches ?: emptyList()
    val activeMatchForEdit = remember(selectedMatchForEdit, currentMatches) {
        selectedMatchForEdit?.let { selected ->
            currentMatches.find { it.id == selected.id } ?: selected
        }
    }

    Scaffold(
        containerColor = Color(0xFF386641), // Verde Oscuro
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
                when (val state = predictionsState) {
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
                        var selectedStageFilter by remember { mutableIntStateOf(0) }
                        var selectedStatusFilter by remember { mutableIntStateOf(0) }

                        val stageFilteredMatches = remember(state.matches, selectedStageFilter) {
                            state.matches.filter { match ->
                                when (selectedStageFilter) {
                                    0 -> match.stage == MatchStage.GROUPS
                                    1 -> match.stage == MatchStage.ROUND_OF_32 || match.stage == MatchStage.ROUND_OF_16 || match.stage == MatchStage.QUARTERFINALS || match.stage == MatchStage.SEMIFINAL
                                    2 -> match.stage == MatchStage.FINAL || match.stage == MatchStage.THIRD_PLACE
                                    else -> true
                                }
                            }
                        }

                        val statusFilteredMatches = remember(stageFilteredMatches, selectedStatusFilter) {
                            stageFilteredMatches.filter { match ->
                                when (selectedStatusFilter) {
                                    0 -> match.status != MatchStatus.FINISHED
                                    1 -> match.status == MatchStatus.FINISHED
                                    else -> true
                                }
                            }
                        }

                        val predictedMatches = remember(statusFilteredMatches) {
                            statusFilteredMatches.filter { it.prediction != null }
                        }

                        val totalMatches = remember(uiState) {
                            (uiState as? CalendarState.Success)?.matches?.size ?: 0
                        }
                        val predictedCount = remember(state.matches) {
                            state.matches.count { it.prediction != null }
                        }

                        // Dialogo de progreso de predicciones
                        if (showInfoDialog) {
                            PredictionProgressDialog(
                                predicted = predictedCount,
                                total = totalMatches,
                                onDismiss = { showInfoDialog = false }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                        ) {
                            // Título de la pestaña + Selector de vista premium en la misma línea
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.title_predictions),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF386641)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Botón de información separado con misma estética
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF6A994E))
                                            .clickable { showInfoDialog = true }
                                    ) {
                                        Canvas(modifier = Modifier.size(16.dp)) {
                                            val w = size.width
                                            val h = size.height
                                            val tint = Color(0xFFF2E8CF)
                                            // Punto superior de la "i"
                                            drawCircle(
                                                color = tint,
                                                radius = 1.8f.dp.toPx(),
                                                center = Offset(w * 0.5f, h * 0.2f)
                                            )
                                            // Cuerpo de la "i"
                                            drawLine(
                                                color = tint,
                                                start = Offset(w * 0.5f, h * 0.4f),
                                                end = Offset(w * 0.5f, h * 0.85f),
                                                strokeWidth = 2.2f.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }

                                    // Switch premium de diseño ovalado
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF6A994E))
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Opción Bento
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isBentoView) Color(0xFF386641) else Color.Transparent)
                                                .clickable { isBentoView = true }
                                        ) {
                                            Canvas(modifier = Modifier.size(16.dp)) {
                                                val sizeSq = 6.dp.toPx()
                                                val gap = 2.dp.toPx()
                                                val tint = Color(0xFFF2E8CF)
                                                val startOffset = 1.dp.toPx()
                                                val secondOffset = startOffset + sizeSq + gap

                                                drawRoundRect(
                                                    color = tint,
                                                    topLeft = Offset(startOffset, startOffset),
                                                    size = androidx.compose.ui.geometry.Size(sizeSq, sizeSq),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2f.dp.toPx())
                                                )
                                                drawRoundRect(
                                                    color = tint,
                                                    topLeft = Offset(secondOffset, startOffset),
                                                    size = androidx.compose.ui.geometry.Size(sizeSq, sizeSq),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2f.dp.toPx())
                                                )
                                                drawRoundRect(
                                                    color = tint,
                                                    topLeft = Offset(startOffset, secondOffset),
                                                    size = androidx.compose.ui.geometry.Size(sizeSq, sizeSq),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2f.dp.toPx())
                                                )
                                                drawRoundRect(
                                                    color = tint,
                                                    topLeft = Offset(secondOffset, secondOffset),
                                                    size = androidx.compose.ui.geometry.Size(sizeSq, sizeSq),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2f.dp.toPx())
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Opción Lista
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (!isBentoView) Color(0xFF386641) else Color.Transparent)
                                                .clickable { isBentoView = false }
                                        ) {
                                            Canvas(modifier = Modifier.size(16.dp)) {
                                                val strokeWidth = 1.8f.dp.toPx()
                                                val w = size.width
                                                val h = size.height
                                                val tint = Color(0xFFF2E8CF)

                                                drawLine(
                                                    color = tint,
                                                    start = Offset(w * 0.15f, h * 0.3f),
                                                    end = Offset(w * 0.85f, h * 0.3f),
                                                    strokeWidth = strokeWidth
                                                )
                                                drawLine(
                                                    color = tint,
                                                    start = Offset(w * 0.15f, h * 0.5f),
                                                    end = Offset(w * 0.85f, h * 0.5f),
                                                    strokeWidth = strokeWidth
                                                )
                                                drawLine(
                                                    color = tint,
                                                    start = Offset(w * 0.15f, h * 0.7f),
                                                    end = Offset(w * 0.85f, h * 0.7f),
                                                    strokeWidth = strokeWidth
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 1. Filtros de Etapa (Fase de grupos, Eliminatorias, Final)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                val stages = listOf(
                                    stringResource(id = R.string.tab_group_stage),
                                    stringResource(id = R.string.tab_eliminations),
                                    stringResource(id = R.string.tab_final)
                                )
                                stages.forEachIndexed { index, title ->
                                    val isSelected = selectedStageFilter == index
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) Color(0xFF386641) else Color(0xFFA7C957))
                                            .clickable { selectedStageFilter = index }
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFFF9F2E1) else Color(0xFF386641)
                                        )
                                    }
                                }
                            }

                            // 2. Filtros de Estado (Próximos, Pasados)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                val statusOptions = listOf(
                                    stringResource(id = R.string.filter_predictions_upcoming),
                                    stringResource(id = R.string.filter_predictions_past)
                                )
                                statusOptions.forEachIndexed { index, title ->
                                    val isSelected = selectedStatusFilter == index
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) Color(0xFF386641) else Color(0xFFA7C957))
                                            .clickable { selectedStatusFilter = index }
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFFF9F2E1) else Color(0xFF386641)
                                        )
                                    }
                                }
                            }


                            // Listado de Predicciones
                            if (predictedMatches.isEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.no_predictions),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF386641).copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                if (isBentoView) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        contentPadding = PaddingValues(bottom = 96.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = predictedMatches,
                                            key = { it.id }
                                        ) { match ->
                                            BentoPredictionCard(
                                                match = match,
                                                showFifaCodes = showFifaCodes,
                                                onClick = { selectedMatchForEdit = match }
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(bottom = 96.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = predictedMatches,
                                            key = { it.id }
                                        ) { match ->
                                            ListPredictionCard(
                                                match = match,
                                                showFifaCodes = showFifaCodes,
                                                onClick = { selectedMatchForEdit = match }
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
    }

    if (activeMatchForEdit != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMatchForEdit = null },
            sheetState = sheetState,
            containerColor = Color(0xFFF2E8CF) // Blanco Vainilla
        ) {
            PredictionEditBottomSheetContent(
                match = activeMatchForEdit,
                onSavePrediction = { matchId, goalsA, goalsB, penaltyWinner ->
                    viewModel.onSavePrediction(matchId, goalsA, goalsB, penaltyWinner)
                },
                onDismiss = { selectedMatchForEdit = null }
            )
        }
    }
}

@Composable
fun BentoPredictionCard(
    match: Match,
    showFifaCodes: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val cleanCity = getCleanCityResId(match.stadium)?.let { stringResource(it) }
        ?: remember(match.stadium) {
            match.stadium.substringAfter("(").substringBefore(")").substringBefore("/").trim()
        }

    val stageName = when (match.stage) {
        MatchStage.GROUPS -> {
            val grp = match.group
            if (!grp.isNullOrBlank()) {
                val groupWord = stringResource(id = R.string.label_group_word)
                grp.replace("Group", groupWord, ignoreCase = true)
            } else {
                stringResource(id = R.string.stage_groups)
            }
        }
        MatchStage.ROUND_OF_32 -> stringResource(id = R.string.stage_dieciseisavos)
        MatchStage.ROUND_OF_16 -> stringResource(id = R.string.stage_octavos)
        MatchStage.QUARTERFINALS -> stringResource(id = R.string.stage_cuartos)
        MatchStage.SEMIFINAL -> stringResource(id = R.string.stage_semis)
        MatchStage.FINAL -> stringResource(id = R.string.stage_final)
        MatchStage.THIRD_PLACE -> stringResource(id = R.string.stage_third_place)
    }

    val teamADisp = remember(match.teamA, showFifaCodes, context) {
        if (showFifaCodes) match.teamA.uppercase() else getTeamDisplayName(match.teamA, context).take(8)
    }
    val teamBDisp = remember(match.teamB, showFifaCodes, context) {
        if (showFifaCodes) match.teamB.uppercase() else getTeamDisplayName(match.teamB, context).take(8)
    }

    val cardBorder = predictionBorder(match)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stage/City header text
            Text(
                text = "$stageName • $cleanCity",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F).copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (match.prediction?.penaltyWinner != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.penalty_winner_label),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386641),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score with Flags
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TeamFlagEmoji(teamCode = match.teamA)
                Spacer(modifier = Modifier.width(6.dp))
                if (match.prediction?.penaltyWinner != null) {
                    val isWinner = match.prediction.penaltyWinner == match.teamA
                    Icon(
                        imageVector = if (isWinner) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (isWinner) Color(0xFF386641) else Color(0xFFBC4749),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "${match.prediction?.goalsA ?: 0}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F1F1F)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "—",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (match.prediction?.penaltyWinner != null) {
                    val isWinner = match.prediction.penaltyWinner == match.teamB
                    Icon(
                        imageVector = if (isWinner) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (isWinner) Color(0xFF386641) else Color(0xFFBC4749),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "${match.prediction?.goalsB ?: 0}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F1F1F)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                TeamFlagEmoji(teamCode = match.teamB)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Team names/VS
            Text(
                text = stringResource(id = R.string.label_team_vs, teamADisp, teamBDisp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F).copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )


        }
    }
}

@Composable
fun ListPredictionCard(
    match: Match,
    showFifaCodes: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val cleanCity = getCleanCityResId(match.stadium)?.let { stringResource(it) }
        ?: remember(match.stadium) {
            match.stadium.substringAfter("(").substringBefore(")").substringBefore("/").trim()
        }

    val stageName = when (match.stage) {
        MatchStage.GROUPS -> {
            val grp = match.group
            if (!grp.isNullOrBlank()) {
                val groupWord = stringResource(id = R.string.label_group_word)
                grp.replace("Group", groupWord, ignoreCase = true)
            } else {
                stringResource(id = R.string.stage_groups)
            }
        }
        MatchStage.ROUND_OF_32 -> stringResource(id = R.string.stage_dieciseisavos)
        MatchStage.ROUND_OF_16 -> stringResource(id = R.string.stage_octavos)
        MatchStage.QUARTERFINALS -> stringResource(id = R.string.stage_cuartos)
        MatchStage.SEMIFINAL -> stringResource(id = R.string.stage_semis)
        MatchStage.FINAL -> stringResource(id = R.string.stage_final)
        MatchStage.THIRD_PLACE -> stringResource(id = R.string.stage_third_place)
    }

    val teamADisp = remember(match.teamA, showFifaCodes, context) {
        if (showFifaCodes) match.teamA.uppercase() else getTeamDisplayName(match.teamA, context)
    }
    val teamBDisp = remember(match.teamB, showFifaCodes, context) {
        if (showFifaCodes) match.teamB.uppercase() else getTeamDisplayName(match.teamB, context)
    }

    val cardBorder = predictionBorder(match)

    Card(
        shape = RoundedCornerShape(20.dp), // down from 24.dp
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp), // down from vertical = 16.dp, horizontal = 20.dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: stage • city
            Text(
                text = "$stageName • $cleanCity",
                fontSize = 11.sp, // down from 12.sp
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F).copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            if (match.prediction?.penaltyWinner != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.penalty_winner_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386641),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp)) // down from 14.dp

            // Score Row with Flags
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val drawableId = getTeamFlagDrawable(match.teamA)
                    if (drawableId != null) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = drawableId),
                            contentDescription = match.teamA,
                            modifier = Modifier
                                .size(38.dp) // down from 48.dp
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        ) {
                            Text(text = "⚽", fontSize = 22.sp)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.width(120.dp) // down from 144.dp
                ) {
                    if (match.prediction?.penaltyWinner != null) {
                        val isWinnerA = match.prediction.penaltyWinner == match.teamA
                        Icon(
                            imageVector = if (isWinnerA) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (isWinnerA) Color(0xFF386641) else Color(0xFFBC4749),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "${match.prediction?.goalsA ?: 0}",
                            fontSize = 30.sp, // down from 38.sp
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1F1F1F)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp)) // down from 16.dp

                    Text(
                        text = "—",
                        fontSize = 20.sp, // down from 24.sp
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.3f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.width(12.dp)) // down from 16.dp

                    if (match.prediction?.penaltyWinner != null) {
                        val isWinnerB = match.prediction.penaltyWinner == match.teamB
                        Icon(
                            imageVector = if (isWinnerB) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (isWinnerB) Color(0xFF386641) else Color(0xFFBC4749),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "${match.prediction?.goalsB ?: 0}",
                            fontSize = 30.sp, // down from 38.sp
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1F1F1F)
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val drawableId = getTeamFlagDrawable(match.teamB)
                    if (drawableId != null) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = drawableId),
                            contentDescription = match.teamB,
                            modifier = Modifier
                                .size(38.dp) // down from 48.dp
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        ) {
                            Text(text = "⚽", fontSize = 22.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp)) // down from 8.dp

            // Team Names Row
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teamADisp,
                        fontSize = 13.sp, // down from 14.sp
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F1F1F),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(120.dp)) // down from 144.dp

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teamBDisp,
                        fontSize = 13.sp, // down from 14.sp
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F1F1F),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }


        }
    }
}

/**
 * Dialogo con el progreso de predicciones del usuario.
 * Muestra barra de progreso lineal, conteo de partidos predecidos y mensaje contextual.
 *
 * @param predicted Cantidad de partidos con prediccion registrada.
 * @param total Cantidad total de partidos del torneo.
 * @param onDismiss Callback al cerrar el dialogo.
 */
@Composable
fun PredictionProgressDialog(
    predicted: Int,
    total: Int,
    onDismiss: () -> Unit
) {
    val progress = if (total > 0) predicted.toFloat() / total.toFloat() else 0f
    val remaining = total - predicted

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF2E8CF) // Blanco Vainilla
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Titulo del dialogo
                Text(
                    text = stringResource(id = R.string.dialog_predictions_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF386641)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Barra de progreso con fondo redondeado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFFA7C957).copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF386641))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Etiqueta de conteo: "X de Y partidos predecidos"
                Text(
                    text = stringResource(id = R.string.dialog_predictions_progress, predicted, total),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mensaje contextual
                Text(
                    text = if (remaining <= 0) {
                        stringResource(id = R.string.dialog_predictions_complete)
                    } else {
                        stringResource(id = R.string.dialog_predictions_remaining, remaining)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Boton de cerrar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF386641))
                        .clickable { onDismiss() }
                ) {
                    Text(
                        text = stringResource(id = R.string.action_ok),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9F2E1)
                    )
                }
            }
        }
    }
}

@Composable
fun PredictionEditBottomSheetContent(
    match: Match,
    onSavePrediction: (String, Int, Int, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentTime = remember { System.currentTimeMillis() / 1000 }
    val kickoff = match.dateUnixTimestamp
    val hasStarted = currentTime >= kickoff
    val isTbdMatch = match.stage != MatchStage.GROUPS && (isPlaceholderTeam(match.teamA) || isPlaceholderTeam(match.teamB))

    val cleanCity = getCleanCityResId(match.stadium)?.let { stringResource(it) }
        ?: remember(match.stadium) {
            match.stadium.substringAfter("(").substringBefore(")").substringBefore("/").trim()
        }
    val cleanStadium = remember(match.stadium) {
        match.stadium.substringBefore("(").trim()
    }

    val stageName = when (match.stage) {
        MatchStage.GROUPS -> {
            val grp = match.group
            if (!grp.isNullOrBlank()) {
                val groupWord = stringResource(id = R.string.label_group_word)
                grp.replace("Group", groupWord, ignoreCase = true)
            } else {
                stringResource(id = R.string.stage_groups)
            }
        }
        MatchStage.ROUND_OF_32 -> stringResource(id = R.string.stage_dieciseisavos)
        MatchStage.ROUND_OF_16 -> stringResource(id = R.string.stage_octavos)
        MatchStage.QUARTERFINALS -> stringResource(id = R.string.stage_cuartos)
        MatchStage.SEMIFINAL -> stringResource(id = R.string.stage_semis)
        MatchStage.FINAL -> stringResource(id = R.string.stage_final)
        MatchStage.THIRD_PLACE -> stringResource(id = R.string.stage_third_place)
    }

    var predA by remember { mutableIntStateOf(match.prediction?.goalsA ?: 0) }
    var predB by remember { mutableIntStateOf(match.prediction?.goalsB ?: 0) }
    var isPenaltyPredict by remember { mutableStateOf(match.prediction?.penaltyWinner != null) }
    var penaltyWinner by remember { mutableStateOf(match.prediction?.penaltyWinner) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isTbdMatch) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF9F2E1)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.msg_knockout_no_prediction),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF386641),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // --- HEADER SECTION ---
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = if (cleanStadium.isNotBlank()) "$cleanCity • $cleanStadium" else cleanCity,
                        fontSize = 12.sp,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stageName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            TeamFlagEmojiLarge(teamCode = match.teamA)
                        }
                        Box(
                            modifier = Modifier.width(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val isFinished = match.status == MatchStatus.FINISHED
                            val goalsA = match.goalsA
                            val goalsB = match.goalsB

                            if (isFinished && goalsA != null && goalsB != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$goalsA - $goalsB",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1F1F1F),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.label_final),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F1F1F).copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(id = R.string.label_vs),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1F1F1F).copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        TeamFlagEmojiLarge(teamCode = match.teamB)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getTeamDisplayName(match.teamA, context),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1F1F1F),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.width(80.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getTeamDisplayName(match.teamB, context),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1F1F1F),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PREDICTION SECTION ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.header_my_prediction),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF386641)
            )
        }

        if (match.stage != MatchStage.GROUPS) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.penalty_toggle_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386641)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isPenaltyPredict,
                    onCheckedChange = { checked ->
                        isPenaltyPredict = checked
                        if (checked) {
                            if (predA != predB) {
                                predA = 1
                                predB = 1
                            }
                            if (penaltyWinner == null) {
                                penaltyWinner = match.teamA
                            }
                        } else {
                            penaltyWinner = null
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFF9F2E1),
                        checkedTrackColor = Color(0xFF6A994E),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF9F2E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                if (isPenaltyPredict) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp)
                    ) {
                        val isSelectedA = penaltyWinner == match.teamA
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelectedA) Color(0xFFA7C957).copy(alpha = 0.35f) else Color.Transparent)
                                .clickable(enabled = !hasStarted) { penaltyWinner = match.teamA }
                                .padding(16.dp)
                        ) {
                            TeamFlagEmojiLarge(teamCode = match.teamA)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getTeamDisplayName(match.teamA, context),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F)
                            )
                            if (isSelectedA) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(id = R.string.penalty_winner_team),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF386641)
                                )
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.label_vs),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F).copy(alpha = 0.4f)
                        )

                        val isSelectedB = penaltyWinner == match.teamB
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelectedB) Color(0xFFA7C957).copy(alpha = 0.35f) else Color.Transparent)
                                .clickable(enabled = !hasStarted) { penaltyWinner = match.teamB }
                                .padding(16.dp)
                        ) {
                            TeamFlagEmojiLarge(teamCode = match.teamB)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = getTeamDisplayName(match.teamB, context),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F)
                            )
                            if (isSelectedB) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(id = R.string.penalty_winner_team),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF386641)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Left Side (Team A controls and flag)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                val flagResA = getTeamFlagDrawable(match.teamA)
                                if (flagResA != null) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = flagResA),
                                        contentDescription = match.teamA,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    ) {
                                        Text(text = "⚽", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = { if (!hasStarted) predA++ },
                                        enabled = !hasStarted,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (hasStarted) Color.LightGray else Color(0xFFA7C957))
                                    ) {
                                        Text(
                                            text = "+",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasStarted) Color.Gray else Color(0xFF386641)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFF2E8CF))
                                    ) {
                                        Text(
                                            text = predA.toString(),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF386641)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    IconButton(
                                        onClick = { if (!hasStarted && predA > 0) predA-- },
                                        enabled = !hasStarted,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hasStarted) Color.LightGray
                                                else Color(0xFFFB6B6E)
                                            )
                                    ) {
                                        Text(
                                            text = "-",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasStarted) Color.Gray else Color(0xFFBC4749)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "—",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F).copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Right Side (Team B controls and flag)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = { if (!hasStarted) predB++ },
                                        enabled = !hasStarted,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (hasStarted) Color.LightGray else Color(0xFFA7C957))
                                    ) {
                                        Text(
                                            text = "+",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasStarted) Color.Gray else Color(0xFF386641)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFF2E8CF))
                                    ) {
                                        Text(
                                            text = predB.toString(),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF386641)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    IconButton(
                                        onClick = { if (!hasStarted && predB > 0) predB-- },
                                        enabled = !hasStarted,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hasStarted) Color.LightGray
                                                else Color(0xFFFB6B6E)
                                            )
                                    ) {
                                        Text(
                                            text = "-",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasStarted) Color.Gray else Color(0xFFBC4749)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                val flagResB = getTeamFlagDrawable(match.teamB)
                                if (flagResB != null) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = flagResB),
                                        contentDescription = match.teamB,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    ) {
                                        Text(text = "⚽", fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = {
                    if (hasStarted) {
                        Toast.makeText(context, context.getString(R.string.error_match_started), Toast.LENGTH_LONG).show()
                        return@IconButton
                    }
                    if (isTbdMatch) {
                        return@IconButton
                    }
                    if (isPenaltyPredict) {
                        if (penaltyWinner == null) {
                            Toast.makeText(context, context.getString(R.string.penalty_winner_required), Toast.LENGTH_LONG).show()
                            return@IconButton
                        }
                    } else {
                        if (predA == predB && match.stage != MatchStage.GROUPS) {
                            Toast.makeText(context, context.getString(R.string.error_draws_groups_only), Toast.LENGTH_LONG).show()
                            return@IconButton
                        }
                    }
                    onSavePrediction(match.id, predA, predB, penaltyWinner)
                    Toast.makeText(context, context.getString(R.string.prediction_saved_success), Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                enabled = !hasStarted && !isTbdMatch,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (hasStarted || isTbdMatch) Color.LightGray else Color(0xFF6A994E))
                    .align(Alignment.BottomCenter)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = stringResource(id = R.string.action_save),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
}
