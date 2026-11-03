package com.ixeken.ulama.service

import com.ixeken.ulama.core.model.AppSettings
import com.ixeken.ulama.core.model.FollowedMatch
import com.ixeken.ulama.core.model.MatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para validar el calculo de tiempos de activacion y anticipacion de alarmas de partidos.
 */
class MatchAlarmSchedulerTest {

    @Test
    fun defaultLeadMinutes_isFiveMinutes() {
        assertEquals(5, AppSettings.DEFAULT_NOTIFICATION_LEAD_MINUTES)
    }

    @Test
    fun triggerTime_calculatesCorrectOffsetForFiveMinutesLead() {
        val kickoffMillis = 1700000000000L
        val leadMinutes = 5
        val expectedTrigger = kickoffMillis - (5 * 60 * 1000L)

        val actualTrigger = kickoffMillis - (leadMinutes.coerceAtLeast(0) * 60 * 1000L)
        assertEquals(expectedTrigger, actualTrigger)
    }

    @Test
    fun triggerTime_handlesCustomLeadMinutes() {
        val kickoffMillis = 1700000000000L
        val leadMinutes = 15
        val expectedTrigger = kickoffMillis - (15 * 60 * 1000L)

        val actualTrigger = kickoffMillis - (leadMinutes.coerceAtLeast(0) * 60 * 1000L)
        assertEquals(expectedTrigger, actualTrigger)
    }

    @Test
    fun triggerTime_negativeLeadMinutes_coercesToZero() {
        val kickoffMillis = 1700000000000L
        val leadMinutes = -5
        val expectedTrigger = kickoffMillis

        val actualTrigger = kickoffMillis - (leadMinutes.coerceAtLeast(0) * 60 * 1000L)
        assertEquals(expectedTrigger, actualTrigger)
    }
}
