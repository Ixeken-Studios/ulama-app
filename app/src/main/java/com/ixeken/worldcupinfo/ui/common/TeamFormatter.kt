package com.ixeken.worldcupinfo.ui.common

import com.ixeken.worldcupinfo.R
import com.ixeken.worldcupinfo.domain.model.MatchStage

/**
 * Maper translating 3-letter FIFA team codes to user-friendly display names.
 */
/**
 * Helper to get the default English display name for a team code.
 */
fun getTeamDefaultEnglishName(teamCode: String): String? {
    return when (teamCode.trim().lowercase()) {
        "mex" -> "Mexico"
        "rsa" -> "South Africa"
        "kor" -> "South Korea"
        "cze" -> "Czechia"
        "can" -> "Canada"
        "bih" -> "Bosnia"
        "usa" -> "United States"
        "par" -> "Paraguay"
        "qat" -> "Qatar"
        "sui" -> "Switzerland"
        "bra" -> "Brazil"
        "mar" -> "Morocco"
        "hai" -> "Haiti"
        "sco" -> "Scotland"
        "aus" -> "Australia"
        "tur" -> "Turkey"
        "ger" -> "Germany"
        "cuw" -> "Curacao"
        "ned" -> "Netherlands"
        "jpn" -> "Japan"
        "arg" -> "Argentina"
        "fra" -> "France"
        "esp" -> "Spain"
        "eng" -> "England"
        "wls" -> "Wales"
        "col" -> "Colombia"
        "crc" -> "Costa Rica"
        "ecu" -> "Ecuador"
        "ury" -> "Uruguay"
        "chl" -> "Chile"
        "per" -> "Peru"
        "ven" -> "Venezuela"
        "bol" -> "Bolivia"
        "ita" -> "Italy"
        "swe" -> "Sweden"
        "ukr" -> "Ukraine"
        "cmr" -> "Cameroon"
        "sen" -> "Senegal"
        "gha" -> "Ghana"
        "tun" -> "Tunisia"
        "dza" -> "Algeria"
        "egy" -> "Egypt"
        "nga" -> "Nigeria"
        "civ" -> "Ivory Coast"
        "bel" -> "Belgium"
        "por" -> "Portugal"
        "cro" -> "Croatia"
        "irn" -> "Iran"
        "sau" -> "Saudi Arabia"
        "den" -> "Denmark"
        "pol" -> "Poland"
        "aut" -> "Austria"
        "cpv" -> "Cape Verde"
        "cod" -> "DR Congo"
        "irq" -> "Iraq"
        "jor" -> "Jordan"
        "nzl" -> "New Zealand"
        "nor" -> "Norway"
        "pan" -> "Panama"
        "uzb" -> "Uzbekistan"
        else -> null
    }
}

/**
 * Maper translating 3-letter FIFA team codes to localized user-friendly display names.
 */
fun getTeamDisplayName(teamCode: String, context: android.content.Context): String {
    val normalized = teamCode.trim().lowercase()
    val resId = context.resources.getIdentifier("team_$normalized", "string", context.packageName)
    if (resId != 0) {
        return context.getString(resId)
    }
    return getTeamDefaultEnglishName(normalized) ?: teamCode.uppercase()
}


/**
 * Verifica si un código de equipo es un marcador de posición (placeholder) o aún no está definido.
 */
fun isPlaceholderTeam(teamCode: String): Boolean {
    val c = teamCode.trim().uppercase()
    return c.matches(Regex("""^\d[A-Z]$""")) || 
           (c.startsWith("W") && c.substring(1).all { it.isDigit() }) ||
           (c.startsWith("RU") && c.substring(2).all { it.isDigit() }) ||
           (c.startsWith("L") && c.substring(1).all { it.isDigit() }) ||
           c.contains("/") ||
           c == "TBD" ||
           c == "TBC"
}

/**
 * Formatea códigos de equipo no definidos (placeholders) para una mejor visualización en la interfaz.
 */
fun formatTeamCodeForDisplay(teamCode: String, stage: MatchStage, matchId: String, tbdString: String, context: android.content.Context): String {
    val trimmed = teamCode.trim()
    val c = trimmed.uppercase()
    val isPlaceholder = isPlaceholderTeam(trimmed)
    
    if (!isPlaceholder) {
        return trimmed
    }
    
    if (stage == MatchStage.ROUND_OF_32) {
        val firstPlaceRegex = Regex("""^1([A-L])$""")
        val secondPlaceRegex = Regex("""^2([A-L])$""")
        
        val firstMatch = firstPlaceRegex.matchEntire(c)
        if (firstMatch != null) {
            return context.getString(R.string.position_ordinal_1st, "1", firstMatch.groupValues[1])
        }
        
        val secondMatch = secondPlaceRegex.matchEntire(c)
        if (secondMatch != null) {
            return context.getString(R.string.position_ordinal_1st, "2", secondMatch.groupValues[1])
        }
        
        if (c.startsWith("3")) {
            val idNum = matchId.replace("match_", "").toIntOrNull() ?: 0
            val index = when (c) {
                "3A/B/C" -> "1"
                "3D/E/F" -> "2"
                "3G/H/I" -> "3"
                "3J/K/L" -> "4"
                "3A/B/C/D" -> "5"
                "3E/F/G/H" -> "6"
                "3I/J/K/L" -> "7"
                else -> {
                    val hash = Math.abs(c.hashCode() + idNum) % 8 + 1
                    hash.toString()
                }
            }
            return context.getString(R.string.position_ordinal_nth, 3, index)
        }
    } else if (stage != MatchStage.GROUPS) {
        return tbdString
    }
    
    return trimmed
}

/**
 * Formats a goalscorer's full name to show the first initial and the last name.
 * For example: "Cyle Larin" becomes "C. Larin", "Raúl Jiménez" becomes "R. Jiménez".
 *
 * @param fullName The player's full name.
 * @return The formatted name with initial and last name.
 */
fun formatScorerName(fullName: String): String {
    val parts = fullName.trim().split(Regex("\\s+"))
    if (parts.size <= 1) return fullName
    val initial = parts[0].firstOrNull()?.uppercaseChar() ?: ""
    val lastName = parts.subList(1, parts.size).joinToString(" ")
    return "$initial. $lastName"
}


