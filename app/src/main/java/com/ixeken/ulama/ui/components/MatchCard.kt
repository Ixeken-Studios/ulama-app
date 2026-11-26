package com.ixeken.ulama.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.ixeken.ulama.ui.theme.LocalUlamaMotion
import com.ixeken.ulama.ui.theme.UlamaMotion
import androidx.compose.ui.platform.LocalContext
import com.ixeken.ulama.core.util.DateTimeUtils
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Componente principal de tarjeta de partido disenado bajo Material 3 Expressive.
 * Se aplican animaciones de resorte reactivo en la geometria del contenedor mediante
 * rememberAnimatedShape y se garantiza la delimitacion estricta de efectos tactiles aplicando
 * Modifier.clip antes de Modifier.clickable segun la regla Clip Before Click.
 * Todos los colores, formas, espaciados y dimensiones se resuelven contra tokens de 3 niveles.
 *
 * @param match Modelo inmutable del encuentro deportivo a renderizar.
 * @param onClick Accion al interactuar con la tarjeta para abrir el detalle o estadisticas.
 * @param modifier Modificador base de Compose para el contenedor exterior.
 * @param showLeagueBadge Indica si se debe mostrar el distintivo del torneo en la tarjeta.
 * @param motion Configuracion activa del sistema de movimiento, permitiendo alternar entre Expressive y Standard.
 */
@Composable
fun MatchCard(
    match: Match,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLeagueBadge: Boolean = false,
    motion: UlamaMotion = LocalUlamaMotion.current
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Regla 11: Geometria que reacciona a la pulsacion tactil segun la fisica configurada
    val cardShape = rememberAnimatedShape(
        isPressed = isPressed,
        restRadius = UlamaTheme.shapes.cardRestRadius,
        morphedRadius = UlamaTheme.shapes.cardPressedRadius,
        motion = motion
    )

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = UlamaTheme.borders.cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = UlamaTheme.dimensions.maxContentWidth)
            .bounceClick(
                enabled = true,
                interactionSource = interactionSource,
                motion = motion
            )
            // Regla 10: Aplicar clip antes de clickable para contener ondulaciones y eventos tactiles
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UlamaTheme.spacing.cardPadding)
        ) {
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
                        color = if (hasSeriesTitle && !hasBadge) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
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
                        tint = MaterialTheme.colorScheme.secondary,
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

@Preview(name = "MatchCard - Dark Theme", showBackground = true, backgroundColor = 0xFF131814)
@Composable
private fun MatchCardDarkPreview() {
    val sampleMatch = Match(
        id = "1",
        dateUtc = "2026-10-06T02:00:00Z",
        homeTeam = Team(
            id = "1",
            name = "América",
            shortName = "América",
            abbreviation = "AME",
            logoUrl = null,
            isHome = true,
            score = 2,
            isWinner = true,
            record = "6-2-1"
        ),
        awayTeam = Team(
            id = "2",
            name = "Guadalajara",
            shortName = "Chivas",
            abbreviation = "CHI",
            logoUrl = null,
            isHome = false,
            score = 1,
            isWinner = false,
            record = "5-3-2"
        ),
        status = com.ixeken.ulama.core.model.MatchStatus(
            state = MatchState.FINISHED,
            displayClock = "FT",
            detail = "Final",
            isCompleted = true
        ),
        venueName = "Estadio Azteca"
    )

    UlamaTheme(darkTheme = true) {
        MatchCard(
            match = sampleMatch,
            onClick = {},
            modifier = Modifier.padding(UlamaTheme.spacing.normal)
        )
    }
}

/**
 * Traduce y formatea los identificadores de etapa y torneo de ESPN hacia recursos oficiales multiidioma.
 * Desacopla la vista de los slugs tecnicos de la API garantizando simetria entre ingles y espanol.
 */
@Composable
fun Match.localizedSeriesTitle(): String? {
    val title = seriesTitle?.trim() ?: return null
    val lower = title.lowercase()
    val isExcludedKnockout = lower.contains("prelim") ||
        lower.contains("play-in") ||
        lower.contains("play in") ||
        lower.contains("relegation") ||
        lower.contains("playoff")
    if (!isExcludedKnockout) {
        when {
            lower == "wild-card" || lower == "wild card" ->
                return stringResource(R.string.round_wild_card)
            lower == "round-one" || lower == "round one" ->
                return stringResource(R.string.round_first_round)
            lower == "elimination-finals" || lower == "elimination finals" ->
                return stringResource(R.string.round_elimination_finals)
            lower == "conference-semifinals" ->
                return stringResource(R.string.round_conference_semifinals)
            lower == "conference-finals" ->
                return stringResource(R.string.round_conference_finals)
            lower == "mls-cup" || lower == "mls cup" ->
                return stringResource(R.string.round_mls_cup)
            lower == "grand-final" || lower == "grand final" ->
                return stringResource(R.string.round_grand_final)
            lower.contains("3rd") || lower.contains("third") || lower.contains("tercer") || lower.contains("3er") ->
                return stringResource(R.string.round_third_place)
            lower.contains("round-of-32") || lower.contains("round of 32") || lower.contains("dieciseis") ->
                return stringResource(R.string.round_round_of_32)
            lower.contains("round-of-16") || lower.contains("round of 16") || lower.contains("octav") ->
                return stringResource(R.string.round_round_of_16)
            lower.contains("quarter") || lower.contains("cuart") ->
                return stringResource(R.string.round_quarterfinals)
            lower.contains("semi") ->
                return stringResource(R.string.round_semifinals)
            lower.contains("final") ->
                return stringResource(R.string.round_final)
        }
    }
    return when {
        lower == "torneo-apertura" || lower == "apertura" || lower == "torneo apertura" ->
            stringResource(R.string.tournament_apertura)
        lower == "torneo-clausura" || lower == "clausura" || lower == "torneo clausura" ->
            stringResource(R.string.tournament_clausura)
        lower == "first stage" || lower == "first-stage" ->
            stringResource(R.string.round_first_stage)
        lower == "second stage" || lower == "second-stage" ->
            stringResource(R.string.round_second_stage)
        lower == "third stage" || lower == "third-stage" ->
            stringResource(R.string.round_third_stage)
        lower == "playoff series" ->
            stringResource(R.string.round_first_round)
        lower.contains("knockout") && lower.contains("playoff") ->
            stringResource(R.string.round_knockout_playoffs)
        else -> title
    }
}

