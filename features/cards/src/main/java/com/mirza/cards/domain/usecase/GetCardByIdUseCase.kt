package com.mirza.cards.domain.usecase

import com.mirza.cards.domain.model.Card
import com.mirza.cards.domain.repository.CardRepository
import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetCardByIdUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<String, Card>(dispatcher) {

    override suspend fun execute(parameters: String): Card =
        cardRepository.getCardById(parameters).getOrThrow()
}