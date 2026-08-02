package com.mirza.transfers.di

import com.mirza.transfers.data.datasource.TransferLocalDataSource
import com.mirza.transfers.data.datasource.TransferLocalDataSourceImpl
import com.mirza.transfers.data.datasource.TransferRemoteDataSource
import com.mirza.transfers.data.datasource.TransferRemoteDataSourceImpl
import com.mirza.transfers.data.repository.TransferRepositoryImpl
import com.mirza.transfers.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TransferModule {

    @Binds
    @Singleton
    abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository

    @Binds
    @Singleton
    abstract fun bindTransferRemoteDataSource(impl: TransferRemoteDataSourceImpl): TransferRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTransferLocalDataSource(impl: TransferLocalDataSourceImpl): TransferLocalDataSource
}