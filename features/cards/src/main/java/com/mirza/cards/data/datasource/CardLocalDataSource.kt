package com.mirza.cards.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface CardLocalDataSource {

    fun observeCards(): Flow<List<CardDto>>

    suspend fun getCardById(cardId: String): CardDto?

    suspend fun cacheCards(cards: List<CardDto>)

    suspend fun updateCardStatus(cardId: String, status: String)
}

@Singleton
class CardLocalDataSourceImpl @Inject constructor() : CardLocalDataSource {

    private val cachedCards = MutableStateFlow<List<CardDto>>(emptyList())

    override fun observeCards(): Flow<List<CardDto>> = cachedCards.asStateFlow()

    override suspend fun getCardById(cardId: String): CardDto? =
        cachedCards.value.find { it.id == cardId }

    override suspend fun cacheCards(cards: List<CardDto>) {
        cachedCards.value = cards
    }

    override suspend fun updateCardStatus(cardId: String, status: String) {
        cachedCards.value = cachedCards.value.map { card ->
            if (card.id == cardId) card.copy(status = status) else card
        }
    }
}