package com.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookingRequest(val scheduledAt: String, val note: String? = null)

@Serializable
data class BookingResponse(
    val id: String,
    val clientId: String,
    val clientName: String? = null,
    val scheduledAt: String,
    val status: String,
    val note: String?
)

@Serializable
data class BookingStatusUpdate(val status: String)
