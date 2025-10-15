package com.jarvis.ui.components

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.data.model.CalendarEvent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 使用 Canvas 直接绘制的时间线视图
 * 实现类似苹果日历的丝滑缩放体验
 * 支持从分钟到100年的连续缩放
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun CanvasTimelineView(
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier,
    scrollToNowTrigger: Long = 0L  // 触发滚动到当前时间，每次变化时执行
) {
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val textMeasurer = rememberTextMeasurer()
    val coroutineScope = rememberCoroutineScope()

    // Android标准TouchSlop
    val touchSlopPx = remember {
        android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    }

    // 核心状态：统一的单位高度（dp）- 适用于所有尺度
    // 每个时间单位的高度，单位根据当前尺度而定
    var unitHeightDp by remember { mutableStateOf(60.dp) }

    // 当前时间尺度级别
    var currentTimeScale by remember { mutableStateOf(TimeScaleLevel.HOUR) }

    // 滚动偏移量（dp）
    var scrollOffsetDp by remember { mutableStateOf(0.dp) }

    // 调试信息
    var debugInfo by remember { mutableStateOf("等待缩放...") }

    // 中心点日期（用于调试） - 使用 mutableStateOf 确保 UI 更新
    var centerDateText by remember { mutableStateOf("--") }

    // 触点调试信息
    var touchPointsInfo by remember { mutableStateOf<List<Pair<Offset, String>>>(emptyList()) }
    var centroidInfo by remember { mutableStateOf<Pair<Offset, String>?>(null) }

    // 强制重绘触发器 - 每次手势事件都更新以触发Canvas重绘
    var updateTrigger by remember { mutableStateOf(0L) }

    // Fling scrolling state
    val velocityTracker = remember { VelocityTracker() }
    val flingAnimation = remember { Animatable(0f) }
    var isSingleFingerDragging by remember { mutableStateOf(false) }
    var shouldCancelFling by remember { mutableStateOf(false) }  // 标志：是否应该取消fling

    // Track drag distance and time for momentum calculation
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableStateOf(0f) }

    // Single-finger scroll sensitivity multiplier (0.5x for controlled scrolling)
    val singleFingerSensitivity = 0.5f
    // Velocity boost multiplier for iOS call log-like fling effect (1x for subtle inertia)
    val velocityBoost = 1.0f

    // Apple Calendar 风格的颜色
    val lineColor = Color(0xFF8E8E93)  // 使用更深的灰色，原来的D1D1D6太浅了
    val textColor = Color(0xFF8E8E93)  // iOS 次要文字颜色
    val currentTimeColor = Color(0xFFFF3B30)  // iOS 红色
    val backgroundColor = Color.White  // 白色背景

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val screenHeightDp = maxHeight

        // 监听scrollToNowTrigger，当它变化时平滑滚动到当前时间
        LaunchedEffect(scrollToNowTrigger) {
            if (scrollToNowTrigger > 0L) {
                // 计算当前时间在当前刻度下的units
                val now = Calendar.getInstance()
                val nowUnits = calendarToScaleUnits(currentTimeScale, now, Calendar.getInstance())

                // 计算屏幕的五分之一位置（dp），从上往下1/5的位置
                val targetPositionDp = screenHeightDp / 5f

                // 计算需要的scrollOffset，让当前时间出现在屏幕的1/5位置
                val targetScrollOffset = nowUnits * unitHeightDp.value - targetPositionDp.value

                Log.d("ScrollToNow", "Starting smooth scroll to now: scale=${currentTimeScale.displayName}, nowUnits=$nowUnits, targetPos=${targetPositionDp.value}dp (1/5), from=${scrollOffsetDp.value}dp to=${targetScrollOffset}dp")

                // 使用Animatable实现平滑滚动
                val scrollAnimatable = Animatable(scrollOffsetDp.value)
                scrollAnimatable.animateTo(
                    targetValue = targetScrollOffset,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 600,  // 600ms的平滑动画
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ) {
                    // 动画的每一帧都更新scrollOffset
                    scrollOffsetDp = value.dp
                    // 移除updateTrigger更新 - scrollOffsetDp变化会自动触发Canvas重绘
                }

                Log.d("ScrollToNow", "Smooth scroll completed")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 注释掉触点可视化 - 单指点击时清空touchPointsInfo/centroidInfo会触发不必要的Canvas重绘
                /*
                .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        // 捕获所有触点位置
                        val touches = event.changes.map { it.position }

                        if (touches.size >= 2) {
                            // 统一的时间格式：精确到秒
                            val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                            // 计算每个触点对应的时间
                            val touchInfos = touches.map { touch ->
                                val touchYDp = touch.y / density.density
                                val touchUnits = (scrollOffsetDp.value + touchYDp) / unitHeightDp.value

                                if (currentTimeScale >= TimeScaleLevel.DECADE) {
                                    Log.e("TOUCH_INPUT", "触点Y像素: ${touch.y}, Y(dp): $touchYDp")
                                    Log.e("TOUCH_INPUT", "滚动偏移(dp): ${scrollOffsetDp.value}")
                                    Log.e("TOUCH_INPUT", "单位高度(dp): ${unitHeightDp.value}")
                                    Log.e("TOUCH_INPUT", "计算的units: $touchUnits")
                                }

                                val cal = Calendar.getInstance()
                                val touchTime = scaleUnitsToCalendar(currentTimeScale, touchUnits, cal)
                                val timeStr = timeFormatter.format(touchTime.time)
                                touch to timeStr
                            }
                            touchPointsInfo = touchInfos

                            // 计算几何中心
                            val centroidX = touches.map { it.x }.average().toFloat()
                            val centroidY = touches.map { it.y }.average().toFloat()
                            val centroid = Offset(centroidX, centroidY)

                            val centroidYDp = centroidY / density.density
                            val centroidUnits = (scrollOffsetDp.value + centroidYDp) / unitHeightDp.value
                            val cal = Calendar.getInstance()
                            val centroidTime = scaleUnitsToCalendar(currentTimeScale, centroidUnits, cal)
                            val centroidTimeStr = timeFormatter.format(centroidTime.time)
                            centroidInfo = centroid to centroidTimeStr

                            // 日志输出触点信息
                            Log.e("TouchPoints", "====== 触点可视化数据 (${currentTimeScale.displayName}) ======")
                            Log.e("TouchPoints", "触点数量: ${touchInfos.size}")
                            touchInfos.forEachIndexed { index, (pos, time) ->
                                Log.e("TouchPoints", "触点${index + 1}: 位置=(${pos.x}, ${pos.y}), 时间=$time")
                            }
                            Log.e("TouchPoints", "中心点: 位置=(${centroid.x}, ${centroid.y}), 时间=$centroidTimeStr")
                            Log.e("TouchPoints", "============================")
                        } else {
                            touchPointsInfo = emptyList()
                            centroidInfo = null
                        }
                    }
                }
            }
            */
            .pointerInput(Unit) {
                // Single-finger drag with fling support
                awaitPointerEventScope {
                    while (true) {
                        // Wait for first finger down
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // 立即设置标志要求取消fling动画
                        // fling动画在下一帧检查时会立即退出
                        Log.e("FlingStop", "Finger down detected, requesting fling cancellation...")
                        shouldCancelFling = true
                        Log.e("FlingStop", "Fling cancellation flag set")

                        // Check if this is the start of a single-finger gesture
                        var currentEvent = awaitPointerEvent()

                        // If more than one finger, skip and let detectTransformGestures handle it
                        if (currentEvent.changes.size > 1) {
                            continue
                        }

                        // Single finger detected - start tracking
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        // Reset drag tracking
                        dragStartTime = System.currentTimeMillis()
                        totalDragDistance = 0f

                        var lastPosition = down.position
                        val startPosition = down.position

                        // 使用Android标准的TouchSlop来区分点击和拖动
                        var isDragging = false

                        // 追踪最大移动距离，用于调试
                        var maxDistanceFromStart = 0f
                        var firstMoveDetectedTime = 0L  // 记录第一次检测到移动的时间

                        Log.e("TouchSlop", "System TouchSlop: ${touchSlopPx}px (${touchSlopPx / density.density}dp)")

                        // Track drag until finger is lifted
                        do {
                            currentEvent = awaitPointerEvent()

                            // If a second finger comes down, stop tracking and let transform handle it
                            if (currentEvent.changes.size > 1) {
                                isSingleFingerDragging = false
                                break
                            }

                            val change = currentEvent.changes.firstOrNull() ?: break

                            if (change.pressed) {
                                val currentPosition = change.position
                                val touchDuration = System.currentTimeMillis() - dragStartTime

                                // Track velocity for all movements (needed for tap vs drag detection)
                                velocityTracker.addPosition(change.uptimeMillis, change.position)

                                // 计算从起始位置的移动距离（用于调试）
                                val moveFromStart = currentPosition - startPosition
                                val distanceFromStart = kotlin.math.sqrt(moveFromStart.x * moveFromStart.x + moveFromStart.y * moveFromStart.y)

                                // 更新最大距离
                                if (distanceFromStart > maxDistanceFromStart) {
                                    maxDistanceFromStart = distanceFromStart
                                    if (firstMoveDetectedTime == 0L && distanceFromStart > 10f) {
                                        firstMoveDetectedTime = System.currentTimeMillis()
                                        Log.e("TouchTracking", "First significant move detected: ${distanceFromStart}px after ${touchDuration}ms")
                                    }
                                }

                                // Android标准逻辑：超过TouchSlop距离就开始拖动
                                if (!isDragging) {
                                    if (distanceFromStart > touchSlopPx) {
                                        isDragging = true
                                        isSingleFingerDragging = true
                                        lastPosition = startPosition

                                        Log.e("TouchTracking", "✓ Drag STARTED after ${touchDuration}ms, distance: ${distanceFromStart}px > touchSlop: ${touchSlopPx}px")
                                    }
                                }

                                // 只有在确认拖动后才应用滚动
                                if (isDragging) {
                                    val dragAmount = currentPosition - lastPosition

                                    // Accumulate total drag distance (absolute value)
                                    totalDragDistance += kotlin.math.abs(dragAmount.y)

                                    // 计算当前速度，判断是否为真实拖动
                                    val currentVelocity = velocityTracker.calculateVelocity()
                                    val currentVelocityY = kotlin.math.abs(currentVelocity.y)

                                    // 关键优化：只有当速度达到一定阈值时才真正滚动屏幕
                                    // 这样可以避免"点击时手指肉垫滚动"导致的抖动
                                    // 真正的拖动意图会产生持续的速度，而点击时的手指偏移速度很低
                                    if (currentVelocityY > 300f) {
                                        // 确认是真实拖动，应用滚动
                                        val sensitiveOffset = dragAmount.y * singleFingerSensitivity
                                        val newOffset = scrollOffsetDp.value - sensitiveOffset / density.density
                                        Log.e("ScrollOffset", "[DRAG] duration=${touchDuration}ms, velocity=${currentVelocityY}px/s, old=${scrollOffsetDp.value}dp, dragY=${dragAmount.y}px, new=${newOffset}dp")
                                        scrollOffsetDp = newOffset.dp
                                        change.consume()
                                    } else {
                                        // 速度太低，可能是点击，暂时不滚动
                                        Log.e("ScrollOffset", "[PENDING] duration=${touchDuration}ms, velocity=${currentVelocityY}px/s < 300px/s, totalDistance=${totalDragDistance}px, NOT SCROLLING YET")
                                    }

                                    // 更新 lastPosition
                                    lastPosition = currentPosition
                                }
                            }
                        } while (currentEvent.changes.any { it.pressed })

                        // 计算总触摸时间
                        val totalTouchDuration = System.currentTimeMillis() - dragStartTime

                        // 计算释放时的速度
                        val releaseVelocity = velocityTracker.calculateVelocity()
                        val releaseVelocityY = kotlin.math.abs(releaseVelocity.y)

                        // 日志：记录触摸事件
                        if (!isDragging) {
                            if (maxDistanceFromStart > 0f) {
                                Log.e("TapDebug", "✓ TAP FILTERED: duration=${totalTouchDuration}ms, distance=${maxDistanceFromStart}px < touchSlop=${touchSlopPx}px, releaseVelocity=${releaseVelocityY}px/s, scrollOffset=${scrollOffsetDp.value}dp (NO SCROLL)")
                            }
                        } else {
                            Log.e("TapDebug", "✗ DRAG COMPLETED: duration=${totalTouchDuration}ms, distance=${maxDistanceFromStart}px > touchSlop=${touchSlopPx}px, releaseVelocity=${releaseVelocityY}px/s, totalDragDistance=${totalDragDistance}px")
                        }

                        // Drag ended with single finger - trigger fling
                        if (isSingleFingerDragging) {
                            isSingleFingerDragging = false

                            // Calculate drag duration and average speed
                            val dragDuration = System.currentTimeMillis() - dragStartTime
                            val dragDurationSeconds = dragDuration / 1000f

                            // Get instantaneous velocity from VelocityTracker
                            val velocity = velocityTracker.calculateVelocity()
                            val instantVelocityY = velocity.y

                            // Calculate average velocity from total distance and time
                            val averageVelocity = if (dragDurationSeconds > 0) {
                                totalDragDistance / dragDurationSeconds
                            } else {
                                0f
                            }

                            // Log velocity for debugging
                            Log.e("FlingVelocity", "===== FLING CALCULATION =====")
                            Log.e("FlingVelocity", "Drag distance: ${totalDragDistance}px")
                            Log.e("FlingVelocity", "Drag duration: ${dragDuration}ms (${dragDurationSeconds}s)")
                            Log.e("FlingVelocity", "Instant velocity: $instantVelocityY px/s")
                            Log.e("FlingVelocity", "Average velocity: $averageVelocity px/s")

                            // 关键修复：只基于瞬时速度判断是否触发fling
                            // 不使用平均速度，因为点击时手指滚动会产生很大的平均速度，但瞬时速度为0
                            // 这样可以完全避免点击时触发fling
                            // Only fling if INSTANTANEOUS velocity is significant (>300 px/s, same threshold as scrolling gate)
                            if (kotlin.math.abs(instantVelocityY) > 300f) {
                                coroutineScope.launch {
                                    // 清除取消标志，开始新的fling
                                    shouldCancelFling = false

                                    flingAnimation.snapTo(0f)

                                    // 只使用瞬时速度，不混合平均速度
                                    val boostedVelocity = -instantVelocityY * singleFingerSensitivity * velocityBoost
                                    Log.e("FlingVelocity", "Starting fling animation with INSTANT velocity: $boostedVelocity, scrollOffset before: ${scrollOffsetDp.value}")

                                    // iOS call log-like fling effect:
                                    // - 8x velocity boost for balanced inertia
                                    // - Moderate friction (0.9f) for natural deceleration
                                    // - 2x scroll sensitivity for responsive feel
                                    // exponentialDecay provides natural deceleration that feels like iOS
                                    var frameCount = 0
                                    var lastValue = 0f
                                    try {
                                        flingAnimation.animateDecay(
                                            initialVelocity = boostedVelocity,
                                            animationSpec = exponentialDecay(frictionMultiplier = 0.9f)
                                        ) {
                                            // 检查是否应该取消fling
                                            if (shouldCancelFling) {
                                                Log.e("FlingVelocity", "Fling cancelled by touch at frame $frameCount")
                                                // 使用Kotlin协程的取消机制
                                                throw kotlinx.coroutines.CancellationException("Fling cancelled by touch")
                                            }

                                            frameCount++
                                            val delta = value - lastValue
                                            lastValue = value
                                            // Delta is already in dp space, don't divide by density again
                                            val oldOffset = scrollOffsetDp.value
                                            scrollOffsetDp = (scrollOffsetDp.value + delta).dp
                                            Log.e("ScrollOffset", "[FLING] frame=$frameCount, delta=${delta}dp, old=${oldOffset}dp, new=${scrollOffsetDp.value}dp")
                                            // 移除updateTrigger更新 - scrollOffsetDp变化会自动触发Canvas重绘
                                            if (frameCount % 10 == 0) {
                                                Log.e("FlingVelocity", "Frame $frameCount: delta=$delta, value=$value, newScrollOffset=${scrollOffsetDp.value}")
                                            }
                                        }
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        Log.e("FlingVelocity", "Fling animation cancelled at frame $frameCount")
                                    }
                                    Log.e("FlingVelocity", "Fling animation completed after $frameCount frames, final scrollOffset: ${scrollOffsetDp.value}")
                                }
                            } else {
                                Log.e("FlingVelocity", "Velocity too low, no fling triggered")
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                // 自定义的多指手势检测 - 只处理2指或以上的手势
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // 等待下一个事件来检查手指数量
                        var event = awaitPointerEvent()

                        // 只有当有2个或以上手指时才处理
                        if (event.changes.size < 2) {
                            Log.e("MultiTouchGesture", "✗ Skipping single-finger touch (${event.changes.size} fingers)")
                            continue
                        }

                        Log.e("MultiTouchGesture", "✓ Multi-touch gesture started with ${event.changes.size} fingers}")

                        // 取消惯性滚动动画
                        coroutineScope.launch {
                            flingAnimation.stop()
                        }

                        // 开始处理多指手势
                        var lastCentroid = event.changes.map { it.position }.let { positions ->
                            Offset(
                                positions.map { it.x }.average().toFloat(),
                                positions.map { it.y }.average().toFloat()
                            )
                        }
                        var lastZoom = 1f
                        var lastSpan = 0f

                        do {
                            event = awaitPointerEvent()

                            // 如果手指数量变为1，退出多指手势处理
                            if (event.changes.size < 2) {
                                break
                            }

                            val positions = event.changes.map { it.position }
                            val centroid = Offset(
                                positions.map { it.x }.average().toFloat(),
                                positions.map { it.y }.average().toFloat()
                            )

                            // 计算缩放
                            val span = positions.let { pts ->
                                if (pts.size < 2) return@let 0f
                                var sum = 0f
                                for (i in 0 until pts.size - 1) {
                                    for (j in i + 1 until pts.size) {
                                        val dx = pts[i].x - pts[j].x
                                        val dy = pts[i].y - pts[j].y
                                        sum += kotlin.math.sqrt(dx * dx + dy * dy)
                                    }
                                }
                                sum / (pts.size * (pts.size - 1) / 2f)
                            }

                            val zoom = if (lastSpan > 0f) span / lastSpan else 1f
                            val pan = centroid - lastCentroid

                            if (lastSpan > 0f) {
                                // 处理手势 - 在这里插入原来的手势处理代码

                    // 实时更新触摸中心点的日期（用于调试）- 无论是缩放还是平移都更新
                    val centroidYDp = centroid.y / density.density
                    val centroidUnits = (scrollOffsetDp.value + centroidYDp) / unitHeightDp.value
                    val calendar = Calendar.getInstance()
                    val centroidTime = scaleUnitsToCalendar(currentTimeScale, centroidUnits, calendar)

                    // 详细日志：记录中心点计算（仅在大刻度下输出，避免日志太多）
                    if (currentTimeScale >= TimeScaleLevel.YEAR) {
                        Log.e("CentroidCalc", "╔════════ CENTROID CALCULATION (${currentTimeScale.displayName}) ════════╗")
                        Log.e("CentroidCalc", "Centroid Y (px): ${centroid.y}")
                        Log.e("CentroidCalc", "Centroid Y (dp): $centroidYDp")
                        Log.e("CentroidCalc", "ScrollOffset (dp): ${scrollOffsetDp.value}")
                        Log.e("CentroidCalc", "UnitHeight (dp): ${unitHeightDp.value}")
                        Log.e("CentroidCalc", "Centroid Units: $centroidUnits")
                        Log.e("CentroidCalc", "Centroid Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(centroidTime.time)}")
                        Log.e("CentroidCalc", "╚════════════════════════════════════════════╝")
                    }

                    // 根据刻度格式化触摸中心点日期 - 尽可能精确到小时和分钟
                    val newCenterDateText = when (currentTimeScale) {
                        // 小时及以下：精确到秒
                        TimeScaleLevel.MINUTE, TimeScaleLevel.FIVE_MINUTES, TimeScaleLevel.TEN_MINUTES,
                        TimeScaleLevel.HALF_HOUR, TimeScaleLevel.HOUR -> {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(centroidTime.time)
                        }
                        // 天/周：精确到分钟
                        TimeScaleLevel.DAY, TimeScaleLevel.WEEK -> {
                            SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(centroidTime.time)
                        }
                        // 月/季度/年：精确到小时
                        TimeScaleLevel.MONTH, TimeScaleLevel.QUARTER, TimeScaleLevel.YEAR -> {
                            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(centroidTime.time)
                        }
                        // 10年/100年：精确到天和小时
                        TimeScaleLevel.DECADE, TimeScaleLevel.CENTURY -> {
                            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(centroidTime.time)
                        }
                    }

                    // 更新状态（调试用，但会触发Canvas重绘）
                    // centerDateText = newCenterDateText

                    // Canvas重绘由zoom操作触发，不需要在这里额外触发
                    // updateTrigger = System.currentTimeMillis()

                    // 处理缩放 - 统一的连续缩放逻辑
                    if (zoom != 1f) {
                        val oldHeight = unitHeightDp
                        val newHeight = oldHeight * zoom
                        val oldScale = currentTimeScale  // 保存旧刻度用于日志

                        // 核心逻辑：
                        // 1. 最小可读高度 = 60dp
                        // 2. 缩小时：当高度 < 60dp，切换到更粗刻度，新高度 = 旧高度 × 比例
                        // 3. 放大时：当高度 >= 60dp × 比例，切换到更细刻度，新高度 = 旧高度 ÷ 比例

                        val minHeight = 60.dp
                        var targetScale = currentTimeScale
                        var targetHeight = newHeight

                        Log.e("Timeline", "ZOOM START: currentScale=$currentTimeScale, oldHeight=${oldHeight.value}dp, newHeight=${newHeight.value}dp, zoom=$zoom")

                        debugInfo = "开始缩放: ${currentTimeScale.displayName}, ${oldHeight.value}dp -> ${newHeight.value}dp"

                        // 检查是否需要切换刻度
                        var loopCount = 0
                        while (true) {
                            loopCount++
                            if (loopCount > 100) {
                                Log.e("Timeline", "INFINITE LOOP DETECTED! Breaking. targetScale=$targetScale, targetHeight=${targetHeight.value}dp")
                                debugInfo = "ERROR: 无限循环!"
                                break
                            }

                            // 4点逻辑的阈值计算:
                            // 1. 最小可读高度 = 60dp
                            // 2. 放大阈值 = 60dp × finerScale的ratio (例如HOUR→HALF_HOUR: 60 × 2 = 120dp)
                            // 3. 缩小阈值 = 60dp
                            // 4. 最细刻度的最大高度 = 60dp (不允许继续放大)
                            val finerScale = targetScale.getFinerScale()
                            val switchToFinerThreshold = if (finerScale != null) {
                                minHeight * finerScale.ratioToNext
                            } else {
                                minHeight  // 已经是最细刻度，最大高度限制为60dp
                            }
                            val switchToCoarserThreshold = minHeight  // 缩小切换阈值

                            Log.e("Timeline", "Loop $loopCount: targetScale=$targetScale, targetHeight=${targetHeight.value}dp, finerThreshold=${switchToFinerThreshold.value}dp, coarserThreshold=${switchToCoarserThreshold.value}dp")

                            when {
                                // 放大：切换到更细刻度
                                targetHeight >= switchToFinerThreshold -> {
                                    if (finerScale != null) {
                                        val oldTargetHeight = targetHeight
                                        targetHeight = (targetHeight.value / finerScale.ratioToNext).dp
                                        targetScale = finerScale
                                        Log.d("Timeline", "  -> Switch to finer: $targetScale, height: ${oldTargetHeight.value}dp -> ${targetHeight.value}dp")
                                        // 继续检查是否需要再次切换
                                    } else {
                                        // 已经是最细刻度，限制高度
                                        targetHeight = targetHeight.coerceAtMost(switchToFinerThreshold)
                                        Log.d("Timeline", "  -> Already finest scale, clamping height to ${targetHeight.value}dp")
                                        break
                                    }
                                }
                                // 缩小：切换到更粗刻度
                                targetHeight < minHeight -> {
                                    val coarserScale = targetScale.getCoarserScale()
                                    if (coarserScale != null) {
                                        val oldTargetHeight = targetHeight
                                        // 切换到更粗刻度后，新高度应该是两个阈值的中间值
                                        // 例如：TEN_MINUTES的有效范围是 [60dp, 180dp]
                                        // 所以新高度应该是 (60 + 180) / 2 = 120dp
                                        val coarserScaleFinerScale = coarserScale.getFinerScale()
                                        val coarserSwitchToCoarserThreshold = minHeight
                                        val coarserSwitchToFinerThreshold = if (coarserScaleFinerScale != null) {
                                            minHeight * coarserScaleFinerScale.ratioToNext
                                        } else {
                                            minHeight
                                        }
                                        // 设置为中间值，远离两个阈值边界
                                        targetHeight = ((coarserSwitchToCoarserThreshold.value + coarserSwitchToFinerThreshold.value) / 2f).dp
                                        targetScale = coarserScale
                                        Log.e("Timeline", "  -> Switch to coarser: $targetScale, height: ${oldTargetHeight.value}dp -> ${targetHeight.value}dp (range: ${coarserSwitchToCoarserThreshold.value}dp - ${coarserSwitchToFinerThreshold.value}dp)")
                                        // 不再继续循环,避免连续切换
                                        break
                                    } else {
                                        // 已经是最粗刻度，限制高度为最小值
                                        targetHeight = minHeight
                                        Log.e("Timeline", "  -> Already coarsest scale, clamping height to ${targetHeight.value}dp")
                                        break
                                    }
                                }
                                // 在合理范围内，不需要切换
                                else -> {
                                    Log.d("Timeline", "  -> Height in valid range, no scale switch needed")
                                    break
                                }
                            }
                        }

                        Log.d("Timeline", "ZOOM END: finalScale=$targetScale, finalHeight=${targetHeight.value}dp, loops=$loopCount")

                        debugInfo = "循环结束: loops=$loopCount, ${targetScale.displayName}, ${targetHeight.value}dp"

                        val (newScale, clampedHeight) = Pair(targetScale, targetHeight)

                        if (clampedHeight != oldHeight || newScale != currentTimeScale) {
                            // 如果尺度发生变化，需要转换scrollOffset
                            if (newScale != currentTimeScale) {
                                Log.e("ScaleSwitch", "")
                                Log.e("ScaleSwitch", "╔════════════════════════════════════════════════════════════╗")
                                Log.e("ScaleSwitch", "║          SCALE SWITCH DETECTED                             ║")
                                Log.e("ScaleSwitch", "╚════════════════════════════════════════════════════════════╝")

                                // 1. 计算捏合中心点在屏幕上的位置（dp）
                                val centroidYDp = centroid.y / density.density
                                Log.e("ScaleSwitch", "1️⃣ Centroid Screen Position:")
                                Log.e("ScaleSwitch", "   Centroid Y (px): ${centroid.y}")
                                Log.e("ScaleSwitch", "   Centroid Y (dp): $centroidYDp")

                                // 2. 计算该点在旧刻度下对应的单位数
                                val oldCenterUnits = (scrollOffsetDp.value + centroidYDp) / oldHeight.value
                                Log.e("ScaleSwitch", "2️⃣ OLD Scale Calculation:")
                                Log.e("ScaleSwitch", "   Old Scale: ${currentTimeScale.displayName}")
                                Log.e("ScaleSwitch", "   Old ScrollOffset (dp): ${scrollOffsetDp.value}")
                                Log.e("ScaleSwitch", "   Old UnitHeight (dp): ${oldHeight.value}")
                                Log.e("ScaleSwitch", "   Old Center Units: $oldCenterUnits")

                                // 3. 将旧刻度的单位转换为绝对时间（Calendar）
                                val calendar = Calendar.getInstance()
                                val focusTime = scaleUnitsToCalendar(currentTimeScale, oldCenterUnits, calendar)
                                Log.e("ScaleSwitch", "3️⃣ Absolute Time (Focus Point):")
                                Log.e("ScaleSwitch", "   Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(focusTime.time)}")
                                Log.e("ScaleSwitch", "   Millis: ${focusTime.timeInMillis}")

                                // 4. 将绝对时间转换为新刻度的单位数
                                val newCenterUnits = calendarToScaleUnits(newScale, focusTime, calendar)
                                Log.e("ScaleSwitch", "4️⃣ NEW Scale Calculation:")
                                Log.e("ScaleSwitch", "   New Scale: ${newScale.displayName}")
                                Log.e("ScaleSwitch", "   New UnitHeight (dp): ${clampedHeight.value}")
                                Log.e("ScaleSwitch", "   New Center Units: $newCenterUnits")

                                // 5. 更新状态
                                currentTimeScale = newScale
                                unitHeightDp = clampedHeight

                                // 6. 计算新的scrollOffset，保持捏合中心点在屏幕上的位置不变
                                val oldScrollOffset = scrollOffsetDp.value
                                val newScrollOffset = newCenterUnits * clampedHeight.value - centroidYDp
                                scrollOffsetDp = newScrollOffset.dp
                                Log.e("ScrollOffset", "[ZOOM-SCALE] old=${oldScrollOffset}dp, new=${newScrollOffset}dp, oldScale=${oldScale.displayName}, newScale=${newScale.displayName}")
                                Log.e("ScaleSwitch", "5️⃣ NEW ScrollOffset Calculation:")
                                Log.e("ScaleSwitch", "   Formula: newCenterUnits * newHeight - centroidYDp")
                                Log.e("ScaleSwitch", "   = $newCenterUnits * ${clampedHeight.value} - $centroidYDp")
                                Log.e("ScaleSwitch", "   = $newScrollOffset")
                                Log.e("ScaleSwitch", "   New ScrollOffset (dp): ${scrollOffsetDp.value}")

                                // 验证：重新计算中心点应该得到相同的时间
                                val verifyCentroidUnits = (scrollOffsetDp.value + centroidYDp) / clampedHeight.value
                                val verifyTime = scaleUnitsToCalendar(newScale, verifyCentroidUnits, Calendar.getInstance())
                                Log.e("ScaleSwitch", "6️⃣ VERIFICATION:")
                                Log.e("ScaleSwitch", "   Verify Centroid Units: $verifyCentroidUnits")
                                Log.e("ScaleSwitch", "   Verify Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(verifyTime.time)}")
                                Log.e("ScaleSwitch", "   Time Match: ${focusTime.timeInMillis == verifyTime.timeInMillis}")
                                Log.e("ScaleSwitch", "   Time Diff (ms): ${verifyTime.timeInMillis - focusTime.timeInMillis}")
                                Log.e("ScaleSwitch", "╚════════════════════════════════════════════════════════════╝")
                                Log.e("ScaleSwitch", "")
                            } else{
                                // 同一尺度内的缩放
                                val oldScrollOffset = scrollOffsetDp.value
                                val centerUnits = (scrollOffsetDp.value + centroid.y / density.density) / oldHeight.value

                                unitHeightDp = clampedHeight

                                // 允许负值，支持无限滚动
                                val newScrollOffset = centerUnits * clampedHeight.value - centroid.y / density.density
                                scrollOffsetDp = newScrollOffset.dp
                                Log.e("ScrollOffset", "[ZOOM-SAME] old=${oldScrollOffset}dp, new=${newScrollOffset}dp, oldHeight=${oldHeight.value}dp, newHeight=${clampedHeight.value}dp")

                                Log.d("Timeline", "Scale: $currentTimeScale, Height: ${clampedHeight.value}dp")
                            }
                        }
                    }

                    // Pan is now handled by single-finger drag gesture with fling support
                    // Two-finger pan during zoom is still applied as part of the zoom gesture
                            }

                            // 更新last values用于下一次计算
                            lastCentroid = centroid
                            lastSpan = span

                        } while (event.changes.any { it.pressed })
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 使用硬件加速层缓存Canvas绘制，避免重组时不必要的重绘
                    // 只有当scrollOffsetDp或unitHeightDp改变时才重绘
                }
        ) {
        // 移除对updateTrigger的依赖 - Canvas只应该在真正需要的状态改变时重绘
        // 依赖的状态：scrollOffsetDp, unitHeightDp, currentTimeScale
        // 这些状态的变化会自动触发Compose重组和Canvas重绘

        // 记录Canvas重绘 - 在Canvas lambda内部，只有真正重绘时才会执行
        Log.e("CanvasRedraw", "Canvas重绘: scrollOffset=${scrollOffsetDp.value}dp, unitHeight=${unitHeightDp.value}dp, scale=${currentTimeScale.displayName}")

        val unitHeightPx = unitHeightDp.toPx()
        val scrollOffsetPx = scrollOffsetDp.toPx()

        // 根据当前时间尺度绘制不同的内容
        when (currentTimeScale) {
            TimeScaleLevel.MINUTE, TimeScaleLevel.FIVE_MINUTES, TimeScaleLevel.TEN_MINUTES, TimeScaleLevel.HALF_HOUR, TimeScaleLevel.HOUR -> {
                // 小时及以下级别：使用统一的时间线绘制
                drawHourLevelScale(currentTimeScale, unitHeightPx, scrollOffsetPx, textMeasurer, lineColor, textColor, currentTimeColor, size.width, events)
            }
            TimeScaleLevel.DAY -> {
                drawDayScale(unitHeightPx, scrollOffsetPx, textMeasurer, lineColor, textColor, currentTimeColor, size.width)
            }
            TimeScaleLevel.WEEK -> {
                drawWeekScale(size, unitHeightPx, scrollOffsetPx, textMeasurer, lineColor, textColor, currentTimeColor, size.width)
            }
            TimeScaleLevel.MONTH -> {
                drawMonthScale(size, unitHeightPx, scrollOffsetPx, textMeasurer, lineColor, textColor, currentTimeColor, size.width)
            }
            TimeScaleLevel.QUARTER -> {
                drawQuarterScale(size, unitHeightPx, scrollOffsetPx, textMeasurer, lineColor, textColor, currentTimeColor, size.width)
            }
            TimeScaleLevel.YEAR -> {
                drawYearScale(size, unitHeightPx, scrollOffsetPx, textMeasurer, lineColor, textColor, currentTimeColor, size.width)
            }
            TimeScaleLevel.DECADE -> {
                drawDecadeScale(size, unitHeightPx, scrollOffsetPx, textMeasurer, lineColor, textColor, currentTimeColor, size.width)
            }
            TimeScaleLevel.CENTURY -> {
                drawCenturyScale(size, unitHeightPx, scrollOffsetPx, textMeasurer, lineColor, textColor, currentTimeColor, size.width)
            }
        }

        // 绘制触点可视化（所有刻度都显示）- 必须在drawIntoCanvas之前，因为需要DrawScope
        // 绘制所有触点
        touchPointsInfo.forEachIndexed { index, (position, timeStr) ->
            // 绘制触点圆圈
            drawCircle(
                color = Color.Red,
                radius = 30f,
                center = position,
                style = Stroke(width = 4f)
            )
        }

        // 绘制几何中心点
        centroidInfo?.let { (position, timeStr) ->
            // 绘制中心点圆圈（蓝色，更大）
            drawCircle(
                color = Color.Blue,
                radius = 40f,
                center = position,
                style = Stroke(width = 5f)
            )
            // 绘制中心十字
            drawLine(
                color = Color.Blue,
                start = Offset(position.x - 50f, position.y),
                end = Offset(position.x + 50f, position.y),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Blue,
                start = Offset(position.x, position.y - 50f),
                end = Offset(position.x, position.y + 50f),
                strokeWidth = 3f
            )
        }

        // 绘制当前时间尺度标识（左下角）- 添加详细调试信息
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 36f
                color = Color.Red.toArgb()
                setShadowLayer(4f, 0f, 0f, Color.Black.toArgb())
            }

            val debugText = "中心: $centerDateText\n刻度: ${currentTimeScale.displayName}\n高度: ${unitHeightDp.value}dp\n$debugInfo"
            var yPos = size.height - 195f
            debugText.lines().forEach { line ->
                canvas.nativeCanvas.drawText(line, 100f, yPos, paint)
                yPos += 45f
            }

            // 绘制触点文字标签（所有刻度都显示，统一精确到秒）
            // 绘制所有触点的文字标签
            touchPointsInfo.forEachIndexed { index, (position, timeStr) ->
                val labelPaint = android.graphics.Paint().apply {
                    color = Color.Red.toArgb()
                    textSize = 32f  // 缩小字体以适应完整时间格式
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.LEFT  // 左对齐，向右偏移
                }
                // 横向偏移150px，避免被手指遮挡
                val offsetX = position.x + 60f
                // 分两行显示：第一行显示"触点N"，第二行显示完整时间
                canvas.nativeCanvas.drawText(
                    "触点${index + 1}",
                    offsetX,
                    position.y - 65f,
                    labelPaint
                )
                canvas.nativeCanvas.drawText(
                    timeStr,
                    offsetX,
                    position.y - 35f,
                    labelPaint
                )
            }

            // 绘制几何中心点的文字标签
            centroidInfo?.let { (position, timeStr) ->
                val centerLabelPaint = android.graphics.Paint().apply {
                    color = Color.Blue.toArgb()
                    textSize = 36f  // 稍大但仍能容纳完整时间
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.LEFT  // 左对齐，向右偏移
                    isFakeBoldText = true
                }
                // 横向偏移，避免遮挡
                val offsetX = position.x + 60f
                // 分两行显示：第一行显示"中心"，第二行显示完整时间
                canvas.nativeCanvas.drawText(
                    "中心",
                    offsetX,
                    position.y + 70f,
                    centerLabelPaint
                )
                canvas.nativeCanvas.drawText(
                    timeStr,
                    offsetX,
                    position.y + 105f,
                    centerLabelPaint
                )
            }
        }
        }
        }  // Canvas内部Box
    }  // BoxWithConstraints
}
