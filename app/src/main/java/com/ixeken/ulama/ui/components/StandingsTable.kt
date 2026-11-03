package com.ixeken.ulama.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.CutoffLineStyle
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.StandingEntry
import com.ixeken.ulama.core.model.TableCutoff
import com.ixeken.ulama.core.model.TableLegendEntry
import com.ixeken.ulama.core.model.TableZoneType
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Tabla general de clasificacion inspirada en la arquitectura limpia de Apple Sports.
 * Organiza las estadisticas clave sin desplazamiento horizontal y delimita las zonas
 * de clasificacion y descenso de manera modular mediante reglas de corte inyectadas.
 * Soporta resaltado contextual de equipos rivales cuando se consulta desde un partido especifico.
 *
 * @param standings Lista inmutable de clubes en la clasificacion.
 * @param title Titulo personalizado para la tarjeta (ej. "Grupo A"), o null para mostrar "Tabla".
 * @param cutoffs Reglas de division visual para clasificacion directa, eliminatoria o descenso.
 * @param legend Lista de descripciones explicativas de zonas para el pie de la tabla.
 * @param highlightedTeamIds Identificadores o nombres de clubes a destacar visualmente.
 * @param modifier Modificador base de Compose para el contenedor exterior.
 */
@Composable
fun StandingsTable(
    standings: List<StandingEntry>,
    title: String? = null,
    cutoffs: List<TableCutoff> = emptyList(),
    legend: List<TableLegendEntry> = emptyList(),
    highlightedTeamIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    if (standings.isEmpty()) return

    Surface(
        shape = UlamaTheme.shapes.card,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = UlamaTheme.spacing.tablePaddingHorizontal,
                    vertical = UlamaTheme.spacing.tablePaddingVertical
                )
        ) {
            // Titulo centrado sobrio
            Text(
                text = title ?: stringResource(id = R.string.standings_table_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(UlamaTheme.spacing.tableHeaderMarginBottom))

            // Fila de encabezados de columnas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = UlamaTheme.spacing.tableRowPaddingHorizontal,
                        vertical = UlamaTheme.spacing.tableHeaderPaddingVertical
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nombre de la seccion de clubes alineada con los escudos
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(UlamaTheme.dimensions.tableRankWidth))
                    Text(
                        text = stringResource(id = R.string.table_col_team),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Cabeceras de columnas numericas
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    HeaderCell(
                        text = stringResource(id = R.string.table_col_gp),
                        width = UlamaTheme.dimensions.tableGpWidth
                    )
                    HeaderCell(
                        text = stringResource(id = R.string.table_col_w),
                        width = UlamaTheme.dimensions.tableStatWidth
                    )
                    HeaderCell(
                        text = stringResource(id = R.string.table_col_d),
                        width = UlamaTheme.dimensions.tableStatWidth
                    )
                    HeaderCell(
                        text = stringResource(id = R.string.table_col_l),
                        width = UlamaTheme.dimensions.tableStatWidth
                    )
                    HeaderCell(
                        text = stringResource(id = R.string.table_col_gd),
                        width = UlamaTheme.dimensions.tableGdWidth
                    )
                    HeaderCell(
                        text = stringResource(id = R.string.table_col_pts),
                        width = UlamaTheme.dimensions.tablePtsWidth,
                        isPrimary = true
                    )
                }
            }

            val isDark = UlamaTheme.isDark
            val normalizedHighlights = remember(highlightedTeamIds) {
                if (highlightedTeamIds.isEmpty()) emptySet()
                else highlightedTeamIds.mapNotNull { it.trim().lowercase().takeIf { s -> s.isNotBlank() } }.toSet()
            }
            val cutoffsByRank = remember(cutoffs) {
                if (cutoffs.isEmpty()) emptyMap()
                else cutoffs.associateBy { it.rankAfter }
            }

            // Filas de clasificacion con delimitacion modular de zonas
            standings.forEach { entry ->
                val isHighlighted = remember(entry.teamId, normalizedHighlights) {
                    isTeamHighlighted(entry, normalizedHighlights)
                }
                StandingRow(
                    entry = entry,
                    isHighlighted = isHighlighted,
                    isDark = isDark
                )

                val matchingCutoff = cutoffsByRank[entry.rank]
                if (matchingCutoff != null) {
                    val dividerColor = when (matchingCutoff.zoneType) {
                        TableZoneType.RELEGATION -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                        TableZoneType.QUALIFICATION, TableZoneType.PLAYOFF -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    }

                    when (matchingCutoff.lineStyle) {
                        CutoffLineStyle.DASHED -> {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(UlamaTheme.dimensions.borderThin)
                                    .padding(
                                        horizontal = UlamaTheme.spacing.tableRowPaddingHorizontal,
                                        vertical = UlamaTheme.spacing.tableDividerPaddingVertical
                                    )
                            ) {
                                drawLine(
                                    color = dividerColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = size.height,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                )
                            }
                        }
                        CutoffLineStyle.SOLID -> {
                            HorizontalDivider(
                                color = dividerColor,
                                thickness = UlamaTheme.dimensions.borderThin,
                                modifier = Modifier.padding(
                                    horizontal = UlamaTheme.spacing.tableRowPaddingHorizontal,
                                    vertical = UlamaTheme.spacing.tableDividerPaddingVertical
                                )
                            )
                        }
                    }
                }
            }

            // Separador y leyenda descriptiva al pie de la tabla
            if (legend.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = UlamaTheme.dimensions.borderThin,
                    modifier = Modifier.padding(
                        horizontal = UlamaTheme.spacing.tableRowPaddingHorizontal,
                        vertical = UlamaTheme.spacing.small
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = UlamaTheme.spacing.tableRowPaddingHorizontal,
                            vertical = UlamaTheme.spacing.extraSmall
                        ),
                    verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.extraSmall)
                ) {
                    legend.forEach { item ->
                        Text(
                            text = stringResource(R.string.table_legend_format, item.rangeText, stringResource(item.labelRes)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila individual con la posicion, escudo, nombre y estadisticas de un equipo.
 * Si el club coincide con los participantes del partido activo, se resalta visualmente
 * mediante un fondo primaryContainer suave, contorno perimetral sutil y tipografia destacada.
 */
@Composable
private fun StandingRow(
    entry: StandingEntry,
    isHighlighted: Boolean = false,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val logoUrl = remember(entry.teamId, isDark) { entry.getLogoUrl(isDark) }
    val shape = MaterialTheme.shapes.small
    val highlightModifier = if (isHighlighted) {
        Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), shape)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(highlightModifier)
            .padding(
                horizontal = UlamaTheme.spacing.tableRowPaddingHorizontal,
                vertical = if (isHighlighted) 6.dp else UlamaTheme.spacing.tableRowPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna de club: posicion, escudo y nombre corto
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.rank.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                },
                modifier = Modifier.width(UlamaTheme.dimensions.tableRankWidth)
            )
            AsyncImage(
                model = logoUrl,
                contentDescription = stringResource(R.string.cd_team_crest, entry.teamName),
                modifier = Modifier.size(UlamaTheme.dimensions.crestSmall)
            )
            Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))
            Text(
                text = entry.teamShortName.ifBlank { entry.teamName },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        // Columnas de metricas numericas
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            StatCell(
                text = entry.gamesPlayed.toString(),
                width = UlamaTheme.dimensions.tableGpWidth
            )
            StatCell(
                text = entry.wins.toString(),
                width = UlamaTheme.dimensions.tableStatWidth
            )
            StatCell(
                text = entry.draws.toString(),
                width = UlamaTheme.dimensions.tableStatWidth
            )
            StatCell(
                text = entry.losses.toString(),
                width = UlamaTheme.dimensions.tableStatWidth
            )
            StatCell(
                text = formatGoalDiff(entry.goalDifference),
                width = UlamaTheme.dimensions.tableGdWidth,
                isSubtle = true
            )
            StatCell(
                text = entry.points.toString(),
                width = UlamaTheme.dimensions.tablePtsWidth,
                isBold = true,
                isPrimary = isHighlighted
            )
        }
    }
}

