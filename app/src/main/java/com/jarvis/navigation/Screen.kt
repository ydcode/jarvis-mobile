package com.jarvis.navigation

/**
 * 定义应用中的所有屏幕路由
 */
sealed class Screen(val route: String) {
    data object Todo : Screen("todo")
    data object Contacts : Screen("contacts")
    data object Discover : Screen("discover")
    data object Me : Screen("me")
}
