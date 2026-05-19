package com.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClientRequest(
    val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val birthDate: String? = null
)

@Serializable
data class ClientResponse(
    val id: String,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val birthDate: String?
)
