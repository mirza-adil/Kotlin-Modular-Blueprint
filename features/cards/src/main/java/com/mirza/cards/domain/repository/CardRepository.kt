package com.mirza.cards.domain.repository

import com.mirza.cards.domain.model.Card
import com.mirza.common.result.Result
import kotlinx.coroutines.flow.Flow

interface CardRepository {

    fun getCards(): Flow<Result<List<Card>>>

    suspend fun getCardById(cardId: String): Result<Card>

    suspend fun setCardFrozen(cardId: String, isFrozen: Boolean): Result<Unit>

    suspend fun refreshCards(): Result<Unit>
}