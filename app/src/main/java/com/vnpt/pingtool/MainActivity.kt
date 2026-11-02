package com.vnpt.pingtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vnpt.pingtool.ui.components.FloatingGlassBottomBar
import com.vnpt.pingtool.ui.components.NavTabItem
import com.vnpt.pingtool.ui.screens.HistoryScreen
import com.vnpt.pingtool.ui.screens.MyIpScreen
import com.vnpt.pingtool.ui.screens.PingScreen
import com.vnpt.pingtool.ui.screens.SettingsScreen
import com.vnpt.pingtool.ui.theme.PingToolTheme
import com.vnpt.pingtool.ui.viewmodel.PingViewModel

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
                val networkInfo by viewModel.networkInfo.collectAsStateWithLifecycle()
                val historySessions by viewModel.historySessions.collectAsStateWithLifecycle()
                val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
                val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

                val tabs = listOf(
                    NavTabItem("Ping", Icons.Default.NetworkCheck, "ping"),
                    NavTabItem("Lịch sử", Icons.Default.History, "history"),
                    NavTabItem("IP của tôi", Icons.Default.Info, "my_ip"),
                    NavTabItem("Cài đặt", Icons.Default.Settings, "settings")
                )

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
                                onStartPing = { viewModel.startPing() },
                                onStopPing = { viewModel.stopPing() },
                                onRunTraceroute = { viewModel.runTraceroute() }
                            )
                            "history" -> HistoryScreen(
                                sessions = historySessions,
                                onSelectHost = { host ->
                                    viewModel.setHostInput(host)
                                    currentRoute = "ping"
                                },
                                onClearHistory = { viewModel.clearHistory() }
                            )
                            "my_ip" -> MyIpScreen(
                                info = networkInfo,
                                onRefresh = { viewModel.refreshNetworkInfo() }
                            )
                            "settings" -> SettingsScreen(
                                config = config,
                                onConfigChange = { viewModel.updateConfig(it) },
                                updateInfo = updateInfo,
                                isCheckingUpdate = isCheckingUpdate,
                                onCheckUpdate = { viewModel.checkForUpdates() },
                                onDownloadUpdate = { viewModel.downloadAndInstallUpdate() }
                            )
                        }
                    }
                }
            }
        }
    }
}
