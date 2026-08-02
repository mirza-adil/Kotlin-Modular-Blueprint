package com.mirza.cards.data.mapper

import com.mirza.cards.data.datasource.CardDto
import com.mirza.cards.domain.model.Card
import com.mirza.cards.domain.model.CardStatus
import com.mirza.cards.domain.model.CardType

fun CardDto.toDomain(): Card = Card(
    id = id,
    maskedNumber = maskedNumber,
    cardHolderName = cardHolderName,
    cardType = runCatching { CardType.valueOf(cardType) }.getOrDefault(CardType.DEBIT),
    expiryMonth = expiryMonth,
    expiryYear = expiryYear,
    status = runCatching { CardStatus.valueOf(status) }.getOrDefault(CardStatus.ACTIVE)
)

fun List<CardDto>.toDomain(): List<Card> = map { it.toDomain() }