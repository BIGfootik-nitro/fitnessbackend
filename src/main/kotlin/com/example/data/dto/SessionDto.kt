package com.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionRequest(
    val title: String,
    val description: String? = null,
    val scheduledAt: String,
    val durationMin: Int = 60,
    val maxCapacity: Int = 10
)

@Serializable
data class SessionResponse(
    val id: String,
    val title: String,
    val description: String?,
    val scheduledAt: String,
    val durationMin: Int,
    val trainerName: String?,
    val maxCapacity: Int,
    val bookedCount: Int
)

@Serializable
data class SessionAttendeeResponse(
    val bookingId: String,
    val clientId: String,
    val clientName: String,
    val status: String
)
