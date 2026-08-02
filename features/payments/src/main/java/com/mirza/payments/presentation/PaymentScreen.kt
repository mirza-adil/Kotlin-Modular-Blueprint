package com.mirza.payments.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mirza.payments.domain.model.Payment
import com.mirza.payments.domain.model.ScheduledPayment
import com.mirza.ui.components.ErrorView
import com.mirza.ui.components.FullScreenLoading

@Composable
fun PaymentScreen(
    modifier: Modifier = Modifier,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            uiState.isLoading && uiState.payments.isEmpty() -> FullScreenLoading(
                modifier = Modifier.padding(paddingValues),
                message = "Loading payments..."
            )

            uiState.errorMessage != null && uiState.payments.isEmpty() -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(PaymentEvent.LoadPayments) }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (uiState.scheduledPayments.isNotEmpty()) {
                    item {
                        Text(text = "Scheduled Payments", style = MaterialTheme.typography.titleMedium)
                    }
                    items(uiState.scheduledPayments, key = { it.id }) { scheduledPayment ->
                        ScheduledPaymentItem(
                            scheduledPayment = scheduledPayment,
                            onCancel = {
                                viewModel.onEvent(PaymentEvent.CancelScheduledPayment(scheduledPayment.id))
                            }
                        )
                    }
                }

                item {
                    Text(text = "Payment History", style = MaterialTheme.typography.titleMedium)
                }
                items(uiState.payments, key = { it.id }) { payment ->
                    PaymentItem(payment = payment)
                }
            }
        }
    }
}

@Composable
private fun PaymentItem(payment: Payment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = payment.billerName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${payment.currency} ${payment.amount}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = payment.status.name, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScheduledPaymentItem(
    scheduledPayment: ScheduledPayment,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = scheduledPayment.billerName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${scheduledPayment.currency} ${scheduledPayment.amount} · ${scheduledPayment.frequency.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onCancel) {
                Text(text = "Cancel")
            }
        }
    }
}