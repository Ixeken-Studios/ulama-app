package com.ixeken.ulama.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.ixeken.ulama.data.update.GithubUpdateChecker
import com.ixeken.ulama.data.update.UpdateCheckResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.AppLanguage
import com.ixeken.ulama.core.model.AppSettings
import com.ixeken.ulama.core.model.ColorPalette
import com.ixeken.ulama.core.model.MotionStyle
import com.ixeken.ulama.core.model.ThemeMode
import com.ixeken.ulama.service.MatchAlarmScheduler
import com.ixeken.ulama.ui.components.animateListEntry
import com.ixeken.ulama.ui.components.bounceClick
import com.ixeken.ulama.ui.theme.AmberPrimary
import com.ixeken.ulama.ui.theme.AmberSecondaryContainer
import com.ixeken.ulama.ui.theme.BluePrimary
import com.ixeken.ulama.ui.theme.BlueSecondaryContainer
import com.ixeken.ulama.ui.theme.DarkGreen
import com.ixeken.ulama.ui.theme.PurplePrimary
import com.ixeken.ulama.ui.theme.PurpleSecondaryContainer
import com.ixeken.ulama.ui.theme.RadiusLarge
import com.ixeken.ulama.ui.theme.UlamaTheme
import com.ixeken.ulama.ui.theme.VanillaWhite

/**
 * Destinos de navegacion interna para la estructura jerarquica de ajustes.
 */
private enum class SettingsDestination {
    ROOT,
    APPEARANCE,
    LANGUAGE,
    NOTIFICATIONS,
    PERMISSIONS,
    ABOUT
}

/**
 * Pantalla principal de ajustes inspirada en Google Pixel y Material 3 Expressive.
 * Unifica el color de todos los contenedores a [MaterialTheme.colorScheme.surfaceContainer],
 * organiza las secciones en tarjetas agrupadas con esquinas de 28dp y divisores sutiles,
 * e incorpora un selector de muestras de color y conmutadores con iconos en el thumb.
 *
 * @param appSettings Estado inmutable de preferencias de usuario.
 * @param currentLanguage Idioma activo a nivel de aplicacion.
 * @param onThemeModeChanged Callback al modificar el modo de tema oscuro.
 * @param onColorPaletteChanged Callback al alternar entre Dynamic Color y tema Ulama.
 * @param onMotionStyleChanged Callback al conmutar entre animaciones Expressive y Standard.
 * @param onAppLanguageChanged Callback al conmutar el idioma de la aplicacion.
 * @param onNotificationLeadMinutesChanged Callback al modificar la anticipacion de avisos de inicio.
 * @param onBackClick Accion para retornar a la pantalla principal.
 * @param modifier Modificador base de Compose.
 */
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    currentLanguage: AppLanguage = AppLanguage.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onColorPaletteChanged: (ColorPalette) -> Unit,
    onMotionStyleChanged: (MotionStyle) -> Unit,
    onAppLanguageChanged: (AppLanguage) -> Unit = {},
    onNotificationLeadMinutesChanged: (Int) -> Unit = {},
    onAutoFollowFavoritesChanged: (Boolean) -> Unit = {},
    onCheckUpdatesOnStartChanged: (Boolean) -> Unit = {},
    onShowWalkthrough: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.ROOT) }

    BackHandler(enabled = destination != SettingsDestination.ROOT) {
        destination = SettingsDestination.ROOT
    }

    val motion = UlamaTheme.motion

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                val isForward = targetState != SettingsDestination.ROOT

                if (isForward) {
                    (slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = motion.spatialSpec()
                    ) + fadeIn(animationSpec = motion.effectsSpec()))
                        .togetherWith(
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                targetOffset = { fullWidth -> fullWidth / 3 },
                                animationSpec = motion.spatialSpec()
                            ) + fadeOut(animationSpec = motion.effectsSpec())
                        )
                } else {
                    (slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        initialOffset = { fullWidth -> -fullWidth / 3 },
                        animationSpec = motion.spatialSpec()
                    ) + fadeIn(animationSpec = motion.effectsSpec()))
                        .togetherWith(
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = motion.spatialSpec()
                            ) + fadeOut(animationSpec = motion.effectsSpec())
                        )
                }
            },
            label = "SettingsDestinationTransition",
            modifier = Modifier.fillMaxSize()
        ) { currentDestination ->
            when (currentDestination) {
                SettingsDestination.ROOT -> {
                    SettingsRootContent(
                        appSettings = appSettings,
                        currentLanguage = currentLanguage,
                        onNavigateToAppearance = { destination = SettingsDestination.APPEARANCE },
                        onNavigateToLanguage = { destination = SettingsDestination.LANGUAGE },
                        onNavigateToNotifications = { destination = SettingsDestination.NOTIFICATIONS },
                        onNavigateToPermissions = { destination = SettingsDestination.PERMISSIONS },
                        onNavigateToAbout = { destination = SettingsDestination.ABOUT },
                        onBackClick = onBackClick
                    )
                }
                SettingsDestination.APPEARANCE -> {
                    AppearanceSettingsContent(
                        appSettings = appSettings,
                        onThemeModeChanged = onThemeModeChanged,
                        onColorPaletteChanged = onColorPaletteChanged,
                        onMotionStyleChanged = onMotionStyleChanged,
                        onBackClick = { destination = SettingsDestination.ROOT }
                    )
                }
                SettingsDestination.LANGUAGE -> {
                    LanguageSettingsContent(
                        currentLanguage = currentLanguage,
                        onAppLanguageChanged = onAppLanguageChanged,
                        onBackClick = { destination = SettingsDestination.ROOT }
                    )
                }
                SettingsDestination.NOTIFICATIONS -> {
                    NotificationsSettingsContent(
                        appSettings = appSettings,
                        onNotificationLeadMinutesChanged = onNotificationLeadMinutesChanged,
                        onAutoFollowFavoritesChanged = onAutoFollowFavoritesChanged,
                        onNavigateToPermissions = { destination = SettingsDestination.PERMISSIONS },
                        onBackClick = { destination = SettingsDestination.ROOT }
                    )
                }
                SettingsDestination.PERMISSIONS -> {
                    PermissionsSettingsContent(
                        onBackClick = { destination = SettingsDestination.ROOT }
                    )
                }
                SettingsDestination.ABOUT -> {
                    AboutSettingsContent(
                        appSettings = appSettings,
                        onCheckUpdatesOnStartChanged = onCheckUpdatesOnStartChanged,
                        onShowWalkthrough = onShowWalkthrough,
                        onBackClick = { destination = SettingsDestination.ROOT }
                    )
                }
            }
        }
    }
}

