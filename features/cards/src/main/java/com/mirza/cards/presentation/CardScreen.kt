package com.mirza.cards.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mirza.cards.domain.model.Card
import com.mirza.cards.domain.model.CardStatus
import com.mirza.ui.components.ErrorView
import com.mirza.ui.components.FullScreenLoading

@Composable
fun CardScreen(
    modifier: Modifier = Modifier,
    viewModel: CardViewModel = hiltViewModel(),
    onCardClick: (Card) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            uiState.isLoading && uiState.cards.isEmpty() -> FullScreenLoading(
                modifier = Modifier.padding(paddingValues),
                message = "Loading cards..."
            )

            uiState.errorMessage != null && uiState.cards.isEmpty() -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(CardEvent.LoadCards) }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(uiState.cards, key = { it.id }) { card ->
                    CardListItem(
                        card = card,
                        onClick = { onCardClick(card) },
                        onFreezeToggle = { frozen ->
                            viewModel.onEvent(CardEvent.ToggleFreeze(card.id, frozen))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardListItem(
    card: Card,
    onClick: () -> Unit,
    onFreezeToggle: (Boolean) -> Unit
) {
    MaterialCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = card.cardHolderName, style = MaterialTheme.typography.titleMedium)
                Text(text = card.maskedNumber, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${card.cardType.name} · ${card.status.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = card.status == CardStatus.FROZEN,
                onCheckedChange = onFreezeToggle
            )
        }
    }
}