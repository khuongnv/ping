package com.vnpt.pingtool.data.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.vnpt.pingtool.data.model.NetworkCardInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

object NetworkInfoService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun getLocalNetworkInfo(context: Context): NetworkCardInfo {
        var wifiIp = "127.0.0.1"
        var wifiSsid = "Disconnected"
        var gateway = "N/A"
        var subnetMask = "255.255.255.0"
        var dnsList = "8.8.8.8"
        var isVpn = false
        var isWifi = false
        var needsLocationPerm = false

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            val linkProperties = cm?.getLinkProperties(activeNetwork)
            if (linkProperties != null) {
                // Gateway
                val routes = linkProperties.routes
                for (route in routes) {
                    if (route.isDefaultRoute && route.gateway != null) {
                        gateway = route.gateway?.hostAddress ?: "N/A"
                        break
                    }
                }

                // DNS
                dnsList = linkProperties.dnsServers.joinToString(", ") { it.hostAddress ?: "" }
            }

            // Find local IPv4 address
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        wifiIp = addr.hostAddress ?: wifiIp
                        // Infer prefix length
                        for (ifaceAddr in iface.interfaceAddresses) {
                            if (ifaceAddr.address == addr) {
                                val prefix = ifaceAddr.networkPrefixLength
                                subnetMask = prefixLengthToSubnet(prefix.toInt())
                            }
                        }
                        break
                    }
                }
            }

            if (isWifi) {
                var detectedSsid = ""

                // 1. Android 10+ (API 29+) via NetworkCapabilities TransportInfo
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val transportWifiInfo = caps?.transportInfo as? WifiInfo
                    val s = transportWifiInfo?.ssid?.replace("\"", "")
                    if (!s.isNullOrBlank() && s != "<unknown ssid>") {
                        detectedSsid = s
                    }
                }

                // 2. WifiManager connectionInfo
                if (detectedSsid.isBlank()) {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    @Suppress("DEPRECATION")
                    val s = wifiManager?.connectionInfo?.ssid?.replace("\"", "")
                    if (!s.isNullOrBlank() && s != "<unknown ssid>") {
                        detectedSsid = s
                    }
                }

                val hasFineLocation = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                val hasCoarseLocation = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (detectedSsid.isNotBlank()) {
                    wifiSsid = detectedSsid
                } else if (!hasFineLocation && !hasCoarseLocation) {
                    wifiSsid = "Wi-Fi (Chạm để cấp quyền hiện tên)"
                    needsLocationPerm = true
                } else {
                    wifiSsid = "Đã kết nối Wi-Fi (Bật GPS để hiện tên)"
                }
            }
        } catch (_: Exception) {}

        return NetworkCardInfo(
            wifiIp = wifiIp,
            wifiSsid = wifiSsid,
            gateway = gateway,
            subnetMask = subnetMask,
            dnsServers = if (dnsList.isBlank()) "8.8.8.8, 1.1.1.1" else dnsList,
            isVpnActive = isVpn,
            isWifiTransport = isWifi,
            needsLocationPermission = needsLocationPerm
        )
    }

    suspend fun fetchPublicIpInfo(): Triple<String, String, String> = withContext(Dispatchers.IO) {
        try {
            // Using ip-api or ipapi.co (free JSON endpoint)
            val request = Request.Builder()
                .url("http://ip-api.com/json/?fields=status,country,query,isp")
                .header("User-Agent", "PingApp/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val ip = json.optString("query", "Unknown")
                    val isp = json.optString("isp", "Internet Service Provider")
                    val country = json.optString("country", "Global")
                    return@withContext Triple(ip, isp, country)
                }
            }
        } catch (_: Exception) {
            // Fallback to simple ifconfig.me
            try {
                val request = Request.Builder()
                    .url("https://api.ipify.org?format=json")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val ip = json.optString("ip", "127.0.0.1")
                        return@withContext Triple(ip, "Internet Provider", "Global")
                    }
                }
            } catch (_: Exception) {}
        }
        Triple("127.0.0.1", "Internet Provider", "Global")
    }

    private fun prefixLengthToSubnet(prefixLength: Int): String {
        var mask = -1 shl (32 - prefixLength)
        return "${(mask ushr 24) and 0xff}.${(mask ushr 16) and 0xff}.${(mask ushr 8) and 0xff}.${mask and 0xff}"
    }
}
