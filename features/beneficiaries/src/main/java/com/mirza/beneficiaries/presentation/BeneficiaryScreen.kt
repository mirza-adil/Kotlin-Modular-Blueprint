package com.mirza.beneficiaries.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mirza.beneficiaries.domain.model.Beneficiary
import com.mirza.ui.components.ErrorView
import com.mirza.ui.components.FullScreenLoading

@Composable
fun BeneficiaryScreen(
    modifier: Modifier = Modifier,
    viewModel: BeneficiaryViewModel = hiltViewModel(),
    onBeneficiaryClick: (Beneficiary) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            uiState.isLoading && uiState.beneficiaries.isEmpty() -> FullScreenLoading(
                modifier = Modifier.padding(paddingValues),
                message = "Loading beneficiaries..."
            )

            uiState.errorMessage != null && uiState.beneficiaries.isEmpty() -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(BeneficiaryEvent.LoadBeneficiaries) }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(uiState.beneficiaries, key = { it.id }) { beneficiary ->
                    BeneficiaryListItem(
                        beneficiary = beneficiary,
                        onClick = { onBeneficiaryClick(beneficiary) },
                        onDelete = { viewModel.onEvent(BeneficiaryEvent.DeleteBeneficiary(beneficiary.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BeneficiaryListItem(
    beneficiary: Beneficiary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = beneficiary.nickname ?: beneficiary.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = beneficiary.bankName, style = MaterialTheme.typography.bodyMedium)
                Text(text = beneficiary.accountNumber, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete beneficiary")
            }
        }
    }
}