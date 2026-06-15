package com.ixeken.worldcupinfo.ui.calendar

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.foundation.border
import com.ixeken.worldcupinfo.ui.groups.calculateStandings
import com.ixeken.worldcupinfo.ui.groups.TeamStanding
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ixeken.worldcupinfo.R
import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.model.MatchStage
import com.ixeken.worldcupinfo.domain.model.MatchStatus
import com.ixeken.worldcupinfo.ui.common.AppHeader
import com.ixeken.worldcupinfo.ui.theme.SpaceGrotesk
import com.ixeken.worldcupinfo.ui.common.TeamFlagEmoji
import com.ixeken.worldcupinfo.ui.common.TeamFlagEmojiLarge
import com.ixeken.worldcupinfo.ui.common.formatTeamCodeForDisplay
import com.ixeken.worldcupinfo.ui.common.isPlaceholderTeam
import com.ixeken.worldcupinfo.ui.common.getTeamDisplayName
import com.ixeken.worldcupinfo.ui.common.formatScorerName
import com.ixeken.worldcupinfo.ui.common.getTeamFlagDrawable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal del Match Center rediseñada siguiendo el esquema de color deportivo
 * y la nueva maquetación de tarjetas con bordes, opacidades y filtros dinámicos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: WorldCupViewModel,
    onSettingsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteTeams by viewModel.favoriteTeams.collectAsStateWithLifecycle()
    val showAlarmsOnly by viewModel.showAlarmsOnly.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val isLiveScoresEnabled by viewModel.isLiveScoresEnabled.collectAsStateWithLifecycle()
    val showFifaCodes by viewModel.showFifaCodes.collectAsStateWithLifecycle()
    val alarmMinutes by viewModel.alarmMinutes.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(isLiveScoresEnabled) {
        if (isLiveScoresEnabled) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    viewModel.syncLiveScores()
                    delay(60000)
                }
            }
        }
    }

    var pendingMatchToToggle by remember { mutableStateOf<Match?>(null) }
    var selectedMatchDetail by remember { mutableStateOf<Match?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentMatches = (uiState as? CalendarState.Success)?.matches ?: emptyList()
    val activeMatchDetail = remember(selectedMatchDetail, currentMatches) {
        selectedMatchDetail?.let { selected ->
            currentMatches.find { it.id == selected.id } ?: selected
        }
    }

    // Toggle status bar icons to dark when the bottom sheet covers the status bar
    val view = LocalView.current
    DisposableEffect(activeMatchDetail != null) {
        val isSheetOpen = activeMatchDetail != null
        if (!view.isInEditMode) {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = isSheetOpen
            }
        }
        onDispose {
            if (!view.isInEditMode) {
                val window = (view.context as? android.app.Activity)?.window
                if (window != null) {
                    androidx.core.view.WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightStatusBars = false
                }
            }
        }
    }

    LaunchedEffect(selectedMatchDetail) {
        selectedMatchDetail?.let { match ->
            val isLiveOrFinished = match.status == MatchStatus.FINISHED ||
                    match.status == MatchStatus.LIVE ||
                    match.status == MatchStatus.HALFTIME ||
                    match.status == MatchStatus.PENALTIES
            if (isLiveOrFinished) {
                viewModel.fetchMatchDetailsOnDemand(match)
            }
        }
    }

    // Launcher para solicitar permisos de notificación en Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingMatchToToggle?.let { match ->
                viewModel.onToggleAlarm(match) { success ->
                    val message = if (success) {
                        context.getString(R.string.toast_alarm_activated, alarmMinutes)
                    } else {
                        context.getString(R.string.toast_alarm_deactivated)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.permission_rationale), Toast.LENGTH_LONG).show()
        }
        pendingMatchToToggle = null
    }

    val onToggleAlarmAction = remember(context, permissionLauncher) {
        { match: Match ->
            val isActivating = !match.isAlarmActive
            if (isActivating && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                pendingMatchToToggle = match
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onToggleAlarm(match) { success ->
                    val message = if (success) {
                        context.getString(R.string.toast_alarm_activated, alarmMinutes)
                    } else {
                        context.getString(R.string.toast_alarm_deactivated)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val onSavePredictionAction = remember(viewModel) {
        { matchId: String, goalsA: Int, goalsB: Int ->
            viewModel.onSavePrediction(matchId, goalsA, goalsB)
        }
    }

    Scaffold(
        containerColor = Color(0xFF386641), // Verde Oscuro de fondo para la cabecera superior
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
                    .background(Color(0xFFF2E8CF)) // Blanco Vainilla de fondo
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
                        // Calcular etapa e inicialización de fecha basada en el día actual
                        val initialSetup = remember(state.matches) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = java.util.TimeZone.getDefault()
                            }
                            val today = sdf.format(Date())
                            val todayMatch = state.matches.find { sdf.format(Date(it.dateUnixTimestamp * 1000)) == today }
                            val targetMatch = todayMatch ?: state.matches.minByOrNull {
                                Math.abs((it.dateUnixTimestamp * 1000) - System.currentTimeMillis())
                            }
                            
                            if (targetMatch != null) {
                                val stageFilter = when (targetMatch.stage) {
                                    MatchStage.GROUPS -> 0
                                    MatchStage.ROUND_OF_32, MatchStage.ROUND_OF_16, MatchStage.QUARTERFINALS, MatchStage.SEMIFINAL -> 1
                                    MatchStage.FINAL, MatchStage.THIRD_PLACE -> 2
                                }
                                val matchDateStr = sdf.format(Date(targetMatch.dateUnixTimestamp * 1000))
                                Pair(stageFilter, matchDateStr)
                            } else {
                                Pair(0, "")
                            }
                        }

                        // Filtro superior por etapa (Píldoras)
                        var selectedStageFilter by remember(initialSetup) { mutableIntStateOf(initialSetup.first) }

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

                        // Extraer fechas únicas de los partidos filtrados
                        val dates = remember(stageFilteredMatches) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = java.util.TimeZone.getDefault()
                            }
                            stageFilteredMatches.map { match ->
                                sdf.format(Date(match.dateUnixTimestamp * 1000))
                            }.distinct().sorted()
                        }

                        var selectedDate by remember(dates, initialSetup) {
                            val matchDate = initialSetup.second
                            mutableStateOf(if (dates.contains(matchDate)) matchDate else (dates.firstOrNull() ?: ""))
                        }

                        if (selectedDate.isNotEmpty() && !dates.contains(selectedDate)) {
                            selectedDate = dates.firstOrNull() ?: ""
                        }

                        val parsedDates = remember(dates) {
                            val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val monthSdf = SimpleDateFormat("MMM", Locale.getDefault()).apply {
                                timeZone = java.util.TimeZone.getDefault()
                            }
                            val daySdf = SimpleDateFormat("d", Locale.getDefault()).apply {
                                timeZone = java.util.TimeZone.getDefault()
                            }
                            dates.map { dateStr ->
                                val date = inputSdf.parse(dateStr) ?: Date()
                                Triple(dateStr, monthSdf.format(date).uppercase(), daySdf.format(date))
                            }
                        }

                        // Filtrar partidos por fecha y filtros adicionales
                        val filteredMatches = remember(stageFilteredMatches, selectedDate, showAlarmsOnly, showFavoritesOnly, favoriteTeams) {
                            var list = stageFilteredMatches
                            if (selectedDate.isNotEmpty()) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                    timeZone = java.util.TimeZone.getDefault()
                                }
                                val parsedDate = try {
                                    sdf.parse(selectedDate)
                                } catch (e: Exception) {
                                    null
                                }
                                if (parsedDate != null) {
                                    val dayStartSeconds = parsedDate.time / 1000
                                    val dayEndSeconds = dayStartSeconds + 86400 - 1
                                    list = list.filter { match ->
                                        match.dateUnixTimestamp in dayStartSeconds..dayEndSeconds
                                    }
                                } else {
                                    // Fallback en caso de error de parseo
                                    list = list.filter { match ->
                                        sdf.format(Date(match.dateUnixTimestamp * 1000)) == selectedDate
                                    }
                                }
                            }
                            if (showAlarmsOnly) {
                                list = list.filter { it.isAlarmActive }
                            }
                            if (showFavoritesOnly) {
                                list = list.filter { favoriteTeams.contains(it.teamA) || favoriteTeams.contains(it.teamB) }
                            }
                            // Ordenar: en vivo primero, luego programados, finalizados al fondo.
                            // Dentro de cada grupo se preserva el orden cronológico ascendente.
                            list.sortedWith(
                                compareBy<Match> { match ->
                                    when (match.status) {
                                        MatchStatus.LIVE, MatchStatus.HALFTIME, MatchStatus.PENALTIES -> 0
                                        MatchStatus.SCHEDULED -> 1
                                        MatchStatus.FINISHED -> 2
                                    }
                                }.thenBy { it.dateUnixTimestamp }
                            )
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Título "Match Center"
                            Text(
                                text = stringResource(id = R.string.title_match_center),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF386641), // Verde Oscuro
                                modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
                            )

                            // Tabs de Etapa
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
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

                            // Selector de Fecha
                            if (parsedDates.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    items(
                                        items = parsedDates,
                                        key = { it.first }
                                    ) { (dateStr, month, day) ->
                                        val isSelected = dateStr == selectedDate
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .width(51.dp)
                                                .height(57.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(if (isSelected) Color(0xFF386641) else Color(0xFFA7C957))
                                                .clickable { selectedDate = dateStr }
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = month,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color(0xFFF9F2E1) else Color(0xFF386641)
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = day,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isSelected) Color(0xFFF9F2E1) else Color(0xFF386641)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Lista de Partidos
                            if (filteredMatches.isEmpty()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.no_matches),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF386641).copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 96.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = filteredMatches,
                                        key = { it.id }
                                    ) { match ->
                                        val isFavorite = favoriteTeams.contains(match.teamA) || favoriteTeams.contains(match.teamB)
                                        
                                        MatchItem(
                                            match = match,
                                            isAlarmActive = match.isAlarmActive,
                                            isFavorite = isFavorite,
                                            isLiveScoresEnabled = isLiveScoresEnabled,
                                            showFifaCodes = showFifaCodes,
                                            onToggleAlarm = {
                                                val isActivating = !match.isAlarmActive
                                                if (isActivating && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    pendingMatchToToggle = match
                                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    viewModel.onToggleAlarm(match) { success ->
                                                        val message = if (success) {
                                                            context.getString(R.string.toast_alarm_activated, alarmMinutes)
                                                        } else {
                                                            context.getString(R.string.toast_alarm_deactivated)
                                                        }
                                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            onClick = { selectedMatchDetail = match }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeMatchDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedMatchDetail = null },
                sheetState = sheetState,
                containerColor = Color(0xFFF2E8CF)
            ) {
                MatchDetailBottomSheetContent(
                    match = activeMatchDetail,
                    allMatches = currentMatches,
                    onToggleAlarm = onToggleAlarmAction,
                    onSavePrediction = onSavePredictionAction,
                    onDismiss = { selectedMatchDetail = null }
                )
            }
        }
    }
}

