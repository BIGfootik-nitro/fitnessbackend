package com.example.data.db.tables

import com.example.domain.model.UserRole
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Users : UUIDTable("users") {
    val username = text("username").uniqueIndex()
    val passwordHash = text("password_hash")
    val role = enumerationByName("role", 16, UserRole::class)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
