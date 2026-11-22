package com.vnpt.pingtool.data.engine

/**
 * Bộ phân tích output của lệnh `ping` (iputils / toybox trên Android).
 * Thuần Kotlin, không phụ thuộc Android framework để có thể unit test.
 */
object PingOutputParser {

    private val headerRegex = Regex("""^PING\s+(\S+)\s+\(([^)]+)\)""", RegexOption.IGNORE_CASE)
    private val bytesRegex = Regex("""(\d+)\s+bytes\s+from""", RegexOption.IGNORE_CASE)
    // "from 8.8.8.8:" | "from dns.google (8.8.8.8):" | "from 2001:4860::8888:"
    private val replyFromRegex = Regex(
        """bytes\s+from\s+([^\s()]+?)(?:\s+\(([^)]+)\))?:?\s+icmp_seq""",
        RegexOption.IGNORE_CASE
    )
    private val errorFromRegex = Regex("""^From\s+([^\s()]+?)(?:\s+\(([^)]+)\))?:?\s+icmp_seq""", RegexOption.IGNORE_CASE)
    private val seqRegex = Regex("""icmp_seq=(\d+)""", RegexOption.IGNORE_CASE)
    private val ttlRegex = Regex("""ttl=(\d+)""", RegexOption.IGNORE_CASE)
    private val timeRegex = Regex("""time[=<]\s*([0-9]+(?:\.[0-9]+)?)\s*ms""", RegexOption.IGNORE_CASE)

    private val fatalMarkers = listOf(
        "unknown host",
        "name or service not known",
        "no address associated",
        "temporary failure in name resolution",
        "network is unreachable",
        "permission denied",
        "operation not permitted",
        "invalid argument",
        "bad address"
    )

    data class Header(val host: String, val ip: String, val rawLine: String)

    data class Reply(
        val bytes: Int,
        val ip: String,
        val sequence: Int?,
        val ttl: Int,
        val timeMs: Float,
        val rawLine: String
    )

    sealed interface ProbeResult {
        val header: Header?

        data class Success(val reply: Reply, override val header: Header?) : ProbeResult

        /** Nhận được thông báo lỗi ICMP (Host Unreachable, TTL exceeded...) từ một router. */
        data class IcmpError(val fromIp: String, val rawLine: String, override val header: Header?) : ProbeResult

        /** Lỗi thực thi / phân giải tên miền / mạng không khả dụng. */
        data class Failure(val message: String, override val header: Header?) : ProbeResult

        /** Không có phản hồi trong thời gian chờ. */
        data class Timeout(override val header: Header?) : ProbeResult
    }

    fun parseHeader(line: String): Header? {
        val m = headerRegex.find(line.trim()) ?: return null
        return Header(host = m.groupValues[1], ip = m.groupValues[2], rawLine = line.trim())
    }

    /** Phân tích một dòng phản hồi thành công. Trả về null nếu không phải dòng reply hợp lệ. */
    fun parseReply(line: String): Reply? {
        val trimmed = line.trim()
        if (!trimmed.contains("bytes from", ignoreCase = true)) return null
        val time = timeRegex.find(trimmed)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        val fromMatch = replyFromRegex.find(trimmed)
        // Ưu tiên IP trong ngoặc (khi ping in tên miền ngược), sau đó tới token sau "from"
        val ip = fromMatch?.let { m ->
            m.groupValues[2].ifBlank { m.groupValues[1] }
        }.orEmpty().trimEnd(':')
        return Reply(
            bytes = bytesRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            ip = ip,
            sequence = seqRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull(),
            ttl = ttlRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            timeMs = time,
            rawLine = trimmed
        )
    }

    /** Dòng "From <ip> icmp_seq=1 Destination Host Unreachable / Time to live exceeded". Trả về IP router. */
    fun parseIcmpErrorSource(line: String): String? {
        val m = errorFromRegex.find(line.trim()) ?: return null
        return m.groupValues[2].ifBlank { m.groupValues[1] }.trimEnd(':')
    }

    /** Phân tích toàn bộ output của một lần chạy `ping -c 1`. */
    fun parseSingleProbe(output: String): ProbeResult {
        val lines = output.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val header = lines.firstNotNullOfOrNull { parseHeader(it) }

        lines.firstNotNullOfOrNull { parseReply(it) }?.let {
            return ProbeResult.Success(it, header)
        }

        lines.forEach { line ->
            val src = parseIcmpErrorSource(line)
            if (src != null) return ProbeResult.IcmpError(src, line, header)
        }

        lines.firstOrNull { line -> fatalMarkers.any { line.contains(it, ignoreCase = true) } }?.let {
            return ProbeResult.Failure(it, header)
        }

        return ProbeResult.Timeout(header)
    }

    /** Chuẩn hóa số thứ tự gói trong dòng log để khớp với bộ đếm phiên. */
    fun normalizeSequence(line: String, sequence: Int): String =
        seqRegex.replace(line) { "icmp_seq=$sequence" }
}
