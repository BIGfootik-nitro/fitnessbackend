package com.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionRequest(
    val type: String,
    val startDate: String,
    val endDate: String,
    val price: String
)

@Serializable
data class SubscriptionResponse(
    val id: String,
    val clientId: String,
    val type: String,
    val startDate: String,
    val endDate: String,
    val isFrozen: Boolean,
    val price: String
)
