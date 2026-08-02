package com.mirza.profile.domain.model

data class AppPreferences(
    val biometricLoginEnabled: Boolean = false,
    val darkModeEnabled: Boolean = false,
    val language: String = "en"
)