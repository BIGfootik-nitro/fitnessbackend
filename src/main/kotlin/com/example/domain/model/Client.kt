package com.example.domain.model

import java.time.LocalDate
import java.util.UUID

data class Client(
    val id: UUID,
    val userId: UUID?,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val birthDate: LocalDate?
)
