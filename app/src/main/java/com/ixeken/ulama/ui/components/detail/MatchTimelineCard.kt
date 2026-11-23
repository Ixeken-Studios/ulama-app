package com.ixeken.ulama.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ixeken.ulama.R
import com.ixeken.ulama.core.model.MatchTimelineEvent
import com.ixeken.ulama.core.model.Team
import com.ixeken.ulama.core.model.TimelineEventType
import com.ixeken.ulama.ui.components.TeamColorText
import com.ixeken.ulama.ui.components.animateListEntry
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Cronologia de eventos clave del encuentro a ancho completo con codigo de color por club.
 * Cada fila conectada presenta la barra cromatica del equipo, el minuto, el titulo del evento,
 * la abreviatura del club y el detalle completo del futbolista involucrado.
 */
@Composable
fun MatchTimelineCard(
    events: List<MatchTimelineEvent>,
    matchId: String,
    homeTeam: Team,
    awayTeam: Team,
    homeColor: Color,
    awayColor: Color,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) return

    val outerRadius = UlamaTheme.shapes.cardRestRadius
    val innerRadius = UlamaTheme.shapes.cardConnectedInnerRadius
    val cardBorder = UlamaTheme.borders.cardBorder
    val isDark = UlamaTheme.isDark

    val accessibleHomeColor = remember(homeColor, isDark) {
        DetailColorUtils.getAccessibleTeamColor(homeColor, isDark)
    }
    val accessibleAwayColor = remember(awayColor, isDark) {
        DetailColorUtils.getAccessibleTeamColor(awayColor, isDark)
    }

    val homeShort = remember(homeTeam) {
        homeTeam.shortName.takeIf { it.isNotBlank() } ?: homeTeam.name
    }
    val awayShort = remember(awayTeam) {
        awayTeam.shortName.takeIf { it.isNotBlank() } ?: awayTeam.name
    }

    val singleShape = UlamaTheme.shapes.card
    val topShape = remember(outerRadius, innerRadius) {
        RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
    }
    val middleShape = remember(innerRadius) { RoundedCornerShape(innerRadius) }
    val bottomShape = remember(outerRadius, innerRadius) {
        RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.connectedCardSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        events.forEachIndexed { index, event ->
            val rowShape = when {
                events.size == 1 -> singleShape
                index == 0 -> topShape
                index == events.lastIndex -> bottomShape
                else -> middleShape
            }
            val rawTeamColor = if (event.isHomeTeam) homeColor else awayColor
            val barColor = if (event.isHomeTeam) accessibleHomeColor else accessibleAwayColor
            val teamLabel = if (event.isHomeTeam) homeShort else awayShort

            Surface(
                shape = rowShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateListEntry(index = index, key = "${matchId}_ev_${event.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = UlamaTheme.spacing.cardPadding,
                            vertical = UlamaTheme.spacing.medium
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .width(UlamaTheme.spacing.extraSmall)
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )

                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))

                    Text(
                        text = event.minute,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = barColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.widthIn(min = 38.dp)
                    )

                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))

                    TimelineEventBody(
                        event = event,
                        teamLabel = teamLabel,
                        rawTeamColor = rawTeamColor,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))

                    TimelineEventBadge(type = event.type)
                }
            }
        }
    }
}

@Composable
@NonRestartableComposable
private fun TimelineEventBody(
    event: MatchTimelineEvent,
    teamLabel: String,
    rawTeamColor: Color,
    modifier: Modifier = Modifier
) {
    val eventTitle = stringResource(
        when (event.type) {
            TimelineEventType.GOAL -> R.string.event_title_goal
            TimelineEventType.PENALTY_GOAL -> R.string.event_title_penalty_goal
            TimelineEventType.OWN_GOAL -> R.string.event_title_own_goal
            TimelineEventType.YELLOW_CARD -> R.string.event_title_yellow_card
            TimelineEventType.SECOND_YELLOW -> R.string.event_title_second_yellow
            TimelineEventType.RED_CARD -> R.string.event_title_red_card
            TimelineEventType.SUBSTITUTION -> R.string.event_title_substitution
        }
    )

    val detailText = when (event.type) {
        TimelineEventType.GOAL,
        TimelineEventType.PENALTY_GOAL -> {
            val assist = event.secondaryPlayerName?.let {
                stringResource(R.string.event_assist_format, it)
            }
            if (assist != null) "${event.primaryPlayerName} · $assist" else event.primaryPlayerName
        }
        TimelineEventType.SUBSTITUTION -> {
            val outName = event.secondaryPlayerName
            if (!outName.isNullOrBlank()) {
                stringResource(R.string.event_sub_in_out_format, event.primaryPlayerName, outName)
            } else {
                event.primaryPlayerName
            }
        }
        TimelineEventType.OWN_GOAL,
        TimelineEventType.YELLOW_CARD,
        TimelineEventType.SECOND_YELLOW,
        TimelineEventType.RED_CARD -> event.primaryPlayerName
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UlamaTheme.spacing.extraSmall)
        ) {
            Text(
                text = eventTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TeamColorText(
                text = teamLabel,
                color = rawTeamColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = detailText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@NonRestartableComposable
private fun TimelineEventBadge(type: TimelineEventType) {
    when (type) {
        TimelineEventType.GOAL,
        TimelineEventType.PENALTY_GOAL -> Icon(
            painter = painterResource(R.drawable.ic_soccer_ball),
            contentDescription = stringResource(R.string.cd_event_goal),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
        )
        TimelineEventType.OWN_GOAL -> Icon(
            painter = painterResource(R.drawable.ic_soccer_ball),
            contentDescription = stringResource(R.string.cd_event_own_goal),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
        )
        TimelineEventType.SUBSTITUTION -> Icon(
            painter = painterResource(R.drawable.ic_swap_horiz),
            contentDescription = stringResource(R.string.cd_event_substitution),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(UlamaTheme.dimensions.iconMedium)
        )
        TimelineEventType.YELLOW_CARD -> TimelineCardRect(
            color = UlamaTheme.colors.cardYellow,
            shape = UlamaTheme.shapes.disciplineCard
        )
        TimelineEventType.RED_CARD -> TimelineCardRect(
            color = UlamaTheme.colors.cardRed,
            shape = UlamaTheme.shapes.disciplineCard
        )
        TimelineEventType.SECOND_YELLOW -> {
            val w = UlamaTheme.dimensions.disciplineCardWidth
            val h = UlamaTheme.dimensions.disciplineCardHeight
            val offset = UlamaTheme.spacing.extraSmall
            Box(modifier = Modifier.size(width = w + offset, height = h + offset / 2)) {
                TimelineCardRect(
                    color = UlamaTheme.colors.cardYellow,
                    shape = UlamaTheme.shapes.disciplineCard
                )
                TimelineCardRect(
                    color = UlamaTheme.colors.cardRed,
                    shape = UlamaTheme.shapes.disciplineCard,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceContainer, UlamaTheme.shapes.disciplineCard)
                )
            }
        }
    }
}

@Composable
@NonRestartableComposable
private fun TimelineCardRect(
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(
                width = UlamaTheme.dimensions.disciplineCardWidth,
                height = UlamaTheme.dimensions.disciplineCardHeight
            )
            .clip(shape)
            .background(color)
    )
}
