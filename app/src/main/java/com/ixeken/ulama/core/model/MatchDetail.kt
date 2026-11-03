package com.ixeken.ulama.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.R

/**
 * Pestañas disponibles en el modal de detalle de partido.
 * Cada una expone su identificador de recurso de cadena para garantizar cero cadenas hardcodeadas.
 */
enum class MatchDetailTab(@field:StringRes val titleRes: Int) {
    STATS(R.string.tab_stats),
    LINEUPS(R.string.tab_lineups),
    TABLE(R.string.tab_table)
}

/**
 * Modelo de dominio inmutable que representa el desglose completo de un partido.
 * Contiene anotadores simetricos para el encabezado de Apple Sports, comparativas estadisticas
 * y alineaciones tacticas mapeadas por lineas de campo.
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
    val isCompleted: Boolean = false
)

/**
 * Anotador individual con minutos agrupados en una sola cadena.
 * Si un jugador anota multiples goles, se agrupan como "34', 88'" respetando el patron de Apple Sports.
 */
@Immutable
data class GoalScorer(
    val playerName: String,
    val minutesDisplay: String
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
)

/**
 * Jugador dentro de la plantilla con su dorsal, rol posicional y clasificacion por linea tactica.
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
    val subbedInForName: String? = null,
    val subbedInForJersey: String? = null
)

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
