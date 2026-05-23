package com.example.domain.model

import java.time.Instant
import java.util.UUID

enum class BookingStatus { PENDING, CONFIRMED, CANCELLED }

data class Booking(
    val id: UUID,
    val clientId: UUID,
    val scheduledAt: Instant,
    val status: BookingStatus,
    val note: String?
)
