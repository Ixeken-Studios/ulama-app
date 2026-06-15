package com.ixeken.worldcupinfo.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.worldcupinfo.R

/**
 * Mapeador que traduce códigos FIFA de 3 letras o nombres de países a su recurso Drawable Vectorial.
 */
fun getTeamFlagDrawable(team: String): Int? {
    val normalized = team.trim().lowercase()
    return when (normalized) {
        "mex", "mexico" -> R.drawable.flag_mx
        "rsa", "south africa" -> R.drawable.flag_za
        "kor", "south korea", "korea republic" -> R.drawable.flag_kr
        "cze", "czech republic", "czechia" -> R.drawable.flag_cz
        "can", "canada" -> R.drawable.flag_ca
        "bih", "bosnia & herzegovina", "bosnia and herzegovina" -> R.drawable.flag_ba
        "usa", "united states", "united states of america" -> R.drawable.flag_us
        "par", "paraguay" -> R.drawable.flag_py
        "qa", "qat", "qatar" -> R.drawable.flag_qa
        "sui", "switzerland" -> R.drawable.flag_ch
        "bra", "brazil" -> R.drawable.flag_br
        "mar", "morocco" -> R.drawable.flag_ma
        "hai", "haiti" -> R.drawable.flag_ht
        "sco", "scotland" -> R.drawable.flag_gb_sct
        "aus", "australia" -> R.drawable.flag_au
        "tur", "turkey", "türkiye" -> R.drawable.flag_tr
        "ger", "germany" -> R.drawable.flag_de
        "ger", "germany" -> R.drawable.flag_de
        "cuw", "curacao", "curaçao" -> R.drawable.flag_cw
        "ned", "netherlands" -> R.drawable.flag_nl
        "jpn", "japan" -> R.drawable.flag_jp
        "arg", "argentina" -> R.drawable.flag_ar
        "fra", "france" -> R.drawable.flag_fr
        "esp", "spain" -> R.drawable.flag_es
        "eng", "england" -> R.drawable.flag_gb_eng
        "wls", "wales" -> R.drawable.flag_gb_wls
        "col", "colombia" -> R.drawable.flag_co
        "crc", "costa rica" -> R.drawable.flag_cr
        "ecu", "ecuador" -> R.drawable.flag_ec
        "ury", "uruguay" -> R.drawable.flag_uy
        "chl", "chile" -> R.drawable.flag_cl
        "per", "peru" -> R.drawable.flag_pe
        "ven", "venezuela" -> R.drawable.flag_ve
        "bol", "bolivia" -> R.drawable.flag_bo
        "ita", "italy" -> R.drawable.flag_it
        "swe", "sweden" -> R.drawable.flag_se
        "ukr", "ukraine" -> R.drawable.flag_ua
        "cmr", "cameroon" -> R.drawable.flag_cm
        "sen", "senegal" -> R.drawable.flag_sn
        "gha", "ghana" -> R.drawable.flag_gh
        "tun", "tunisia" -> R.drawable.flag_tn
        "dza", "algeria" -> R.drawable.flag_dz
        "egy", "egypt" -> R.drawable.flag_eg
        "nga", "nigeria" -> R.drawable.flag_ng
        "civ", "ivory coast", "cote d'ivoire" -> R.drawable.flag_ci
        "bel", "belgium" -> R.drawable.flag_be
        "pt", "por", "portugal" -> R.drawable.flag_pt
        "cro", "croatia" -> R.drawable.flag_hr
        "irn", "ira", "iran" -> R.drawable.flag_ir
        "sau", "ksa", "saudi arabia" -> R.drawable.flag_sa
        "den", "dnk", "denmark" -> R.drawable.flag_dk
        "pol", "poland" -> R.drawable.flag_pl
        "aut", "austria" -> R.drawable.flag_at
        "cpv", "cape verde" -> R.drawable.flag_cv
        "cod", "dr congo" -> R.drawable.flag_cd
        "irq", "iraq" -> R.drawable.flag_iq
        "jor", "jordan" -> R.drawable.flag_jo
        "nzl", "new zealand" -> R.drawable.flag_nz
        "nor", "norway" -> R.drawable.flag_no
        "pan", "panama" -> R.drawable.flag_pa
        "uzb", "uzbekistan" -> R.drawable.flag_uz
        else -> null
    }
}

/**
 * Renderiza la bandera del equipo a partir de los recursos vectoriales.
 * Cae en fallback seguro si no se encuentra mapeado el país.
 */
@Composable
fun TeamFlagEmoji(teamCode: String) {
    val drawableId = getTeamFlagDrawable(teamCode)
    if (drawableId != null) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = teamCode,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        ) {
            Text(text = "⚽", fontSize = 20.sp)
        }
    }
}

/**
 * Renderiza la bandera del equipo en tamaño grande a partir de los recursos vectoriales.
 */
@Composable
fun TeamFlagEmojiLarge(teamCode: String) {
    val drawableId = getTeamFlagDrawable(teamCode)
    if (drawableId != null) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = teamCode,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        ) {
            Text(text = "⚽", fontSize = 28.sp)
        }
    }
}
