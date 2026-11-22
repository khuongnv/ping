package com.vnpt.pingtool.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnpt.pingtool.data.model.ConnectionType
import com.vnpt.pingtool.data.model.NetworkCardInfo
import com.vnpt.pingtool.data.model.PublicInfoState
import com.vnpt.pingtool.ui.components.LiquidGlassCard
import com.vnpt.pingtool.ui.theme.*

@Composable
fun MyIpScreen(
    info: NetworkCardInfo,
    onRefresh: () -> Unit,
    onOpenSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefresh()
    }

    fun copyToClipboard(label: String, text: String) {
        if (text.isBlank() || text == "Không khả dụng" || text == "Không xác định") return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Đã sao chép $label", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onOpenSidebar) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Mở cài đặt",
                        tint = TextPrimaryDark
                    )
                }
                Text(
                    text = "IP của tôi",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Làm mới",
                    tint = PrimaryAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Local Network Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MẠNG NỘI BỘ (LOCAL NETWORK)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )

                val badgeColor = when (info.connectionType) {
                    ConnectionType.WIFI, ConnectionType.ETHERNET -> LatencyGood
                    ConnectionType.CELLULAR -> LatencyMedium
                    ConnectionType.VPN -> PrimaryAccent
                    ConnectionType.NONE -> PacketLossColor
                    ConnectionType.OTHER -> TextSecondaryDark
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = info.connectionType.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            val localIpLabel = when (info.connectionType) {
                ConnectionType.WIFI -> "IP Wi-Fi (Cục bộ)"
                ConnectionType.CELLULAR -> "IP Di động (Cục bộ)"
                ConnectionType.ETHERNET -> "IP Ethernet (Cục bộ)"
                else -> "IP Cục bộ"
            }

            NetworkFieldRow(
                label = localIpLabel,
                value = info.localIp.ifBlank { "Chưa kết nối" },
                onCopy = { copyToClipboard("IP Cục bộ", info.localIp) }
            )

            if (info.connectionType == ConnectionType.WIFI) {
                HairlineDivider()

                val ssidDisplay = when {
                    info.wifiSsid.isNotBlank() -> info.wifiSsid
                    info.ssidHint.isNotBlank() -> info.ssidHint
                    else -> "Không xác định"
                }

                NetworkFieldRow(
                    label = "Tên Wi-Fi (SSID)",
                    value = ssidDisplay,
                    isHighlight = info.needsLocationPermission,
                    extraActionIcon = if (info.needsLocationPermission) Icons.Default.LocationOn else null,
                    onExtraAction = {
                        if (info.needsLocationPermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onCopy = {
                        if (info.needsLocationPermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else if (info.wifiSsid.isNotBlank()) {
                            copyToClipboard("SSID", info.wifiSsid)
                        }
                    }
                )

                if (info.needsLocationPermission) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LatencyMedium.copy(alpha = 0.15f))
                            .border(0.75.dp, LatencyMedium.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = LatencyMedium,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Chạm để cấp quyền vị trí giúp hiển thị tên Wi-Fi",
                                fontSize = 12.sp,
                                color = LatencyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            HairlineDivider()

            NetworkFieldRow(
                label = "Cổng kết nối (Gateway)",
                value = info.gateway.ifBlank { "Không khả dụng" },
                onCopy = { copyToClipboard("Gateway", info.gateway) }
            )

            HairlineDivider()

            NetworkFieldRow(
                label = "Mặt nạ mạng (Subnet Mask)",
                value = info.subnetMask.ifBlank { "Không khả dụng" },
                onCopy = { copyToClipboard("Subnet", info.subnetMask) }
            )

            HairlineDivider()

            NetworkFieldRow(
                label = "Máy chủ DNS",
                value = info.dnsServers.ifBlank { "Không xác định" },
                onCopy = { copyToClipboard("DNS", info.dnsServers) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Public Internet IP Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTERNET CÔNG CỘNG (PUBLIC IP)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )

                if (info.publicInfoState == PublicInfoState.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryAccent
                    )
                }
            }

            val publicIpDisplay = when (info.publicInfoState) {
                PublicInfoState.LOADING -> "Đang kiểm tra..."
                PublicInfoState.ERROR -> "Không thể xác định (Lỗi kết nối)"
                PublicInfoState.SUCCESS -> info.publicIp.ifBlank { "Không xác định" }
            }

            NetworkFieldRow(
                label = "IP Internet",
                value = publicIpDisplay,
                isHighlight = info.publicInfoState == PublicInfoState.SUCCESS,
                onCopy = { copyToClipboard("IP Internet", info.publicIp) }
            )

            HairlineDivider()

            val ispDisplay = when (info.publicInfoState) {
                PublicInfoState.LOADING -> "Đang kiểm tra..."
                PublicInfoState.ERROR -> "Không khả dụng"
                PublicInfoState.SUCCESS -> info.isp.ifBlank { "Không xác định" }
            }

            NetworkFieldRow(
                label = "Nhà cung cấp (ISP)",
                value = ispDisplay,
                onCopy = { copyToClipboard("ISP", info.isp) }
            )

            HairlineDivider()

            val countryDisplay = when (info.publicInfoState) {
                PublicInfoState.LOADING -> "Đang kiểm tra..."
                PublicInfoState.ERROR -> "Không khả dụng"
                PublicInfoState.SUCCESS -> info.country.ifBlank { "Không xác định" }
            }

            NetworkFieldRow(
                label = "Quốc gia",
                value = countryDisplay,
                onCopy = { copyToClipboard("Quốc gia", info.country) }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun NetworkFieldRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    extraActionIcon: ImageVector? = null,
    onExtraAction: (() -> Unit)? = null,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onCopy)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextSecondaryDark
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHighlight) LatencyMedium else TextPrimaryDark
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (extraActionIcon != null && onExtraAction != null) {
                IconButton(
                    onClick = onExtraAction,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = extraActionIcon,
                        contentDescription = "Thao tác",
                        tint = if (isHighlight) LatencyMedium else PrimaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Sao chép",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(HairlineDividerDark)
    )
}
