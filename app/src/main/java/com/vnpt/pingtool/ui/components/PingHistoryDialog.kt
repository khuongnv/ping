package com.vnpt.pingtool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vnpt.pingtool.data.model.PingSessionStats
import com.vnpt.pingtool.ui.screens.MetricItem
import com.vnpt.pingtool.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PingHistoryDialog(
    sessions: List<PingSessionStats>,
    onSelectHost: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    var viewingSession by remember { mutableStateOf<PingSessionStats?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141418),
            border = androidx.compose.foundation.BorderStroke(0.75.dp, LuminousRimBorderDark)
        ) {
            if (viewingSession != null) {
                // Chi tiết phiên đo kèm BIỂU ĐỒ SÓNG LỊCH SỬ
                PingSessionDetailView(
                    session = viewingSession!!,
                    onBack = { viewingSession = null },
                    onRePing = {
                        onSelectHost(viewingSession!!.targetHost)
                        onDismiss()
                    }
                )
            } else {
                // Danh sách các phiên đo lịch sử
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lịch sử đo Ping",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sessions.isNotEmpty()) {
                                IconButton(onClick = onClearHistory) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa lịch sử",
                                        tint = TextSecondaryDark
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng",
                                    tint = TextSecondaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (sessions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có phiên đo ping nào được lưu.",
                                color = TextSecondaryDark,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sessions) { session ->
                                LiquidGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewingSession = session },
                                    contentPadding = PaddingValues(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.targetHost,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            val timeStr = if (session.startedAt > 0) dateFormat.format(Date(session.startedAt)) + " • " else ""
                                            val ipStr = if (session.resolvedIp.isNotBlank() && session.resolvedIp != session.targetHost) {
                                                "${session.resolvedIp} • "
                                            } else ""
                                            Text(
                                                text = "$timeStr$ipStr${session.sentCount} gói",
                                                fontSize = 12.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = "Chạm để xem lại biểu đồ sóng",
                                                fontSize = 10.sp,
                                                color = PrimaryAccent,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            val rttText = if (session.receivedCount > 0) "${session.avgMs.toInt()} ms" else "--"
                                            val rttColor = latencyColor(session.avgMs, session.receivedCount > 0)
                                            Text(
                                                text = rttText,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = rttColor
                                            )
                                            Text(
                                                text = "Mất ${session.lossPercentage.toInt()}%",
                                                fontSize = 11.sp,
                                                color = if (session.lossPercentage > 0) PacketLossColor else LatencyGood
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PingSessionDetailView(
    session: PingSessionStats,
    onBack: () -> Unit,
    onRePing: () -> Unit
) {
    val scrollState = rememberScrollState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = PrimaryAccent
                    )
                }
                Column {
                    Text(
                        text = session.targetHost,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    if (session.startedAt > 0) {
                        Text(
                            text = dateFormat.format(Date(session.startedAt)),
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            GlassCapsuleButton(
                text = "Ping lại",
                icon = Icons.Default.PlayArrow,
                isActive = true,
                modifier = Modifier.height(34.dp),
                onClick = onRePing
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Biểu đồ sóng Canvas của phiên đo quá khứ
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            RealtimePingChart(
                packets = session.packets,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Thống kê số liệu RTT & Gói tin
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

            val hasPackets = session.receivedCount > 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Trung bình",
                    value = if (hasPackets) "${session.avgMs.toInt()} ms" else "--",
                    color = latencyColor(session.avgMs, hasPackets)
                )
                MetricItem(
                    label = "Tối đa",
                    value = if (hasPackets) "${session.maxMs.toInt()} ms" else "--",
                    color = latencyColor(session.maxMs, hasPackets)
                )
                MetricItem(
                    label = "Tối thiểu",
                    value = if (hasPackets) "${session.minMs.toInt()} ms" else "--",
                    color = latencyColor(session.minMs, hasPackets)
                )
                MetricItem(
                    label = "Jitter",
                    value = if (hasPackets && session.jitterMs > 0) "${session.jitterMs.toInt()} ms" else "--",
                    color = latencyColor(session.jitterMs, hasPackets)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = HairlineDividerDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

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
                MetricItem(label = "Đã gửi", value = "${session.sentCount}")
                MetricItem(label = "Đã nhận", value = "${session.receivedCount}")
                MetricItem(
                    label = "Tỉ lệ mất",
                    value = "${session.lossPercentage.toInt()}%",
                    color = if (session.lossPercentage > 0) PacketLossColor else LatencyGood
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Nhật ký lệnh gói tin
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp)
        ) {
            Text(
                text = "NHẬT KÝ GÓI TIN CHI TIẾT",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TerminalLogPanel(packets = session.packets)
        }
    }
}
