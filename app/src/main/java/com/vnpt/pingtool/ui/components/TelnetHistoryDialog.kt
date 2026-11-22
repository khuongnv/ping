package com.vnpt.pingtool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vnpt.pingtool.data.model.TelnetPortResult
import com.vnpt.pingtool.data.model.TelnetPortStatus
import com.vnpt.pingtool.data.model.TelnetSessionRecord
import com.vnpt.pingtool.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelnetHistoryDialog(
    records: List<TelnetSessionRecord>,
    onSelectSession: (host: String, ports: String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    var viewingRecord by remember { mutableStateOf<TelnetSessionRecord?>(null) }

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
            if (viewingRecord != null) {
                // Chi tiết phiên quét Telnet
                TelnetSessionDetailView(
                    record = viewingRecord!!,
                    onBack = { viewingRecord = null },
                    onReScan = {
                        onSelectSession(viewingRecord!!.targetHost, viewingRecord!!.portInput)
                        onDismiss()
                    }
                )
            } else {
                // Danh sách lịch sử Telnet
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
                            text = "Lịch sử quét Telnet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (records.isNotEmpty()) {
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

                    if (records.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có phiên quét Telnet nào được lưu.",
                                color = TextSecondaryDark,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(records) { rec ->
                                LiquidGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewingRecord = rec },
                                    contentPadding = PaddingValues(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rec.targetHost,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            val timeStr = if (rec.timestamp > 0) dateFormat.format(Date(rec.timestamp)) + " • " else ""
                                            Text(
                                                text = "${timeStr}Cổng: ${rec.portInput} (${rec.totalPorts} ports)",
                                                fontSize = 12.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = "Chạm để xem chi tiết từng cổng",
                                                fontSize = 10.sp,
                                                color = PrimaryAccent,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            StatusCounterBadge(
                                                label = "Mở",
                                                count = rec.openCount,
                                                color = LatencyGood
                                            )
                                            StatusCounterBadge(
                                                label = "Đóng",
                                                count = rec.closedCount,
                                                color = PacketLossColor
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
private fun StatusCounterBadge(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$label: $count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TelnetSessionDetailView(
    record: TelnetSessionRecord,
    onBack: () -> Unit,
    onReScan: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    var filterOpenOnly by remember { mutableStateOf(false) }

    val displayResults = remember(record.results, filterOpenOnly) {
        if (filterOpenOnly) record.results.filter { it.status == TelnetPortStatus.OPEN }
        else record.results
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        text = record.targetHost,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    if (record.timestamp > 0) {
                        Text(
                            text = dateFormat.format(Date(record.timestamp)),
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            GlassCapsuleButton(
                text = "Quét lại",
                icon = Icons.Default.PlayArrow,
                isActive = true,
                modifier = Modifier.height(34.dp),
                onClick = onReScan
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(12.dp)
        ) {
            Text(
                text = "DẢI CỔNG ĐÃ QUÉT: ${record.portInput} (${record.totalPorts} ports)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "${record.openCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LatencyGood)
                    Text(text = "Cổng mở", fontSize = 11.sp, color = TextSecondaryDark)
                }
                Column {
                    Text(text = "${record.closedCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PacketLossColor)
                    Text(text = "Cổng đóng", fontSize = 11.sp, color = TextSecondaryDark)
                }
                Column {
                    Text(text = "${record.timeoutCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LatencyMedium)
                    Text(text = "Timeout", fontSize = 11.sp, color = TextSecondaryDark)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter button
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!filterOpenOnly) PrimaryAccent.copy(alpha = 0.2f) else Color(0x222C2C2E))
                        .clickable { filterOpenOnly = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Tất cả (${record.results.size})",
                        fontSize = 11.sp,
                        color = if (!filterOpenOnly) PrimaryAccent else TextSecondaryDark
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (filterOpenOnly) LatencyGood.copy(alpha = 0.2f) else Color(0x222C2C2E))
                        .clickable { filterOpenOnly = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Chỉ cổng mở (${record.openCount})",
                        fontSize = 11.sp,
                        color = if (filterOpenOnly) LatencyGood else TextSecondaryDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Results List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayResults, key = { it.port }) { item ->
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Port ${item.port}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                if (item.serviceName.isNotBlank()) {
                                    Text(
                                        text = "• ${item.serviceName}",
                                        fontSize = 12.sp,
                                        color = PrimaryAccent
                                    )
                                }
                            }
                            if (item.errorDetail.isNotBlank() && item.status != TelnetPortStatus.OPEN) {
                                Text(
                                    text = item.errorDetail,
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        val (statusText, statusColor) = when (item.status) {
                            TelnetPortStatus.OPEN -> "MỞ" to LatencyGood
                            TelnetPortStatus.CLOSED -> "ĐÓNG" to PacketLossColor
                            TelnetPortStatus.TIMEOUT -> "TIMEOUT" to LatencyMedium
                            TelnetPortStatus.FILTERED -> "LỌC" to LatencyMedium
                            TelnetPortStatus.ERROR -> "LỖI" to TextSecondaryDark
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (item.status == TelnetPortStatus.OPEN && item.timeMs > 0) {
                                Text(
                                    text = "${item.timeMs.toInt()} ms",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = latencyColor(item.timeMs)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