/**
 * Tarjeta de partido rediseñada con colores Blanco Vainilla Claro,
 * bordes opcionales para favoritos y botón flotante de campana superpuesto.
 */
@Composable
fun MatchItem(
    match: Match,
    isAlarmActive: Boolean,
    isFavorite: Boolean,
    isLiveScoresEnabled: Boolean,
    showFifaCodes: Boolean,
    onToggleAlarm: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeText = remember(match.dateUnixTimestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        sdf.format(Date(match.dateUnixTimestamp * 1000))
    }

    // Nombre limpio del estadio/ciudad
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

    // Regla de opacidad para ganador/perdedor
    val scoreAOpacity = remember(match.status, match.goalsA, match.goalsB) {
        if (match.status == MatchStatus.FINISHED && match.goalsA != null && match.goalsB != null) {
            if (match.goalsA < match.goalsB) 0.6f else 1.0f
        } else 1.0f
    }
    
    val scoreBOpacity = remember(match.status, match.goalsA, match.goalsB) {
        if (match.status == MatchStatus.FINISHED && match.goalsA != null && match.goalsB != null) {
            if (match.goalsB < match.goalsA) 0.6f else 1.0f
        } else 1.0f
    }

    val currentTime = remember { System.currentTimeMillis() / 1000 }
    val kickoff = match.dateUnixTimestamp
    val endWindow = kickoff + 7200 // 2 hours

    val hasStarted = remember(match.status, kickoff, currentTime) {
        match.status == MatchStatus.FINISHED ||
        match.status == MatchStatus.LIVE ||
        match.status == MatchStatus.HALFTIME ||
        match.status == MatchStatus.PENALTIES ||
        currentTime >= kickoff
    }

    val isLive = remember(match.status, kickoff, currentTime) {
        match.status == MatchStatus.LIVE ||
        match.status == MatchStatus.HALFTIME ||
        match.status == MatchStatus.PENALTIES ||
        (currentTime in kickoff until endWindow && match.status != MatchStatus.FINISHED)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "live_dot")
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    val showScoreboard = remember(match.status, isLive, isLiveScoresEnabled) {
        match.status == MatchStatus.FINISHED || (isLive && isLiveScoresEnabled)
    }

    val statusLabel = when {
        match.status == MatchStatus.FINISHED -> stringResource(id = R.string.label_final_score)
        match.status == MatchStatus.HALFTIME -> stringResource(id = R.string.label_halftime)
        match.status == MatchStatus.PENALTIES -> stringResource(id = R.string.label_penalties)
        isLive -> stringResource(id = R.string.label_live)
        else -> stringResource(id = R.string.label_waiting_results)
    }

    val statusColor = when {
        match.status == MatchStatus.FINISHED -> Color(0xFF1F1F1F).copy(alpha = 0.6f)
        isLive -> Color(0xFFBC4749) // Rojo BC4749
        else -> Color(0xFFA7C957) // Verde Claro A7C957
    }

    val context = LocalContext.current

    val teamADisplay = remember(match.teamA, match.stage, match.id, showFifaCodes, context) {
        val formatted = formatTeamCodeForDisplay(match.teamA, match.stage, match.id, context.getString(R.string.label_tbd), context)
        if (formatted.equals(match.teamA.trim(), ignoreCase = true)) {
            if (showFifaCodes) match.teamA.trim().uppercase() else getTeamDisplayName(match.teamA, context)
        } else {
            formatted
        }
    }

    val teamBDisplay = remember(match.teamB, match.stage, match.id, showFifaCodes, context) {
        val formatted = formatTeamCodeForDisplay(match.teamB, match.stage, match.id, context.getString(R.string.label_tbd), context)
        if (formatted.equals(match.teamB.trim(), ignoreCase = true)) {
            if (showFifaCodes) match.teamB.trim().uppercase() else getTeamDisplayName(match.teamB, context)
        } else {
            formatted
        }
    }

    val showDetailsButton = true
    val cardBottomPadding = 18.dp
    val itemVerticalPadding = 6.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = itemVerticalPadding)
    ) {
        // Tarjeta del Partido (Blanco Vainilla Claro)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
            ),
            border = if (isFavorite) BorderStroke(2.5.dp, Color(0xFF386641)) else null, // Borde Verde Oscuro para Favoritos
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = cardBottomPadding) // Deja espacio inferior para la superposición del botón de alarma si aplica
                .clickable { onClick() }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 14.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                // Cabecera: Etapa + Ciudad limpia
                Text(
                    text = "$stageName • $cleanCity",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fila de banderas, nombres y marcador central integrados para centrar el texto con la bandera
                // Row containing Flag A, Scores/Time, Flag B (vertically centered)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Box for Flag A
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        TeamFlagEmojiLarge(teamCode = match.teamA)
                    }

                    // Marcador / Hora central
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.width(160.dp)
                    ) {
                        if (hasStarted && showScoreboard) {
                            Text(
                                text = match.goalsA?.toString() ?: "0",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F).copy(alpha = scoreAOpacity)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (hasStarted) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLive) statusColor.copy(alpha = liveDotAlpha) else statusColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    // Partido Programado: solo muestra la hora
                                    Text(
                                        text = timeText,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1F1F1F),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (showScoreboard && isLive && !match.liveMinute.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = match.liveMinute,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F1F1F).copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (hasStarted && showScoreboard) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = match.goalsB?.toString() ?: "0",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F).copy(alpha = scoreBOpacity)
                            )
                        }
                    }

                    // Box for Flag B
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        TeamFlagEmojiLarge(teamCode = match.teamB)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row containing Name A, Spacer(160.dp), Name B
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AutoResizingText(
                            text = teamADisplay,
                            style = TextStyle(
                                fontFamily = SpaceGrotesk,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F),
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(160.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AutoResizingText(
                            text = teamBDisplay,
                            style = TextStyle(
                                fontFamily = SpaceGrotesk,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }

        // Botón de flecha superpuesto en el borde inferior para ver detalles
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF6A994E))
                .align(Alignment.BottomCenter)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.desc_match_details),
                tint = Color(0xFFF9F2E1), // Blanco Vainilla Claro
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Traduce de forma robusta nombres de estadios con localizaciones complejas a un recurso de ID de ciudad.
 */
