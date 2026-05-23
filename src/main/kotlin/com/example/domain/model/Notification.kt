package com.example.domain.model

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: UUID,
    val userId: UUID,
    val title: String,
    val body: String,
    val read: Boolean,
    val createdAt: Instant
)
