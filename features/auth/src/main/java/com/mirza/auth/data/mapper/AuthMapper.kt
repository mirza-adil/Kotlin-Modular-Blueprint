package com.mirza.auth.data.mapper

import com.mirza.auth.domain.model.User
import model.UserDto

fun UserDto.toDomain(): User = User(
    id = id,
    email = email,
    name = name,
    avatarUrl = avatarUrl
)