/**
 * Vista raiz de ajustes organizada en un contenedor agrupado para preferencias del sistema
 * y una tarjeta independiente para la informacion de la aplicacion y su version.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsRootContent(
    appSettings: AppSettings,
    currentLanguage: AppLanguage,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showPrivacySheet by remember { mutableStateOf(false) }

    if (showPrivacySheet) {
        PrivacyBottomSheet(onDismissRequest = { showPrivacySheet = false })
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = UlamaTheme.spacing.small)
                            .size(UlamaTheme.dimensions.minTouchTarget)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.size(UlamaTheme.dimensions.minTouchTarget))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UlamaTheme.spacing.screenHorizontalCompact)
                .padding(top = UlamaTheme.spacing.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                val appearanceSummary = buildAppearanceSummary(appSettings)
                val languageSummary = buildLanguageSummary(currentLanguage)
                val notificationsSummary = buildNotificationsSummary(appSettings)

                val topShape = RoundedCornerShape(
                    topStart = UlamaTheme.shapes.cardRestRadius,
                    topEnd = UlamaTheme.shapes.cardRestRadius,
                    bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
                )
                val middleShape = RoundedCornerShape(
                    topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
                )
                val bottomShape = RoundedCornerShape(
                    topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomStart = UlamaTheme.shapes.cardRestRadius,
                    bottomEnd = UlamaTheme.shapes.cardRestRadius
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = topShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateListEntry(index = 0, key = "settings_root_appearance")
                    ) {
                        SettingsCategoryRow(
                            title = stringResource(R.string.settings_section_appearance),
                            description = appearanceSummary,
                            icon = painterResource(R.drawable.ic_palette),
                            iconContentDescription = stringResource(R.string.cd_settings_palette),
                            onClick = onNavigateToAppearance,
                            shape = topShape
                        )
                    }

                    Surface(
                        shape = middleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateListEntry(index = 1, key = "settings_root_language")
                    ) {
                        SettingsCategoryRow(
                            title = stringResource(R.string.settings_section_language),
                            description = languageSummary,
                            icon = painterResource(R.drawable.ic_language),
                            iconContentDescription = stringResource(R.string.cd_settings_language),
                            onClick = onNavigateToLanguage,
                            shape = middleShape
                        )
                    }

                    Surface(
                        shape = middleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateListEntry(index = 2, key = "settings_root_notifications")
                    ) {
                        SettingsCategoryRow(
                            title = stringResource(R.string.settings_section_notifications),
                            description = notificationsSummary,
                            icon = painterResource(R.drawable.ic_notifications),
                            iconContentDescription = stringResource(R.string.cd_settings_notifications),
                            onClick = onNavigateToNotifications,
                            shape = middleShape
                        )
                    }

                    Surface(
                        shape = middleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateListEntry(index = 3, key = "settings_root_permissions")
                    ) {
                        SettingsCategoryRow(
                            title = stringResource(R.string.settings_section_permissions),
                            description = stringResource(R.string.settings_section_permissions_desc),
                            icon = painterResource(R.drawable.ic_settings),
                            iconContentDescription = stringResource(R.string.cd_settings_permissions),
                            onClick = onNavigateToPermissions,
                            shape = middleShape
                        )
                    }

                    Surface(
                        shape = bottomShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateListEntry(index = 4, key = "settings_root_privacy")
                    ) {
                        SettingsCategoryRow(
                            title = stringResource(R.string.settings_section_privacy),
                            description = stringResource(R.string.settings_section_privacy_desc),
                            icon = painterResource(R.drawable.ic_security),
                            iconContentDescription = stringResource(R.string.cd_settings_privacy),
                            onClick = { showPrivacySheet = true },
                            shape = bottomShape
                        )
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.medium))

                val standaloneShape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius)

                Surface(
                    shape = standaloneShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateListEntry(index = 5, key = "settings_root_about")
                ) {
                    SettingsCategoryRow(
                        title = stringResource(R.string.settings_section_about),
                        description = stringResource(R.string.settings_section_about_desc),
                        icon = painterResource(R.drawable.ic_info),
                        iconContentDescription = stringResource(R.string.cd_settings_info),
                        onClick = onNavigateToAbout,
                        shape = standaloneShape
                    )
                }
            }
        }
    }
}

/**
 * Fila interactiva de categoria de ajustes con distintivo circular pastel y ondulacion tactil integrada.
 */
@Composable
private fun SettingsCategoryRow(
    title: String,
    description: String,
    icon: Painter,
    iconContentDescription: String,
    badgeColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    shape: Shape = UlamaTheme.shapes.card
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .bounceClick(enabled = true, interactionSource = interactionSource)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
    } else {
        modifier.fillMaxWidth()
    }

    Box(modifier = clickableModifier) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = iconContentDescription,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

/**
 * Subpantalla de Apariencia con diseno Pixel: tarjetas homogeneas con el mismo color de contenedor,
 * selector de muestras de paleta y conmutadores con iconos en el thumb.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppearanceSettingsContent(
    appSettings: AppSettings,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onColorPaletteChanged: (ColorPalette) -> Unit,
    onMotionStyleChanged: (MotionStyle) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_section_appearance),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = UlamaTheme.spacing.small)
                            .size(UlamaTheme.dimensions.minTouchTarget)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.size(UlamaTheme.dimensions.minTouchTarget))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UlamaTheme.spacing.screenHorizontalCompact)
                .padding(top = UlamaTheme.spacing.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                // Etiqueta de seccion: Colores y tema
                Text(
                    text = stringResource(R.string.settings_section_colors_style),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = UlamaTheme.spacing.medium,
                        bottom = UlamaTheme.spacing.small
                    )
                )

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

                // Grupo conectado: Paleta de colores y modo de tema
                // Grupo conectado de paleta y modo de tema
                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Elemento superior: Muestras de paleta de colores
                    ColorPaletteSettingCard(
                        colorPalette = appSettings.colorPalette,
                        onColorPaletteChanged = onColorPaletteChanged,
                        shape = topConnectedShape,
                        modifier = Modifier.animateListEntry(index = 0, key = "appearance_palette")
                    )

                    // Elemento inferior: Modo de tema con Connected Button Group
                    ThemeModeSettingCard(
                        themeMode = appSettings.themeMode,
                        onThemeModeChanged = onThemeModeChanged,
                        shape = bottomConnectedShape,
                        modifier = Modifier.animateListEntry(index = 1, key = "appearance_theme_mode")
                    )
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.large))

                // Etiqueta de seccion: Movimiento del sistema
                Text(
                    text = stringResource(R.string.settings_section_motion_system),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = UlamaTheme.spacing.medium,
                        bottom = UlamaTheme.spacing.small
                    )
                )

                // Tarjeta 2: Animaciones del sistema con el mismo color uniforme
                Surface(
                    shape = UlamaTheme.shapes.card,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = UlamaTheme.borders.cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateListEntry(index = 2, key = "appearance_motion")
                ) {
                    val isExpressive = appSettings.motionStyle == MotionStyle.EXPRESSIVE
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_motion_toggle_title),
                        description = if (isExpressive) {
                            stringResource(R.string.settings_motion_expressive_desc)
                        } else {
                            stringResource(R.string.settings_motion_standard_desc)
                        },
                        checked = isExpressive,
                        onCheckedChange = { isChecked ->
                            onMotionStyleChanged(
                                if (isChecked) MotionStyle.EXPRESSIVE else MotionStyle.STANDARD
                            )
                        },
                        icon = painterResource(R.drawable.ic_animation),
                        iconContentDescription = stringResource(R.string.cd_settings_animation),
                        shape = UlamaTheme.shapes.card
                    )
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.large))
            }
        }
    }
}

/**
 * Muestra circular de paleta de colores con halo de seleccion al estilo Pixel Colors.
 */
