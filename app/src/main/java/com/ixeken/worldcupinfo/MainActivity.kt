package com.ixeken.worldcupinfo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ixeken.worldcupinfo.ui.calendar.CalendarScreen
import com.ixeken.worldcupinfo.ui.groups.GroupsScreen
import com.ixeken.worldcupinfo.ui.predictions.PredictionsScreen
import com.ixeken.worldcupinfo.ui.settings.SettingsScreen
import com.ixeken.worldcupinfo.ui.favorites.FavoritesScreen
import com.ixeken.worldcupinfo.ui.theme.WorldCupInfoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad principal de la aplicación, anotada con AndroidEntryPoint para inyección por Hilt.
 * Incorpora la barra de navegación flotante customizada de Material Design 3 con animaciones.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: com.ixeken.worldcupinfo.ui.calendar.WorldCupViewModel = hiltViewModel()
            val fontStyle by viewModel.fontStyle.collectAsStateWithLifecycle()

            WorldCupInfoTheme(fontStyle = fontStyle) {
                var selectedTab by remember { mutableIntStateOf(0) }
                var lastSelectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    bottomBar = {
                        // Ocultar la barra de navegación cuando se está en la pantalla de Ajustes (Tab 3) o Favoritos (Tab 4)
                        if (selectedTab != 3 && selectedTab != 4) {
                            CustomBottomNavBar(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val screenModifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = 0.dp,
                            bottom = 0.dp
                        )
                    when (selectedTab) {
                        0 -> CalendarScreen(
                            viewModel = viewModel,
                            modifier = screenModifier,
                            onSettingsClick = {
                                lastSelectedTab = 0
                                selectedTab = 3
                            },
                            onFavoritesClick = {
                                lastSelectedTab = 0
                                selectedTab = 4
                            }
                        )
                        1 -> GroupsScreen(
                            viewModel = viewModel,
                            modifier = screenModifier,
                            onSettingsClick = {
                                lastSelectedTab = 1
                                selectedTab = 3
                            },
                            onFavoritesClick = {
                                lastSelectedTab = 1
                                selectedTab = 4
                            }
                        )
                        2 -> PredictionsScreen(
                            viewModel = viewModel,
                            modifier = screenModifier,
                            onSettingsClick = {
                                lastSelectedTab = 2
                                selectedTab = 3
                            },
                            onFavoritesClick = {
                                lastSelectedTab = 2
                                selectedTab = 4
                            }
                        )
                        3 -> SettingsScreen(
                            viewModel = viewModel,
                            modifier = screenModifier,
                            onBackClick = { selectedTab = lastSelectedTab }
                        )
                        4 -> FavoritesScreen(
                            viewModel = viewModel,
                            modifier = screenModifier,
                            onBackClick = { selectedTab = lastSelectedTab }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Barra de navegación flotante premium inspirada en Material Design 3 con animaciones y
 * el esquema de colores deportivos configurado.
 */
@Composable
fun CustomBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF386641)) // Verde Oscuro
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Tab 0: Matches
            NavBarTab(
                selected = selectedTab == 0,
                icon = { tint ->
                    // Icono de calendario con punto
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val strokeWidth = 2.dp.toPx()
                        val w = size.width
                        val h = size.height
                        
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(w * 0.12f, h * 0.2f),
                            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.7f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                        
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.12f, h * 0.44f),
                            end = Offset(w * 0.88f, h * 0.44f),
                            strokeWidth = strokeWidth
                        )
                        
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.32f, h * 0.08f),
                            end = Offset(w * 0.32f, h * 0.28f),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.68f, h * 0.08f),
                            end = Offset(w * 0.68f, h * 0.28f),
                            strokeWidth = strokeWidth
                        )
                        
                        drawCircle(
                            color = tint,
                            radius = 2.2f.dp.toPx(),
                            center = Offset(w * 0.36f, h * 0.68f)
                        )
                    }
                },
                text = stringResource(id = R.string.tab_matches_nav),
                onClick = { onTabSelected(0) }
            )

            // Tab 1: Groups
            NavBarTab(
                selected = selectedTab == 1,
                icon = { tint ->
                    // Icono de lista (3 viñetas + 3 líneas)
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val strokeWidth = 2.dp.toPx()
                        val w = size.width
                        val h = size.height
                        
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.38f, h * 0.25f),
                            end = Offset(w * 0.88f, h * 0.25f),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.38f, h * 0.5f),
                            end = Offset(w * 0.88f, h * 0.5f),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.38f, h * 0.75f),
                            end = Offset(w * 0.88f, h * 0.75f),
                            strokeWidth = strokeWidth
                        )
                        
                        drawCircle(
                            color = tint,
                            radius = 2.dp.toPx(),
                            center = Offset(w * 0.16f, h * 0.25f)
                        )
                        drawCircle(
                            color = tint,
                            radius = 2.dp.toPx(),
                            center = Offset(w * 0.16f, h * 0.5f)
                        )
                        drawCircle(
                            color = tint,
                            radius = 2.dp.toPx(),
                            center = Offset(w * 0.16f, h * 0.75f)
                        )
                    }
                },
                text = stringResource(id = R.string.tab_groups_nav),
                onClick = { onTabSelected(1) }
            )

            // Tab 2: Predictions
            NavBarTab(
                selected = selectedTab == 2,
                icon = { tint ->
                    // Icono de papel y lápiz dibujado en Canvas
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val strokeWidth = 2.dp.toPx()
                        val w = size.width
                        val h = size.height
                        
                        // Papel (Rectángulo redondeado al fondo)
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(w * 0.12f, h * 0.15f),
                            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.7f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                        
                        // Líneas de texto en el papel
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.22f, h * 0.35f),
                            end = Offset(w * 0.48f, h * 0.35f),
                            strokeWidth = 1.5f.dp.toPx()
                        )
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.22f, h * 0.55f),
                            end = Offset(w * 0.48f, h * 0.55f),
                            strokeWidth = 1.5f.dp.toPx()
                        )
                        
                        // Lápiz cruzando diagonalmente a la derecha
                        val pencilPath = androidx.compose.ui.graphics.Path().apply {
                            // Punta
                            moveTo(w * 0.58f, h * 0.82f)
                            lineTo(w * 0.55f, h * 0.85f) // Punta extrema
                            lineTo(w * 0.62f, h * 0.85f)
                            lineTo(w * 0.65f, h * 0.75f)
                            close()
                        }
                        drawPath(
                            path = pencilPath,
                            color = tint
                        )
                        
                        // Cuerpo del lápiz
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.62f, h * 0.78f),
                            end = Offset(w * 0.88f, h * 0.3f),
                            strokeWidth = 3.dp.toPx()
                        )
                        
                        // Borrador del lápiz
                        drawCircle(
                            color = tint,
                            radius = 1.8f.dp.toPx(),
                            center = Offset(w * 0.88f, h * 0.3f)
                        )
                    }
                },
                text = stringResource(id = R.string.nav_predictions),
                onClick = { onTabSelected(2) }
            )
        }
    }
}

/**
 * Tab individual animada para la barra de navegación flotante.
 */
@Composable
fun NavBarTab(
    selected: Boolean,
    icon: @Composable (Color) -> Unit,
    text: String,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF6A994E) else Color.Transparent,
        label = "tab_bg"
    )
    val contentColor = Color(0xFFF2E8CF) // Blanco Vainilla

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        icon(contentColor)
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )
        }
    }
}