package com.example.data.repository

import com.example.data.db.tables.Notifications
import com.example.domain.model.Notification
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class NotificationRepository {

    fun create(userId: UUID, title: String, body: String): UUID = transaction {
        Notifications.insert {
            it[Notifications.userId] = userId
            it[Notifications.title] = title
            it[Notifications.body] = body
        }[Notifications.id].value
    }

    fun getByUserId(userId: UUID): List<Notification> = transaction {
        Notifications.selectAll().where { Notifications.userId eq userId }
            .orderBy(Notifications.createdAt, SortOrder.DESC)
            .map { it.toNotification() }
    }

    fun markRead(id: UUID, userId: UUID): Boolean = transaction {
        Notifications.update({ (Notifications.id eq id) and (Notifications.userId eq userId) }) {
            it[Notifications.read] = true
        } > 0
    }

    private fun ResultRow.toNotification() = Notification(
        id = this[Notifications.id].value,
        userId = this[Notifications.userId].value,
        title = this[Notifications.title],
        body = this[Notifications.body],
        read = this[Notifications.read],
        createdAt = this[Notifications.createdAt]
    )
}
