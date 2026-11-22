package com.vnpt.pingtool.data.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.vnpt.pingtool.data.model.ConnectionType
import com.vnpt.pingtool.data.model.NetworkCardInfo
import com.vnpt.pingtool.data.model.PublicInfoState
import com.vnpt.pingtool.data.model.PublicIpInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.Inet6Address
import java.util.Locale
import java.util.concurrent.TimeUnit

object NetworkInfoService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT = "PingApp-Android"

    /**
     * Đọc thông tin mạng cục bộ thực tế. Khi có VPN, ưu tiên lấy thông tin từ mạng vật lý bên dưới
     * (Wi-Fi / Ethernet / Di động) để hiển thị đúng IP LAN, Gateway, Subnet.
     */
    suspend fun getLocalNetworkInfo(context: Context): NetworkCardInfo = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@withContext NetworkCardInfo(isLocalLoaded = true, publicInfoState = PublicInfoState.ERROR)

        val activeNetwork = cm.activeNetwork
        val activeCaps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isVpn = activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        val (network, caps) = selectPhysicalNetwork(cm, activeNetwork, activeCaps)
        val linkProperties: LinkProperties? = network?.let { cm.getLinkProperties(it) }

        val connectionType = when {
            caps == null -> if (isVpn) ConnectionType.VPN else ConnectionType.NONE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
            else -> ConnectionType.OTHER
        }

        var localIp = ""
        var subnetMask = ""
        var gateway = ""
        var dnsServers = ""

        if (linkProperties != null) {
            // IP + Subnet: ưu tiên IPv4, nếu mạng chỉ có IPv6 thì dùng địa chỉ IPv6 toàn cục
            val linkAddresses = linkProperties.linkAddresses
            val v4 = linkAddresses.firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
            val v6 = linkAddresses.firstOrNull {
                val a = it.address
                a is Inet6Address && !a.isLinkLocalAddress && !a.isLoopbackAddress
            }
            when {
                v4 != null -> {
                    localIp = v4.address.hostAddress.orEmpty()
                    subnetMask = prefixLengthToSubnet(v4.prefixLength)
                }
                v6 != null -> {
                    localIp = v6.address.hostAddress.orEmpty().substringBefore('%')
                    subnetMask = "/${v6.prefixLength}"
                }
            }

            // Gateway: route mặc định có địa chỉ gateway thực (ưu tiên IPv4)
            val defaultGateways = linkProperties.routes
                .filter { it.isDefaultRoute }
                .mapNotNull { it.gateway }
                .filter { !it.isAnyLocalAddress }
            gateway = (defaultGateways.firstOrNull { it is Inet4Address } ?: defaultGateways.firstOrNull())
                ?.hostAddress.orEmpty().substringBefore('%')

            // DNS servers thực tế của kết nối + Private DNS (DoT) nếu đang bật
            val dnsList = linkProperties.dnsServers.mapNotNull { it.hostAddress?.substringBefore('%') }
                .filter { it.isNotBlank() }
            dnsServers = dnsList.joinToString(", ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && linkProperties.isPrivateDnsActive) {
                val name = linkProperties.privateDnsServerName
                val dot = if (!name.isNullOrBlank()) "Private DNS: $name" else "Private DNS (tự động)"
                dnsServers = if (dnsServers.isBlank()) dot else "$dnsServers\n$dot"
            }
        }

        var ssid = ""
        var ssidHint = ""
        var needsLocationPerm = false
        if (connectionType == ConnectionType.WIFI) {
            ssid = readSsid(context, caps)
            if (ssid.isBlank()) {
                val hasLocationPerm = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val locationOn = lm?.let { LocationManagerCompat.isLocationEnabled(it) } == true
                when {
                    !hasLocationPerm -> {
                        needsLocationPerm = true
                        ssidHint = "Cần cấp quyền vị trí để đọc tên Wi-Fi"
                    }
                    !locationOn -> ssidHint = "Bật Vị trí (GPS) để đọc tên Wi-Fi"
                    else -> ssidHint = "Hệ thống không cung cấp tên Wi-Fi"
                }
            }
        }

        NetworkCardInfo(
            connectionType = connectionType,
            localIp = localIp,
            wifiSsid = ssid,
            ssidHint = ssidHint,
            gateway = gateway,
            subnetMask = subnetMask,
            dnsServers = dnsServers,
            isVpnActive = isVpn,
            needsLocationPermission = needsLocationPerm,
            isLocalLoaded = true
        )
    }

    @Suppress("DEPRECATION")
    private fun selectPhysicalNetwork(
        cm: ConnectivityManager,
        activeNetwork: Network?,
        activeCaps: NetworkCapabilities?
    ): Pair<Network?, NetworkCapabilities?> {
        if (activeCaps != null && !activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return activeNetwork to activeCaps
        }
        // Mạng đang hoạt động là VPN (hoặc null) -> tìm mạng vật lý có Internet
        val candidates = cm.allNetworks.mapNotNull { n ->
            cm.getNetworkCapabilities(n)?.let { c -> n to c }
        }.filter { (_, c) ->
            !c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        val preferred = candidates.firstOrNull { it.second.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }
            ?: candidates.firstOrNull { it.second.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) }
            ?: candidates.firstOrNull { it.second.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) }
            ?: candidates.firstOrNull()
        return preferred ?: (activeNetwork to activeCaps)
    }

    private fun readSsid(context: Context, caps: NetworkCapabilities?): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fromCaps = cleanSsid((caps?.transportInfo as? WifiInfo)?.ssid)
            if (fromCaps.isNotBlank()) return fromCaps
        }
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        return cleanSsid(wifiManager?.connectionInfo?.ssid)
    }

    private fun cleanSsid(raw: String?): String {
        val s = raw?.trim()?.removeSurrounding("\"").orEmpty()
        return if (s.isBlank() || s.equals("<unknown ssid>", ignoreCase = true) || s == "0x") "" else s
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Truy vấn Public IP / ISP / Quốc gia qua các API HTTPS (theo thứ tự dự phòng).
     * Trả về null nếu tất cả nguồn đều thất bại — tuyệt đối không trả về dữ liệu giả.
     */
    suspend fun fetchPublicIpInfo(): PublicIpInfo? = withContext(Dispatchers.IO) {
        fetchFromIpWhoIs() ?: fetchFromIpInfo() ?: fetchFromIpify()
    }

    private fun getJson(url: String): JSONObject? = try {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null
            else response.body?.string()?.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
        }
    } catch (_: Exception) {
        null
    }

    private fun fetchFromIpWhoIs(): PublicIpInfo? {
        val json = getJson("https://ipwho.is/") ?: return null
        if (!json.optBoolean("success", false)) return null
        val ip = json.optString("ip").takeIf { isValidIp(it) } ?: return null
        val connection = json.optJSONObject("connection")
        val isp = connection?.optString("isp").orEmpty().ifBlank { connection?.optString("org").orEmpty() }
        val country = countryName(json.optString("country_code"), json.optString("country"))
        return PublicIpInfo(ip = ip, isp = isp, country = country)
    }

    private fun fetchFromIpInfo(): PublicIpInfo? {
        val json = getJson("https://ipinfo.io/json") ?: return null
        val ip = json.optString("ip").takeIf { isValidIp(it) } ?: return null
        // "AS45899 VNPT Corp" -> "VNPT Corp"
        val isp = json.optString("org").replace(Regex("""^AS\d+\s+"""), "")
        val country = countryName(json.optString("country"), "")
        return PublicIpInfo(ip = ip, isp = isp, country = country)
    }

    private fun fetchFromIpify(): PublicIpInfo? {
        val json = getJson("https://api64.ipify.org?format=json") ?: return null
        val ip = json.optString("ip").takeIf { isValidIp(it) } ?: return null
        return PublicIpInfo(ip = ip, isp = "", country = "")
    }

    private fun isValidIp(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return value.matches(Regex("""^[0-9a-fA-F.:]+$""")) && (value.contains('.') || value.contains(':'))
    }

    private fun countryName(code: String?, fallback: String): String {
        val c = code?.trim().orEmpty()
        if (c.length == 2) {
            val name = Locale("", c.uppercase(Locale.ROOT)).getDisplayCountry(Locale("vi"))
            if (name.isNotBlank() && !name.equals(c, ignoreCase = true)) return name
        }
        return fallback
    }

    private fun prefixLengthToSubnet(prefixLength: Int): String {
        if (prefixLength !in 0..32) return ""
        val mask = if (prefixLength == 0) 0 else -1 shl (32 - prefixLength)
        return "${(mask ushr 24) and 0xff}.${(mask ushr 16) and 0xff}.${(mask ushr 8) and 0xff}.${mask and 0xff}"
    }
}
