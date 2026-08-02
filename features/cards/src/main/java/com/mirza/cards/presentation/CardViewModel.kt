package com.mirza.cards.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.cards.domain.usecase.GetCardByIdUseCase
import com.mirza.cards.domain.usecase.GetCardsUseCase
import com.mirza.cards.domain.usecase.ToggleCardFreezeUseCase
import com.mirza.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardViewModel @Inject constructor(
    private val getCardsUseCase: GetCardsUseCase,
    private val getCardByIdUseCase: GetCardByIdUseCase,
    private val toggleCardFreezeUseCase: ToggleCardFreezeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    init {
        onEvent(CardEvent.LoadCards)
    }

    fun onEvent(event: CardEvent) {
        when (event) {
            CardEvent.LoadCards -> loadCards()
            is CardEvent.SelectCard -> selectCard(event.cardId)
            is CardEvent.ToggleFreeze -> toggleFreeze(event.cardId, event.isFrozen)
            CardEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadCards() {
        viewModelScope.launch {
            getCardsUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, cards = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun selectCard(cardId: String) {
        viewModelScope.launch {
            when (val result = getCardByIdUseCase(cardId)) {
                is Result.Success -> _uiState.update { it.copy(selectedCard = result.data) }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    private fun toggleFreeze(cardId: String, isFrozen: Boolean) {
        viewModelScope.launch {
            val params = ToggleCardFreezeUseCase.Params(cardId, isFrozen)
            when (val result = toggleCardFreezeUseCase(params)) {
                is Result.Success -> loadCards()
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }
}