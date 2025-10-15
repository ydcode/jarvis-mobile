package com.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jarvis.navigation.BottomNavItem
import com.jarvis.navigation.bottomNavItems
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding

/**
 * 微信风格的底部导航栏 - 完全自定义紧凑布局
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    onTodoDoubleClick: () -> Unit = {}
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    // 双击检测状态
    var lastTodoClickTime by remember { mutableStateOf(0L) }

    // 使用Column来包含导航栏内容和系统导航栏padding
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)  // 整个区域背景为白色
    ) {
        // 导航栏内容（无阴影）
        Box(modifier = Modifier.fillMaxWidth()) {
            // 导航栏内容区域固定高度50dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any {
                    it.route == item.screen.route
                } == true

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null  // 禁用水波纹效果
                        ) {
                            // 检测是否是待办按钮的双击
                            if (item.screen == com.jarvis.navigation.Screen.Todo && selected) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastTodoClickTime < 500) {
                                    // 双击触发
                                    onTodoDoubleClick()
                                    lastTodoClickTime = 0L  // 重置
                                } else {
                                    lastTodoClickTime = currentTime
                                }
                            } else {
                                // 正常导航
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(vertical = 4.dp),  // 上下各4dp
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) Color(0xFF07C160) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        color = if (selected) Color(0xFF07C160) else Color.Gray
                    )
                }
            }
            }  // Row
        }  // Box

        // 系统导航栏区域也使用白色背景
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        )
    }  // Column
}
