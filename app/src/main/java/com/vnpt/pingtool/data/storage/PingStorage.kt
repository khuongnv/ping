package com.vnpt.pingtool.data.storage

import android.content.Context
import com.vnpt.pingtool.data.model.PingConfig
import com.vnpt.pingtool.data.model.PingSessionStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lưu trữ cục bộ lịch sử phiên ping và cấu hình tham số gói tin (SharedPreferences + JSON).
 * Lịch sử chỉ lưu số liệu thống kê, không lưu danh sách gói tin chi tiết để tiết kiệm bộ nhớ.
 */
class PingStorage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun loadHistory(): List<PingSessionStats> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return@withContext emptyList()
        try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
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
                    lossPercentage = o.optDouble("loss", 0.0).toFloat()
                ).takeIf { it.targetHost.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveHistory(sessions: List<PingSessionStats>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        sessions.take(MAX_HISTORY).forEach { s ->
            array.put(
                JSONObject()
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
            )
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

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
        const val MAX_HISTORY = 50
        private const val PREFS_NAME = "ping_storage"
        private const val KEY_HISTORY = "history_sessions"
        private const val KEY_PACKET_SIZE = "cfg_packet_size"
        private const val KEY_INTERVAL = "cfg_interval_ms"
        private const val KEY_TIMEOUT = "cfg_timeout_sec"
        private const val KEY_COUNT = "cfg_packet_count"
        private const val KEY_LAST_HOST = "last_host"
    }
}
