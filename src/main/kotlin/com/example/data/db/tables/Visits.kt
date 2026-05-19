package com.example.data.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Visits : UUIDTable("visits") {
    val clientId = reference("client_id", Clients)
    val visitedAt = timestamp("visited_at")
    val note = text("note").nullable()
}
