package com.ixeken.ulama.core.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.R

/**
 * Pestañas disponibles en el modal de detalle de partido.
 * Cada una expone su recurso de cadena y su vector nativo Material Symbols Rounded Fill 1.
 */
enum class MatchDetailTab(
    @field:StringRes val titleRes: Int,
    @field:DrawableRes val iconRes: Int
) {
    STATS(R.string.tab_stats, R.drawable.ic_bar_chart),
    LINEUPS(R.string.tab_lineups, R.drawable.ic_groups),
    EVENTS(R.string.tab_events, R.drawable.ic_timeline),
    TABLE(R.string.tab_table, R.drawable.ic_leaderboard)
}

/**
 * Modelo de dominio inmutable que representa el desglose completo de un partido.
 * Contiene anotadores simetricos para el encabezado de Apple Sports, comparativas estadisticas,
 * cronologia de eventos clave y alineaciones tacticas mapeadas por lineas de campo.
 */
@Immutable
data class MatchDetail(
    val matchId: String,
    val homeScorers: List<GoalScorer> = emptyList(),
    val awayScorers: List<GoalScorer> = emptyList(),
    val stats: List<MatchStatComparison> = emptyList(),
    val homeLineup: TeamLineup? = null,
    val awayLineup: TeamLineup? = null,
    val homeColor: String? = null,
    val awayColor: String? = null,
    val homeAlternateColor: String? = null,
    val awayAlternateColor: String? = null,
    val broadcasts: List<String> = emptyList(),
    val attendance: Int? = null,
    val referee: String? = null,
    val weather: String? = null,
    val homeRecentForm: List<TeamFormMatch> = emptyList(),
    val awayRecentForm: List<TeamFormMatch> = emptyList(),
    val headToHead: HeadToHeadSummary? = null,
    val leaders: List<MatchLeaderCategory> = emptyList(),
    val timelineEvents: List<MatchTimelineEvent> = emptyList(),
    val isCompleted: Boolean = false
)

/**
 * Evento individual de gol para desacoplar el formateo localizado de etiquetas en UI.
 */
@Immutable
data class GoalEvent(
    val minute: String,
    val isOwnGoal: Boolean = false,
    val isPenalty: Boolean = false
)

/**
 * Anotador individual con minutos agrupados en una sola cadena.
 * Si un jugador anota multiples goles, se agrupan como "34', 88'" respetando el patron de Apple Sports.
 */
@Immutable
data class GoalScorer(
    val playerName: String,
    val minutesDisplay: String,
    val goals: List<GoalEvent> = emptyList()
)

/**
 * Comparativa metrica entre ambos equipos con valores formateados y ratios normalizados para barras visuales.
 */
@Immutable
data class MatchStatComparison(
    @field:StringRes val labelRes: Int,
    val homeDisplayValue: String,
    val awayDisplayValue: String,
    val homeRatio: Float,
    val awayRatio: Float
)

/**
 * Formacion tactica y lista de jugadores titulares y suplentes de un club.
 */
@Immutable
data class TeamLineup(
    val teamId: String,
    val teamName: String,
    val shortName: String = "",
    val formation: String,
    val teamColor: String? = null,
    val teamAlternateColor: String? = null,
    val starters: List<LineupPlayer> = emptyList(),
    val bench: List<LineupPlayer> = emptyList()
) {
    /** Indica si existen sustituciones ejecutadas para habilitar la vista de alineacion final. */
    val hasSubstitutions: Boolean
        get() = bench.any { it.subbedIn } || starters.any { it.subbedOut }

    /**
     * Proyecta el once final en cancha intercambiando cada titular sustituido por su reemplazo
     * directo o en cadena y preservando el orden tactico original asignado al inicio.
     */
    fun resolveFinalStarters(): List<LineupPlayer> {
        if (!hasSubstitutions) return starters

        val subbedInPlayers = bench.filter { it.subbedIn }
        val replacementByOutId = mutableMapOf<String, LineupPlayer>()
        val replacementByOutName = mutableMapOf<String, LineupPlayer>()
        subbedInPlayers.forEach { sub ->
            sub.subbedInForId?.takeIf { it.isNotBlank() }?.let { replacementByOutId[it] = sub }
            sub.subbedInForName?.takeIf { it.isNotBlank() }?.let { replacementByOutName[it.lowercase()] = sub }
        }

        return starters.map { starter ->
            var current = starter
            val visitedIds = mutableSetOf<String>()
            while (current.subbedOut) {
                if (current.id.isNotBlank() && !visitedIds.add(current.id)) break
                val next = replacementByOutId[current.id]
                    ?: replacementByOutName[current.name.lowercase()]
                    ?: break
                current = next.copy(
                    formationPlace = starter.formationPlace,
                    line = starter.line,
                    position = starter.position
                )
            }
            current
        }
    }
}

