package com.vnpt.pingtool.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiConnectionManagerTest {

    @Test
    fun testFormatWifiQrString_wpa() {
        val qr = WifiConnectionManager.formatWifiQrString(
            ssid = "MyHome_WiFi",
            password = "password123",
            securityType = "WPA2_PSK"
        )
        assertEquals("WIFI:S:MyHome_WiFi;T:WPA;P:password123;;", qr)
    }

    @Test
    fun testFormatWifiQrString_specialCharactersEscaped() {
        val qr = WifiConnectionManager.formatWifiQrString(
            ssid = "VNPT:Office;5G",
            password = "p;a:s,s\\123",
            securityType = "WPA"
        )
        assertTrue(qr.contains("VNPT\\:Office\\;5G"))
        assertTrue(qr.contains("p\\;a\\:s\\,s\\\\123"))
    }

    @Test
    fun testFormatWifiQrString_openNetwork() {
        val qr = WifiConnectionManager.formatWifiQrString(
            ssid = "FreeCoffee",
            password = "",
            securityType = "OPEN"
        )
        assertEquals("WIFI:S:FreeCoffee;T:nopass;;", qr)
    }

    @Test
    fun testFormatWifiQrString_wep() {
        val qr = WifiConnectionManager.formatWifiQrString(
            ssid = "LegacyNet",
            password = "1234567890",
            securityType = "WEP"
        )
        assertEquals("WIFI:S:LegacyNet;T:WEP;P:1234567890;;", qr)
    }
}
