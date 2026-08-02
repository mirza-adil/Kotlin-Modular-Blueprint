package com.mirza.beneficiaries.di

import com.mirza.beneficiaries.data.datasource.BeneficiaryLocalDataSource
import com.mirza.beneficiaries.data.datasource.BeneficiaryLocalDataSourceImpl
import com.mirza.beneficiaries.data.datasource.BeneficiaryRemoteDataSource
import com.mirza.beneficiaries.data.datasource.BeneficiaryRemoteDataSourceImpl
import com.mirza.beneficiaries.data.repository.BeneficiaryRepositoryImpl
import com.mirza.beneficiaries.domain.repository.BeneficiaryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BeneficiaryModule {

    @Binds
    @Singleton
    abstract fun bindBeneficiaryRepository(impl: BeneficiaryRepositoryImpl): BeneficiaryRepository

    @Binds
    @Singleton
    abstract fun bindBeneficiaryRemoteDataSource(impl: BeneficiaryRemoteDataSourceImpl): BeneficiaryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindBeneficiaryLocalDataSource(impl: BeneficiaryLocalDataSourceImpl): BeneficiaryLocalDataSource
}