/**
 * Determina si una entrada de la tabla pertenece a alguno de los clubes activos en el partido abierto.
 */
private fun isTeamHighlighted(entry: StandingEntry, normalizedHighlights: Set<String>): Boolean {
    if (normalizedHighlights.isEmpty()) return false
    if (entry.teamId.isNotBlank() && entry.teamId.lowercase() in normalizedHighlights) return true
    return entry.teamName.lowercase() in normalizedHighlights ||
           entry.teamShortName.lowercase() in normalizedHighlights ||
           entry.teamAbbreviation.lowercase() in normalizedHighlights
}

@Composable
private fun HeaderCell(
    text: String,
    width: Dp,
    isPrimary: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.SemiBold,
        color = if (isPrimary) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun StatCell(
    text: String,
    width: Dp,
    isBold: Boolean = false,
    isSubtle: Boolean = false,
    isPrimary: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
        color = when {
            isPrimary -> MaterialTheme.colorScheme.primary
            isBold -> MaterialTheme.colorScheme.onSurface
            isSubtle -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        },
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun formatGoalDiff(diff: Int): String {
    return if (diff > 0) {
        stringResource(R.string.goal_diff_positive_format, diff)
    } else {
        stringResource(R.string.goal_diff_format, diff)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF131814)
@Composable
private fun StandingsTablePreview() {
    val sampleStandings = listOf(
        StandingEntry(1, "Toluca", "Toluca", "TOL", null, 10, 6, 2, 2, 22, 11, 11, 20),
        StandingEntry(2, "América", "América", "AME", null, 9, 6, 2, 1, 19, 8, 11, 20),
        StandingEntry(3, "Chivas", "Chivas", "CHI", null, 10, 5, 3, 2, 16, 9, 7, 18),
        StandingEntry(6, "Tigres", "Tigres", "TIG", null, 10, 4, 3, 3, 15, 12, 3, 15),
        StandingEntry(10, "Monterrey", "Monterrey", "MTY", null, 9, 4, 1, 4, 15, 13, 2, 13),
        StandingEntry(17, "Bournemouth", "Bournemouth", "BOU", null, 5, 0, 3, 2, 3, 5, -2, 3),
        StandingEntry(18, "Coventry", "Coventry", "COV", null, 5, 1, 0, 4, 4, 13, -9, 3),
        StandingEntry(19, "Fulham", "Fulham", "FUL", null, 5, 0, 2, 3, 3, 6, -3, 2),
        StandingEntry(20, "Spurs", "Spurs", "TOT", null, 5, 0, 2, 3, 2, 8, -6, 2)
    )

    UlamaTheme(darkTheme = true) {
        StandingsTable(
            standings = sampleStandings,
            cutoffs = League.PREMIER_LEAGUE.tableCutoffs,
            legend = League.PREMIER_LEAGUE.tableLegend,
            modifier = Modifier.padding(UlamaTheme.spacing.normal)
        )
    }
}
