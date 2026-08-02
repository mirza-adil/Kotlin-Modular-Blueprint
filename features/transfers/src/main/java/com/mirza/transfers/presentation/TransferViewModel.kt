package com.mirza.transfers.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.common.result.Result
import com.mirza.transfers.domain.usecase.CreateTransferUseCase
import com.mirza.transfers.domain.usecase.GetTransfersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val getTransfersUseCase: GetTransfersUseCase,
    private val createTransferUseCase: CreateTransferUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        onEvent(TransferEvent.LoadTransfers)
    }

    fun onEvent(event: TransferEvent) {
        when (event) {
            TransferEvent.LoadTransfers -> loadTransfers()
            is TransferEvent.CreateTransfer -> createTransfer(event)
            TransferEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadTransfers() {
        viewModelScope.launch {
            getTransfersUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, transfers = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun createTransfer(event: TransferEvent.CreateTransfer) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val params = CreateTransferUseCase.Params(
                transferType = event.transferType,
                sourceAccountId = event.sourceAccountId,
                recipientName = event.recipientName,
                recipientAccountNumber = event.recipientAccountNumber,
                bankName = event.bankName,
                amount = event.amount,
                currency = event.currency,
                note = event.note
            )
            when (val result = createTransferUseCase(params)) {
                is Result.Success -> _uiState.update {
                    it.copy(isSubmitting = false, transfers = listOf(result.data) + it.transfers)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.message)
                }
                Result.Loading -> Unit
            }
        }
    }
}