package com.vnpt.pingtool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnpt.pingtool.data.model.PingSessionStats
import com.vnpt.pingtool.ui.components.LiquidGlassCard
import com.vnpt.pingtool.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    sessions: List<PingSessionStats>,
    onSelectHost: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lịch sử",
                style = MaterialTheme.typography.headlineLarge
            )

            if (sessions.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa lịch sử",
                        tint = TextSecondaryDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có phiên ping nào được lưu.",
                    color = TextSecondaryDark,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(sessions) { session ->
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectHost(session.targetHost) },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.targetHost,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )

                                val timeStr = if (session.startedAt > 0) dateFormat.format(Date(session.startedAt)) + " • " else ""
                                val ipStr = if (session.resolvedIp.isNotBlank() && session.resolvedIp != session.targetHost) {
                                    "${session.resolvedIp} • "
                                } else ""

                                Text(
                                    text = "$timeStr$ipStr${session.sentCount} gói",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val rttText = if (session.receivedCount > 0) "${session.avgMs.toInt()} ms" else "--"
                                    val rttColor = latencyColor(session.avgMs, session.receivedCount > 0)
                                    Text(
                                        text = rttText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = rttColor
                                    )
                                    Text(
                                        text = "Mất ${session.lossPercentage.toInt()}%",
                                        fontSize = 11.sp,
                                        color = if (session.lossPercentage > 0) PacketLossColor else LatencyGood
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Ping lại",
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
