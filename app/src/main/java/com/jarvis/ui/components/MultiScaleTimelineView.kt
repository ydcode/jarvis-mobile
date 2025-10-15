package com.jarvis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 时间尺度枚举 - 从小时到10亿年
 */
enum class TimeScale(
    val displayName: String,
    val millisecondsPerUnit: Long,
    val unitsPerMajorTick: Int,
    val minorTicksPerMajorTick: Int
) {
    HOUR("小时", 3600_000L, 6, 6),           // 主刻度：6小时，次刻度：每小时
    DAY("天", 86400_000L, 7, 24),             // 主刻度：7天（一周），次刻度：每天
    WEEK("周", 604800_000L, 4, 7),            // 主刻度：4周（约一月），次刻度：每周
    MONTH("月", 2592000_000L, 12, 4),         // 主刻度：12个月（一年），次刻度：每月（约）
    YEAR("年", 31536000_000L, 10, 12),        // 主刻度：10年，次刻度：每年
    DECADE("10年", 315360000_000L, 10, 10),   // 主刻度：100年，次刻度：每10年
    CENTURY("100年", 3153600000_000L, 10, 10), // 主刻度：1000年，次刻度：每100年
    MILLENNIUM("1000年", 31536000000L * 1000, 10, 10), // 主刻度：10000年，次刻度：每1000年
    TEN_THOUSAND_YEARS("万年", 31536000000L * 10000, 10, 10), // 主刻度：10万年
    HUNDRED_THOUSAND_YEARS("10万年", 31536000000L * 100000, 10, 10), // 主刻度：100万年
    MILLION_YEARS("100万年", 31536000000L * 1000000, 10, 10), // 主刻度：1000万年
    TEN_MILLION_YEARS("1000万年", 31536000000L * 10000000, 10, 10), // 主刻度：1亿年
    HUNDRED_MILLION_YEARS("亿年", 31536000000L * 100000000, 10, 10), // 主刻度：10亿年
    BILLION_YEARS("10亿年", Long.MAX_VALUE, 10, 10); // 最大刻度（使用Long.MAX_VALUE避免溢出）

    /**
     * 获取下一个更大的时间尺度
     */
    fun zoomOut(): TimeScale? {
        val nextOrdinal = ordinal + 1
        return if (nextOrdinal < entries.size) entries[nextOrdinal] else null
    }

    /**
     * 获取上一个更小的时间尺度
     */
    fun zoomIn(): TimeScale? {
        val prevOrdinal = ordinal - 1
        return if (prevOrdinal >= 0) entries[prevOrdinal] else null
    }

    companion object {
        fun fromOrdinal(ordinal: Int): TimeScale {
            return entries.getOrNull(ordinal) ?: HOUR
        }
    }
}

/**
 * 多尺度时间轴视图
 * 支持从小时到10亿年的连续缩放
 */
@Composable
fun MultiScaleTimelineView(
    selectedDate: Calendar = Calendar.getInstance(),
    modifier: Modifier = Modifier
) {
    // 当前时间尺度
    var currentScale by remember { mutableStateOf(TimeScale.HOUR) }

    // 时间轴的中心时间点（毫秒）
    var centerTimeMillis by remember { mutableStateOf(selectedDate.timeInMillis) }

    // 缩放动画状态（0.0 到 1.0，用于刻度切换动画）
    val zoomProgress = remember { Animatable(0f) }

    // 水平滚动偏移
    val scrollOffset = remember { Animatable(0f) }

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidth = configuration.screenWidthDp.dp
    val screenWidthPx = with(density) { screenWidth.toPx() }

    // 捏合手势检测
    var lastZoom by remember { mutableStateOf(1f) }
    var accumulatedZoom by remember { mutableStateOf(1f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 时间轴画布
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        // 处理捏合缩放
                        accumulatedZoom *= zoom

                        // 缩小手势（zoom out）- 切换到更大的时间尺度
                        if (accumulatedZoom < 0.7f) {
                            currentScale.zoomOut()?.let { newScale ->
                                currentScale = newScale
                                accumulatedZoom = 1f

                                // 触发缩放动画
                                coroutineScope.launch {
                                    zoomProgress.snapTo(0f)
                                    zoomProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = 300f
                                        )
                                    )
                                    zoomProgress.snapTo(0f)
                                }
                            }
                        }
                        // 放大手势（zoom in）- 切换到更小的时间尺度
                        else if (accumulatedZoom > 1.3f) {
                            currentScale.zoomIn()?.let { newScale ->
                                currentScale = newScale
                                accumulatedZoom = 1f

                                // 触发缩放动画
                                coroutineScope.launch {
                                    zoomProgress.snapTo(0f)
                                    zoomProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = 300f
                                        )
                                    )
                                    zoomProgress.snapTo(0f)
                                }
                            }
                        }

                        // 处理平移
                        coroutineScope.launch {
                            // 横向平移转换为时间偏移
                            val timeOffset = (pan.x / screenWidthPx) *
                                (currentScale.millisecondsPerUnit * currentScale.unitsPerMajorTick)
                            centerTimeMillis -= timeOffset.toLong()
                        }
                    }
                }
        ) {
            drawTimeline(
                scale = currentScale,
                centerTimeMillis = centerTimeMillis,
                screenWidthPx = size.width,
                screenHeightPx = size.height,
                zoomProgress = zoomProgress.value
            )
        }
    }
}