fun getCleanCityResId(stadium: String): Int? {
    val lower = stadium.lowercase()
    return when {
        lower.contains("ciudad de méxico") || lower.contains("mexico city") -> R.string.city_mexico_city
        lower.contains("guadalajara") -> R.string.city_guadalajara
        lower.contains("guadalupe") || lower.contains("monterrey") -> R.string.city_monterrey
        lower.contains("toronto") -> R.string.city_toronto
        lower.contains("los ángeles") || lower.contains("los angeles") -> R.string.city_los_angeles
        lower.contains("san francisco") || lower.contains("santa clara") -> R.string.city_san_francisco
        lower.contains("nueva york") || lower.contains("nueva jersey") || lower.contains("new york") || lower.contains("new jersey") -> R.string.city_new_york
        lower.contains("boston") || lower.contains("foxborough") -> R.string.city_boston
        lower.contains("vancouver") -> R.string.city_vancouver
        lower.contains("houston") -> R.string.city_houston
        lower.contains("dallas") || lower.contains("arlington") || lower.contains("texas") -> R.string.city_texas
        lower.isEmpty() || lower.contains("estadio no definido") || lower.contains("stadium undefined") -> R.string.stadium_undefined
        else -> null
    }
}

@Composable
private fun AutoResizingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minFontSize: TextUnit = 9.sp
) {
    var fontSizeValue by remember(text) { mutableStateOf(style.fontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        style = style.copy(fontSize = fontSizeValue),
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && fontSizeValue > minFontSize) {
                fontSizeValue = (fontSizeValue.value - 0.5f).sp
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun MatchDetailBottomSheetContent(
    match: Match,
    allMatches: List<Match>,
    onToggleAlarm: (Match) -> Unit,
    onSavePrediction: (String, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (match.status == MatchStatus.SCHEDULED) {
        UpcomingMatchDetailBottomSheetContent(
            match = match,
            allMatches = allMatches,
            onToggleAlarm = onToggleAlarm,
            onSavePrediction = onSavePrediction,
            onDismiss = onDismiss
        )
    } else {
        LiveOrFinishedMatchDetailBottomSheetContent(
            match = match,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun LiveOrFinishedMatchDetailBottomSheetContent(
    match: Match,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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

    val allEvents = remember(match.eventsDetailsA, match.eventsDetailsB) {
        val listA = match.eventsDetailsA.map { it to true }
        val listB = match.eventsDetailsB.map { it to false }
        (listA + listB).sortedWith(compareBy {
            val minStr = it.first.minute.substringBefore("+").trim()
            minStr.toIntOrNull() ?: 0
        })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp)
    ) {
        // --- 1. HEADER SECTION ---
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
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = if (cleanStadium.isNotBlank()) "$cleanCity • $cleanStadium" else cleanCity,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stageName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Teams and Score Row
                // Flags and Score Row (vertically centered with flags)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Team A Flag Box
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        TeamFlagEmojiLarge(teamCode = match.teamA)
                    }

                    // Scores
                    val isLiveOrHtOrPen = match.status == MatchStatus.LIVE ||
                            match.status == MatchStatus.HALFTIME ||
                            match.status == MatchStatus.PENALTIES

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${match.goalsA ?: 0}",
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            if (isLiveOrHtOrPen) {
                                val statusLabel = when (match.status) {
                                    MatchStatus.HALFTIME -> stringResource(id = R.string.label_halftime)
                                    MatchStatus.PENALTIES -> stringResource(id = R.string.label_penalties)
                                    else -> stringResource(id = R.string.label_live)
                                }
                                Text(
                                    text = statusLabel,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBC4749),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.label_final),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F1F1F),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "${match.goalsB ?: 0}",
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F)
                            )
                        }

                        if (isLiveOrHtOrPen && !match.liveMinute.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = match.liveMinute,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F).copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Team B Flag Box
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        TeamFlagEmojiLarge(teamCode = match.teamB)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Names Row (aligned under flags)
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

                    Spacer(modifier = Modifier.width(144.dp))

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

                // Attendance
                if (match.attendance != null && match.attendance > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val attendanceFormatted = java.text.NumberFormat.getInstance().format(match.attendance)
                    Text(
                        text = stringResource(id = R.string.label_attendance, attendanceFormatted),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.5f)
                    )
                }
            }
        }

        // --- 2. TIMELINE SECTION ---
        if (allEvents.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.label_timeline),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF386641),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF9F2E1)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    allEvents.forEach { (event, isTeamA) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            // Left Side (Team A)
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (isTeamA) {
                                    val nameText = when (event.type) {
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.PENALTY -> context.getString(R.string.event_penalty_suffix, formatScorerName(event.name))
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.OWN_GOAL -> context.getString(R.string.event_own_goal_suffix, formatScorerName(event.name))
                                        else -> formatScorerName(event.name)
                                    }
                                    val annotatedText = buildAnnotatedString {
                                        withStyle(style = SpanStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1F1F1F)
                                        )) {
                                            append(nameText)
                                        }
                                        append(" ")
                                        withStyle(style = SpanStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F1F1F).copy(alpha = 0.6f)
                                        )) {
                                            append("${event.minute}'")
                                        }
                                    }
                                    Text(
                                        text = annotatedText,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Center Icon
                            val eventIcon = when (event.type) {
                                com.ixeken.worldcupinfo.domain.model.MatchEventType.GOAL -> "⚽"
                                com.ixeken.worldcupinfo.domain.model.MatchEventType.PENALTY -> "⚽"
                                com.ixeken.worldcupinfo.domain.model.MatchEventType.OWN_GOAL -> "⚽"
                                com.ixeken.worldcupinfo.domain.model.MatchEventType.YELLOW_CARD -> "🟡"
                                com.ixeken.worldcupinfo.domain.model.MatchEventType.RED_CARD -> "🔴"
                            }
                            Box(
                                modifier = Modifier.width(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = eventIcon,
                                    fontSize = 13.sp
                                )
                            }

                            // Right Side (Team B)
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (!isTeamA) {
                                    val nameText = when (event.type) {
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.PENALTY -> context.getString(R.string.event_penalty_suffix, formatScorerName(event.name))
                                        com.ixeken.worldcupinfo.domain.model.MatchEventType.OWN_GOAL -> context.getString(R.string.event_own_goal_suffix, formatScorerName(event.name))
                                        else -> formatScorerName(event.name)
                                    }
                                    val annotatedText = buildAnnotatedString {
                                        withStyle(style = SpanStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F1F1F).copy(alpha = 0.6f)
                                        )) {
                                            append("${event.minute}'")
                                        }
                                        append(" ")
                                        withStyle(style = SpanStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1F1F1F)
                                        )) {
                                            append(nameText)
                                        }
                                    }
                                    Text(
                                        text = annotatedText,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. STATS SECTION ---
        Text(
            text = stringResource(id = R.string.label_match_stats),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF386641),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
        )

        val stats = match.stats
        val colorA = parseColorSafe(match.teamAColor, Color(0xFF386641))
        val colorB = parseColorSafe(match.teamBColor, Color(0xFFBC4749))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF9F2E1)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            if (stats == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF386641))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.label_syncing_stats),
                        fontSize = 12.sp,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    StatRow(stringResource(id = R.string.stat_possession), stats.possessionA, stats.possessionB, colorA, colorB, "%")
                    StatRow(stringResource(id = R.string.stat_total_shots), stats.shotsA, stats.shotsB, colorA, colorB)
                    StatRow(stringResource(id = R.string.stat_shots_on_target), stats.shotsOnTargetA, stats.shotsOnTargetB, colorA, colorB)
                    StatRow(stringResource(id = R.string.stat_fouls), stats.foulsA, stats.foulsB, colorA, colorB)
                    StatRow(stringResource(id = R.string.stat_corners), stats.cornersA, stats.cornersB, colorA, colorB)
                }
            }
        }

        // --- 4. PREDICTION SECTION ---
        Text(
            text = stringResource(id = R.string.label_your_prediction),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF386641),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF9F2E1)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            if (match.prediction != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF2E8CF))
                    ) {
                        Text(
                            text = "${match.prediction.goalsA}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF386641)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Text(
                        text = "—",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF2E8CF))
                    ) {
                        Text(
                            text = "${match.prediction.goalsB}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF386641)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.label_no_prediction),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun UpcomingMatchDetailBottomSheetContent(
    match: Match,
    allMatches: List<Match>,
    onToggleAlarm: (Match) -> Unit,
    onSavePrediction: (String, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(match) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(10000)
        }
    }

    val kickoffMs = match.dateUnixTimestamp * 1000
    val diffMs = kickoffMs - currentTimeMs
    val hasStarted = currentTimeMs >= kickoffMs
    val isTbdMatch = match.stage != MatchStage.GROUPS && (isPlaceholderTeam(match.teamA) || isPlaceholderTeam(match.teamB))

    val countdownText = if (diffMs > 0) {
        stringResource(id = R.string.starts_in, formatCountdown(diffMs, context))
    } else {
        stringResource(id = R.string.starts_in, stringResource(id = R.string.countdown_minutes, 0))
    }

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

    val matchTimeText = remember(match.dateUnixTimestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        sdf.format(Date(match.dateUnixTimestamp * 1000))
    }

    val statsA = remember(allMatches, match.teamA, context) {
        calculateTeamStats(context, allMatches, match.teamA, match.teamB, match.group)
    }
    val statsB = remember(allMatches, match.teamB, context) {
        calculateTeamStats(context, allMatches, match.teamB, match.teamA, match.group)
    }

    val posColorA = remember(statsA.positionVal, statsB.positionVal) {
        when {
            statsA.positionVal < statsB.positionVal -> Color(0xFF6A994E)
            statsA.positionVal > statsB.positionVal -> Color(0xFFBC4749)
            else -> Color(0xFF1F1F1F)
        }
    }
    val posColorB = remember(statsA.positionVal, statsB.positionVal) {
        when {
            statsB.positionVal < statsA.positionVal -> Color(0xFF6A994E)
            statsB.positionVal > statsA.positionVal -> Color(0xFFBC4749)
            else -> Color(0xFF1F1F1F)
        }
    }

    val ptsColorA = remember(statsA.totalPoints, statsB.totalPoints) {
        when {
            statsA.totalPoints > statsB.totalPoints -> Color(0xFF6A994E)
            statsA.totalPoints < statsB.totalPoints -> Color(0xFFBC4749)
            else -> Color(0xFF1F1F1F)
        }
    }
    val ptsColorB = remember(statsA.totalPoints, statsB.totalPoints) {
        when {
            statsB.totalPoints > statsA.totalPoints -> Color(0xFF6A994E)
            statsB.totalPoints < statsA.totalPoints -> Color(0xFFBC4749)
            else -> Color(0xFF1F1F1F)
        }
    }

    val goalsColorA = remember(statsA.totalGoals, statsB.totalGoals) {
        when {
            statsA.totalGoals > statsB.totalGoals -> Color(0xFF6A994E)
            statsA.totalGoals < statsB.totalGoals -> Color(0xFFBC4749)
            else -> Color(0xFF1F1F1F)
        }
    }
    val goalsColorB = remember(statsA.totalGoals, statsB.totalGoals) {
        when {
            statsB.totalGoals > statsA.totalGoals -> Color(0xFF6A994E)
            statsB.totalGoals < statsA.totalGoals -> Color(0xFFBC4749)
            else -> Color(0xFF1F1F1F)
        }
    }

    var predA by remember { mutableIntStateOf(match.prediction?.goalsA ?: 0) }
    var predB by remember { mutableIntStateOf(match.prediction?.goalsB ?: 0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- HEADER SECTION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp) // Leave space for overlapping button
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
                    Spacer(modifier = Modifier.height(20.dp))

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
                            modifier = Modifier.width(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = matchTimeText,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F1F1F),
                                textAlign = TextAlign.Center
                            )
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

                        Spacer(modifier = Modifier.width(120.dp))

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

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFF2E8CF)) // Color Vainilla
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = countdownText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF386641)
                        )
                    }
                }
            }

            IconButton(
                onClick = { onToggleAlarm(match) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6A994E))
                    .align(Alignment.BottomCenter)
            ) {
                Icon(
                    imageVector = if (match.isAlarmActive) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                    contentDescription = stringResource(id = if (match.isAlarmActive) R.string.alert_on else R.string.alert_off),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TEAMS INFO SECTION ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.label_teams_info),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF386641)
            )
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF9F2E1)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.label_position_group),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386641)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statsA.position,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = posColorA
                    )
                    Text(
                        text = statsB.position,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = posColorB
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.label_total_points),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386641)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statsA.totalPoints.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = ptsColorA
                    )
                    Text(
                        text = statsB.totalPoints.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = ptsColorB
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.label_goals_scored),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386641)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statsA.totalGoals.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = goalsColorA
                    )
                    Text(
                        text = statsB.totalGoals.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = goalsColorB
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.label_last_matches),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386641),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FormDotsRow(statsA.form)
                    FormDotsRow(statsB.form)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            if (isTbdMatch) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF9F2E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
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
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF9F2E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
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
                                // Bandera cuadrada de 32.dp
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

                            // Center (VS separator)
                            Text(
                                text = stringResource(id = R.string.label_vs),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF386641).copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 24.dp)
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
                                // Bandera cuadrada de 32.dp
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
                    if (predA == predB && match.stage != MatchStage.GROUPS) {
                        Toast.makeText(context, context.getString(R.string.error_draws_groups_only), Toast.LENGTH_LONG).show()
                        return@IconButton
                    }
                    onSavePrediction(match.id, predA, predB)
                    Toast.makeText(context, context.getString(R.string.prediction_saved_success), Toast.LENGTH_LONG).show()
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

