package com.mirza.cards.presentation

sealed interface CardEvent {
    data object LoadCards : CardEvent
    data class SelectCard(val cardId: String) : CardEvent
    data class ToggleFreeze(val cardId: String, val isFrozen: Boolean) : CardEvent
    data object DismissError : CardEvent
}