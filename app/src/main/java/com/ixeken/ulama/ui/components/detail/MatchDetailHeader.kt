package com.ixeken.ulama.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ixeken.ulama.core.model.FormResult
import com.ixeken.ulama.core.model.TeamFormMatch
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ixeken.ulama.ui.components.localizedSeriesTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.GoalScorer
import com.ixeken.ulama.core.model.Match
import com.ixeken.ulama.core.model.MatchDetail
import com.ixeken.ulama.core.model.MatchState
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.core.model.resolveExtraTimeSuffixRes
import androidx.compose.ui.platform.LocalContext
import com.ixeken.ulama.core.util.DateTimeUtils
import com.ixeken.ulama.ui.components.RollingScoreText
import com.ixeken.ulama.ui.components.bounceClick
import com.ixeken.ulama.ui.components.iconPop
import com.ixeken.ulama.ui.components.livePulse
import com.ixeken.ulama.ui.components.rememberAnimatedShape
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Cabecera del modal de detalle inspirada en Apple Sports.
 * Renderiza los escudos, nombres, records, marcador central con opacidad de perdedor,
 * la fila de anotadores con el balon vectorial central y el boton sobrio de seguimiento.
 */
@Composable
fun MatchDetailHeader(
    match: Match,
    matchDetail: MatchDetail?,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = UlamaTheme.spacing.cardPadding)
            .padding(top = UlamaTheme.spacing.normal, bottom = UlamaTheme.spacing.contentSpacing)
    ) {
        val legText = when (match.legNumber) {
            1 -> stringResource(R.string.match_leg_first)
            2 -> stringResource(R.string.match_leg_second)
            else -> match.legText
        }
        val seriesTitle = match.localizedSeriesTitle()
        val seriesBadgeText = listOfNotNull(seriesTitle, legText).joinToString(" · ")
        if (seriesBadgeText.isNotBlank()) {
            Text(
                text = seriesBadgeText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = UlamaTheme.spacing.small)
            )
        }

        // Fila Principal: Equipo Local, Marcador Central y Equipo Visitante
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderTeamColumn(
                team = match.homeTeam,
                isLoser = match.isHomeLoser,
                recentForm = matchDetail?.homeRecentForm.orEmpty(),
                modifier = Modifier.weight(1f)
            )

            HeaderScoreCenter(
                match = match,
                modifier = Modifier.weight(1.2f)
            )

            HeaderTeamColumn(
                team = match.awayTeam,
                isLoser = match.isAwayLoser,
                recentForm = matchDetail?.awayRecentForm.orEmpty(),
                modifier = Modifier.weight(1f)
            )
        }

        // Fila de Goleadores estilo Apple Sports (con respaldo inmediato desde el marcador si el detalle sigue cargando)
        val homeScorers = matchDetail?.homeScorers?.takeIf { it.isNotEmpty() } ?: match.homeScorers
        val awayScorers = matchDetail?.awayScorers?.takeIf { it.isNotEmpty() } ?: match.awayScorers
        if (homeScorers.isNotEmpty() || awayScorers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
            HeaderScorersRow(homeScorers = homeScorers, awayScorers = awayScorers)
        }

        // Boton Seguir Notificaciones (M3 Expressive): Se oculta en partidos concluidos porque ya no emitiran eventos en vivo
        if (!match.isFinished) {
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))
            HeaderFollowButton(
                isFollowing = isFollowing,
                onFollowToggle = onFollowToggle
            )
        }

        // Metadatos del Encuentro: Estadio, Transmisiones TV y Arbitraje/Asistencia/Clima
        HeaderMetadataSection(
            match = match,
            matchDetail = matchDetail
        )
    }
}

/**
 * Metadatos complementarios del encuentro: sede, difusion televisiva y detalles tecnicos.
 * Se renderizan de forma compacta y semi-transparente para evitar sobrecargar la vista principal.
 */
