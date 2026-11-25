package com.ixeken.ulama.ui.components.detail

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.LineupPlayer
import com.ixeken.ulama.core.model.TeamLineup
import com.ixeken.ulama.ui.components.ExpressiveConnectedButtonGroup
import com.ixeken.ulama.ui.components.TeamColorText
import com.ixeken.ulama.ui.components.UlamaSegmentedTabs
import com.ixeken.ulama.ui.theme.UlamaTheme

private enum class LineupPhase(@field:StringRes val labelRes: Int) {
    INITIAL(R.string.lineup_phase_initial),
    FINAL(R.string.lineup_phase_final)
}

/**
 * Pizarra tactica que renderiza el terreno de juego, las lineas dinamicas de jugadores titulares
 * segun la formacion del equipo, el conmutador entre clubes y la lista de suplentes con sus respectivos cambios.
 */
@Composable
fun SoccerPitch(
    homeLineup: TeamLineup?,
    awayLineup: TeamLineup?,
    homeColor: Color? = null,
    awayColor: Color? = null,
    modifier: Modifier = Modifier
) {
    if (homeLineup == null && awayLineup == null) return

    val lineups = listOfNotNull(homeLineup, awayLineup)
    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentLineup = lineups.getOrNull(selectedIndex) ?: lineups.first()
    var selectedPhase by remember(currentLineup.teamId) { mutableStateOf(LineupPhase.INITIAL) }
    val isFinalView = selectedPhase == LineupPhase.FINAL && currentLineup.hasSubstitutions
    val displayedStarters = remember(currentLineup, isFinalView) {
        if (isFinalView) currentLineup.resolveFinalStarters() else currentLineup.starters
    }

    val isHome = currentLineup == homeLineup
    val teamBadgeColor = if (isHome) {
        homeColor ?: DetailColorUtils.parseHexColor(currentLineup.teamColor, MaterialTheme.colorScheme.primary)
    } else {
        awayColor ?: DetailColorUtils.parseHexColor(currentLineup.teamColor, MaterialTheme.colorScheme.tertiary)
    }
    val contrastTextColor = DetailColorUtils.getContrastingTextColor(teamBadgeColor)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Conmutador de club reutilizando UlamaSegmentedTabs
        if (lineups.size > 1) {
            UlamaSegmentedTabs(
                items = lineups,
                selectedItem = currentLineup,
                onItemSelected = { selectedIndex = lineups.indexOf(it) },
                label = { it.shortName.ifBlank { it.teamName } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UlamaTheme.spacing.small)
            )
        }

        // Leyenda permanente centrada sobre posiciones referenciales antes de la cancha
        Text(
            text = stringResource(R.string.lineup_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = UlamaTheme.spacing.small)
        )

        // Terreno de Juego Verde Sobrio mediante tokens semanticos de nivel 3
        val pitchBackgroundColor = UlamaTheme.colors.pitchTurf
        val pitchBorderColor = UlamaTheme.colors.pitchBorder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pitchBackgroundColor)
                .border(UlamaTheme.dimensions.borderThin, pitchBorderColor, RoundedCornerShape(16.dp))
        ) {
            val pitchLineColor = UlamaTheme.colors.pitchLines

            // Lineas reglamentarias de la cancha
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 1.5.dp.toPx())

                // Linea de medio campo
                drawLine(
                    color = pitchLineColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = stroke.width
                )

                // Circulo central
                drawCircle(
                    color = pitchLineColor,
                    radius = size.width * 0.16f,
                    center = Offset(size.width / 2, size.height / 2),
                    style = stroke
                )

                // Area del portero inferior
                val penaltyBoxWidth = size.width * 0.45f
                val penaltyBoxHeight = size.height * 0.18f
                drawRect(
                    color = pitchLineColor,
                    topLeft = Offset((size.width - penaltyBoxWidth) / 2, size.height - penaltyBoxHeight),
                    size = Size(penaltyBoxWidth, penaltyBoxHeight),
                    style = stroke
                )

                // Area chica inferior
                val smallBoxWidth = size.width * 0.24f
                val smallBoxHeight = size.height * 0.08f
                drawRect(
                    color = pitchLineColor,
                    topLeft = Offset((size.width - smallBoxWidth) / 2, size.height - smallBoxHeight),
                    size = Size(smallBoxWidth, smallBoxHeight),
                    style = stroke
                )
            }

            // Filas tacticas dinamicas segun la formacion del equipo y fase seleccionada
            val tacticalRows = remember(currentLineup.formation, displayedStarters) {
                TacticalFormationUtils.resolveTacticalRows(currentLineup.formation, displayedStarters)
            }

            if (displayedStarters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.lineups_not_available),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    tacticalRows.forEach { rowPlayers ->
                        TacticalRow(
                            players = rowPlayers,
                            badgeColor = teamBadgeColor,
                            textColor = contrastTextColor,
                            pitchBackgroundColor = pitchBackgroundColor
                        )
                    }
                }
            }

            // Formacion tactica discreta en la esquina inferior izquierda con el color tenue de las lineas
            if (currentLineup.formation.isNotBlank()) {
                Text(
                    text = currentLineup.formation,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = pitchLineColor,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 8.dp)
                )
            }
        }

        // Conmutador M3 Expressive Inicial | Final debajo de la cancha solo si hubo sustituciones
        if (currentLineup.hasSubstitutions) {
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.lineup_phase_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExpressiveConnectedButtonGroup(
                    options = LineupPhase.entries,
                    selectedOption = selectedPhase,
                    onOptionSelected = { selectedPhase = it },
                    labelProvider = { stringResource(it.labelRes) },
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
        }

        // Bloque fijo de Jugadores Suplentes (Cuadricula equilibrada de 2 columnas)
        if (currentLineup.bench.isNotEmpty()) {
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
            Surface(
                shape = UlamaTheme.shapes.card,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = UlamaTheme.borders.cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UlamaTheme.spacing.cardPadding)
                ) {
                    Text(
                        text = stringResource(R.string.lineup_bench),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

                    val benchPairs = remember(currentLineup.bench) {
                        currentLineup.bench.chunked(2)
                    }

                    benchPairs.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BenchPlayerItem(
                                player = pair[0],
                                teamColor = teamBadgeColor,
                                modifier = Modifier.weight(1f)
                            )
                            if (pair.size > 1) {
                                BenchPlayerItem(
                                    player = pair[1],
                                    teamColor = teamBadgeColor,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalRow(
    players: List<LineupPlayer>,
    badgeColor: Color,
    textColor: Color,
    pitchBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        players.forEach { player ->
            PlayerPitchBadge(
                player = player,
                badgeColor = badgeColor,
                textColor = textColor,
                pitchBackgroundColor = pitchBackgroundColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlayerPitchBadge(
    player: LineupPlayer,
    badgeColor: Color,
    textColor: Color,
    pitchBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val sentOffAlpha = if (player.isSentOff) 0.52f else 1f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // El contenedor exterior evita que CircleShape recorte el overlay de la tarjeta en esquina superior derecha
        Box(contentAlignment = Alignment.Center) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { alpha = sentOffAlpha }
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.5.dp, textColor.copy(alpha = 0.8f), CircleShape)
            ) {
                Text(
                    text = player.jersey,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
            DisciplineCardBadge(
                yellowCards = player.yellowCards,
                hasRedCard = player.hasRedCard,
                cutoutBorderColor = pitchBackgroundColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = UlamaTheme.spacing.extraSmall,
                        y = -UlamaTheme.spacing.extraSmall / 2
                    )
            )
            PitchStatBadge(
                count = player.goalsCount,
                iconRes = R.drawable.ic_soccer_ball,
                iconTint = UlamaTheme.colors.pitchGoalIcon,
                descriptionRes = R.string.cd_player_scored,
                cutoutBorderColor = pitchBackgroundColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(
                        x = UlamaTheme.spacing.extraSmall,
                        y = UlamaTheme.spacing.extraSmall / 2
                    )
            )
            PitchStatBadge(
                count = player.assistsCount,
                iconRes = R.drawable.ic_steps,
                iconTint = UlamaTheme.colors.pitchAssistIcon,
                descriptionRes = R.string.cd_player_assisted,
                cutoutBorderColor = pitchBackgroundColor,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = -UlamaTheme.spacing.extraSmall,
                        y = UlamaTheme.spacing.extraSmall / 2
                    )
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = player.name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, lineHeight = 11.sp),
            fontWeight = FontWeight.SemiBold,
            color = UlamaTheme.colors.pitchPlayerText,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 68.dp)
                .padding(horizontal = 2.dp)
                .graphicsLayer { alpha = sentOffAlpha }
        )
    }
}

@Composable
private fun BenchPlayerItem(
    player: LineupPlayer,
    teamColor: Color,
    modifier: Modifier = Modifier
) {
    val sentOffAlpha = if (player.isSentOff) 0.6f else 1f
    Column(
        modifier = modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamColorText(
                text = player.jersey,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = teamColor,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(22.dp)
                    .graphicsLayer { alpha = sentOffAlpha }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (player.subbedIn) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .graphicsLayer { alpha = sentOffAlpha }
            )
            BenchStatBadge(
                count = player.goalsCount,
                iconRes = R.drawable.ic_soccer_ball,
                iconTint = MaterialTheme.colorScheme.primary,
                descriptionRes = R.string.cd_player_scored
            )
            BenchStatBadge(
                count = player.assistsCount,
                iconRes = R.drawable.ic_steps,
                iconTint = MaterialTheme.colorScheme.secondary,
                descriptionRes = R.string.cd_player_assisted
            )
            if (player.subbedIn) {
                Spacer(modifier = Modifier.width(4.dp))
                val subText = if (!player.subInMinute.isNullOrBlank()) "↑ ${player.subInMinute}" else "↑"
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (player.yellowCards > 0 || player.hasRedCard) {
                Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                DisciplineCardBadge(
                    yellowCards = player.yellowCards,
                    hasRedCard = player.hasRedCard,
                    cutoutBorderColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
        }
        if (player.subbedIn && !player.subbedInForName.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(28.dp))
                Text(
                    text = stringResource(R.string.lineup_subbed_in_for, player.subbedInForName.orEmpty()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Distintivo compacto reutilizable en la cancha para goles (esquina inferior derecha)
 * y asistencias (esquina inferior izquierda) con borde de recorte del color del cesped.
 */
@Composable
@NonRestartableComposable
private fun PitchStatBadge(
    count: Int,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    @StringRes descriptionRes: Int,
    cutoutBorderColor: Color,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    val description = stringResource(descriptionRes)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(cutoutBorderColor)
            .padding(horizontal = 1.5.dp, vertical = 1.dp)
            .semantics { contentDescription = description }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(11.dp)
        )
        if (count > 1) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 9.sp),
                fontWeight = FontWeight.Black,
                color = iconTint,
                modifier = Modifier.padding(start = 1.dp)
            )
        }
    }
}

@Composable
@NonRestartableComposable
private fun BenchStatBadge(
    count: Int,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    @StringRes descriptionRes: Int
) {
    if (count <= 0) return

    Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
    Icon(
        painter = painterResource(iconRes),
        contentDescription = stringResource(descriptionRes),
        tint = iconTint,
        modifier = Modifier.size(12.dp)
    )
    if (count > 1) {
        Text(
            text = "×$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = iconTint
        )
    }
}

/**
 * Distintivo vertical compacto para amonestaciones y expulsiones en cancha y suplentes.
 * Diferencia visualmente amarilla simple, roja directa y doble amarilla superpuesta sin recargar el texto.
 */
@Composable
@NonRestartableComposable
private fun DisciplineCardBadge(
    yellowCards: Int,
    hasRedCard: Boolean,
    cutoutBorderColor: Color,
    modifier: Modifier = Modifier
) {
    if (yellowCards <= 0 && !hasRedCard) return

    val badgeWidth = UlamaTheme.dimensions.disciplineCardWidth
    val badgeHeight = UlamaTheme.dimensions.disciplineCardHeight
    val badgeShape = UlamaTheme.shapes.disciplineCard
    val borderWidth = UlamaTheme.dimensions.disciplineCardBorder
    val yellowColor = UlamaTheme.colors.cardYellow
    val redColor = UlamaTheme.colors.cardRed
    val isDoubleCard = yellowCards >= 2 || (yellowCards >= 1 && hasRedCard)

    val description = stringResource(
        when {
            isDoubleCard -> R.string.cd_second_yellow_card
            hasRedCard -> R.string.cd_red_card
            else -> R.string.cd_yellow_card
        }
    )

    if (isDoubleCard) {
        val stackOffsetX = UlamaTheme.spacing.extraSmall
        val stackOffsetY = stackOffsetX / 2
        Box(
            modifier = modifier
                .size(width = badgeWidth + stackOffsetX, height = badgeHeight + stackOffsetY)
                .semantics { contentDescription = description }
        ) {
            DisciplineCardRectangle(
                fillColor = yellowColor,
                borderColor = cutoutBorderColor,
                shape = badgeShape,
                width = badgeWidth,
                height = badgeHeight,
                borderWidth = borderWidth
            )
            DisciplineCardRectangle(
                fillColor = redColor,
                borderColor = cutoutBorderColor,
                shape = badgeShape,
                width = badgeWidth,
                height = badgeHeight,
                borderWidth = borderWidth,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    } else {
        val singleColor = if (hasRedCard) redColor else yellowColor
        DisciplineCardRectangle(
            fillColor = singleColor,
            borderColor = cutoutBorderColor,
            shape = badgeShape,
            width = badgeWidth,
            height = badgeHeight,
            borderWidth = borderWidth,
            modifier = modifier.semantics { contentDescription = description }
        )
    }
}

@Composable
@NonRestartableComposable
private fun DisciplineCardRectangle(
    fillColor: Color,
    borderColor: Color,
    shape: Shape,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    borderWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(fillColor)
            .border(borderWidth, borderColor, shape)
    )
}
