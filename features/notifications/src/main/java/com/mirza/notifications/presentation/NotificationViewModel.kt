package com.mirza.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.common.result.Result
import com.mirza.notifications.domain.model.NotificationPreferences
import com.mirza.notifications.domain.usecase.GetNotificationPreferencesUseCase
import com.mirza.notifications.domain.usecase.GetNotificationsUseCase
import com.mirza.notifications.domain.usecase.MarkNotificationAsReadUseCase
import com.mirza.notifications.domain.usecase.UpdateNotificationPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getNotificationPreferencesUseCase: GetNotificationPreferencesUseCase,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase,
    private val updateNotificationPreferencesUseCase: UpdateNotificationPreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        onEvent(NotificationEvent.LoadNotifications)
        observePreferences()
    }

    fun onEvent(event: NotificationEvent) {
        when (event) {
            NotificationEvent.LoadNotifications -> loadNotifications()
            is NotificationEvent.MarkAsRead -> markAsRead(event.notificationId)
            is NotificationEvent.TogglePushEnabled -> updatePreferences {
                it.copy(pushEnabled = event.enabled)
            }
            is NotificationEvent.ToggleTransactionAlerts -> updatePreferences {
                it.copy(transactionAlerts = event.enabled)
            }
            is NotificationEvent.ToggleSecurityAlerts -> updatePreferences {
                it.copy(securityAlerts = event.enabled)
            }
            is NotificationEvent.TogglePromotions -> updatePreferences {
                it.copy(promotions = event.enabled)
            }
            NotificationEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            getNotificationsUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, notifications = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            getNotificationPreferencesUseCase(Unit).collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(preferences = result.data) }
                }
            }
        }
    }

    private fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val result = markNotificationAsReadUseCase(notificationId)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    private fun updatePreferences(transform: (NotificationPreferences) -> NotificationPreferences) {
        viewModelScope.launch {
            val updated = transform(_uiState.value.preferences)
            val result = updateNotificationPreferencesUseCase(updated)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }
}