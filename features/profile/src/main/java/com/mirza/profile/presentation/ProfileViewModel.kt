package com.mirza.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.common.result.Result
import com.mirza.profile.domain.model.AppPreferences
import com.mirza.profile.domain.usecase.GetPreferencesUseCase
import com.mirza.profile.domain.usecase.GetUserProfileUseCase
import com.mirza.profile.domain.usecase.UpdatePreferencesUseCase
import com.mirza.profile.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getPreferencesUseCase: GetPreferencesUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val updatePreferencesUseCase: UpdatePreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        onEvent(ProfileEvent.LoadProfile)
        observePreferences()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.LoadProfile -> loadProfile()
            is ProfileEvent.UpdatePersonalDetails -> updatePersonalDetails(event)
            is ProfileEvent.ToggleBiometricLogin -> updatePreferences {
                it.copy(biometricLoginEnabled = event.enabled)
            }
            is ProfileEvent.ToggleDarkMode -> updatePreferences {
                it.copy(darkModeEnabled = event.enabled)
            }
            ProfileEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            getUserProfileUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, userProfile = result.data, errorMessage = null)
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
            getPreferencesUseCase(Unit).collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(preferences = result.data) }
                }
            }
        }
    }

    private fun updatePersonalDetails(event: ProfileEvent.UpdatePersonalDetails) {
        val currentProfile = _uiState.value.userProfile ?: return
        viewModelScope.launch {
            val updated = currentProfile.copy(
                fullName = event.fullName,
                phoneNumber = event.phoneNumber
            )
            val result = updateUserProfileUseCase(updated)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    private fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        viewModelScope.launch {
            val updated = transform(_uiState.value.preferences)
            val result = updatePreferencesUseCase(updated)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }
}