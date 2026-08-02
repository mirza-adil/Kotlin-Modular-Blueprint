package com.mirza.dashboard.presentation

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mirza.dashboard.domain.model.AccountSummary
import com.mirza.dashboard.domain.model.RecentTransaction
import com.mirza.ui.components.ErrorView
import com.mirza.ui.components.FullScreenLoading

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            uiState.isLoading && uiState.summary == null -> FullScreenLoading(
                modifier = Modifier.padding(paddingValues),
                message = "Loading dashboard..."
            )

            uiState.errorMessage != null && uiState.summary == null -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(DashboardEvent.LoadSummary) }
            )

            uiState.summary != null -> {
                val summary = uiState.summary!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        TotalBalanceCard(
                            totalBalance = summary.totalBalance,
                            currency = summary.currency
                        )
                    }

                    items(summary.accountSummaries, key = { it.accountId }) { account ->
                        AccountSummaryItem(account = account)
                    }

                    items(summary.recentTransactions, key = { it.id }) { transaction ->
                        RecentTransactionItem(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(totalBalance: Double, currency: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(text = "Total Balance", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$currency $totalBalance",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun AccountSummaryItem(account: AccountSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = account.accountName, style = MaterialTheme.typography.titleMedium)
            Text(text = account.maskedAccountNumber, style = MaterialTheme.typography.bodyMedium)
            Text(text = account.balance.toString(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RecentTransactionItem(transaction: RecentTransaction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = transaction.title, style = MaterialTheme.typography.titleSmall)
            Text(text = transaction.date, style = MaterialTheme.typography.bodySmall)
            Text(
                text = if (transaction.isCredit) "+${transaction.amount}" else "-${transaction.amount}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (transaction.isCredit) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}