data class TeamTournamentStats(
    val position: String,
    val positionVal: Int,
    val totalPoints: Int,
    val totalGoals: Int,
    val form: List<Char>
)

fun calculateTeamStats(
    context: android.content.Context,
    allMatches: List<Match>,
    teamCode: String,
    opponentCode: String,
    group: String?
): TeamTournamentStats {
    val standingsMap = calculateStandings(allMatches)
    val groupName = group ?: allMatches.firstOrNull {
        it.stage == MatchStage.GROUPS && (it.teamA == teamCode || it.teamB == teamCode)
    }?.group

    val groupStandings = standingsMap[groupName] ?: emptyList()
    val teamStandingIndex = groupStandings.indexOfFirst { it.teamCode == teamCode }
    val positionVal = if (teamStandingIndex != -1) teamStandingIndex + 1 else 5
    val position = if (teamStandingIndex != -1) {
        val pos = teamStandingIndex + 1
        when (pos) {
            1 -> context.getString(R.string.position_1st)
            2 -> context.getString(R.string.position_2nd)
            3 -> context.getString(R.string.position_3rd)
            else -> context.getString(R.string.position_nth, pos)
        }
    } else {
        "-"
    }

    var totalPoints = 0
    var totalGoals = 0
    val formList = mutableListOf<Char>()

    val finishedMatches = allMatches.filter {
        it.status == MatchStatus.FINISHED && (it.teamA == teamCode || it.teamB == teamCode)
    }.sortedBy { it.dateUnixTimestamp }

    for (m in finishedMatches) {
        val isTeamA = m.teamA == teamCode
        val goalsThis = if (isTeamA) m.goalsA else m.goalsB
        val goalsOpp = if (isTeamA) m.goalsB else m.goalsA

        if (goalsThis != null && goalsOpp != null) {
            totalGoals += goalsThis
            if (m.stage == MatchStage.GROUPS) {
                if (goalsThis > goalsOpp) {
                    totalPoints += 3
                } else if (goalsThis == goalsOpp) {
                    totalPoints += 1
                }
            }
            if (goalsThis > goalsOpp) {
                formList.add('W')
            } else if (goalsThis < goalsOpp) {
                formList.add('L')
            } else {
                formList.add('D')
            }
        }
    }

    return TeamTournamentStats(
        position = position,
        positionVal = positionVal,
        totalPoints = totalPoints,
        totalGoals = totalGoals,
        form = formList
    )
}

