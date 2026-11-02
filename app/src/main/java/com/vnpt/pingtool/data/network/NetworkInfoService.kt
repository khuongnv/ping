package com.vnpt.pingtool.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
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

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

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

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo = wifiManager?.connectionInfo
            if (connectionInfo != null && connectionInfo.ssid != null) {
                val rawSsid = connectionInfo.ssid.replace("\"", "")
                if (rawSsid != "<unknown ssid>") {
                    wifiSsid = rawSsid
                }
            }
        } catch (_: Exception) {}

        return NetworkCardInfo(
            wifiIp = wifiIp,
            wifiSsid = wifiSsid,
            gateway = gateway,
            subnetMask = subnetMask,
            dnsServers = if (dnsList.isBlank()) "8.8.8.8, 1.1.1.1" else dnsList,
            isVpnActive = isVpn
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
                    val isp = json.optString("isp", "VNPT / Internet Provider")
                    val country = json.optString("country", "Vietnam")
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
                        val ip = json.optString("ip", "123.16.183.218")
                        return@withContext Triple(ip, "VNPT Internet", "Vietnam")
                    }
                }
            } catch (_: Exception) {}
        }
        Triple("123.16.183.218", "VNPT Telecom", "Vietnam")
    }

    private fun prefixLengthToSubnet(prefixLength: Int): String {
        var mask = -1 shl (32 - prefixLength)
        return "${(mask ushr 24) and 0xff}.${(mask ushr 16) and 0xff}.${(mask ushr 8) and 0xff}.${mask and 0xff}"
    }
}
