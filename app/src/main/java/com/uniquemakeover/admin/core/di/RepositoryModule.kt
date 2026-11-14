package com.uniquemakeover.admin.core.di

import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.feature.auth.data.PinRepositoryImpl
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import com.uniquemakeover.admin.feature.services.data.repository.ServiceRepositoryImpl
import com.uniquemakeover.admin.feature.settings.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServiceRepository(
        serviceRepositoryImpl: ServiceRepositoryImpl
    ): ServiceRepository

    @Binds
    @Singleton
    abstract fun bindPinRepository(
        pinRepositoryImpl: PinRepositoryImpl
    ): PinRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
