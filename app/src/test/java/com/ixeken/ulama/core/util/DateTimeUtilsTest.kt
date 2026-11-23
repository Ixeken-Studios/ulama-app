package com.ixeken.ulama.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/**
 * Pruebas unitarias para validar el parseo de zona horaria, formatos de 12 y 24 horas, y fechas de partidos.
 */
class DateTimeUtilsTest {

    private val mexicoZone = ZoneId.of("America/Mexico_City") // UTC-6

    @Test
    fun formatMatchTime_24Hour_convertsUtcToLocalTimeProperly() {
        // 02:00 UTC del 19 de octubre son las 20:00 del 18 de octubre en Ciudad de México
        val dateUtc = "2026-10-19T02:00:00Z"
        val time = DateTimeUtils.formatMatchTime(
            dateUtc = dateUtc,
            is24Hour = true,
            zoneId = mexicoZone,
            locale = Locale.US
        )
        assertEquals("20:00", time)
    }

    @Test
    fun formatMatchTime_12Hour_convertsUtcToLocal12HourFormat() {
        val dateUtc = "2026-10-19T02:00:00Z"
        val time = DateTimeUtils.formatMatchTime(
            dateUtc = dateUtc,
            is24Hour = false,
            zoneId = mexicoZone,
            locale = Locale.US
        )
        assertEquals("8:00 PM", time)
    }

    @Test
    fun formatMatchDate_convertsDateAcrossDayBoundaries() {
        // En UTC es 19 de octubre, pero en México son las 20:00 del 18 de octubre (domingo)
        val dateUtc = "2026-10-19T02:00:00Z"
        val formattedDate = DateTimeUtils.formatMatchDate(
            context = null,
            dateUtc = dateUtc,
            zoneId = mexicoZone,
            locale = Locale.US
        )
        assertTrue(formattedDate.contains("18"))
        assertTrue(formattedDate.contains("Oct"))
    }

    @Test
    fun normalizeIsoDate_handlesDatesWithoutSeconds() {
        val incompleteDate = "2026-10-19T02:00Z"
        val normalized = DateTimeUtils.normalizeIsoDate(incompleteDate)
        assertEquals("2026-10-19T02:00:00Z", normalized)
    }

    @Test
    fun parseUtcEpochMillis_returnsCorrectTimestamp() {
        val dateUtc = "2026-10-19T02:00:00Z"
        val millis = DateTimeUtils.parseUtcEpochMillis(dateUtc)
        assertTrue(millis > 0L)
    }

    @Test
    fun parseUtcEpochMillis_handlesDatesWithoutSeconds() {
        val dateUtcWithoutSeconds = "2026-10-10T01:00Z"
        val dateUtcWithSeconds = "2026-10-10T01:00:00Z"
        val millisWithoutSeconds = DateTimeUtils.parseUtcEpochMillis(dateUtcWithoutSeconds)
        val millisWithSeconds = DateTimeUtils.parseUtcEpochMillis(dateUtcWithSeconds)
        assertTrue(millisWithoutSeconds > 0L)
        assertEquals(millisWithSeconds, millisWithoutSeconds)
    }

    @Test
    fun isToday_returnsTrueForNightMatchInMexicoEvenWhenUtcIsNextDay() {
        // En UTC es 10 de octubre a la 01:00, pero en Ciudad de Mexico son las 19:00 del 9 de octubre
        val dateUtc = "2026-10-10T01:00Z"
        val nowInstant = java.time.Instant.parse("2026-10-09T22:00:00Z") // 16:00 del 9 de octubre en Mexico
        val isToday = DateTimeUtils.isToday(dateUtc, mexicoZone, nowInstant)
        assertTrue(isToday)
    }

    @Test
    fun isToday_handlesDatesWithoutSecondsWithoutThrowing() {
        val dateUtc = "2026-10-10T03:00Z"
        val nowInstant = java.time.Instant.parse("2026-10-09T22:00:00Z")
        val isToday = DateTimeUtils.isToday(dateUtc, mexicoZone, nowInstant)
        assertTrue(isToday)
    }

