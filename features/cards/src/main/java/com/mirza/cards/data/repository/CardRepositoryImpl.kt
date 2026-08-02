package com.mirza.cards.data.repository

import com.mirza.cards.data.datasource.CardLocalDataSource
import com.mirza.cards.data.datasource.CardRemoteDataSource
import com.mirza.cards.data.mapper.toDomain
import com.mirza.cards.domain.model.Card
import com.mirza.cards.domain.model.CardStatus
import com.mirza.cards.domain.repository.CardRepository
import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val remoteDataSource: CardRemoteDataSource,
    private val localDataSource: CardLocalDataSource
) : CardRepository {

    override fun getCards(): Flow<Result<List<Card>>> =
        localDataSource.observeCards()
            .map { it.toDomain() }
            .asResult()

    override suspend fun getCardById(cardId: String): Result<Card> {
        return try {
            val dto = localDataSource.getCardById(cardId)
                ?: throw NoSuchElementException("Card $cardId not found")
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun setCardFrozen(cardId: String, isFrozen: Boolean): Result<Unit> {
        return try {
            remoteDataSource.setCardFrozen(cardId, isFrozen)
            val status = if (isFrozen) CardStatus.FROZEN else CardStatus.ACTIVE
            localDataSource.updateCardStatus(cardId, status.name)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshCards(): Result<Unit> {
        return try {
            val cards = remoteDataSource.getCards()
            localDataSource.cacheCards(cards)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}