/**
 * 绘制时间轴
 */
private fun DrawScope.drawTimeline(
    scale: TimeScale,
    centerTimeMillis: Long,
    screenWidthPx: Float,
    screenHeightPx: Float,
    zoomProgress: Float
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        // 绘制中心线
        paint.color = Color(0xFFFF3B30).toArgb()
        paint.strokeWidth = 4f
        canvas.nativeCanvas.drawLine(
            screenWidthPx / 2,
            0f,
            screenWidthPx / 2,
            screenHeightPx,
            paint
        )

        // 计算可见范围内的刻度
        val pixelsPerUnit = screenWidthPx / (scale.unitsPerMajorTick * 2f)
        val centerUnitIndex = (centerTimeMillis / scale.millisecondsPerUnit).toDouble()

        // 绘制主刻度和次刻度
        val visibleUnits = (screenWidthPx / pixelsPerUnit).toInt() + 2

        for (i in -visibleUnits..visibleUnits) {
            val unitIndex = centerUnitIndex + i
            val unitTimeMillis = (unitIndex * scale.millisecondsPerUnit).toLong()
            val offsetFromCenter = (unitIndex - centerUnitIndex) * pixelsPerUnit
            val x = screenWidthPx / 2 + offsetFromCenter.toFloat()

            // 只绘制在可见范围内的刻度
            if (x >= 0 && x <= screenWidthPx) {
                // 判断是否为主刻度
                val isMajorTick = unitIndex.toLong() % scale.unitsPerMajorTick == 0L

                // 绘制刻度线
                paint.color = if (isMajorTick) Color.Black.toArgb() else Color(0xFFE5E5E7).toArgb()
                paint.strokeWidth = if (isMajorTick) 2f else 1f

                val tickHeight = if (isMajorTick) 80f else 40f
                canvas.nativeCanvas.drawLine(
                    x,
                    screenHeightPx / 2 - tickHeight / 2,
                    x,
                    screenHeightPx / 2 + tickHeight / 2,
                    paint
                )

                // 绘制主刻度标签
                if (isMajorTick) {
                    paint.textSize = 36f
                    paint.color = Color.Black.toArgb()
                    paint.textAlign = android.graphics.Paint.Align.CENTER

                    val label = formatTimeLabel(unitTimeMillis, scale)
                    canvas.nativeCanvas.drawText(
                        label,
                        x,
                        screenHeightPx / 2 + 120f,
                        paint
                    )
                }
            }
        }

        // 绘制当前尺度标识
        paint.textSize = 48f
        paint.color = Color(0xFF007AFF).toArgb()
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.nativeCanvas.drawText(
            "当前刻度: ${scale.displayName}",
            screenWidthPx / 2,
            100f,
            paint
        )
    }
}

/**
 * 格式化时间标签
 */
private fun formatTimeLabel(timeMillis: Long, scale: TimeScale): String {
    val calendar = Calendar.getInstance().apply {
        this.timeInMillis = timeMillis
    }

    return when (scale) {
        TimeScale.HOUR -> {
            SimpleDateFormat("HH:00", Locale.getDefault()).format(calendar.time)
        }
        TimeScale.DAY -> {
            SimpleDateFormat("M/d", Locale.getDefault()).format(calendar.time)
        }
        TimeScale.WEEK -> {
            val weekNum = calendar.get(Calendar.WEEK_OF_YEAR)
            "第${weekNum}周"
        }
        TimeScale.MONTH -> {
            SimpleDateFormat("yyyy/M", Locale.getDefault()).format(calendar.time)
        }
        TimeScale.YEAR -> {
            calendar.get(Calendar.YEAR).toString()
        }
        TimeScale.DECADE -> {
            val decade = (calendar.get(Calendar.YEAR) / 10) * 10
            "${decade}年代"
        }
        TimeScale.CENTURY -> {
            val century = (calendar.get(Calendar.YEAR) / 100) * 100
            "${century}年"
        }
        TimeScale.MILLENNIUM -> {
            val millennium = (calendar.get(Calendar.YEAR) / 1000) * 1000
            "${millennium}年"
        }
        TimeScale.TEN_THOUSAND_YEARS -> {
            val tenThousand = (calendar.get(Calendar.YEAR) / 10000) * 10000
            "${tenThousand}年"
        }
        TimeScale.HUNDRED_THOUSAND_YEARS -> {
            val hundredThousand = (calendar.get(Calendar.YEAR) / 100000) * 100000
            "${hundredThousand / 10000}万年"
        }
        TimeScale.MILLION_YEARS -> {
            val million = (calendar.get(Calendar.YEAR) / 1000000) * 1000000
            "${million / 10000}万年"
        }
        TimeScale.TEN_MILLION_YEARS -> {
            val tenMillion = (calendar.get(Calendar.YEAR) / 10000000) * 10000000
            "${tenMillion / 10000}万年"
        }
        TimeScale.HUNDRED_MILLION_YEARS -> {
            val hundredMillion = (calendar.get(Calendar.YEAR) / 100000000) * 100000000
            "${hundredMillion / 100000000}亿年"
        }
        TimeScale.BILLION_YEARS -> {
            val billion = (calendar.get(Calendar.YEAR) / 1000000000) * 1000000000
            "${billion / 100000000}亿年"
        }
    }
}
