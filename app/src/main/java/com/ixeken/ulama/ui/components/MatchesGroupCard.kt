package com.ixeken.ulama.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ixeken.ulama.core.model.resolveExtraTimeSuffixRes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.League
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.Team
import androidx.compose.ui.platform.LocalContext
import com.ixeken.ulama.core.util.DateTimeUtils
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Contenedor de lista conectada con estetica Material 3 Expressive que agrupa multiples encuentros deportivos.
 *
 * En lugar de recurrir a divisores lineales tradicionales, emplea separacion fisica mediante espacios
 * negativos e individualiza cada fila con esquinas adaptadas segun su posicion en la coleccion:
 * esquinas amplias en los extremos del grupo y sutiles en los enlaces contiguos.
 *
 * @param matches Lista inmutable de encuentros deportivos a agrupar.
 * @param onMatchClick Callback ejecutado al interactuar con un partido individual.
 * @param modifier Modificador base de Compose para el contenedor exterior.
 * @param showLeagueBadge Indica si se debe mostrar el distintivo del torneo en cada partido.
 */
@Composable
fun MatchesGroupCard(
    matches: List<Match>,
    onMatchClick: (Match) -> Unit,
    modifier: Modifier = Modifier,
    showLeagueBadge: Boolean = false
) {
    if (matches.isEmpty()) return

    val restRadius = UlamaTheme.shapes.cardRestRadius
    val innerRadius = UlamaTheme.shapes.cardConnectedInnerRadius
    val singleShape = UlamaTheme.shapes.card
    val topRowShape = remember(restRadius, innerRadius) {
        RoundedCornerShape(
            topStart = restRadius,
            topEnd = restRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
    }
    val bottomRowShape = remember(restRadius, innerRadius) {
        RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = restRadius,
            bottomEnd = restRadius
        )
    }
    val middleRowShape = remember(innerRadius) {
        RoundedCornerShape(innerRadius)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
    ) {
        matches.forEachIndexed { index, match ->
            val rowShape = when {
                matches.size == 1 -> singleShape
                index == 0 -> topRowShape
                index == matches.lastIndex -> bottomRowShape
                else -> middleRowShape
            }

            Surface(
                shape = rowShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = UlamaTheme.borders.cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateListEntry(index = index, key = match.id)
            ) {
                MatchRow(
                    match = match,
                    shape = rowShape,
                    onClick = { onMatchClick(match) },
                    showLeagueBadge = showLeagueBadge
                )
            }
        }
    }
}

/**
 * Fila interactiva para un partido individual dentro del contenedor agrupado.
 * Aplica Clip Before Click garantizando que la ondulacion no se desborde del contorno curvo.
 */