@Composable
internal fun ColorPaletteSwatch(
    palette: ColorPalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = UlamaTheme.isDark
    val dynamicColors = remember(context, isDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            null
        }
    }
    val dynamicPrimary = dynamicColors?.primary ?: Color(0xFF006C4C)
    val dynamicSecondary = dynamicColors?.secondaryContainer ?: Color(0xFF89F8C7)

    val outlineColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val outlineBorderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(UlamaTheme.spacing.small)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(2.dp, outlineColor, RoundedCornerShape(18.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(44.dp)) {
                when (palette) {
                    ColorPalette.DYNAMIC -> {
                        // Circulo dividido bicromatico con colores autenticos del wallpaper
                        drawArc(
                            color = dynamicPrimary,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawArc(
                            color = dynamicSecondary,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawCircle(
                            color = outlineBorderColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    ColorPalette.ULAMA -> {
                        // Circulo oficial de Ulama: Verde Mesoamericano y centro Blanco Vainilla
                        drawCircle(color = DarkGreen)
                        drawCircle(
                            color = VanillaWhite,
                            radius = size.minDimension / 4f
                        )
                        drawCircle(
                            color = outlineBorderColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    ColorPalette.MONOCHROME -> {
                        // Circulo bicromatico blanco y negro puro estilo Pixel Monochrome
                        drawArc(
                            color = Color.Black,
                            startAngle = 90f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawArc(
                            color = Color.White,
                            startAngle = 270f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawCircle(
                            color = outlineBorderColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    ColorPalette.PURPLE -> {
                        // Circulo bicromatico violeta y lavanda suave estilo MD3 Expressive Purple
                        drawArc(
                            color = PurplePrimary,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawArc(
                            color = PurpleSecondaryContainer,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawCircle(
                            color = outlineBorderColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    ColorPalette.BLUE -> {
                        // Circulo bicromatico azul zafiro y celeste suave estilo MD3 Expressive Blue
                        drawArc(
                            color = BluePrimary,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawArc(
                            color = BlueSecondaryContainer,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawCircle(
                            color = outlineBorderColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    ColorPalette.AMBER -> {
                        // Circulo bicromatico ambar miel y oro suave estilo MD3 Expressive Amber
                        drawArc(
                            color = AmberPrimary,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawArc(
                            color = AmberSecondaryContainer,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = true
                        )
                        drawCircle(
                            color = outlineBorderColor,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

        Text(
            text = when (palette) {
                ColorPalette.DYNAMIC -> stringResource(R.string.settings_color_palette_dynamic)
                ColorPalette.ULAMA -> stringResource(R.string.settings_color_palette_ulama)
                ColorPalette.MONOCHROME -> stringResource(R.string.settings_color_palette_monochrome)
                ColorPalette.PURPLE -> stringResource(R.string.settings_color_palette_purple)
                ColorPalette.BLUE -> stringResource(R.string.settings_color_palette_blue)
                ColorPalette.AMBER -> stringResource(R.string.settings_color_palette_amber)
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Tarjeta interactiva de seleccion de paleta de colores para Ajustes y Bienvenida.
 * Encapsula la cabecera uniforme con distintivo circular y las muestras circulares de paleta.
 */
@Composable
internal fun ColorPaletteSettingCard(
    colorPalette: ColorPalette,
    onColorPaletteChanged: (ColorPalette) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = UlamaTheme.spacing.medium,
                        vertical = UlamaTheme.spacing.medium
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_colors),
                        contentDescription = stringResource(R.string.cd_settings_colors),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_color_palette_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (colorPalette) {
                            ColorPalette.DYNAMIC -> stringResource(R.string.settings_color_palette_dynamic_desc)
                            ColorPalette.ULAMA -> stringResource(R.string.settings_color_palette_ulama_desc)
                            ColorPalette.MONOCHROME -> stringResource(R.string.settings_color_palette_monochrome_desc)
                            ColorPalette.PURPLE -> stringResource(R.string.settings_color_palette_purple_desc)
                            ColorPalette.BLUE -> stringResource(R.string.settings_color_palette_blue_desc)
                            ColorPalette.AMBER -> stringResource(R.string.settings_color_palette_amber_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UlamaTheme.spacing.medium)
                    .padding(bottom = UlamaTheme.spacing.medium)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        UlamaTheme.spacing.small,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ColorPaletteSwatch(
                        palette = ColorPalette.DYNAMIC,
                        isSelected = colorPalette == ColorPalette.DYNAMIC,
                        onClick = { onColorPaletteChanged(ColorPalette.DYNAMIC) },
                        modifier = Modifier.weight(1f)
                    )
                    ColorPaletteSwatch(
                        palette = ColorPalette.ULAMA,
                        isSelected = colorPalette == ColorPalette.ULAMA,
                        onClick = { onColorPaletteChanged(ColorPalette.ULAMA) },
                        modifier = Modifier.weight(1f)
                    )
                    ColorPaletteSwatch(
                        palette = ColorPalette.MONOCHROME,
                        isSelected = colorPalette == ColorPalette.MONOCHROME,
                        onClick = { onColorPaletteChanged(ColorPalette.MONOCHROME) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        UlamaTheme.spacing.small,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ColorPaletteSwatch(
                        palette = ColorPalette.PURPLE,
                        isSelected = colorPalette == ColorPalette.PURPLE,
                        onClick = { onColorPaletteChanged(ColorPalette.PURPLE) },
                        modifier = Modifier.weight(1f)
                    )
                    ColorPaletteSwatch(
                        palette = ColorPalette.BLUE,
                        isSelected = colorPalette == ColorPalette.BLUE,
                        onClick = { onColorPaletteChanged(ColorPalette.BLUE) },
                        modifier = Modifier.weight(1f)
                    )
                    ColorPaletteSwatch(
                        palette = ColorPalette.AMBER,
                        isSelected = colorPalette == ColorPalette.AMBER,
                        onClick = { onColorPaletteChanged(ColorPalette.AMBER) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de seleccion de modo de tema claro, oscuro o del sistema.
 * Utiliza botonera conectada de Material 3 Expressive y distintivo circular uniforme.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ThemeModeSettingCard(
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UlamaTheme.spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UlamaTheme.spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dark_mode),
                        contentDescription = stringResource(R.string.cd_theme_mode_icon),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

                Text(
                    text = stringResource(R.string.settings_theme_mode_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val themeModes = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_mode_system),
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_mode_light),
                    ThemeMode.DARK to stringResource(R.string.settings_theme_mode_dark)
                )

                themeModes.forEachIndexed { index, (mode, label) ->
                    val isSelected = themeMode == mode
                    val shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        themeModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }

                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onThemeModeChanged(mode) },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de seleccion de anticipacion para notificaciones de inicio de partidos.
 * Construida con botonera conectada de Material 3 Expressive y distintivo circular uniforme.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotificationLeadSettingCard(
    leadMinutes: Int,
    onNotificationLeadMinutesChanged: (Int) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = UlamaTheme.spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UlamaTheme.spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notifications_active),
                        contentDescription = stringResource(R.string.cd_settings_notifications_lead),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_notifications_lead_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_notifications_lead_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val leadOptions = listOf(0, 5, 10)
            val selectedOption = if (leadMinutes in leadOptions) {
                leadMinutes
            } else {
                5
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val options = listOf(
                    0 to stringResource(R.string.settings_notifications_lead_0),
                    5 to stringResource(R.string.settings_notifications_lead_5),
                    10 to stringResource(R.string.settings_notifications_lead_10)
                )

                options.forEachIndexed { index, (minutes, label) ->
                    val isSelected = selectedOption == minutes
                    val shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }

                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onNotificationLeadMinutesChanged(minutes) },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de configuracion y estado del permiso de notificaciones del sistema.
 * Disenada para su reutilizacion en el walkthrough de bienvenida y la pantalla de ajustes de permisos.
 *
 * @param hasPermission Indica si el permiso POST_NOTIFICATIONS se encuentra concedido.
 * @param onRequestPermission Callback para invocar el launcher de permisos o ajustes del sistema.
 * @param shape Forma geometrica exterior adaptada a esquinas independientes o listas conectadas.
 * @param modifier Modificador base de Compose.
 */
@Composable
internal fun NotificationPermissionSettingCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    PermissionStatusCard(
        title = stringResource(R.string.settings_permissions_notif_title),
        description = stringResource(R.string.settings_permissions_notif_desc),
        icon = painterResource(R.drawable.ic_notifications),
        isGranted = hasPermission,
        grantedLabel = stringResource(R.string.settings_permissions_notif_granted),
        actionLabel = stringResource(R.string.settings_permissions_notif_action),
        onRequest = onRequestPermission,
        shape = shape,
        modifier = modifier
    )
}

/**
 * Tarjeta interactiva para gestionar y solicitar el permiso de alarmas exactas del sistema.
 * Garantiza que el despertador de partidos arranque puntual sin retrasos de Doze.
 *
 * @param hasPermission Indica si el permiso de alarmas exactas esta concedido.
 * @param onRequestPermission Callback para abrir los ajustes de alarmas y recordatorios del sistema.
 * @param shape Forma geometrica exterior adaptada a listas conectadas.
 * @param modifier Modificador base de Compose.
 */
@Composable
internal fun ExactAlarmPermissionSettingCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    PermissionStatusCard(
        title = stringResource(R.string.settings_permissions_alarm_title),
        description = stringResource(R.string.settings_permissions_alarm_desc),
        icon = painterResource(R.drawable.ic_notifications_active),
        isGranted = hasPermission,
        grantedLabel = stringResource(R.string.settings_permissions_alarm_granted),
        actionLabel = stringResource(R.string.settings_permissions_alarm_action),
        onRequest = onRequestPermission,
        shape = shape,
        modifier = modifier
    )
}

/**
 * Tarjeta interactiva para gestionar y solicitar la excepcion de optimizaciones de bateria.
 * Permite que los servicios en segundo plano de Ulama funcionen sin restricciones del sistema.
 *
 * @param isIgnoringBatteryOptimizations Indica si la app ya esta exenta de restricciones de bateria.
 * @param onRequestIgnoreBattery Callback para abrir el dialogo del sistema o ajustes de bateria.
 * @param shape Forma geometrica exterior adaptada a esquinas independientes o listas conectadas.
 * @param modifier Modificador base de Compose.
 */
@Composable
internal fun BatteryOptimizationSettingCard(
    isIgnoringBatteryOptimizations: Boolean,
    onRequestIgnoreBattery: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    PermissionStatusCard(
        title = stringResource(R.string.settings_permissions_battery_title),
        description = stringResource(R.string.settings_permissions_battery_desc),
        icon = painterResource(R.drawable.ic_system_update),
        isGranted = isIgnoringBatteryOptimizations,
        grantedLabel = stringResource(R.string.settings_permissions_battery_unrestricted),
        actionLabel = stringResource(R.string.settings_permissions_battery_action),
        onRequest = onRequestIgnoreBattery,
        shape = shape,
        modifier = modifier
    )
}

/**
 * Contenedor visual generico para elementos de estado de permisos y capacidades del sistema.
 * Presenta el estado concedido con distintivo de confirmacion o un boton de accion directa.
 */
@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    icon: Painter,
    isGranted: Boolean,
    grantedLabel: String,
    actionLabel: String,
    onRequest: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UlamaTheme.spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UlamaTheme.spacing.small)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isGranted) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = UlamaTheme.spacing.small)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))
                        Text(
                            text = grantedLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRequest,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Fila interactiva con interruptor de Material 3 Expressive que incluye iconos de verificacion y cierre en el thumb.
 */
@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    iconContentDescription: String? = null,
    shape: androidx.compose.ui.graphics.Shape? = null
) {
    val clickModifier = if (shape != null) {
        modifier.fillMaxWidth().clip(shape)
    } else {
        modifier.fillMaxWidth()
    }

    Box(
        modifier = clickModifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = if (icon != null) {
                {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = icon,
                            contentDescription = iconContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else null,
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = {
                        if (checked) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

/**
 * Genera el resumen dinamico de la configuracion de apariencia activa.
 */
@Composable
@NonRestartableComposable
private fun buildAppearanceSummary(settings: AppSettings): String {
    val themeLabel = when (settings.themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_mode_system)
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_mode_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_mode_dark)
    }
    val paletteLabel = when (settings.colorPalette) {
        ColorPalette.DYNAMIC -> stringResource(R.string.settings_color_palette_dynamic)
        ColorPalette.ULAMA -> stringResource(R.string.settings_color_palette_ulama)
        ColorPalette.MONOCHROME -> stringResource(R.string.settings_color_palette_monochrome)
        ColorPalette.PURPLE -> stringResource(R.string.settings_color_palette_purple)
        ColorPalette.BLUE -> stringResource(R.string.settings_color_palette_blue)
        ColorPalette.AMBER -> stringResource(R.string.settings_color_palette_amber)
    }
    val motionLabel = when (settings.motionStyle) {
        MotionStyle.EXPRESSIVE -> stringResource(R.string.settings_motion_expressive)
        MotionStyle.STANDARD -> stringResource(R.string.settings_motion_standard)
    }
    return "$themeLabel · $paletteLabel · $motionLabel"
}

/**
 * Genera el resumen contextual del idioma de la aplicacion actualmente seleccionado.
 * Utiliza when para mapear de manera estricta cada valor del enum a su cadena correspondiente.
 */
@Composable
@NonRestartableComposable
private fun buildLanguageSummary(language: AppLanguage): String {
    return stringResource(language.localizedNameRes)
}

/**
 * Genera el resumen dinamico de la configuracion de notificaciones activa.
 */
@Composable
@NonRestartableComposable
private fun buildNotificationsSummary(settings: AppSettings): String {
    return when (settings.notificationLeadMinutes) {
        0 -> stringResource(R.string.settings_notifications_lead_summary_0)
        10 -> stringResource(R.string.settings_notifications_lead_summary_10)
        else -> stringResource(R.string.settings_notifications_lead_summary_5)
    }
}

/**
 * Subpantalla de seleccion de idioma inspirada en Google Pixel y Material 3 Expressive.
 * Permite conmutar la aplicacion entre la configuracion regional del sistema, espanol e ingles,
 * aplicando el cambio inmediatamente mediante LocaleManager en Android 13+.
 *
 * @param currentLanguage Idioma actualmente configurado en la app.
 * @param onAppLanguageChanged Callback invocado al seleccionar un idioma diferente.
 * @param onBackClick Accion para retornar a la vista raiz de ajustes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguageSettingsContent(
    currentLanguage: AppLanguage,
    onAppLanguageChanged: (AppLanguage) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_section_language),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = UlamaTheme.spacing.small)
                            .size(UlamaTheme.dimensions.minTouchTarget)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.size(UlamaTheme.dimensions.minTouchTarget))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UlamaTheme.spacing.screenHorizontalCompact)
                .padding(top = UlamaTheme.spacing.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                Text(
                    text = stringResource(R.string.settings_language_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = UlamaTheme.spacing.medium,
                        bottom = UlamaTheme.spacing.small
                    )
                )

                val topShape = RoundedCornerShape(
                    topStart = UlamaTheme.shapes.cardRestRadius,
                    topEnd = UlamaTheme.shapes.cardRestRadius,
                    bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
                )
                val middleShape = RoundedCornerShape(UlamaTheme.shapes.cardConnectedInnerRadius)
                val bottomShape = RoundedCornerShape(
                    topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomStart = UlamaTheme.shapes.cardRestRadius,
                    bottomEnd = UlamaTheme.shapes.cardRestRadius
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val languages = AppLanguage.entries
                    languages.forEachIndexed { index, language ->
                        val shape = when (index) {
                            0 -> topShape
                            languages.lastIndex -> bottomShape
                            else -> middleShape
                        }
                        val title = language.nativeName ?: stringResource(language.localizedNameRes)
                        val description = if (language.nativeName != null) {
                            stringResource(language.localizedNameRes)
                        } else {
                            stringResource(language.descriptionRes ?: language.localizedNameRes)
                        }

                        LanguageOptionCard(
                            title = title,
                            description = description,
                            isSelected = currentLanguage == language,
                            onClick = { onAppLanguageChanged(language) },
                            shape = shape,
                            modifier = Modifier.animateListEntry(index = index, key = language.name)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de seleccion de idioma para la lista conectada.
 * Aplica la regla Clip Before Click para asegurar que la respuesta tactil coincida con las esquinas redondeadas.
 *
 * @param title Titulo descriptivo de la opcion de idioma.
 * @param description Subtitulo explicativo de la procedencia del idioma.
 * @param isSelected Estado de seleccion de esta opcion.
 * @param onClick Accion al pulsar sobre la tarjeta.
 * @param shape Geometria de la tarjeta adaptada a su posicion en la lista conectada.
 * @param modifier Modificador base de Compose.
 */
@Composable
private fun LanguageOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick
                )
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = if (isSelected) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else null,
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * Subpantalla de Notificaciones con diseno Pixel: tarjeta homogenea con selector
 * de 3 opciones (0 min, 5 min y 10 min) utilizando UlamaSegmentedTabs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotificationsSettingsContent(
    appSettings: AppSettings,
    onNotificationLeadMinutesChanged: (Int) -> Unit,
    onAutoFollowFavoritesChanged: (Boolean) -> Unit,
    onNavigateToPermissions: () -> Unit,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    var hasAllPermissions by remember {
        mutableStateOf(
            checkNotificationPermission(context) &&
                MatchAlarmScheduler.canScheduleExactAlarms(context) &&
                checkBatteryOptimization(context)
        )
    }

    LifecycleResumeEffect(Unit) {
        hasAllPermissions = checkNotificationPermission(context) &&
            MatchAlarmScheduler.canScheduleExactAlarms(context) &&
            checkBatteryOptimization(context)
        onPauseOrDispose { }
    }

    val showPermissionsWarning = appSettings.autoFollowFavorites && !hasAllPermissions
    val restRadius = UlamaTheme.shapes.cardRestRadius
    val innerRadius = UlamaTheme.shapes.cardConnectedInnerRadius
    val topConnectedShape = remember(restRadius, innerRadius) {
        RoundedCornerShape(
            topStart = restRadius,
            topEnd = restRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
    }
    val bottomConnectedShape = remember(restRadius, innerRadius) {
        RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = restRadius,
            bottomEnd = restRadius
        )
    }
    val autoFollowCardShape = if (showPermissionsWarning) topConnectedShape else UlamaTheme.shapes.card

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_section_notifications),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = UlamaTheme.spacing.small)
                            .size(UlamaTheme.dimensions.minTouchTarget)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.size(UlamaTheme.dimensions.minTouchTarget))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UlamaTheme.spacing.screenHorizontalCompact)
                .padding(top = UlamaTheme.spacing.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                Text(
                    text = stringResource(R.string.settings_notifications_section_lead),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = UlamaTheme.spacing.medium,
                        bottom = UlamaTheme.spacing.small
                    )
                )

                // Elemento superior: Selector de anticipacion de inicio con Connected Button Group
                NotificationLeadSettingCard(
                    leadMinutes = appSettings.notificationLeadMinutes,
                    onNotificationLeadMinutesChanged = onNotificationLeadMinutesChanged,
                    shape = UlamaTheme.shapes.card,
                    modifier = Modifier.animateListEntry(index = 0, key = "notifications_lead")
                )

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.large))

                Text(
                    text = stringResource(R.string.settings_notifications_section_favorites),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = UlamaTheme.spacing.medium,
                        bottom = UlamaTheme.spacing.small
                    )
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = autoFollowCardShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = UlamaTheme.borders.cardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateListEntry(index = 1, key = "notifications_auto_follow")
                    ) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_notifications_auto_follow_favorites_title),
                            description = stringResource(R.string.settings_notifications_auto_follow_favorites_desc),
                            checked = appSettings.autoFollowFavorites,
                            onCheckedChange = onAutoFollowFavoritesChanged,
                            icon = painterResource(R.drawable.ic_star),
                            iconContentDescription = stringResource(R.string.cd_fav_teams_star),
                            shape = autoFollowCardShape
                        )
                    }

                    if (showPermissionsWarning) {
                        val warningInteractionSource = remember { MutableInteractionSource() }
                        Surface(
                            shape = bottomConnectedShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateListEntry(index = 2, key = "notifications_permissions_warning")
                                .clip(bottomConnectedShape)
                                .bounceClick(enabled = true, interactionSource = warningInteractionSource)
                                .clickable(
                                    interactionSource = warningInteractionSource,
                                    indication = ripple(),
                                    onClick = onNavigateToPermissions
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(UlamaTheme.spacing.medium)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_security),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_notifications_permissions_warning_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.settings_notifications_permissions_warning_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))

                                Icon(
                                    painter = painterResource(R.drawable.ic_chevron_right),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.large))
            }
        }
    }
}

/**
 * Subpantalla de Permisos del sistema inspirada en Material 3 Expressive.
 * Centraliza la verificacion y solicitud del permiso de notificaciones y la
 * excepcion de optimizaciones de bateria para garantizar el funcionamiento
 * en segundo plano de las alertas y la Now Bar.
 *
 * @param onBackClick Accion para retornar a la vista raiz de ajustes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionsSettingsContent(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(MatchAlarmScheduler.canScheduleExactAlarms(context))
    }
    var isIgnoringBattery by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LifecycleResumeEffect(Unit) {
        hasNotificationPermission = checkNotificationPermission(context)
        hasExactAlarmPermission = MatchAlarmScheduler.canScheduleExactAlarms(context)
        isIgnoringBattery = checkBatteryOptimization(context)
        onPauseOrDispose { }
    }

    val restRadius = UlamaTheme.shapes.cardRestRadius
    val innerRadius = UlamaTheme.shapes.cardConnectedInnerRadius
    val topConnectedShape = remember(restRadius, innerRadius) {
        RoundedCornerShape(
            topStart = restRadius,
            topEnd = restRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
    }
    val middleConnectedShape = remember(innerRadius) {
        RoundedCornerShape(innerRadius)
    }
    val bottomConnectedShape = remember(restRadius, innerRadius) {
        RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = restRadius,
            bottomEnd = restRadius
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_section_permissions),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = UlamaTheme.spacing.small)
                            .size(UlamaTheme.dimensions.minTouchTarget)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.size(UlamaTheme.dimensions.minTouchTarget))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UlamaTheme.spacing.screenHorizontalCompact)
                .padding(top = UlamaTheme.spacing.medium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                Text(
                    text = stringResource(R.string.settings_section_permissions),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = UlamaTheme.spacing.medium,
                        bottom = UlamaTheme.spacing.small
                    )
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NotificationPermissionSettingCard(
                        hasPermission = hasNotificationPermission,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openAppNotificationSettings(context)
                            }
                        },
                        shape = topConnectedShape,
                        modifier = Modifier.animateListEntry(index = 0, key = "permissions_notif")
                    )

                    ExactAlarmPermissionSettingCard(
                        hasPermission = hasExactAlarmPermission,
                        onRequestPermission = {
                            requestExactAlarmPermission(context)
                        },
                        shape = middleConnectedShape,
                        modifier = Modifier.animateListEntry(index = 1, key = "permissions_exact_alarm")
                    )

                    BatteryOptimizationSettingCard(
                        isIgnoringBatteryOptimizations = isIgnoringBattery,
                        onRequestIgnoreBattery = {
                            requestIgnoreBatteryOptimizations(context)
                        },
                        shape = bottomConnectedShape,
                        modifier = Modifier.animateListEntry(index = 2, key = "permissions_battery")
                    )
                }
            }
        }
    }
}

/**
 * Determina si el permiso POST_NOTIFICATIONS se encuentra concedido en el sistema.
 */
private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/**
 * Determina si la aplicacion esta exenta de optimizaciones de bateria del sistema.
 */
private fun checkBatteryOptimization(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    } else {
        true
    }
}

/**
 * Redirige a los ajustes de notificaciones de la aplicacion en el sistema.
 */
private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(fallback)
    }
}

/**
 * Abre la pantalla del sistema para otorgar permiso de alarmas y recordatorios exactos.
 */
private fun requestExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(fallback)
        }
    }
}

/**
 * Solicita la exclusion de optimizaciones de bateria o abre la pantalla correspondiente del sistema.
 */
private fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
    }
}

/**
 * Subpantalla de Informacion de la aplicacion inspirada en Material 3 Expressive.
 * Despliega tarjetas independientes con esquinas redondeadas generosas para
 * presentar la identidad de Ulama, su version, la autoria de ixeken studios y la licencia MIT.
 *
 * @param onBackClick Accion para retornar a la vista principal de ajustes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutSettingsContent(
    appSettings: AppSettings,
    onCheckUpdatesOnStartChanged: (Boolean) -> Unit,
    onShowWalkthrough: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var activeSheet by remember { mutableStateOf(AboutSheetType.NONE) }
    val uriHandler = LocalUriHandler.current

    var showManualConfirmDialog by remember { mutableStateOf(false) }
    var showStartConfirmDialog by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    val updateChecker = remember(context) { GithubUpdateChecker(context) }

    when (activeSheet) {
        AboutSheetType.CHANGELOG -> ChangelogBottomSheet(onDismissRequest = { activeSheet = AboutSheetType.NONE })
        AboutSheetType.THIRD_PARTY -> ThirdPartyBottomSheet(onDismissRequest = { activeSheet = AboutSheetType.NONE })
        AboutSheetType.LICENSE -> LicenseBottomSheet(onDismissRequest = { activeSheet = AboutSheetType.NONE })
        AboutSheetType.NONE -> Unit
    }

    if (showManualConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showManualConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_update_confirm_manual_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_update_confirm_manual_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualConfirmDialog = false
                        isCheckingUpdates = true
                        coroutineScope.launch {
                            val result = updateChecker.checkForUpdates()
                            isCheckingUpdates = false
                            updateResult = result
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.dialog_update_action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualConfirmDialog = false }) {
                    Text(text = stringResource(R.string.dialog_update_action_cancel))
                }
            }
        )
    }

    if (showStartConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStartConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_update_confirm_on_start_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_update_confirm_on_start_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartConfirmDialog = false
                        onCheckUpdatesOnStartChanged(true)
                    }
                ) {
                    Text(text = stringResource(R.string.dialog_update_action_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartConfirmDialog = false }) {
                    Text(text = stringResource(R.string.dialog_update_action_cancel))
                }
            }
        )
    }

    if (isCheckingUpdates) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(UlamaTheme.spacing.medium)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))
                    Text(
                        text = stringResource(R.string.dialog_update_checking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    when (val result = updateResult) {
        is UpdateCheckResult.NoInternet -> {
            AlertDialog(
                onDismissRequest = { updateResult = null },
                title = {
                    Text(
                        text = stringResource(R.string.dialog_update_no_internet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.dialog_update_no_internet_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = { updateResult = null }) {
                        Text(text = stringResource(R.string.dialog_update_action_accept))
                    }
                }
            )
        }
        is UpdateCheckResult.RepoNotFoundOrPrivate -> {
            AlertDialog(
                onDismissRequest = { updateResult = null },
                title = {
                    Text(
                        text = stringResource(R.string.dialog_update_repo_unavailable_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.dialog_update_repo_unavailable_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = { updateResult = null }) {
                        Text(text = stringResource(R.string.dialog_update_action_accept))
                    }
                }
            )
        }
        is UpdateCheckResult.UpToDate -> {
            AlertDialog(
                onDismissRequest = { updateResult = null },
                title = {
                    Text(
                        text = stringResource(R.string.dialog_update_up_to_date_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.dialog_update_up_to_date_desc, result.version),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = { updateResult = null }) {
                        Text(text = stringResource(R.string.dialog_update_action_accept))
                    }
                }
            )
        }
        is UpdateCheckResult.NewVersionAvailable -> {
            AlertDialog(
                onDismissRequest = { updateResult = null },
                title = {
                    Text(
                        text = stringResource(R.string.dialog_update_new_version_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.dialog_update_new_version_desc, result.version),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(result.releaseUrl)
                            } catch (_: Exception) {
                            }
                            updateResult = null
                        }
                    ) {
                        Text(text = stringResource(R.string.dialog_update_new_version_action_download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { updateResult = null }) {
                        Text(text = stringResource(R.string.dialog_update_new_version_action_later))
                    }
                }
            )
        }
        is UpdateCheckResult.AheadOfRelease -> {
            AlertDialog(
                onDismissRequest = { updateResult = null },
                title = {
                    Text(
                        text = stringResource(R.string.dialog_update_ahead_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            R.string.dialog_update_ahead_desc,
                            result.installedVersion,
                            result.releaseVersion
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = { updateResult = null }) {
                        Text(text = stringResource(R.string.dialog_update_action_accept))
                    }
                }
            )
        }
        null -> Unit
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_section_about),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = UlamaTheme.spacing.small)
                            .size(UlamaTheme.dimensions.minTouchTarget)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.size(UlamaTheme.dimensions.minTouchTarget))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UlamaTheme.spacing.screenHorizontalCompact)
                .padding(top = UlamaTheme.spacing.medium, bottom = UlamaTheme.spacing.extraLarge)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            ) {
                val topConnectedShape = RoundedCornerShape(
                    topStart = UlamaTheme.shapes.cardRestRadius,
                    topEnd = UlamaTheme.shapes.cardRestRadius,
                    bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
                )
                val middleConnectedShape = RoundedCornerShape(
                    topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
                )
                val bottomConnectedShape = RoundedCornerShape(
                    topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
                    topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
                    bottomStart = UlamaTheme.shapes.cardRestRadius,
                    bottomEnd = UlamaTheme.shapes.cardRestRadius
                )

                // Tarjeta de marca con emblema mesoamericano y version
                AboutBrandCard(modifier = Modifier.animateListEntry(index = 0, key = "about_brand"))

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

                // Grupo conectado de notas de cambios y repositorio
                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Opcion para consultar el registro historico de cambios
                    AboutActionCard(
                        title = stringResource(R.string.settings_about_changelog_title),
                        icon = painterResource(R.drawable.ic_history),
                        iconContentDescription = stringResource(R.string.cd_settings_about_changelog),
                        onClick = { activeSheet = AboutSheetType.CHANGELOG },
                        shape = topConnectedShape,
                        modifier = Modifier.animateListEntry(index = 1, key = "about_changelog")
                    )

                    // Opcion para repetir la guia de bienvenida
                    AboutActionCard(
                        title = stringResource(R.string.settings_about_walkthrough_title),
                        icon = painterResource(R.drawable.ic_animation),
                        iconContentDescription = stringResource(R.string.cd_settings_about_walkthrough),
                        onClick = onShowWalkthrough,
                        shape = middleConnectedShape,
                        modifier = Modifier.animateListEntry(index = 2, key = "about_walkthrough")
                    )

                    // Tarjeta de autoria que enlaza al perfil de organizacion en GitHub con el logo de Ixeken
                    AboutActionCard(
                        title = stringResource(R.string.settings_about_creator_label) + " " + stringResource(R.string.settings_about_creator_name),
                        icon = painterResource(R.drawable.ic_ixeken_logo),
                        iconContentDescription = stringResource(R.string.cd_settings_about_creator),
                        onClick = {
                            try {
                                uriHandler.openUri("https://github.com/Ixeken-Studios")
                            } catch (_: Exception) {
                            }
                        },
                        shape = middleConnectedShape,
                        modifier = Modifier.animateListEntry(index = 3, key = "about_creator")
                    )

                    // Opcion para explorar el repositorio de codigo fuente
                    AboutActionCard(
                        title = stringResource(R.string.settings_about_repository_title),
                        icon = painterResource(R.drawable.ic_code),
                        iconContentDescription = stringResource(R.string.cd_settings_about_repository),
                        onClick = {
                            try {
                                uriHandler.openUri("https://github.com/Ixeken-Studios/ulama-app")
                            } catch (_: Exception) {
                            }
                        },
                        shape = middleConnectedShape,
                        modifier = Modifier.animateListEntry(index = 4, key = "about_repo")
                    )

                    // Tarjeta interactiva para consultar la licencia MIT
                    AboutActionCard(
                        title = stringResource(R.string.settings_about_license_name),
                        icon = painterResource(R.drawable.ic_description),
                        iconContentDescription = stringResource(R.string.cd_settings_about_license),
                        onClick = { activeSheet = AboutSheetType.LICENSE },
                        shape = middleConnectedShape,
                        modifier = Modifier.animateListEntry(index = 5, key = "about_license")
                    )

                    // Tarjeta interactiva para consultar licencias y fuentes de terceros
                    AboutActionCard(
                        title = stringResource(R.string.settings_about_third_party_title),
                        icon = painterResource(R.drawable.ic_info),
                        iconContentDescription = stringResource(R.string.cd_settings_about_third_party),
                        onClick = { activeSheet = AboutSheetType.THIRD_PARTY },
                        shape = bottomConnectedShape,
                        modifier = Modifier.animateListEntry(index = 6, key = "about_third_party")
                    )
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

                // Grupo conectado de opciones de actualizacion
                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Opcion para consultar actualizaciones disponibles
                    AboutActionCard(
                        title = stringResource(R.string.settings_about_check_updates_title),
                        icon = painterResource(R.drawable.ic_update),
                        iconContentDescription = stringResource(R.string.cd_settings_about_check_updates),
                        onClick = { showManualConfirmDialog = true },
                        shape = topConnectedShape,
                        modifier = Modifier.animateListEntry(index = 7, key = "about_check_updates")
                    )

                    // Conmutador para comprobar actualizaciones al iniciar la aplicacion
                    AboutSwitchCard(
                        title = stringResource(R.string.settings_about_check_on_start_title),
                        description = stringResource(R.string.settings_about_check_on_start_desc),
                        icon = painterResource(R.drawable.ic_system_update),
                        iconContentDescription = stringResource(R.string.cd_settings_about_check_on_start),
                        checked = appSettings.checkUpdatesOnStart,
                        onCheckedChange = { willEnable ->
                            if (willEnable) {
                                showStartConfirmDialog = true
                            } else {
                                onCheckUpdatesOnStartChanged(false)
                            }
                        },
                        shape = bottomConnectedShape,
                        modifier = Modifier.animateListEntry(index = 8, key = "about_check_on_start")
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de identidad de marca que despliega el emblema de Ulama junto
 * con el nombre de la aplicacion y su distintivo de version en formato pildora.
 */
@Composable
private fun AboutBrandCard(
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius)

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(UlamaTheme.spacing.large)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(RadiusLarge))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = stringResource(R.string.cd_settings_about_app_logo),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.width(UlamaTheme.spacing.large))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = stringResource(R.string.settings_about_version_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = UlamaTheme.spacing.small,
                                vertical = UlamaTheme.spacing.extraSmall
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

                Text(
                    text = stringResource(R.string.settings_about_brand_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tarjeta interactiva generica para filas informativas y de navegacion en la seccion Acerca de.
 * Centraliza la geometria de esquinas, el distintivo circular y la respuesta tactil para evitar duplicacion.
 */
@Composable
private fun AboutActionCard(
    title: String,
    icon: Painter,
    iconContentDescription: String?,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius),
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = UlamaTheme.spacing.normal,
                    vertical = UlamaTheme.spacing.normal
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = iconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(UlamaTheme.spacing.normal))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Tarjeta interactiva con conmutador Switch para configurar la verificacion de actualizaciones al arrancar.
 * Implementa el patron visual de Google Pixel con iconos en el thumb para estados activo e inactivo.
 */
@Composable
private fun AboutSwitchCard(
    title: String,
    description: String,
    icon: Painter,
    iconContentDescription: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius),
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = UlamaTheme.spacing.normal,
                    vertical = UlamaTheme.spacing.normal
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = iconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(UlamaTheme.spacing.normal))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                }
            )
        }
    }
}

