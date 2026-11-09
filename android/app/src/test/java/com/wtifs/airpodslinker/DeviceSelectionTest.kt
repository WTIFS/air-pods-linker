package com.wtifs.airpodslinker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceSelectionTest {
    private val old = PairedDevice("A", "AirPods", false)
    private val recent = PairedDevice("B", "AirPods Pro", false)

    @Test fun recentOverridesSavedAndSurvivesDisconnect() {
        assertEquals("B", selectRecentDevice(listOf(old, recent), "A", listOf("B", "A")))
        assertEquals(listOf("B", "A"), recordConnections(listOf("B", "A"), emptySet(), setOf("B")))
    }

    @Test fun activeOverridesHistory() {
        assertEquals("A", selectRecentDevice(listOf(old.copy(connected = true), recent), "B", listOf("B", "A")))
    }

    @Test fun newConnectionMovesToFrontButRefreshDoesNot() {
        val history = recordConnections(listOf("A", "B"), setOf("A", "B"), setOf("A"))
        assertEquals(listOf("B", "A"), history)
        assertEquals(history, recordConnections(history, setOf("A", "B"), setOf("A", "B")))
        assertEquals("B", selectRecentDevice(listOf(old.copy(connected = true), recent.copy(connected = true)), "A", history))
    }

    @Test fun renamedKnownDeviceRemainsEligibleAndUnpairedHistoryIsIgnored() {
        assertEquals("B", selectRecentDevice(listOf(old, recent.copy(name = "Headphones")), "A", listOf("B")))
        assertEquals("A", selectRecentDevice(listOf(old), "B", listOf("B")))
        assertNull(selectRecentDevice(listOf(PairedDevice("C", "Keyboard", true)), "B", listOf("B")))
    }

    @Test fun unknownHistoryUsesSavedThenStableFallback() {
        assertEquals("B", selectRecentDevice(listOf(old, recent), "B", emptyList()))
        assertEquals("A", selectRecentDevice(listOf(recent, old), null, emptyList()))
    }
}
