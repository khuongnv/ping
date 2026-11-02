package com.vnpt.pingtool.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vnpt.pingtool.data.network.WifiQrManager
import com.vnpt.pingtool.ui.theme.*

@Composable
fun WifiQrShareDialog(
    ssid: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Lọc bỏ chuỗi chỉ dẫn nếu chưa có tên SSID thực
    val initialCleanSsid = remember(ssid) {
        if (ssid.contains("(") || ssid.startsWith("<") || ssid.isBlank()) "" else ssid
    }

    var currentSsid by remember(ssid) { mutableStateOf(initialCleanSsid) }
    var isEditingSsid by remember { mutableStateOf(initialCleanSsid.isBlank()) }
    var ssidInput by remember { mutableStateOf(initialCleanSsid) }

    var savedPassword by remember(currentSsid) {
        mutableStateOf(if (currentSsid.isNotBlank()) WifiQrManager.getSavedPassword(context, currentSsid) ?: "" else "")
    }
    var isEditingPassword by remember(savedPassword) {
        mutableStateOf(savedPassword.isBlank())
    }
    var passwordInput by remember(savedPassword) {
        mutableStateOf(savedPassword)
    }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val qrContent = remember(currentSsid, savedPassword, isEditingSsid, isEditingPassword) {
        if (currentSsid.isNotBlank() && !isEditingSsid && !isEditingPassword) {
            WifiQrManager.createWifiQrString(ssid = currentSsid, password = savedPassword)
        } else {
            ""
        }
    }

    val qrBitmap = remember(qrContent) {
        if (qrContent.isNotBlank()) {
            WifiQrManager.generateQrBitmap(qrContent, size = 640)
        } else {
            null
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Đã sao chép $label", Toast.LENGTH_SHORT).show()
    }

    fun shareViaSystem() {
        val shareText = buildString {
            append("📡 Thông tin kết nối Wi-Fi\n")
            append("• Tên mạng (SSID): $currentSsid\n")
            if (savedPassword.isNotBlank()) {
                append("• Mật khẩu: $savedPassword\n")
            } else {
                append("• Mật khẩu: Mạng mở (Không mật khẩu)\n")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ Wi-Fi $currentSsid")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ thông tin Wi-Fi"))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(26.dp))
                .background(GlassSurfaceDark)
                .border(
                    width = 0.75.dp,
                    color = LuminousRimBorderDark,
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LatencyGood.copy(alpha = 0.15f))
                                .border(0.75.dp, LatencyGood.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Wi-Fi",
                                tint = LatencyGood,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Chia sẻ mạng Wi-Fi",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = if (currentSsid.isNotBlank()) currentSsid else "Chưa đặt tên SSID",
                                fontSize = 13.sp,
                                color = if (currentSsid.isNotBlank()) PrimaryAccent else TextSecondaryDark,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CardSurfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // QR Code Area
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Mã QR Wi-Fi",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardSurface.copy(alpha = 0.8f))
                            .border(0.75.dp, LuminousRimBorderDark, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = PrimaryAccent,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = if (currentSsid.isBlank()) "Vui lòng nhập tên Wi-Fi và mật khẩu\nđể tạo mã QR chia sẻ" else "Nhập mật khẩu bên dưới để tạo mã QR",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // KHU VỰC TÊN WI-FI (SSID)
                if (isEditingSsid || currentSsid.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardSurface.copy(alpha = 0.85f))
                            .border(0.75.dp, LuminousRimBorderDark, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "TÊN MẠNG WI-FI (SSID)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = ssidInput,
                            onValueChange = { ssidInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("VD: VNPT-Wi-Fi-5G", fontSize = 13.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (ssidInput.isNotBlank()) {
                                    IconButton(onClick = {
                                        currentSsid = ssidInput.trim()
                                        isEditingSsid = false
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "Xong", tint = PrimaryAccent)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = LuminousRimBorderDark
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // PASSWORD SECTION (HIỂN THỊ MẬT KHẨU Ở DƯỚI MÃ QR)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface.copy(alpha = 0.85f))
                        .border(0.75.dp, LuminousRimBorderDark, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MẬT KHẨU WI-FI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            letterSpacing = 1.sp
                        )

                        if (!isEditingPassword) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        passwordInput = savedPassword
                                        isEditingPassword = true
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Chỉnh sửa",
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Đổi mật khẩu",
                                    fontSize = 12.sp,
                                    color = PrimaryAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isEditingPassword) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (savedPassword.isBlank()) {
                                    Text(
                                        text = "Mạng mở (Không có mật khẩu)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LatencyGood
                                    )
                                } else {
                                    Text(
                                        text = if (isPasswordVisible) savedPassword else "••••••••••••",
                                        fontSize = 17.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                            }

                            if (savedPassword.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { isPasswordVisible = !isPasswordVisible },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Hiện/Ẩn mật khẩu",
                                            tint = TextSecondaryDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { copyToClipboard("Mật khẩu Wi-Fi", savedPassword) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Sao chép mật khẩu",
                                            tint = PrimaryAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Nhập mật khẩu mới
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nhập mật khẩu Wi-Fi", fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (currentSsid.isNotBlank()) {
                                        WifiQrManager.savePassword(context, currentSsid, passwordInput)
                                    }
                                    savedPassword = passwordInput
                                    isEditingPassword = false
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Hiện/Ẩn",
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = LuminousRimBorderDark,
                                focusedLabelColor = PrimaryAccent,
                                unfocusedLabelColor = TextSecondaryDark,
                                cursorColor = PrimaryAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    passwordInput = ""
                                    if (currentSsid.isNotBlank()) {
                                        WifiQrManager.savePassword(context, currentSsid, "")
                                    }
                                    savedPassword = ""
                                    isEditingPassword = false
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.75.dp, LuminousRimBorderDark)
                            ) {
                                Text(
                                    text = "Mạng mở",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            Button(
                                onClick = {
                                    if (currentSsid.isNotBlank()) {
                                        WifiQrManager.savePassword(context, currentSsid, passwordInput)
                                    }
                                    savedPassword = passwordInput
                                    isEditingPassword = false
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.weight(1.4f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryAccent
                                )
                            ) {
                                Text(
                                    text = "Lưu & Tạo mã",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Guide tip
                Text(
                    text = "💡 Quét mã này bằng Camera trên iPhone hoặc Android để kết nối ngay vào Wi-Fi mà không cần gõ mật khẩu.",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { shareViaSystem() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimaryDark
                        ),
                        border = BorderStroke(0.75.dp, LuminousRimBorderDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chia sẻ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "Đóng",
                            fontSize = 13.sp,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