    @Test
    fun isToday_returnsFalseForMatchOnDifferentDay() {
        val dateUtc = "2026-10-12T01:00Z"
        val nowInstant = java.time.Instant.parse("2026-10-09T22:00:00Z")
        val isToday = DateTimeUtils.isToday(dateUtc, mexicoZone, nowInstant)
        org.junit.Assert.assertFalse(isToday)
    }

    @Test
    fun isToday_returnsFalseForTomorrowMatchEvenWhenUtcIsNextDay() {
        // En Mexico son las 20:10 del 9 de octubre (en UTC ya son las 02:10 del 10 de octubre)
        val nowInstant = java.time.Instant.parse("2026-10-10T02:10:00Z")
        // Partido de manana en Mexico a las 17:00 (en UTC son las 23:00 del 10 de octubre)
        val tomorrowMatchUtc = "2026-10-10T23:00:00Z"
        val isToday = DateTimeUtils.isToday(tomorrowMatchUtc, mexicoZone, nowInstant)
        org.junit.Assert.assertFalse(isToday)
    }

    @Test
    fun isBeforeToday_returnsTrueForYesterdayMatch() {
        // En Mexico son las 20:00 del 13 de octubre
        val nowInstant = java.time.Instant.parse("2026-10-14T02:00:00Z")
        // Partido jugado el 12 de octubre
        val pastMatchUtc = "2026-10-12T19:00:00Z"
        val isBefore = DateTimeUtils.isBeforeToday(pastMatchUtc, mexicoZone, nowInstant)
        assertTrue(isBefore)
        org.junit.Assert.assertFalse(DateTimeUtils.isToday(pastMatchUtc, mexicoZone, nowInstant))
        org.junit.Assert.assertFalse(DateTimeUtils.isAfterToday(pastMatchUtc, mexicoZone, nowInstant))
    }

    @Test
    fun isAfterToday_returnsTrueForTomorrowMatch() {
        // En Mexico son las 20:00 del 13 de octubre
        val nowInstant = java.time.Instant.parse("2026-10-14T02:00:00Z")
        // Partido de Champions el 14 de octubre
        val tomorrowMatchUtc = "2026-10-14T19:00:00Z"
        val isAfter = DateTimeUtils.isAfterToday(tomorrowMatchUtc, mexicoZone, nowInstant)
        assertTrue(isAfter)
        org.junit.Assert.assertFalse(DateTimeUtils.isToday(tomorrowMatchUtc, mexicoZone, nowInstant))
        org.junit.Assert.assertFalse(DateTimeUtils.isBeforeToday(tomorrowMatchUtc, mexicoZone, nowInstant))
    }

    @Test
    fun getLocalDateKey_resolvesAccurateLocalDateAcrossTimezones() {
        val matchUtc = "2026-06-11T20:00:00Z"
        val localKey = DateTimeUtils.getLocalDateKey(matchUtc, mexicoZone)
        assertEquals("2026-06-11", localKey)
    }

    @Test
    fun formatDateHeader_formatsCorrectlyInSpanishAndEnglish() {
        val matchUtc = "2026-06-11T20:00:00Z"
        val spanishHeader = DateTimeUtils.formatDateHeader(
            dateUtc = matchUtc,
            zoneId = mexicoZone,
            locale = java.util.Locale.forLanguageTag("es-MX")
        )
        val englishHeader = DateTimeUtils.formatDateHeader(
            dateUtc = matchUtc,
            zoneId = mexicoZone,
            locale = java.util.Locale.US
        )

        assertTrue(spanishHeader.contains("11"))
        assertTrue(spanishHeader.contains("2026"))
        assertTrue(englishHeader.contains("11"))
        assertTrue(englishHeader.contains("2026"))
        assertTrue(englishHeader.contains("June") || englishHeader.contains("Thursday"))
    }
}
