package com.mirza.notifications.presentation

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mirza.notifications.domain.model.NotificationItem
import com.mirza.ui.components.ErrorView
import com.mirza.ui.components.FullScreenLoading

@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> FullScreenLoading(
                modifier = Modifier.padding(paddingValues),
                message = "Loading notifications..."
            )

            uiState.errorMessage != null && uiState.notifications.isEmpty() -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(NotificationEvent.LoadNotifications) }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    PreferencesSection(
                        pushEnabled = uiState.preferences.pushEnabled,
                        transactionAlerts = uiState.preferences.transactionAlerts,
                        securityAlerts = uiState.preferences.securityAlerts,
                        promotions = uiState.preferences.promotions,
                        onPushToggle = { viewModel.onEvent(NotificationEvent.TogglePushEnabled(it)) },
                        onTransactionToggle = {
                            viewModel.onEvent(NotificationEvent.ToggleTransactionAlerts(it))
                        },
                        onSecurityToggle = { viewModel.onEvent(NotificationEvent.ToggleSecurityAlerts(it)) },
                        onPromotionsToggle = { viewModel.onEvent(NotificationEvent.TogglePromotions(it)) }
                    )
                }

                items(uiState.notifications, key = { it.id }) { notification ->
                    NotificationListItem(
                        notification = notification,
                        onClick = { viewModel.onEvent(NotificationEvent.MarkAsRead(notification.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferencesSection(
    pushEnabled: Boolean,
    transactionAlerts: Boolean,
    securityAlerts: Boolean,
    promotions: Boolean,
    onPushToggle: (Boolean) -> Unit,
    onTransactionToggle: (Boolean) -> Unit,
    onSecurityToggle: (Boolean) -> Unit,
    onPromotionsToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Preferences", style = MaterialTheme.typography.titleMedium)
            PreferenceRow("Push notifications", pushEnabled, onPushToggle)
            PreferenceRow("Transaction alerts", transactionAlerts, onTransactionToggle)
            PreferenceRow("Security alerts", securityAlerts, onSecurityToggle)
            PreferenceRow("Promotions", promotions, onPromotionsToggle)
        }
    }
}

@Composable
private fun PreferenceRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NotificationListItem(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = notification.title,
                style = if (notification.isRead) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.titleMedium
                }
            )
            Text(text = notification.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}