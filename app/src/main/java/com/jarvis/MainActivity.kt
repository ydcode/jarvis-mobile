package com.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.jarvis.ui.MainScreen
import com.jarvis.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 设置状态栏为透明，深色图标
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            JarvisTheme {
                MainScreen()
            }
        }
    }
}