package com.vnpt.pingtool.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    currentRoute: String,
    onNavigate: (String) -> Unit,
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
        drawerContainerColor = Color(0xFF121215),
        drawerContentColor = TextPrimaryDark,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // Header with Brand and Close button
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
                            text = "VNPT Ping Tool",
                            fontSize = 18.sp,
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
                        contentDescription = "Đóng menu",
                        tint = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = HairlineDividerDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // MENU ĐIỀU HƯỚNG CHÍNH (SIDEBAR MENU)
            // ==========================================
            Text(
                text = "DANH MỤC TIỆN ÍCH",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )

            // Mục 1: IP của tôi
            SidebarMenuItem(
                title = "IP của tôi",
                subtitle = "IP nội bộ, Wi-Fi, Public IP & Nhà mạng",
                icon = Icons.Default.Language,
                isSelected = currentRoute == "my_ip",
                onClick = {
                    onNavigate("my_ip")
                    onCloseSidebar()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mục 2: Cài đặt tham số
            SidebarMenuItem(
                title = "Cài đặt",
                subtitle = "Tham số gói tin, tần suất ping, timeout",
                icon = Icons.Default.Tune,
                isSelected = currentRoute == "settings",
                onClick = {
                    onNavigate("settings")
                    onCloseSidebar()
                }
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = HairlineDividerDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // CẬP NHẬT PHẦN MỀM (GITHUB RELEASES)
            Text(
                text = "CẬP NHẬT PHẦN MỀM (OTA)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
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

            // THÔNG TIN DỰ ÁN
            Text(
                text = "THÔNG TIN DỰ ÁN",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
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
                    text = "Chuẩn thiết kế Apple Music HIG x AMOLED Black. 100% không quảng cáo, tối ưu mạng độ trễ thấp.",
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
private fun SidebarMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) PrimaryAccent.copy(alpha = 0.16f) else GlassSurfaceDark
    val borderColor = if (isSelected) PrimaryAccent.copy(alpha = 0.5f) else LuminousRimBorderDark
    val iconColor = if (isSelected) PrimaryAccent else TextSecondaryDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(width = 0.75.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryAccent.copy(alpha = 0.25f) else Color(0x1AFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) PrimaryAccent else TextPrimaryDark
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = if (isSelected) PrimaryAccent else TextSecondaryDark.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
