package com.example.data.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp

object TrainingSessions : UUIDTable("training_sessions") {
    val title       = varchar("title", 200)
    val description = text("description").nullable()
    val scheduledAt = timestamp("scheduled_at")
    val durationMin = integer("duration_min").default(60)
    val trainerId   = reference("trainer_id", Users).nullable()
    val maxCapacity = integer("max_capacity").default(10)
    val createdAt   = datetime("created_at").defaultExpression(CurrentDateTime)
}
