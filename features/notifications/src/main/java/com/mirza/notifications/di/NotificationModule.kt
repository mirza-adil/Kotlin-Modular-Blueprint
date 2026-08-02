package com.mirza.notifications.di

import com.mirza.notifications.data.datasource.NotificationLocalDataSource
import com.mirza.notifications.data.datasource.NotificationLocalDataSourceImpl
import com.mirza.notifications.data.datasource.NotificationRemoteDataSource
import com.mirza.notifications.data.datasource.NotificationRemoteDataSourceImpl
import com.mirza.notifications.data.repository.NotificationRepositoryImpl
import com.mirza.notifications.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRemoteDataSource(impl: NotificationRemoteDataSourceImpl): NotificationRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindNotificationLocalDataSource(impl: NotificationLocalDataSourceImpl): NotificationLocalDataSource
}