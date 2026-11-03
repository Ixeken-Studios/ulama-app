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
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.core.model.MatchStatComparison
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Vista de comparativa estadistica de equipo estilo Apple Sports.
 * Renderiza pares de valores con el nombre de la metrica al centro y barras horizontales
 * proporcionadas utilizando tokens semanticos de nivel 3 (primary y tertiary).
 */
@Composable
fun MatchStatsView(
    stats: List<MatchStatComparison>,
    homeColor: Color? = null,
    awayColor: Color? = null,
    modifier: Modifier = Modifier
) {
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
            stats.forEachIndexed { index, stat ->
                StatComparisonRow(
                    stat = stat,
                    homeColor = effectiveHomeColor,
                    awayColor = effectiveAwayColor
                )
                if (index < stats.lastIndex) {
                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
                }
            }
        }
    }
}

@Composable
private fun StatComparisonRow(
    stat: MatchStatComparison,
    homeColor: Color,
    awayColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Fila de Valores y Etiqueta Central
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stat.homeDisplayValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = stringResource(stat.labelRes),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(2f)
            )

            Text(
                text = stat.awayDisplayValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

        // Barra de Proporcion Comparativa con colores de club
        val isHomeZero = stat.homeRatio <= 0.0001f
        val isAwayZero = stat.awayRatio <= 0.0001f
        val isBothZero = isHomeZero && isAwayZero
        val isTied = kotlin.math.abs(stat.homeRatio - stat.awayRatio) < 0.0001f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            when {
                isBothZero || isTied -> {
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(6.dp)
                            .background(homeColor)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(6.dp)
                            .background(awayColor)
                    )
                }
                isAwayZero -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(homeColor)
                    )
                }
                isHomeZero -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(awayColor)
                    )
                }
                else -> {
                    val totalRatio = stat.homeRatio + stat.awayRatio
                    val homeWeight = stat.homeRatio / totalRatio
                    val awayWeight = stat.awayRatio / totalRatio

                    Box(
                        modifier = Modifier
                            .weight(homeWeight)
                            .height(6.dp)
                            .background(homeColor)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                    Box(
                        modifier = Modifier
                            .weight(awayWeight)
                            .height(6.dp)
                            .background(awayColor)
                    )
                }
            }
        }
    }
}
