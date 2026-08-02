package com.mirza.profile.data.datasource

data class UserProfileDto(
    val userId: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val avatarUrl: String?
)

data class AppPreferencesDto(
    val biometricLoginEnabled: Boolean,
    val darkModeEnabled: Boolean,
    val language: String
)