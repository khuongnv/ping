package com.vnpt.pingtool.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vnpt.pingtool.data.model.PasswordAuditMatchRecord
import com.vnpt.pingtool.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PasswordAuditHistoryDialog(
    records: List<PasswordAuditMatchRecord>,
    onDeleteRecord: (id: String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

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
                                    .background(if (records.isNotEmpty()) PacketLossColor else TextSecondaryDark.copy(alpha = 0.3f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${records.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
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
                                text = "Khi chạy SCAN với mật khẩu mục tiêu và phát hiện trùng khớp trong từ điển cấp 1->99, nhật ký đối chiếu sẽ tự động được lưu lại tại đây.",
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
                                record.level <= 15 -> Color(0xFFFF3B30)
                                record.level <= 35 -> Color(0xFFFF9500)
                                record.level <= 60 -> Color(0xFFFFD600)
                                record.level <= 85 -> Color(0xFF00E5FF)
                                else -> Color(0xFF00E676)
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
}
