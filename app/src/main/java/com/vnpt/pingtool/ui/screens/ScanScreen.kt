package com.vnpt.pingtool.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnpt.pingtool.data.engine.WifiScanEngine
import com.vnpt.pingtool.data.model.*
import com.vnpt.pingtool.ui.components.GlassCapsuleButton
import com.vnpt.pingtool.ui.components.LiquidGlassCard
import com.vnpt.pingtool.ui.components.PasswordAuditHistoryDialog
import com.vnpt.pingtool.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ScanScreen(
    wifiList: List<WifiScanItem>,
    isWifiScanning: Boolean,
    wifiScanError: String?,
    onScanWifi: () -> Unit,
    auditConfig: PasswordAuditConfig,
    onUpdateAuditConfig: (PasswordAuditConfig) -> Unit,
    onSetAuditTargetSsid: (String) -> Unit,
    auditStatus: PasswordAuditStatus,
    auditLogs: List<AuditLogEntry>,
    targetPasswordInput: String,
    onTargetPasswordChange: (String) -> Unit,
    passwordStrength: PasswordStrengthResult?,
    isSyncingDictionary: Boolean,
    dictionaryCount: Int,
    onSyncOnlineDictionary: () -> Unit,
    onStartAudit: () -> Unit,
    onStopAudit: () -> Unit,
    onClearAuditLogs: () -> Unit,
    auditHistory: List<PasswordAuditMatchRecord> = emptyList(),
    onDeleteAuditRecord: (String) -> Unit = {},
    onClearAuditHistory: () -> Unit = {},
    onConnectWifi: (ssid: String, password: String) -> Unit = { _, _ -> },
    onOpenSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Network Scanner, 1: Password Auditor
    var passwordVisible by remember { mutableStateOf(false) }
    var showAuditHistoryDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onScanWifi()
    }

    LaunchedEffect(Unit) {
        if (wifiList.isEmpty()) {
            if (WifiScanEngine.hasScanPermissions(context)) {
                onScanWifi()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onOpenSidebar) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Mở menu",
                        tint = TextPrimaryDark
                    )
                }

                Text(
                    text = "WiFi Inspector",
                    style = MaterialTheme.typography.headlineLarge
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryAccent.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SCAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryAccent
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedSubTab == 1) {
                    // Password auditor tab header action: Sync online dictionary
                    if (isSyncingDictionary) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PrimaryAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onSyncOnlineDictionary) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Tải từ điển mạng",
                                tint = PrimaryAccent
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-Tab Switcher Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSurfaceDark)
                .border(0.75.dp, LuminousRimBorderDark, RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            val tabs = listOf(
                "Quét mạng & Bảo mật" to Icons.Default.WifiFind,
                "Password Auditor" to Icons.Default.Security
            )

            tabs.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryAccent.copy(alpha = 0.22f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 0.75.dp else 0.dp,
                            color = if (isSelected) PrimaryAccent.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedSubTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) PrimaryAccent else TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TextPrimaryDark else TextSecondaryDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nội dung theo Tab đã chọn
        when (selectedSubTab) {
            0 -> NetworkScannerTab(
                wifiList = wifiList,
                isScanning = isWifiScanning,
                errorMessage = wifiScanError,
                onScan = {
                    if (!WifiScanEngine.hasScanPermissions(context)) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        onScanWifi()
                    }
                },
                onSelectNetworkToAudit = { ssid ->
                    onSetAuditTargetSsid(ssid)
                    selectedSubTab = 1
                    Toast.makeText(context, "Đã chuyển mạng \"$ssid\" sang Password Auditor", Toast.LENGTH_SHORT).show()
                }
            )
            1 -> PasswordAuditorTab(
                config = auditConfig,
                onUpdateConfig = onUpdateAuditConfig,
                status = auditStatus,
                logs = auditLogs,
                targetPassword = targetPasswordInput,
                onTargetPasswordChange = onTargetPasswordChange,
                strength = passwordStrength,
                dictionaryCount = dictionaryCount,
                isSyncingDictionary = isSyncingDictionary,
                onSyncDictionary = onSyncOnlineDictionary,
                onStartAudit = onStartAudit,
                onStopAudit = onStopAudit,
                onClearLogs = onClearAuditLogs,
                auditHistory = auditHistory,
                onOpenAuditHistory = { showAuditHistoryDialog = true }
            )
        }
    }

    // Modal xem lịch sử log mật khẩu đã khớp
    if (showAuditHistoryDialog) {
        PasswordAuditHistoryDialog(
            records = auditHistory,
            onDeleteRecord = onDeleteAuditRecord,
            onClearHistory = onClearAuditHistory,
            onConnectWifi = onConnectWifi,
            onDismiss = { showAuditHistoryDialog = false }
        )
    }
}

