package com.vnpt.pingtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vnpt.pingtool.ui.components.AppSidebar
import com.vnpt.pingtool.ui.components.FloatingGlassBottomBar
import com.vnpt.pingtool.ui.components.NavTabItem
import com.vnpt.pingtool.ui.screens.*
import com.vnpt.pingtool.ui.theme.PingToolTheme
import com.vnpt.pingtool.ui.viewmodel.PingViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PingToolTheme {
                var currentRoute by remember { mutableStateOf("ping") }

                val pingStats by viewModel.pingStats.collectAsStateWithLifecycle()
                val currentHost by viewModel.currentHostInput.collectAsStateWithLifecycle()
                val config by viewModel.config.collectAsStateWithLifecycle()
                val tracerouteHops by viewModel.tracerouteHops.collectAsStateWithLifecycle()
                val isTracerouteRunning by viewModel.isTracerouteRunning.collectAsStateWithLifecycle()
                val tracerouteError by viewModel.tracerouteError.collectAsStateWithLifecycle()
                val networkInfo by viewModel.networkInfo.collectAsStateWithLifecycle()
                val historySessions by viewModel.historySessions.collectAsStateWithLifecycle()
                val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
                val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
                val updateCheckFailed by viewModel.updateCheckFailed.collectAsStateWithLifecycle()

                // Telnet states
                val currentTelnetHost by viewModel.currentTelnetHost.collectAsStateWithLifecycle()
                val currentTelnetPortInput by viewModel.currentTelnetPortInput.collectAsStateWithLifecycle()
                val telnetResults by viewModel.telnetResults.collectAsStateWithLifecycle()
                val telnetSummary by viewModel.telnetSummary.collectAsStateWithLifecycle()
                val isTelnetScanning by viewModel.isTelnetScanning.collectAsStateWithLifecycle()
                val telnetHistory by viewModel.telnetHistory.collectAsStateWithLifecycle()

                // Wi-Fi Inspector & Password Auditor states
                val wifiList by viewModel.wifiScanList.collectAsStateWithLifecycle()
                val isWifiScanning by viewModel.isWifiScanning.collectAsStateWithLifecycle()
                val wifiScanError by viewModel.wifiScanError.collectAsStateWithLifecycle()

                val auditConfig by viewModel.auditConfig.collectAsStateWithLifecycle()
                val auditStatus by viewModel.auditStatus.collectAsStateWithLifecycle()
                val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
                val targetPasswordInput by viewModel.targetPasswordInput.collectAsStateWithLifecycle()
                val passwordStrength by viewModel.passwordStrength.collectAsStateWithLifecycle()
                val isSyncingDictionary by viewModel.isSyncingDictionary.collectAsStateWithLifecycle()
                val dictionaryCount by viewModel.dictionaryCount.collectAsStateWithLifecycle()
                val auditHistory by viewModel.auditHistory.collectAsStateWithLifecycle()

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                // 3 tabs chính ở thanh điều hướng đáy theo yêu cầu mới: Ping, Telnet, Scan
                val tabs = listOf(
                    NavTabItem("Ping", Icons.Default.NetworkCheck, "ping"),
                    NavTabItem("Telnet", Icons.Default.Lan, "telnet"),
                    NavTabItem("Scan", Icons.Default.WifiFind, "scan")
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        AppSidebar(
                            currentRoute = currentRoute,
                            onNavigate = { route -> currentRoute = route },
                            updateInfo = updateInfo,
                            isCheckingUpdate = isCheckingUpdate,
                            updateCheckFailed = updateCheckFailed,
                            onCheckUpdate = { viewModel.checkForUpdates() },
                            onDownloadUpdate = { viewModel.downloadAndInstallUpdate() },
                            onCloseSidebar = { coroutineScope.launch { drawerState.close() } }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            FloatingGlassBottomBar(
                                tabs = tabs,
                                currentRoute = currentRoute,
                                onTabSelected = { currentRoute = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentRoute) {
                                "ping" -> PingScreen(
                                    currentHost = currentHost,
                                    onHostChange = { viewModel.setHostInput(it) },
                                    stats = pingStats,
                                    isTracerouteRunning = isTracerouteRunning,
                                    tracerouteHops = tracerouteHops,
                                    tracerouteError = tracerouteError,
                                    historySessions = historySessions,
                                    onClearHistory = { viewModel.clearHistory() },
                                    onOpenSidebar = { coroutineScope.launch { drawerState.open() } },
                                    onStartPing = { viewModel.startPing() },
                                    onStopPing = { viewModel.stopPing() },
                                    onRunTraceroute = { viewModel.runTraceroute() },
                                    onStopTraceroute = { viewModel.stopTraceroute() }
                                )
                                "telnet" -> TelnetScreen(
                                    currentHost = currentTelnetHost,
                                    onHostChange = { viewModel.setTelnetHost(it) },
                                    portInput = currentTelnetPortInput,
                                    onPortInputChange = { viewModel.setTelnetPortInput(it) },
                                    results = telnetResults,
                                    summary = telnetSummary,
                                    isScanning = isTelnetScanning,
                                    telnetHistory = telnetHistory,
                                    onClearTelnetHistory = { viewModel.clearTelnetHistory() },
                                    onOpenSidebar = { coroutineScope.launch { drawerState.open() } },
                                    onStartScan = { viewModel.startTelnetScan() },
                                    onStopScan = { viewModel.stopTelnetScan() },
                                    onClearResults = { viewModel.clearTelnetResults() }
                                )
                                "scan" -> ScanScreen(
                                    wifiList = wifiList,
                                    isWifiScanning = isWifiScanning,
                                    wifiScanError = wifiScanError,
                                    onScanWifi = { viewModel.scanWifiNetworks() },
                                    auditConfig = auditConfig,
                                    onUpdateAuditConfig = { viewModel.updateAuditConfig(it) },
                                    onSetAuditTargetSsid = { viewModel.setAuditTargetSsid(it) },
                                    auditStatus = auditStatus,
                                    auditLogs = auditLogs,
                                    targetPasswordInput = targetPasswordInput,
                                    onTargetPasswordChange = { viewModel.setTargetPasswordInput(it) },
                                    passwordStrength = passwordStrength,
                                    isSyncingDictionary = isSyncingDictionary,
                                    dictionaryCount = dictionaryCount,
                                    onSyncOnlineDictionary = { viewModel.syncOnlineDictionary() },
                                    onStartAudit = { viewModel.startPasswordAudit() },
                                    onStopAudit = { viewModel.stopPasswordAudit() },
                                    onClearAuditLogs = { viewModel.clearAuditLogs() },
                                    auditHistory = auditHistory,
                                    onDeleteAuditRecord = { viewModel.deleteAuditHistoryRecord(it) },
                                    onClearAuditHistory = { viewModel.clearAuditHistory() },
                                    onConnectWifi = { ssid, pwd -> viewModel.connectToWifi(ssid, pwd) },
                                    onOpenSidebar = { coroutineScope.launch { drawerState.open() } }
                                )
                                "my_ip" -> MyIpScreen(
                                    info = networkInfo,
                                    onRefresh = { viewModel.refreshNetworkInfo() },
                                    onOpenSidebar = { coroutineScope.launch { drawerState.open() } },
                                    onBack = { currentRoute = "ping" }
                                )
                                "settings" -> SettingsScreen(
                                    config = config,
                                    onConfigChange = { viewModel.updateConfig(it) },
                                    updateInfo = updateInfo,
                                    isCheckingUpdate = isCheckingUpdate,
                                    updateCheckFailed = updateCheckFailed,
                                    onCheckUpdate = { viewModel.checkForUpdates() },
                                    onDownloadUpdate = { viewModel.downloadAndInstallUpdate() },
                                    onBack = { currentRoute = "ping" },
                                    onOpenSidebar = { coroutineScope.launch { drawerState.open() } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
