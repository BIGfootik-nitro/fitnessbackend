package com.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val userId: String,
    val username: String,
    val role: String,
    val client: ClientResponse? = null
)
