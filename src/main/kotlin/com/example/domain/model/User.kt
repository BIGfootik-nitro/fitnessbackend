package com.example.domain.model

import java.util.UUID

enum class UserRole { ADMIN, TRAINER }

data class User(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val role: UserRole
)
