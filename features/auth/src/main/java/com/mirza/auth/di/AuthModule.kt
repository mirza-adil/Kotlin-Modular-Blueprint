package com.mirza.auth.di

import com.mirza.auth.data.datasource.AuthLocalDataSource
import com.mirza.auth.data.datasource.AuthLocalDataSourceImpl
import com.mirza.auth.data.datasource.AuthRemoteDataSource
import com.mirza.auth.data.datasource.AuthRemoteDataSourceImpl
import com.mirza.auth.data.repository.AuthRepositoryImpl
import com.mirza.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: AuthRemoteDataSourceImpl): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAuthLocalDataSource(impl: AuthLocalDataSourceImpl): AuthLocalDataSource
}