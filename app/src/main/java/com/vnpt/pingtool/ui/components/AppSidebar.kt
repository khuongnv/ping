package com.vnpt.pingtool.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnpt.pingtool.BuildConfig
import com.vnpt.pingtool.data.model.PingConfig
import com.vnpt.pingtool.data.model.UpdateReleaseInfo
import com.vnpt.pingtool.ui.theme.*

@Composable
fun AppSidebar(
    config: PingConfig,
    onConfigChange: (PingConfig) -> Unit,
    updateInfo: UpdateReleaseInfo?,
    isCheckingUpdate: Boolean,
    updateCheckFailed: Boolean,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onCloseSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${pInfo.versionName ?: BuildConfig.VERSION_NAME}"
        } catch (_: Exception) {
            "v${BuildConfig.VERSION_NAME}"
        }
    }

    ModalDrawerSheet(
        modifier = modifier
            .width(330.dp)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF141417),
        drawerContentColor = TextPrimaryDark,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // Header with App Brand and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = PrimaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Ping",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Zero Ads Edition • $currentVersion",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                IconButton(onClick = onCloseSidebar) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng sidebar",
                        tint = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = HairlineDividerDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Cập nhật phần mềm qua GitHub Releases
            Text(
                text = "CẬP NHẬT PHẦN MỀM (GITHUB)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

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
                            text = "Bản hiện tại: $currentVersion",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        val statusText = when {
                            isCheckingUpdate -> "Đang kiểm tra GitHub..."
                            updateCheckFailed -> "Lỗi kết nối kiểm tra"
                            updateInfo != null && !updateInfo.hasNewVersion -> "Đang là bản mới nhất"
                            else -> "Kiểm tra phiên bản mới"
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = if (updateCheckFailed) PacketLossColor else TextSecondaryDark
                        )
                    }

                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PrimaryAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        GlassCapsuleButton(
                            text = "Kiểm tra",
                            isActive = false,
                            modifier = Modifier.height(34.dp),
                            onClick = onCheckUpdate
                        )
                    }
                }

                if (updateInfo != null && updateInfo.hasNewVersion) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryAccent.copy(alpha = 0.15f))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "🚀 Có bản mới: ${updateInfo.tagName}",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent,
                                fontSize = 13.sp
                            )
                            if (updateInfo.body.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = updateInfo.body,
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            GlassCapsuleButton(
                                text = "Tải & Cài đặt APK",
                                icon = Icons.Default.SystemUpdate,
                                isActive = true,
                                onClick = onDownloadUpdate
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cài đặt tham số Ping
            Text(
                text = "THAM SỐ GÓI TIN PING",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                // Kích thước gói tin
                SidebarSlider(
                    label = "Kích thước gói",
                    display = "${config.packetSize} bytes",
                    value = config.packetSize.toFloat(),
                    range = 32f..512f,
                    steps = 15,
                    onValueChange = { onConfigChange(config.copy(packetSize = it.toInt())) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tần suất ping (Interval)
                SidebarSlider(
                    label = "Tần suất (Interval)",
                    display = "${config.intervalMs} ms",
                    value = config.intervalMs.toFloat(),
                    range = 200f..3000f,
                    steps = 28,
                    onValueChange = { onConfigChange(config.copy(intervalMs = it.toLong())) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Thời gian chờ (Timeout)
                SidebarSlider(
                    label = "Thời gian chờ (Timeout)",
                    display = "${config.timeoutSec} giây",
                    value = config.timeoutSec.toFloat(),
                    range = 1f..10f,
                    steps = 9,
                    onValueChange = { onConfigChange(config.copy(timeoutSec = it.toInt())) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Số lượng gói
                val countDisplay = if (config.packetCount == 0) "Liên tục" else "${config.packetCount} gói"
                SidebarSlider(
                    label = "Số lượng gói",
                    display = countDisplay,
                    value = when (config.packetCount) {
                        0 -> 0f
                        5 -> 1f
                        10 -> 2f
                        20 -> 3f
                        50 -> 4f
                        100 -> 5f
                        else -> 0f
                    },
                    range = 0f..5f,
                    steps = 4,
                    onValueChange = { step ->
                        val count = when (step.toInt()) {
                            1 -> 5
                            2 -> 10
                            3 -> 20
                            4 -> 50
                            5 -> 100
                            else -> 0
                        }
                        onConfigChange(config.copy(packetCount = count))
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Thông tin dự án
            Text(
                text = "THÔNG TIN DỰ ÁN",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(
                    text = "VNPT Ping Tool (Liquid Glass Edition)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chuẩn thiết kế Apple Music HIG x AMOLED Black. 100% không quảng cáo, công cụ kỹ thuật mạng tốc độ cao.",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mã nguồn: github.com/khuongnv/ping",
                    fontSize = 11.sp,
                    color = PrimaryAccent,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/khuongnv/ping"))
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SidebarSlider(
    label: String,
    display: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, color = TextSecondaryDark)
            Text(text = display, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryAccent)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = PrimaryAccent,
                activeTrackColor = PrimaryAccent,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
