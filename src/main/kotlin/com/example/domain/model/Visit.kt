package com.example.domain.model

import java.time.Instant
import java.util.UUID

data class Visit(
    val id: UUID,
    val clientId: UUID,
    val visitedAt: Instant,
    val note: String?
)
