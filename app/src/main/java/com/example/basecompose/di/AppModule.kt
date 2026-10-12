package com.example.basecompose.di

import com.example.basecompose.data.local.AppSettingsRepositoryImpl
import com.example.basecompose.domain.repository.AppSettingsRepository
import com.example.basecompose.utils.network.LiveNetworkMonitor
import com.example.basecompose.utils.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt Module cung cấp các dependency cấp Application.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /** [LiveNetworkMonitor] đã có `@Inject constructor` + `@Singleton`, chỉ cần bind interface. */
    @Binds
    abstract fun bindNetworkMonitor(impl: LiveNetworkMonitor): NetworkMonitor

    @Binds
    abstract fun bindAppSettingsRepository(impl: AppSettingsRepositoryImpl): AppSettingsRepository
}
