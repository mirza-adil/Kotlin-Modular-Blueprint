package com.mirza.profile.data.mapper

import com.mirza.profile.data.datasource.AppPreferencesDto
import com.mirza.profile.data.datasource.UserProfileDto
import com.mirza.profile.domain.model.AppPreferences
import com.mirza.profile.domain.model.UserProfile

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    userId = userId,
    fullName = fullName,
    email = email,
    phoneNumber = phoneNumber,
    avatarUrl = avatarUrl
)

fun UserProfile.toDto(): UserProfileDto = UserProfileDto(
    userId = userId,
    fullName = fullName,
    email = email,
    phoneNumber = phoneNumber,
    avatarUrl = avatarUrl
)

fun AppPreferencesDto.toDomain(): AppPreferences = AppPreferences(
    biometricLoginEnabled = biometricLoginEnabled,
    darkModeEnabled = darkModeEnabled,
    language = language
)

fun AppPreferences.toDto(): AppPreferencesDto = AppPreferencesDto(
    biometricLoginEnabled = biometricLoginEnabled,
    darkModeEnabled = darkModeEnabled,
    language = language
)