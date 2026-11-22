package com.vnpt.pingtool.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
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
import com.vnpt.pingtool.data.model.TelnetPortResult
import com.vnpt.pingtool.data.model.TelnetPortStatus
import com.vnpt.pingtool.data.model.TelnetScanSummary
import com.vnpt.pingtool.ui.components.GlassCapsuleButton
import com.vnpt.pingtool.ui.components.LiquidGlassCard
import com.vnpt.pingtool.ui.theme.*
import java.util.Locale

private enum class PortFilter { ALL, OPEN_ONLY, CLOSED_ONLY, TIMEOUT_ONLY }

@Composable
fun TelnetScreen(
    currentHost: String,
    onHostChange: (String) -> Unit,
    portInput: String,
    onPortInputChange: (String) -> Unit,
    results: List<TelnetPortResult>,
    summary: TelnetScanSummary,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClearResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var selectedFilter by remember { mutableStateOf(PortFilter.ALL) }

    val quickHosts = listOf("8.8.8.8", "1.1.1.1", "google.com", "192.168.1.1")

    val quickPortPresets = listOf(
        "Web (80, 443, 8080)" to "80, 443, 8080",
        "Dải Web (80-88)" to "80-88",
        "Quản trị (22, 23, 3389)" to "22, 23, 3389",
        "Database (3306, 5432)" to "3306, 5432, 1433, 27017",
        "Dải 8000-8010" to "8000-8010"
    )

    val filteredResults = remember(results, selectedFilter) {
        when (selectedFilter) {
            PortFilter.ALL -> results
            PortFilter.OPEN_ONLY -> results.filter { it.status == TelnetPortStatus.OPEN }
            PortFilter.CLOSED_ONLY -> results.filter { it.status == TelnetPortStatus.CLOSED }
            PortFilter.TIMEOUT_ONLY -> results.filter {
                it.status == TelnetPortStatus.TIMEOUT || it.status == TelnetPortStatus.FILTERED
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Large Title
        Text(
            text = "Telnet",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Glass Input Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(14.dp)
        ) {
            Text(
                text = "ĐỊA CHỈ IP HOẶC TÊN MIỀN MỤC TIÊU",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = currentHost,
                onValueChange = onHostChange,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = LuminousRimBorderDark,
                    focusedContainerColor = Color(0x33000000),
                    unfocusedContainerColor = Color(0x22000000)
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Nhập IP (vd: 192.168.1.1 hoặc google.com)", color = TextSecondaryDark) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Quick Host Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickHosts.forEach { host ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x332C2C2E))
                            .border(0.5.dp, LuminousRimBorderDark, RoundedCornerShape(12.dp))
                            .clickable { onHostChange(host) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = host,
                            fontSize = 11.sp,
                            color = if (currentHost == host) PrimaryAccent else TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CỔNG HOẶC DẢI CỔNG (VD: 80, 443 HOẶC 80-90)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = portInput,
                onValueChange = onPortInputChange,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = LuminousRimBorderDark,
                    focusedContainerColor = Color(0x33000000),
                    unfocusedContainerColor = Color(0x22000000)
                ),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Nhập cổng: 80, 443 hoặc 80-90", color = TextSecondaryDark) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            // Quick Port Presets
            Text(
                text = "Gợi ý cổng phổ biến:",
                fontSize = 11.sp,
                color = TextSecondaryDark,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPortPresets.take(3).forEach { (label, value) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x262C2C2E))
                            .border(0.5.dp, LuminousRimBorderDark, RoundedCornerShape(10.dp))
                            .clickable { onPortInputChange(value) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (portInput == value) PrimaryAccent else TextPrimaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isScanning) {
                    GlassCapsuleButton(
                        text = "Kiểm tra Telnet",
                        icon = Icons.Default.PlayArrow,
                        isActive = true,
                        accentColor = PrimaryAccent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            focusManager.clearFocus()
                            onStartScan()
                        }
                    )
                } else {
                    GlassCapsuleButton(
                        text = "Dừng quét",
                        icon = Icons.Default.Stop,
                        isActive = true,
                        accentColor = PacketLossColor,
                        modifier = Modifier.weight(1f),
                        onClick = onStopScan
                    )
                }

                if (results.isNotEmpty() && !isScanning) {
                    GlassCapsuleButton(
                        text = "Xóa",
                        icon = Icons.Default.Clear,
                        isActive = false,
                        modifier = Modifier.width(90.dp),
                        onClick = onClearResults
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scan Progress & Summary Card
        AnimatedVisibility(visible = results.isNotEmpty() || isScanning) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                if (isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đang quét cổng: ${summary.scannedPorts} / ${summary.totalPorts}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent
                        )
                        Text(
                            text = "${(summary.progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { summary.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = PrimaryAccent,
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Summary Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatPill(label = "Đã quét", count = summary.scannedPorts, color = TextPrimaryDark)
                    StatPill(label = "Cổng mở", count = summary.openCount, color = LatencyGood)
                    StatPill(label = "Cổng đóng", count = summary.closedCount, color = PacketLossColor)
                    StatPill(label = "Timeout", count = summary.timeoutCount, color = LatencyMedium)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        label = "Tất cả",
                        isSelected = selectedFilter == PortFilter.ALL,
                        onClick = { selectedFilter = PortFilter.ALL }
                    )
                    FilterChip(
                        label = "Cổng mở (${summary.openCount})",
                        isSelected = selectedFilter == PortFilter.OPEN_ONLY,
                        accentColor = LatencyGood,
                        onClick = { selectedFilter = PortFilter.OPEN_ONLY }
                    )
                    FilterChip(
                        label = "Đóng (${summary.closedCount})",
                        isSelected = selectedFilter == PortFilter.CLOSED_ONLY,
                        accentColor = PacketLossColor,
                        onClick = { selectedFilter = PortFilter.CLOSED_ONLY }
                    )
                    FilterChip(
                        label = "Timeout (${summary.timeoutCount})",
                        isSelected = selectedFilter == PortFilter.TIMEOUT_ONLY,
                        accentColor = LatencyMedium,
                        onClick = { selectedFilter = PortFilter.TIMEOUT_ONLY }
                    )
                }
            }
        }

        // Results List
        if (results.isEmpty() && !isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nhập IP/tên miền và số cổng rồi nhấn 'Kiểm tra Telnet'.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredResults, key = { it.port }) { item ->
                    PortResultItem(item = item)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondaryDark
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color = PrimaryAccent,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0x222C2C2E))
            .border(
                0.5.dp,
                if (isSelected) accentColor else LuminousRimBorderDark,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) accentColor else TextSecondaryDark
        )
    }
}

@Composable
private fun PortResultItem(item: TelnetPortResult) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Port ${item.port}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    if (item.serviceName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x332C2C2E))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.serviceName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryAccent
                            )
                        }
                    }
                }
                if (item.errorDetail.isNotBlank() && item.status != TelnetPortStatus.OPEN) {
                    Text(
                        text = item.errorDetail,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val (statusText, statusColor) = when (item.status) {
                    TelnetPortStatus.OPEN -> "MỞ (OPEN)" to LatencyGood
                    TelnetPortStatus.CLOSED -> "ĐÓNG (CLOSED)" to PacketLossColor
                    TelnetPortStatus.TIMEOUT -> "TIMEOUT" to LatencyMedium
                    TelnetPortStatus.FILTERED -> "LỌC (FILTERED)" to LatencyMedium
                    TelnetPortStatus.ERROR -> "LỖI" to TextSecondaryDark
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(0.5.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                if (item.status == TelnetPortStatus.OPEN && item.timeMs > 0) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", item.timeMs)} ms",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = latencyColor(item.timeMs),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
