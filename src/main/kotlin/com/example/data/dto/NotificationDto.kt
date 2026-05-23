package com.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: String,
    val title: String,
    val body: String,
    val read: Boolean,
    val createdAt: String
)
