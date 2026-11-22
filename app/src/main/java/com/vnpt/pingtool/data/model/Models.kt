package com.vnpt.pingtool.data.model

import androidx.compose.runtime.Immutable

/**
 * Một sự kiện trong phiên ping.
 * - [isInfo] = true: dòng thông tin (header "PING host (ip)..."), KHÔNG được tính vào thống kê gói tin.
 * - [isSuccess] = true: nhận được phản hồi ICMP Echo Reply thực tế.
 */
@Immutable
data class PingPacket(
    val sequence: Int,
    val bytes: Int = 0,
    val ip: String,
    val ttl: Int = 0,
    val timeMs: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = false,
    val isInfo: Boolean = false,
    val rawLog: String = ""
)

@Immutable
data class PingSessionStats(
    val targetHost: String,
    val resolvedIp: String = "",
    val startedAt: Long = 0L,
    val sentCount: Int = 0,
    val receivedCount: Int = 0,
    val minMs: Float = 0f,
    val avgMs: Float = 0f,
    val maxMs: Float = 0f,
    val jitterMs: Float = 0f,
    val lossPercentage: Float = 0f,
    /** Ring-buffer các gói gần nhất (tối đa [MAX_BUFFERED_PACKETS]) phục vụ biểu đồ và nhật ký. */
    val packets: List<PingPacket> = emptyList(),
    val isRunning: Boolean = false
) {
    companion object {
        const val MAX_BUFFERED_PACKETS = 100
    }
}

@Immutable
data class TracerouteHop(
    val hopNumber: Int,
    val ip: String,
    val timeMs: Float = 0f,
    val isTimeout: Boolean = false,
    /** true nếu đây là đích cuối (RTT lấy chính xác từ ICMP Echo Reply). */
    val isDestination: Boolean = false
)

enum class ConnectionType(val label: String) {
    NONE("Không có kết nối"),
    WIFI("Wi-Fi"),
    CELLULAR("Dữ liệu di động"),
    ETHERNET("Ethernet"),
    VPN("VPN"),
    OTHER("Khác")
}

enum class PublicInfoState { LOADING, SUCCESS, ERROR }

/**
 * Thông tin mạng thực tế đọc từ thiết bị. Chuỗi rỗng nghĩa là KHÔNG xác định được
 * (UI hiển thị trạng thái tương ứng thay vì giá trị giả lập).
 */
@Immutable
data class NetworkCardInfo(
    val connectionType: ConnectionType = ConnectionType.NONE,
    val localIp: String = "",
    val wifiSsid: String = "",
    /** Lý do không hiển thị được SSID (thiếu quyền, tắt định vị...). */
    val ssidHint: String = "",
    val gateway: String = "",
    val subnetMask: String = "",
    val dnsServers: String = "",
    val isVpnActive: Boolean = false,
    val needsLocationPermission: Boolean = false,
    val publicIp: String = "",
    val isp: String = "",
    val country: String = "",
    val publicInfoState: PublicInfoState = PublicInfoState.LOADING,
    val isLocalLoaded: Boolean = false
) {
    val isWifiConnected: Boolean
        get() = connectionType == ConnectionType.WIFI
}

data class PublicIpInfo(
    val ip: String,
    val isp: String,
    val country: String
)

@Immutable
data class PingConfig(
    val packetSize: Int = 56, // Payload bytes, 56 + 8 ICMP header = 64 bytes
    val intervalMs: Long = 1000L,
    val timeoutSec: Int = 3,
    val packetCount: Int = 0 // 0 means continuous
)

@Immutable
data class UpdateReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val apkDownloadUrl: String,
    val publishedAt: String,
    val hasNewVersion: Boolean
)

enum class TelnetPortStatus(val label: String) {
    OPEN("Mở"),
    CLOSED("Đóng"),
    TIMEOUT("Hết giờ"),
    FILTERED("Bị chặn"),
    ERROR("Lỗi")
}

