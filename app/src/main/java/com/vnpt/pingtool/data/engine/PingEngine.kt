package com.vnpt.pingtool.data.engine

import com.vnpt.pingtool.data.engine.PingOutputParser.ProbeResult
import com.vnpt.pingtool.data.model.PingConfig
import com.vnpt.pingtool.data.model.PingPacket
import com.vnpt.pingtool.data.model.TracerouteHop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Động cơ đo kiểm mạng dựa trên lệnh `ping` hệ thống.
 *
 * Mỗi gói tin được gửi bằng một tiến trình `ping -c 1` độc lập, nhờ đó:
 * - Mọi gói đều có kết quả xác định (thành công / timeout / lỗi) => thống kê mất gói chính xác.
 * - Tiến trình luôn bị hủy (`destroy()`) khi coroutine bị hủy, không rò rỉ tiến trình nền.
 */
object PingEngine {

    private const val PROCESS_GRACE_MS = 3_000L

    private data class ProcessOutput(val text: String, val exitCode: Int)

    /**
     * Chạy tiến trình và đọc toàn bộ output. Khi coroutine bị hủy hoặc quá hạn,
     * tiến trình bị `destroy()` ngay lập tức để giải phóng luồng đọc.
     */
    private suspend fun runProcess(cmd: List<String>, hardTimeoutMs: Long): ProcessOutput? =
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            try {
                withTimeoutOrNull(hardTimeoutMs) {
                    coroutineScope {
                        val reader = async(Dispatchers.IO) {
                            process.inputStream.bufferedReader().use { it.readText() }
                        }
                        try {
                            val text = reader.await()
                            val exit = runInterruptible { process.waitFor() }
                            ProcessOutput(text, exit)
                        } finally {
                            // Hủy tiến trình ngay trong scope để luồng đọc đang chặn được giải phóng,
                            // tránh coroutineScope chờ vô hạn khi bị hủy / quá hạn.
                            process.destroy()
                        }
                    }
                }
            } finally {
                process.destroy()
            }
        }

    private fun buildPingCommand(target: String, config: PingConfig, ttl: Int? = null, numeric: Boolean = false): List<String> {
        val cmd = mutableListOf("ping", "-c", "1", "-W", config.timeoutSec.coerceAtLeast(1).toString())
        if (numeric) cmd.add("-n")
        cmd.add("-s")
        cmd.add(config.packetSize.coerceIn(0, 65_500).toString())
        if (ttl != null) {
            cmd.add("-t")
            cmd.add(ttl.toString())
        }
        cmd.add(target)
        return cmd
    }

    fun startPing(host: String, config: PingConfig): Flow<PingPacket> = flow {
        val target = host.trim()
        val intervalMs = config.intervalMs.coerceAtLeast(200L)
        val hardTimeout = config.timeoutSec * 1000L + PROCESS_GRACE_MS
        var sequence = 0
        var headerEmitted = false

        while (currentCoroutineContext().isActive) {
            if (config.packetCount > 0 && sequence >= config.packetCount) break
            sequence++
            val startNs = System.nanoTime()

            val result: ProbeResult = try {
                val output = runProcess(buildPingCommand(target, config), hardTimeout)
                if (output == null) ProbeResult.Timeout(null) else PingOutputParser.parseSingleProbe(output.text)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ProbeResult.Failure("Lỗi thực thi lệnh ping: ${e.localizedMessage ?: e.javaClass.simpleName}", null)
            }

            if (!headerEmitted) {
                result.header?.let { header ->
                    emit(
                        PingPacket(
                            sequence = 0,
                            ip = header.ip,
                            isInfo = true,
                            rawLog = header.rawLine
                        )
                    )
                    headerEmitted = true
                }
            }

            val resolvedIp = result.header?.ip ?: target
            val packet = when (result) {
                is ProbeResult.Success -> PingPacket(
                    sequence = sequence,
                    bytes = result.reply.bytes,
                    ip = result.reply.ip.ifBlank { resolvedIp },
                    ttl = result.reply.ttl,
                    timeMs = result.reply.timeMs,
                    isSuccess = true,
                    rawLog = PingOutputParser.normalizeSequence(result.reply.rawLine, sequence)
                )
                is ProbeResult.IcmpError -> PingPacket(
                    sequence = sequence,
                    ip = result.fromIp,
                    isSuccess = false,
                    rawLog = PingOutputParser.normalizeSequence(result.rawLine, sequence)
                )
                is ProbeResult.Failure -> PingPacket(
                    sequence = sequence,
                    ip = resolvedIp,
                    isSuccess = false,
                    rawLog = result.message
                )
                is ProbeResult.Timeout -> PingPacket(
                    sequence = sequence,
                    ip = resolvedIp,
                    isSuccess = false,
                    rawLog = "Request timeout for icmp_seq=$sequence (${config.timeoutSec}s)"
                )
            }
            emit(packet)

            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            val waitMs = intervalMs - elapsedMs
            if (waitMs > 0) delay(waitMs)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Traceroute bằng ICMP với TTL tăng dần (`ping -n -c 1 -t <ttl>`).
     * RTT của chặng trung gian được đo bằng thời gian thực thi probe (xấp xỉ),
     * RTT của đích cuối lấy chính xác từ Echo Reply.
     *
     * @throws UnknownHostException nếu không phân giải được tên miền.
     */
    suspend fun runTraceroute(
        host: String,
        maxHops: Int = 30,
        timeoutSec: Int = 2,
        onHop: suspend (TracerouteHop) -> Unit
    ) = withContext(Dispatchers.IO) {
        val target = host.trim()
        val destinationIp = InetAddress.getByName(target).hostAddress ?: target
        val probeConfig = PingConfig(timeoutSec = timeoutSec)

        for (ttl in 1..maxHops) {
            ensureActive()
            val startNs = System.nanoTime()
            val output = try {
                runProcess(
                    buildPingCommand(destinationIp, probeConfig, ttl = ttl, numeric = true),
                    timeoutSec * 1000L + PROCESS_GRACE_MS
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000f

            val hop = when (val result = output?.let { PingOutputParser.parseSingleProbe(it.text) }) {
                is ProbeResult.Success -> TracerouteHop(
                    hopNumber = ttl,
                    ip = result.reply.ip.ifBlank { destinationIp },
                    timeMs = result.reply.timeMs,
                    isDestination = true
                )
                is ProbeResult.IcmpError -> TracerouteHop(
                    hopNumber = ttl,
                    ip = result.fromIp,
                    timeMs = elapsedMs,
                    isDestination = result.fromIp == destinationIp
                )
                else -> TracerouteHop(hopNumber = ttl, ip = "*", isTimeout = true)
            }
            onHop(hop)

            if (hop.isDestination) break
        }
    }
}
