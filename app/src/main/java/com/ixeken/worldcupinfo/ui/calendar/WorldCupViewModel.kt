package com.ixeken.worldcupinfo.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import com.ixeken.worldcupinfo.R
import com.ixeken.worldcupinfo.domain.model.Match
import com.ixeken.worldcupinfo.domain.model.MatchStage
import com.ixeken.worldcupinfo.domain.model.MatchStatus
import com.ixeken.worldcupinfo.domain.model.Prediction
import com.ixeken.worldcupinfo.domain.repository.MatchRepository
import com.ixeken.worldcupinfo.domain.usecase.GetMatchesUseCase
import com.ixeken.worldcupinfo.domain.usecase.SavePredictionUseCase
import com.ixeken.worldcupinfo.domain.usecase.GetPredictionsUseCase
import com.ixeken.worldcupinfo.notification.alarm.MatchAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * ViewModel principal de la aplicación que expone el estado global del torneo y procesa las acciones del usuario (UDF).
 */
@HiltViewModel
class WorldCupViewModel @Inject constructor(
    private val getMatchesUseCase: GetMatchesUseCase,
    private val getPredictionsUseCase: GetPredictionsUseCase,
    private val savePredictionUseCase: SavePredictionUseCase,
    private val matchRepository: MatchRepository,
    private val alarmScheduler: MatchAlarmScheduler,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("worldcupinfo_prefs", Context.MODE_PRIVATE)

    // Filtros de la cabecera superior
    private val _showAlarmsOnly = MutableStateFlow(false)
    val showAlarmsOnly = _showAlarmsOnly.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly = _showFavoritesOnly.asStateFlow()

    // Equipos favoritos persistidos
    private val _favoriteTeams = MutableStateFlow<Set<String>>(loadFavorites())
    val favoriteTeams = _favoriteTeams.asStateFlow()

    // Minutos de aviso previo (predeterminado: 5)
    private val _alarmMinutes = MutableStateFlow(loadAlarmMinutes())
    val alarmMinutes = _alarmMinutes.asStateFlow()

    // Marcadores en vivo desde ESPN (Desactivado por defecto)
    private val _isLiveScoresEnabled = MutableStateFlow(loadLiveScoresEnabled())
    val isLiveScoresEnabled = _isLiveScoresEnabled.asStateFlow()

    // Mostrar códigos FIFA en lugar de nombres de países
    private val _showFifaCodes = MutableStateFlow(loadShowFifaCodes())
    val showFifaCodes = _showFifaCodes.asStateFlow()

    // Estilo de fuente ("system" o "space_grotesk")
    private val _fontStyle = MutableStateFlow(loadFontStyle())
    val fontStyle = _fontStyle.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                matchRepository.deleteKnockoutMatches()
                matchRepository.syncFixture()
            } catch (e: Exception) {
                // Silently handle viewmodel scope errors during init sync
            }
        }
    }

    private fun loadAlarmMinutes(): Int {
        return sharedPrefs.getInt("alarm_minutes", 5)
    }

    fun onChangeAlarmMinutes(minutes: Int) {
        _alarmMinutes.value = minutes
        sharedPrefs.edit().putInt("alarm_minutes", minutes).apply()
    }

    private fun loadLiveScoresEnabled(): Boolean {
        return sharedPrefs.getBoolean("live_scoreboard_enabled", true)
    }

    fun onToggleLiveScores(enabled: Boolean) {
        _isLiveScoresEnabled.value = enabled
        sharedPrefs.edit().putBoolean("live_scoreboard_enabled", enabled).apply()
        if (enabled) {
            syncLiveScores()
        }
    }

    private fun loadShowFifaCodes(): Boolean {
        return sharedPrefs.getBoolean("show_fifa_codes", false)
    }

    fun onToggleShowFifaCodes(enabled: Boolean) {
        _showFifaCodes.value = enabled
        sharedPrefs.edit().putBoolean("show_fifa_codes", enabled).apply()
    }

    private fun loadFontStyle(): String {
        return sharedPrefs.getString("app_font_style", "space_grotesk") ?: "space_grotesk"
    }

    fun onChangeFontStyle(style: String) {
        _fontStyle.value = style
        sharedPrefs.edit().putString("app_font_style", style).apply()
    }

    fun syncLiveScores() {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis() / 1000
                val matches = matchRepository.getMatchesFlow().first()
                val hasLiveMatch = matches.any {
                    val withinSyncWindow = currentTime >= it.dateUnixTimestamp && currentTime < (it.dateUnixTimestamp + 86400)
                    val isExplicitlyLive = it.status == MatchStatus.LIVE || it.status == MatchStatus.HALFTIME || it.status == MatchStatus.PENALTIES
                    (withinSyncWindow || isExplicitlyLive) && it.status != MatchStatus.FINISHED
                }
                if (hasLiveMatch) {
                    matchRepository.syncLiveScores()
                }
            } catch (e: Exception) {
                // Silently ignore sync errors
            }
        }
    }

    // Observar el flujo de datos del dominio y transformarlo en estado de UI
    val uiState: StateFlow<CalendarState> = getMatchesUseCase()
        .onStart { 
            // Inicializar datos falsos si la base de datos está vacía
            prepopulateDataIfNeeded()
        }
        .map { matches ->
            CalendarState.Success(matches) as CalendarState
        }
        .catch { exception ->
            emit(CalendarState.Error(exception.message ?: context.getString(R.string.error_unknown)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarState.Loading
        )

    val predictionsState: StateFlow<CalendarState> = getPredictionsUseCase()
        .map { matches ->
            CalendarState.Success(matches) as CalendarState
        }
        .catch { exception ->
            emit(CalendarState.Error(exception.message ?: context.getString(R.string.error_unknown)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarState.Loading
        )

    private fun loadFavorites(): Set<String> {
        return sharedPrefs.getStringSet("favorite_teams", emptySet()) ?: emptySet()
    }

    /**
     * Alterna un equipo como favorito y lo persiste en SharedPreferences.
     * Al agregar un equipo, activa automáticamente las alarmas de todos sus partidos futuros programados.
     */
    fun onToggleFavorite(teamCode: String) {
        val current = _favoriteTeams.value
        val isAdding = !current.contains(teamCode)
        val updated = if (isAdding) current + teamCode else current - teamCode
        _favoriteTeams.value = updated
        sharedPrefs.edit().putStringSet("favorite_teams", updated).apply()
        if (isAdding) {
            scheduleAlarmsForTeams(setOf(teamCode))
        }
    }

    /**
     * Guarda el conjunto completo de equipos favoritos y lo persiste en SharedPreferences.
     * Para los equipos recién agregados, activa automáticamente las alarmas de sus partidos futuros.
     */
    fun onSaveAllFavorites(favorites: Set<String>) {
        val previousFavorites = _favoriteTeams.value
        val newlyAdded = favorites - previousFavorites
        _favoriteTeams.value = favorites
        sharedPrefs.edit().putStringSet("favorite_teams", favorites).apply()
        if (newlyAdded.isNotEmpty()) {
            scheduleAlarmsForTeams(newlyAdded)
        }
    }

    /**
     * Alterna el filtro de mostrar solo alertas activas.
     */
    fun onToggleShowAlarmsOnly() {
        _showAlarmsOnly.value = !_showAlarmsOnly.value
    }

    /**
     * Alterna el filtro de mostrar solo favoritos.
     */
    fun onToggleShowFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    /**
     * Guarda el marcador de la quiniela predicho por el usuario.
     */
    fun onSavePrediction(matchId: String, goalsA: Int, goalsB: Int, penaltyWinner: String? = null) {
        viewModelScope.launch {
            val prediction = Prediction(
                matchId = matchId,
                goalsA = goalsA,
                goalsB = goalsB,
                penaltyWinner = penaltyWinner
            )
            savePredictionUseCase(prediction)
        }
    }
    /**
     * Programa alarmas automáticamente para todos los partidos futuros programados
     * de los equipos indicados que aún no tengan alarma activa.
     *
     * @param teamCodes Conjunto de códigos FIFA de los equipos recién marcados como favoritos.
     */
    private fun scheduleAlarmsForTeams(teamCodes: Set<String>) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis() / 1000
            val allMatches = matchRepository.getMatchesFlow().first()
            val matchesToSchedule = allMatches.filter { match ->
                !match.isAlarmActive
                    && match.status == MatchStatus.SCHEDULED
                    && match.dateUnixTimestamp > currentTime
                    && (match.teamA in teamCodes || match.teamB in teamCodes)
            }
            for (match in matchesToSchedule) {
                val success = alarmScheduler.scheduleMatchAlarm(match)
                if (success) {
                    matchRepository.updateAlarmState(match.id, true)
                }
            }
        }
    }

    /**
     * Alterna la programación de la alarma para un partido específico.
     * Guarda el estado isAlarmActive en la base de datos de manera persistente.
     */
    fun onToggleAlarm(match: Match, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val nextState = !match.isAlarmActive
            if (nextState) {
                // Programar notificaciones. Esto usa setExactAndAllowWhileIdle o setAndAllowWhileIdle como fallback.
                val success = alarmScheduler.scheduleMatchAlarm(match)
                if (success) {
                    matchRepository.updateAlarmState(match.id, true)
                }
                onResult(success)
            } else {
                // Cancelar notificaciones.
                alarmScheduler.cancelMatchAlarm(match.id)
                matchRepository.updateAlarmState(match.id, false)
                onResult(false)
            }
        }
    }

    /**
     * Fuerza la sincronización de los partidos con el repositorio remoto.
     */
    fun onForceRefresh(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                matchRepository.syncFixture()
                if (isLiveScoresEnabled.value) {
                    matchRepository.syncLiveScores()
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    /**
     * Sincroniza los detalles de un partido específico bajo demanda.
     */
    fun fetchMatchDetailsOnDemand(match: Match) {
        viewModelScope.launch {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                    .withZone(java.time.ZoneId.of("UTC"))
                val dateString = formatter.format(java.time.Instant.ofEpochSecond(match.dateUnixTimestamp))
                matchRepository.syncLiveScores(dateString)
            } catch (e: Exception) {
                // Silently ignore errors
            }
        }
    }

    /**
     * Inserta partidos de prueba si la base de datos está vacía.
     */
    private suspend fun prepopulateDataIfNeeded() {
        // Verificar si la base de datos está vacía mediante first() para evitar el ciclo infinito de collect
        val currentMatches = matchRepository.getMatchesFlow().first()
        if (currentMatches.isEmpty()) {
            val mockMatches = listOf(
                // 10 partidos oficiales de la fase de grupos (Apertura y fase inicial, Junio 2026)
                Match(
                    id = "match_1",
                    dateUnixTimestamp = Instant.parse("2026-06-11T19:00:00Z").epochSecond,
                    teamA = "MEX",
                    teamB = "RSA",
                    stadium = "Estadio Azteca (Ciudad de México)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group A"
                ),
                Match(
                    id = "match_2",
                    dateUnixTimestamp = Instant.parse("2026-06-12T02:00:00Z").epochSecond,
                    teamA = "KOR",
                    teamB = "CZE",
                    stadium = "Estadio Akron (Guadalajara)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group A"
                ),
                Match(
                    id = "match_7",
                    dateUnixTimestamp = Instant.parse("2026-06-12T19:00:00Z").epochSecond,
                    teamA = "CAN",
                    teamB = "BIH",
                    stadium = "BMO Field (Toronto)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group B"
                ),
                Match(
                    id = "match_19",
                    dateUnixTimestamp = Instant.parse("2026-06-13T01:00:00Z").epochSecond,
                    teamA = "USA",
                    teamB = "PAR",
                    stadium = "SoFi Stadium (Los Ángeles)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group D"
                ),
                Match(
                    id = "match_8",
                    dateUnixTimestamp = Instant.parse("2026-06-13T19:00:00Z").epochSecond,
                    teamA = "QAT",
                    teamB = "SUI",
                    stadium = "Levi's Stadium (San Francisco / Santa Clara)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group B"
                ),
                Match(
                    id = "match_13",
                    dateUnixTimestamp = Instant.parse("2026-06-13T22:00:00Z").epochSecond,
                    teamA = "BRA",
                    teamB = "MAR",
                    stadium = "MetLife Stadium (Nueva York / Nueva Jersey)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group C"
                ),
                Match(
                    id = "match_14",
                    dateUnixTimestamp = Instant.parse("2026-06-14T01:00:00Z").epochSecond,
                    teamA = "HAI",
                    teamB = "SCO",
                    stadium = "Gillette Stadium (Boston / Foxborough)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group C"
                ),
                Match(
                    id = "match_20",
                    dateUnixTimestamp = Instant.parse("2026-06-14T04:00:00Z").epochSecond,
                    teamA = "AUS",
                    teamB = "TUR",
                    stadium = "BC Place (Vancouver)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group D"
                ),
                Match(
                    id = "match_25",
                    dateUnixTimestamp = Instant.parse("2026-06-14T17:00:00Z").epochSecond,
                    teamA = "GER",
                    teamB = "CUW",
                    stadium = "NRG Stadium (Houston)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group E"
                ),
                Match(
                    id = "match_31",
                    dateUnixTimestamp = Instant.parse("2026-06-14T20:00:00Z").epochSecond,
                    teamA = "NED",
                    teamB = "JPN",
                    stadium = "AT&T Stadium (Dallas)",
                    status = MatchStatus.SCHEDULED,
                    stage = MatchStage.GROUPS,
                    prediction = null,
                    goalsA = null,
                    goalsB = null,
                    group = "Group F"
                )
            )
            matchRepository.saveMatches(mockMatches)
        }
    }
}
