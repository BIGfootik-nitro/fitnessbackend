package com.example.domain.model

import java.time.Instant
import java.util.UUID

data class TrainingSession(
    val id: UUID,
    val title: String,
    val description: String?,
    val scheduledAt: Instant,
    val durationMin: Int,
    val trainerId: UUID?,
    val trainerName: String?,
    val maxCapacity: Int,
    val bookedCount: Int = 0
)
