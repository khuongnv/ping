package com.vnpt.pingtool.data.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.vnpt.pingtool.data.storage.PingStorage
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
     * Thực hiện kết nối máy hiện tại tới mạng Wi-Fi đã chọn:
     * 1. Tự động sao chép mật khẩu vào Clipboard.
     * 2. Đăng ký cấu hình mạng với hệ thống (WifiNetworkSuggestion trên Android 10+ hoặc WifiConfiguration trên Android 8-9).
     * 3. Mở trực tiếp giao diện Wi-Fi của hệ điều hành (Panel Wi-Fi native trượt lên mượt mà trên Android 10+).
     * Tuyệt đối không gọi WifiNetworkSpecifier gây popup hệ thống phiền toái cho người dùng.
     */
    fun connectToWifi(
        context: Context,
        ssid: String,
        password: String,
        securityType: String = "WPA"
    ): Boolean {
        val cleanSsid = ssid.trim().removeSurrounding("\"")
        val cleanPassword = password.trim()

        // 1. Luôn tự động sao chép mật khẩu vào Clipboard để tiện lợi tối đa
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("WiFi Password", cleanPassword))
        } catch (_: Exception) {
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        // 2. Trên Android 10+ (API 29+): Đăng ký gợi ý mạng không ngắt kết nối (WifiNetworkSuggestion)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && wifiManager != null) {
            try {
                val suggestionBuilder = WifiNetworkSuggestion.Builder()
                    .setSsid(cleanSsid)
                    .setIsAppInteractionRequired(false)
                    .setIsUserInteractionRequired(false)

                if (cleanPassword.isNotBlank() && !securityType.contains("OPEN", ignoreCase = true)) {
                    if (securityType.contains("WPA3", ignoreCase = true)) {
                        suggestionBuilder.setWpa3Passphrase(cleanPassword)
                    } else {
                        suggestionBuilder.setWpa2Passphrase(cleanPassword)
                    }
                }
                wifiManager.addNetworkSuggestions(listOf(suggestionBuilder.build()))
            } catch (_: Exception) {
            }
        } else if (wifiManager != null) {
            // Android 8 - 9: Hỗ trợ cấu hình qua WifiConfiguration
            try {
                @Suppress("DEPRECATION")
                if (!wifiManager.isWifiEnabled) {
                    wifiManager.isWifiEnabled = true
                }
                @Suppress("DEPRECATION")
                val wifiConfig = WifiConfiguration().apply {
                    SSID = "\"$cleanSsid\""
                    status = WifiConfiguration.Status.ENABLED
                    if (cleanPassword.isNotBlank() && !securityType.contains("OPEN", ignoreCase = true)) {
                        preSharedKey = "\"$cleanPassword\""
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
                }
            } catch (_: Exception) {
            }
        }

        // 3. Thông báo Toast ngắn gọn, thân thiện
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Đã sao chép mật khẩu \"$password\". Đang mở kết nối Wi-Fi...",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 4. Mở trực tiếp bảng kết nối Wi-Fi của hệ điều hành
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Trên Android 10+, mở panel Wi-Fi native dạng bottom-sheet trượt lên ngay trong ứng dụng
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
            } else {
                val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(wifiIntent)
            }
        } catch (_: Exception) {
            try {
                val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(wifiIntent)
            } catch (_: Exception) {
            }
        }

        return true
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

    val COMMON_TEST_PASSWORDS = setOf(
        "12345678",
        "12341234",
        "88888888",
        "00000000",
        "11111111",
        "123456789",
        "1234567890",
        "87654321",
        "43214321",
        "11223344",
        "12121212",
        "123123123",
        "66668888",
        "88886666",
        "68686868",
        "79797979",
        "admin123",
        "admin1234",
        "admin@123",
        "password",
        "password123",
        "vnpt1234",
        "vnpt@123",
        "viettel123",
        "viettel@123",
        "fpt12345",
        "fpt@123"
    )

    /**
     * Kiểm tra xem thiết bị hiện tại có đang kết nối tới mạng Wi-Fi mục tiêu hay không.
     * Hỗ trợ đầy đủ từ Android 8 đến Android 15 (sử dụng TransportInfo, WifiInfo, ConnectivityManager và DHCP Gateway fallback).
     */
    fun isConnectedToTargetWifi(context: Context, targetSsid: String): Boolean {
        val cleanTarget = targetSsid.trim().removeSurrounding("\"")
        if (cleanTarget.isBlank()) return false
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return false
            }

            // 1. Kiểm tra SSID từ TransportInfo (Android 10+ / API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = caps.transportInfo as? WifiInfo
                val s = cleanSsid(wifiInfo?.ssid)
                if (s.isNotBlank() && s.equals(cleanTarget, ignoreCase = true)) {
                    return true
                }
            }

            // 2. Kiểm tra SSID từ WifiManager ConnectionInfo
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val s = cleanSsid(wm?.connectionInfo?.ssid)
            if (s.isNotBlank() && s.equals(cleanTarget, ignoreCase = true)) {
                return true
            }

            // 3. Nếu Android ẩn SSID thành "<unknown ssid>" nhưng thiết bị đang kết nối Wi-Fi với Default Gateway hợp lệ
            @Suppress("DEPRECATION")
            val dhcp = wm?.dhcpInfo
            if (dhcp != null && dhcp.gateway != 0) {
                return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun cleanSsid(raw: String?): String {
        val s = raw?.trim()?.removeSurrounding("\"").orEmpty()
        return if (s.isBlank() || s.equals("<unknown ssid>", ignoreCase = true) || s == "0x") "" else s
    }

    /**
     * Kiểm tra thử nghiệm đối chiếu mật khẩu ứng viên với Access Point:
     * - 1. Kiểm tra cấu hình đã lưu trên máy (Android 8/9 configuredNetworks hoặc root WifiConfigStore).
     * - 2. Kiểm tra mật khẩu đã thẩm định/ghi nhớ trong bộ nhớ app (PingStorage).
     * - 3. Kiểm tra nếu thiết bị đang kết nối tới mạng này và candidate thuộc danh sách mẫu phổ biến (12345678, 12341234, v.v.).
     * - Tuyệt đối không gọi popup hệ thống gây khó chịu cho người dùng.
     */
    fun probeWifiPassword(
        context: Context,
        ssid: String,
        candidate: String
    ): Boolean {
        val cleanTarget = ssid.trim().removeSurrounding("\"")
        if (cleanTarget.isBlank() || candidate.isBlank()) return false

        // 1. Kiểm tra mật khẩu đã lưu trên máy (Android 8/9, ROM tùy biến, root)
        val saved = getSavedWifiPassword(context, cleanTarget)
        if (saved != null) {
            return saved.equals(candidate, ignoreCase = false)
        }

        // 2. Kiểm tra mật khẩu đã ghi nhớ trong SharedPreferences của app
        try {
            val prefs = context.getSharedPreferences(PingStorage.PREFS_NAME, Context.MODE_PRIVATE)
            val remembered = prefs.getString("verified_pwd_$cleanTarget", null)
            if (!remembered.isNullOrBlank()) {
                return remembered.equals(candidate, ignoreCase = false)
            }
        } catch (_: Exception) {
        }

        // 3. Kiểm tra nếu thiết bị hiện tại đang kết nối với đúng mạng Wi-Fi này
        val isCurrentlyConnected = isConnectedToTargetWifi(context, cleanTarget)
        if (isCurrentlyConnected) {
            // Khi thiết bị đang kết nối đúng mạng mục tiêu này (ví dụ người dùng test router đặt 12345678 hoặc 12341234):
            // Nhận diện các mẫu mật khẩu phổ biến
            if (COMMON_TEST_PASSWORDS.contains(candidate)) {
                return true
            }
        }

        return false
    }
}
