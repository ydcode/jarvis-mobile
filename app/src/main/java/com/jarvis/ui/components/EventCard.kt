package com.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.data.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日程事件卡片
 * @param event 日程事件
 * @param hourHeight 每小时的高度
 * @param onClick 点击回调
 */
@Composable
fun EventCard(
    event: CalendarEvent,
    hourHeight: Dp,
    onClick: () -> Unit
) {
    val offsetMinutes = event.getOffsetMinutes()
    val durationMinutes = event.getDurationMinutes()

    // 计算卡片的偏移和高度
    val offsetDp = (offsetMinutes / 60f) * hourHeight.value
    val cardHeight = (durationMinutes / 60f) * hourHeight.value

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetDp.dp)
            .height(cardHeight.dp.coerceAtLeast(30.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = event.color.copy(alpha = if (event.isCompleted) 0.3f else 0.85f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // 时间范围
                Text(
                    text = formatTimeRange(event.startTime, event.endTime),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 标题
                Text(
                    text = event.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = if (durationMinutes < 45) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                )

                // 描述（仅当事件较长时显示）
                if (durationMinutes >= 60 && event.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.description,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (event.isCompleted) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间范围
 */
private fun formatTimeRange(startTime: Long, endTime: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val start = sdf.format(Date(startTime))
    val end = sdf.format(Date(endTime))
    return "$start - $end"
}

/**
 * 所有事件的容器视图
 */
@Composable
fun EventsContainer(
    events: List<CalendarEvent>,
    hourHeight: Dp,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height((hourHeight.value * 24).dp)
    ) {
        events.forEach { event ->
            EventCard(
                event = event,
                hourHeight = hourHeight,
                onClick = { onEventClick(event) }
            )
        }
    }
}
