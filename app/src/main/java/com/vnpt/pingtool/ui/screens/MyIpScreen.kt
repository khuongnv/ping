package com.vnpt.pingtool.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnpt.pingtool.data.model.NetworkCardInfo
import com.vnpt.pingtool.ui.components.LiquidGlassCard
import com.vnpt.pingtool.ui.theme.*

@Composable
fun MyIpScreen(
    info: NetworkCardInfo,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun copyToClipboard(label: String, text: String) {
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
            Text(
                text = "IP của tôi",
                style = MaterialTheme.typography.headlineLarge
            )

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Làm mới",
                    tint = PrimaryAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Local Wi-Fi Info Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "MẠNG NỘI BỘ (LOCAL NETWORK)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            NetworkFieldRow(
                label = "IP Wi-Fi (Cục bộ)",
                value = info.wifiIp,
                onCopy = { copyToClipboard("IP Wi-Fi", info.wifiIp) }
            )

            HairlineDivider()

            NetworkFieldRow(
                label = "Tên Wi-Fi (SSID)",
                value = info.wifiSsid,
                onCopy = { copyToClipboard("SSID", info.wifiSsid) }
            )

            HairlineDivider()

            NetworkFieldRow(
                label = "Cổng kết nối (Gateway)",
                value = info.gateway,
                onCopy = { copyToClipboard("Gateway", info.gateway) }
            )

            HairlineDivider()

            NetworkFieldRow(
                label = "Mặt nạ mạng (Subnet Mask)",
                value = info.subnetMask,
                onCopy = { copyToClipboard("Subnet", info.subnetMask) }
            )

            HairlineDivider()

            NetworkFieldRow(
                label = "Máy chủ DNS",
                value = info.dnsServers,
                onCopy = { copyToClipboard("DNS", info.dnsServers) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Public Internet IP Card
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "INTERNET CÔNG CỘNG (PUBLIC IP)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            NetworkFieldRow(
                label = "IP Internet",
                value = info.publicIp,
                isHighlight = true,
                onCopy = { copyToClipboard("IP Internet", info.publicIp) }
            )

            HairlineDivider()

            NetworkFieldRow(
                label = "Nhà cung cấp (ISP)",
                value = info.isp,
                onCopy = { copyToClipboard("ISP", info.isp) }
            )

            HairlineDivider()

            NetworkFieldRow(
                label = "Quốc gia",
                value = info.country,
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
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHighlight) PrimaryAccent else TextPrimaryDark
            )
        }
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Sao chép",
            tint = TextSecondaryDark,
            modifier = Modifier.size(16.dp)
        )
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
