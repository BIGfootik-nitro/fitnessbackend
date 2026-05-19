package com.example.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(val username: String, val password: String, val role: String = "TRAINER")

@Serializable
data class AuthResponse(val token: String)
