package com.example.basecompose.di

import android.content.Context
import com.example.basecompose.utils.network.LiveNetworkMonitor
import com.example.basecompose.utils.network.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module cung cấp các dependency cấp Application.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return LiveNetworkMonitor(context)
    }
}
