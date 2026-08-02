package com.mirza.accounts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.mirza.accounts.domain.model.Account
import com.mirza.ui.components.ErrorView
import com.mirza.ui.components.FullScreenLoading

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
    onAccountClick: (Account) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            uiState.isLoading && uiState.accounts.isEmpty() -> FullScreenLoading(
                modifier = Modifier.padding(paddingValues),
                message = "Loading accounts..."
            )

            uiState.errorMessage != null && uiState.accounts.isEmpty() -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(AccountEvent.LoadAccounts) }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(uiState.accounts, key = { it.id }) { account ->
                    AccountListItem(account = account, onClick = { onAccountClick(account) })
                }
            }
        }
    }
}

@Composable
private fun AccountListItem(
    account: Account,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = account.accountHolderName, style = MaterialTheme.typography.titleMedium)
            Text(text = account.accountNumber, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${account.currency} ${account.balance}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}