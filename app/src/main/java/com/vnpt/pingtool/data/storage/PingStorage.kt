package com.vnpt.pingtool.data.storage

import android.content.Context
import com.vnpt.pingtool.data.model.PasswordAuditMatchRecord
import com.vnpt.pingtool.data.model.PingConfig
import com.vnpt.pingtool.data.model.PingPacket
import com.vnpt.pingtool.data.model.PingSessionStats
import com.vnpt.pingtool.data.model.TelnetPortResult
import com.vnpt.pingtool.data.model.TelnetPortStatus
import com.vnpt.pingtool.data.model.TelnetSessionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lưu trữ cục bộ SharedPreferences + JSON:
 * - Lịch sử phiên Ping (kèm chi tiết các gói tin để vẽ lại biểu đồ sóng).
 * - Lịch sử phiên quét Telnet (kèm chi tiết từng cổng, trạng thái và RTT).
 * - Cấu hình tham số gói tin và host gần nhất.
 */
class PingStorage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ==========================================
    // PING HISTORY
    // ==========================================

    suspend fun loadHistory(): List<PingSessionStats> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return@withContext emptyList()
        try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val packetsJson = o.optJSONArray("packets")
                val packets = mutableListOf<PingPacket>()
                if (packetsJson != null) {
                    for (j in 0 until packetsJson.length()) {
                        val p = packetsJson.optJSONObject(j) ?: continue
                        packets.add(
                            PingPacket(
                                sequence = p.optInt("seq"),
                                bytes = p.optInt("bytes"),
                                ip = p.optString("ip"),
                                ttl = p.optInt("ttl"),
                                timeMs = p.optDouble("time", 0.0).toFloat(),
                                timestamp = p.optLong("ts"),
                                isSuccess = p.optBoolean("ok", true),
                                isInfo = p.optBoolean("info", false),
                                rawLog = p.optString("raw")
                            )
                        )
                    }
                }

                PingSessionStats(
                    targetHost = o.optString("host"),
                    resolvedIp = o.optString("ip"),
                    startedAt = o.optLong("startedAt"),
                    sentCount = o.optInt("sent"),
                    receivedCount = o.optInt("received"),
                    minMs = o.optDouble("min", 0.0).toFloat(),
                    avgMs = o.optDouble("avg", 0.0).toFloat(),
                    maxMs = o.optDouble("max", 0.0).toFloat(),
                    jitterMs = o.optDouble("jitter", 0.0).toFloat(),
                    lossPercentage = o.optDouble("loss", 0.0).toFloat(),
                    packets = packets
                ).takeIf { it.targetHost.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveHistory(sessions: List<PingSessionStats>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        sessions.take(MAX_PING_HISTORY).forEach { s ->
            val sessionObj = JSONObject()
                .put("host", s.targetHost)
                .put("ip", s.resolvedIp)
                .put("startedAt", s.startedAt)
                .put("sent", s.sentCount)
                .put("received", s.receivedCount)
                .put("min", s.minMs.toDouble())
                .put("avg", s.avgMs.toDouble())
                .put("max", s.maxMs.toDouble())
                .put("jitter", s.jitterMs.toDouble())
                .put("loss", s.lossPercentage.toDouble())

            val packetsArr = JSONArray()
            s.packets.takeLast(MAX_PACKETS_PER_SESSION).forEach { p ->
                packetsArr.put(
                    JSONObject()
                        .put("seq", p.sequence)
                        .put("bytes", p.bytes)
                        .put("ip", p.ip)
                        .put("ttl", p.ttl)
                        .put("time", p.timeMs.toDouble())
                        .put("ts", p.timestamp)
                        .put("ok", p.isSuccess)
                        .put("info", p.isInfo)
                        .put("raw", p.rawLog)
                )
            }
            sessionObj.put("packets", packetsArr)
            array.put(sessionObj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    // ==========================================
    // TELNET HISTORY
    // ==========================================

    suspend fun loadTelnetHistory(): List<TelnetSessionRecord> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_TELNET_HISTORY, null) ?: return@withContext emptyList()
        try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val resultsJson = o.optJSONArray("results")
                val results = mutableListOf<TelnetPortResult>()
                if (resultsJson != null) {
                    for (j in 0 until resultsJson.length()) {
                        val r = resultsJson.optJSONObject(j) ?: continue
                        val statusStr = r.optString("status", TelnetPortStatus.CLOSED.name)
                        val status = try {
                            TelnetPortStatus.valueOf(statusStr)
                        } catch (_: Exception) {
                            TelnetPortStatus.CLOSED
                        }
                        results.add(
                            TelnetPortResult(
                                port = r.optInt("port"),
                                serviceName = r.optString("svc"),
                                status = status,
                                timeMs = r.optDouble("time", 0.0).toFloat(),
                                errorDetail = r.optString("err"),
                                timestamp = r.optLong("ts")
                            )
                        )
                    }
                }

                TelnetSessionRecord(
                    id = o.optString("id"),
                    targetHost = o.optString("host"),
                    portInput = o.optString("ports"),
                    timestamp = o.optLong("ts"),
                    totalPorts = o.optInt("total"),
                    openCount = o.optInt("open"),
                    closedCount = o.optInt("closed"),
                    timeoutCount = o.optInt("timeout"),
                    results = results
                ).takeIf { it.targetHost.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveTelnetHistory(records: List<TelnetSessionRecord>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        records.take(MAX_TELNET_HISTORY).forEach { rec ->
            val obj = JSONObject()
                .put("id", rec.id)
                .put("host", rec.targetHost)
                .put("ports", rec.portInput)
                .put("ts", rec.timestamp)
                .put("total", rec.totalPorts)
                .put("open", rec.openCount)
                .put("closed", rec.closedCount)
                .put("timeout", rec.timeoutCount)

            val resultsArr = JSONArray()
            rec.results.forEach { r ->
                resultsArr.put(
                    JSONObject()
                        .put("port", r.port)
                        .put("svc", r.serviceName)
                        .put("status", r.status.name)
                        .put("time", r.timeMs.toDouble())
                        .put("err", r.errorDetail)
                        .put("ts", r.timestamp)
                )
            }
            obj.put("results", resultsArr)
            array.put(obj)
        }
        prefs.edit().putString(KEY_TELNET_HISTORY, array.toString()).apply()
    }

    // ==========================================
    // PASSWORD AUDIT MATCHES HISTORY
    // ==========================================

    suspend fun loadAuditMatches(): List<PasswordAuditMatchRecord> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_AUDIT_MATCHES, null) ?: return@withContext emptyList()
        try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                PasswordAuditMatchRecord(
                    id = o.optString("id", java.util.UUID.randomUUID().toString()),
                    ssid = o.optString("ssid"),
                    matchedPassword = o.optString("pwd"),
                    level = o.optInt("level", 1),
                    levelLabel = o.optString("levelLabel", "Cấp độ ${o.optInt("level", 1)}"),
                    timestamp = o.optLong("ts", System.currentTimeMillis()),
                    speedAttemptsPerSec = o.optInt("speed", 0),
                    attemptsCount = o.optInt("attempts", 0),
                    riskRating = o.optString("risk", "Nguy hiểm")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveAuditMatches(matches: List<PasswordAuditMatchRecord>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        matches.take(MAX_AUDIT_HISTORY).forEach { m ->
            val o = JSONObject()
            o.put("id", m.id)
            o.put("ssid", m.ssid)
            o.put("pwd", m.matchedPassword)
            o.put("level", m.level)
            o.put("levelLabel", m.levelLabel)
            o.put("ts", m.timestamp)
            o.put("speed", m.speedAttemptsPerSec)
            o.put("attempts", m.attemptsCount)
            o.put("risk", m.riskRating)
            array.put(o)
        }
        prefs.edit().putString(KEY_AUDIT_MATCHES, array.toString()).apply()
    }

    /**
     * Lấy mật khẩu thẩm định đã ghi nhớ cho một SSID cụ thể
     */
    fun getVerifiedPassword(ssid: String): String? {
        val clean = ssid.trim().removeSurrounding("\"")
        if (clean.isBlank()) return null
        return prefs.getString("verified_pwd_$clean", null)
    }

    /**
     * Lưu ghi nhớ mật khẩu thẩm định cho một SSID cụ thể
     */
    fun saveVerifiedPassword(ssid: String, password: String) {
        val clean = ssid.trim().removeSurrounding("\"")
        if (clean.isNotBlank() && password.isNotBlank()) {
            prefs.edit().putString("verified_pwd_$clean", password.trim()).apply()
        }
    }

    // ==========================================
    // CONFIG & HOST
    // ==========================================

    fun loadConfig(): PingConfig {
        val defaults = PingConfig()
        return PingConfig(
            packetSize = prefs.getInt(KEY_PACKET_SIZE, defaults.packetSize),
            intervalMs = prefs.getLong(KEY_INTERVAL, defaults.intervalMs),
            timeoutSec = prefs.getInt(KEY_TIMEOUT, defaults.timeoutSec),
            packetCount = prefs.getInt(KEY_COUNT, defaults.packetCount)
        )
    }

    fun saveConfig(config: PingConfig) {
        prefs.edit()
            .putInt(KEY_PACKET_SIZE, config.packetSize)
            .putLong(KEY_INTERVAL, config.intervalMs)
            .putInt(KEY_TIMEOUT, config.timeoutSec)
            .putInt(KEY_COUNT, config.packetCount)
            .apply()
    }

    fun loadLastHost(): String? = prefs.getString(KEY_LAST_HOST, null)

    fun saveLastHost(host: String) {
        prefs.edit().putString(KEY_LAST_HOST, host).apply()
    }

    companion object {
        const val MAX_PING_HISTORY = 30
        const val MAX_PACKETS_PER_SESSION = 100
        const val MAX_TELNET_HISTORY = 30
        const val MAX_AUDIT_HISTORY = 50

        const val PREFS_NAME = "ping_storage"
        private const val KEY_HISTORY = "history_sessions"
        private const val KEY_TELNET_HISTORY = "history_telnet_sessions"
        private const val KEY_AUDIT_MATCHES = "history_audit_matches"
        private const val KEY_PACKET_SIZE = "cfg_packet_size"
        private const val KEY_INTERVAL = "cfg_interval_ms"
        private const val KEY_TIMEOUT = "cfg_timeout_sec"
        private const val KEY_COUNT = "cfg_packet_count"
        private const val KEY_LAST_HOST = "last_host"
    }
}
