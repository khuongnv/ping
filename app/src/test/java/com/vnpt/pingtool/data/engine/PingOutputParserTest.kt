package com.vnpt.pingtool.data.engine

import com.vnpt.pingtool.data.engine.PingOutputParser.ProbeResult
import com.vnpt.pingtool.ui.viewmodel.PingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingOutputParserTest {

    @Test
    fun parseHeader_standardDomain() {
        val line = "PING google.com (142.250.71.142) 56(84) bytes of data."
        val header = PingOutputParser.parseHeader(line)
        assertNotNull(header)
        assertEquals("google.com", header?.host)
        assertEquals("142.250.71.142", header?.ip)
    }

    @Test
    fun parseReply_numericIp() {
        val line = "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=14.2 ms"
        val reply = PingOutputParser.parseReply(line)
        assertNotNull(reply)
        assertEquals(64, reply?.bytes)
        assertEquals("8.8.8.8", reply?.ip)
        assertEquals(1, reply?.sequence)
        assertEquals(117, reply?.ttl)
        assertEquals(14.2f, reply?.timeMs ?: 0f, 0.01f)
    }

    @Test
    fun parseReply_withReverseDnsDomain() {
        val line = "64 bytes from dns.google (8.8.8.8): icmp_seq=1 ttl=117 time=25.4 ms"
        val reply = PingOutputParser.parseReply(line)
        assertNotNull(reply)
        assertEquals("8.8.8.8", reply?.ip)
        assertEquals(25.4f, reply?.timeMs ?: 0f, 0.01f)
    }

    @Test
    fun parseSingleProbe_successfulProbe() {
        val output = """
            PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.
            64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=12.5 ms

            --- 8.8.8.8 ping statistics ---
            1 packets transmitted, 1 received, 0% packet loss, time 0ms
            rtt min/avg/max/mdev = 12.500/12.500/12.500/0.000 ms
        """.trimIndent()

        val result = PingOutputParser.parseSingleProbe(output)
        assertTrue(result is ProbeResult.Success)
        val success = result as ProbeResult.Success
        assertEquals("8.8.8.8", success.reply.ip)
        assertEquals(12.5f, success.reply.timeMs, 0.01f)
        assertEquals("8.8.8.8", success.header?.ip)
    }

    @Test
    fun parseSingleProbe_timeout() {
        val output = """
            PING 192.0.2.1 (192.0.2.1) 56(84) bytes of data.

            --- 192.0.2.1 ping statistics ---
            1 packets transmitted, 0 received, 100% packet loss, time 0ms
        """.trimIndent()

        val result = PingOutputParser.parseSingleProbe(output)
        assertTrue(result is ProbeResult.Timeout)
        assertEquals("192.0.2.1", result.header?.ip)
    }

    @Test
    fun parseSingleProbe_icmpError() {
        val output = """
            PING 10.254.254.254 (10.254.254.254) 56(84) bytes of data.
            From 192.168.1.1 icmp_seq=1 Destination Host Unreachable

            --- 10.254.254.254 ping statistics ---
            1 packets transmitted, 0 errors, 100% packet loss, time 0ms
        """.trimIndent()

        val result = PingOutputParser.parseSingleProbe(output)
        assertTrue(result is ProbeResult.IcmpError)
        val err = result as ProbeResult.IcmpError
        assertEquals("192.168.1.1", err.fromIp)
    }

    @Test
    fun sanitizeHost_variousFormats() {
        assertEquals("8.8.8.8", PingViewModel.sanitizeHost("https://8.8.8.8/test?query=1"))
        assertEquals("google.com", PingViewModel.sanitizeHost("http://google.com:8080/path"))
        assertEquals("1.1.1.1", PingViewModel.sanitizeHost("  1.1.1.1:443  "))
        assertEquals("cloudflare.com", PingViewModel.sanitizeHost("cloudflare.com"))
        assertEquals(PingViewModel.DEFAULT_HOST, PingViewModel.sanitizeHost(""))
    }
}
