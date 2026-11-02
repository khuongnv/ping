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

    LaunchedEffect(packets.size) {
        if (packets.isNotEmpty()) {
            listState.animateScrollToItem(packets.size - 1)
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
                    text = "Nhấn 'BẮT ĐẦU PING' để nhận phản hồi...",
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
                itemsIndexed(packets) { index, packet ->
                    val textColor = if (!packet.isSuccess) {
                        PacketLossColor
                    } else if (packet.timeMs > 150f) {
                        LatencyPoor
                    } else {
                        Color(0xFF00E676) // Terminal green
                    }

                    Text(
                        text = if (packet.rawLog.isNotBlank()) packet.rawLog else "${packet.bytes} bytes from ${packet.ip}: icmp_seq=${packet.sequence} ttl=${packet.ttl} time=${packet.timeMs}ms",
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
