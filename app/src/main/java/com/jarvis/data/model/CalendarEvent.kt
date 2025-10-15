package com.jarvis.data.model

import androidx.compose.ui.graphics.Color
import java.util.*

/**
 * 日历事件数据模型
 * @param id 唯一标识符
 * @param title 标题
 * @param description 描述
 * @param startTime 开始时间（时间戳）
 * @param endTime 结束时间（时间戳）
 * @param color 事件颜色
 * @param isCompleted 是否完成
 */
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val color: Color = Color(0xFF07C160),
    val isCompleted: Boolean = false
) {
    /**
     * 获取事件持续时长（分钟）
     */
    fun getDurationMinutes(): Int {
        return ((endTime - startTime) / 60000).toInt()
    }

    /**
     * 获取事件开始的小时
     */
    fun getStartHour(): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    /**
     * 获取事件开始的分钟
     */
    fun getStartMinute(): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        return calendar.get(Calendar.MINUTE)
    }

    /**
     * 获取事件在时间线上的偏移量（从00:00开始的分钟数）
     */
    fun getOffsetMinutes(): Int {
        return getStartHour() * 60 + getStartMinute()
    }
}

/**
 * 示例数据 - 今天的日程
 */
fun getSampleEvents(): List<CalendarEvent> {
    return getSampleEventsForDate(Calendar.getInstance())
}

/**
 * 获取指定日期的示例事件
 * @param date 指定的日期
 * @return 该日期的事件列表
 */
fun getSampleEventsForDate(date: Calendar): List<CalendarEvent> {
    val targetDate = date.clone() as Calendar
    targetDate.set(Calendar.HOUR_OF_DAY, 0)
    targetDate.set(Calendar.MINUTE, 0)
    targetDate.set(Calendar.SECOND, 0)
    targetDate.set(Calendar.MILLISECOND, 0)

    // 获取日期的hash值，用于生成不同的事件
    val dayOfYear = targetDate.get(Calendar.DAY_OF_YEAR)
    val year = targetDate.get(Calendar.YEAR)
    val dateHash = (year * 1000 + dayOfYear) % 7 // 用于决定显示哪些事件

    val allPossibleEvents = listOf(
        CalendarEvent(
            id = "1",
            title = "晨会",
            description = "团队日常站会",
            startTime = targetDate.timeInMillis + (9 * 60 + 0) * 60000L,
            endTime = targetDate.timeInMillis + (9 * 60 + 30) * 60000L,
            color = Color(0xFF5FC9F8),
            isCompleted = true
        ),
        CalendarEvent(
            id = "2",
            title = "产品需求评审",
            description = "讨论Q4新功能",
            startTime = targetDate.timeInMillis + (10 * 60 + 0) * 60000L,
            endTime = targetDate.timeInMillis + (11 * 60 + 30) * 60000L,
            color = Color(0xFFFF6B6B),
            isCompleted = false
        ),
        CalendarEvent(
            id = "3",
            title = "午餐时间",
            description = "和团队一起吃饭",
            startTime = targetDate.timeInMillis + (12 * 60 + 0) * 60000L,
            endTime = targetDate.timeInMillis + (13 * 60 + 0) * 60000L,
            color = Color(0xFFFFA500),
            isCompleted = false
        ),
        CalendarEvent(
            id = "4",
            title = "代码开发",
            description = "实现日历功能",
            startTime = targetDate.timeInMillis + (14 * 60 + 0) * 60000L,
            endTime = targetDate.timeInMillis + (16 * 60 + 30) * 60000L,
            color = Color(0xFF07C160),
            isCompleted = false
        ),
        CalendarEvent(
            id = "5",
            title = "健身运动",
            description = "跑步5公里",
            startTime = targetDate.timeInMillis + (18 * 60 + 0) * 60000L,
            endTime = targetDate.timeInMillis + (19 * 60 + 0) * 60000L,
            color = Color(0xFF9C27B0),
            isCompleted = false
        ),
        CalendarEvent(
            id = "6",
            title = "学习时间",
            description = "Jetpack Compose 进阶",
            startTime = targetDate.timeInMillis + (20 * 60 + 0) * 60000L,
            endTime = targetDate.timeInMillis + (21 * 60 + 30) * 60000L,
            color = Color(0xFF3F51B5),
            isCompleted = false
        ),
        CalendarEvent(
            id = "7",
            title = "项目复盘",
            description = "回顾本周工作",
            startTime = targetDate.timeInMillis + (15 * 60 + 0) * 60000L,
            endTime = targetDate.timeInMillis + (16 * 60 + 0) * 60000L,
            color = Color(0xFFE91E63),
            isCompleted = false
        )
    )

    // 根据日期返回不同数量的事件，模拟真实场景
    return when (dateHash) {
        0 -> allPossibleEvents.take(3) // 3个事件
        1 -> allPossibleEvents.take(4) // 4个事件
        2 -> allPossibleEvents.take(6) // 6个事件
        3 -> allPossibleEvents.take(2) // 2个事件
        4 -> allPossibleEvents.take(5) // 5个事件
        5 -> allPossibleEvents // 全部事件
        else -> allPossibleEvents.take(4) // 默认4个事件
    }
}
