package com.jarvis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.data.model.TimeScale
import java.text.SimpleDateFormat
import java.util.*

/**
 * 24小时时间线视图
 * @param modifier Modifier
 * @param hourHeight 每小时的高度
 */
@Composable
fun TimelineView(
    modifier: Modifier = Modifier,
    hourHeight: Float = 60f
) {
    // 根据 hourHeight 动态确定刻度间隔
    val labelInterval = when {
        hourHeight < 90f -> 60   // 1小时间隔
        hourHeight < 200f -> 30  // 30分钟间隔
        hourHeight < 450f -> 15  // 15分钟间隔
        hourHeight < 1200f -> 10 // 10分钟间隔
        hourHeight < 2400f -> 5  // 5分钟间隔
        else -> 1                // 1分钟间隔
    }

    val majorInterval = when {
        hourHeight < 90f -> 60
        hourHeight < 200f -> 60
        hourHeight < 450f -> 30
        hourHeight < 1200f -> 15
        hourHeight < 2400f -> 10
        else -> 5
    }

    val minorInterval = when {
        hourHeight < 90f -> 30
        hourHeight < 200f -> 15
        hourHeight < 450f -> 15
        hourHeight < 1200f -> 5
        hourHeight < 2400f -> 1
        else -> 1
    }

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // 左侧时间标签
        Column(
            modifier = Modifier
                .width(60.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val totalMinutes = 24 * 60

            for (minutes in 0 until totalMinutes step labelInterval) {
                val hour = minutes / 60
                val minute = minutes % 60
                TimeLabel(
                    hour = hour,
                    minute = minute,
                    height = (labelInterval / 60f * hourHeight).dp,
                    showMinute = hourHeight >= 90f
                )
            }
        }

        // 右侧时间线
        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 绘制时间线
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((hourHeight * 24).dp)
            ) {
                val width = size.width
                val hourHeightPx = hourHeight * density
                val totalMinutes = 24 * 60

                // 绘制主刻度线（粗线）
                val majorColor = Color.LightGray.copy(alpha = 0.4f)
                for (minutes in 0..totalMinutes step majorInterval) {
                    val y = (minutes / 60f) * hourHeightPx
                    drawLine(
                        color = majorColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 2f
                    )
                }

                // 绘制次刻度线（细线）
                val minorColor = Color.LightGray.copy(alpha = 0.2f)
                for (minutes in 0..totalMinutes step minorInterval) {
                    if (minutes % majorInterval != 0) {
                        val y = (minutes / 60f) * hourHeightPx
                        drawLine(
                            color = minorColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // 当前时间指示线
            CurrentTimeIndicator(hourHeight = hourHeight.dp)
        }
    }
}

/**
 * 时间标签
 */
@Composable
fun TimeLabel(
    hour: Int,
    minute: Int = 0,
    height: Dp,
    showMinute: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = if (showMinute) {
                String.format("%02d:%02d", hour, minute)
            } else {
                String.format("%02d:00", hour)
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = if (minute == 0 && hour % 3 == 0) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier
                .padding(end = 8.dp)
                .offset(y = if (hour == 0 && minute == 0) 0.dp else (-6).dp)
        )
    }
}

/**
 * 当前时间指示线（红色横线）
 */
@Composable
fun CurrentTimeIndicator(hourHeight: Dp) {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val currentTimeMinutes = currentHour * 60 + currentMinute

    val offsetDp = (currentTimeMinutes / 60f) * hourHeight.value

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetDp.dp)
            .padding(start = 0.dp)
    ) {
        // 红色圆点
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = Color(0xFFFF3B30),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        // 红色横线
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(Color(0xFFFF3B30))
        )
    }
}
