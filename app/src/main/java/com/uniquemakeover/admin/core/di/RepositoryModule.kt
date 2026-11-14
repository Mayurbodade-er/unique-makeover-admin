package com.uniquemakeover.admin.core.di

import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.feature.services.data.repository.ServiceRepositoryImpl
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
}
