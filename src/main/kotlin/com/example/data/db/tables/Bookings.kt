package com.example.data.db.tables

import com.example.domain.model.BookingStatus
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp

object Bookings : UUIDTable("bookings") {
    val clientId = reference("client_id", Clients)
    val scheduledAt = timestamp("scheduled_at")
    val status = enumerationByName("status", 16, BookingStatus::class)
    val note = text("note").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
