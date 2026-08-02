package com.mirza.profile.presentation

import com.mirza.profile.domain.model.AppPreferences
import com.mirza.profile.domain.model.UserProfile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val preferences: AppPreferences = AppPreferences(),
    val errorMessage: String? = null
)