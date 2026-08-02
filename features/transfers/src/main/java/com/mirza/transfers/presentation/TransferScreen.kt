package com.mirza.transfers.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mirza.transfers.domain.model.Transfer
import com.mirza.ui.components.ErrorView
import com.mirza.ui.components.FullScreenLoading

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            uiState.isLoading && uiState.transfers.isEmpty() -> FullScreenLoading(
                modifier = Modifier.padding(paddingValues),
                message = "Loading transfers..."
            )

            uiState.errorMessage != null && uiState.transfers.isEmpty() -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(TransferEvent.LoadTransfers) }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(text = "Recent Transfers", style = MaterialTheme.typography.titleMedium)
                }
                items(uiState.transfers, key = { it.id }) { transfer ->
                    TransferListItem(transfer = transfer)
                }
            }
        }
    }
}

@Composable
private fun TransferListItem(transfer: Transfer) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = transfer.recipientName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${transfer.currency} ${transfer.amount} · ${transfer.transferType.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = transfer.status.name, style = MaterialTheme.typography.bodySmall)
        }
    }
}