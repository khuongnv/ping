package com.vnpt.pingtool.data.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelnetEngineTest {

    @Test
    fun parsePortInput_singlePort() {
        val result = TelnetEngine.parsePortInput("80")
        assertEquals(listOf(80), result)
    }

    @Test
    fun parsePortInput_commaSeparated() {
        val result = TelnetEngine.parsePortInput("80, 443, 8080")
        assertEquals(listOf(80, 443, 8080), result)
    }

    @Test
    fun parsePortInput_range() {
        val result = TelnetEngine.parsePortInput("80-84")
        assertEquals(listOf(80, 81, 82, 83, 84), result)
    }

    @Test
    fun parsePortInput_mixedAndDeduplicated() {
        val result = TelnetEngine.parsePortInput("22, 80-82, 443, 80, 8080")
        assertEquals(listOf(22, 80, 81, 82, 443, 8080), result)
    }

    @Test
    fun parsePortInput_outOfBoundsIgnored() {
        val result = TelnetEngine.parsePortInput("0, 80, 70000, -5, 443")
        assertEquals(listOf(80, 443), result)
    }

    @Test
    fun parsePortInput_maxBatchLimit() {
        val result = TelnetEngine.parsePortInput("1-1000")
        assertEquals(TelnetEngine.MAX_PORTS_PER_BATCH, result.size)
        assertEquals(1, result.first())
        assertEquals(TelnetEngine.MAX_PORTS_PER_BATCH, result.last())
    }

    @Test
    fun getServiceName_commonPorts() {
        assertEquals("HTTP", TelnetEngine.getServiceName(80))
        assertEquals("HTTPS", TelnetEngine.getServiceName(443))
        assertEquals("SSH", TelnetEngine.getServiceName(22))
        assertEquals("Telnet", TelnetEngine.getServiceName(23))
        assertEquals("MySQL", TelnetEngine.getServiceName(3306))
        assertEquals("RDP Remote Desktop", TelnetEngine.getServiceName(3389))
        assertTrue(TelnetEngine.getServiceName(54321).isEmpty())
    }
}
