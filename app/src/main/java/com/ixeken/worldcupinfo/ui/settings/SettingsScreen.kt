package com.ixeken.worldcupinfo.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.core.content.ContextCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ixeken.worldcupinfo.R
import com.ixeken.worldcupinfo.ui.calendar.WorldCupViewModel
import com.ixeken.worldcupinfo.ui.common.AppHeader

import android.app.AlarmManager
import android.app.LocaleManager
import android.os.LocaleList
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner

/**
 * Pantalla de ajustes rediseñada con el esquema de color deportivo vainilla
 * y la cabecera verde oscuro coherente con el Match Center.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WorldCupViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isKickOffAlertEnabled by remember { mutableStateOf(true) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showAppearanceSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val isNotificationsGranted = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val isBatteryIgnored = remember {
        mutableStateOf(
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        )
    }
    val isExactAlarmGranted = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else true
        )
    }

    // Actualiza los estados de permisos cuando la app vuelve al primer plano
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationsGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true

                isBatteryIgnored.value = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .isIgnoringBatteryOptimizations(context.packageName)

                isExactAlarmGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.canScheduleExactAlarms()
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val showAlarmsOnly by viewModel.showAlarmsOnly.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val isLiveScoresEnabled by viewModel.isLiveScoresEnabled.collectAsStateWithLifecycle()
    val showFifaCodes by viewModel.showFifaCodes.collectAsStateWithLifecycle()
    val fontStyle by viewModel.fontStyle.collectAsStateWithLifecycle()
    val alarmMinutes by viewModel.alarmMinutes.collectAsStateWithLifecycle()

    val minuteOptions = listOf(5, 10, 15, 30, 45, 60)

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
                showFavoritesOnly = showFavoritesOnly,
                onToggleShowFavoritesOnly = { viewModel.onToggleShowFavoritesOnly() },
                onBackClick = onBackClick
            )

            // 2. Contenedor principal de datos (Blanco Vainilla)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFFF2E8CF)) // Blanco Vainilla
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Título "Settings"
                Text(
                    text = stringResource(id = R.string.settings_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF386641), // Verde Oscuro
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- SECCIÓN: MATCH SETTINGS ---
                SettingsHeader(text = stringResource(id = R.string.settings_reminders_header))

                // Dropdown de aviso previo
                SettingsDropdownItem(
                    title = stringResource(id = R.string.settings_warning_label),
                    description = stringResource(id = R.string.settings_warning_desc),
                    selectedValue = stringResource(
                        id = when (alarmMinutes) {
                            5 -> R.string.minutes_5
                            10 -> R.string.minutes_10
                            15 -> R.string.minutes_15
                            30 -> R.string.minutes_30
                            45 -> R.string.minutes_45
                            60 -> R.string.minutes_60
                            else -> R.string.minutes_5
                        }
                    ),
                    options = minuteOptions.map { minutes ->
                        stringResource(
                            id = when (minutes) {
                                5 -> R.string.minutes_5
                                10 -> R.string.minutes_10
                                15 -> R.string.minutes_15
                                30 -> R.string.minutes_30
                                45 -> R.string.minutes_45
                                60 -> R.string.minutes_60
                                else -> R.string.minutes_5
                            }
                        )
                    },
                    onOptionSelected = { index ->
                        viewModel.onChangeAlarmMinutes(minuteOptions[index])
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                SettingsSwitchItem(
                    title = stringResource(id = R.string.settings_kickoff_title),
                    description = stringResource(id = R.string.settings_kickoff_desc),
                    checked = isKickOffAlertEnabled,
                    onCheckedChange = { isKickOffAlertEnabled = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSwitchItem(
                    title = stringResource(id = R.string.settings_live_scores_title),
                    description = stringResource(id = R.string.settings_live_scores_desc),
                    checked = isLiveScoresEnabled,
                    onCheckedChange = { viewModel.onToggleLiveScores(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- SECCIÓN: GENERAL ---
                SettingsHeader(text = stringResource(id = R.string.settings_general_header))

                SettingsClickItem(
                    title = stringResource(id = R.string.settings_permissions_title),
                    description = stringResource(id = R.string.settings_optimize_delivery_desc),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { showPermissionsSheet = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- SECCIÓN: PREFERENCES ---
                SettingsHeader(text = stringResource(id = R.string.settings_preferences_header))

                SettingsSwitchItem(
                    title = stringResource(id = R.string.settings_show_fifa_codes_title),
                    description = stringResource(id = R.string.settings_show_fifa_codes_desc),
                    checked = showFifaCodes,
                    onCheckedChange = { viewModel.onToggleShowFifaCodes(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsClickItem(
                    title = stringResource(id = R.string.settings_appearance_title),
                    description = stringResource(id = R.string.settings_appearance_desc),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { showAppearanceSheet = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- SECCIÓN: ABOUT ---
                SettingsHeader(text = stringResource(id = R.string.settings_about_header))

                SettingsClickItem(
                    title = stringResource(id = R.string.settings_view_changelog_title),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { showChangelogDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsClickItem(
                    title = stringResource(id = R.string.settings_project_repo_title),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ixeken-Studios/ulama-app"))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsClickItem(
                    title = stringResource(id = R.string.settings_data_repo_title),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/openfootball/worldcup.json"))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsClickItem(
                    title = stringResource(id = R.string.settings_created_by_title),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.EmojiEmotions,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ixeken-Studios"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    // Diálogo del Historial de Cambios (Changelog) con estilo coordinado
    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.settings_view_changelog_title),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.changelog_text),
                    color = Color(0xFF1F1F1F).copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            containerColor = Color(0xFFF9F2E1), // Blanco Vainilla Claro
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    Text(stringResource(id = R.string.action_ok), color = Color(0xFF386641))
                }
            }
        )
    }

    // Bottom Sheet de Permisos
    if (showPermissionsSheet) {
        val notifPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            isNotificationsGranted.value = isGranted
        }

        ModalBottomSheet(
            onDismissRequest = { showPermissionsSheet = false },
            containerColor = Color(0xFFF2E8CF),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF1F1F1F).copy(alpha = 0.2f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Sección 1: Permisos Utilizados (Solo Notificaciones)
                Text(
                    text = stringResource(id = R.string.permissions_sheet_used_header),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PermissionStatusCard(
                    title = stringResource(id = R.string.permissions_sheet_notifications),
                    description = stringResource(id = R.string.permissions_sheet_notifications_desc),
                    icon = Icons.Filled.Notifications,
                    isGranted = isNotificationsGranted.value,
                    onGrantAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sección 2: Estabilidad de la app
                Text(
                    text = stringResource(id = R.string.permissions_sheet_stability),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(id = R.string.permissions_sheet_stability_desc),
                    fontSize = 13.sp,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PermissionStatusCard(
                    title = stringResource(id = R.string.dialog_optimize_alarms),
                    description = stringResource(id = R.string.dialog_optimize_alarms_desc),
                    icon = Icons.Filled.Timer,
                    isGranted = isExactAlarmGranted.value,
                    onGrantAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_exact_alarm_not_required), Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionStatusCard(
                    title = stringResource(id = R.string.dialog_optimize_battery),
                    description = stringResource(id = R.string.dialog_optimize_battery_desc),
                    icon = Icons.Filled.BatteryAlert,
                    isGranted = isBatteryIgnored.value,
                    onGrantAction = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Botón: Revocar Permisos (Acceso directo a la configuración de la app)
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBC4749) // Color rojo de advertencia/revocar
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.settings_revoke_perm_title),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Bottom Sheet de Apariencia
    if (showAppearanceSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAppearanceSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFFF2E8CF),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF1F1F1F).copy(alpha = 0.2f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Sección: Estilo de fuente
                Text(
                    text = stringResource(id = R.string.appearance_font_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FontOptionChip(
                        text = stringResource(id = R.string.appearance_font_system),
                        selected = fontStyle == "system",
                        onClick = {
                            viewModel.onChangeFontStyle("system")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FontOptionChip(
                        text = stringResource(id = R.string.appearance_font_space_grotesk),
                        selected = fontStyle == "space_grotesk",
                        onClick = {
                            viewModel.onChangeFontStyle("space_grotesk")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sección: Idioma
                Text(
                    text = stringResource(id = R.string.appearance_language_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val localeManager = context.getSystemService(LocaleManager::class.java)
                val appLocales = localeManager.applicationLocales
                val isSpanish = if (appLocales.isEmpty) {
                    java.util.Locale.getDefault().language == "es"
                } else {
                    appLocales[0]?.language == "es"
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FontOptionChip(
                        text = stringResource(id = R.string.appearance_language_spanish),
                        selected = isSpanish,
                        onClick = {
                            showAppearanceSheet = false
                            localeManager.applicationLocales = LocaleList.forLanguageTags("es")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FontOptionChip(
                        text = stringResource(id = R.string.appearance_language_english),
                        selected = !isSpanish,
                        onClick = {
                            showAppearanceSheet = false
                            localeManager.applicationLocales = LocaleList.forLanguageTags("en")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun FontOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF386641) else Color(0xFFF9F2E1)
        ),
        modifier = modifier
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Color(0xFF1F1F1F),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun PermissionStatusCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onGrantAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(
        onClick = { if (!isGranted) onGrantAction() },
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) Color(0xFF6A994E) else Color(0xFF1F1F1F).copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color.White else Color(0xFF1F1F1F).copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF6A994E) else Color(0xFFBC4749),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
fun SettingsHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1F1F1F).copy(alpha = 0.7f),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9F2E1) // Blanco Vainilla Claro
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    ) {
        content()
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6A994E),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF1F1F1F).copy(alpha = 0.15f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun SettingsDropdownItem(
    title: String,
    description: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    SettingsCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2E8CF))
                        .clickable { expanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = Color(0xFF6A994E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = selectedValue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F1F1F),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF1F1F1F).copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFFF9F2E1))
                ) {
                    options.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1F1F1F)
                                )
                            },
                            onClick = {
                                onOptionSelected(index)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsClickItem(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    SettingsCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6A994E)) // Verde
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1F1F)
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color(0xFF1F1F1F).copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF1F1F1F).copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


