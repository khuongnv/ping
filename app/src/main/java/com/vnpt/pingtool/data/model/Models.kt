package com.vnpt.pingtool.data.model

data class PingPacket(
    val sequence: Int,
    val bytes: Int = 64,
    val ip: String,
    val ttl: Int,
    val timeMs: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val rawLog: String = ""
)

data class PingSessionStats(
    val targetHost: String,
    val resolvedIp: String = "",
    val sentCount: Int = 0,
    val receivedCount: Int = 0,
    val minMs: Float = 0f,
    val avgMs: Float = 0f,
    val maxMs: Float = 0f,
    val lossPercentage: Float = 0f,
    val packets: List<PingPacket> = emptyList(),
    val isRunning: Boolean = false
)

data class TracerouteHop(
    val hopNumber: Int,
    val ip: String,
    val hostname: String = "",
    val timeMs: Float,
    val isTimeout: Boolean = false
)

data class NetworkCardInfo(
    val wifiIp: String = "127.0.0.1",
    val wifiSsid: String = "Not connected",
    val gateway: String = "N/A",
    val subnetMask: String = "255.255.255.0",
    val dnsServers: String = "8.8.8.8",
    val publicIp: String = "Loading...",
    val isp: String = "Loading...",
    val country: String = "Loading...",
    val isVpnActive: Boolean = false,
    val isWifiTransport: Boolean = false,
    val needsLocationPermission: Boolean = false
) {
    val isWifiConnected: Boolean
        get() = isWifiTransport || (wifiSsid.isNotBlank() &&
                wifiSsid != "Disconnected" &&
                wifiSsid != "Not connected" &&
                wifiIp != "127.0.0.1" &&
                wifiIp.isNotBlank())
}

data class PingConfig(
    val packetSize: Int = 56, // Payload bytes, 56 + 8 ICMP header = 64 bytes
    val intervalMs: Long = 1000L,
    val timeoutSec: Int = 3,
    val packetCount: Int = 0, // 0 means continuous
    val ttl: Int = 64
)

data class UpdateReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val apkDownloadUrl: String,
    val publishedAt: String,
    val hasNewVersion: Boolean
)
