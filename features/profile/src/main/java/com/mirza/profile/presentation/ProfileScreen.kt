package com.mirza.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mirza.ui.components.ErrorView
import com.mirza.ui.components.FullScreenLoading

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            uiState.isLoading && uiState.userProfile == null -> FullScreenLoading(
                modifier = Modifier.padding(paddingValues),
                message = "Loading profile..."
            )

            uiState.errorMessage != null && uiState.userProfile == null -> ErrorView(
                modifier = Modifier.padding(paddingValues),
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(ProfileEvent.LoadProfile) }
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    uiState.userProfile?.let { profile ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = profile.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(text = profile.email, style = MaterialTheme.typography.bodyMedium)
                                profile.phoneNumber?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Preferences", style = MaterialTheme.typography.titleMedium)
                            PreferenceRow(
                                label = "Biometric login",
                                checked = uiState.preferences.biometricLoginEnabled,
                                onCheckedChange = {
                                    viewModel.onEvent(ProfileEvent.ToggleBiometricLogin(it))
                                }
                            )
                            PreferenceRow(
                                label = "Dark mode",
                                checked = uiState.preferences.darkModeEnabled,
                                onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleDarkMode(it)) }
                            )
                        }
                    }
                }
            }
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