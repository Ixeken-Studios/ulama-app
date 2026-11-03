package com.ixeken.ulama.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.HeadToHeadSummary
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.ui.components.TeamColorText
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Tarjeta de historial cara a cara (Head-to-Head) entre dos clubes.
 * Muestra el balance acumulado de victorias y empates con barra proporcional tricromatica
 * y el desglose de los encuentros previos mas recientes.
 */
@Composable
fun HeadToHeadCard(
    h2h: HeadToHeadSummary,
    homeTeam: Team,
    awayTeam: Team,
    homeColor: Color? = null,
    awayColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val effectiveHomeColor = homeColor ?: MaterialTheme.colorScheme.primary
    val effectiveAwayColor = awayColor ?: MaterialTheme.colorScheme.tertiary
    val neutralColor = MaterialTheme.colorScheme.outlineVariant

    val total = (h2h.homeWins + h2h.draws + h2h.awayWins).coerceAtLeast(1)
    val homeWeight = (h2h.homeWins.toFloat() / total).coerceAtLeast(0.01f)
    val drawWeight = (h2h.draws.toFloat() / total).coerceAtLeast(0.01f)
    val awayWeight = (h2h.awayWins.toFloat() / total).coerceAtLeast(0.01f)

    Surface(
        shape = UlamaTheme.shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UlamaTheme.spacing.cardPadding)
        ) {
            // Marcadores de balance: Victorias local, Empates, Victorias visitante
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Local
                Column(horizontalAlignment = Alignment.Start) {
                    TeamColorText(
                        text = stringResource(R.string.h2h_wins_format, h2h.homeWins),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = effectiveHomeColor
                    )
                    Text(
                        text = homeTeam.shortName.takeIf { it.isNotBlank() } ?: homeTeam.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Empates
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.h2h_draws_format, h2h.draws),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.cd_form_draw),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Visitante
                Column(horizontalAlignment = Alignment.End) {
                    TeamColorText(
                        text = stringResource(R.string.h2h_wins_format, h2h.awayWins),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = effectiveAwayColor
                    )
                    Text(
                        text = awayTeam.shortName.takeIf { it.isNotBlank() } ?: awayTeam.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

            // Barra proporcional tricromatica (Local / Empates / Visitante)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .weight(homeWeight)
                        .height(6.dp)
                        .background(effectiveHomeColor)
                )
                Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                Box(
                    modifier = Modifier
                        .weight(drawWeight)
                        .height(6.dp)
                        .background(neutralColor)
                )
                Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                Box(
                    modifier = Modifier
                        .weight(awayWeight)
                        .height(6.dp)
                        .background(effectiveAwayColor)
                )
            }

            // Desglose de partidos previos recientes (hasta 3)
            if (h2h.matches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))

                Column(
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.extraSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    h2h.matches.take(3).forEach { match ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = match.homeTeamName.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "${match.homeScore ?: "0"} - ${match.awayScore ?: "0"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = UlamaTheme.spacing.small)
                            )

                            Text(
                                text = match.awayTeamName.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Pie descriptivo estilo tabla de posiciones
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = UlamaTheme.dimensions.borderThin,
                modifier = Modifier.padding(
                    top = if (h2h.matches.isNotEmpty()) UlamaTheme.spacing.small else UlamaTheme.spacing.medium,
                    bottom = UlamaTheme.spacing.extraSmall
                )
            )

            Text(
                text = stringResource(R.string.h2h_footer),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
