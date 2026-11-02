package com.vnpt.pingtool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnpt.pingtool.data.model.PingSessionStats
import com.vnpt.pingtool.data.model.TracerouteHop
import com.vnpt.pingtool.ui.components.GlassCapsuleButton
import com.vnpt.pingtool.ui.components.LiquidGlassCard
import com.vnpt.pingtool.ui.components.RealtimePingChart
import com.vnpt.pingtool.ui.components.TerminalLogPanel
import com.vnpt.pingtool.ui.theme.*

@Composable
fun PingScreen(
    currentHost: String,
    onHostChange: (String) -> Unit,
    stats: PingSessionStats,
    isTracerouteRunning: Boolean,
    tracerouteHops: List<TracerouteHop>,
    onStartPing: () -> Unit,
    onStopPing: () -> Unit,
    onRunTraceroute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    var showTracerouteDialog by remember { mutableStateOf(false) }

    val quickHosts = listOf("8.8.8.8", "1.1.1.1", "google.com", "cloudflare.com")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Large Title per Section 4
        Text(
            text = "Ping",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Glass Input Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(14.dp)
        ) {
            Text(
                text = "ĐỊA CHỈ IP HOẶC TÊN MIỀN",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentHost,
                    onValueChange = onHostChange,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = LuminousRimBorderDark,
                        focusedContainerColor = Color(0x33000000),
                        unfocusedContainerColor = Color(0x22000000)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Nhập IP (ví dụ: 8.8.8.8)", color = TextSecondaryDark) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            // Quick Host Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickHosts.forEach { host ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x332C2C2E))
                            .border(0.5.dp, LuminousRimBorderDark, RoundedCornerShape(12.dp))
                            .clickable { onHostChange(host) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = host,
                            fontSize = 12.sp,
                            color = if (currentHost == host) PrimaryAccent else TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Start / Stop / Traceroute)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!stats.isRunning) {
                    GlassCapsuleButton(
                        text = "Bắt đầu",
                        icon = Icons.Default.PlayArrow,
                        isActive = true,
                        accentColor = PrimaryAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onStartPing
                    )
                } else {
                    GlassCapsuleButton(
                        text = "Dừng lại",
                        icon = Icons.Default.Stop,
                        isActive = true,
                        accentColor = PacketLossColor,
                        modifier = Modifier.weight(1f),
                        onClick = onStopPing
                    )
                }

                GlassCapsuleButton(
                    text = "Traceroute",
                    icon = Icons.Default.Route,
                    isActive = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showTracerouteDialog = true
                        onRunTraceroute()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Realtime Ping Graph
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            RealtimePingChart(
                packets = stats.packets,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Metrics Grid (Latency & Packets)
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(14.dp)
        ) {
            Text(
                text = "THỜI GIAN ĐỘ TRỄ (RTT)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Trung bình", value = String.format(java.util.Locale.US, "%.1f ms", stats.avgMs), color = LatencyMedium)
                MetricItem(label = "Tối đa", value = String.format(java.util.Locale.US, "%.1f ms", stats.maxMs), color = LatencyPoor)
                MetricItem(label = "Tối thiểu", value = String.format(java.util.Locale.US, "%.1f ms", stats.minMs), color = LatencyGood)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(HairlineDividerDark))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "THỐNG KÊ GÓI TIN",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Đã gửi", value = "${stats.sentCount}")
                MetricItem(label = "Đã nhận", value = "${stats.receivedCount}")
                MetricItem(
                    label = "Tỉ lệ mất",
                    value = String.format(java.util.Locale.US, "%.0f%%", stats.lossPercentage),
                    color = if (stats.lossPercentage > 0) PacketLossColor else LatencyGood
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Logs Panel
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp)
        ) {
            Text(
                text = "NHẬT KÝ LỆNH (LIVE LOG)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TerminalLogPanel(packets = stats.packets)
        }

        Spacer(modifier = Modifier.height(80.dp)) // Padding for floating bar
    }

    // Traceroute Dialog
    if (showTracerouteDialog) {
        AlertDialog(
            onDismissRequest = { showTracerouteDialog = false },
            containerColor = CardSurface,
            title = {
                Text(
                    text = "Traceroute đến $currentHost",
                    color = TextPrimaryDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (tracerouteHops.isEmpty()) {
                        Text(
                            text = if (isTracerouteRunning) "Đang dò từng chặng mạng (hop)..." else "Bắt đầu dò...",
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    } else {
                        tracerouteHops.forEach { hop ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${hop.hopNumber}. ${hop.ip}",
                                    color = if (hop.isTimeout) PacketLossColor else TextPrimaryDark,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (hop.isTimeout) "* Timeout" else "${hop.timeMs.toInt()} ms",
                                    color = if (hop.isTimeout) PacketLossColor else LatencyGood,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTracerouteDialog = false }) {
                    Text("Đóng", color = PrimaryAccent)
                }
            }
        )
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    color: Color = TextPrimaryDark
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondaryDark
        )
    }
}
