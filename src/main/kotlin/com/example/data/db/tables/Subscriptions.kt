package com.example.data.db.tables

import com.example.domain.model.SubscriptionType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date

object Subscriptions : UUIDTable("subscriptions") {
    val clientId = reference("client_id", Clients)
    val type = enumerationByName("type", 16, SubscriptionType::class)
    val startDate = date("start_date")
    val endDate = date("end_date")
    val isFrozen = bool("is_frozen").default(false)
    val price = decimal("price", 10, 2)
}
