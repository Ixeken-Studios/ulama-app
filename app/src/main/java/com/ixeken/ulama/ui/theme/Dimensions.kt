package com.ixeken.ulama.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nivel 1: Tokens primitivos de dimensiones escalares y tamanos de componentes.
 */
val DimNone: Dp = 0.dp
val DimBorderThin: Dp = 1.dp
val DimIconSmall: Dp = 14.dp
val DimIconMedium: Dp = 20.dp
val DimIconNormal: Dp = 24.dp
val DimCrestSmall: Dp = 20.dp
val DimCrestNormal: Dp = 48.dp
val DimTouchTarget: Dp = 48.dp
val DimMaxContentWidth: Dp = 600.dp
val DimTeamNameHeight: Dp = 36.dp
val DimTableRankWidth: Dp = 22.dp
val DimTableGpWidth: Dp = 26.dp
val DimTableStatWidth: Dp = 22.dp
val DimTableGdWidth: Dp = 32.dp
val DimTablePtsWidth: Dp = 30.dp
val DimTopBarLogo: Dp = 44.dp
val DimPullToRefreshThreshold: Dp = 80.dp
val DimDisciplineCardWidth: Dp = 8.dp
val DimDisciplineCardHeight: Dp = 11.dp
val DimDisciplineCardBorder: Dp = 1.5.dp
val DimWorldMapHeight: Dp = 172.dp
val DimLeagueLogoList: Dp = 28.dp
val DimTeamGridCrest: Dp = 44.dp
val DimTeamGridCellMin: Dp = 152.dp

/**
 * Nivel 2: Tokens semanticos de dimensiones para componentes de UI.
 */
@Immutable
data class UlamaDimensions(
    val borderThin: Dp = DimBorderThin,
    val iconSmall: Dp = DimIconSmall,
    val iconMedium: Dp = DimIconMedium,
    val iconNormal: Dp = DimIconNormal,
    val crestSmall: Dp = DimCrestSmall,
    val crestNormal: Dp = DimCrestNormal,
    val minTouchTarget: Dp = DimTouchTarget,
    val maxContentWidth: Dp = DimMaxContentWidth,
    val teamNameBoxHeight: Dp = DimTeamNameHeight,
    val tableRankWidth: Dp = DimTableRankWidth,
    val tableGpWidth: Dp = DimTableGpWidth,
    val tableStatWidth: Dp = DimTableStatWidth,
    val tableGdWidth: Dp = DimTableGdWidth,
    val tablePtsWidth: Dp = DimTablePtsWidth,
    val topBarLogo: Dp = DimTopBarLogo,
    val pullToRefreshThreshold: Dp = DimPullToRefreshThreshold,
    val disciplineCardWidth: Dp = DimDisciplineCardWidth,
    val disciplineCardHeight: Dp = DimDisciplineCardHeight,
    val disciplineCardBorder: Dp = DimDisciplineCardBorder,
    val worldMapHeight: Dp = DimWorldMapHeight,
    val leagueLogoList: Dp = DimLeagueLogoList,
    val teamGridCrest: Dp = DimTeamGridCrest,
    val teamGridCellMin: Dp = DimTeamGridCellMin
)

val LocalUlamaDimensions = staticCompositionLocalOf { UlamaDimensions() }
