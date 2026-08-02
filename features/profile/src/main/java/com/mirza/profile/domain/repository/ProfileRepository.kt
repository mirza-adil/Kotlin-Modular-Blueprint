package com.mirza.profile.domain.repository

import com.mirza.common.result.Result
import com.mirza.profile.domain.model.AppPreferences
import com.mirza.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {

    fun getUserProfile(): Flow<Result<UserProfile>>

    fun getPreferences(): Flow<Result<AppPreferences>>

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit>

    suspend fun updatePreferences(preferences: AppPreferences): Result<Unit>

    suspend fun refreshUserProfile(): Result<Unit>
}