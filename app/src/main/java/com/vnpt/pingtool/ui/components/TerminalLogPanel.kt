package com.vnpt.pingtool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnpt.pingtool.data.model.PingPacket
import com.vnpt.pingtool.ui.theme.*

@Composable
fun TerminalLogPanel(
    packets: List<PingPacket>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Theo dõi gói cuối (ring-buffer giữ kích thước cố định nên không thể dựa vào packets.size)
    LaunchedEffect(packets.lastOrNull()) {
        if (packets.isNotEmpty()) {
            listState.scrollToItem(packets.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F0F12))
            .padding(10.dp)
    ) {
        if (packets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nhấn 'Bắt đầu' để gửi gói ICMP...",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(packets) { _, packet ->
                    val textColor = when {
                        packet.isInfo -> TextSecondaryDark
                        !packet.isSuccess -> PacketLossColor
                        packet.timeMs > LATENCY_MEDIUM_THRESHOLD_MS -> LatencyPoor
                        else -> Color(0xFF00E676) // Terminal green
                    }

                    Text(
                        text = packet.rawLog.ifBlank {
                            "${packet.bytes} bytes from ${packet.ip}: icmp_seq=${packet.sequence} ttl=${packet.ttl} time=${packet.timeMs} ms"
                        },
                        color = textColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
