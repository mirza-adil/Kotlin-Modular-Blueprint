package com.mirza.cards.data.datasource

data class CardDto(
    val id: String,
    val maskedNumber: String,
    val cardHolderName: String,
    val cardType: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val status: String
)