/**
 * Tipos de hojas modales inferiores desplegables en la seccion Acerca de.
 * Centraliza la seleccion modal para evitar proliferacion de banderas booleanas independientes.
 */
private enum class AboutSheetType {
    NONE,
    CHANGELOG,
    THIRD_PARTY,
    LICENSE
}

/**
 * Contenedor base para hojas modales inferiores en la seccion Acerca de.
 * Provee barra de arrastre estandarizada, cabecera con titulo, subtitulo y boton de cierre circular.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutBaseBottomSheet(
    title: String,
    subtitle: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UlamaTheme.spacing.normal)
                .padding(bottom = UlamaTheme.spacing.listBottomPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UlamaTheme.spacing.medium)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end = 48.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cd_close_sheet),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
                    .verticalScroll(rememberScrollState()),
                content = content
            )
        }
    }
}

/**
 * Tarjeta de caracteristica para hojas modales con distintivo circular e informacion estructurada.
 */
@Composable
private fun AboutFeatureItemCard(
    title: String,
    description: String,
    icon: Painter,
    shape: Shape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius),
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(UlamaTheme.spacing.medium)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Hoja modal con el desglose de cambios y nuevas caracteristicas en Ulama 2.0.
 */
@Composable
private fun ChangelogBottomSheet(
    onDismissRequest: () -> Unit
) {
    val restRadius = UlamaTheme.shapes.cardRestRadius
    val innerRadius = UlamaTheme.shapes.cardConnectedInnerRadius
    val topShape = remember(restRadius, innerRadius) {
        RoundedCornerShape(
            topStart = restRadius,
            topEnd = restRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
    }
    val middleShape = remember(innerRadius) { RoundedCornerShape(innerRadius) }
    val bottomShape = remember(restRadius, innerRadius) {
        RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = restRadius,
            bottomEnd = restRadius
        )
    }
    val entries = remember {
        listOf(
            Triple(R.string.settings_about_changelog_feat4_title, R.string.settings_about_changelog_feat4_desc, R.drawable.ic_update),
            Triple(R.string.settings_about_changelog_feat5_title, R.string.settings_about_changelog_feat5_desc, R.drawable.ic_language),
            Triple(R.string.settings_about_changelog_feat1_title, R.string.settings_about_changelog_feat1_desc, R.drawable.ic_soccer_ball),
            Triple(R.string.settings_about_changelog_feat2_title, R.string.settings_about_changelog_feat2_desc, R.drawable.ic_palette),
            Triple(R.string.settings_about_changelog_feat3_title, R.string.settings_about_changelog_feat3_desc, R.drawable.ic_notifications_active)
        )
    }

    AboutBaseBottomSheet(
        title = stringResource(R.string.settings_about_changelog_sheet_title),
        subtitle = stringResource(R.string.settings_about_changelog_sheet_subtitle),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            entries.forEachIndexed { index, (titleRes, descRes, iconRes) ->
                val shape = when (index) {
                    0 -> topShape
                    entries.lastIndex -> bottomShape
                    else -> middleShape
                }
                AboutFeatureItemCard(
                    title = stringResource(titleRes),
                    description = stringResource(descRes),
                    icon = painterResource(iconRes),
                    shape = shape
                )
            }
        }
    }
}

