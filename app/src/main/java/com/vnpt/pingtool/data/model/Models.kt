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