@Immutable
data class TelnetPortResult(
    val port: Int,
    val serviceName: String = "",
    val status: TelnetPortStatus,
    val timeMs: Float = 0f,
    val errorDetail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class TelnetScanSummary(
    val targetHost: String = "",
    val totalPorts: Int = 0,
    val scannedPorts: Int = 0,
    val openCount: Int = 0,
    val closedCount: Int = 0,
    val timeoutCount: Int = 0,
    val isScanning: Boolean = false
) {
    val progress: Float
        get() = if (totalPorts > 0) scannedPorts.toFloat() / totalPorts else 0f
}

@Immutable
data class TelnetSessionRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetHost: String,
    val portInput: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalPorts: Int = 0,
    val openCount: Int = 0,
    val closedCount: Int = 0,
    val timeoutCount: Int = 0,
    val results: List<TelnetPortResult> = emptyList()
)

// ==========================================
// WIFI INSPECTOR & PASSWORD AUDITOR MODELS
// ==========================================

enum class WifiSecurityType(val label: String, val badgeColor: Long) {
    WPA3_SAE("WPA3-Personal", 0xFF00E676),
    WPA2_PSK("WPA2-PSK", 0xFF00E5FF),
    WPA_PSK("WPA-PSK", 0xFFFFD600),
    WEP("WEP (Kém)", 0xFFFF9100),
    EAP_ENTERPRISE("WPA-Enterprise", 0xFF7C4DFF),
    OPEN("Mạng mở (Nguy hiểm)", 0xFFFF3B30),
    UNKNOWN("Không xác định", 0xFF8E8E93)
}

@Immutable
data class WifiScanItem(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val level: Int, // 0..4 vạch
    val frequencyMhz: Int,
    val channel: Int,
    val bandLabel: String, // 2.4 GHz, 5 GHz, 6 GHz
    val capabilities: String,
    val securityType: WifiSecurityType,
    val securityRating: String,
    val securityWarning: String? = null,
    val isConnected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PasswordAuditCharset(val label: String) {
    ALL("Tất cả từ điển"),
    DIGITS_ONLY("Chỉ số (0-9)"),
    ALPHANUMERIC("Chữ & Số (a-z, 0-9)"),
    COMPLEX("Kèm ký tự đặc biệt")
}

@Immutable
data class DictionaryItem(
    val password: String,
    val level: Int = 1,
    val type: String = "Number" // "Number", "Number + String", "Complex"
)

@Immutable
data class PasswordAuditConfig(
    val targetSsid: String = "",
    val minLength: Int = 8,
    val maxLength: Int = 12,
    val charset: PasswordAuditCharset = PasswordAuditCharset.ALL,
    val maxLevel: Int = 50, // Cấp độ khó từ điển: 1 đến 99
    val maxAttempts: Int = 2000,
    val useOnlineDictionary: Boolean = true
)

@Immutable
data class PasswordAuditStatus(
    val isAuditing: Boolean = false,
    val targetSsid: String = "",
    val currentAttempt: Int = 0,
    val totalCandidates: Int = 0,
    val attemptsPerSec: Int = 0,
    val currentCandidate: String = "",
    val currentLevel: Int = 1,
    val matchedPassword: String? = null,
    val matchedLevel: Int? = null,
    val isFinished: Boolean = false,
    val summaryResult: String = ""
) {
    val progress: Float
        get() = if (totalCandidates > 0) (currentAttempt.toFloat() / totalCandidates).coerceIn(0f, 1f) else 0f
}

enum class AuditLogLevel { INFO, WARN, ALERT, SUCCESS }

@Immutable
data class AuditLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val message: String,
    val level: AuditLogLevel = AuditLogLevel.INFO
)

@Immutable
data class PasswordAuditMatchRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val ssid: String,
    val matchedPassword: String,
    val level: Int,
    val levelLabel: String = "Cấp độ $level",
    val timestamp: Long = System.currentTimeMillis(),
    val speedAttemptsPerSec: Int = 0,
    val attemptsCount: Int = 0,
    val riskRating: String = "Nguy hiểm"
)

@Immutable
data class PasswordStrengthResult(
    val password: String,
    val length: Int,
    val hasDigits: Boolean,
    val hasLetters: Boolean,
    val hasUpper: Boolean,
    val hasSpecial: Boolean,
    val isCommon: Boolean,
    val dictionaryRank: Int? = null,
    val entropyBits: Double,
    val estimatedCrackTime: String,
    val ratingLabel: String,
    val ratingColor: Long,
    val recommendations: List<String>
)

