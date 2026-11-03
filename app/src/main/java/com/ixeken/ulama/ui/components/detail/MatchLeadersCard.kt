package com.ixeken.ulama.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.MatchLeaderCategory
import com.ixeken.ulama.core.model.MatchPlayerLeader
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.ui.components.TeamColorText
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Tarjeta de líderes individuales del partido estilo Material 3 Expressive.
 * Proyecta la comparativa cara a cara de los futbolistas más destacados en métricas
 * ofensivas, asociativas y defensivas reportadas por ESPN (tiros, pases, defensa y atajadas).
 */
@Composable
fun MatchLeadersCard(
    leaders: List<MatchLeaderCategory>,
    homeTeam: Team,
    awayTeam: Team,
    homeColor: Color? = null,
    awayColor: Color? = null,
    modifier: Modifier = Modifier
) {
    if (leaders.isEmpty()) return

    val effectiveHomeColor = homeColor ?: MaterialTheme.colorScheme.primary
    val effectiveAwayColor = awayColor ?: MaterialTheme.colorScheme.tertiary

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
            // Título de la tarjeta y nombres cortos de contendientes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamColorText(
                    text = homeTeam.shortName.takeIf { it.isNotBlank() } ?: homeTeam.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = effectiveHomeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = stringResource(R.string.match_leaders_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = UlamaTheme.spacing.small)
                )

                TeamColorText(
                    text = awayTeam.shortName.takeIf { it.isNotBlank() } ?: awayTeam.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = effectiveAwayColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(UlamaTheme.spacing.small))

            // Desglose por categoría
            leaders.forEachIndexed { index, category ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = UlamaTheme.dimensions.borderThin,
                        modifier = Modifier.padding(vertical = UlamaTheme.spacing.small)
                    )
                }

                val categoryLabel = when (category.categoryKey) {
                    "totalShots" -> stringResource(R.string.leader_category_shots)
                    "accuratePasses" -> stringResource(R.string.leader_category_passes)
                    "defensiveInterventions" -> stringResource(R.string.leader_category_defense)
                    "saves" -> stringResource(R.string.leader_category_saves)
                    else -> category.displayNameFallback.ifBlank { category.categoryKey }
                }

                LeaderRow(
                    categoryLabel = categoryLabel,
                    homeLeader = category.homeLeader,
                    awayLeader = category.awayLeader,
                    homeColor = effectiveHomeColor,
                    awayColor = effectiveAwayColor
                )
            }
        }
    }
}

/**
 * Fila comparativa simétrica para una métrica individual de líderes.
 */
@Composable
private fun LeaderRow(
    categoryLabel: String,
    homeLeader: MatchPlayerLeader?,
    awayLeader: MatchPlayerLeader?,
    homeColor: Color,
    awayColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna local
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            if (homeLeader != null) {
                Text(
                    text = homeLeader.athleteName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamColorText(
                        text = homeLeader.valueDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = homeColor
                    )
                    if (!homeLeader.jersey.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "#${homeLeader.jersey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                }
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Píldora central de categoría
        Surface(
            shape = UlamaTheme.shapes.chip,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        // Columna visitante
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            if (awayLeader != null) {
                Text(
                    text = awayLeader.athleteName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!awayLeader.jersey.isNullOrBlank()) {
                        Text(
                            text = "#${awayLeader.jersey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    TeamColorText(
                        text = awayLeader.valueDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = awayColor,
                        textAlign = TextAlign.End
                    )
                }
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
