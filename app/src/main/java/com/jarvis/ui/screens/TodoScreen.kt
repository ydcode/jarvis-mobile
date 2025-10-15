package com.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jarvis.data.model.getSampleEvents
import com.jarvis.data.model.getSampleEventsForDate
import com.jarvis.ui.components.*
import java.util.*

/**
 * 日历日程视图（类似 Apple Calendar）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    scrollToNowTrigger: Long = 0L  // 接收滚动到当前时间的触发器
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    // 根据选中的日期动态生成事件
    val events = remember(selectedDate) {
        getSampleEventsForDate(selectedDate)
    }

    // 视图模式：true为周视图，false为月视图
    var isWeekView by remember { mutableStateOf(true) }

    // 根据选中日期动态获取月份名称
    val monthName = remember(selectedDate) {
        val monthNames = arrayOf("一月", "二月", "三月", "四月", "五月", "六月",
                                 "七月", "八月", "九月", "十月", "十一月", "十二月")
        monthNames[selectedDate.get(Calendar.MONTH)]
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // 顶部标题栏
                TopAppBar(
                    title = {
                        Text(
                            text = monthName,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* 搜索功能 */ }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                        IconButton(onClick = { /* 添加事件 */ }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加事件"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)  // 移除所有内边距
                )

                // 周日期选择器（Canvas版本，支持无缝无限滑动）
                CanvasWeekDateSelector(
                    selectedDate = selectedDate,
                    onDateChange = { newDate ->
                        selectedDate = newDate
                    },
                    onPinchToMonthView = { /* TODO: 实现切换到月视图 */ }
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CanvasTimelineView(
                events = events,
                modifier = Modifier.fillMaxSize(),
                scrollToNowTrigger = scrollToNowTrigger
            )
        }
    }
}
