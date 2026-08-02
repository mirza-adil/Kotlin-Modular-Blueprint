package com.mirza.accounts.di

import com.mirza.accounts.data.datasource.AccountLocalDataSource
import com.mirza.accounts.data.datasource.AccountLocalDataSourceImpl
import com.mirza.accounts.data.datasource.AccountRemoteDataSource
import com.mirza.accounts.data.datasource.AccountRemoteDataSourceImpl
import com.mirza.accounts.data.repository.AccountRepositoryImpl
import com.mirza.accounts.domain.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindAccountRemoteDataSource(impl: AccountRemoteDataSourceImpl): AccountRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAccountLocalDataSource(impl: AccountLocalDataSourceImpl): AccountLocalDataSource
}