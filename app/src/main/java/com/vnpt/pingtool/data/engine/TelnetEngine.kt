package com.vnpt.pingtool.data.engine

import com.vnpt.pingtool.data.model.TelnetPortResult
import com.vnpt.pingtool.data.model.TelnetPortStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object TelnetEngine {

    const val MAX_PORTS_PER_BATCH = 500
    const val DEFAULT_TIMEOUT_MS = 2500

    private val commonServices = mapOf(
        20 to "FTP Data",
        21 to "FTP",
        22 to "SSH",
        23 to "Telnet",
        25 to "SMTP",
        53 to "DNS",
        67 to "DHCP Server",
        68 to "DHCP Client",
        69 to "TFTP",
        80 to "HTTP",
        110 to "POP3",
        123 to "NTP",
        135 to "RPC",
        139 to "NetBIOS",
        143 to "IMAP",
        161 to "SNMP",
        389 to "LDAP",
        443 to "HTTPS",
        445 to "SMB",
        465 to "SMTPS",
        514 to "Syslog",
        587 to "SMTP Submission",
        636 to "LDAPS",
        993 to "IMAPS",
        995 to "POP3S",
        1080 to "SOCKS Proxy",
        1433 to "MSSQL",
        1521 to "Oracle DB",
        2049 to "NFS",
        3306 to "MySQL",
        3389 to "RDP Remote Desktop",
        5432 to "PostgreSQL",
        5900 to "VNC",
        6379 to "Redis",
        8000 to "HTTP Dev",
        8080 to "HTTP Alternate",
        8443 to "HTTPS Alternate",
        8888 to "HTTP Alternate",
        9000 to "SonarQube/PHP-FPM",
        9090 to "Cockpit / Web",
        9200 to "Elasticsearch",
        27017 to "MongoDB"
    )

    fun getServiceName(port: Int): String = commonServices[port] ?: ""

    /**
     * Phân tích chuỗi cổng nhập vào.
     * Hỗ trợ:
     * - Cổng đơn: "80"
     * - Danh sách phẩy: "80, 443, 8080"
     * - Dải cổng: "80-90", "20-25"
     * - Kết hợp: "22, 80-85, 443, 8080"
     *
     * Giới hạn tối đa [MAX_PORTS_PER_BATCH] cổng để bảo đảm hiệu năng và an toàn mạng.
     */
    fun parsePortInput(input: String): List<Int> {
        if (input.isBlank()) return emptyList()
        val ports = LinkedHashSet<Int>()

        val tokens = input.split(',', ';', ' ', '\n', '\t')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (token in tokens) {
            if (token.contains('-')) {
                val parts = token.split('-').map { it.trim() }
                if (parts.size == 2) {
                    val start = parts[0].toIntOrNull()
                    val end = parts[1].toIntOrNull()
                    if (start != null && end != null && start in 1..65535 && end in 1..65535) {
                        val min = minOf(start, end)
                        val max = maxOf(start, end)
                        for (p in min..max) {
                            ports.add(p)
                            if (ports.size >= MAX_PORTS_PER_BATCH) break
                        }
                    }
                }
            } else {
                val p = token.toIntOrNull()
                if (p != null && p in 1..65535) {
                    ports.add(p)
                }
            }
            if (ports.size >= MAX_PORTS_PER_BATCH) break
        }

        return ports.toList()
    }

    /**
     * Thực hiện thăm dò một cổng TCP cụ thể qua Socket.
     */
    suspend fun probePort(
        host: String,
        port: Int,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): TelnetPortResult = withContext(Dispatchers.IO) {
        val target = host.trim()
        val startNs = System.nanoTime()
        val service = getServiceName(port)

        try {
            Socket().use { socket ->
                socket.reuseAddress = true
                socket.connect(InetSocketAddress(target, port), timeoutMs.coerceAtLeast(200))
            }
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000f
            TelnetPortResult(
                port = port,
                serviceName = service,
                status = TelnetPortStatus.OPEN,
                timeMs = elapsedMs
            )
        } catch (e: ConnectException) {
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000f
            TelnetPortResult(
                port = port,
                serviceName = service,
                status = TelnetPortStatus.CLOSED,
                timeMs = elapsedMs,
                errorDetail = "Cổng đóng (Connection refused)"
            )
        } catch (e: SocketTimeoutException) {
            TelnetPortResult(
                port = port,
                serviceName = service,
                status = TelnetPortStatus.TIMEOUT,
                timeMs = timeoutMs.toFloat(),
                errorDetail = "Hết thời gian chờ (${timeoutMs}ms)"
            )
        } catch (e: NoRouteToHostException) {
            TelnetPortResult(
                port = port,
                serviceName = service,
                status = TelnetPortStatus.FILTERED,
                errorDetail = "Không tìm thấy đường đi tới host"
            )
        } catch (e: UnknownHostException) {
            TelnetPortResult(
                port = port,
                serviceName = service,
                status = TelnetPortStatus.ERROR,
                errorDetail = "Không phân giải được tên miền"
            )
        } catch (e: Exception) {
            TelnetPortResult(
                port = port,
                serviceName = service,
                status = TelnetPortStatus.ERROR,
                errorDetail = e.localizedMessage ?: e.javaClass.simpleName
            )
        }
    }

    /**
     * Quét danh sách các cổng theo luồng bất đồng bộ song song có giới hạn (`concurrency`).
     * Phát kết quả từng cổng ngay khi xong để giao diện người dùng hiển thị tức thì.
     */
    fun scanPorts(
        host: String,
        ports: List<Int>,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        concurrency: Int = 8
    ): Flow<TelnetPortResult> = flow {
        val target = host.trim()
        if (target.isBlank() || ports.isEmpty()) return@flow

        // Phân giải DNS một lần duy nhất trước khi quét dải cổng để tăng tốc độ
        val resolvedIp = try {
            withContext(Dispatchers.IO) {
                InetAddress.getByName(target).hostAddress ?: target
            }
        } catch (e: Exception) {
            target
        }

        val semaphore = Semaphore(concurrency.coerceIn(1, 16))
        val channel = Channel<TelnetPortResult>(Channel.BUFFERED)

        coroutineScope {
            val producer = async(Dispatchers.IO) {
                ports.map { port ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            if (currentCoroutineContext().isActive) {
                                val result = probePort(resolvedIp, port, timeoutMs)
                                channel.send(result)
                            }
                        }
                    }
                }.awaitAll()
                channel.close()
            }

            for (result in channel) {
                emit(result)
            }
            producer.await()
        }
    }.flowOn(Dispatchers.IO)
}
