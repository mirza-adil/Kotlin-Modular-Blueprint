package com.mirza.cards.data.datasource

import javax.inject.Inject

interface CardRemoteDataSource {

    suspend fun getCards(): List<CardDto>

    suspend fun setCardFrozen(cardId: String, isFrozen: Boolean)
}

class CardRemoteDataSourceImpl @Inject constructor() : CardRemoteDataSource {

    override suspend fun getCards(): List<CardDto> {
        // TODO: wire up to the real cards endpoint once it is available
        return emptyList()
    }

    override suspend fun setCardFrozen(cardId: String, isFrozen: Boolean) {
        // TODO: wire up to the real cards endpoint once it is available
    }
}