/**
 * Hoja modal con el detalle de las fuentes de datos deportivas y licencias de terceros.
 */
@Composable
private fun ThirdPartyBottomSheet(
    onDismissRequest: () -> Unit
) {
    val topShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardRestRadius,
        topEnd = UlamaTheme.shapes.cardRestRadius,
        bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
    )
    val bottomShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomStart = UlamaTheme.shapes.cardRestRadius,
        bottomEnd = UlamaTheme.shapes.cardRestRadius
    )

    AboutBaseBottomSheet(
        title = stringResource(R.string.settings_about_third_party_sheet_title),
        subtitle = stringResource(R.string.settings_about_third_party_sheet_subtitle),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            AboutFeatureItemCard(
                title = stringResource(R.string.settings_about_third_party_espn_title),
                description = stringResource(R.string.settings_about_third_party_espn_desc),
                icon = painterResource(R.drawable.ic_info),
                shape = topShape
            )
            AboutFeatureItemCard(
                title = stringResource(R.string.settings_about_third_party_oss_title),
                description = stringResource(R.string.settings_about_third_party_oss_desc),
                icon = painterResource(R.drawable.ic_code),
                shape = bottomShape
            )
        }
    }
}

/**
 * Hoja modal con los terminos de la licencia MIT de Ulama.
 */
