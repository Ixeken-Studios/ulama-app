package com.ixeken.ulama.core.util

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Utilidades para el parseo, conversion de zona horaria y formateo de fechas y horas de partidos.
 * Respeta la configuracion del sistema del dispositivo para formato de 12 o 24 horas y
 * las convenciones de localizacion de fechas segun el idioma y pais configurado por el usuario.
 */
object DateTimeUtils {

    /**
     * Determina si el sistema operativo del dispositivo esta configurado en formato de 24 horas.
     * En pruebas locales JVM sin entorno Android retorna verdadero como valor de respaldo seguro.
     */
    fun is24HourFormat(context: Context?): Boolean {
        if (context == null) return true
        return runCatching {
            android.text.format.DateFormat.is24HourFormat(context)
        }.getOrDefault(true)
    }

    private val formatterCache = ConcurrentHashMap<Pair<String, Locale>, DateTimeFormatter>()
    private val instantCache = ConcurrentHashMap<String, Instant>()
    private val bestPatternCache = ConcurrentHashMap<Locale, String>()

    private fun getFormatter(pattern: String, locale: Locale): DateTimeFormatter {
        return formatterCache.computeIfAbsent(Pair(pattern, locale)) {
            DateTimeFormatter.ofPattern(pattern, locale)
        }
    }

    /**
     * Memoriza el parseo de cadenas ISO UTC inmutables para evitar reconstruir objetos Instant
     * repetidamente durante el filtrado temporal y el desplazamiento de listas.
     */
    private fun parseInstant(dateUtc: String): Instant {
        return instantCache.computeIfAbsent(dateUtc) { raw ->
            Instant.parse(normalizeIsoDate(raw))
        }
    }

    /**
     * Memoriza el patron localizado de fecha corta por idioma para evitar invocar JNI en cada tarjeta.
     */
    private fun getBestDatePattern(locale: Locale): String {
        return bestPatternCache.computeIfAbsent(locale) { loc ->
            runCatching {
                android.text.format.DateFormat.getBestDateTimePattern(loc, "EEE d MMM") ?: "EEE, d MMM"
            }.getOrDefault("EEE, d MMM")
        }
    }

    /**
     * Formatea la hora de un partido a partir de su cadena ISO UTC, convirtiendola a la zona horaria local
     * del dispositivo y respetando la preferencia de 12 o 24 horas del usuario.
     *
     * @param dateUtc Cadena de fecha y hora en formato ISO 8601 UTC.
     * @param is24Hour Indica si se debe formatear en 24 horas o en 12 horas con indicador meridiano.
     * @param zoneId Zona horaria a emplear, por defecto la zona activa en el sistema.
     * @param locale Configuracion regional activa en el dispositivo.
     */
    fun formatMatchTime(
        dateUtc: String,
        is24Hour: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        return runCatching {
            val instant = parseInstant(dateUtc)
            val zonedDateTime = instant.atZone(zoneId)
            val pattern = if (is24Hour) "HH:mm" else "h:mm a"
            getFormatter(pattern, locale).format(zonedDateTime)
        }.getOrDefault(dateUtc.substringAfter("T").take(5))
    }

    /**
     * Sobrecarga contextual para formatear la hora del partido consultando directamente las preferencias del sistema.
     */
    fun formatMatchTime(
        context: Context?,
        dateUtc: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val is24Hour = is24HourFormat(context)
        return formatMatchTime(dateUtc, is24Hour, zoneId, locale)
    }

    /**
     * Formatea la fecha de un partido convirtiendola a la zona horaria del dispositivo
     * y adaptandola a las convenciones de orden de fecha y nombres abreviados de la region activa.
     */
    fun formatMatchDate(
        context: Context?,
        dateUtc: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        return runCatching {
            val instant = parseInstant(dateUtc)
            val zonedDateTime = instant.atZone(zoneId)
            val pattern = getBestDatePattern(locale)
            getFormatter(pattern, locale).format(zonedDateTime)
        }.getOrDefault("")
    }