@Composable
private fun MatchRow(
    match: Match,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLeagueBadge: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(
                enabled = true,
                interactionSource = interactionSource
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(UlamaTheme.spacing.cardPadding)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val isDark = UlamaTheme.isDark
            val hasBadge = showLeagueBadge && !match.league.isVirtual
            val seriesTitle = match.localizedSeriesTitle()
            val hasSeriesTitle = !seriesTitle.isNullOrBlank()

            if (hasBadge || hasSeriesTitle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = UlamaTheme.spacing.small),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasBadge) {
                        LeagueCrestImage(
                            logoUrl = match.league.getLogoUrl(isDark),
                            contentDescription = stringResource(match.league.nameRes),
                            modifier = Modifier.size(UlamaTheme.dimensions.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                    }
                    val badgeText = when {
                        hasBadge && hasSeriesTitle -> "${stringResource(match.league.nameRes)} · $seriesTitle"
                        hasBadge -> stringResource(match.league.nameRes)
                        else -> seriesTitle.orEmpty()
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (hasSeriesTitle) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Escenario Central M3: 3 columnas simetricas limpias sin contenedores intermedios
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Equipo Local
                TeamColumn(
                    team = match.homeTeam,
                    isLoser = match.isHomeLoser,
                    modifier = Modifier.weight(1f)
                )

                // Marcador o Cronometro central
                ScoreCenter(
                    match = match,
                    modifier = Modifier.weight(1.2f)
                )

                // Equipo Visitante
                TeamColumn(
                    team = match.awayTeam,
                    isLoser = match.isAwayLoser,
                    modifier = Modifier.weight(1f)
                )
            }

            // Fila Inferior: Sede del encuentro centrada horizontalmente
            val venue = match.venueName
            if (!venue.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(UlamaTheme.spacing.cardBottomRowSpacing))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location_on),
                        contentDescription = stringResource(R.string.cd_venue_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(UlamaTheme.dimensions.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.extraSmall))
                    Text(
                        text = venue,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Columna vertical para renderizar el escudo oficial, nombre y record del club.
 */
@Composable
private fun TeamColumn(
    team: Team,
    isLoser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = UlamaTheme.isDark
    val logoUrl = remember(team.id, isDark) { team.getLogoUrl(isDark) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TeamCrestImage(
            logoUrl = logoUrl,
            teamName = team.name,
            abbreviation = team.abbreviation,
            teamId = team.id,
            contentDescription = stringResource(R.string.cd_team_crest, team.name),
            modifier = Modifier
                .size(UlamaTheme.dimensions.crestNormal)
                .alpha(if (isLoser) 0.8f else 1f)
        )
        Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
        Box(
            modifier = Modifier
                .height(UlamaTheme.dimensions.teamNameBoxHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = team.shortName.ifBlank { team.name },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isLoser) FontWeight.Medium else FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLoser) 0.6f else 1f)
            )
        }
        val teamRecord = team.record
        if (!teamRecord.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
            Text(
                text = teamRecord,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isLoser) 0.45f else 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Nucleo central de puntuacion y estado temporal del juego.
 * Emplea seleccion de ramas con when segun el estandar de condicionales del proyecto.
 */
@Composable
private fun ScoreCenter(
    match: Match,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (match.status.state) {
            MatchState.LIVE -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RollingScoreText(
                        score = match.homeTeam.score ?: 0
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))
                    Text(
                        text = stringResource(R.string.match_score_separator),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))
                    RollingScoreText(
                        score = match.awayTeam.score ?: 0
                    )
                }

                // Pildora de penales en vivo si existen aciertos registrados
                val liveHomeShootout = match.homeTeam.shootoutScore
                val liveAwayShootout = match.awayTeam.shootoutScore
                if (match.status.isShootout && liveHomeShootout != null && liveAwayShootout != null) {
                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.match_status_penalties_score_format,
                                liveHomeShootout,
                                liveAwayShootout
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Pildora semantica de marcador global acumulado en eliminatorias
                if (match.isAggregateActive) {
                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.match_aggregate_pill,
                                match.aggregateHomeScore ?: 0,
                                match.aggregateAwayScore ?: 0
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                val liveClockText = when {
                    match.isHalftime -> stringResource(id = R.string.match_status_halftime)
                    match.status.isShootout -> stringResource(id = R.string.match_status_live_penalties)
                    match.status.isExtraTime -> {
                        val clock = match.status.displayClock
                        val extraSuffix = resolveExtraTimeSuffixRes(clock)?.let { stringResource(it) }.orEmpty()
                        if (clock.isNotBlank()) "$clock$extraSuffix" else stringResource(R.string.match_status_live_extra_time)
                    }
                    match.status.displayClock.isNotBlank() -> match.status.displayClock
                    else -> stringResource(id = R.string.match_status_live)
                }
                Text(
                    text = liveClockText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.livePulse(isLive = true)
                )
            }
            MatchState.FINISHED -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RollingScoreText(
                        score = match.homeTeam.score ?: 0,
                        color = if (match.isHomeLoser) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))
                    Text(
                        text = stringResource(R.string.match_score_separator),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.medium))
                    RollingScoreText(
                        score = match.awayTeam.score ?: 0,
                        color = if (match.isAwayLoser) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // Subtitulo dedicado con pildora semantica para penales bajo el marcador
                val homeShootout = match.homeTeam.shootoutScore
                val awayShootout = match.awayTeam.shootoutScore
                val showShootoutPill = homeShootout != null && awayShootout != null

                if (showShootoutPill) {
                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.match_status_penalties_score_format,
                                homeShootout,
                                awayShootout
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Pildora semantica de marcador global acumulado en eliminatorias
                if (match.isAggregateActive) {
                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.match_aggregate_pill,
                                match.aggregateHomeScore ?: 0,
                                match.aggregateAwayScore ?: 0
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                val finishedStatusText = when {
                    match.status.isFinishedPenalties || showShootoutPill ->
                        stringResource(id = R.string.match_status_finished_penalties)
                    match.status.isFinishedAet ->
                        stringResource(id = R.string.match_status_finished_aet)
                    else ->
                        stringResource(id = R.string.match_status_finished)
                }
                Text(
                    text = finishedStatusText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                val playedDate = remember(match.dateUtc) {
                    DateTimeUtils.formatMatchDate(context, match.dateUtc)
                }
                if (playedDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = playedDate,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            MatchState.SCHEDULED -> {
                val formattedTime = remember(match.dateUtc) {
                    DateTimeUtils.formatMatchTime(context, match.dateUtc)
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // En partidos de vuelta programados, mostrar el marcador global previo si existe
                if (match.isAggregateActive) {
                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.match_aggregate_pill,
                                match.aggregateHomeScore ?: 0,
                                match.aggregateAwayScore ?: 0
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                val dateText = remember(match.dateUtc) {
                    DateTimeUtils.formatMatchDate(context, match.dateUtc)
                }
                if (dateText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "MatchesGroupCard - Dark Theme", showBackground = true, backgroundColor = 0xFF131814)
@Composable
private fun MatchesGroupCardDarkPreview() {
    val sampleMatch1 = Match(
        id = "1",
        dateUtc = "2026-10-06T02:00:00Z",
        homeTeam = Team("1", "Arsenal", "Arsenal", "ARS", null, true, 4, true, "4-0-1"),
        awayTeam = Team("2", "Leeds", "Leeds", "LEE", null, false, 0, false, "2-3-0"),
        status = com.ixeken.ulama.core.model.MatchStatus(MatchState.FINISHED, "FT", "Final", true),
        venueName = "Emirates Stadium"
    )
    val sampleMatch2 = Match(
        id = "2",
        dateUtc = "2026-10-06T18:00:00Z",
        homeTeam = Team("3", "Chelsea", "Chelsea", "CHE", null, true, null, false, "2-1-2"),
        awayTeam = Team("4", "Bournemouth", "Bournemouth", "BOU", null, false, null, false, "0-3-2"),
        status = com.ixeken.ulama.core.model.MatchStatus(MatchState.SCHEDULED, "08:00", "08:00", false),
        venueName = "Stamford Bridge"
    )

    UlamaTheme(darkTheme = true) {
        MatchesGroupCard(
            matches = listOf(sampleMatch1, sampleMatch2),
            onMatchClick = {},
            modifier = Modifier.padding(UlamaTheme.spacing.normal)
        )
    }
}
