package com.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class VisitRequest(
    val visitedAt: String,
    val note: String? = null
)

@Serializable
data class VisitResponse(
    val id: String,
    val clientId: String,
    val visitedAt: String,
    val note: String?
)
