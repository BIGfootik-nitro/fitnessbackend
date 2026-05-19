package com.example.data.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object Clients : UUIDTable("clients") {
    val fullName = text("full_name")
    val phone = text("phone").nullable()
    val email = text("email").nullable()
    val birthDate = date("birth_date").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
