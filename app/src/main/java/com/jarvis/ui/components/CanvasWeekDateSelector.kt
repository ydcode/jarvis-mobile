package com.jarvis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Canvas版本的周视图日期选择器
 */
@Composable
fun CanvasWeekDateSelector(
    selectedDate: Calendar,
    onDateChange: (Calendar) -> Unit,
    onPinchToMonthView: () -> Unit,  // 添加切换到月视图的回调
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

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // 计算实际的日期区域宽度
    val horizontalPadding = 16.dp
    val screenWidth = configuration.screenWidthDp.dp
    val actualContentWidth = screenWidth - (horizontalPadding * 2)
    val actualContentWidthPx = with(density) { actualContentWidth.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 星期标签行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 4.dp)
        ) {
            val weekLabels = listOf("日", "一", "二", "三", "四", "五", "六")
            weekLabels.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Canvas日期选择器
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .pointerInput(Unit) {
                    // 检测双指捏合手势
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom < 0.8f) {
                            // 缩小手势，切换到月视图
                            onPinchToMonthView()
                        }
                    }
                }
                .pointerInput(Unit) {  // 使用Unit作为key，不依赖其他状态
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            coroutineScope.launch {
                                val currentOffset = dragOffsetX.value
                                val threshold = actualContentWidthPx * (0.4f / 7f)  // 0.4天的距离，非常灵敏

                                when {
                                    currentOffset < -threshold -> {
                                        // 向左滑动，切换到下一周
                                        // 先更新周数据
                                        currentWeekStart = (currentWeekStart.clone() as Calendar).apply {
                                            add(Calendar.WEEK_OF_YEAR, 1)
                                        }
                                        // 立即重置偏移（不用动画）
                                        dragOffsetX.snapTo(currentOffset + actualContentWidthPx)
                                        // 然后动画回到0
                                        dragOffsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.8f,
                                                stiffness = 300f
                                            )
                                        )
                                    }
                                    currentOffset > threshold -> {
                                        // 向右滑动，切换到上一周
                                        // 先更新周数据
                                        currentWeekStart = (currentWeekStart.clone() as Calendar).apply {
                                            add(Calendar.WEEK_OF_YEAR, -1)
                                        }
                                        // 立即重置偏移（不用动画）
                                        dragOffsetX.snapTo(currentOffset - actualContentWidthPx)
                                        // 然后动画回到0
                                        dragOffsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.8f,
                                                stiffness = 300f
                                            )
                                        )
                                    }
                                    else -> {
                                        // 回弹到原位
                                        dragOffsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.75f,
                                                stiffness = 350f
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (isDragging) {
                                coroutineScope.launch {
                                    dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {  // 同样使用Unit作为key
                    detectTapGestures { offset ->
                        // 计算点击了哪一天
                        val dayWidth = actualContentWidthPx / 7f
                        val clickedDay = ((offset.x - horizontalPaddingPx) / dayWidth).toInt()
                        if (clickedDay in 0..6) {
                            val clickedDate = (currentWeekStart.clone() as Calendar).apply {
                                add(Calendar.DAY_OF_MONTH, clickedDay)
                            }
                            onDateChange(clickedDate)
                        }
                    }
                }
        ) {
            val offsetX = dragOffsetX.value

            // 绘制更多周以确保流畅切换：前10周、当前周、后10周
            // 只绘制可见范围附近的周以优化性能
            for (weekOffset in -10..10) {
                // 计算这一周的x偏移
                val weekOffsetX = weekOffset * actualContentWidthPx + offsetX

                // 只绘制在可见范围内或即将进入可见范围的周（优化性能）
                if (weekOffsetX > -actualContentWidthPx * 2 && weekOffsetX < size.width + actualContentWidthPx) {
                    val weekStart = (currentWeekStart.clone() as Calendar).apply {
                        add(Calendar.WEEK_OF_YEAR, weekOffset)
                    }
                    val weekDates = getWeekDates(weekStart)

                    // 绘制这一周的日期
                    drawWeek(
                        weekDates = weekDates,
                        selectedDate = selectedDate,
                        today = today,
                        offsetX = weekOffsetX,
                        dayWidth = actualContentWidthPx / 7f,
                        horizontalPadding = horizontalPaddingPx
                    )
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
 * 绘制一周的日期
 */
private fun DrawScope.drawWeek(
    weekDates: List<Calendar>,
    selectedDate: Calendar,
    today: Calendar,
    offsetX: Float,
    dayWidth: Float,
    horizontalPadding: Float
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // 农历数据
        val lunarDays = listOf("初一", "初二", "初三", "初四", "初五", "初六", "初七",
                              "初八", "初九", "初十", "十一", "十二", "十三", "十四",
                              "十五", "十六", "十七", "十八", "十九", "二十", "廿一",
                              "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八",
                              "廿九", "三十")

        weekDates.forEachIndexed { index, date ->
            val isSelected = isSameDay(date, selectedDate)
            val isToday = isSameDay(date, today)

            // 计算日期的中心位置
            val centerX = horizontalPadding + offsetX + index * dayWidth + dayWidth / 2
            val centerY = size.height / 2

            // 如果这一天在可见范围内才绘制
            if (centerX > -dayWidth && centerX < size.width + dayWidth) {
                // 绘制选中背景
                if (isSelected || isToday) {
                    paint.color = when {
                        isSelected && isToday -> Color(0xFFFF3B30).toArgb()
                        isSelected -> Color.Black.toArgb()
                        isToday -> Color(0xFFFF3B30).toArgb()
                        else -> Color.Transparent.toArgb()
                    }

                    if (isSelected) {
                        // 绘制圆形背景
                        canvas.nativeCanvas.drawCircle(
                            centerX,
                            centerY - 5,
                            36f,
                            paint
                        )
                    }
                }

                // 绘制日期数字
                paint.textSize = 54f // 20sp
                paint.isFakeBoldText = false
                paint.color = when {
                    isSelected -> Color.White.toArgb()
                    isToday -> Color(0xFFFF3B30).toArgb()
                    else -> Color.Black.toArgb()
                }

                val dayText = date.get(Calendar.DAY_OF_MONTH).toString()
                canvas.nativeCanvas.drawText(
                    dayText,
                    centerX,
                    centerY,
                    paint
                )

                // 绘制农历
                val dayOfMonth = date.get(Calendar.DAY_OF_MONTH)
                val lunarDay = if (dayOfMonth <= lunarDays.size) lunarDays[dayOfMonth - 1] else "初${dayOfMonth}"

                paint.textSize = 27f // 10sp
                paint.isFakeBoldText = false
                paint.color = when {
                    isSelected && isToday -> Color(0xFFFF3B30).toArgb()
                    isToday -> Color(0xFFFF3B30).toArgb()
                    else -> Color(0xFF8E8E93).toArgb()
                }

                canvas.nativeCanvas.drawText(
                    lunarDay,
                    centerX,
                    centerY + 35,
                    paint
                )
            }
        }
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