package com.ixeken.worldcupinfo.ui.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.BackHandler
import com.ixeken.worldcupinfo.R
import com.ixeken.worldcupinfo.ui.calendar.CalendarState
import com.ixeken.worldcupinfo.ui.calendar.WorldCupViewModel
import com.ixeken.worldcupinfo.ui.common.AppHeader
import com.ixeken.worldcupinfo.ui.common.TeamFlagEmoji
import com.ixeken.worldcupinfo.ui.common.getTeamDisplayName
import com.ixeken.worldcupinfo.domain.model.MatchStage

/**
 * Screen displaying the user's favorite teams with the ability to add/remove via a bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: WorldCupViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteTeams by viewModel.favoriteTeams.collectAsStateWithLifecycle()
    val showAlarmsOnly by viewModel.showAlarmsOnly.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var tempSelected by remember { mutableStateOf(setOf<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Toggle status bar icons to dark when the bottom sheet covers the status bar
    val view = LocalView.current
    DisposableEffect(showBottomSheet) {
        if (!view.isInEditMode) {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = showBottomSheet
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
                onBackClick = onBackClick,
                isFavoritesActive = true,
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
                        val allTeamsList = remember(state.matches) {
                            state.matches
                                .filter { it.stage == MatchStage.GROUPS }
                                .flatMap { listOf(it.teamA, it.teamB) }
                                .distinct()
                                .sortedBy { getTeamDisplayName(it, context) }
                        }

                        val sortedFavorites = remember(favoriteTeams) {
                            favoriteTeams.toList().sortedBy { getTeamDisplayName(it, context) }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 24.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.title_favorites),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF386641),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                if (sortedFavorites.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.no_favorites),
                                            color = Color(0xFF1F1F1F).copy(alpha = 0.5f),
                                            fontSize = 15.sp,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(bottom = 80.dp) // Deja espacio para el FAB
                                    ) {
                                        items(
                                            items = sortedFavorites,
                                            key = { it }
                                        ) { teamCode ->
                                            FavoriteTeamCard(
                                                teamCode = teamCode,
                                                onRemove = { viewModel.onToggleFavorite(teamCode) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Floating "Add" Button
                            Button(
                                onClick = {
                                    tempSelected = favoriteTeams
                                    showBottomSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF386641) // Verde Oscuro
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 24.dp, end = 20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    AddIcon()
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.action_add),
                                        color = Color(0xFFF9F2E1),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Bottom Sheet for adding favorites
                        if (showBottomSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showBottomSheet = false },
                                sheetState = sheetState,
                                containerColor = Color(0xFFF2E8CF), // Blanco Vainilla
                                dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF1F1F1F).copy(alpha = 0.2f)) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            items = allTeamsList,
                                            key = { it }
                                        ) { teamCode ->
                                            val isSelected = tempSelected.contains(teamCode)
                                            Card(
                                                shape = RoundedCornerShape(20.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
                                                ),
                                                border = if (isSelected) BorderStroke(2.5.dp, Color(0xFF386641)) else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        tempSelected = if (isSelected) {
                                                            tempSelected - teamCode
                                                        } else {
                                                            tempSelected + teamCode
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 20.dp, vertical = 14.dp)
                                                ) {
                                                    TeamFlagEmoji(teamCode = teamCode)
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(
                                                        text = getTeamDisplayName(teamCode, context),
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1F1F1F)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Floating "Save" Button inside bottom sheet
                                    Button(
                                        onClick = {
                                            viewModel.onSaveAllFavorites(tempSelected)
                                            showBottomSheet = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF386641) // Verde Oscuro
                                        ),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(bottom = 24.dp, end = 20.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            SaveIcon()
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(id = R.string.action_save_mixed),
                                                color = Color(0xFFF9F2E1),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
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
}

/**
 * Card representing a single favorite team in the grid.
 */
@Composable
fun FavoriteTeamCard(
    teamCode: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onRemove() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            TeamFlagEmoji(teamCode = teamCode)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = getTeamDisplayName(teamCode, context),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


// Fin de FavoritesScreen

/**
 * Premium custom drawn Add (+) icon.
 */
@Composable
fun AddIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height
        // Horizontal line
        drawLine(
            color = Color(0xFFF9F2E1), // Blanco Vainilla Claro
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = strokeWidth
        )
        // Vertical line
        drawLine(
            color = Color(0xFFF9F2E1),
            start = Offset(w * 0.5f, h * 0.2f),
            end = Offset(w * 0.5f, h * 0.8f),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Premium custom drawn Save (download) icon.
 */
@Composable
fun SaveIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height
        // Downward arrow: vertical line
        drawLine(
            color = Color(0xFFF9F2E1), // Blanco Vainilla Claro
            start = Offset(w * 0.5f, h * 0.15f),
            end = Offset(w * 0.5f, h * 0.6f),
            strokeWidth = strokeWidth
        )
        // Arrow head left
        drawLine(
            color = Color(0xFFF9F2E1),
            start = Offset(w * 0.3f, h * 0.4f),
            end = Offset(w * 0.5f, h * 0.6f),
            strokeWidth = strokeWidth
        )
        // Arrow head right
        drawLine(
            color = Color(0xFFF9F2E1),
            start = Offset(w * 0.7f, h * 0.4f),
            end = Offset(w * 0.5f, h * 0.6f),
            strokeWidth = strokeWidth
        )
        // Bottom horizontal bar
        drawLine(
            color = Color(0xFFF9F2E1),
            start = Offset(w * 0.2f, h * 0.85f),
            end = Offset(w * 0.8f, h * 0.85f),
            strokeWidth = strokeWidth
        )
    }
}
