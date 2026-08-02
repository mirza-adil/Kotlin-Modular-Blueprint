package com.mirza.profile.domain.model

data class UserProfile(
    val userId: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val avatarUrl: String?
)