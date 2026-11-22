package com.vnpt.pingtool.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnpt.pingtool.data.engine.PingEngine
import com.vnpt.pingtool.data.engine.TelnetEngine
import com.vnpt.pingtool.data.model.*
import com.vnpt.pingtool.data.network.NetworkInfoService
import com.vnpt.pingtool.data.storage.PingStorage
import com.vnpt.pingtool.data.update.GitHubUpdateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import kotlin.math.abs

class PingViewModel(application: Application) : AndroidViewModel(application) {

    private val updateManager = GitHubUpdateManager(application)
    private val storage = PingStorage(application)
    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    // Ping State
    private val initialHost = storage.loadLastHost()?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST
    private val _pingStats = MutableStateFlow(PingSessionStats(targetHost = initialHost))
    val pingStats: StateFlow<PingSessionStats> = _pingStats.asStateFlow()

    private val _currentHostInput = MutableStateFlow(initialHost)
    val currentHostInput: StateFlow<String> = _currentHostInput.asStateFlow()

    private val _config = MutableStateFlow(storage.loadConfig())
    val config: StateFlow<PingConfig> = _config.asStateFlow()

    private var pingJob: Job? = null

    // Bộ tích lũy thống kê toàn phiên (độc lập với ring-buffer hiển thị)
    private var rttSum = 0.0
    private var lastRtt: Float? = null
    private var jitterSum = 0.0
    private var jitterSamples = 0

    // Traceroute State
    private val _tracerouteHops = MutableStateFlow<List<TracerouteHop>>(emptyList())
    val tracerouteHops: StateFlow<List<TracerouteHop>> = _tracerouteHops.asStateFlow()

    private val _isTracerouteRunning = MutableStateFlow(false)
    val isTracerouteRunning: StateFlow<Boolean> = _isTracerouteRunning.asStateFlow()

    private val _tracerouteError = MutableStateFlow<String?>(null)
    val tracerouteError: StateFlow<String?> = _tracerouteError.asStateFlow()

    private var tracerouteJob: Job? = null

    // Telnet State
    private val _currentTelnetHost = MutableStateFlow(initialHost)
    val currentTelnetHost: StateFlow<String> = _currentTelnetHost.asStateFlow()

    private val _currentTelnetPortInput = MutableStateFlow("80, 443, 8080")
    val currentTelnetPortInput: StateFlow<String> = _currentTelnetPortInput.asStateFlow()

    private val _telnetResults = MutableStateFlow<List<TelnetPortResult>>(emptyList())
    val telnetResults: StateFlow<List<TelnetPortResult>> = _telnetResults.asStateFlow()

    private val _telnetSummary = MutableStateFlow(TelnetScanSummary())
    val telnetSummary: StateFlow<TelnetScanSummary> = _telnetSummary.asStateFlow()

    private val _isTelnetScanning = MutableStateFlow(false)
    val isTelnetScanning: StateFlow<Boolean> = _isTelnetScanning.asStateFlow()

    private val _telnetTimeoutMs = MutableStateFlow(TelnetEngine.DEFAULT_TIMEOUT_MS)
    val telnetTimeoutMs: StateFlow<Int> = _telnetTimeoutMs.asStateFlow()

    private var telnetJob: Job? = null

    // Network Info State
    private val _networkInfo = MutableStateFlow(NetworkCardInfo())
    val networkInfo: StateFlow<NetworkCardInfo> = _networkInfo.asStateFlow()

    private var localRefreshJob: Job? = null
    private var publicRefreshJob: Job? = null
    private var lastNetwork: Network? = null

    // History State
    private val _historySessions = MutableStateFlow<List<PingSessionStats>>(emptyList())
    val historySessions: StateFlow<List<PingSessionStats>> = _historySessions.asStateFlow()

    // Telnet History State
    private val _telnetHistory = MutableStateFlow<List<TelnetSessionRecord>>(emptyList())
    val telnetHistory: StateFlow<List<TelnetSessionRecord>> = _telnetHistory.asStateFlow()

