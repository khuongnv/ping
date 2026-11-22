package com.vnpt.pingtool.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
    maxDisplayPoints: Int = 36
) {
    // Chỉ lấy các gói đo ICMP thực tế
    val displayList = remember(packets, maxDisplayPoints) {
        packets.filter { !it.isInfo }.takeLast(maxDisplayPoints)
    }

    // Thang đo RTT động mượt mà
    val maxPacketMs = displayList.filter { it.isSuccess }.maxOfOrNull { it.timeMs } ?: 0f
    val chartMaxY = remember(maxPacketMs) {
        when {
            maxPacketMs <= 60f -> 100f
            maxPacketMs <= 120f -> 160f
            maxPacketMs <= 250f -> 300f
            maxPacketMs <= 500f -> 600f
            else -> ((maxPacketMs / 100).toInt() + 1) * 100f
        }
    }

    // Hiệu ứng pulse sóng thời gian thực cho điểm mới nhất
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Trạng thái tương tác chạm / kéo (Touch Scrub)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedPacket = selectedIndex?.let { displayList.getOrNull(it) }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(modifier = modifier) {
        // Header biểu đồ & Tooltip thông tin điểm chạm
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BIỂU ĐỒ SÓNG RTT THỜI GIAN THỰC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Thang đo trần: ${chartMaxY.toInt()} ms",
                    fontSize = 10.sp,
                    color = PrimaryAccent
                )
            }

            if (selectedPacket != null) {
                // Interactive inspection badge
                val badgeColor = latencyColor(selectedPacket.timeMs, selectedPacket.isSuccess)
                val statusText = if (selectedPacket.isSuccess) {
                    "${String.format(Locale.US, "%.1f", selectedPacket.timeMs)} ms"
                } else {
                    "MẤT GÓI"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#${selectedPacket.sequence}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                        Text(
                            text = timeFormat.format(Date(selectedPacket.timestamp)),
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            } else {
                Text(
                    text = if (displayList.isNotEmpty()) "Chạm/kéo để soi chi tiết" else "",
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // Khung Canvas vẽ biểu đồ sóng mượt mà
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0C0C0E))
                .border(0.5.dp, LuminousRimBorderDark, RoundedCornerShape(12.dp))
                .pointerInput(displayList.size) {
                    if (displayList.isEmpty()) return@pointerInput

                    detectTapGestures(
                        onPress = { offset ->
                            val stepX = if (displayList.size > 1) size.width / (displayList.size - 1) else size.width
                            val idx = (offset.x / stepX).toInt().coerceIn(0, displayList.size - 1)
                            selectedIndex = idx
                        },
                        onTap = {
                            // Giữ hiển thị thông tin
                        }
                    )
                }
                .pointerInput(displayList.size) {
                    if (displayList.isEmpty()) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            val stepX = if (displayList.size > 1) size.width / (displayList.size - 1) else size.width
                            val idx = (offset.x / stepX).toInt().coerceIn(0, displayList.size - 1)
                            selectedIndex = idx
                        },
                        onDrag = { change, _ ->
                            val stepX = if (displayList.size > 1) size.width / (displayList.size - 1) else size.width
                            val idx = (change.position.x / stepX).toInt().coerceIn(0, displayList.size - 1)
                            selectedIndex = idx
                        },
                        onDragEnd = {
                            // Giữ nguyên lựa chọn
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                val width = size.width
                val height = size.height

                // 1. Vẽ các đường lưới tham chiếu RTT mờ ảo
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
                    // Đường chuẩn đáy khi chưa có dữ liệu
                    drawLine(
                        color = PrimaryAccent.copy(alpha = 0.25f),
                        start = Offset(0f, height - 2.dp.toPx()),
                        end = Offset(width, height - 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    return@Canvas
                }

                val stepX = if (displayList.size > 1) width / (displayList.size - 1) else width
                val points = mutableListOf<Offset>()

                displayList.forEachIndexed { index, packet ->
                    val x = index * stepX
                    val normalizedY = if (packet.isSuccess) {
                        (packet.timeMs / chartMaxY).coerceIn(0.04f, 0.96f)
                    } else {
                        0.04f // Mất gói đặt ở chân cột cảnh báo
                    }
                    val y = height * (1f - normalizedY)
                    points.add(Offset(x, y))
                }

                // 2. Vẽ cột cảnh báo mất gói tin (Hazard Column)
                displayList.forEachIndexed { index, packet ->
                    if (!packet.isSuccess) {
                        val x = points[index].x
                        val colWidth = 6.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    PacketLossColor.copy(alpha = 0.35f),
                                    PacketLossColor.copy(alpha = 0.05f)
                                )
                            ),
                            topLeft = Offset(x - colWidth / 2f, 0f),
                            size = Size(colWidth, height)
                        )
                    }
                }

                // 3. Xây dựng đường cong Bezier mượt mà (Smooth Cubic Spline)
                val strokePath = Path()
                val fillPath = Path()

                strokePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, height)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val controlX = (prev.x + curr.x) / 2f
                    strokePath.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                    fillPath.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                }

                fillPath.lineTo(points.last().x, height)
                fillPath.close()

                // 4. Đổ bóng gradient đa tầng phía dưới đường sóng
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PrimaryAccent.copy(alpha = 0.38f),
                            PrimaryAccent.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // 5. Đường hào quang mờ (Ambient Glow Stroke)
                drawPath(
                    path = strokePath,
                    color = PrimaryAccent.copy(alpha = 0.28f),
                    style = Stroke(
                        width = 5.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 6. Đường sóng chính (Main Wave Stroke)
                drawPath(
                    path = strokePath,
                    color = PrimaryAccent,
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 7. Vẽ các điểm nút dữ liệu (Data Nodes)
                points.forEachIndexed { index, pt ->
                    val packet = displayList[index]
                    val dotColor = latencyColor(packet.timeMs, packet.isSuccess)

                    if (!packet.isSuccess) {
                        // Nút cảnh báo mất gói
                        drawCircle(
                            color = PacketLossColor,
                            radius = 3.dp.toPx(),
                            center = Offset(pt.x, height - 6.dp.toPx())
                        )
                    } else {
                        // Hào quang nhẹ
                        drawCircle(
                            color = dotColor.copy(alpha = 0.35f),
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                        // Lõi sáng
                        drawCircle(
                            color = dotColor,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // 8. Hiệu ứng xung nhịp thời gian thực (Pulse Halo) tại điểm mới nhất
                if (points.isNotEmpty()) {
                    val lastPt = points.last()
                    val lastPacket = displayList.last()
                    val pulseColor = latencyColor(lastPacket.timeMs, lastPacket.isSuccess)

                    drawCircle(
                        color = pulseColor.copy(alpha = pulseAlpha),
                        radius = (pulseRadius * 1.5f).dp.toPx(),
                        center = lastPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = lastPt
                    )
                }

                // 9. Đường chỉ báo vị trí chạm (Touch Scrub Indicator)
                selectedIndex?.let { idx ->
                    if (idx in points.indices) {
                        val touchPt = points[idx]
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(touchPt.x, 0f),
                            end = Offset(touchPt.x, height),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                        drawCircle(
                            color = PrimaryAccent,
                            radius = 5.5.dp.toPx(),
                            center = touchPt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = touchPt
                        )
                    }
                }
            }
        }

        // Trục thời gian chân biểu đồ
        if (displayList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
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
