package com.vnpt.pingtool.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vnpt.pingtool.data.model.PasswordAuditMatchRecord
import com.vnpt.pingtool.data.network.WifiConnectionManager
import com.vnpt.pingtool.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PasswordAuditHistoryDialog(
    records: List<PasswordAuditMatchRecord>,
    onDeleteRecord: (id: String) -> Unit,
    onClearHistory: () -> Unit,
    onConnectWifi: (ssid: String, password: String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    var qrRecordToShow by remember { mutableStateOf<PasswordAuditMatchRecord?>(null) }

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
            border = BorderStroke(0.75.dp, LuminousRimBorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lịch sử mật khẩu đã khớp",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (records.isNotEmpty()) Color(0xFF30D158).copy(alpha = 0.2f) else TextSecondaryDark.copy(alpha = 0.2f))
                                    .border(0.5.dp, if (records.isNotEmpty()) Color(0xFF30D158).copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${records.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (records.isNotEmpty()) Color(0xFF30D158) else TextSecondaryDark
                                )
                            }
                        }
                        Text(
                            text = "Các mạng Wi-Fi đã phát hiện mật khẩu trong từ điển",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (records.isNotEmpty()) {
                            IconButton(onClick = onClearHistory) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Xóa tất cả",
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

                Spacer(modifier = Modifier.height(14.dp))

                if (records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = "Chưa có phát hiện mật khẩu khớp",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Khi chạy SCAN với Access Point mục tiêu và phát hiện trùng khớp trong từ điển cấp 1->99, nhật ký đối chiếu sẽ tự động được lưu lại tại đây kèm mã QR kết nối.",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                lineHeight = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(records, key = { it.id }) { record ->
                            val levelColor = when {
                                record.level <= 20 -> Color(0xFFFF9F0A)
                                record.level <= 40 -> Color(0xFFFFD60A)
                                record.level <= 70 -> Color(0xFF64D2FF)
                                record.level <= 90 -> Color(0xFF30D158)
                                else -> Color(0xFFBF5AF2)
                            }

                            LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        // SSID & Level Badge
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = record.ssid,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(levelColor.copy(alpha = 0.2f))
                                                    .border(0.5.dp, levelColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Cấp độ ${record.level} / 99",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = levelColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Mật khẩu phát hiện
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Mật khẩu:",
                                                fontSize = 12.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = record.matchedPassword,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E676),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Password", record.matchedPassword))
                                                    Toast.makeText(context, "Đã sao chép mật khẩu", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Sao chép mật khẩu",
                                                    tint = PrimaryAccent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Chi tiết rủi ro & Thời gian
                                        Text(
                                            text = "Đánh giá: ${record.riskRating} • Lần thử #${record.attemptsCount} (${record.speedAttemptsPerSec} thử/s)",
                                            fontSize = 11.sp,
                                            color = TextSecondaryDark
                                        )
                                        Text(
                                            text = "Ghi nhận: ${dateFormat.format(Date(record.timestamp))}",
                                            fontSize = 10.sp,
                                            color = TextSecondaryDark.copy(alpha = 0.8f)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Hàng nút hành động: Xem mã QR & Kết nối ngay
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Nút xem mã QR
                                            OutlinedButton(
                                                onClick = { qrRecordToShow = record },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(0.75.dp, PrimaryAccent.copy(alpha = 0.6f)),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCode2,
                                                    contentDescription = "Mã QR",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mã QR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            // Nút kết nối Wi-Fi ngay
                                            Button(
                                                onClick = { onConnectWifi(record.ssid, record.matchedPassword) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PrimaryAccent,
                                                    contentColor = Color.Black
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Wifi,
                                                    contentDescription = "Kết nối",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Kết nối", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteRecord(record.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Xóa bản ghi",
                                            tint = TextSecondaryDark,
                                            modifier = Modifier.size(18.dp)
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

    // Modal hiển thị mã QR Code kết nối Wi-Fi
    qrRecordToShow?.let { rec ->
        WifiQrCodeDialog(
            record = rec,
            onConnectWifi = onConnectWifi,
            onDismiss = { qrRecordToShow = null }
        )
    }
}

/**
 * Dialog hiển thị mã QR chuẩn kết nối Wi-Fi (WIFI:S:<SSID>;T:WPA;P:<PWD>;;)
 */
@Composable
fun WifiQrCodeDialog(
    record: PasswordAuditMatchRecord,
    onConnectWifi: (ssid: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(record) {
        try {
            WifiConnectionManager.generateWifiQrCodeBitmap(
                ssid = record.ssid,
                password = record.matchedPassword,
                securityType = "WPA",
                sizePx = 512
            )
        } catch (_: Exception) {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141418),
            border = BorderStroke(0.75.dp, LuminousRimBorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mã QR kết nối Wi-Fi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = record.ssid,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryAccent
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Image
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, PrimaryAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Wi-Fi QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF222228)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Không thể tạo mã QR", color = TextSecondaryDark, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Info Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1C1C22))
                        .border(0.5.dp, LuminousRimBorderDark, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mật khẩu:", fontSize = 12.sp, color = TextSecondaryDark)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = record.matchedPassword,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("WiFi Password", record.matchedPassword))
                                        Toast.makeText(context, "Đã sao chép mật khẩu", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "💡 Quét mã bằng máy ảnh của thiết bị khác để kết nối Wi-Fi ngay lập tức mà không cần nhập mật khẩu.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.75.dp, LuminousRimBorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark)
                    ) {
                        Text("Đóng", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onConnectWifi(record.ssid, record.matchedPassword)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAccent,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kết nối máy này", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
