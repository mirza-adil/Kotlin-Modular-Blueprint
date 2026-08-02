package com.mirza.dashboard.di

import com.mirza.dashboard.data.datasource.DashboardLocalDataSource
import com.mirza.dashboard.data.datasource.DashboardLocalDataSourceImpl
import com.mirza.dashboard.data.datasource.DashboardRemoteDataSource
import com.mirza.dashboard.data.datasource.DashboardRemoteDataSourceImpl
import com.mirza.dashboard.data.repository.DashboardRepositoryImpl
import com.mirza.dashboard.domain.repository.DashboardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRemoteDataSource(impl: DashboardRemoteDataSourceImpl): DashboardRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindDashboardLocalDataSource(impl: DashboardLocalDataSourceImpl): DashboardLocalDataSource
}