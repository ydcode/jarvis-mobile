package com.jarvis.ui.components

import android.util.Log
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.data.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Canvas绘制工具函数集合
 * 包含所有时间线的绘制逻辑
 */

/**
 * 绘制小时级别及以下的刻度（分钟、5分钟、10分钟、半小时、小时）
 */
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawHourLevelScale(
    scale: TimeScaleLevel,
    unitHeightPx: Float,
    scrollOffsetPx: Float,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    currentTimeColor: Color,
    width: Float,
    events: List<CalendarEvent>
) {
    val leftMargin = 60.dp.toPx()

    // 根据刻度类型计算单位
    val unitName = when (scale) {
        TimeScaleLevel.MINUTE -> "分钟"
        TimeScaleLevel.FIVE_MINUTES -> "5分钟"
        TimeScaleLevel.TEN_MINUTES -> "10分钟"
        TimeScaleLevel.HALF_HOUR -> "半小时"
        TimeScaleLevel.HOUR -> "小时"
        else -> "小时"
    }

    // 计算可见范围 - 支持无限滚动
    val startUnit = (scrollOffsetPx / unitHeightPx).toInt() - 1
    val endUnit = ((scrollOffsetPx + size.height) / unitHeightPx).toInt() + 1

    // 绘制刻度线
    val linesPath = Path()
    for (i in startUnit..endUnit) {
        val y = i * unitHeightPx - scrollOffsetPx
        if (y >= -10 && y <= size.height + 10) {
            linesPath.moveTo(leftMargin, y)
            linesPath.lineTo(size.width, y)
        }
    }

    drawPath(
        path = linesPath,
        color = lineColor.copy(alpha = 0.4f),
        style = Stroke(
            width = 0.5.dp.toPx(),
            cap = StrokeCap.Square
        )
    )

    // 绘制标签
    val textStyle = TextStyle(
        fontSize = 12.sp,
        color = textColor
    )

    for (i in startUnit..endUnit) {
        val y = i * unitHeightPx - scrollOffsetPx
        if (y >= -20 && y <= size.height + 20) {
            val timeText = when (scale) {
                TimeScaleLevel.MINUTE -> {
                    // 支持无限滚动的分钟显示
                    val totalMinutes = i
                    val hour = (totalMinutes / 60) % 24
                    val minute = totalMinutes % 60
                    val adjustedHour = if (hour < 0) hour + 24 else hour
                    val adjustedMinute = if (minute < 0) minute + 60 else minute
                    String.format("%02d:%02d", adjustedHour, adjustedMinute)
                }
                TimeScaleLevel.FIVE_MINUTES -> {
                    // 支持无限滚动的5分钟显示
                    val totalMinutes = i * 5
                    val hour = (totalMinutes / 60) % 24
                    val minute = totalMinutes % 60
                    val adjustedHour = if (hour < 0) hour + 24 else hour
                    val adjustedMinute = if (minute < 0) minute + 60 else minute
                    String.format("%02d:%02d", adjustedHour, adjustedMinute)
                }
                TimeScaleLevel.TEN_MINUTES -> {
                    // 支持无限滚动的10分钟显示
                    val totalMinutes = i * 10
                    val hour = (totalMinutes / 60) % 24
                    val minute = totalMinutes % 60
                    val adjustedHour = if (hour < 0) hour + 24 else hour
                    val adjustedMinute = if (minute < 0) minute + 60 else minute
                    String.format("%02d:%02d", adjustedHour, adjustedMinute)
                }
                TimeScaleLevel.HALF_HOUR -> {
                    // 支持无限滚动的半小时显示
                    val totalMinutes = i * 30
                    val hour = (totalMinutes / 60) % 24
                    val minute = totalMinutes % 60
                    val adjustedHour = if (hour < 0) hour + 24 else hour
                    val adjustedMinute = if (minute < 0) minute + 60 else minute
                    String.format("%02d:%02d", adjustedHour, adjustedMinute)
                }
                TimeScaleLevel.HOUR -> {
                    // 支持无限滚动的小时显示
                    val hour = i % 24
                    val adjustedHour = if (hour < 0) hour + 24 else hour
                    String.format("%02d:00", adjustedHour)
                }
                else -> "$i"
            }

            val textLayout = textMeasurer.measure(
                text = timeText,
                style = textStyle
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x = 55.dp.toPx() - textLayout.size.width,
                    y = y - textLayout.size.height / 2
                )
            )
        }
    }

    // 绘制当前时间指示器（所有时间级别）
    drawCurrentTimeIndicator(unitHeightPx, scrollOffsetPx, currentTimeColor, width, textMeasurer, scale)

    // 绘制事件（仅在小时级别）
    if (scale == TimeScaleLevel.HOUR) {
        drawEvents(events, unitHeightPx, scrollOffsetPx, width, textMeasurer)
    }
}

