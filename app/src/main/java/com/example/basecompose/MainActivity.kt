package com.example.basecompose

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.basecompose.base.BaseApp
import com.example.basecompose.navigation.AppNavHost
import com.example.basecompose.utils.locale.AppLocale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Áp dụng ngôn ngữ người dùng đã chọn ở màn Language. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash hệ thống (Android 12+) đóng ngay, nhường cho SplashScreen Compose cùng màu nền.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Game chơi offline, không cần chặn màn hình khi mất mạng.
        setContent { BaseApp(checkNetwork = false) { AppNavHost() } }
    }
}
