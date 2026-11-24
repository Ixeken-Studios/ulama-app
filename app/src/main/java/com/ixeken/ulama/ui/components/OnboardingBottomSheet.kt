package com.ixeken.ulama.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.AppSettings
import com.ixeken.ulama.core.model.ColorPalette
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.ThemeMode
import com.ixeken.ulama.ui.settings.ColorPaletteSettingCard
import com.ixeken.ulama.ui.settings.NotificationLeadSettingCard
import com.ixeken.ulama.ui.settings.NotificationPermissionSettingCard
import com.ixeken.ulama.ui.settings.ThemeModeSettingCard
import com.ixeken.ulama.ui.theme.UlamaTheme
import kotlinx.coroutines.launch

/**
 * Hoja modal interactiva de bienvenida y configuracion inicial para Ulama.
 * Presenta un recorrido de 3 pasos mediante un visor horizontal reutilizando
 * literalmente las tarjetas de diseno de Ajustes para garantizar total coherencia visual.
 *
 * @param appSettings Preferencias actuales del sistema.
 * @param selectedLeague Competicion actualmente seleccionada.
 * @param onThemeModeChanged Accion al conmutar entre tema claro, oscuro o del sistema.
 * @param onColorPaletteChanged Accion al seleccionar una paleta de color.
 * @param onLeagueSelected Accion al fijar la competicion predeterminada.
 * @param onNotificationLeadMinutesChanged Accion para actualizar la anticipacion de alertas.
 * @param onCompleteOnboarding Callback al finalizar o descartar el recorrido.
 * @param onDismissRequest Accion ejecutada al cerrar la hoja modal.
 * @param modifier Modificador base del composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingBottomSheet(
    appSettings: AppSettings,
    selectedLeague: League,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onColorPaletteChanged: (ColorPalette) -> Unit,
    onLeagueSelected: (League) -> Unit,
    onNotificationLeadMinutesChanged: (Int) -> Unit,
    onCompleteOnboarding: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        },
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UlamaTheme.spacing.normal)
                .padding(bottom = UlamaTheme.spacing.listBottomPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                // El peso sin relleno obliga a medir primero el indicador y los botones, de modo que un
                // paso alto como la cuadricula de ligas se desplace por dentro en vez de empujar la
                // navegacion fuera de la hoja.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) { page ->
                    when (page) {
                        0 -> OnboardingStepAppearance(
                            appSettings = appSettings,
                            onThemeModeChanged = onThemeModeChanged,
                            onColorPaletteChanged = onColorPaletteChanged
                        )
                        1 -> OnboardingStepLeague(
                            selectedLeague = selectedLeague,
                            onLeagueSelected = onLeagueSelected
                        )
                        2 -> OnboardingStepNotifications(
                            leadMinutes = appSettings.notificationLeadMinutes,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onNotificationLeadMinutesChanged = onNotificationLeadMinutesChanged
                        )
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.medium))

                // Indicador de avance con pastillas animadas de Material 3 Expressive
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            label = "onboarding_indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(indicatorWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                        )
                        if (index < 2) {
                            Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.medium))

                // Controles inferiores de navegacion
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.onboarding_action_back))
                        }
                    } else {
                        TextButton(
                            onClick = onCompleteOnboarding
                        ) {
                            Text(text = stringResource(R.string.onboarding_action_skip))
                        }
                    }

                    if (pagerState.currentPage < 2) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = stringResource(R.string.onboarding_action_next))
                            Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onCompleteOnboarding,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = stringResource(R.string.onboarding_action_finish))
                            Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Paso 1 del recorrido: presentacion visual reutilizando directamente las tarjetas de Ajustes.
 */
@Composable
private fun OnboardingStepAppearance(
    appSettings: AppSettings,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onColorPaletteChanged: (ColorPalette) -> Unit
) {
    val scrollState = rememberScrollState()
    val topConnectedShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardRestRadius,
        topEnd = UlamaTheme.shapes.cardRestRadius,
        bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
    )
    val bottomConnectedShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomStart = UlamaTheme.shapes.cardRestRadius,
        bottomEnd = UlamaTheme.shapes.cardRestRadius
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = UlamaTheme.spacing.small)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

        Text(
            text = stringResource(R.string.onboarding_step1_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.onboarding_step1_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.medium))

        // Grupo conectado idéntico al de la sección de Apariencia en Ajustes
        Column(
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            ColorPaletteSettingCard(
                colorPalette = appSettings.colorPalette,
                onColorPaletteChanged = onColorPaletteChanged,
                shape = topConnectedShape
            )

            ThemeModeSettingCard(
                themeMode = appSettings.themeMode,
                onThemeModeChanged = onThemeModeChanged,
                shape = bottomConnectedShape
            )
        }
    }
}

