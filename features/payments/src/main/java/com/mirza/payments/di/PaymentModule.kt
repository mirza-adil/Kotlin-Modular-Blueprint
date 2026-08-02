package com.mirza.payments.di

import com.mirza.payments.data.datasource.PaymentLocalDataSource
import com.mirza.payments.data.datasource.PaymentLocalDataSourceImpl
import com.mirza.payments.data.datasource.PaymentRemoteDataSource
import com.mirza.payments.data.datasource.PaymentRemoteDataSourceImpl
import com.mirza.payments.data.repository.PaymentRepositoryImpl
import com.mirza.payments.domain.repository.PaymentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRemoteDataSource(impl: PaymentRemoteDataSourceImpl): PaymentRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindPaymentLocalDataSource(impl: PaymentLocalDataSourceImpl): PaymentLocalDataSource
}