package com.vnpt.pingtool.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnpt.pingtool.data.model.PingPacket
import com.vnpt.pingtool.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RealtimePingChart(
    packets: List<PingPacket>,
    modifier: Modifier = Modifier,
    maxDisplayPoints: Int = 30
) {
    // Take the latest packets
    val displayList = packets.takeLast(maxDisplayPoints)

    // Compute dynamic max scale (at least 200ms)
    val maxPacketMs = displayList.filter { it.isSuccess }.maxOfOrNull { it.timeMs } ?: 100f
    val chartMaxY = when {
        maxPacketMs <= 100f -> 150f
        maxPacketMs <= 200f -> 250f
        maxPacketMs <= 500f -> 600f
        else -> ((maxPacketMs / 100).toInt() + 1) * 100f
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "THỐNG KÊ BIỂU ĐỒ PING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryDark,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "Thang đo: ${chartMaxY.toInt()}ms",
                fontSize = 11.sp,
                color = PrimaryAccent
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val width = size.width
            val height = size.height

            // Draw horizontal grid lines (4 levels: 25%, 50%, 75%, 100%)
            val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1f)
            gridLevels.forEach { level ->
                val y = height * (1f - level)
                drawLine(
                    color = HairlineDividerDark,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (displayList.isEmpty()) {
                // Empty state baseline
                drawLine(
                    color = PrimaryAccent.copy(alpha = 0.2f),
                    start = Offset(0f, height - 2.dp.toPx()),
                    end = Offset(width, height - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                return@Canvas
            }

            val stepX = if (displayList.size > 1) width / (displayList.size - 1) else width
            val strokePath = Path()
            val fillPath = Path()

            val points = mutableListOf<Offset>()

            displayList.forEachIndexed { index, packet ->
                val x = index * stepX
                val normalizedY = if (packet.isSuccess) {
                    (packet.timeMs / chartMaxY).coerceIn(0f, 1f)
                } else {
                    1f // packet loss drawn at bottom or ceiling
                }
                // Coordinate in Canvas: y=0 is top, y=height is bottom
                val y = height * (1f - normalizedY)
                val pt = Offset(x, y)
                points.add(pt)

                if (index == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    strokePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            // Close fill path
            fillPath.lineTo(points.last().x, height)
            fillPath.close()

            // Draw gradient fill below line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryAccent.copy(alpha = 0.35f),
                        PrimaryAccent.copy(alpha = 0.05f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line stroke
            drawPath(
                path = strokePath,
                color = PrimaryAccent,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Draw glowing dots for each packet
            points.forEachIndexed { index, pt ->
                val packet = displayList[index]
                val dotColor = if (!packet.isSuccess) {
                    PacketLossColor
                } else if (packet.timeMs < 60f) {
                    LatencyGood
                } else if (packet.timeMs < 150f) {
                    LatencyMedium
                } else {
                    LatencyPoor
                }

                // Outer halo
                drawCircle(
                    color = dotColor.copy(alpha = 0.4f),
                    radius = 4.5.dp.toPx(),
                    center = pt
                )
                // Center core
                drawCircle(
                    color = dotColor,
                    radius = 2.5.dp.toPx(),
                    center = pt
                )
            }
        }

        // Time axis labels
        if (displayList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val firstTime = timeFormat.format(Date(displayList.first().timestamp))
                val lastTime = timeFormat.format(Date(displayList.last().timestamp))
                Text(text = firstTime, fontSize = 10.sp, color = TextSecondaryDark)
                if (displayList.size > 2) {
                    val midTime = timeFormat.format(Date(displayList[displayList.size / 2].timestamp))
                    Text(text = midTime, fontSize = 10.sp, color = TextSecondaryDark)
                }
                Text(text = lastTime, fontSize = 10.sp, color = TextSecondaryDark)
            }
        }
    }
}
