package com.jarvis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

/**
 * Canvas版本的月视图日期选择器
 */
@Composable
fun CanvasMonthDateSelector(
    selectedDate: Calendar,
    onDateChange: (Calendar) -> Unit,
    onPinchToWeekView: () -> Unit,  // 捏合切换到周视图
    modifier: Modifier = Modifier
) {
    // 当前显示的月份
    var currentMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    // 当selectedDate变化时，如果不在当前月份，更新currentMonth
    LaunchedEffect(selectedDate) {
        if (selectedDate.get(Calendar.YEAR) != currentMonth.get(Calendar.YEAR) ||
            selectedDate.get(Calendar.MONTH) != currentMonth.get(Calendar.MONTH)) {
            currentMonth = Calendar.getInstance().apply {
                time = selectedDate.time
                set(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }

    // 今天的日期
    val today = remember { Calendar.getInstance() }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val horizontalPadding = 16.dp
    val screenWidth = configuration.screenWidthDp.dp
    val actualContentWidth = screenWidth - (horizontalPadding * 2)
    val actualContentWidthPx = with(density) { actualContentWidth.toPx() }
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 月份标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = SimpleDateFormat("yyyy年MM月", Locale.CHINESE).format(currentMonth.time),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

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

        // Canvas月历
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)  // 月视图需要更多高度
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        // 检测捏合手势
                        if (zoom > 1.2f) {
                            // 放大手势，切换到周视图
                            onPinchToWeekView()
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // 计算点击的日期
                        val dayWidth = actualContentWidthPx / 7f
                        val dayHeight = size.height / 6f  // 最多6行

                        val col = ((offset.x - horizontalPaddingPx) / dayWidth).toInt()
                        val row = (offset.y / dayHeight).toInt()

                        if (col in 0..6 && row in 0..5) {
                            val dayIndex = row * 7 + col
                            val dates = getMonthDates(currentMonth)
                            if (dayIndex < dates.size) {
                                val clickedDate = dates[dayIndex]
                                // 只响应当前月份的日期
                                if (clickedDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)) {
                                    onDateChange(clickedDate)
                                }
                            }
                        }
                    }
                }
        ) {
            drawMonth(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                today = today,
                dayWidth = actualContentWidthPx / 7f,
                horizontalPadding = horizontalPaddingPx
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 日期详情行
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
 * 绘制月历
 */
private fun DrawScope.drawMonth(
    currentMonth: Calendar,
    selectedDate: Calendar,
    today: Calendar,
    dayWidth: Float,
    horizontalPadding: Float
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val dates = getMonthDates(currentMonth)
        val rowHeight = size.height / 6f  // 最多6行

        dates.forEachIndexed { index, date ->
            val col = index % 7
            val row = index / 7

            val isSelected = isSameDay(date, selectedDate)
            val isToday = isSameDay(date, today)
            val isCurrentMonth = date.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)

            // 计算日期的中心位置
            val centerX = horizontalPadding + col * dayWidth + dayWidth / 2
            val centerY = row * rowHeight + rowHeight / 2

            // 绘制选中背景
            if (isSelected && isCurrentMonth) {
                paint.color = if (isToday) Color(0xFFFF3B30).toArgb() else Color.Black.toArgb()
                canvas.nativeCanvas.drawCircle(
                    centerX,
                    centerY - 5,
                    36f,
                    paint
                )
            }

            // 绘制日期数字
            paint.textSize = 48f
            paint.isFakeBoldText = false
            paint.color = when {
                !isCurrentMonth -> Color(0xFFD1D1D6).toArgb()  // 非当前月份的日期显示为灰色
                isSelected -> Color.White.toArgb()
                isToday -> Color(0xFFFF3B30).toArgb()
                else -> Color.Black.toArgb()
            }

            val dayText = date.get(Calendar.DAY_OF_MONTH).toString()
            canvas.nativeCanvas.drawText(
                dayText,
                centerX,
                centerY + 5,
                paint
            )
        }
    }
}

/**
 * 获取月视图所需的所有日期（包括前后月份的日期以填满6x7网格）
 */
private fun getMonthDates(month: Calendar): List<Calendar> {
    val dates = mutableListOf<Calendar>()

    val firstDay = (month.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // 获取这个月第一天是星期几
    val firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) - 1  // 0=周日

    // 添加上个月的日期
    for (i in firstDayOfWeek - 1 downTo 0) {
        val date = (firstDay.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -i - 1)
        }
        dates.add(date)
    }

    // 添加这个月的所有日期
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in 1..daysInMonth) {
        val date = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, i)
        }
        dates.add(date)
    }

    // 添加下个月的日期以填满6行
    while (dates.size < 42) {  // 6行 x 7列 = 42
        val lastDate = dates.last()
        val nextDate = (lastDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        dates.add(nextDate)
    }

    return dates
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