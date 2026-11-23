package com.ixeken.ulama.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.ixeken.ulama.core.model.TimelineEventType
import com.ixeken.ulama.ui.components.animateListEntry
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Cronologia de eventos clave del encuentro con tarjetas conectadas Material 3 Expressive.
 * Muestra en un eje temporal central los goles, tarjetas disciplinarias y sustituciones
 * separando visualmente las acciones del equipo local a la izquierda y visitante a la derecha.
 */
@Composable
fun MatchTimelineCard(
    events: List<MatchTimelineEvent>,
    matchId: String,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) return

    val outerRadius = UlamaTheme.shapes.cardRestRadius
    val innerRadius = UlamaTheme.shapes.cardConnectedInnerRadius
    val cardBorder = UlamaTheme.borders.cardBorder

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
                            horizontal = UlamaTheme.spacing.medium,
                            vertical = UlamaTheme.spacing.small
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (event.isHomeTeam) {
                            TimelineSideContent(event = event, alignRight = true)
                        }
                    }

                    Text(
                        text = event.minute,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = UlamaTheme.spacing.small)
                            .widthIn(min = 40.dp)
                    )

                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!event.isHomeTeam) {
                            TimelineSideContent(event = event, alignRight = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@NonRestartableComposable
private fun TimelineSideContent(
    event: MatchTimelineEvent,
    alignRight: Boolean
) {
    val alignment = if (alignRight) Alignment.End else Alignment.Start
    val textAlign = if (alignRight) TextAlign.End else TextAlign.Start

    val title = when (event.type) {
        TimelineEventType.PENALTY_GOAL ->
            "${event.primaryPlayerName} (${stringResource(R.string.goal_penalty_tag)})"
        TimelineEventType.OWN_GOAL ->
            "${event.primaryPlayerName} (${stringResource(R.string.goal_own_goal_tag)})"
        else -> event.primaryPlayerName
    }

    val subtitle = when (event.type) {
        TimelineEventType.GOAL,
        TimelineEventType.PENALTY_GOAL -> event.secondaryPlayerName?.let {
            stringResource(R.string.event_assist_format, it)
        }
        TimelineEventType.SUBSTITUTION -> event.secondaryPlayerName?.let {
            stringResource(R.string.event_sub_out_format, it)
        }
        else -> null
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignRight) Arrangement.End else Arrangement.Start
    ) {
        if (!alignRight) {
            TimelineEventBadge(type = event.type)
            Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))
        }

        Column(horizontalAlignment = alignment) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign
                )
            }
        }

        if (alignRight) {
            Spacer(modifier = Modifier.width(UlamaTheme.spacing.small))
            TimelineEventBadge(type = event.type)
        }
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
            modifier = Modifier.size(UlamaTheme.dimensions.iconSmall)
        )
        TimelineEventType.OWN_GOAL -> Icon(
            painter = painterResource(R.drawable.ic_soccer_ball),
            contentDescription = stringResource(R.string.cd_event_own_goal),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(UlamaTheme.dimensions.iconSmall)
        )
        TimelineEventType.SUBSTITUTION -> Icon(
            painter = painterResource(R.drawable.ic_swap_horiz),
            contentDescription = stringResource(R.string.cd_event_substitution),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(UlamaTheme.dimensions.iconSmall)
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
