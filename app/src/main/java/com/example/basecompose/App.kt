package com.example.basecompose

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class - Entry point cho Hilt Dependency Injection.
 *
 * Annotation @HiltAndroidApp triggers Hilt's code generation,
 * bao gồm base class cho application để sử dụng dependency injection.
 */
@HiltAndroidApp
class App : Application()
