package com.ixeken.ulama.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.RadialBracketTree
import com.ixeken.ulama.core.model.RadialMatch
import com.ixeken.ulama.core.model.RadialTeam
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Visualizador adaptativo universal de eliminatorias en formato clasico de dos alas simetricas.
 * Escala dinamicamente su geometria entre 320dp y 600dp segun el factor de forma del dispositivo
 * (celulares compactos, dispositivos plegables foldables y pantallas grandes).
 * Garantiza cero colisiones, aprovechamiento optimo de margenes laterales y horquillas ortogonales precisas.
 *
 * @param tree Arbol de eliminatorias con rondas jerarquicas, campeon y partido opcional de tercer lugar.
 * @param modifier Modificador base para el contenedor.
 * @param title Titulo descriptivo opcional para el encabezado de la tarjeta.
 * @param onTeamClick Callback opcional al presionar cualquier club en la llave.
 */
@Composable
fun RadialKnockoutBracket(
    tree: RadialBracketTree,
    modifier: Modifier = Modifier,
    title: String? = null,
    onTeamClick: ((RadialTeam) -> Unit)? = null
) {
    if (tree.rounds.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = UlamaTheme.dimensions.maxContentWidth),
        shape = UlamaTheme.shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder
    ) {
        Column(
            modifier = Modifier.padding(UlamaTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Encabezado con titulo y chip de cantidad de participantes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title ?: stringResource(R.string.title_radial_bracket),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = UlamaTheme.shapes.chip,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.badge_teams_count, tree.totalTeams),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

            // Cuadro eliminatorio con resolucion adaptativa segun el ancho disponible
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Aprovecha el ancho completo del contenedor acotado al maximo de diseno
                val canvasWidthDp = maxWidth.coerceAtMost(UlamaTheme.dimensions.maxContentWidth)
                // Altura proporcional que escala segun el volumen de participantes y factor de forma
                val canvasHeightDp = when {
                    tree.totalTeams >= 32 -> (canvasWidthDp * 1.35f).coerceIn(520.dp, 660.dp)
                    tree.totalTeams >= 16 -> (canvasWidthDp * 0.85f).coerceIn(340.dp, 440.dp)
                    else -> (canvasWidthDp * 0.70f).coerceIn(280.dp, 360.dp)
                }

                MacroTwoSidedBracket(
                    tree = tree,
                    widthDp = canvasWidthDp,
                    heightDp = canvasHeightDp,
                    onTeamClick = onTeamClick
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Componente de Cuadro Simetrico de Dos Alas (Macro)
// -----------------------------------------------------------------------------------------

@Composable
private fun MacroTwoSidedBracket(
    tree: RadialBracketTree,
    widthDp: Dp,
    heightDp: Dp,
    onTeamClick: ((RadialTeam) -> Unit)?
) {
    val is32Teams = tree.totalTeams >= 32

    // Dimensiones adaptativas segun el ancho del viewport (celulares compactos, dispositivos plegables y tablets)
    val nodeWidthDp = when {
        is32Teams -> when {
            widthDp >= 520.dp -> 34.dp
            widthDp >= 440.dp -> 30.dp
            else -> 26.dp
        }
        widthDp >= 520.dp -> 40.dp
        widthDp >= 440.dp -> 34.dp
        else -> 30.dp
    }
    val nodeHeightDp = when {
        is32Teams -> when {
            widthDp >= 520.dp -> 24.dp
            widthDp >= 440.dp -> 21.dp
            else -> 18.dp
        }
        widthDp >= 520.dp -> 26.dp
        widthDp >= 440.dp -> 23.dp
        else -> 20.dp
    }
    val champSizeDp = when {
        widthDp >= 520.dp -> 46.dp
        widthDp >= 440.dp -> 40.dp
        else -> 36.dp
    }
    val thirdWidthDp = when {
        widthDp >= 520.dp -> 96.dp
        widthDp >= 440.dp -> 90.dp
        else -> 84.dp
    }
    val thirdHeightDp = when {
        widthDp >= 520.dp -> 26.dp
        widthDp >= 440.dp -> 25.dp
        else -> 24.dp
    }

    val layoutData = remember(tree, widthDp, heightDp, nodeWidthDp, champSizeDp, thirdWidthDp, thirdHeightDp) {
        computeTwoSidedBracketLayout(
            tree = tree,
            widthDp = widthDp,
            heightDp = heightDp,
            nodeWidthDp = nodeWidthDp,
            champSizeDp = champSizeDp,
            thirdWidthDp = thirdWidthDp,
            thirdHeightDp = thirdHeightDp
        )
    }

    val isDark = UlamaTheme.isDark

    Box(
        modifier = Modifier.size(width = widthDp, height = heightDp),
        contentAlignment = Alignment.Center
    ) {
        // Capa 1: Canvas de lineas de conexion con puntas redondeadas y trazo de 2.2dp
        val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = if (is32Teams) 1.6.dp.toPx() else 2.2.dp.toPx()

            layoutData.lines.forEach { (start, end) ->
                drawLine(
                    color = lineColor,
                    start = Offset(start.x.toPx(), start.y.toPx()),
                    end = Offset(end.x.toPx(), end.y.toPx()),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }

        // Capa 2: Nodos de equipos con pastillas adaptativas
        layoutData.nodes.forEach { node ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = node.position.x - (nodeWidthDp / 2),
                        y = node.position.y - (nodeHeightDp / 2)
                    )
            ) {
                TwoSidedTeamPill(
                    team = node.team,
                    isDark = isDark,
                    isWinner = node.isWinner,
                    isLoser = node.isLoser,
                    width = nodeWidthDp,
                    height = nodeHeightDp,
                    onClick = onTeamClick
                )
            }
        }

        // Capa 3: Escudo del Campeon o trofeo en espera ubicado en el centro superior (sin solapar finalistas)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = layoutData.championPos.x - (champSizeDp / 2),
                    y = layoutData.championPos.y - (champSizeDp / 2)
                )
        ) {
            CenterChampionCrest(
                champion = tree.champion,
                isDark = isDark,
                size = champSizeDp,
                onClick = onTeamClick
            )
        }

        // Capa 4: Partido por el 3er Lugar centrado horizontalmente en el centro inferior
        layoutData.thirdPlaceMatchData?.let { thirdData ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = thirdData.position.x - (thirdData.width / 2),
                        y = thirdData.position.y - (thirdData.height / 2)
                    )
            ) {
                ThirdPlaceMacroBadge(
                    match = thirdData.match,
                    isDark = isDark,
                    modifier = Modifier.height(thirdData.height),
                    onClick = onTeamClick
                )
            }
        }
    }
}

