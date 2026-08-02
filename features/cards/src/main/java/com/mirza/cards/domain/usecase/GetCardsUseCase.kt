package com.mirza.cards.domain.usecase

import com.mirza.cards.domain.model.Card
import com.mirza.cards.domain.repository.CardRepository
import com.mirza.common.base.FlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<Card>>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<List<Card>>> =
        cardRepository.getCards()
}