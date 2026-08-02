package com.mirza.profile.di

import com.mirza.profile.data.datasource.ProfileLocalDataSource
import com.mirza.profile.data.datasource.ProfileLocalDataSourceImpl
import com.mirza.profile.data.datasource.ProfileRemoteDataSource
import com.mirza.profile.data.datasource.ProfileRemoteDataSourceImpl
import com.mirza.profile.data.repository.ProfileRepositoryImpl
import com.mirza.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindProfileRemoteDataSource(impl: ProfileRemoteDataSourceImpl): ProfileRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindProfileLocalDataSource(impl: ProfileLocalDataSourceImpl): ProfileLocalDataSource
}