// ==========================================
// TAB 1: NETWORK SCANNER & SECURITY ANALYZER
// ==========================================

@Composable
private fun NetworkScannerTab(
    wifiList: List<WifiScanItem>,
    isScanning: Boolean,
    errorMessage: String?,
    onScan: () -> Unit,
    onSelectNetworkToAudit: (String) -> Unit
) {
    val context = LocalContext.current
    val hasPermission = remember(wifiList) { WifiScanEngine.hasScanPermissions(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!hasPermission) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yêu cầu quyền vị trí để quét Wi-Fi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF9F0A)
                        )
                        Text(
                            text = "Android yêu cầu quyền vị trí để quét và nhận diện danh sách Access Point lân cận.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassCapsuleButton(
                        text = "Cấp quyền",
                        isActive = true,
                        onClick = onScan
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Summary Card
        val openCount = remember(wifiList) { wifiList.count { it.securityType == WifiSecurityType.OPEN } }
        val wpa3Count = remember(wifiList) { wifiList.count { it.securityType == WifiSecurityType.WPA3_SAE } }
        val wpa2Count = remember(wifiList) { wifiList.count { it.securityType == WifiSecurityType.WPA2_PSK } }

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
                    Text(
                        text = "PHÂN TÍCH TỔNG QUAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = if (wifiList.isNotEmpty()) "Phát hiện ${wifiList.size} điểm phát Wi-Fi" else "Chưa có dữ liệu quét",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SecurityStatBadge(count = wpa3Count, label = "WPA3", color = Color(0xFF00E676))
                        SecurityStatBadge(count = wpa2Count, label = "WPA2", color = Color(0xFF00E5FF))
                        if (openCount > 0) {
                            SecurityStatBadge(count = openCount, label = "Mở ⚠️", color = Color(0xFFFF9F0A))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Nút Quét lại (Scan) căn phải theo yêu cầu
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryAccent.copy(alpha = 0.15f))
                            .border(0.75.dp, PrimaryAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = PrimaryAccent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Đang quét...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryAccent.copy(alpha = 0.15f))
                            .border(0.75.dp, PrimaryAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onScan)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Quét Wi-Fi",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Quét lại",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (errorMessage != null && wifiList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassCapsuleButton(
                        text = "Quét lại",
                        icon = Icons.Default.Refresh,
                        isActive = true,
                        onClick = onScan
                    )
                }
            }
        } else if (wifiList.isEmpty() && isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Đang quét tín hiệu Wi-Fi xung quanh...",
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(wifiList, key = { it.bssid }) { item ->
                    WifiNetworkCard(
                        item = item,
                        onAuditClick = { onSelectNetworkToAudit(item.ssid) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityStatBadge(count: Int, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$count $label",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun WifiNetworkCard(
    item: WifiScanItem,
    onAuditClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Biểu tượng cường độ sóng
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (item.isConnected) PrimaryAccent.copy(alpha = 0.25f)
                            else Color(0x1AFFFFFF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.level) {
                            4 -> Icons.Default.Wifi
                            3 -> Icons.Default.Wifi
                            2 -> Icons.Default.Wifi2Bar
                            1 -> Icons.Default.Wifi1Bar
                            else -> Icons.Default.WifiOff
                        },
                        contentDescription = null,
                        tint = if (item.isConnected) PrimaryAccent else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.ssid,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        if (item.isConnected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Đang kết nối",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${item.bssid} • ${item.bandLabel} (Kênh ${item.channel}) • ${item.rssi} dBm",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            // Badge chuẩn bảo mật & Icon mở rộng
            val badgeBg = Color(item.securityType.badgeColor).copy(alpha = 0.2f)
            val badgeFg = Color(item.securityType.badgeColor)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(0.5.dp, badgeFg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.securityType.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Cảnh báo mạng không mã hóa hoặc WEP khi ở trạng thái thu gọn
        if (item.securityWarning != null && !isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF9F0A).copy(alpha = 0.12f))
                    .border(0.5.dp, Color(0xFFFF9F0A).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9F0A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.securityWarning,
                        fontSize = 11.sp,
                        color = TextPrimaryDark,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Mở rộng thông tin chi tiết: Đánh giá bảo mật và liên kết Kiểm định
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                HorizontalDivider(color = HairlineDividerDark, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Dòng Đánh giá bảo mật
                Text(
                    text = "Đánh giá bảo mật: ${item.securityRating}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(item.securityType.badgeColor)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Chuỗi mã hóa: ${item.capabilities}",
                    fontSize = 10.sp,
                    color = TextSecondaryDark,
                    fontFamily = FontFamily.Monospace
                )

                if (item.securityWarning != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF9F0A).copy(alpha = 0.12f))
                            .border(0.5.dp, Color(0xFFFF9F0A).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9F0A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.securityWarning,
                                fontSize = 11.sp,
                                color = TextPrimaryDark,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Dưới dòng đánh giá bảo mật: Dòng kiểm định và link sang Password audit
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryAccent.copy(alpha = 0.12f))
                        .border(0.75.dp, PrimaryAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onAuditClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PrimaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Kiểm định mật khẩu mạng này",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryAccent
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Audit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: PASSWORD AUDITOR
// ==========================================

@Composable
private fun PasswordAuditorTab(
    config: PasswordAuditConfig,
    onUpdateConfig: (PasswordAuditConfig) -> Unit,
    status: PasswordAuditStatus,
    logs: List<AuditLogEntry>,
    targetPassword: String,
    onTargetPasswordChange: (String) -> Unit,
    strength: PasswordStrengthResult?,
    dictionaryCount: Int,
    isSyncingDictionary: Boolean,
    onSyncDictionary: () -> Unit,
    onStartAudit: () -> Unit,
    onStopAudit: () -> Unit,
    onClearLogs: () -> Unit,
    auditHistory: List<PasswordAuditMatchRecord> = emptyList(),
    onOpenAuditHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val logListState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // MỤC 1: KIỂM ĐỊNH MẬT KHẨU TỪ ĐIỂN (CONFIG)
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    text = "CẤU HÌNH PASSWORD AUDITOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mục tiêu Wi-Fi SSID
                OutlinedTextField(
                    value = config.targetSsid,
                    onValueChange = { onUpdateConfig(config.copy(targetSsid = it)) },
                    label = { Text("Tên mạng Wi-Fi mục tiêu (SSID)", fontSize = 12.sp) },
                    placeholder = { Text("Nhập hoặc chọn từ tab Quét mạng", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = LuminousRimBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mật khẩu cần kiểm tra trong từ điển (Tùy chọn)
                var showPassword by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = targetPassword,
                    onValueChange = onTargetPasswordChange,
                    label = { Text("Thẩm định độ an toàn mật khẩu (Tùy chọn)", fontSize = 12.sp) },
                    placeholder = {
                        Text("Nhập mật khẩu để kiểm tra độ mạnh & entropy (để trống khi SCAN Wi-Fi)", fontSize = 11.sp)
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondaryDark
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = LuminousRimBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
                val guidanceText = if (targetPassword.isNotBlank()) {
                    "🔍 Chế độ kiểm tra mật khẩu: Đánh giá entropy, mức độ an toàn và tra cứu trong từ điển (Không gán làm mật khẩu Wi-Fi)."
                } else {
                    "📡 Chế độ SCAN Wi-Fi: Tự động rà soát từ điển cấp 1->${config.maxLevel} cho mạng Wi-Fi mục tiêu \"${config.targetSsid.ifBlank { "được chọn" }}\"."
                }
                Text(
                    text = guidanceText,
                    fontSize = 11.sp,
                    color = if (targetPassword.isNotBlank()) PrimaryAccent else TextSecondaryDark,
                    lineHeight = 15.sp
                )

                if (strength != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF16161A))
                            .border(0.75.dp, Color(strength.ratingColor).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Đánh giá: ${strength.ratingLabel}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(strength.ratingColor)
                                )
                                Text(
                                    text = "Entropy: ${String.format(java.util.Locale.US, "%.1f", strength.entropyBits)} bits",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bẻ khóa dự kiến: ${strength.estimatedCrackTime}",
                                fontSize = 11.sp,
                                color = TextPrimaryDark
                            )
                            if (strength.isCommon) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "⚠️ Xếp hạng #${strength.dictionaryRank} trong TOP mật khẩu phổ biến nhất!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF9F0A)
                                )
                            }
                        }
                    }
                }

                // Cấp độ khó từ điển (1 -> 99)
                Spacer(modifier = Modifier.height(14.dp))
                val tierText: String
                val tierColor: Color
                when {
                    config.maxLevel <= 20 -> {
                        tierText = "Cơ bản (Dãy số phổ biến)"
                        tierColor = Color(0xFFFF9F0A)
                    }
                    config.maxLevel <= 40 -> {
                        tierText = "Thông dụng (Năm sinh, ngày tháng)"
                        tierColor = Color(0xFFFFD60A)
                    }
                    config.maxLevel <= 70 -> {
                        tierText = "Trung bình (Router mặc định)"
                        tierColor = Color(0xFF64D2FF)
                    }
                    config.maxLevel <= 90 -> {
                        tierText = "Nâng cao (Từ ngữ & số)"
                        tierColor = Color(0xFF30D158)
                    }
                    else -> {
                        tierText = "Chuyên sâu (Ký tự đặc biệt)"
                        tierColor = Color(0xFFBF5AF2)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ĐỘ KHÓ TỪ ĐIỂN: CẤP 1 ➜ ${config.maxLevel} / 99",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryAccent
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tierColor.copy(alpha = 0.15f))
                            .border(0.5.dp, tierColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tierText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tierColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = config.maxLevel.toFloat(),
                    onValueChange = { onUpdateConfig(config.copy(maxLevel = it.roundToInt().coerceIn(1, 99))) },
                    valueRange = 1f..99f,
                    steps = 97,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAccent,
                        activeTrackColor = PrimaryAccent,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Sẽ scan tuần tự từ cấp 1 đến cấp ${config.maxLevel}. Dừng ngay khi phát hiện khớp mật khẩu hoặc khi tạm dừng.",
                    fontSize = 10.sp,
                    color = TextSecondaryDark,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chọn định dạng ký tự (Chỉ số / Chữ & Số / Kèm đặc biệt / Tất cả)
                Text(
                    text = "Định dạng quét: ${config.charset.label}",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PasswordAuditCharset.values().forEach { charset ->
                        val isSelected = config.charset == charset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryAccent.copy(alpha = 0.2f) else GlassSurfaceDark)
                                .border(
                                    0.75.dp,
                                    if (isSelected) PrimaryAccent else LuminousRimBorderDark,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onUpdateConfig(config.copy(charset = charset)) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (charset) {
                                    PasswordAuditCharset.ALL -> "Tất cả"
                                    PasswordAuditCharset.DIGITS_ONLY -> "Chỉ số"
                                    PasswordAuditCharset.ALPHANUMERIC -> "Chữ+Số"
                                    PasswordAuditCharset.COMPLEX -> "Ký tự lạ"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryAccent else TextPrimaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Độ dài kiểm tra
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Độ dài quét: ${config.minLength} - ${config.maxLength} ký tự",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "Từ điển: $dictionaryCount mẫu",
                        fontSize = 11.sp,
                        color = PrimaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nút Bắt đầu / Dừng kiểm định
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (status.isAuditing) {
                        GlassCapsuleButton(
                            text = "DỪNG SCAN",
                            icon = Icons.Default.Stop,
                            isActive = true,
                            accentColor = Color(0xFFFF9F0A),
                            modifier = Modifier.weight(1f),
                            onClick = onStopAudit
                        )
                    } else {
                        GlassCapsuleButton(
                            text = "SCAN",
                            icon = Icons.Default.PlayArrow,
                            isActive = true,
                            modifier = Modifier.weight(1f),
                            onClick = onStartAudit
                        )
                    }

                    GlassCapsuleButton(
                        text = "Cập nhật từ điển",
                        icon = Icons.Default.CloudDownload,
                        isActive = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSyncDictionary()
                            Toast.makeText(context, "Đang nạp và cập nhật từ điển từ GitHub...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // MỤC: LỊCH SỬ MẬT KHẨU ĐÃ KHỚP (MATCH LOG)
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAuditHistory),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (auditHistory.isNotEmpty()) Color(0xFF30D158).copy(alpha = 0.2f) else Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = if (auditHistory.isNotEmpty()) Color(0xFF30D158) else TextSecondaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Lịch sử mật khẩu đã khớp",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (auditHistory.isNotEmpty()) Color(0xFF30D158).copy(alpha = 0.2f) else TextSecondaryDark.copy(alpha = 0.2f))
                                        .border(0.5.dp, if (auditHistory.isNotEmpty()) Color(0xFF30D158).copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 7.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "${auditHistory.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (auditHistory.isNotEmpty()) Color(0xFF30D158) else TextSecondaryDark
                                    )
                                }
                            }
                            Text(
                                text = if (auditHistory.isNotEmpty()) "Phát hiện ${auditHistory.size} mạng có mật khẩu trong từ điển" else "Chưa có mạng nào phát hiện khớp mật khẩu",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Xem log",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryAccent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // MỤC 2: TIẾN TRÌNH THỰC HIỆN VÀ SỐ LẦN THỬ
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TIẾN TRÌNH & SỐ LẦN THỬ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "${status.currentAttempt} / ${status.totalCandidates} lần thử",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Cấp độ hiện tại",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "Cấp ${status.currentLevel} / 99",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryAccent,
                    trackColor = Color(0x33FFFFFF)
                )

                if (status.currentCandidate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mẫu đang kiểm tra: \"${status.currentCandidate}\" (Cấp ${status.currentLevel}/99 • ${status.attemptsPerSec} thử/s)",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (status.matchedPassword != null) {
                    val isOfflineAudit = targetPassword.isNotBlank()
                    val matchAccentColor = if (isOfflineAudit) Color(0xFFFF9F0A) else Color(0xFF30D158)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(matchAccentColor.copy(alpha = 0.12f))
                            .border(0.75.dp, matchAccentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isOfflineAudit)
                                        "⚠️ MẬT KHẨU NẰM TRONG TỪ ĐIỂN PHỔ BIẾN!"
                                    else
                                        "🎯 ĐÃ PHÁT HIỆN MẬT KHẨU CHO MẠNG WI-FI!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = matchAccentColor
                                )
                                Text(
                                    text = if (isOfflineAudit)
                                        "Mật khẩu: \"${status.matchedPassword}\" (Xếp hạng cấp độ ${status.matchedLevel ?: status.currentLevel}/99 trong từ điển)"
                                    else
                                        "Mật khẩu: \"${status.matchedPassword}\" (Cấp độ ${status.matchedLevel ?: status.currentLevel}/99)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = matchAccentColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (!isOfflineAudit) {
                                Button(
                                    onClick = onOpenAuditHistory,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Xem QR / Kết nối", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                if (status.summaryResult.isNotBlank()) {
                    val summaryColor = if (status.matchedPassword != null) {
                        if (targetPassword.isNotBlank()) Color(0xFFFF9F0A) else Color(0xFF30D158)
                    } else Color(0xFF30D158)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status.summaryResult,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = summaryColor
                    )
                }
            }
        }

        // MỤC 3: NHẬT KÝ THỰC THI (AUDIT LOG)
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NHẬT KÝ THỰC THI (AUDIT LOG)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val fullLog = logs.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Audit Log", fullLog))
                                Toast.makeText(context, "Đã sao chép nhật ký", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Sao chép log",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onClearLogs,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa log",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0C0C0E))
                        .border(0.75.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Nhấn 'SCAN' để bắt đầu quét và theo dõi nhật ký log...",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logs, key = { it.id }) { log ->
                                val textColor = when (log.level) {
                                    AuditLogLevel.ALERT -> Color(0xFFFF9F0A)
                                    AuditLogLevel.WARN -> Color(0xFFFFD60A)
                                    AuditLogLevel.SUCCESS -> Color(0xFF30D158)
                                    AuditLogLevel.INFO -> Color(0xFF9E9EA7)
                                }

                                Text(
                                    text = "[${log.timestamp}] ${log.message}",
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
