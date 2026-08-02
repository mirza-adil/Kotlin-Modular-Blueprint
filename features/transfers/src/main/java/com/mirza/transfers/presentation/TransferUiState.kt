package com.mirza.transfers.presentation

import com.mirza.transfers.domain.model.Transfer

data class TransferUiState(
    val isLoading: Boolean = false,
    val transfers: List<Transfer> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)