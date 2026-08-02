package com.mirza.payments.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.common.result.Result
import com.mirza.payments.domain.model.ScheduledPayment
import com.mirza.payments.domain.usecase.CancelScheduledPaymentUseCase
import com.mirza.payments.domain.usecase.GetPaymentsUseCase
import com.mirza.payments.domain.usecase.GetScheduledPaymentsUseCase
import com.mirza.payments.domain.usecase.PayBillUseCase
import com.mirza.payments.domain.usecase.ScheduleBillPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val getPaymentsUseCase: GetPaymentsUseCase,
    private val getScheduledPaymentsUseCase: GetScheduledPaymentsUseCase,
    private val payBillUseCase: PayBillUseCase,
    private val scheduleBillPaymentUseCase: ScheduleBillPaymentUseCase,
    private val cancelScheduledPaymentUseCase: CancelScheduledPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        onEvent(PaymentEvent.LoadPayments)
        observeScheduledPayments()
    }

    fun onEvent(event: PaymentEvent) {
        when (event) {
            PaymentEvent.LoadPayments -> loadPayments()
            is PaymentEvent.PayBill -> payBill(event)
            is PaymentEvent.ScheduleBillPayment -> scheduleBillPayment(event.scheduledPayment)
            is PaymentEvent.CancelScheduledPayment -> cancelScheduledPayment(event.scheduledPaymentId)
            PaymentEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadPayments() {
        viewModelScope.launch {
            getPaymentsUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, payments = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun observeScheduledPayments() {
        viewModelScope.launch {
            getScheduledPaymentsUseCase(Unit).collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(scheduledPayments = result.data) }
                }
            }
        }
    }

    private fun payBill(event: PaymentEvent.PayBill) {
        viewModelScope.launch {
            val params = PayBillUseCase.Params(
                billerName = event.billerName,
                category = event.category,
                amount = event.amount,
                currency = event.currency
            )
            val result = payBillUseCase(params)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    private fun scheduleBillPayment(scheduledPayment: ScheduledPayment) {
        viewModelScope.launch {
            val result = scheduleBillPaymentUseCase(scheduledPayment)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    private fun cancelScheduledPayment(scheduledPaymentId: String) {
        viewModelScope.launch {
            val result = cancelScheduledPaymentUseCase(scheduledPaymentId)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }
}