package com.vnpt.pingtool.data.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.EnumMap

object WifiConnectionManager {

    /**
     * Tạo chuỗi QR Code chuẩn Wi-Fi theo định dạng quốc tế:
     * WIFI:S:<SSID>;T:<WPA|WEP|nopass>;P:<PASSWORD>;;
     */
    fun formatWifiQrString(ssid: String, password: String, securityType: String = "WPA"): String {
        val escapedSsid = escapeQrField(ssid)
        val escapedPassword = escapeQrField(password)
        val type = when {
            securityType.contains("WEP", ignoreCase = true) -> "WEP"
            password.isBlank() || securityType.contains("OPEN", ignoreCase = true) -> "nopass"
            else -> "WPA"
        }
        return if (type == "nopass") {
            "WIFI:S:$escapedSsid;T:nopass;;"
        } else {
            "WIFI:S:$escapedSsid;T:$type;P:$escapedPassword;;"
        }
    }

    private fun escapeQrField(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(":", "\\:")
            .replace(",", "\\,")
            .replace("\"", "\\\"")
    }

    /**
     * Tạo Bitmap mã QR Code chuẩn độ phân giải cao để quét kết nối Wi-Fi
     */
    fun generateWifiQrCodeBitmap(
        ssid: String,
        password: String,
        securityType: String = "WPA",
        sizePx: Int = 512
    ): Bitmap {
        val qrContent = formatWifiQrString(ssid, password, securityType)
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 1)
        }

        val bitMatrix = QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        return bitmap
    }

    /**
     * Thực hiện kết nối máy hiện tại tới mạng Wi-Fi đã chọn
     */
    fun connectToWifi(
        context: Context,
        ssid: String,
        password: String,
        securityType: String = "WPA"
    ): Boolean {
        // 1. Luôn tự động sao chép mật khẩu vào Clipboard để tiện lợi tối đa
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("WiFi Password", password))
        } catch (_: Exception) {
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return false

        // Bật Wi-Fi nếu đang tắt (trên Android 9 trở xuống)
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                if (!wifiManager.isWifiEnabled) {
                    wifiManager.isWifiEnabled = true
                }
            }
        } catch (_: Exception) {
        }

        var isConfigured = false

        // 2. Trên Android 10+ (API 29+): Sử dụng WifiNetworkSuggestion & WifiNetworkSpecifier
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val suggestionBuilder = WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setIsAppInteractionRequired(false)

                if (password.isNotBlank() && !securityType.contains("OPEN", ignoreCase = true)) {
                    if (securityType.contains("WPA3", ignoreCase = true)) {
                        suggestionBuilder.setWpa3Passphrase(password)
                    } else {
                        suggestionBuilder.setWpa2Passphrase(password)
                    }
                }

                val status = wifiManager.addNetworkSuggestions(listOf(suggestionBuilder.build()))
                if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                    isConfigured = true
                }
            } catch (_: Exception) {
            }

            // Kích hoạt thêm WifiNetworkSpecifier để gợi ý hệ thống kết nối ngay
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (cm != null) {
                    val specifierBuilder = WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)

                    if (password.isNotBlank() && !securityType.contains("OPEN", ignoreCase = true)) {
                        if (securityType.contains("WPA3", ignoreCase = true)) {
                            specifierBuilder.setWpa3Passphrase(password)
                        } else {
                            specifierBuilder.setWpa2Passphrase(password)
                        }
                    }

                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifierBuilder.build())
                        .build()

                    cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            try {
                                cm.bindProcessToNetwork(network)
                            } catch (_: Exception) {
                            }
                        }
                    })
                    isConfigured = true
                }
            } catch (_: Exception) {
            }
        }

        // 3. Hỗ trợ chuẩn Legacy WifiConfiguration (Android 8 - 9 & các dòng máy tương thích)
        try {
            @Suppress("DEPRECATION")
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                status = WifiConfiguration.Status.ENABLED
                if (password.isNotBlank() && !securityType.contains("OPEN", ignoreCase = true)) {
                    preSharedKey = "\"$password\""
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                } else {
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }
            }

            @Suppress("DEPRECATION")
            val netId = wifiManager.addNetwork(wifiConfig)
            if (netId != -1) {
                @Suppress("DEPRECATION")
                wifiManager.disconnect()
                @Suppress("DEPRECATION")
                wifiManager.enableNetwork(netId, true)
                @Suppress("DEPRECATION")
                wifiManager.reconnect()
                isConfigured = true
            }
        } catch (_: Exception) {
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Đang gửi yêu cầu kết nối tới \"$ssid\" (Mật khẩu đã sao chép vào bộ nhớ tạm)!",
                Toast.LENGTH_LONG
            ).show()
        }

        return isConfigured
    }

    /**
     * Tìm mật khẩu đã lưu của mạng Wi-Fi trên thiết bị (nếu hệ điều hành cho phép truy xuất)
     */
    fun getSavedWifiPassword(context: Context, ssid: String): String? {
        // 1. Thử đọc cấu hình mạng Wi-Fi đã lưu của Android (Android 8, 9 và nhiều ROM tùy biến)
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager != null) {
            try {
                @Suppress("DEPRECATION")
                val configured = wifiManager.configuredNetworks
                if (configured != null) {
                    val cleanTarget = ssid.trim().trim('\"')
                    for (config in configured) {
                        val s = config.SSID?.trim()?.trim('\"')
                        if (s != null && s.equals(cleanTarget, ignoreCase = true)) {
                            val psk = config.preSharedKey?.trim()?.trim('\"')
                            if (!psk.isNullOrBlank() && !psk.contains("*")) {
                                return psk
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 2. Thử đọc trên các thiết bị đã Root
        return getSavedWifiPasswordFromRoot(ssid)
    }

    /**
     * Đọc mật khẩu Wi-Fi từ file hệ thống trên các thiết bị Android đã Root
     */
    fun getSavedWifiPasswordFromRoot(ssid: String): String? {
        try {
            val paths = arrayOf(
                "/data/misc/wifi/WifiConfigStore.xml",
                "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml",
                "/data/misc/wifi/wpa_supplicant.conf"
            )
            for (path in paths) {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $path"))
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (output.isNotBlank() && output.contains(ssid)) {
                    val cleanTarget = ssid.trim().trim('\"')
                    val xmlRegex = Regex("<string name=\"SSID\">&quot;${Regex.escape(cleanTarget)}&quot;</string>[\\s\\S]*?<string name=\"PreSharedKey\">&quot;(.*?)&quot;</string>")
                    val xmlMatch = xmlRegex.find(output)
                    if (xmlMatch != null) {
                        return xmlMatch.groupValues[1]
                    }
                    val wpaRegex = Regex("ssid=\"${Regex.escape(cleanTarget)}\"[\\s\\S]*?psk=\"(.*?)\"")
                    val wpaMatch = wpaRegex.find(output)
                    if (wpaMatch != null) {
                        return wpaMatch.groupValues[1]
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    /**
     * Kiểm tra thử nghiệm (Probe) mật khẩu ứng viên với Access Point thực tế:
     * - Trả về true nếu mật khẩu đúng (Access Point chấp thuận xác thực 4-way handshake).
     * - Trả về false nếu sai mật khẩu hoặc lỗi.
     */
    suspend fun probeWifiPassword(
        context: Context,
        ssid: String,
        candidate: String,
        timeoutMs: Long = 1200
    ): Boolean = withContext(Dispatchers.IO) {
        // Kiểm tra 0: Nếu lấy được mật khẩu đã lưu (hệ điều hành hoặc root), đối chiếu trực tiếp
        val saved = getSavedWifiPassword(context, ssid)
        if (saved != null) {
            return@withContext saved.equals(candidate, ignoreCase = false)
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return@withContext false

        // Kiểm tra 1: Thiết bị hiện tại đang kết nối với đúng mạng Wi-Fi này
        val currentSsid = try {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo?.ssid?.trim('\"')
        } catch (_: Exception) {
            null
        }

        val isCurrentlyConnected = currentSsid != null && currentSsid.equals(ssid, ignoreCase = true)

        // Kiểm tra 2: Thử nghiệm kết nối trực tiếp với Access Point qua WifiNetworkSpecifier (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val latch = CompletableDeferred<Boolean>()
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        latch.complete(true)
                    }

                    override fun onUnavailable() {
                        latch.complete(false)
                    }
                }

                try {
                    val specifier = WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(candidate)
                        .build()

                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifier)
                        .build()

                    cm.requestNetwork(request, callback, timeoutMs.toInt())
                    val connected = withTimeoutOrNull(timeoutMs) { latch.await() } ?: false
                    if (connected) return@withContext true
                } catch (_: Exception) {
                } finally {
                    try {
                        cm.unregisterNetworkCallback(callback)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        // Kiểm tra 3: Thử nghiệm với WifiConfiguration (Android 9 trở xuống)
        try {
            @Suppress("DEPRECATION")
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                preSharedKey = "\"$candidate\""
                status = WifiConfiguration.Status.ENABLED
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            }

            @Suppress("DEPRECATION")
            val netId = wifiManager.addNetwork(wifiConfig)
            if (netId != -1) {
                @Suppress("DEPRECATION")
                val enabled = wifiManager.enableNetwork(netId, true)
                @Suppress("DEPRECATION")
                wifiManager.reconnect()

                if (enabled) {
                    // Chờ phản hồi từ supplicant
                    kotlinx.coroutines.delay(1200)
                    @Suppress("DEPRECATION")
                    val updatedSsid = wifiManager.connectionInfo?.ssid?.trim('\"')
                    if (updatedSsid != null && updatedSsid.equals(ssid, ignoreCase = true)) {
                        return@withContext true
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Fallback kiểm tra khi đang kết nối cùng mạng và thử đối chiếu
        if (isCurrentlyConnected) {
            // Nếu người dùng đang kết nối mạng này và thử đúng mật khẩu thông dụng
            // có thể thử kiểm tra qua gateway socket probe
            try {
                val dhcp = @Suppress("DEPRECATION") wifiManager.dhcpInfo
                if (dhcp != null && dhcp.gateway != 0) {
                    val gatewayIp = android.text.format.Formatter.formatIpAddress(dhcp.gateway)
                    java.net.Socket().use { socket ->
                        socket.connect(java.net.InetSocketAddress(gatewayIp, 80), 800)
                        if (socket.isConnected) {
                            // Gateway hoạt động bình thường
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        return@withContext false
    }
}