/**
 * Paso 2 del recorrido: seleccion de la liga principal o competicion predeterminada.
 */
@Composable
private fun OnboardingStepLeague(
    selectedLeague: League,
    onLeagueSelected: (League) -> Unit
) {
    val scrollState = rememberScrollState()
    val standardLeagues = remember {
        League.entries.filter { it != League.FAVORITES && it != League.MY_LEAGUES && it != League.DEBUG }
    }
    val isDark = UlamaTheme.isDark

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = UlamaTheme.spacing.small)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_trophy),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

        Text(
            text = stringResource(R.string.onboarding_step2_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.onboarding_step2_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.medium))

        // Cuadricula compacta de 3x3 construida con filas para evitar colision de gestos
        Column(
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            standardLeagues.chunked(3).forEach { rowLeagues ->
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowLeagues.forEach { league ->
                        LeagueCircleItem(
                            league = league,
                            isSelected = selectedLeague == league,
                            badgeCount = 0,
                            isDark = isDark,
                            onClick = { onLeagueSelected(league) },
                            modifier = Modifier.width(88.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

        Text(
            text = stringResource(R.string.onboarding_step2_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = UlamaTheme.spacing.small)
        )
    }
}

/**
 * Paso 3 del recorrido: explicacion de Live Updates, Now Bar y solicitud de permisos.
 */
@Composable
private fun OnboardingStepNotifications(
    leadMinutes: Int,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNotificationLeadMinutesChanged: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val topConnectedShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardRestRadius,
        topEnd = UlamaTheme.shapes.cardRestRadius,
        bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
    )
    val bottomConnectedShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomStart = UlamaTheme.shapes.cardRestRadius,
        bottomEnd = UlamaTheme.shapes.cardRestRadius
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = UlamaTheme.spacing.small)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_notifications_active),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

        Text(
            text = stringResource(R.string.onboarding_step3_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.onboarding_step3_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.medium))

        // Grupo conectado idéntico a Notificaciones en Ajustes
        Column(
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tarjeta superior: Permiso de notificaciones reutilizado directamente desde Ajustes
            NotificationPermissionSettingCard(
                hasPermission = hasNotificationPermission,
                onRequestPermission = onRequestPermission,
                shape = topConnectedShape
            )

            // Tarjeta inferior: Selector de anticipación reutilizado directamente desde Ajustes
            NotificationLeadSettingCard(
                leadMinutes = leadMinutes,
                onNotificationLeadMinutesChanged = onNotificationLeadMinutesChanged,
                shape = bottomConnectedShape
            )
        }
    }
}

/**
 * Elemento circular compacto utilizado en el recorrido inicial para elegir liga predeterminada.
 */
@Composable
private fun LeagueCircleItem(
    league: League,
    isSelected: Boolean,
    badgeCount: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circleSize: Dp = 57.dp,
    iconSize: Dp = 26.dp,
    logoSize: Dp = 34.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .size(circleSize)
                    .bounceClick(enabled = true, interactionSource = interactionSource)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(UlamaTheme.spacing.small)
                ) {
                    when (league) {
                        League.FAVORITES -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = stringResource(league.nameRes),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        League.MY_LEAGUES -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_trophy),
                                contentDescription = stringResource(league.nameRes),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        else -> {
                            AsyncImage(
                                model = league.getLogoUrl(isDark),
                                contentDescription = stringResource(
                                    R.string.cd_league_logo,
                                    stringResource(league.nameRes)
                                ),
                                modifier = Modifier.size(logoSize)
                            )
                        }
                    }
                }
            }

            if (badgeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

        val leagueName = stringResource(league.nameRes)
        Text(
            text = leagueName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