/**
 * Pastilla adaptativa que exhibe el escudo en formato natural y el marcador de goles.
 * Mantiene un borde neutro y uniforme eliminando resaltados perimetrales innecesarios.
 * La distincion de victoria se enfoca unicamente en el marcador en negrita y color primario,
 * mientras que los goles del perdedor se atenuan para proveer contraste inmediato.
 * Aplica Clip Before Click garantizando confinamiento estricto del ripple tactil.
 */
@Composable
private fun TwoSidedTeamPill(
    team: RadialTeam?,
    isDark: Boolean,
    isWinner: Boolean,
    isLoser: Boolean,
    width: Dp,
    height: Dp,
    onClick: ((RadialTeam) -> Unit)?
) {
    val pillShape = CircleShape

    if (team == null) {
        Surface(
            modifier = Modifier.size(width = width, height = height),
            shape = pillShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "-",
                    style = if (width >= 36.dp) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    Surface(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(pillShape)
            .clickable(enabled = onClick != null) { onClick?.invoke(team) },
        shape = pillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (width >= 36.dp) 4.dp else 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val crestSize = when {
                width >= 36.dp -> if (team.score != null) 16.dp else 18.dp
                width >= 32.dp -> if (team.score != null) 14.dp else 16.dp
                width >= 26.dp -> if (team.score != null) 13.dp else 14.dp
                else -> if (team.score != null) 11.dp else 12.dp
            }
            TeamCrestImage(
                logoUrl = team.getCrestUrl(isDark),
                teamName = team.name,
                abbreviation = team.shortName,
                teamId = team.id,
                showMonogram = false,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(crestSize)
            )
            if (team.score != null) {
                Spacer(modifier = Modifier.width(if (width >= 36.dp) 3.5.dp else 2.dp))
                Text(
                    text = team.score,
                    style = if (width >= 36.dp) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isWinner -> MaterialTheme.colorScheme.primary
                        isLoser -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Escudo exclusivo del campeon o trofeo en espera en el centro superior.
 */
@Composable
private fun CenterChampionCrest(
    champion: RadialTeam?,
    isDark: Boolean,
    size: Dp,
    onClick: ((RadialTeam) -> Unit)?
) {
    val centerShape = CircleShape

    if (champion == null) {
        Surface(
            modifier = Modifier
                .size(size)
                .clip(centerShape),
            shape = centerShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_trophy),
                    contentDescription = stringResource(R.string.label_champion),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(size - 18.dp)
                )
            }
        }
        return
    }

    Surface(
        modifier = Modifier
            .size(size)
            .clip(centerShape)
            .clickable(enabled = onClick != null) { onClick?.invoke(champion) },
        shape = centerShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            TeamCrestImage(
                logoUrl = champion.getCrestUrl(isDark),
                teamName = champion.name,
                abbreviation = champion.shortName,
                teamId = champion.id,
                showMonogram = false,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size - 10.dp)
            )
        }
    }
}

/**
 * Badge horizontal compacto del partido por el 3er lugar en el centro inferior.
 */
@Composable
private fun ThirdPlaceMacroBadge(
    match: RadialMatch,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: ((RadialTeam) -> Unit)?
) {
    val badgeShape = MaterialTheme.shapes.medium

    Surface(
        modifier = modifier,
        shape = badgeShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
        ) {
            Text(
                text = stringResource(R.string.bracket_third_place_short),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            val teamA = match.teamA
            if (teamA != null) {
                val hasWinner = match.winnerTeamId != null
                val isWA = hasWinner && match.winnerTeamId == teamA.id
                val isLA = hasWinner && match.winnerTeamId != teamA.id
                TeamCrestImage(
                    logoUrl = teamA.getCrestUrl(isDark),
                    teamName = teamA.name,
                    abbreviation = teamA.shortName,
                    teamId = teamA.id,
                    showMonogram = false,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .clickable(enabled = onClick != null) { onClick?.invoke(teamA) }
                )
                teamA.score?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isWA) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isWA -> MaterialTheme.colorScheme.primary
                            isLA -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Text(
                text = if (teamA?.score != null && match.teamB?.score != null) "-" else "vs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            val teamB = match.teamB
            if (teamB != null) {
                val hasWinner = match.winnerTeamId != null
                val isWB = hasWinner && match.winnerTeamId == teamB.id
                val isLB = hasWinner && match.winnerTeamId != teamB.id
                teamB.score?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isWB) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isWB -> MaterialTheme.colorScheme.primary
                            isLB -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                TeamCrestImage(
                    logoUrl = teamB.getCrestUrl(isDark),
                    teamName = teamB.name,
                    abbreviation = teamB.shortName,
                    teamId = teamB.id,
                    showMonogram = false,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .clickable(enabled = onClick != null) { onClick?.invoke(teamB) }
                )
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Motor Matematico Universal Adaptativo de Dos Alas Simetricas
// -----------------------------------------------------------------------------------------

@Immutable
private data class PointDp(val x: Dp, val y: Dp)

@Immutable
private data class MacroLayoutNode(
    val team: RadialTeam?,
    val position: PointDp,
    val isWinner: Boolean,
    val isLoser: Boolean = false
)

@Immutable
private data class ThirdPlaceMacroData(
    val match: RadialMatch,
    val position: PointDp,
    val width: Dp,
    val height: Dp
)

@Immutable
private data class TwoSidedLayoutData(
    val nodes: List<MacroLayoutNode>,
    val lines: List<Pair<PointDp, PointDp>>,
    val championPos: PointDp,
    val thirdPlaceMatchData: ThirdPlaceMacroData? = null
)

/**
 * Calcula de manera completamente parametrizada y adaptativa las coordenadas espaciales
 * del cuadro eliminatorio para cualquier ancho (320dp a 600dp) y altura del viewport.
 */
private fun computeTwoSidedBracketLayout(
    tree: RadialBracketTree,
    widthDp: Dp,
    heightDp: Dp,
    nodeWidthDp: Dp,
    champSizeDp: Dp,
    thirdWidthDp: Dp,
    thirdHeightDp: Dp
): TwoSidedLayoutData {
    val nodes = mutableListOf<MacroLayoutNode>()
    val lines = mutableListOf<Pair<PointDp, PointDp>>()

    val totalRounds = tree.rounds.size
    if (totalRounds < 2) {
        return TwoSidedLayoutData(emptyList(), emptyList(), PointDp(widthDp / 2, heightDp / 2))
    }

    val cx = widthDp / 2
    val cy = heightDp * 0.47f

    // Campeon arriba en el centro (escala con la altura)
    val championPos = PointDp(cx, (heightDp * 0.12f).coerceIn(36.dp, 48.dp))
    val champRadius = champSizeDp / 2

    val wingRoundsCount = totalRounds - 1
    val is32Teams = tree.totalTeams >= 32

    val nodeHalfW = nodeWidthDp / 2

    // Margen lateral adaptativo segun factor de forma que garantiza espaciado simetrico en ambos extremos
    val sidePadding = when {
        widthDp >= 520.dp -> 16.dp
        widthDp >= 440.dp -> 12.dp
        is32Teams -> 8.dp
        else -> 10.dp
    }
    val xLeftMin = sidePadding + nodeHalfW

    // Separacion central de los finalistas
    val finalistCenterOffset = when {
        is32Teams -> when {
            widthDp >= 520.dp -> 30.dp
            widthDp >= 440.dp -> 26.dp
            else -> 22.dp
        }
        widthDp >= 520.dp -> 38.dp
        widthDp >= 440.dp -> 32.dp
        else -> 26.dp
    }
    val xLeftFinal = cx - finalistCenterOffset
    val xRightFinal = cx + finalistCenterOffset

    val stepX = if (wingRoundsCount > 0) (xLeftFinal - xLeftMin) / wingRoundsCount else 0.dp

    val leftYCenters = mutableMapOf<Pair<Int, Int>, Dp>()
    val rightYCenters = mutableMapOf<Pair<Int, Int>, Dp>()

    // 1. Ronda exterior inicial (roundIndex = 0)
    val round0 = tree.rounds[0]
    val matches0 = round0.matches
    val totalMatches0 = matches0.size
    val halfMatches0 = totalMatches0 / 2

    val yMin = if (widthDp >= 480.dp) 24.dp else 20.dp
    val yMax = heightDp - if (widthDp >= 480.dp) 48.dp else 42.dp
    val stepY0 = if (halfMatches0 > 0) (yMax - yMin) / halfMatches0 else 0.dp
    val pairDeltaY = (stepY0 * 0.23f).coerceIn(
        if (widthDp >= 480.dp) 15.dp else 12.dp,
        if (widthDp >= 480.dp) 20.dp else 16.dp
    )

    for (m in 0 until halfMatches0) {
        val yMid = yMin + (stepY0 * m) + (stepY0 / 2)
        val yA = yMid - pairDeltaY
        val yB = yMid + pairDeltaY

        // Ala Izquierda
        val matchLeft = matches0.getOrNull(m)
        if (matchLeft != null) {
            val xPos = xLeftMin
            val hasW = matchLeft.winnerTeamId != null
            val isWA = hasW && matchLeft.winnerTeamId == matchLeft.teamA?.id
            val isLA = hasW && matchLeft.winnerTeamId != matchLeft.teamA?.id
            val isWB = hasW && matchLeft.winnerTeamId == matchLeft.teamB?.id
            val isLB = hasW && matchLeft.winnerTeamId != matchLeft.teamB?.id
            nodes.add(MacroLayoutNode(matchLeft.teamA, PointDp(xPos, yA), isWinner = isWA, isLoser = isLA))
            nodes.add(MacroLayoutNode(matchLeft.teamB, PointDp(xPos, yB), isWinner = isWB, isLoser = isLB))
            leftYCenters[Pair(0, m)] = yMid

            // Horquilla entre los dos equipos del partido m
            val nextX = if (wingRoundsCount > 1) xLeftMin + stepX else xLeftFinal
            val forkX = (xPos + nodeHalfW + nextX - nodeHalfW) / 2
            lines.add(Pair(PointDp(xPos + nodeHalfW, yA), PointDp(forkX, yA)))
            lines.add(Pair(PointDp(xPos + nodeHalfW, yB), PointDp(forkX, yB)))
            lines.add(Pair(PointDp(forkX, yA), PointDp(forkX, yB)))
            lines.add(Pair(PointDp(forkX, yMid), PointDp(nextX - nodeHalfW, yMid)))
        }

        // Ala Derecha (en espejo simetrico)
        val matchRight = matches0.getOrNull(m + halfMatches0)
        if (matchRight != null) {
            val xPos = widthDp - xLeftMin
            val hasW = matchRight.winnerTeamId != null
            val isWA = hasW && matchRight.winnerTeamId == matchRight.teamA?.id
            val isLA = hasW && matchRight.winnerTeamId != matchRight.teamA?.id
            val isWB = hasW && matchRight.winnerTeamId == matchRight.teamB?.id
            val isLB = hasW && matchRight.winnerTeamId != matchRight.teamB?.id
            nodes.add(MacroLayoutNode(matchRight.teamA, PointDp(xPos, yA), isWinner = isWA, isLoser = isLA))
            nodes.add(MacroLayoutNode(matchRight.teamB, PointDp(xPos, yB), isWinner = isWB, isLoser = isLB))
            rightYCenters[Pair(0, m)] = yMid

            val nextX = if (wingRoundsCount > 1) widthDp - (xLeftMin + stepX) else xRightFinal
            val forkX = (xPos - nodeHalfW + nextX + nodeHalfW) / 2
            lines.add(Pair(PointDp(xPos - nodeHalfW, yA), PointDp(forkX, yA)))
            lines.add(Pair(PointDp(xPos - nodeHalfW, yB), PointDp(forkX, yB)))
            lines.add(Pair(PointDp(forkX, yA), PointDp(forkX, yB)))
            lines.add(Pair(PointDp(forkX, yMid), PointDp(nextX + nodeHalfW, yMid)))
        }
    }

    // 2. Rondas intermedias (Cuartos, Semifinales)
    for (r in 1 until wingRoundsCount) {
        val currentRound = tree.rounds[r]
        val matchesInRound = currentRound.matches
        val halfMatches = matchesInRound.size / 2
        val xLeft = xLeftMin + (stepX * r)
        val xRight = widthDp - xLeft

        val isLastWingRound = (r == wingRoundsCount - 1)
        val nextXLeft = if (isLastWingRound) xLeftFinal else xLeft + stepX
        val nextXRight = widthDp - nextXLeft

        val forkXLeft = (xLeft + nodeHalfW + nextXLeft - nodeHalfW) / 2
        val forkXRight = (xRight - nodeHalfW + nextXRight + nodeHalfW) / 2

        for (m in 0 until halfMatches) {
            val yA = leftYCenters[Pair(r - 1, m * 2)] ?: yMin
            val yB = leftYCenters[Pair(r - 1, (m * 2) + 1)] ?: yMax
            val yMid = (yA + yB) / 2
            leftYCenters[Pair(r, m)] = yMid

            // Ala Izquierda
            val matchLeft = matchesInRound.getOrNull(m)
            if (matchLeft != null) {
                val hasW = matchLeft.winnerTeamId != null
                val isWA = hasW && matchLeft.winnerTeamId == matchLeft.teamA?.id
                val isLA = hasW && matchLeft.winnerTeamId != matchLeft.teamA?.id
                val isWB = hasW && matchLeft.winnerTeamId == matchLeft.teamB?.id
                val isLB = hasW && matchLeft.winnerTeamId != matchLeft.teamB?.id
                nodes.add(MacroLayoutNode(matchLeft.teamA, PointDp(xLeft, yA), isWinner = isWA, isLoser = isLA))
                nodes.add(MacroLayoutNode(matchLeft.teamB, PointDp(xLeft, yB), isWinner = isWB, isLoser = isLB))

                lines.add(Pair(PointDp(xLeft + nodeHalfW, yA), PointDp(forkXLeft, yA)))
                lines.add(Pair(PointDp(xLeft + nodeHalfW, yB), PointDp(forkXLeft, yB)))
                lines.add(Pair(PointDp(forkXLeft, yA), PointDp(forkXLeft, yB)))
                lines.add(Pair(PointDp(forkXLeft, yMid), PointDp(nextXLeft - nodeHalfW, yMid)))
            }

            // Ala Derecha
            val yRightA = rightYCenters[Pair(r - 1, m * 2)] ?: yMin
            val yRightB = rightYCenters[Pair(r - 1, (m * 2) + 1)] ?: yMax
            val yRightMid = (yRightA + yRightB) / 2
            rightYCenters[Pair(r, m)] = yRightMid

            val matchRight = matchesInRound.getOrNull(m + halfMatches)
            if (matchRight != null) {
                val hasW = matchRight.winnerTeamId != null
                val isWA = hasW && matchRight.winnerTeamId == matchRight.teamA?.id
                val isLA = hasW && matchRight.winnerTeamId != matchRight.teamA?.id
                val isWB = hasW && matchRight.winnerTeamId == matchRight.teamB?.id
                val isLB = hasW && matchRight.winnerTeamId != matchRight.teamB?.id
                nodes.add(MacroLayoutNode(matchRight.teamA, PointDp(xRight, yRightA), isWinner = isWA, isLoser = isLA))
                nodes.add(MacroLayoutNode(matchRight.teamB, PointDp(xRight, yRightB), isWinner = isWB, isLoser = isLB))

                lines.add(Pair(PointDp(xRight - nodeHalfW, yRightA), PointDp(forkXRight, yRightA)))
                lines.add(Pair(PointDp(xRight - nodeHalfW, yRightB), PointDp(forkXRight, yRightB)))
                lines.add(Pair(PointDp(forkXRight, yRightA), PointDp(forkXRight, yRightB)))
                lines.add(Pair(PointDp(forkXRight, yRightMid), PointDp(nextXRight + nodeHalfW, yRightMid)))
            }
        }
    }

    // 3. Finalistas en el centro y conexion ascendente hacia el Campeon
    val finalMatch = tree.rounds.lastOrNull()?.matches?.firstOrNull()
    if (finalMatch != null) {
        val hasW = finalMatch.winnerTeamId != null
        val isWA = hasW && finalMatch.winnerTeamId == finalMatch.teamA?.id
        val isLA = hasW && finalMatch.winnerTeamId != finalMatch.teamA?.id
        val isWB = hasW && finalMatch.winnerTeamId == finalMatch.teamB?.id
        val isLB = hasW && finalMatch.winnerTeamId != finalMatch.teamB?.id

        val finalY = leftYCenters[Pair(wingRoundsCount - 1, 0)] ?: cy
        nodes.add(MacroLayoutNode(finalMatch.teamA, PointDp(xLeftFinal, finalY), isWinner = isWA, isLoser = isLA))
        nodes.add(MacroLayoutNode(finalMatch.teamB, PointDp(xRightFinal, finalY), isWinner = isWB, isLoser = isLB))

        // Linea horizontal directa entre los dos finalistas
        lines.add(Pair(PointDp(xLeftFinal + nodeHalfW, finalY), PointDp(xRightFinal - nodeHalfW, finalY)))

        // Vástago vertical ascendente desde el punto medio hacia el Campeon superior
        lines.add(Pair(PointDp(cx, finalY), PointDp(cx, championPos.y + champRadius)))
    }

    // 4. Partido de Tercer Lugar: Abajo en el centro
    var thirdPlaceData: ThirdPlaceMacroData? = null
    tree.thirdPlaceMatch?.let { thirdMatch ->
        val thirdY = (heightDp - 42.dp).coerceAtLeast(cy + 60.dp)
        val thirdPos = PointDp(cx, thirdY)
        thirdPlaceData = ThirdPlaceMacroData(
            match = thirdMatch,
            position = thirdPos,
            width = thirdWidthDp,
            height = thirdHeightDp
        )

        // Linea vertical descendente conectando al tercer lugar sin solapamientos
        lines.add(Pair(PointDp(cx, cy), PointDp(cx, thirdY - (thirdHeightDp / 2))))
    }

    return TwoSidedLayoutData(
        nodes = nodes,
        lines = lines,
        championPos = championPos,
        thirdPlaceMatchData = thirdPlaceData
    )
}
