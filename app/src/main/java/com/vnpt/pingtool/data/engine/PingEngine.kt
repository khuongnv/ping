package com.vnpt.pingtool.data.engine

import com.vnpt.pingtool.data.model.PingConfig
import com.vnpt.pingtool.data.model.PingPacket
import com.vnpt.pingtool.data.model.PingSessionStats
import com.vnpt.pingtool.data.model.TracerouteHop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.regex.Pattern

object PingEngine {

    // Regex for ping output:
    // 64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=65.2 ms
    // or bytes=64 from 8.8.8.8: icmp_seq=1 ttl=117 time=65.2ms
    private val pingPattern = Pattern.compile(
        """(?:(\d+)\s+bytes\s+from\s+)?([0-9a-fA-F.:]+)(?::\s+icmp_seq=(\d+))?(?:\s+ttl=(\d+))?(?:\s+time[=<]([0-9.]+)\s*ms)?""",
        Pattern.CASE_INSENSITIVE
    )

    fun startPing(
        host: String,
        config: PingConfig
    ): Flow<PingPacket> = callbackFlow {
        val target = host.trim()
        val countParam = if (config.packetCount > 0) "-c ${config.packetCount}" else ""
        val intervalSec = (config.intervalMs / 1000.0).coerceAtLeast(0.2)
        val sizeParam = "-s ${config.packetSize}"
        val timeoutParam = "-W ${config.timeoutSec}"

        // Build command: ping -i 1 -s 56 -W 3 target
        val cmd = mutableListOf("ping")
        if (config.packetCount > 0) {
            cmd.add("-c")
            cmd.add(config.packetCount.toString())
        }
        cmd.add("-i")
        cmd.add(String.format(java.util.Locale.US, "%.1f", intervalSec))
        cmd.add("-s")
        cmd.add(config.packetSize.toString())
        cmd.add("-W")
        cmd.add(config.timeoutSec.toString())
        cmd.add(target)

        var process: Process? = null
        try {
            val processBuilder = ProcessBuilder(cmd)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var seqCounter = 1

            while (process.isAlive || reader.ready()) {
                line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                val matcher = pingPattern.matcher(trimmed)
                if (matcher.find() && trimmed.contains("time", ignoreCase = true)) {
                    val bytes = matcher.group(1)?.toIntOrNull() ?: (config.packetSize + 8)
                    val ip = matcher.group(2) ?: target
                    val seq = matcher.group(3)?.toIntOrNull() ?: seqCounter++
                    val ttl = matcher.group(4)?.toIntOrNull() ?: config.ttl
                    val timeMs = matcher.group(5)?.toFloatOrNull() ?: 0f

                    val packet = PingPacket(
                        sequence = seq,
                        bytes = bytes,
                        ip = ip,
                        ttl = ttl,
                        timeMs = timeMs,
                        isSuccess = true,
                        rawLog = trimmed
                    )
                    trySend(packet)
                } else if (trimmed.contains("Request timeout", ignoreCase = true) ||
                    trimmed.contains("Destination Host Unreachable", ignoreCase = true) ||
                    trimmed.contains("100% packet loss", ignoreCase = true) ||
                    trimmed.contains("unknown host", ignoreCase = true)
                ) {
                    val packet = PingPacket(
                        sequence = seqCounter++,
                        bytes = 0,
                        ip = target,
                        ttl = 0,
                        timeMs = 0f,
                        isSuccess = false,
                        rawLog = trimmed
                    )
                    trySend(packet)
                } else if (trimmed.startsWith("PING ", ignoreCase = true)) {
                    // Header info log
                    val packet = PingPacket(
                        sequence = 0,
                        bytes = 0,
                        ip = target,
                        ttl = 0,
                        timeMs = 0f,
                        isSuccess = true,
                        rawLog = trimmed
                    )
                    trySend(packet)
                }
            }
        } catch (e: Exception) {
            trySend(
                PingPacket(
                    sequence = 0,
                    ip = target,
                    ttl = 0,
                    timeMs = 0f,
                    isSuccess = false,
                    rawLog = "Lỗi thực thi: ${e.localizedMessage ?: e.message}"
                )
            )
        } finally {
            process?.destroy()
        }

        awaitClose {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    // Traceroute implementation via ping with incrementing TTL (-t on Linux/Android)
    suspend fun runTraceroute(
        host: String,
        maxHops: Int = 20,
        onHop: (TracerouteHop) -> Unit
    ) = withContext(Dispatchers.IO) {
        val target = host.trim()
        val resolvedIp = try {
            InetAddress.getByName(target).hostAddress ?: target
        } catch (e: Exception) {
            target
        }

        for (ttl in 1..maxHops) {
            if (!coroutineContext.isActive) break
            var hopIp = "*"
            var timeMs = 0f
            var isTimeout = true

            try {
                // Command: ping -c 1 -t <ttl> -W 2 <target>
                val process = ProcessBuilder("ping", "-c", "1", "-t", ttl.toString(), "-W", "2", target)
                    .redirectErrorStream(true)
                    .start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val startTime = System.currentTimeMillis()
                val output = reader.readText()
                val elapsed = (System.currentTimeMillis() - startTime).toFloat()
                process.waitFor()

                // Check for "From 10.0.0.1: icmp_seq=1 Time to live exceeded"
                val ipMatcher = Pattern.compile("""(?:From|from)\s+([0-9a-fA-F.:]+)""").matcher(output)
                if (ipMatcher.find()) {
                    hopIp = ipMatcher.group(1) ?: "*"
                    timeMs = elapsed
                    isTimeout = false
                } else if (output.contains("bytes from", ignoreCase = true)) {
                    // Reached target
                    hopIp = resolvedIp
                    val timeMatcher = Pattern.compile("""time[=<]([0-9.]+)\s*ms""").matcher(output)
                    timeMs = if (timeMatcher.find()) timeMatcher.group(1)?.toFloatOrNull() ?: elapsed else elapsed
                    isTimeout = false
                }
            } catch (e: Exception) {
                isTimeout = true
            }

            val hop = TracerouteHop(
                hopNumber = ttl,
                ip = hopIp,
                timeMs = timeMs,
                isTimeout = isTimeout
            )
            onHop(hop)

            if (hopIp == resolvedIp && !isTimeout) {
                // Target reached
                break
            }
        }
    }
}
