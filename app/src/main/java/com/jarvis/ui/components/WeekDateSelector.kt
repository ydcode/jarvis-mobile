package com.jarvis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlin.math.abs

/**
 * Apple Calendar 风格的周视图日期选择器
 */
@Composable
fun WeekDateSelector(
    selectedDate: Calendar,
    onDateChange: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    // 当前显示的周（可能与selectedDate所在周不同）
    var currentWeekStart by remember { mutableStateOf(getWeekStart(selectedDate)) }

    // 当selectedDate变化时，如果不在当前显示的周内，更新currentWeekStart
    LaunchedEffect(selectedDate) {
        if (!isInSameWeek(selectedDate, currentWeekStart)) {
            currentWeekStart = getWeekStart(selectedDate)
        }
    }


    // 今天的日期（用于高亮）
    val today = remember { Calendar.getInstance() }

    // 拖拽状态
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var startDragOffset by remember { mutableStateOf(0f) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // 计算实际的日期区域宽度（减去padding）
    val horizontalPadding = 16.dp
    val screenWidth = configuration.screenWidthDp.dp
    val actualContentWidth = screenWidth - (horizontalPadding * 2)
    val actualContentWidthPx = with(density) { actualContentWidth.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 星期标签行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
            weekLabels.forEachIndexed { index, day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day,
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93), // iOS secondary label color
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 日期选择器 - 支持左右滑动切换周
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 16.dp)
                .clipToBounds()
                .pointerInput(currentWeekStart) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            startDragOffset = dragOffsetX.value
                            Log.d("WeekSwipe", "Drag started")
                        },
                        onDragEnd = {
                            isDragging = false
                            coroutineScope.launch {
                                // 计算一天的宽度 (总宽度/7)
                                val dayWidth = actualContentWidthPx / 7
                                val threshold = dayWidth / 2  // 半天的宽度

                                Log.d("WeekSwipe", "Drag ended. Offset: ${dragOffsetX.value}, Threshold: $threshold")

                                when {
                                    dragOffsetX.value > threshold -> {
                                        Log.d("WeekSwipe", "Switching to previous week")
                                        // 向右滑动超过阈值，切换到上一周
                                        currentWeekStart = (currentWeekStart.clone() as Calendar).apply {
                                            add(Calendar.WEEK_OF_YEAR, -1)
                                        }
                                        dragOffsetX.snapTo(0f)
                                    }
                                    dragOffsetX.value < -threshold -> {
                                        Log.d("WeekSwipe", "Switching to next week")
                                        // 向左滑动超过阈值，切换到下一周
                                        currentWeekStart = (currentWeekStart.clone() as Calendar).apply {
                                            add(Calendar.WEEK_OF_YEAR, 1)
                                        }
                                        dragOffsetX.snapTo(0f)
                                    }
                                    else -> {
                                        Log.d("WeekSwipe", "Spring back to center")
                                        // 未超过阈值，回弹到原位置
                                        dragOffsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring()
                                        )
                                    }
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                                Log.d("WeekSwipe", "Dragging: offset=${dragOffsetX.value}, delta=$dragAmount")
                            }
                        }
                    )
                }
        ) {
            // 前一周、当前周、下一周的日期数据
            val prevWeek = (currentWeekStart.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, -1) }
            val nextWeek = (currentWeekStart.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, 1) }

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = dragOffsetX.value
                    }
            ) {
                // 前一周 (左侧)
                Row(
                    modifier = Modifier.width(actualContentWidth),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    getWeekDates(prevWeek).forEach { date ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onDateChange(date)
                                    currentWeekStart = prevWeek
                                    coroutineScope.launch { dragOffsetX.snapTo(0f) }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DateCell(
                                date = date,
                                isSelected = isSameDay(date, selectedDate),
                                isToday = isSameDay(date, today)
                            )
                        }
                    }
                }

                // 当前周 (中间)
                Row(
                    modifier = Modifier.width(actualContentWidth),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    getWeekDates(currentWeekStart).forEach { date ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onDateChange(date) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DateCell(
                                date = date,
                                isSelected = isSameDay(date, selectedDate),
                                isToday = isSameDay(date, today)
                            )
                        }
                    }
                }

                // 下一周 (右侧)
                Row(
                    modifier = Modifier.width(actualContentWidth),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    getWeekDates(nextWeek).forEach { date ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onDateChange(date)
                                    currentWeekStart = nextWeek
                                    coroutineScope.launch { dragOffsetX.snapTo(0f) }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DateCell(
                                date = date,
                                isSelected = isSameDay(date, selectedDate),
                                isToday = isSameDay(date, today)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 日期详情行（显示当前选中的日期）
        Text(
            text = formatSelectedDateDetail(selectedDate),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        HorizontalDivider(color = Color(0xFFE5E5E7), thickness = 0.5.dp)
    }
}

/**
 * 获取给定日期所在周的开始日期（周日）
 */
private fun getWeekStart(date: Calendar): Calendar {
    return (date.clone() as Calendar).apply {
        firstDayOfWeek = Calendar.SUNDAY
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }
}

/**
 * 获取给定日期所在周的所有日期
 */
private fun getWeekDates(weekStart: Calendar): List<Calendar> {
    return (0..6).map { dayOffset ->
        val dayCalendar = weekStart.clone() as Calendar
        dayCalendar.add(Calendar.DAY_OF_MONTH, dayOffset)
        dayCalendar
    }
}

/**
 * 判断两个日期是否在同一周
 */
private fun isInSameWeek(date1: Calendar, date2: Calendar): Boolean {
    val week1Start = getWeekStart(date1)
    val week2Start = getWeekStart(date2)

    return week1Start.get(Calendar.YEAR) == week2Start.get(Calendar.YEAR) &&
           week1Start.get(Calendar.WEEK_OF_YEAR) == week2Start.get(Calendar.WEEK_OF_YEAR)
}

/**
 * 单个日期单元格
 */
@Composable
private fun DateCell(
    date: Calendar,
    isSelected: Boolean,
    isToday: Boolean
) {
    // 农历示例数据（实际应该根据日期计算真实农历）
    val lunarDays = listOf("初一", "初二", "初三", "初四", "初五", "初六", "初七",
                          "初八", "初九", "初十", "十一", "十二", "十三", "十四",
                          "十五", "十六", "十七", "十八", "十九", "二十", "廿一",
                          "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八",
                          "廿九", "三十")
    // 简单示例：用阳历日期作为索引（实际应该使用农历库）
    val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
    val lunarDay = if (dayOfMonth <= lunarDays.size) lunarDays[dayOfMonth - 1] else "初${dayOfMonth}"

    // 调试日志
    Log.d("DateCell", "Rendering date: $dayOfMonth, lunar: $lunarDay, isSelected: $isSelected, isToday: $isToday")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 日期数字
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(
                    if (isToday && isSelected) {
                        Modifier.background(
                            color = Color(0xFFFF3B30),
                            shape = CircleShape
                        )
                    } else if (isToday) {
                        Modifier // 今天但未选中，只显示红色数字
                    } else if (isSelected) {
                        Modifier.background(
                            color = Color.Black,
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.get(Calendar.DAY_OF_MONTH).toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = when {
                    isSelected && isToday -> Color.White
                    isSelected -> Color.White
                    isToday -> Color(0xFFFF3B30)
                    else -> Color.Black
                }
            )
        }
        // 农历或其他标签
        Text(
            text = lunarDay,
            fontSize = 10.sp,
            color = when {
                isSelected && isToday -> Color(0xFFFF3B30)
                isToday -> Color(0xFFFF3B30)
                else -> Color(0xFF8E8E93)
            }
        )
    }
}

/**
 * 格式化选中日期的详细信息
 */
private fun formatSelectedDateDetail(date: Calendar): String {
    val dayFormat = SimpleDateFormat("yyyy年M月d日 — 周", Locale.CHINESE)
    val weekDays = arrayOf("日", "一", "二", "三", "四", "五", "六")
    val weekDay = weekDays[date.get(Calendar.DAY_OF_WEEK) - 1]

    // 农历信息（示例）
    val lunarInfo = "乙巳年八月廿二"

    return "${dayFormat.format(date.time)}$weekDay $lunarInfo"
}