@Composable
private fun LicenseBottomSheet(
    onDismissRequest: () -> Unit
) {
    AboutBaseBottomSheet(
        title = stringResource(R.string.settings_about_license_sheet_title),
        subtitle = stringResource(R.string.settings_about_license_sheet_subtitle),
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            shape = RoundedCornerShape(UlamaTheme.shapes.cardRestRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = UlamaTheme.borders.cardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.settings_about_license_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(UlamaTheme.spacing.large)
            )
        }
    }
}

/**
 * Hoja modal con la politica de privacidad y transparencia de uso de red de Ulama.
 * Detalla que no existe telemetria ni recoleccion de datos personales, explicando
 * el consumo de datos de las APIs publicas de ESPN.
 */
@Composable
private fun PrivacyBottomSheet(
    onDismissRequest: () -> Unit
) {
    val topShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardRestRadius,
        topEnd = UlamaTheme.shapes.cardRestRadius,
        bottomStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomEnd = UlamaTheme.shapes.cardConnectedInnerRadius
    )
    val middleShape = RoundedCornerShape(UlamaTheme.shapes.cardConnectedInnerRadius)
    val bottomShape = RoundedCornerShape(
        topStart = UlamaTheme.shapes.cardConnectedInnerRadius,
        topEnd = UlamaTheme.shapes.cardConnectedInnerRadius,
        bottomStart = UlamaTheme.shapes.cardRestRadius,
        bottomEnd = UlamaTheme.shapes.cardRestRadius
    )

    AboutBaseBottomSheet(
        title = stringResource(R.string.settings_about_privacy_sheet_title),
        subtitle = stringResource(R.string.settings_about_privacy_sheet_subtitle),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            AboutFeatureItemCard(
                title = stringResource(R.string.settings_about_privacy_no_data_title),
                description = stringResource(R.string.settings_about_privacy_no_data_desc),
                icon = painterResource(R.drawable.ic_check),
                shape = topShape
            )
            AboutFeatureItemCard(
                title = stringResource(R.string.settings_about_privacy_internet_title),
                description = stringResource(R.string.settings_about_privacy_internet_desc),
                icon = painterResource(R.drawable.ic_soccer_ball),
                shape = middleShape
            )
            AboutFeatureItemCard(
                title = stringResource(R.string.settings_about_privacy_espn_title),
                description = stringResource(R.string.settings_about_privacy_espn_desc),
                icon = painterResource(R.drawable.ic_code),
                shape = bottomShape
            )
        }
    }
}