    // Update state
    private val _updateInfo = MutableStateFlow<UpdateReleaseInfo?>(null)
    val updateInfo: StateFlow<UpdateReleaseInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateCheckFailed = MutableStateFlow(false)
    val updateCheckFailed: StateFlow<Boolean> = _updateCheckFailed.asStateFlow()

    // Callback chạy trên ConnectivityThread -> chuyển về Main để cập nhật state an toàn
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            viewModelScope.launch {
                val changed = network != lastNetwork
                lastNetwork = network
                scheduleNetworkRefresh(refreshPublic = changed)
            }
        }

        override fun onLost(network: Network) {
            viewModelScope.launch {
                if (network == lastNetwork) lastNetwork = null
                scheduleNetworkRefresh(refreshPublic = true)
            }
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            viewModelScope.launch { scheduleNetworkRefresh(refreshPublic = false) }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            // Bỏ qua thay đổi cường độ tín hiệu liên tục, chỉ làm mới thông tin cục bộ (có debounce)
            viewModelScope.launch { scheduleNetworkRefresh(refreshPublic = false) }
        }
    }

    init {
        viewModelScope.launch {
            _historySessions.value = storage.loadHistory()
            _telnetHistory.value = storage.loadTelnetHistory()
        }
        refreshNetworkInfo()
        registerNetworkCallback()
        checkForUpdates()
    }

    fun setHostInput(host: String) {
        _currentHostInput.value = host
    }

    fun updateConfig(newConfig: PingConfig) {
        _config.value = newConfig
        storage.saveConfig(newConfig)
    }

    fun startPing() {
        if (_pingStats.value.isRunning) return

        val host = sanitizeHost(_currentHostInput.value)
        _currentHostInput.value = host
        storage.saveLastHost(host)

        rttSum = 0.0
        lastRtt = null
        jitterSum = 0.0
        jitterSamples = 0
        _pingStats.value = PingSessionStats(
            targetHost = host,
            startedAt = System.currentTimeMillis(),
            isRunning = true
        )

        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            try {
                PingEngine.startPing(host, _config.value).collect { packet -> onPacket(packet) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onPacket(
                    PingPacket(
                        sequence = 0,
                        ip = host,
                        isInfo = true,
                        rawLog = "Lỗi động cơ ping: ${e.localizedMessage ?: e.javaClass.simpleName}"
                    )
                )
            }
            // Kết thúc tự nhiên (đủ số gói cấu hình hoặc lỗi)
            finishSession()
        }
    }

    private fun onPacket(packet: PingPacket) {
        if (!packet.isInfo) {
            if (packet.isSuccess) {
                rttSum += packet.timeMs
                lastRtt?.let {
                    jitterSum += abs(packet.timeMs - it)
                    jitterSamples++
                }
                lastRtt = packet.timeMs
            }
        }

        _pingStats.update { current ->
            val buffered = (current.packets + packet).takeLast(PingSessionStats.MAX_BUFFERED_PACKETS)
            if (packet.isInfo) {
                current.copy(
                    resolvedIp = current.resolvedIp.ifBlank { packet.ip },
                    packets = buffered
                )
            } else {
                val sent = current.sentCount + 1
                val received = current.receivedCount + if (packet.isSuccess) 1 else 0
                val min = when {
                    !packet.isSuccess -> current.minMs
                    current.receivedCount == 0 -> packet.timeMs
                    else -> minOf(current.minMs, packet.timeMs)
                }
                val max = if (packet.isSuccess) maxOf(current.maxMs, packet.timeMs) else current.maxMs
                current.copy(
                    resolvedIp = if (packet.isSuccess && packet.ip.isNotBlank()) packet.ip else current.resolvedIp,
                    sentCount = sent,
                    receivedCount = received,
                    minMs = min,
                    maxMs = max,
                    avgMs = if (received > 0) (rttSum / received).toFloat() else 0f,
                    jitterMs = if (jitterSamples > 0) (jitterSum / jitterSamples).toFloat() else 0f,
                    lossPercentage = ((sent - received).toFloat() / sent) * 100f,
                    packets = buffered
                )
            }
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        pingJob = null
        finishSession()
    }

    private fun finishSession() {
        val current = _pingStats.value
        if (!current.isRunning) return
        val finished = current.copy(isRunning = false)
        _pingStats.value = finished

        if (finished.sentCount > 0) {
            val updated = (listOf(finished) + _historySessions.value)
                .take(PingStorage.MAX_PING_HISTORY)
            _historySessions.value = updated
            viewModelScope.launch { storage.saveHistory(updated) }
        }
    }

    fun runTraceroute() {
        if (_isTracerouteRunning.value) return
        val host = sanitizeHost(_currentHostInput.value)
        _currentHostInput.value = host
        _tracerouteHops.value = emptyList()
        _tracerouteError.value = null
        _isTracerouteRunning.value = true

        tracerouteJob = viewModelScope.launch {
            try {
                PingEngine.runTraceroute(host) { hop ->
                    _tracerouteHops.update { it + hop }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: UnknownHostException) {
                _tracerouteError.value = "Không phân giải được tên miền \"$host\""
            } catch (e: Exception) {
                _tracerouteError.value = "Lỗi traceroute: ${e.localizedMessage ?: e.javaClass.simpleName}"
            } finally {
                _isTracerouteRunning.value = false
            }
        }
    }

    fun stopTraceroute() {
        tracerouteJob?.cancel()
        tracerouteJob = null
        _isTracerouteRunning.value = false
    }

    // Telnet Actions
    fun setTelnetHost(host: String) {
        _currentTelnetHost.value = host
    }

    fun setTelnetPortInput(input: String) {
        _currentTelnetPortInput.value = input
    }

    fun setTelnetTimeout(timeoutMs: Int) {
        _telnetTimeoutMs.value = timeoutMs
    }

    fun startTelnetScan() {
        if (_isTelnetScanning.value) return
        val host = sanitizeHost(_currentTelnetHost.value)
        _currentTelnetHost.value = host

        val ports = TelnetEngine.parsePortInput(_currentTelnetPortInput.value)
        if (ports.isEmpty()) return

        _telnetResults.value = emptyList()
        _isTelnetScanning.value = true
        _telnetSummary.value = TelnetScanSummary(
            targetHost = host,
            totalPorts = ports.size,
            scannedPorts = 0,
            openCount = 0,
            closedCount = 0,
            timeoutCount = 0,
            isScanning = true
        )

        telnetJob?.cancel()
        telnetJob = viewModelScope.launch {
            try {
                TelnetEngine.scanPorts(host, ports, _telnetTimeoutMs.value).collect { res ->
                    _telnetResults.update { current ->
                        (current + res).sortedBy { it.port }
                    }
                    _telnetSummary.update { summary ->
                        summary.copy(
                            scannedPorts = summary.scannedPorts + 1,
                            openCount = summary.openCount + if (res.status == TelnetPortStatus.OPEN) 1 else 0,
                            closedCount = summary.closedCount + if (res.status == TelnetPortStatus.CLOSED) 1 else 0,
                            timeoutCount = summary.timeoutCount + if (res.status == TelnetPortStatus.TIMEOUT || res.status == TelnetPortStatus.FILTERED) 1 else 0
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                _isTelnetScanning.value = false
                _telnetSummary.update { it.copy(isScanning = false) }
                finishTelnetSession()
            }
        }
    }

    private fun finishTelnetSession() {
        val summary = _telnetSummary.value
        val results = _telnetResults.value
        if (summary.scannedPorts > 0) {
            val record = TelnetSessionRecord(
                targetHost = summary.targetHost,
                portInput = _currentTelnetPortInput.value,
                timestamp = System.currentTimeMillis(),
                totalPorts = summary.totalPorts,
                openCount = summary.openCount,
                closedCount = summary.closedCount,
                timeoutCount = summary.timeoutCount,
                results = results
            )
            val updated = (listOf(record) + _telnetHistory.value).take(PingStorage.MAX_TELNET_HISTORY)
            _telnetHistory.value = updated
            viewModelScope.launch { storage.saveTelnetHistory(updated) }
        }
    }

    fun stopTelnetScan() {
        telnetJob?.cancel()
        telnetJob = null
        _isTelnetScanning.value = false
        _telnetSummary.update { it.copy(isScanning = false) }
        finishTelnetSession()
    }

    fun clearTelnetResults() {
        _telnetResults.value = emptyList()
        _telnetSummary.value = TelnetScanSummary()
    }

    fun clearTelnetHistory() {
        _telnetHistory.value = emptyList()
        viewModelScope.launch { storage.saveTelnetHistory(emptyList()) }
    }

    fun refreshNetworkInfo() {
        scheduleNetworkRefresh(refreshPublic = true, debounceMs = 0L)
    }

    private fun scheduleNetworkRefresh(refreshPublic: Boolean, debounceMs: Long = 600L) {
        localRefreshJob?.cancel()
        localRefreshJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val local = NetworkInfoService.getLocalNetworkInfo(getApplication())
            _networkInfo.update { old ->
                local.copy(
                    publicIp = old.publicIp,
                    isp = old.isp,
                    country = old.country,
                    publicInfoState = old.publicInfoState
                )
            }
        }

        if (refreshPublic) {
            publicRefreshJob?.cancel()
            publicRefreshJob = viewModelScope.launch {
                if (debounceMs > 0) delay(debounceMs)
                _networkInfo.update { it.copy(publicInfoState = PublicInfoState.LOADING) }
                val info = NetworkInfoService.fetchPublicIpInfo()
                _networkInfo.update {
                    if (info != null) {
                        it.copy(
                            publicIp = info.ip,
                            isp = info.isp,
                            country = info.country,
                            publicInfoState = PublicInfoState.SUCCESS
                        )
                    } else {
                        it.copy(publicIp = "", isp = "", country = "", publicInfoState = PublicInfoState.ERROR)
                    }
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
            // Thiết bị không hỗ trợ -> người dùng vẫn có thể làm mới thủ công
        }
    }

    fun checkForUpdates() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val release = updateManager.checkForUpdates()
            _updateInfo.value = release
            _updateCheckFailed.value = release == null
            _isCheckingUpdate.value = false
        }
    }

    fun downloadAndInstallUpdate() {
        val info = _updateInfo.value ?: return
        if (info.apkDownloadUrl.isNotBlank()) {
            updateManager.startDownloadAndInstall(info.apkDownloadUrl, info.tagName)
        }
    }

    fun clearHistory() {
        _historySessions.value = emptyList()
        viewModelScope.launch { storage.saveHistory(emptyList()) }
    }

    override fun onCleared() {
        pingJob?.cancel()
        tracerouteJob?.cancel()
        telnetJob?.cancel()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        updateManager.release()
        super.onCleared()
    }

    companion object {
        const val DEFAULT_HOST = "8.8.8.8"

        /** Chuẩn hóa đầu vào: bỏ scheme (http://), đường dẫn, cổng và khoảng trắng. */
        fun sanitizeHost(input: String): String {
            var h = input.trim()
            h = h.substringAfter("://", h)
            h = h.substringBefore('/').substringBefore('?').substringBefore('#')
            h = h.substringAfter('@', h)
            // Bỏ cổng với IPv4/tên miền (không áp dụng IPv6 có nhiều dấu ':')
            if (h.count { it == ':' } == 1) h = h.substringBefore(':')
            h = h.removePrefix("[").removeSuffix("]").trim()
            return h.ifEmpty { DEFAULT_HOST }
        }
    }
}
