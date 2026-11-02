package com.vnpt.pingtool.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object WifiQrManager {

    private const val PREFS_NAME = "ping_tool_wifi_passwords"

    /**
     * Tạo chuỗi định dạng Wi-Fi QR Code chuẩn quốc tế (MECARD / Wi-Fi DPP)
     * Định dạng: WIFI:T:WPA;S:MySSID;P:MyPassword;;
     * Nếu không có mật khẩu: WIFI:T:nopass;S:MySSID;;
     */
    fun createWifiQrString(
        ssid: String,
        password: String,
        securityType: String = "WPA",
        isHidden: Boolean = false
    ): String {
        val cleanSsid = escapeWifiString(ssid)
        return if (password.isBlank() || securityType.equals("nopass", ignoreCase = true)) {
            "WIFI:T:nopass;S:$cleanSsid;;"
        } else {
            val cleanPassword = escapeWifiString(password)
            val sec = if (securityType.isNotBlank()) securityType else "WPA"
            if (isHidden) {
                "WIFI:T:$sec;S:$cleanSsid;P:$cleanPassword;H:true;;"
            } else {
                "WIFI:T:$sec;S:$cleanSsid;P:$cleanPassword;;"
            }
        }
    }

    private fun escapeWifiString(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(":", "\\:")
            .replace(",", "\\,")
            .replace("\"", "\\\"")
    }

    /**
     * Sinh ảnh Bitmap QR Code chất lượng cao từ chuỗi ký tự
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1) // Padding tối thiểu để mã quét to rõ
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            }

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Lấy mật khẩu Wi-Fi đã lưu trong SharedPreferences theo SSID
     */
    fun getSavedPassword(context: Context, ssid: String): String? {
        if (ssid.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ssid, null)
    }

    /**
     * Lưu mật khẩu Wi-Fi vào SharedPreferences theo SSID
     */
    fun savePassword(context: Context, ssid: String, password: String) {
        if (ssid.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ssid, password).apply()
    }

    /**
     * Xóa mật khẩu Wi-Fi đã lưu
     */
    fun clearPassword(context: Context, ssid: String) {
        if (ssid.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(ssid).apply()
    }
}