/**
 * Jugador dentro de la plantilla con su dorsal, rol posicional, tarjetas, goles y cambios.
 */
@Immutable
data class LineupPlayer(
    val id: String,
    val name: String,
    val jersey: String,
    val position: String,
    val formationPlace: Int,
    val line: TacticalLine,
    val subbedIn: Boolean = false,
    val subInMinute: String? = null,
    val subbedInForId: String? = null,
    val subbedInForName: String? = null,
    val subbedInForJersey: String? = null,
    val subbedOut: Boolean = false,
    val subOutMinute: String? = null,
    val subbedOutForId: String? = null,
    val subbedOutForName: String? = null,
    val goalsCount: Int = 0,
    val assistsCount: Int = 0,
    val yellowCards: Int = 0,
    val hasRedCard: Boolean = false
) {
    /** Indica expulsion por roja directa o doble amarilla para atenuar el dorsal en cancha. */
    val isSentOff: Boolean
        get() = hasRedCard || yellowCards >= 2
}

/**
 * Linea tactica en el terreno de juego para ubicar al jugador en las filas de la cancha interactiva.
 */
enum class TacticalLine {
    GOALKEEPER,
    DEFENSE,
    MIDFIELD,
    FORWARD
}

/**
 * Resultado individual de partido para la racha de forma reciente.
 */
enum class FormResult {
    WIN,
    DRAW,
    LOSS
}

/**
 * Encuentro previo individual dentro de la racha de los ultimos partidos.
 */
@Immutable
data class TeamFormMatch(
    val result: FormResult,
    val score: String? = null,
    val opponentName: String? = null,
    val dateUtc: String? = null
)

/**
 * Resumen del historial cara a cara (Head-to-Head) entre dos clubes.
 */
@Immutable
data class HeadToHeadSummary(
    val summary: String? = null,
    val homeWins: Int = 0,
    val draws: Int = 0,
    val awayWins: Int = 0,
    val matches: List<HeadToHeadMatch> = emptyList()
)

/**
 * Encuentro directo previo entre los dos contrincantes.
 */
@Immutable
data class HeadToHeadMatch(
    val dateUtc: String? = null,
    val homeScore: String? = null,
    val awayScore: String? = null,
    val homeTeamName: String? = null,
    val awayTeamName: String? = null
)

/**
 * Lider individual de un club en una metrica del partido (ej. tiros, pases, atajadas).
 */
@Immutable
data class MatchPlayerLeader(
    val athleteName: String,
    val jersey: String? = null,
    val valueDisplay: String,
    val statSummary: String? = null
)

/**
 * Categoria comparativa de lideres individuales del partido entre ambos contendientes.
 */
@Immutable
data class MatchLeaderCategory(
    val categoryKey: String,
    val displayNameFallback: String = "",
    val homeLeader: MatchPlayerLeader? = null,
    val awayLeader: MatchPlayerLeader? = null
)

/**
 * Tipo de evento disciplinario, anotacion o sustitucion mostrado en la cronologia del encuentro.
 */
@Immutable
enum class TimelineEventType {
    GOAL,
    OWN_GOAL,
    PENALTY_GOAL,
    YELLOW_CARD,
    SECOND_YELLOW,
    RED_CARD,
    SUBSTITUTION
}

/**
 * Evento cronologico individual del partido para la pestana Eventos.
 */
@Immutable
data class MatchTimelineEvent(
    val id: String,
    val minute: String,
    val sortMinute: Int,
    val type: TimelineEventType,
    val isHomeTeam: Boolean,
    val primaryPlayerName: String,
    val secondaryPlayerName: String? = null
)
