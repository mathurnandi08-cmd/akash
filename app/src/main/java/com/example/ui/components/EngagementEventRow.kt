package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EngagementEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EngagementEventRow(
    event: EngagementEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = rememberSimpleTimeFormatter()
    val timeStr = formatter.format(Date(event.timestamp))

    val (icon, color) = when (event.eventType) {
        "session_start" -> Pair(Icons.Default.PlayArrow, ChartColors.Purple)
        "purchase" -> Pair(Icons.Default.Check, ChartColors.Green)
        "error" -> Pair(Icons.Default.Warning, ChartColors.Pink)
        else -> Pair(Icons.Default.Info, ChartColors.Cyan)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_item_${event.id}")
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0E14).copy(alpha = 0.6f))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Indicator
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = event.eventType,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Main info: Event Type & Target Element Name
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = event.eventType.uppercase(Locale.getDefault()),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeStr,
                    color = ChartColors.TextColor,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "${event.elementName} (${event.userId})",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right metadata block: Platform Pill & Country Code
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (event.platform) {
                            "Android" -> Color(0xFF3DDC84).copy(alpha = 0.15f)
                            "iOS" -> Color(0xFFFFFFFF).copy(alpha = 0.15f)
                            else -> Color(0xFF007ACC).copy(alpha = 0.15f)
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = event.platform,
                    color = when (event.platform) {
                        "Android" -> Color(0xFF3DDC84)
                        "iOS" -> Color.White
                        else -> Color(0xFF64B5F6)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.country,
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
                if (event.value > 0.0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (event.eventType == "purchase") {
                            "$${String.format(Locale.US, "%.1f", event.value)}"
                        } else {
                            "${event.value.toInt()}"
                        },
                        color = if (event.eventType == "purchase") ChartColors.Green else ChartColors.TextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun rememberSimpleTimeFormatter(): SimpleDateFormat {
    return androidx.compose.runtime.remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }
}
