package com.jarvis.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.jarvis.navigation.NavGraph
import com.jarvis.ui.components.BottomNavigationBar

/**
 * 主屏幕
 * 包含底部导航栏和内容区域
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // 滚动到当前时间的触发器
    var scrollToNowTrigger by remember { mutableStateOf(0L) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                onTodoDoubleClick = {
                    // 双击待办按钮时，更新触发器
                    scrollToNowTrigger = System.currentTimeMillis()
                }
            )
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            scrollToNowTrigger = scrollToNowTrigger
        )
    }
}