/**
 * 绘制事件卡片 - Apple Calendar 风格
 */
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawEvents(
    events: List<CalendarEvent>,
    hourHeightPx: Float,
    scrollOffsetPx: Float,
    width: Float,
    textMeasurer: TextMeasurer
) {
    val leftMargin = 65.dp.toPx()
    val rightMargin = 8.dp.toPx()
    val eventWidth = width - leftMargin - rightMargin

    events.forEach { event ->
        val startMinutes = event.getOffsetMinutes()
        val durationMinutes = event.getDurationMinutes()

        val top = (startMinutes / 60f) * hourHeightPx - scrollOffsetPx + 2.dp.toPx()
        val height = (durationMinutes / 60f) * hourHeightPx - 4.dp.toPx()

        // 只绘制可见的事件
        if (top < size.height && top + height > 0) {
            val eventHeight = height.coerceAtLeast(24.dp.toPx())

            // 绘制事件背景
            drawRoundRect(
                color = event.color.copy(alpha = if (event.isCompleted) 0.4f else 1f),
                topLeft = Offset(leftMargin, top),
                size = Size(eventWidth, eventHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // 绘制左侧边条（更深的颜色）
            drawRoundRect(
                color = event.color,
                topLeft = Offset(leftMargin, top),
                size = Size(3.dp.toPx(), eventHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // 绘制事件标题
            if (eventHeight > 16.dp.toPx()) {
                val titleStyle = TextStyle(
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                val titleText = textMeasurer.measure(
                    text = event.title,
                    style = titleStyle,
                    constraints = androidx.compose.ui.unit.Constraints(
                        maxWidth = (eventWidth - 16.dp.toPx()).toInt()
                    )
                )

                drawText(
                    textLayoutResult = titleText,
                    topLeft = Offset(
                        x = leftMargin + 8.dp.toPx(),
                        y = top + 4.dp.toPx()
                    )
                )

                // 如果高度足够，显示时间
                if (eventHeight > 36.dp.toPx() && durationMinutes >= 30) {
                    val timeStyle = TextStyle(
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    val timeText = formatEventTime(event)
                    val timeLayout = textMeasurer.measure(
                        text = timeText,
                        style = timeStyle
                    )

                    drawText(
                        textLayoutResult = timeLayout,
                        topLeft = Offset(
                            x = leftMargin + 8.dp.toPx(),
                            y = top + 20.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

/**
 * 格式化事件时间
 */
fun formatEventTime(event: CalendarEvent): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val start = sdf.format(Date(event.startTime))
    val end = sdf.format(Date(event.endTime))
    return "$start-$end"
}

/**
 * 绘制当前时间指示器
 */
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawCurrentTimeIndicator(
    unitHeightPx: Float,
    scrollOffsetPx: Float,
    currentTimeColor: Color,
    width: Float,
    textMeasurer: TextMeasurer,
    scale: TimeScaleLevel
) {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
    val currentWeekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    // 根据刻度计算当前时间的位置
    val currentUnit = when (scale) {
        TimeScaleLevel.MINUTE -> (currentHour * 60 + currentMinute).toFloat()
        TimeScaleLevel.FIVE_MINUTES -> (currentHour * 60 + currentMinute) / 5f
        TimeScaleLevel.TEN_MINUTES -> (currentHour * 60 + currentMinute) / 10f
        TimeScaleLevel.HALF_HOUR -> (currentHour * 60 + currentMinute) / 30f
        TimeScaleLevel.HOUR -> currentHour + currentMinute / 60f
        TimeScaleLevel.DAY -> {
            // 天视图中 i=0 代表今天，所以当前时间应该是 0.0
            // 计算今天已过的时间比例（0.0-1.0）
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val minuteOfDay = calendar.get(Calendar.MINUTE)
            val secondOfDay = calendar.get(Calendar.SECOND)
            val progressInDay = (hourOfDay * 3600 + minuteOfDay * 60 + secondOfDay) / (24f * 3600)

            progressInDay
        }
        TimeScaleLevel.WEEK -> {
            // 周视图中 i=0 代表当前周，所以当前时间应该是 0.0
            // 计算当前时间在本周内的进度（0.0-1.0）
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            // Calendar.DAY_OF_WEEK: 1=Sunday, 2=Monday, ..., 7=Saturday
            // 转换为 Monday=0, ..., Sunday=6 的系统
            val mondayBasedDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

            // 计算本周已过的时间比例
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val minuteOfDay = calendar.get(Calendar.MINUTE)
            val secondOfDay = calendar.get(Calendar.SECOND)
            val progressInDay = (hourOfDay * 3600 + minuteOfDay * 60 + secondOfDay) / (24f * 3600)
            val progressInWeek = (mondayBasedDayOfWeek + progressInDay) / 7f

            progressInWeek
        }
        TimeScaleLevel.MONTH -> {
            // 计算当前日期在当前月份中的精确位置
            // 月刻度中,i=0对应当前月份
            val monthStartCal = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 计算当前月有多少天
            val monthEndCal = (monthStartCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }
            val monthDays = ((monthEndCal.timeInMillis - monthStartCal.timeInMillis) / (24 * 60 * 60 * 1000)).toFloat()

            // 计算当前时间距离月初有多少天
            val currentDaysSinceMonthStart = ((calendar.timeInMillis - monthStartCal.timeInMillis) / (24f * 60 * 60 * 1000))

            // 返回在月内的相对位置（0.0 到 1.0 之间）
            currentDaysSinceMonthStart / monthDays
        }
        TimeScaleLevel.QUARTER -> {
            // 计算当前日期在当前季度中的精确位置
            // 获取当前季度的起始日期
            val currentQuarter = currentMonth / 3  // 0-3 对应 Q1-Q4
            val quarterStartMonth = currentQuarter * 3  // 季度开始月份 (0, 3, 6, 9)

            val quarterStartCal = (calendar.clone() as Calendar).apply {
                set(Calendar.MONTH, quarterStartMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 计算当前季度有多少天
            val quarterEndCal = (quarterStartCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 3)
            }
            val quarterDays = ((quarterEndCal.timeInMillis - quarterStartCal.timeInMillis) / (24 * 60 * 60 * 1000)).toFloat()

            // 计算当前时间距离季度开始有多少天（包含小数部分表示具体时刻）
            val currentDaysSinceQuarterStart = ((calendar.timeInMillis - quarterStartCal.timeInMillis) / (24f * 60 * 60 * 1000))

            // 返回在季度内的相对位置（0.0 到 1.0 之间）
            currentDaysSinceQuarterStart / quarterDays
        }
        TimeScaleLevel.YEAR -> {
            // 计算当前日期在当前年份中的精确位置
            // 年份刻度中,当前年份在位置10
            val yearStartCal = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val yearEndCal = (yearStartCal.clone() as Calendar).apply {
                add(Calendar.YEAR, 1)
            }

            val yearDays = ((yearEndCal.timeInMillis - yearStartCal.timeInMillis) / (24 * 60 * 60 * 1000)).toFloat()
            val daysSinceYearStart = ((calendar.timeInMillis - yearStartCal.timeInMillis) / (24f * 60 * 60 * 1000))

            // 返回在年份内的位置（10.0 + [0.0 到 1.0]）
            10f + (daysSinceYearStart / yearDays)
        }
        TimeScaleLevel.DECADE -> {
            // 计算当前年份在当前10年中的精确位置
            // 10年刻度中,当前10年在位置10,每个单位代表10年
            val currentDecade = (currentYear / 10) * 10  // 当前10年的起始年份
            val yearInDecade = currentYear - currentDecade  // 当前年份在10年中的位置 (0-9)

            // 计算年内进度
            val yearStartCal = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val yearEndCal = (yearStartCal.clone() as Calendar).apply {
                add(Calendar.YEAR, 1)
            }

            val yearDays = ((yearEndCal.timeInMillis - yearStartCal.timeInMillis) / (24 * 60 * 60 * 1000)).toFloat()
            val daysSinceYearStart = ((calendar.timeInMillis - yearStartCal.timeInMillis) / (24f * 60 * 60 * 1000))
            val yearProgress = daysSinceYearStart / yearDays

            // 将年份位置转换为10年单位: yearInDecade/10表示在这个10年中的比例
            // 返回在10年内的位置（10.0 + [0.0 到 1.0]）
            10f + (yearInDecade + yearProgress) / 10f
        }
        TimeScaleLevel.CENTURY -> {
            // 计算当前年份在当前100年中的精确位置
            // 100年刻度中,当前100年在位置10,每个单位代表100年
            val currentCentury = (currentYear / 100) * 100  // 当前100年的起始年份
            val yearInCentury = currentYear - currentCentury  // 当前年份在100年中的位置 (0-99)

            // 计算年内进度
            val yearStartCal = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val yearEndCal = (yearStartCal.clone() as Calendar).apply {
                add(Calendar.YEAR, 1)
            }

            val yearDays = ((yearEndCal.timeInMillis - yearStartCal.timeInMillis) / (24 * 60 * 60 * 1000)).toFloat()
            val daysSinceYearStart = ((calendar.timeInMillis - yearStartCal.timeInMillis) / (24f * 60 * 60 * 1000))
            val yearProgress = daysSinceYearStart / yearDays

            // 将年份位置转换为100年单位: yearInCentury/100表示在这个100年中的比例
            // 返回在100年内的位置（10.0 + [0.0 到 1.0]）
            10f + (yearInCentury + yearProgress) / 100f
        }
    }

    val y = currentUnit * unitHeightPx - scrollOffsetPx

    if (y >= 0 && y <= size.height) {
        // 根据刻度类型选择左边距
        val leftMargin = when (scale) {
            TimeScaleLevel.MINUTE, TimeScaleLevel.FIVE_MINUTES, TimeScaleLevel.TEN_MINUTES,
            TimeScaleLevel.HALF_HOUR, TimeScaleLevel.HOUR -> 60.dp.toPx()
            else -> 80.dp.toPx()
        }

        // 根据刻度类型生成不同的当前时间文本
        val timeText = when (scale) {
            // 小时级别及以下：显示时:分
            TimeScaleLevel.MINUTE, TimeScaleLevel.FIVE_MINUTES, TimeScaleLevel.TEN_MINUTES,
            TimeScaleLevel.HALF_HOUR, TimeScaleLevel.HOUR -> {
                String.format("%02d:%02d", currentHour, currentMinute)
            }
            // 天/周级别：显示月/日
            TimeScaleLevel.DAY, TimeScaleLevel.WEEK -> {
                val monthNum = calendar.get(Calendar.MONTH) + 1  // Calendar.MONTH是0-11，需要+1
                val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
                String.format("%02d/%02d", monthNum, dayNum)
            }
            // 月级别：显示年月日
            TimeScaleLevel.MONTH -> {
                val monthNum = calendar.get(Calendar.MONTH) + 1
                val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
                String.format("%04d/%02d/%02d", currentYear, monthNum, dayNum)
            }
            // 季度级别：显示年/Q季度
            TimeScaleLevel.QUARTER -> {
                val quarter = currentMonth / 3 + 1
                "${currentYear}/Q${quarter}"
            }
            // 年级别：显示年月日
            TimeScaleLevel.YEAR -> {
                val monthNum = calendar.get(Calendar.MONTH) + 1
                val dayNum = calendar.get(Calendar.DAY_OF_MONTH)
                String.format("%04d/%02d/%02d", currentYear, monthNum, dayNum)
            }
            // 10年/100年级别：显示年份
            TimeScaleLevel.DECADE, TimeScaleLevel.CENTURY -> {
                "$currentYear"
            }
        }

        val textStyle = TextStyle(
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        val textLayoutResult = textMeasurer.measure(
            text = timeText,
            style = textStyle
        )

        // 计算胶囊形状标签的尺寸
        val textPaddingHorizontal = 6.dp.toPx()
        val textPaddingVertical = 1.dp.toPx()  // 最小垂直padding
        val labelHeight = textLayoutResult.size.height + textPaddingVertical * 2  // 基于文字高度计算
        val labelRadius = labelHeight / 2  // 半圆半径
        val rectWidth = textLayoutResult.size.width + textPaddingHorizontal * 2 - labelRadius * 2 // 矩形部分宽度
        val labelTotalWidth = textLayoutResult.size.width + textPaddingHorizontal * 2  // 包含半圆的总宽度

        // 标签位置（靠近左边，留一些空间给时间刻度）
        val labelRight = leftMargin - 4.dp.toPx()
        val labelLeft = labelRight - labelTotalWidth

        // 绘制左侧半圆
        drawCircle(
            color = currentTimeColor,
            radius = labelRadius,
            center = Offset(labelLeft + labelRadius, y)
        )

        // 绘制中间矩形部分
        if (rectWidth > 0) {
            drawRect(
                color = currentTimeColor,
                topLeft = Offset(labelLeft + labelRadius, y - labelRadius),
                size = Size(rectWidth, labelHeight)
            )
        }

        // 绘制右侧半圆
        drawCircle(
            color = currentTimeColor,
            radius = labelRadius,
            center = Offset(labelRight - labelRadius, y)
        )

        // 白色时间文本（居中显示）
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = labelLeft + (labelTotalWidth - textLayoutResult.size.width) / 2,
                y = y - textLayoutResult.size.height / 2
            )
        )

        // 绘制红色横线（从时间标签右边缘开始，无缝连接，加粗）
        drawLine(
            color = currentTimeColor,
            start = Offset(labelRight, y),
            end = Offset(width, y),
            strokeWidth = 2.dp.toPx()
        )
    }
}

// 天级别绘制
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawDayScale(
    unitHeightPx: Float,
    scrollOffsetPx: Float,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    currentTimeColor: Color,
    width: Float
) {
    val leftMargin = 80.dp.toPx()
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    // 计算可见范围的天数
    val startDay = (scrollOffsetPx / unitHeightPx).toInt() - 1
    val endDay = ((scrollOffsetPx + size.height) / unitHeightPx).toInt() + 1

    // 绘制刻度线
    val linesPath = Path()
    for (i in startDay..endDay) {
        val y = i * unitHeightPx - scrollOffsetPx
        if (y >= -10 && y <= size.height + 10) {
            linesPath.moveTo(leftMargin, y)
            linesPath.lineTo(size.width, y)
        }
    }

    drawPath(
        path = linesPath,
        color = lineColor.copy(alpha = 0.4f),
        style = Stroke(
            width = 0.5.dp.toPx(),
            cap = StrokeCap.Square
        )
    )

    // 绘制标签
    val textStyle = TextStyle(
        fontSize = 12.sp,
        color = textColor
    )

    for (i in startDay..endDay) {
        val y = i * unitHeightPx - scrollOffsetPx
        if (y >= -20 && y <= size.height + 20) {
            val date = (calendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, i)
            }
            val dateText = dateFormat.format(date.time)
            val textLayout = textMeasurer.measure(
                text = dateText,
                style = textStyle
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x = leftMargin - textLayout.size.width - 12.dp.toPx(),
                    y = y - textLayout.size.height / 2
                )
            )
        }
    }

    // 绘制当前时间指示器
    drawCurrentTimeIndicator(unitHeightPx, scrollOffsetPx, currentTimeColor, width, textMeasurer, TimeScaleLevel.DAY)
}

// 周级别绘制
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawWeekScale(
    size: Size,
    unitHeightPx: Float,
    scrollOffset: Float,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    currentTimeColor: Color,
    width: Float
) {
    val calendar = Calendar.getInstance()
    val leftMargin = 80.dp.toPx()
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    val textStyle = TextStyle(
        fontSize = 13.sp,
        color = textColor
    )

    // 计算可见范围的周数
    val startWeek = (scrollOffset / unitHeightPx).toInt() - 2
    val endWeek = ((scrollOffset + size.height) / unitHeightPx).toInt() + 2

    // 绘制可见范围内的周（每周显示该周的起始日期）
    for (i in startWeek..endWeek) {
        val weekStartDate = (calendar.clone() as Calendar).apply {
            add(Calendar.WEEK_OF_YEAR, i)
            // 设置为本周的第一天（周一）
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val y = i * unitHeightPx - scrollOffset

        if (y >= -20 && y <= size.height + 20) {
            // 绘制横线
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(leftMargin, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx()
            )

            // 绘制周起始日期标签
            val dateText = dateFormat.format(weekStartDate.time)
            val textLayout = textMeasurer.measure(
                text = dateText,
                style = textStyle
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x = leftMargin - textLayout.size.width - 12.dp.toPx(),
                    y = y - textLayout.size.height / 2
                )
            )
        }
    }

    // 绘制当前时间指示器
    drawCurrentTimeIndicator(unitHeightPx, scrollOffset, currentTimeColor, width, textMeasurer, TimeScaleLevel.WEEK)
}

// 月级别绘制
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawMonthScale(
    size: Size,
    unitHeightPx: Float,
    scrollOffset: Float,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    currentTimeColor: Color,
    width: Float
) {
    val calendar = Calendar.getInstance()
    val leftMargin = 80.dp.toPx()
    val dateFormat = SimpleDateFormat("yyyy/MM", Locale.getDefault())

    val textStyle = TextStyle(
        fontSize = 13.sp,
        color = textColor
    )

    // 计算可见范围的月数
    val startMonth = (scrollOffset / unitHeightPx).toInt() - 2
    val endMonth = ((scrollOffset + size.height) / unitHeightPx).toInt() + 2

    // 绘制可见范围内的月
    for (i in startMonth..endMonth) {
        val monthDate = (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, i)
            set(Calendar.DAY_OF_MONTH, 1) // 设置为该月的第一天
        }

        val y = i * unitHeightPx - scrollOffset

        if (y >= -20 && y <= size.height + 20) {
            // 绘制横线
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(leftMargin, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx()
            )

            // 绘制月份标签（年/月格式）
            val dateText = dateFormat.format(monthDate.time)
            val textLayout = textMeasurer.measure(
                text = dateText,
                style = textStyle
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x = leftMargin - textLayout.size.width - 12.dp.toPx(),
                    y = y - textLayout.size.height / 2
                )
            )
        }
    }

    // 绘制当前时间指示器
    drawCurrentTimeIndicator(unitHeightPx, scrollOffset, currentTimeColor, width, textMeasurer, TimeScaleLevel.MONTH)
}

// 季度级别绘制
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawQuarterScale(
    size: Size,
    unitHeightPx: Float,
    scrollOffset: Float,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    currentTimeColor: Color,
    width: Float
) {
    val calendar = Calendar.getInstance()
    val leftMargin = 80.dp.toPx()

    val textStyle = TextStyle(
        fontSize = 13.sp,
        color = textColor
    )

    // 计算可见范围的季度数
    val startQuarter = (scrollOffset / unitHeightPx).toInt() - 2
    val endQuarter = ((scrollOffset + size.height) / unitHeightPx).toInt() + 2

    // 绘制可见范围内的季度
    for (i in startQuarter..endQuarter) {
        val quarterDate = (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, i * 3)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val y = i * unitHeightPx - scrollOffset

        if (y >= -20 && y <= size.height + 20) {
            // 绘制横线
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(leftMargin, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx()
            )

            // 绘制季度标签（年/Q季度格式）
            val year = quarterDate.get(Calendar.YEAR)
            val month = quarterDate.get(Calendar.MONTH)
            val quarter = month / 3 + 1
            val dateText = "${year}/Q${quarter}"

            val textLayout = textMeasurer.measure(
                text = dateText,
                style = textStyle
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x = leftMargin - textLayout.size.width - 12.dp.toPx(),
                    y = y - textLayout.size.height / 2
                )
            )
        }
    }

    // 绘制当前时间指示器
    drawCurrentTimeIndicator(unitHeightPx, scrollOffset, currentTimeColor, width, textMeasurer, TimeScaleLevel.QUARTER)
}

// 年级别绘制
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawYearScale(
    size: Size,
    unitHeightPx: Float,
    scrollOffset: Float,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    currentTimeColor: Color,
    width: Float
) {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val leftMargin = 80.dp.toPx()

    val textStyle = TextStyle(
        fontSize = 13.sp,
        color = textColor
    )

    // 计算可见范围的年份索引（相对于当前年份的偏移）
    val startYearOffset = (scrollOffset / unitHeightPx).toInt() - 10 - 2
    val endYearOffset = ((scrollOffset + size.height) / unitHeightPx).toInt() - 10 + 2

    for (i in startYearOffset..endYearOffset) {
        val year = currentYear + i
        val y = (i + 10) * unitHeightPx - scrollOffset

        if (y >= -20 && y <= size.height + 20) {
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(leftMargin, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx()
            )

            // 格式化年份：公元前显示为 "年份 BC"
            val yearText = if (year > 0) {
                "$year"
            } else {
                "${-year + 1} BC"  // 公元前1年是 year = 0，所以需要 +1
            }

            val textLayout = textMeasurer.measure(
                text = yearText,
                style = textStyle
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x = leftMargin - textLayout.size.width - 12.dp.toPx(),
                    y = y - textLayout.size.height / 2
                )
            )
        }
    }

    // 绘制当前时间指示器
    drawCurrentTimeIndicator(unitHeightPx, scrollOffset, currentTimeColor, width, textMeasurer, TimeScaleLevel.YEAR)
}

// 10年级别绘制
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawDecadeScale(
    size: Size,
    unitHeightPx: Float,
    scrollOffset: Float,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    currentTimeColor: Color,
    width: Float
) {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentDecade = (currentYear / 10) * 10
    val leftMargin = 80.dp.toPx()

    val textStyle = TextStyle(
        fontSize = 13.sp,
        color = textColor
    )

    // 计算可见范围的10年索引（相对于当前10年的偏移）
    val startDecadeOffset = (scrollOffset / unitHeightPx).toInt() - 10 - 2
    val endDecadeOffset = ((scrollOffset + size.height) / unitHeightPx).toInt() - 10 + 2

    for (i in startDecadeOffset..endDecadeOffset) {
        val decade = currentDecade + i * 10
        val y = (i + 10) * unitHeightPx - scrollOffset

        if (y >= -20 && y <= size.height + 20) {
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(leftMargin, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx()
            )

            // 格式化10年：公元前显示为 "年份 BC"
            val decadeText = if (decade > 0) {
                "$decade"
            } else {
                "${-decade + 10} BC"  // 公元前的10年需要调整
            }

            val textLayout = textMeasurer.measure(
                text = decadeText,
                style = textStyle
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x = leftMargin - textLayout.size.width - 12.dp.toPx(),
                    y = y - textLayout.size.height / 2
                )
            )
        }
    }

    // 绘制当前时间指示器
    drawCurrentTimeIndicator(unitHeightPx, scrollOffset, currentTimeColor, width, textMeasurer, TimeScaleLevel.DECADE)
}

// 100年级别
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawCenturyScale(
    size: Size,
    unitHeightPx: Float,
    scrollOffset: Float,
    textMeasurer: TextMeasurer,
    lineColor: Color,
    textColor: Color,
    currentTimeColor: Color,
    width: Float
) {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentCentury = (currentYear / 100) * 100
    val leftMargin = 80.dp.toPx()

    val textStyle = TextStyle(
        fontSize = 13.sp,
        color = textColor
    )

    // 计算可见范围的100年索引（相对于当前100年的偏移）
    val startCenturyOffset = (scrollOffset / unitHeightPx).toInt() - 10 - 2
    val endCenturyOffset = ((scrollOffset + size.height) / unitHeightPx).toInt() - 10 + 2

    for (i in startCenturyOffset..endCenturyOffset) {
        val century = currentCentury + i * 100
        val y = (i + 10) * unitHeightPx - scrollOffset

        if (y >= -20 && y <= size.height + 20) {
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(leftMargin, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx()
            )

            // 格式化100年：公元前显示为 "年份 BC"
            val centuryText = if (century > 0) {
                "$century"
            } else {
                "${-century + 100} BC"  // 公元前的100年需要调整
            }

            val textLayout = textMeasurer.measure(
                text = centuryText,
                style = textStyle
            )

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    x = leftMargin - textLayout.size.width - 12.dp.toPx(),
                    y = y - textLayout.size.height / 2
                )
            )
        }
    }

    // 绘制当前时间指示器
    drawCurrentTimeIndicator(unitHeightPx, scrollOffset, currentTimeColor, width, textMeasurer, TimeScaleLevel.CENTURY)
}
