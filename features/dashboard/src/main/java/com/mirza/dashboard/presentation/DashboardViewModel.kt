package com.mirza.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.common.result.Result
import com.mirza.dashboard.domain.usecase.GetDashboardSummaryUseCase
import com.mirza.dashboard.domain.usecase.RefreshDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val refreshDashboardSummaryUseCase: RefreshDashboardSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        onEvent(DashboardEvent.LoadSummary)
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.LoadSummary -> loadSummary()
            DashboardEvent.Refresh -> refresh()
            DashboardEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadSummary() {
        viewModelScope.launch {
            getDashboardSummaryUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, summary = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            when (val result = refreshDashboardSummaryUseCase()) {
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }
}