@Composable
private fun HeaderMetadataSection(
    match: Match,
    matchDetail: MatchDetail?,
    modifier: Modifier = Modifier
) {
    val venue = match.venueName?.takeIf { it.isNotBlank() }
    val broadcasts = matchDetail?.broadcasts.orEmpty()
    val refereeText = matchDetail?.referee?.takeIf { it.isNotBlank() }?.let {
        stringResource(R.string.match_referee_format, it)
    }
    val attendanceText = matchDetail?.attendance?.takeIf { it > 0 }?.let {
        stringResource(R.string.match_attendance_format, "%,d".format(it))
    }
    val weatherText = matchDetail?.weather?.takeIf { it.isNotBlank() }

    val gameInfoDetails = listOfNotNull(refereeText, attendanceText, weatherText).joinToString(" · ")

    val hasAnyMetadata = venue != null || broadcasts.isNotEmpty() || gameInfoDetails.isNotBlank()
    if (!hasAnyMetadata) return

    Spacer(modifier = Modifier.height(UlamaTheme.spacing.contentSpacing))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.extraSmall),
        modifier = modifier.fillMaxWidth()
    ) {
        if (venue != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (broadcasts.isNotEmpty()) {
            Text(
                text = stringResource(R.string.match_broadcasts_label, broadcasts.joinToString(", ")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        if (gameInfoDetails.isNotBlank()) {
            Text(
                text = gameInfoDetails,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeaderTeamColumn(
    team: Team,
    isLoser: Boolean,
    recentForm: List<TeamFormMatch> = emptyList(),
    modifier: Modifier = Modifier
) {
    val nameAlpha = if (isLoser) 0.60f else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AsyncImage(
                model = team.getLogoUrl(UlamaTheme.isDark),
                contentDescription = stringResource(R.string.cd_team_crest, team.name),
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))

        Text(
            text = team.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isLoser) FontWeight.Medium else FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = nameAlpha),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        val recordText = team.record
        if (!recordText.isNullOrBlank()) {
            Text(
                text = recordText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        if (recentForm.isNotEmpty()) {
            Spacer(modifier = Modifier.height(UlamaTheme.spacing.extraSmall))
            TeamRecentFormRow(form = recentForm)
        }
    }
}

/**
 * Fila de racha reciente de partidos (ultimos 5) con formas de Material 3 Expressive:
 * - Victoria: Circulo pleno (Verde Ulama).
 * - Empate: Cuadrado suave redondeado (Gris neutro).
 * - Derrota: Diamante biselado (Rojo Ulama).
 */
@Composable
private fun TeamRecentFormRow(
    form: List<TeamFormMatch>,
    modifier: Modifier = Modifier
) {
    val winText = stringResource(R.string.cd_form_win)
    val drawText = stringResource(R.string.cd_form_draw)
    val lossText = stringResource(R.string.cd_form_loss)

    val formSummary = form.takeLast(5).joinToString(", ") { m ->
        when (m.result) {
            FormResult.WIN -> winText
            FormResult.DRAW -> drawText
            FormResult.LOSS -> lossText
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics {
            contentDescription = formSummary
        }
    ) {
        form.takeLast(5).forEach { match ->
            val (shape, color) = when (match.result) {
                FormResult.WIN -> Pair(CircleShape, Color(0xFF6A994E))
                FormResult.DRAW -> Pair(
                    RoundedCornerShape(2.5.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
                FormResult.LOSS -> Pair(CutCornerShape(50), Color(0xFFBC4749))
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(shape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun HeaderScoreCenter(
    match: Match,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        when (match.status.state) {
            MatchState.FINISHED -> {
                val homeAlpha = if (match.isHomeLoser) 0.38f else 1.0f
                val awayAlpha = if (match.isAwayLoser) 0.38f else 1.0f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RollingScoreText(
                        score = match.homeTeam.score ?: 0,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = homeAlpha)
                    )
                    Text(
                        text = stringResource(R.string.match_score_separator),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    RollingScoreText(
                        score = match.awayTeam.score ?: 0,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = awayAlpha)
                    )
                }

                // Pildora semantica de penales (Opcion 1)
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
                        stringResource(R.string.match_status_finished_penalties)
                    match.status.isFinishedAet ->
                        stringResource(R.string.match_status_finished_aet)
                    else ->
                        stringResource(R.string.match_status_finished)
                }
                Text(
                    text = finishedStatusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val playedDate = DateTimeUtils.formatMatchDate(context, match.dateUtc)
                if (playedDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = playedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            MatchState.LIVE -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RollingScoreText(
                        score = match.homeTeam.score ?: 0,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.match_score_separator),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    RollingScoreText(
                        score = match.awayTeam.score ?: 0,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

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
                val clockText = when {
                    match.isHalftime -> stringResource(R.string.match_status_halftime)
                    match.status.isShootout -> stringResource(R.string.match_status_live_penalties)
                    match.status.isExtraTime -> {
                        val clock = match.status.displayClock
                        val extraSuffix = resolveExtraTimeSuffixRes(clock)?.let { stringResource(it) }.orEmpty()
                        if (clock.isNotBlank()) "$clock$extraSuffix" else stringResource(R.string.match_status_live_extra_time)
                    }
                    match.status.displayClock.isNotBlank() -> match.status.displayClock
                    else -> stringResource(R.string.match_status_live)
                }
                Text(
                    text = clockText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.livePulse(isLive = true)
                )
            }
            MatchState.SCHEDULED -> {
                Text(
                    text = DateTimeUtils.formatMatchTime(context, match.dateUtc),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
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

                val scheduledDate = DateTimeUtils.formatMatchDate(context, match.dateUtc)
                val scheduledSubtitle = scheduledDate.ifBlank { stringResource(R.string.match_status_scheduled) }
                Text(
                    text = scheduledSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun formatGoalScorerMinutes(scorer: GoalScorer): String {
    if (scorer.goals.isEmpty()) return scorer.minutesDisplay
    val ownGoalTag = stringResource(R.string.goal_own_goal_tag)
    val penaltyTag = stringResource(R.string.goal_penalty_tag)
    return scorer.goals.joinToString(", ") { event ->
        when {
            event.isOwnGoal -> "${event.minute} ($ownGoalTag)"
            event.isPenalty -> "${event.minute} ($penaltyTag)"
            else -> event.minute
        }
    }
}

@Composable
private fun HeaderScorersRow(
    homeScorers: List<GoalScorer>,
    awayScorers: List<GoalScorer>,
    modifier: Modifier = Modifier
) {
    val scorerTextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Goleadores del equipo local alineados a la derecha
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            homeScorers.forEach { scorer ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = scorer.playerName,
                        style = scorerTextStyle,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatGoalScorerMinutes(scorer),
                        style = scorerTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Icono de Balon Vectorial Central
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_soccer_ball),
                contentDescription = stringResource(R.string.cd_goal_icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
            )
        }

        // Goleadores del equipo visitante alineados a la izquierda
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            awayScorers.forEach { scorer ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = scorer.playerName,
                        style = scorerTextStyle,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatGoalScorerMinutes(scorer),
                        style = scorerTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderFollowButton(
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedShape = rememberAnimatedShape(
        isPressed = isPressed,
        isSelected = isFollowing,
        restRadius = UlamaTheme.shapes.buttonRestRadius,
        morphedRadius = UlamaTheme.shapes.buttonSelectedRadius
    )

    val containerColorState = animateColorAsState(
        targetValue = if (isFollowing) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "FollowButtonContainerColor"
    )

    val contentColorState = animateColorAsState(
        targetValue = if (isFollowing) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "FollowButtonContentColor"
    )

    val cardBorder = UlamaTheme.borders.cardBorder
    val borderModifier = if (!isFollowing && cardBorder != null) {
        Modifier.border(cardBorder, animatedShape)
    } else {
        Modifier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .bounceClick(enabled = true, interactionSource = interactionSource)
            .clip(animatedShape)
            // Lectura diferida a fase de dibujado para evitar recomposiciones durante la transicion de color
            .drawBehind { drawRect(containerColorState.value) }
            .then(borderModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onFollowToggle
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(if (isFollowing) R.drawable.ic_check else R.drawable.ic_notifications),
                tint = { contentColorState.value },
                contentDescription = stringResource(R.string.cd_follow_match),
                modifier = Modifier
                    .size(18.dp)
                    .iconPop(trigger = isFollowing)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(if (isFollowing) R.string.match_following else R.string.match_follow),
                color = { contentColorState.value },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
