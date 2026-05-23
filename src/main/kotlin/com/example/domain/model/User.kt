package com.example.domain.model

import java.util.UUID

enum class UserRole { ADMIN, TRAINER, CLIENT }

data class User(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val role: UserRole
)