    /**
     * Normaliza cadenas de fecha ISO que carecen de segundos antes del sufijo UTC para asegurar compatibilidad
     * con Instant.parse en todas las versiones de runtime.
     */
    fun normalizeIsoDate(dateStr: String): String {
        return if (dateStr.length == 17 && dateStr.endsWith("Z")) {
            dateStr.substring(0, 16) + ":00Z"
        } else {
            dateStr
        }
    }

    /**
     * Determina si la fecha ISO UTC de un encuentro corresponde al dia de hoy
     * evaluando estrictamente en la zona horaria local del dispositivo.
     */
    fun isToday(
        dateUtc: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowInstant: Instant = Instant.now()
    ): Boolean {
        return runCatching {
            val instant = parseInstant(dateUtc)
            val matchLocalDate = instant.atZone(zoneId).toLocalDate()
            val todayLocal = nowInstant.atZone(zoneId).toLocalDate()
            matchLocalDate.isEqual(todayLocal)
        }.getOrDefault(false)
    }

    /**
     * Determina si la fecha ISO UTC de un encuentro corresponde a un dia anterior a hoy
     * evaluando estrictamente en la zona horaria local del dispositivo.
     */
    fun isBeforeToday(
        dateUtc: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowInstant: Instant = Instant.now()
    ): Boolean {
        return runCatching {
            val instant = parseInstant(dateUtc)
            val matchLocalDate = instant.atZone(zoneId).toLocalDate()
            val todayLocal = nowInstant.atZone(zoneId).toLocalDate()
            matchLocalDate.isBefore(todayLocal)
        }.getOrDefault(false)
    }

    /**
     * Determina si la fecha ISO UTC de un encuentro corresponde a un dia posterior a hoy
     * evaluando estrictamente en la zona horaria local del dispositivo.
     */
    fun isAfterToday(
        dateUtc: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowInstant: Instant = Instant.now()
    ): Boolean {
        return runCatching {
            val instant = parseInstant(dateUtc)
            val matchLocalDate = instant.atZone(zoneId).toLocalDate()
            val todayLocal = nowInstant.atZone(zoneId).toLocalDate()
            matchLocalDate.isAfter(todayLocal)
        }.getOrDefault(false)
    }

    /**
     * Convierte la cadena ISO UTC a milisegundos de epoca para programacion de alarmas con el sistema.
     */
    fun parseUtcEpochMillis(dateUtc: String): Long {
        return runCatching {
            parseInstant(dateUtc).toEpochMilli()
        }.getOrDefault(0L)
    }

    /**
     * Resuelve la clave de fecha local en formato YYYY-MM-DD para agrupar partidos de una misma jornada.
     * Convierte la fecha UTC a la zona horaria del usuario para evitar desfasajes en partidos nocturnos.
     */
    fun getLocalDateKey(
        dateUtc: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return runCatching {
            val instant = parseInstant(dateUtc)
            instant.atZone(zoneId).toLocalDate().toString()
        }.getOrDefault(dateUtc.take(10))
    }

    /**
     * Formatea un encabezado de fecha descriptivo y localizado para la agrupacion de partidos pasados.
     * Adapta el patron de texto segun el idioma activo capitalizando la letra inicial del dia.
     */
    fun formatDateHeader(
        dateUtc: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        return runCatching {
            val instant = parseInstant(dateUtc)
            val zonedDateTime = instant.atZone(zoneId)
            val isSpanish = locale.language.equals("es", ignoreCase = true)
            val pattern = if (isSpanish) "EEEE, d 'de' MMMM 'de' yyyy" else "EEEE, MMMM d, yyyy"
            val formatted = getFormatter(pattern, locale).format(zonedDateTime)
            formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }.getOrDefault(dateUtc.take(10))
    }
}
