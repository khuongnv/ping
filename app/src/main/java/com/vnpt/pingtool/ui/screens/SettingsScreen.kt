package com.vnpt.pingtool.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.vnpt.pingtool.ui.components.GlassCapsuleButton
import com.vnpt.pingtool.ui.components.LiquidGlassCard
import com.vnpt.pingtool.ui.theme.*

@Composable
fun SettingsScreen(
    config: PingConfig,
    onConfigChange: (PingConfig) -> Unit,
    updateInfo: UpdateReleaseInfo?,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Cài đặt",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Cập nhật phần mềm qua GitHub
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "CẬP NHẬT PHẦN MỀM (GITHUB RELEASES)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Phiên bản hiện tại",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "$currentVersion (Bản chính thức)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PrimaryAccent,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (updateInfo != null && updateInfo.hasNewVersion) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryAccent.copy(alpha = 0.15f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "🚀 Có bản cập nhật mới: ${updateInfo.tagName}",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo.body,
                            color = TextPrimaryDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCapsuleButton(
                            text = "Tải & Cài đặt APK",
                            icon = Icons.Default.SystemUpdate,
                            isActive = true,
                            onClick = onDownloadUpdate
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (updateInfo != null) "Bạn đang dùng phiên bản mới nhất" else "Kiểm tra phiên bản mới từ GitHub",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                    GlassCapsuleButton(
                        text = "Kiểm tra",
                        isActive = false,
                        onClick = onCheckUpdate
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cấu hình tham số Ping
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "THAM SỐ GÓI TIN PING",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Packet size
            ConfigSliderRow(
                label = "Kích thước gói tin",
                displayValue = "${config.packetSize} bytes",
                value = config.packetSize.toFloat(),
                range = 32f..512f,
                steps = 15,
                onValueChange = { onConfigChange(config.copy(packetSize = it.toInt())) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interval
            ConfigSliderRow(
                label = "Tần suất Ping (Interval)",
                displayValue = "${config.intervalMs} ms",
                value = config.intervalMs.toFloat(),
                range = 200f..3000f,
                steps = 28,
                onValueChange = { onConfigChange(config.copy(intervalMs = it.toLong())) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Timeout
            ConfigSliderRow(
                label = "Thời gian chờ (Timeout)",
                displayValue = "${config.timeoutSec} giây",
                value = config.timeoutSec.toFloat(),
                range = 1f..10f,
                steps = 9,
                onValueChange = { onConfigChange(config.copy(timeoutSec = it.toInt())) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Thông tin nhà phát triển & Git
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "THÔNG TIN DỰ ÁN",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Text(
                text = "VNPT Ping Tool (Zero Ads Edition)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Thiết kế chuẩn Liquid Glass & Apple HIG theo tài liệu kỹ thuật UI/UX Design Standards.",
                fontSize = 13.sp,
                color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Mã nguồn mở tại: https://github.com/khuongnv/ping",
                fontSize = 12.sp,
                color = PrimaryAccent,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/khuongnv/ping"))
                    context.startActivity(intent)
                }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ConfigSliderRow(
    label: String,
    displayValue: String,
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
            Text(text = label, fontSize = 13.sp, color = TextSecondaryDark)
            Text(text = displayValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryAccent)
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
