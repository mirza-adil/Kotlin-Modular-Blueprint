package com.mirza.profile.data.datasource

import com.mirza.security.di.SecureTokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileLocalDataSource {

    fun observeUserProfile(): Flow<UserProfileDto?>

    fun observePreferences(): Flow<AppPreferencesDto>

    suspend fun cacheUserProfile(profile: UserProfileDto)

    suspend fun savePreferences(preferences: AppPreferencesDto)
}

@Singleton
class ProfileLocalDataSourceImpl @Inject constructor(
    private val secureTokenManager: SecureTokenManager
) : ProfileLocalDataSource {

    private val cachedProfile = MutableStateFlow(loadProfileFromSession())
    private val cachedPreferences = MutableStateFlow(
        AppPreferencesDto(
            biometricLoginEnabled = false,
            darkModeEnabled = false,
            language = "en"
        )
    )

    override fun observeUserProfile(): Flow<UserProfileDto?> = cachedProfile.asStateFlow()

    override fun observePreferences(): Flow<AppPreferencesDto> = cachedPreferences.asStateFlow()

    override suspend fun cacheUserProfile(profile: UserProfileDto) {
        cachedProfile.value = profile
    }

    override suspend fun savePreferences(preferences: AppPreferencesDto) {
        cachedPreferences.value = preferences
    }

    private fun loadProfileFromSession(): UserProfileDto? {
        val userId = secureTokenManager.getUserId() ?: return null
        val email = secureTokenManager.getUserEmail() ?: return null
        val name = secureTokenManager.getUserName() ?: return null
        return UserProfileDto(
            userId = userId,
            fullName = name,
            email = email,
            phoneNumber = null,
            avatarUrl = null
        )
    }
}