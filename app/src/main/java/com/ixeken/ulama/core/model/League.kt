package com.ixeken.ulama.core.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ixeken.ulama.R

/**
 * Representa las competiciones de futbol soportadas de forma modular en Ulama.
 * Encapsula la configuracion de codigos de endpoint, recursos de texto, reglas
 * visuales de cortes de clasificacion o descenso y explicaciones de leyendas al pie.
 *
 * @param code Identificador de endpoint en la API de ESPN.
 * @param nameRes Identificador de recurso de cadena para soporte multiidioma.
 * @param tableCutoffs Lista inmutable de cortes visuales para delimitacion de zonas.
 * @param lightLogoUrl URL oficial del logotipo optimizado para fondos claros.
 * @param darkLogoUrl URL oficial del logotipo optimizado para fondos oscuros.
 * @param tableCutoffs Lista inmutable de cortes visuales para delimitacion de zonas.
 * @param tableLegend Lista inmutable de leyendas explicativas mostradas al pie de la tabla.
 */
@Immutable
enum class League(
    val code: String,
    @param:StringRes val nameRes: Int,
    val lightLogoUrl: String,
    val darkLogoUrl: String,
    val category: LeagueCategory = LeagueCategory.CLUBS,
    val region: LeagueRegion = LeagueRegion.GLOBAL,
    @param:StringRes val subRegionRes: Int = R.string.subregion_global_tournaments,
    val isTournament: Boolean = false,
    val tournamentYear: String? = null,
    val knockoutBracketTeams: Int = 0,
    val hasThirdPlaceMatch: Boolean = false,
    @param:StringRes val bracketTitleRes: Int = R.string.title_radial_bracket,
    val hasStandings: Boolean = true,
    val tableCutoffs: List<TableCutoff> = emptyList(),
    val tableLegend: List<TableLegendEntry> = emptyList(),
    val groupCutoffs: List<TableCutoff> = emptyList(),
    val groupLegend: List<TableLegendEntry> = emptyList(),
    val mergeStandingsGroups: Boolean = false
) {
    FAVORITES(
        code = "favorites",
        nameRes = R.string.league_favorites,
        lightLogoUrl = "",
        darkLogoUrl = "",
        hasStandings = false
    ),
    MY_LEAGUES(
        code = "my_leagues",
        nameRes = R.string.league_my_leagues,
        lightLogoUrl = "",
        darkLogoUrl = "",
        hasStandings = false
    ),
    DEBUG(
        code = "debug",
        nameRes = R.string.league_debug,
        lightLogoUrl = "",
        darkLogoUrl = "",
        knockoutBracketTeams = 16,
        hasThirdPlaceMatch = true,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-2", labelRes = R.string.legend_champions_league),
            TableLegendEntry(rangeText = "3-4", labelRes = R.string.legend_playoff_round)
        )
    ),
    AFC_CHAMPIONS(
        code = "afc.champions",
        nameRes = R.string.league_afc_champions,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2200.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/2200.png",
        region = LeagueRegion.ASIA,
        subRegionRes = R.string.subregion_asia,
        isTournament = true,
        knockoutBracketTeams = 16,
        groupCutoffs = listOf(
            TableCutoff(rankAfter = 8, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        groupLegend = listOf(
            TableLegendEntry(rangeText = "1-8", labelRes = R.string.legend_round_of_16)
        )
    ),
    A_LEAGUE_MEN(
        code = "aus.1",
        nameRes = R.string.league_aus_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/1308.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/1308.png",
        region = LeagueRegion.OCEANIA,
        subRegionRes = R.string.subregion_oceania,
        knockoutBracketTeams = 6,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 6, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-2", labelRes = R.string.legend_semifinals_direct),
            TableLegendEntry(rangeText = "3-6", labelRes = R.string.legend_elimination_finals)
        )
    ),
    BRAZILEIRAO(
        code = "bra.1",
        nameRes = R.string.league_bra_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/85.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/85.png",
        region = LeagueRegion.AMERICAS,
        subRegionRes = R.string.subregion_brazil,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 6, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 12, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.DASHED),
            TableCutoff(rankAfter = 16, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-4", labelRes = R.string.legend_libertadores),
            TableLegendEntry(rangeText = "5-6", labelRes = R.string.legend_pre_libertadores),
            TableLegendEntry(rangeText = "7-12", labelRes = R.string.legend_sudamericana),
            TableLegendEntry(rangeText = "17-20", labelRes = R.string.legend_relegation)
        )
    ),
    BUNDESLIGA(
        code = "ger.1",
        nameRes = R.string.league_ger_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/10.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/10.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_germany,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 6, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 15, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED),
            TableCutoff(rankAfter = 16, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-4", labelRes = R.string.legend_champions_league),
            TableLegendEntry(rangeText = "5", labelRes = R.string.legend_europa_league),
            TableLegendEntry(rangeText = "6", labelRes = R.string.legend_conference_league),
            TableLegendEntry(rangeText = "16", labelRes = R.string.legend_relegation_playoff),
            TableLegendEntry(rangeText = "17-18", labelRes = R.string.legend_relegation)
        )
    ),
    CAF_CHAMPIONS(
        code = "caf.champions",
        nameRes = R.string.league_caf_champions,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2391.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/2391.png",
        region = LeagueRegion.AFRICA,
        subRegionRes = R.string.subregion_africa,
        isTournament = true,
        knockoutBracketTeams = 8,
        groupCutoffs = listOf(
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        groupLegend = listOf(
            TableLegendEntry(rangeText = "1-2", labelRes = R.string.legend_quarterfinals_direct)
        )
    ),
    CHAMPIONS_LEAGUE(
        code = "uefa.champions",
        nameRes = R.string.league_uefa_champions,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/2.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_european_competitions,
        isTournament = true,
        knockoutBracketTeams = 16,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 8, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 24, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-8", labelRes = R.string.legend_round_of_16),
            TableLegendEntry(rangeText = "9-24", labelRes = R.string.legend_playoff_round)
        )
    ),
    CHAMPIONSHIP(
        code = "eng.2",
        nameRes = R.string.league_eng_2,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/24.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/24.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_england,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 6, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 21, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-2", labelRes = R.string.legend_direct_promotion),
            TableLegendEntry(rangeText = "3-6", labelRes = R.string.legend_promotion_playoff),
            TableLegendEntry(rangeText = "22-24", labelRes = R.string.legend_relegation)
        )
    ),
    CONFERENCE_LEAGUE(
        code = "uefa.europa.conf",
        nameRes = R.string.league_uefa_europa_conf,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/20296.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/20296.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_european_competitions,
        isTournament = true,
        knockoutBracketTeams = 16,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 8, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 24, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-8", labelRes = R.string.legend_round_of_16),
            TableLegendEntry(rangeText = "9-24", labelRes = R.string.legend_playoff_round)
        )
    ),
    EREDIVISIE(
        code = "ned.1",
        nameRes = R.string.league_ned_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/11.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/11.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_netherlands,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 8, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.DASHED),
            TableCutoff(rankAfter = 15, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED),
            TableCutoff(rankAfter = 16, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-2", labelRes = R.string.legend_champions_league),
            TableLegendEntry(rangeText = "3-4", labelRes = R.string.legend_europa_league),
            TableLegendEntry(rangeText = "5-8", labelRes = R.string.legend_conference_playoff),
            TableLegendEntry(rangeText = "16", labelRes = R.string.legend_relegation_playoff),
            TableLegendEntry(rangeText = "17-18", labelRes = R.string.legend_relegation)
        )
    ),
    EUROPA_LEAGUE(
        code = "uefa.europa",
        nameRes = R.string.league_uefa_europa,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2310.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/2310.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_european_competitions,
        isTournament = true,
        knockoutBracketTeams = 16,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 8, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 24, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-8", labelRes = R.string.legend_round_of_16),
            TableLegendEntry(rangeText = "9-24", labelRes = R.string.legend_playoff_round)
        )
    ),
    LA_LIGA(
        code = "esp.1",
        nameRes = R.string.league_esp_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/15.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/15.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_spain,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 6, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 17, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-4", labelRes = R.string.legend_champions_league),
            TableLegendEntry(rangeText = "5-6", labelRes = R.string.legend_europa_league),
            TableLegendEntry(rangeText = "18-20", labelRes = R.string.legend_relegation)
        )
    ),
    LIBERTADORES(
        code = "conmebol.libertadores",
        nameRes = R.string.league_conmebol_libertadores,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/58.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/58.png",
        region = LeagueRegion.AMERICAS,
        subRegionRes = R.string.subregion_south_america,
        isTournament = true,
        knockoutBracketTeams = 16,
        groupCutoffs = listOf(
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 3, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.DASHED)
        ),
        groupLegend = listOf(
            TableLegendEntry(rangeText = "1-2", labelRes = R.string.legend_round_of_16),
            TableLegendEntry(rangeText = "3", labelRes = R.string.legend_sudamericana_playoffs)
        )
    ),
    LIGA_MX(
        code = "mex.1",
        nameRes = R.string.league_mex_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/22.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/22.png",
        region = LeagueRegion.AMERICAS,
        subRegionRes = R.string.subregion_mexico,
        knockoutBracketTeams = 8,
        bracketTitleRes = R.string.legend_liguilla,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 8, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-8", labelRes = R.string.legend_liguilla)
        )
    ),
    LIGA_MX_FEMENIL(
        code = "mex.w.1",
        nameRes = R.string.league_mex_w_1,
        lightLogoUrl = "",
        darkLogoUrl = "",
        region = LeagueRegion.AMERICAS,
        subRegionRes = R.string.subregion_mexico,
        knockoutBracketTeams = 8,
        bracketTitleRes = R.string.legend_liguilla,
        mergeStandingsGroups = true,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 8, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-8", labelRes = R.string.legend_liguilla)
        )
    ),
    LIGUE_1(
        code = "fra.1",
        nameRes = R.string.league_fra_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/9.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/9.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_france,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 3, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 6, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 15, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED),
            TableCutoff(rankAfter = 16, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-3", labelRes = R.string.legend_champions_league),
            TableLegendEntry(rangeText = "4-5", labelRes = R.string.legend_europa_league),
            TableLegendEntry(rangeText = "6", labelRes = R.string.legend_conference_league),
            TableLegendEntry(rangeText = "16", labelRes = R.string.legend_relegation_playoff),
            TableLegendEntry(rangeText = "17-18", labelRes = R.string.legend_relegation)
        )
    ),
    MLS(
        code = "usa.1",
        nameRes = R.string.league_usa_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/19.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/19.png",
        region = LeagueRegion.AMERICAS,
        subRegionRes = R.string.subregion_usa_canada,
        knockoutBracketTeams = 16,
        groupCutoffs = listOf(
            TableCutoff(rankAfter = 7, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 9, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.DASHED)
        ),
        groupLegend = listOf(
            TableLegendEntry(rangeText = "1-7", labelRes = R.string.legend_mls_round_one),
            TableLegendEntry(rangeText = "8-9", labelRes = R.string.legend_mls_wild_card)
        )
    ),
    PREMIER_LEAGUE(
        code = "eng.1",
        nameRes = R.string.league_eng_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/23.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/23.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_england,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 5, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 17, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-4", labelRes = R.string.legend_champions_league),
            TableLegendEntry(rangeText = "5", labelRes = R.string.legend_europa_league),
            TableLegendEntry(rangeText = "6", labelRes = R.string.legend_conference_league),
            TableLegendEntry(rangeText = "18-20", labelRes = R.string.legend_relegation)
        )
    ),
    PRIMEIRA_LIGA(
        code = "por.1",
        nameRes = R.string.league_por_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/14.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/14.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_portugal,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 1, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 3, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.DASHED),
            TableCutoff(rankAfter = 15, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED),
            TableCutoff(rankAfter = 16, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1", labelRes = R.string.legend_champions_league),
            TableLegendEntry(rangeText = "2", labelRes = R.string.legend_champions_qualifying),
            TableLegendEntry(rangeText = "3", labelRes = R.string.legend_europa_qualifying),
            TableLegendEntry(rangeText = "4", labelRes = R.string.legend_conference_qualifying),
            TableLegendEntry(rangeText = "16", labelRes = R.string.legend_relegation_playoff),
            TableLegendEntry(rangeText = "17-18", labelRes = R.string.legend_relegation)
        )
    ),
    SERIE_A(
        code = "ita.1",
        nameRes = R.string.league_ita_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/12.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/12.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_italy,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 6, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 17, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-4", labelRes = R.string.legend_champions_league),
            TableLegendEntry(rangeText = "5", labelRes = R.string.legend_europa_league),
            TableLegendEntry(rangeText = "6", labelRes = R.string.legend_conference_league),
            TableLegendEntry(rangeText = "18-20", labelRes = R.string.legend_relegation)
        )
    ),
    WOMENS_CHAMPIONS_LEAGUE(
        code = "uefa.wchampions",
        nameRes = R.string.league_uefa_wchampions,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2408.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/2408.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_european_competitions,
        isTournament = true,
        knockoutBracketTeams = 8,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 4, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 12, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-4", labelRes = R.string.legend_quarterfinals_direct),
            TableLegendEntry(rangeText = "5-12", labelRes = R.string.legend_playoff_round)
        )
    ),
    WSL(
        code = "eng.w.1",
        nameRes = R.string.league_eng_w_1,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2314.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/2314.png",
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_england,
        tableCutoffs = listOf(
            TableCutoff(rankAfter = 2, zoneType = TableZoneType.QUALIFICATION, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 3, zoneType = TableZoneType.PLAYOFF, lineStyle = CutoffLineStyle.SOLID),
            TableCutoff(rankAfter = 12, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.DASHED),
            TableCutoff(rankAfter = 13, zoneType = TableZoneType.RELEGATION, lineStyle = CutoffLineStyle.SOLID)
        ),
        tableLegend = listOf(
            TableLegendEntry(rangeText = "1-2", labelRes = R.string.legend_uwcl),
            TableLegendEntry(rangeText = "3", labelRes = R.string.legend_uwcl_qualifying),
            TableLegendEntry(rangeText = "13", labelRes = R.string.legend_relegation_playoff),
            TableLegendEntry(rangeText = "14", labelRes = R.string.legend_relegation)
        )
    ),
    WORLD_CUP(
        code = "fifa.world",
        nameRes = R.string.league_fifa_world,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/4.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/4.png",
        category = LeagueCategory.INTERNATIONAL,
        region = LeagueRegion.GLOBAL,
        subRegionRes = R.string.subregion_global_tournaments,
        isTournament = true,
        knockoutBracketTeams = 32,
        hasThirdPlaceMatch = true
    ),
    UEFA_NATIONS_LEAGUE(
        code = "uefa.nations",
        nameRes = R.string.league_uefa_nations,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2395.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/2395.png",
        category = LeagueCategory.INTERNATIONAL,
        region = LeagueRegion.EUROPE,
        subRegionRes = R.string.subregion_european_competitions,
        isTournament = true,
        knockoutBracketTeams = 8,
        hasThirdPlaceMatch = true
    ),
    CONCACAF_NATIONS_LEAGUE(
        code = "concacaf.nations.league",
        nameRes = R.string.league_concacaf_nations,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/2406.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/2406.png",
        category = LeagueCategory.INTERNATIONAL,
        region = LeagueRegion.AMERICAS,
        subRegionRes = R.string.subregion_concacaf,
        isTournament = true,
        knockoutBracketTeams = 8,
        hasThirdPlaceMatch = true
    ),
    FRIENDLIES(
        code = "fifa.friendly",
        nameRes = R.string.league_fifa_friendly,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/53.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/53.png",
        category = LeagueCategory.INTERNATIONAL,
        region = LeagueRegion.GLOBAL,
        subRegionRes = R.string.subregion_global_tournaments,
        hasStandings = false
    ),
    GOLD_CUP(
        code = "concacaf.gold",
        nameRes = R.string.league_concacaf_gold,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/59.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/59.png",
        category = LeagueCategory.INTERNATIONAL,
        region = LeagueRegion.AMERICAS,
        subRegionRes = R.string.subregion_concacaf,
        isTournament = true,
        tournamentYear = "2027",
        knockoutBracketTeams = 8
    ),
    AFCON(
        code = "caf.nations",
        nameRes = R.string.league_caf_nations,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/76.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/76.png",
        category = LeagueCategory.INTERNATIONAL,
        region = LeagueRegion.AFRICA,
        subRegionRes = R.string.subregion_africa,
        isTournament = true,
        tournamentYear = "2027",
        knockoutBracketTeams = 16,
        hasThirdPlaceMatch = true
    ),
    ASIAN_CUP(
        code = "afc.asian.cup",
        nameRes = R.string.league_afc_asian_cup,
        lightLogoUrl = "https://a.espncdn.com/combiner/i?img=/i/leaguelogos/soccer/500/2243.png",
        darkLogoUrl = "https://a.espncdn.com/combiner/i?img=/i/leaguelogos/soccer/500-dark/2243.png",
        category = LeagueCategory.INTERNATIONAL,
        region = LeagueRegion.ASIA,
        subRegionRes = R.string.subregion_asia,
        isTournament = true,
        tournamentYear = "2027",
        knockoutBracketTeams = 16
    ),
    WOMENS_WORLD_CUP(
        code = "fifa.wwc",
        nameRes = R.string.league_fifa_wwc,
        lightLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500/60.png",
        darkLogoUrl = "https://a.espncdn.com/i/leaguelogos/soccer/500-dark/60.png",
        category = LeagueCategory.INTERNATIONAL,
        region = LeagueRegion.GLOBAL,
        subRegionRes = R.string.subregion_global_tournaments,
        isTournament = true,
        tournamentYear = "2027",
        knockoutBracketTeams = 16,
        hasThirdPlaceMatch = true
    );

    /**
     * Determina si la liga corresponde a una vista virtual agregada como Favoritos o Mis Ligas.
     */
    val isVirtual: Boolean
        get() = this == FAVORITES || this == MY_LEAGUES

    /**
     * Determina si la competicion corresponde a torneos internacionales de selecciones nacionales.
     */
    val isInternational: Boolean
        get() = category == LeagueCategory.INTERNATIONAL

    /**
     * Resuelve el ano base de consulta para el endpoint de ESPN en torneos por etapas.
     */
    fun defaultTournamentYear(currentYear: Int = 2026): String =
        tournamentYear ?: currentYear.toString()

    /**
     * Determina si la liga contempla fase de eliminatorias o liguilla con cuadro visual.
     */
    val hasKnockoutBracket: Boolean
        get() = knockoutBracketTeams > 0

    /**
     * Resuelve la URL del logotipo segun la configuracion de brillo del tema activo.
     */
    fun getLogoUrl(isDark: Boolean): String = if (isDark) darkLogoUrl else lightLogoUrl

    companion object {
        /**
         * Coleccion inmutable de competiciones de clubes nacionales y continentales.
         */
        val clubLeagues: List<League> = entries.filter { it.category == LeagueCategory.CLUBS && !it.isVirtual && it != DEBUG }

        /**
         * Coleccion inmutable de torneos de selecciones nacionales internacionales.
         */
        val internationalLeagues: List<League> = entries.filter { it.category == LeagueCategory.INTERNATIONAL }

        /**
         * Coleccion inmutable de todas las competiciones deportivas reales soportadas por la API de ESPN.
         */
        val standardLeagues: List<League> = clubLeagues + internationalLeagues

        /**
         * Obtiene la instancia de League correspondiente al codigo de ESPN o LIGA_MX por defecto.
         */
        fun fromCode(code: String): League = entries.firstOrNull { it.code == code } ?: LIGA_MX
    }
}
