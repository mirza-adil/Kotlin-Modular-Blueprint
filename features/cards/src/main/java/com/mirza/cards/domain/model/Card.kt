package com.mirza.cards.domain.model

data class Card(
    val id: String,
    val maskedNumber: String,
    val cardHolderName: String,
    val cardType: CardType,
    val expiryMonth: Int,
    val expiryYear: Int,
    val status: CardStatus
)

enum class CardType {
    DEBIT,
    CREDIT,
    VIRTUAL
}

enum class CardStatus {
    ACTIVE,
    FROZEN,
    BLOCKED,
    EXPIRED
}