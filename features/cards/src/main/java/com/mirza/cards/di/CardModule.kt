package com.mirza.cards.di

import com.mirza.cards.data.datasource.CardLocalDataSource
import com.mirza.cards.data.datasource.CardLocalDataSourceImpl
import com.mirza.cards.data.datasource.CardRemoteDataSource
import com.mirza.cards.data.datasource.CardRemoteDataSourceImpl
import com.mirza.cards.data.repository.CardRepositoryImpl
import com.mirza.cards.domain.repository.CardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CardModule {

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindCardRemoteDataSource(impl: CardRemoteDataSourceImpl): CardRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCardLocalDataSource(impl: CardLocalDataSourceImpl): CardLocalDataSource
}