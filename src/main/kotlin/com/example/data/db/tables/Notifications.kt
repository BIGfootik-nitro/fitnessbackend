package com.example.data.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Notifications : UUIDTable("notifications") {
    val userId = reference("user_id", Users)
    val title = text("title")
    val body = text("body")
    val read = bool("is_read").default(false)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}
