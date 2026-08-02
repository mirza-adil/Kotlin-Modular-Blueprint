package com.mirza.beneficiaries.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.beneficiaries.domain.usecase.DeleteBeneficiaryUseCase
import com.mirza.beneficiaries.domain.usecase.GetBeneficiariesUseCase
import com.mirza.beneficiaries.domain.usecase.GetBeneficiaryByIdUseCase
import com.mirza.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeneficiaryViewModel @Inject constructor(
    private val getBeneficiariesUseCase: GetBeneficiariesUseCase,
    private val getBeneficiaryByIdUseCase: GetBeneficiaryByIdUseCase,
    private val deleteBeneficiaryUseCase: DeleteBeneficiaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BeneficiaryUiState())
    val uiState: StateFlow<BeneficiaryUiState> = _uiState.asStateFlow()

    init {
        onEvent(BeneficiaryEvent.LoadBeneficiaries)
    }

    fun onEvent(event: BeneficiaryEvent) {
        when (event) {
            BeneficiaryEvent.LoadBeneficiaries -> loadBeneficiaries()
            is BeneficiaryEvent.SelectBeneficiary -> selectBeneficiary(event.beneficiaryId)
            is BeneficiaryEvent.DeleteBeneficiary -> deleteBeneficiary(event.beneficiaryId)
            BeneficiaryEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadBeneficiaries() {
        viewModelScope.launch {
            getBeneficiariesUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, beneficiaries = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun selectBeneficiary(beneficiaryId: String) {
        viewModelScope.launch {
            when (val result = getBeneficiaryByIdUseCase(beneficiaryId)) {
                is Result.Success -> _uiState.update { it.copy(selectedBeneficiary = result.data) }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    private fun deleteBeneficiary(beneficiaryId: String) {
        viewModelScope.launch {
            when (val result = deleteBeneficiaryUseCase(beneficiaryId)) {
                is Result.Success -> _uiState.update {
                    it.copy(beneficiaries = it.beneficiaries.filterNot { b -> b.id == beneficiaryId })
                }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }
}