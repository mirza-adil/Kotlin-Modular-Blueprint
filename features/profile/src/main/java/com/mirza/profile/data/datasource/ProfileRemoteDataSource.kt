package com.mirza.profile.data.datasource

import javax.inject.Inject

interface ProfileRemoteDataSource {

    suspend fun getUserProfile(): UserProfileDto

    suspend fun updateUserProfile(profile: UserProfileDto)
}

class ProfileRemoteDataSourceImpl @Inject constructor() : ProfileRemoteDataSource {

    override suspend fun getUserProfile(): UserProfileDto {
        throw UnsupportedOperationException(
            "No remote profile endpoint available yet; relies on the locally cached session"
        )
    }

    override suspend fun updateUserProfile(profile: UserProfileDto) {
        // TODO: wire up to the real profile endpoint once it is available
    }
}