package com.mirza.profile.presentation

sealed interface ProfileEvent {
    data object LoadProfile : ProfileEvent
    data class UpdatePersonalDetails(val fullName: String, val phoneNumber: String?) : ProfileEvent
    data class ToggleBiometricLogin(val enabled: Boolean) : ProfileEvent
    data class ToggleDarkMode(val enabled: Boolean) : ProfileEvent
    data object DismissError : ProfileEvent
}