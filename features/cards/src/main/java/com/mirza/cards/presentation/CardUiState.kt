package com.mirza.cards.presentation

import com.mirza.cards.domain.model.Card

data class CardUiState(
    val isLoading: Boolean = false,
    val cards: List<Card> = emptyList(),
    val selectedCard: Card? = null,
    val errorMessage: String? = null
)