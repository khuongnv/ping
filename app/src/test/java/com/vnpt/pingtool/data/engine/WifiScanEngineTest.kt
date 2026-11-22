package com.vnpt.pingtool.data.engine

import com.vnpt.pingtool.data.model.PasswordAuditCharset
import com.vnpt.pingtool.data.model.PasswordAuditConfig
import com.vnpt.pingtool.data.model.WifiSecurityType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WifiScanEngineTest {

    @Test
    fun testParseSecurityType_wpa3Sae() {
        val caps = "[WPA3-SAE-CCMP][RSN-SAE-CCMP][ESS]"
        assertEquals(WifiSecurityType.WPA3_SAE, WifiScanEngine.parseSecurityType(caps))
    }

    @Test
    fun testParseSecurityType_wpa2Psk() {
        val caps = "[WPA2-PSK-CCMP][ESS]"
        assertEquals(WifiSecurityType.WPA2_PSK, WifiScanEngine.parseSecurityType(caps))
    }

    @Test
    fun testParseSecurityType_wpaPsk() {
        val caps = "[WPA-PSK-TKIP][ESS]"
        assertEquals(WifiSecurityType.WPA_PSK, WifiScanEngine.parseSecurityType(caps))
    }

    @Test
    fun testParseSecurityType_wep() {
        val caps = "[WEP][ESS]"
        assertEquals(WifiSecurityType.WEP, WifiScanEngine.parseSecurityType(caps))
    }

    @Test
    fun testParseSecurityType_open() {
        val caps = "[ESS]"
        assertEquals(WifiSecurityType.OPEN, WifiScanEngine.parseSecurityType(caps))
    }

    @Test
    fun testParseSecurityType_enterprise() {
        val caps = "[WPA2-EAP-CCMP][ESS]"
        assertEquals(WifiSecurityType.EAP_ENTERPRISE, WifiScanEngine.parseSecurityType(caps))
    }

    @Test
    fun testEvaluateWifiSecurity_openWarning() {
        val (rating, warning) = WifiScanEngine.evaluateWifiSecurity(WifiSecurityType.OPEN, "[ESS]")
        assertTrue(rating.contains("Nguy hiểm"))
        assertNotNull(warning)
        assertTrue(warning!!.contains("không mã hóa"))
    }

    @Test
    fun testEvaluateWifiSecurity_wpa3Safe() {
        val (rating, warning) = WifiScanEngine.evaluateWifiSecurity(WifiSecurityType.WPA3_SAE, "[WPA3-SAE-CCMP]")
        assertTrue(rating.contains("Rất an toàn"))
        assertNull(warning)
    }

    @Test
    fun testCalculateSignalLevel() {
        assertEquals(4, WifiScanEngine.calculateSignalLevel(-45))
        assertEquals(3, WifiScanEngine.calculateSignalLevel(-60))
        assertEquals(2, WifiScanEngine.calculateSignalLevel(-72))
        assertEquals(1, WifiScanEngine.calculateSignalLevel(-82))
        assertEquals(0, WifiScanEngine.calculateSignalLevel(-95))
    }

    @Test
    fun testFrequencyToChannel() {
        assertEquals(1, WifiScanEngine.frequencyToChannel(2412))
        assertEquals(6, WifiScanEngine.frequencyToChannel(2437))
        assertEquals(11, WifiScanEngine.frequencyToChannel(2462))
        assertEquals(14, WifiScanEngine.frequencyToChannel(2484))
        assertEquals(36, WifiScanEngine.frequencyToChannel(5180))
    }

    @Test
    fun testEvaluatePasswordStrength_commonPassword() {
        val result = WifiScanEngine.evaluatePasswordStrength("12345678")
        assertTrue(result.isCommon)
        assertEquals(1, result.dictionaryRank)
        assertEquals("Cực kỳ yếu", result.ratingLabel)
        assertTrue(result.recommendations.any { it.contains("TOP mật khẩu phổ biến") })
    }

    @Test
    fun testEvaluatePasswordStrength_strongPassword() {
        val result = WifiScanEngine.evaluatePasswordStrength("Vnpt@Secure2026!#$")
        assertFalse(result.isCommon)
        assertTrue(result.hasDigits)
        assertTrue(result.hasLetters)
        assertTrue(result.hasUpper)
        assertTrue(result.hasSpecial)
        assertTrue(result.entropyBits > 70.0)
        assertEquals("Rất mạnh", result.ratingLabel)
    }

    @Test
    fun testAuditWifiPassword_simulationDigitsOnly() = runBlocking {
        val config = PasswordAuditConfig(
            targetSsid = "Test_WiFi",
            minLength = 8,
            maxLength = 8,
            charset = PasswordAuditCharset.DIGITS_ONLY,
            maxAttempts = 100
        )

        val flowResults = WifiScanEngine.auditWifiPassword(config, targetPasswordForSim = "12345678").toList()
        assertTrue(flowResults.isNotEmpty())

        val lastStatus = flowResults.last().first
        assertTrue(lastStatus.isFinished)
        assertEquals("12345678", lastStatus.matchedPassword)
    }

    @Test
    fun testAuditWifiPassword_withoutTargetPassword_doesNotFakeMatch() = runBlocking {
        val config = PasswordAuditConfig(
            targetSsid = "VNPT_WiFi_5G",
            minLength = 8,
            maxLength = 8,
            charset = PasswordAuditCharset.DIGITS_ONLY,
            maxAttempts = 10
        )

        val flowResults = WifiScanEngine.auditWifiPassword(config, targetPasswordForSim = null).toList()
        assertTrue(flowResults.isNotEmpty())

        val lastStatus = flowResults.last().first
        assertTrue(lastStatus.isFinished)
        // Khi không nhập mật khẩu mục tiêu, TUYỆT ĐỐI không tự động báo khớp 12345678!
        assertNull(lastStatus.matchedPassword)
        assertTrue(lastStatus.summaryResult.contains("HOÀN TẤT KIỂM ĐỊNH"))
    }

    @Test
    fun testAuditWifiPassword_matchesSpecificPassword() = runBlocking {
        val config = PasswordAuditConfig(
            targetSsid = "VNPT_WiFi_5G",
            minLength = 8,
            maxLength = 8,
            charset = PasswordAuditCharset.ALPHANUMERIC,
            maxAttempts = 100
        )

        val flowResults = WifiScanEngine.auditWifiPassword(config, targetPasswordForSim = "admin123").toList()
        assertTrue(flowResults.isNotEmpty())

        val lastStatus = flowResults.last().first
        assertTrue(lastStatus.isFinished)
        assertEquals("admin123", lastStatus.matchedPassword)
    }
}
