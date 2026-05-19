package com.example.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

enum class SubscriptionType { MONTHLY, QUARTERLY, ANNUAL }

data class Subscription(
    val id: UUID,
    val clientId: UUID,
    val type: SubscriptionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isFrozen: Boolean,
    val price: BigDecimal
)
