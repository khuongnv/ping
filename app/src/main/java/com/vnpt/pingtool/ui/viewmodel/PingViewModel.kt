package com.vnpt.pingtool.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vnpt.pingtool.data.engine.PingEngine
import com.vnpt.pingtool.data.model.*
import com.vnpt.pingtool.data.network.NetworkInfoService
import com.vnpt.pingtool.data.update.GitHubUpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PingViewModel(application: Application) : AndroidViewModel(application) {

    private val updateManager = GitHubUpdateManager(application.applicationContext)

    // Ping State
    private val _pingStats = MutableStateFlow(PingSessionStats(targetHost = "8.8.8.8"))
    val pingStats: StateFlow<PingSessionStats> = _pingStats.asStateFlow()

    private val _currentHostInput = MutableStateFlow("8.8.8.8")
    val currentHostInput: StateFlow<String> = _currentHostInput.asStateFlow()

    private val _config = MutableStateFlow(PingConfig())
    val config: StateFlow<PingConfig> = _config.asStateFlow()

    private var pingJob: Job? = null

    // Traceroute State
    private val _tracerouteHops = MutableStateFlow<List<TracerouteHop>>(emptyList())
    val tracerouteHops: StateFlow<List<TracerouteHop>> = _tracerouteHops.asStateFlow()

    private val _isTracerouteRunning = MutableStateFlow(false)
    val isTracerouteRunning: StateFlow<Boolean> = _isTracerouteRunning.asStateFlow()

    // Network Info State
    private val _networkInfo = MutableStateFlow(NetworkCardInfo())
    val networkInfo: StateFlow<NetworkCardInfo> = _networkInfo.asStateFlow()

    // History State
    private val _historySessions = MutableStateFlow<List<PingSessionStats>>(emptyList())
    val historySessions: StateFlow<List<PingSessionStats>> = _historySessions.asStateFlow()

    // Update state
    private val _updateInfo = MutableStateFlow<UpdateReleaseInfo?>(null)
    val updateInfo: StateFlow<UpdateReleaseInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    init {
        refreshNetworkInfo()
        checkForUpdates()
    }

    fun setHostInput(host: String) {
        _currentHostInput.value = host
    }

    fun updateConfig(newConfig: PingConfig) {
        _config.value = newConfig
    }

    fun startPing() {
        if (_pingStats.value.isRunning) return

        val host = _currentHostInput.value.trim().ifEmpty { "8.8.8.8" }
        _pingStats.value = PingSessionStats(
            targetHost = host,
            isRunning = true
        )

        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            PingEngine.startPing(host, _config.value).collect { packet ->
                _pingStats.update { current ->
                    val updatedPackets = current.packets + packet
                    val successPackets = updatedPackets.filter { it.isSuccess && it.timeMs > 0 }
                    val sent = updatedPackets.size
                    val received = successPackets.size
                    val min = if (successPackets.isNotEmpty()) successPackets.minOf { it.timeMs } else 0f
                    val max = if (successPackets.isNotEmpty()) successPackets.maxOf { it.timeMs } else 0f
                    val avg = if (successPackets.isNotEmpty()) successPackets.map { it.timeMs }.average().toFloat() else 0f
                    val loss = if (sent > 0) ((sent - received).toFloat() / sent) * 100f else 0f

                    current.copy(
                        resolvedIp = packet.ip,
                        sentCount = sent,
                        receivedCount = received,
                        minMs = min,
                        avgMs = avg,
                        maxMs = max,
                        lossPercentage = loss,
                        packets = updatedPackets
                    )
                }
            }
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        pingJob = null
        val finishedSession = _pingStats.value.copy(isRunning = false)
        _pingStats.value = finishedSession

        // Save to history if at least 1 packet was sent
        if (finishedSession.sentCount > 0) {
            _historySessions.update { listOf(finishedSession) + it.take(49) }
        }
    }

    fun runTraceroute() {
        if (_isTracerouteRunning.value) return
        val host = _currentHostInput.value.trim().ifEmpty { "8.8.8.8" }
        _tracerouteHops.value = emptyList()
        _isTracerouteRunning.value = true

        viewModelScope.launch {
            PingEngine.runTraceroute(host) { hop ->
                _tracerouteHops.update { it + hop }
            }
            _isTracerouteRunning.value = false
        }
    }

    fun refreshNetworkInfo() {
        val localInfo = NetworkInfoService.getLocalNetworkInfo(getApplication())
        _networkInfo.value = localInfo

        viewModelScope.launch {
            val (pubIp, isp, country) = NetworkInfoService.fetchPublicIpInfo()
            _networkInfo.update {
                it.copy(
                    publicIp = pubIp,
                    isp = isp,
                    country = country
                )
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val release = updateManager.checkForUpdates()
            _updateInfo.value = release
            _isCheckingUpdate.value = false
        }
    }

    fun downloadAndInstallUpdate() {
        val url = _updateInfo.value?.apkDownloadUrl
        if (!url.isNullOrBlank()) {
            updateManager.startDownloadAndInstall(url)
        }
    }

    fun clearHistory() {
        _historySessions.value = emptyList()
    }
}
