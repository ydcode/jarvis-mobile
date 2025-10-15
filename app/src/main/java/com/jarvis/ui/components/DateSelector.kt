package com.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期选择器组件
 * @param selectedDate 当前选中的日期
 * @param onDateChange 日期改变回调
 * @param onTodayClick 点击"今天"按钮的回调
 */
@Composable
fun DateSelector(
    selectedDate: Calendar,
    onDateChange: (Calendar) -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：日期显示和箭头
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 向前箭头
            IconButton(
                onClick = {
                    val newDate = selectedDate.clone() as Calendar
                    newDate.add(Calendar.DAY_OF_YEAR, -1)
                    onDateChange(newDate)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "前一天",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 日期显示
            Column {
                Text(
                    text = formatDateFull(selectedDate),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatWeekday(selectedDate),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 向后箭头
            IconButton(
                onClick = {
                    val newDate = selectedDate.clone() as Calendar
                    newDate.add(Calendar.DAY_OF_YEAR, 1)
                    onDateChange(newDate)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "后一天",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 右侧："今天"按钮
        Button(
            onClick = onTodayClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "今天",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 格式化完整日期 (例如: 2025年1月15日)
 */
private fun formatDateFull(calendar: Calendar): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE)
    return sdf.format(calendar.time)
}

/**
 * 格式化星期 (例如: 星期三)
 */
private fun formatWeekday(calendar: Calendar): String {
    val weekdays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
    return weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1]
}

/**
 * 检查是否是今天
 */
fun isSameDay(date1: Calendar, date2: Calendar): Boolean {
    return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
            date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR)
}