fun formatCountdown(diffMs: Long, context: android.content.Context): String {
    val diffSec = diffMs / 1000
    val diffMin = (diffSec / 60) % 60
    val diffHr = (diffSec / 3600) % 24
    val diffDay = diffSec / 86400

    return when {
        diffDay > 0 -> context.getString(R.string.countdown_days, diffDay, diffHr, diffMin)
        diffHr > 0 -> context.getString(R.string.countdown_hours, diffHr, diffMin)
        else -> context.getString(R.string.countdown_minutes, diffMin)
    }
}

@Composable
fun FormDotsRow(form: List<Char>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        form.forEach { outcome ->
            val color = when (outcome) {
                'W' -> Color(0xFF6A994E)
                'L' -> Color(0xFFBC4749)
                else -> Color(0xFFEADBBE)
            }
            val border = if (outcome == 'D') BorderStroke(1.dp, Color(0xFF1F1F1F).copy(alpha = 0.3f)) else null

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            )
        }
    }
}

@Composable
fun StatRow(
    title: String,
    valA: Int?,
    valB: Int?,
    colorA: Color,
    colorB: Color,
    suffix: String = ""
) {
    if (valA == null && valB == null) return

    val cleanA = valA ?: 0
    val cleanB = valB ?: 0
    val total = cleanA + cleanB
    val weightA = if (total > 0) cleanA.toFloat() / total else 0.5f
    val weightB = if (total > 0) cleanB.toFloat() / total else 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Title centered above the bar
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF386641),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Row containing left value, progress bar, right value
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${valA ?: 0}$suffix",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                if (weightA > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weightA)
                            .background(colorA)
                    )
                }
                if (weightB > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weightB)
                            .background(colorB)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${valB ?: 0}$suffix",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

fun parseColorSafe(hexStr: String?, fallback: Color): Color {
    if (hexStr.isNullOrEmpty()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hexStr))
    } catch (e: Exception) {
        fallback
    }
}

