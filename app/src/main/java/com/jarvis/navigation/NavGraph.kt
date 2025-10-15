package com.jarvis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jarvis.ui.screens.TodoScreen
import com.jarvis.ui.screens.ContactsScreen
import com.jarvis.ui.screens.DiscoverScreen
import com.jarvis.ui.screens.MeScreen

/**
 * 应用导航图
 * @param navController 导航控制器
 * @param modifier 修饰符
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    scrollToNowTrigger: Long = 0L
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Todo.route,
        modifier = modifier
    ) {
        composable(route = Screen.Todo.route) {
            TodoScreen(scrollToNowTrigger = scrollToNowTrigger)
        }
        composable(route = Screen.Contacts.route) {
            ContactsScreen()
        }
        composable(route = Screen.Discover.route) {
            DiscoverScreen()
        }
        composable(route = Screen.Me.route) {
            MeScreen()
        }
    }
}
