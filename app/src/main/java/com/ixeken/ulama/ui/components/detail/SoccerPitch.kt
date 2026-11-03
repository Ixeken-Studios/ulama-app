package com.ixeken.ulama.ui.components.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.LineupPlayer
import com.ixeken.ulama.core.model.TeamLineup
import com.ixeken.ulama.ui.components.TeamColorText
import com.ixeken.ulama.ui.components.UlamaSegmentedTabs
import com.ixeken.ulama.ui.theme.UlamaTheme

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
                    .padding(bottom = UlamaTheme.spacing.contentSpacing)
            )
        }

        // Terreno de Juego Verde Sobrio con altura optimizada para formaciones complejas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF26472D)) // Verde cesped tenue sobrio
                .border(1.dp, Color(0xFF386641), RoundedCornerShape(16.dp))
        ) {
            val pitchLineColor = Color(0x60FFFFFF)

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

            // Filas tacticas dinamicas segun la formacion del equipo
            val tacticalRows = remember(currentLineup.formation, currentLineup.starters) {
                TacticalFormationUtils.resolveTacticalRows(currentLineup.formation, currentLineup.starters)
            }

            if (currentLineup.starters.isEmpty()) {
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
                        TacticalRow(players = rowPlayers, badgeColor = teamBadgeColor, textColor = contrastTextColor)
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

        // Bloque de Jugadores Suplentes (Cuadricula equilibrada de 2 columnas)
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
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
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

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = player.name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, lineHeight = 11.sp),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF9F2E1),
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 68.dp)
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun BenchPlayerItem(
    player: LineupPlayer,
    teamColor: Color,
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier.width(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (player.subbedIn) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
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
