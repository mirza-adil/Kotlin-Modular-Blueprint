package com.mirza.cards.domain.usecase

import com.mirza.cards.domain.repository.CardRepository
import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ToggleCardFreezeUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<ToggleCardFreezeUseCase.Params, Unit>(dispatcher) {

    data class Params(val cardId: String, val isFrozen: Boolean)

    override suspend fun execute(parameters: Params) {
        cardRepository.setCardFrozen(parameters.cardId, parameters.isFrozen).getOrThrow()
    }
}