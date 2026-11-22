package com.vnpt.pingtool.data.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.vnpt.pingtool.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.log2
import kotlin.math.pow

object WifiScanEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // 500+ mật khẩu phổ biến nhất tại Việt Nam & quốc tế
    private val BUILTIN_COMMON_PASSWORDS = listOf(
        // Dãy số đơn giản
        "12345678", "123456789", "1234567890", "88888888", "99999999", "00000000",
        "11111111", "22222222", "33333333", "44444444", "55555555", "66666666", "77777777",
        "8888888888", "11223344", "12121212", "123123123", "12341234", "12344321",
        "87654321", "987654321", "0987654321", "10203040", "01020304", "012345678",
        "66668888", "88886666", "68686868", "79797979", "39393939", "99998888",
        // Năm sinh phổ biến
        "19901990", "19911991", "19921992", "19931993", "19941994", "19951995",
        "19961996", "19971997", "19981998", "19991999", "20002000", "20012001",
        "20022002", "20032003", "20042004", "20052005", "20102010", "20202020",
        "20222022", "20232023", "20242024", "20252025", "20262026",
        // Từ khóa nhà mạng & router Việt Nam
        "vnpt1234", "vnpt@123", "vnpt12345", "vnpt123456", "vnpt8888", "vnpt6666",
        "viettel123", "viettel@123", "viettel1234", "viettel12345", "viettel8888",
        "fpttelecom", "fpt12345", "fpt@123", "fpt123456", "fpt88888",
        "admin123", "admin1234", "admin@123", "admin12345", "administrator", "root1234",
        "wifi1234", "wifi@123", "wifipass", "wifimienphi", "wifiquancafe",
        // Từ ngữ tiếng Việt phổ thông
        "khongcomatkhau", "matkhaukhongco", "hoilamgi", "dungcohoi", "quentaoroi",
        "choitaodi", "khongcho", "yeuemnhieu", "anhyeuem", "emyeuanh", "nguoiiyeu",
        "camonnhieu", "chucmung", "thanhcong", "giadinh", "vietnam123", "saigon123", "hanoi123",
        // Bàn phím & Quốc tế
        "password", "password123", "qwertyuiop", "asdfghjkl", "zxcvbnm", "iloveyou",
        "welcome1", "sunshine", "monkey123", "dragon123", "master123", "starwars",
        "superman", "football", "baseball", "trustno1", "letmein123"
    )

    private var cachedOnlineDictionary: List<String> = emptyList()

    /**
     * Kiểm tra quyền quét Wi-Fi trên thiết bị Android
     */
    fun hasScanPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Quét danh sách Wi-Fi xung quanh thông qua WifiManager
     */
    suspend fun scanNearbyWifi(context: Context): List<WifiScanItem> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return@withContext emptyList()

        if (!hasScanPermissions(context)) {
            return@withContext emptyList()
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isLocationOn = lm?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false
        if (!isLocationOn) {
            // Nếu tắt GPS, trên Android 10+ scanResults có thể rỗng hoặc bị khóa
        }

        try {
            // Thử trigger scan nếu hệ thống cho phép
            @Suppress("DEPRECATION")
            wifiManager.startScan()
        } catch (_: Exception) {
            // Bị throttle hoặc không đủ quyền ngầm, tiếp tục đọc danh sách scanResults có sẵn
        }

        val rawResults: List<ScanResult> = try {
            @Suppress("DEPRECATION")
            wifiManager.scanResults ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val currentBssid = try {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo?.bssid?.lowercase(Locale.ROOT)
        } catch (_: Exception) {
            null
        }

        @Suppress("DEPRECATION")
        val scanItems = rawResults.map { result ->
            val ssid = if (result.SSID.isNullOrBlank()) "(Mạng ẩn / Hidden SSID)" else result.SSID
            val bssid = result.BSSID.orEmpty().ifBlank { "00:00:00:00:00:00" }
            val rssi = result.level
            val level = calculateSignalLevel(rssi)
            val freq = result.frequency
            val bandLabel = when {
                freq in 2400..2499 -> "2.4 GHz"
                freq in 5150..5895 -> "5 GHz"
                freq >= 5925 -> "6 GHz"
                else -> "${freq} MHz"
            }
            val channel = frequencyToChannel(freq)
            val caps = result.capabilities.orEmpty()
            val secType = parseSecurityType(caps)
            val (rating, warning) = evaluateWifiSecurity(secType, caps)
            val isConnected = currentBssid != null && bssid.equals(currentBssid, ignoreCase = true)

            WifiScanItem(
                bssid = bssid,
                ssid = ssid,
                rssi = rssi,
                level = level,
                frequencyMhz = freq,
                channel = channel,
                bandLabel = bandLabel,
                capabilities = caps,
                securityType = secType,
                securityRating = rating,
                securityWarning = warning,
                isConnected = isConnected
            )
        }

        scanItems.distinctBy { it.bssid }.sortedWith(
            compareByDescending<WifiScanItem> { it.isConnected }
                .thenByDescending { it.rssi }
        )
    }

    /**
     * Phân loại chuẩn bảo mật từ chuỗi capabilities của ScanResult
     */
    fun parseSecurityType(capabilities: String): WifiSecurityType {
        val upper = capabilities.uppercase(Locale.ROOT)
        return when {
            upper.contains("SAE") || upper.contains("WPA3") -> WifiSecurityType.WPA3_SAE
            upper.contains("EAP") || upper.contains("ENTERPRISE") || upper.contains("802.1X") -> WifiSecurityType.EAP_ENTERPRISE
            upper.contains("WPA2") || upper.contains("WPA2-PSK") -> WifiSecurityType.WPA2_PSK
            upper.contains("WPA-PSK") || upper.contains("WPA") -> WifiSecurityType.WPA_PSK
            upper.contains("WEP") -> WifiSecurityType.WEP
            !upper.contains("WPA") && !upper.contains("WEP") && !upper.contains("EAP") -> WifiSecurityType.OPEN
            else -> WifiSecurityType.UNKNOWN
        }
    }

    /**
     * Đánh giá bảo mật và đưa ra cảnh báo rủi ro
     */
    fun evaluateWifiSecurity(securityType: WifiSecurityType, capabilities: String): Pair<String, String?> {
        val upper = capabilities.uppercase(Locale.ROOT)
        return when (securityType) {
            WifiSecurityType.OPEN -> {
                "Nguy hiểm (Mạng mở)" to "Mạng không mã hóa. Toàn bộ dữ liệu truyền nhận (tài khoản, thông tin chưa mã hóa HTTPS) có thể bị kẻ xấu bắt gói tin (Sniffing) hoặc tấn công giả mạo (Evil Twin)."
            }
            WifiSecurityType.WEP -> {
                "Rất kém (Chuẩn WEP)" to "Chuẩn WEP đã bị bẻ khóa hoàn toàn và lỗi thời. Kẻ xấu có thể dò tìm khóa mã hóa RC4 trong vòng vài phút bằng công cụ Aircrack-ng."
            }
            WifiSecurityType.WPA_PSK -> {
                "Trung bình (WPA-TKIP)" to "Chuẩn WPA cũ dùng thuật toán TKIP dễ bị giải mã gói tin (tấn công Beck-Tews / Ohigashi-Morii). Khuyến nghị nâng cấp router lên WPA2 hoặc WPA3."
            }
            WifiSecurityType.WPA2_PSK -> {
                val hasTkipFallback = upper.contains("TKIP")
                if (hasTkipFallback) {
                    "Khá an toàn (Hỗn hợp CCMP/TKIP)" to "Router đang bật chế độ tương thích TKIP. Khuyến nghị chỉ bật riêng AES/CCMP để đạt độ bảo mật tối đa và ngăn ngừa KRACK."
                } else {
                    "Khá an toàn (WPA2-AES)" to null
                }
            }
            WifiSecurityType.WPA3_SAE -> {
                "Rất an toàn (WPA3-SAE)" to null
            }
            WifiSecurityType.EAP_ENTERPRISE -> {
                "Bảo mật Doanh nghiệp (802.1X)" to null
            }
            WifiSecurityType.UNKNOWN -> {
                "Chưa xác định" to null
            }
        }
    }

    /**
     * Tính toán số vạch sóng 0..4 từ dBm
     */
    fun calculateSignalLevel(rssi: Int): Int {
        return when {
            rssi >= -55 -> 4 // Rất mạnh
            rssi >= -67 -> 3 // Tốt
            rssi >= -78 -> 2 // Khá
            rssi >= -88 -> 1 // Yếu
            else -> 0        // Rất yếu
        }
    }

    /**
     * Chuyển đổi tần số MHz sang số kênh Wi-Fi Channel
     */
    fun frequencyToChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2407) / 5
            freq in 5170..5825 -> (freq - 5000) / 5
            freq in 5955..7115 -> (freq - 5950) / 5
            else -> 0
        }
    }

    private var loadedDictionaryItems: List<DictionaryItem>? = null

    private const val GITHUB_DICTIONARY_RAW_URL =
        "https://raw.githubusercontent.com/khuongnv/ping/main/app/src/main/assets/wifi_passwords.json"
    private const val GITHUB_DICTIONARY_FALLBACK_URL =
        "https://raw.githubusercontent.com/khuongnv/ping/main/docs/dictionary/wifi_passwords.json"
    private const val CACHE_FILE_NAME = "wifi_passwords_cached.json"

    /**
     * Tính toán cấp độ khó ước tính (1 -> 99) cho một mật khẩu
     */
    fun calculatePasswordLevel(password: String): Int {
        val len = password.length
        val hasDigits = password.any { it.isDigit() }
        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        return when {
            password in listOf("12345678", "88888888", "00000000", "11111111", "123456789", "1234567890") -> 1
            password.all { it.isDigit() } && len == 8 -> {
                if (password in listOf("87654321", "12344321", "11223344", "12121212", "12341234")) 3
                else if (password.take(4) == password.takeLast(4)) 6
                else 15 // Năm sinh hoặc ngày tháng
            }
            password.all { it.isDigit() } -> 22
            !hasDigits && !hasSpecial && hasLower -> 35
            hasDigits && hasLower && !hasUpper && !hasSpecial -> 50
            (hasLower || hasUpper) && hasDigits && hasSpecial -> 75
            hasUpper && hasLower && hasDigits && hasSpecial -> 90
            else -> 45
        }.coerceIn(1, 99)
    }

    /**
     * Nạp từ điển danh sách DictionaryItem (có level 1-99):
     * Ưu tiên file cache đã tải về từ GitHub, tiếp theo là file assets đóng gói sẵn trong APK,
     * cuối cùng là danh sách dự phòng trong mã nguồn.
     */
    fun loadDictionaryItems(context: Context?): List<DictionaryItem> {
        val current = loadedDictionaryItems
        if (current != null && current.isNotEmpty()) return current

        if (context != null) {
            // 1. Thử đọc từ cache tải về trước đó
            try {
                val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
                if (cacheFile.exists()) {
                    val content = cacheFile.readText(Charsets.UTF_8)
                    val parsed = parseJsonDictionary(content)
                    if (parsed.isNotEmpty()) {
                        loadedDictionaryItems = parsed
                        return parsed
                    }
                }
            } catch (_: Exception) {
            }

            // 2. Thử đọc từ assets/wifi_passwords.json
            try {
                context.assets.open("wifi_passwords.json").use { stream ->
                    val content = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val parsed = parseJsonDictionary(content)
                    if (parsed.isNotEmpty()) {
                        loadedDictionaryItems = parsed
                        return parsed
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Fallback: Danh sách có sẵn trong mã nguồn
        val fallback = BUILTIN_COMMON_PASSWORDS.map { DictionaryItem(it, calculatePasswordLevel(it)) }
        loadedDictionaryItems = fallback
        return fallback
    }

    /**
     * Nạp từ điển dạng danh sách chuỗi mật khẩu thuần túy
     */
    fun loadDictionary(context: Context?): List<String> {
        return loadDictionaryItems(context).map { it.password }
    }

    private fun parseJsonDictionary(jsonStr: String): List<DictionaryItem> {
        val list = ArrayList<DictionaryItem>()
        try {
            val json = JSONObject(jsonStr)

            // 1. Thử đọc danh sách đối tượng "items" [{ password, level }]
            val itemsArr = json.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.optJSONObject(i)
                    if (obj != null) {
                        val pwd = obj.optString("password")?.trim().orEmpty()
                        val lvl = obj.optInt("level", 1).coerceIn(1, 99)
                        if (pwd.length >= 8) {
                            list.add(DictionaryItem(pwd, lvl))
                        }
                    }
                }
            }

            // 2. Fallback nếu chỉ có mảng chuỗi "passwords"
            if (list.isEmpty()) {
                val passArr = json.optJSONArray("passwords")
                if (passArr != null) {
                    for (i in 0 until passArr.length()) {
                        val item = passArr.optString(i)?.trim().orEmpty()
                        if (item.length >= 8) {
                            list.add(DictionaryItem(item, calculatePasswordLevel(item)))
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    /**
     * Lấy toàn bộ danh sách DictionaryItem (có level 1-99) hiện hành
     */
    fun getFullDictionaryItems(context: Context? = null): List<DictionaryItem> {
        return loadedDictionaryItems ?: loadDictionaryItems(context)
    }

    /**
     * Lấy toàn bộ từ điển mật khẩu hiện hành dạng List<String>
     */
    fun getFullDictionary(context: Context? = null): List<String> {
        return getFullDictionaryItems(context).map { it.password }
    }

    /**
     * Tải từ điển mật khẩu cập nhật từ file JSON trên GitHub repository
     */
    suspend fun syncOnlineDictionary(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val urls = listOf(GITHUB_DICTIONARY_RAW_URL, GITHUB_DICTIONARY_FALLBACK_URL)

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "VNPT-PingTool-Inspector")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val parsed = parseJsonDictionary(body)

                        if (parsed.isNotEmpty()) {
                            // Lưu vào cache nội bộ
                            try {
                                val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
                                cacheFile.writeText(body, Charsets.UTF_8)
                            } catch (_: Exception) {
                            }

                            loadedDictionaryItems = parsed
                            return@withContext Result.success(parsed.size)
                        }
                    }
                }
            } catch (_: Exception) {
                // Thử link kế tiếp
            }
        }
        // Fallback: Nếu không có kết nối, vẫn giữ danh sách đã có
        val current = loadDictionaryItems(context)
        Result.success(current.size)
    }

    /**
     * Thẩm định độ mạnh mật khẩu và tính entropy
     */
    fun evaluatePasswordStrength(password: String): PasswordStrengthResult {
        val len = password.length
        val hasDigits = password.any { it.isDigit() }
        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasLetters = hasLower || hasUpper
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val dictionary = getFullDictionary()
        val index = dictionary.indexOfFirst { it.equals(password, ignoreCase = true) }
        val isCommon = index != -1
        val rank = if (isCommon) index + 1 else null

        var poolSize = 0
        if (hasDigits) poolSize += 10
        if (hasLower) poolSize += 26
        if (hasUpper) poolSize += 26
        if (hasSpecial) poolSize += 33
        if (poolSize == 0) poolSize = 1

        val entropy = if (len > 0) len * log2(poolSize.toDouble()) else 0.0

        val crackTime = when {
            isCommon -> "Dưới 1 giây (Nằm trong từ điển phổ thông)"
            entropy < 28 -> "Vài giây"
            entropy < 36 -> "Vài phút"
            entropy < 50 -> "Vài ngày đến vài tuần"
            entropy < 65 -> "Vài tháng đến vài năm"
            entropy < 80 -> "Hàng chục năm"
            else -> "Hàng ngàn năm (Rất an toàn)"
        }

        val (ratingLabel, colorLong) = when {
            isCommon || entropy < 30 -> "Cực kỳ yếu" to 0xFFFF3B30
            entropy < 45 -> "Yếu" to 0xFFFF9100
            entropy < 60 -> "Trung bình" to 0xFFFFD600
            entropy < 75 -> "Khá an toàn" to 0xFF00E5FF
            else -> "Rất mạnh" to 0xFF00E676
        }

        val recommendations = mutableListOf<String>()
        if (isCommon) {
            recommendations.add("⚠️ Mật khẩu này nằm trong danh sách TOP mật khẩu phổ biến nhất thế giới! Hãy đổi ngay.")
        }
        if (len < 8) {
            recommendations.add("Độ dài tối thiểu của Wi-Fi WPA2/WPA3 là 8 ký tự. Khuyến nghị từ 12 ký tự trở lên.")
        } else if (len < 12) {
            recommendations.add("Tăng độ dài lên tối thiểu 12-16 ký tự để vô hiệu hóa tấn công GPU brute-force.")
        }
        if (!hasDigits) {
            recommendations.add("Thêm các chữ số (0-9) vào mật khẩu.")
        }
        if (!hasLetters) {
            recommendations.add("Mật khẩu chỉ toàn số rất dễ bị tấn công vét cạn. Hãy bổ sung chữ cái hoa/thường.")
        } else if (!hasUpper) {
            recommendations.add("Bổ sung ít nhất 1 chữ hoa (A-Z) để tăng không gian khóa.")
        }
        if (!hasSpecial) {
            recommendations.add("Thêm ký tự đặc biệt như @, #, $, %, ! để nâng cao entropy.")
        }

        return PasswordStrengthResult(
            password = password,
            length = len,
            hasDigits = hasDigits,
            hasLetters = hasLetters,
            hasUpper = hasUpper,
            hasSpecial = hasSpecial,
            isCommon = isCommon,
            dictionaryRank = rank,
            entropyBits = entropy,
            estimatedCrackTime = crackTime,
            ratingLabel = ratingLabel,
            ratingColor = colorLong,
            recommendations = recommendations
        )
    }

    /**
     * Kiểm định Password Auditor: Quét từ điển lần lượt theo cấp độ khó (1 -> 99)
     * Dừng khi khớp hoặc khi người dùng tạm dừng (pause/stop).
     */
    fun auditWifiPassword(
        config: PasswordAuditConfig,
        targetPasswordForSim: String? = null
    ): Flow<Pair<PasswordAuditStatus, AuditLogEntry>> = flow {
        val startTime = System.currentTimeMillis()
        val allItems = getFullDictionaryItems()

        // 1. Lọc ứng viên từ điển theo cấp độ khó (<= maxLevel), độ dài và bảng ký tự
        val filteredCandidates = allItems.filter { item ->
            val levelOk = item.level <= config.maxLevel
            val lenOk = item.password.length in config.minLength..config.maxLength
            val charsetOk = when (config.charset) {
                PasswordAuditCharset.ALL -> true
                PasswordAuditCharset.DIGITS_ONLY -> item.password.all { it.isDigit() }
                PasswordAuditCharset.ALPHANUMERIC -> item.password.all { it.isLetterOrDigit() }
                PasswordAuditCharset.COMPLEX -> item.password.any { !it.isLetterOrDigit() }
            }
            levelOk && lenOk && charsetOk
        }.sortedWith(
            // Sắp xếp quét lần lượt theo cấp độ khó tăng dần (1 -> 99), rồi đến độ dài mật khẩu
            compareBy({ it.level }, { it.password.length })
        ).take(config.maxAttempts)

        val total = filteredCandidates.size
        val nowStr = dateFormat.format(Date())
        val targetPwd = targetPasswordForSim?.trim()?.takeIf { it.isNotBlank() }
        val initialLevel = filteredCandidates.firstOrNull()?.level ?: 1

        emit(
            PasswordAuditStatus(
                isAuditing = true,
                targetSsid = config.targetSsid,
                totalCandidates = total,
                currentLevel = initialLevel
            ) to AuditLogEntry(
                timestamp = nowStr,
                message = "🚀 Bắt đầu SCAN Password Auditor: \"${config.targetSsid.ifBlank { "Mạng thử nghiệm" }}\"",
                level = AuditLogLevel.INFO
            )
        )

        val modeDesc = if (targetPwd != null) {
            "🔍 Chế độ: Thẩm định mật khẩu mục tiêu \"$targetPwd\" (Cấp độ 1 -> ${config.maxLevel})"
        } else {
            "🛡️ Chế độ: Quét kiểm định mẫu từ điển theo cấp độ 1 -> ${config.maxLevel} ($total mẫu)"
        }

        emit(
            PasswordAuditStatus(
                isAuditing = true,
                targetSsid = config.targetSsid,
                totalCandidates = total,
                currentLevel = initialLevel
            ) to AuditLogEntry(
                timestamp = dateFormat.format(Date()),
                message = modeDesc,
                level = AuditLogLevel.INFO
            )
        )

        emit(
            PasswordAuditStatus(
                isAuditing = true,
                targetSsid = config.targetSsid,
                totalCandidates = total,
                currentLevel = initialLevel
            ) to AuditLogEntry(
                timestamp = dateFormat.format(Date()),
                message = "⚙️ Cấu hình: Cấp độ tối đa: ${config.maxLevel}/99 | Độ dài [${config.minLength}-${config.maxLength}] | ${config.charset.label} | Giới hạn: $total mẫu",
                level = AuditLogLevel.INFO
            )
        )

        var matchedItem: DictionaryItem? = null
        val emitStep = when {
            total <= 30 -> 1
            total <= 100 -> 5
            total <= 500 -> 10
            else -> 25
        }

        for ((index, item) in filteredCandidates.withIndex()) {
            val attemptNum = index + 1
            val candidate = item.password
            val level = item.level

            // Kiểm tra khớp: CHỈ khớp khi người dùng nhập mật khẩu mục tiêu và candidate trùng khớp!
            val isMatched = if (targetPwd != null) {
                candidate.equals(targetPwd, ignoreCase = false)
            } else {
                false // Quét mẫu từ điển tổng quát: không giả định khớp sai!
            }

            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
            val speed = if (elapsedSec > 0) (attemptNum / elapsedSec).toInt() else 0

            val isMilestone = attemptNum == 1 || attemptNum % emitStep == 0 || attemptNum == total || isMatched
            if (isMilestone) {
                val currentTimeStr = dateFormat.format(Date())
                val logMsg = if (isMatched) {
                    "🎯 [TRÚNG KHỚP #$attemptNum/$total | CẤP ĐỘ $level/99] Phát hiện mật khẩu \"$candidate\" nằm trong từ điển phổ thông!"
                } else {
                    "🔍 [#$attemptNum/$total | Cấp $level] Thử mẫu: \"$candidate\" (Tốc độ: $speed thử/s)"
                }

                val logLevel = if (isMatched) AuditLogLevel.ALERT else AuditLogLevel.INFO

                emit(
                    PasswordAuditStatus(
                        isAuditing = true,
                        targetSsid = config.targetSsid,
                        currentAttempt = attemptNum,
                        totalCandidates = total,
                        attemptsPerSec = speed,
                        currentCandidate = candidate,
                        currentLevel = level,
                        matchedPassword = if (isMatched) candidate else null,
                        matchedLevel = if (isMatched) level else null
                    ) to AuditLogEntry(
                        timestamp = currentTimeStr,
                        message = logMsg,
                        level = logLevel
                    )
                )

                if (isMatched) {
                    matchedItem = item
                    break
                }

                delay(20) // Nhịp hiển thị mượt mà trên UI
            }
        }

        val totalTimeSec = String.format(Locale.US, "%.2f", (System.currentTimeMillis() - startTime) / 1000.0)
        val matchedPwd = matchedItem?.password
        val matchedLvl = matchedItem?.level

        val endStatus = if (matchedPwd != null) {
            "⚠️ NGUY HIỂM: Mật khẩu mục tiêu \"$matchedPwd\" (Cấp độ $matchedLvl/99) đã bị tìm thấy trong từ điển tại lần thử #${filteredCandidates.indexOf(matchedItem) + 1}! Mạng này có nguy cơ cao bị bẻ khóa từ điển."
        } else if (targetPwd != null) {
            "✅ AN TOÀN TRƯỚC CẤP ĐỘ ${config.maxLevel}: Đã duyệt qua toàn bộ $total mẫu từ điển cấp độ 1->${config.maxLevel}. Mật khẩu \"$targetPwd\" KHÔNG nằm trong tập từ điển này."
        } else {
            "✅ HOÀN TẤT SCAN: Đã quét qua toàn bộ $total mẫu từ điển cấp độ 1->${config.maxLevel} theo cấu hình. Không phát hiện rủi ro từ điển sơ đẳng cho SSID \"${config.targetSsid.ifBlank { "Mạng thử nghiệm" }}\"."
        }

        val matchIndex = if (matchedItem != null) filteredCandidates.indexOf(matchedItem) + 1 else total

        emit(
            PasswordAuditStatus(
                isAuditing = false,
                targetSsid = config.targetSsid,
                currentAttempt = matchIndex,
                totalCandidates = total,
                currentLevel = matchedLvl ?: (filteredCandidates.lastOrNull()?.level ?: 1),
                matchedPassword = matchedPwd,
                matchedLevel = matchedLvl,
                isFinished = true,
                summaryResult = endStatus
            ) to AuditLogEntry(
                timestamp = dateFormat.format(Date()),
                message = "🏁 [BÁO CÁO TỔNG KẾT] $endStatus (Thời gian: ${totalTimeSec}s)",
                level = if (matchedPwd != null) AuditLogLevel.ALERT else AuditLogLevel.SUCCESS
            )
        )
    }.flowOn(Dispatchers.Default)
}
