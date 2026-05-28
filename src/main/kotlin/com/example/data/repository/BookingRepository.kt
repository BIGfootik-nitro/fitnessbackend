package com.example.data.repository

import com.example.data.db.tables.Bookings
import com.example.data.db.tables.Clients
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class BookingRepository {

    fun create(clientId: UUID, scheduledAt: Instant, note: String?, sessionId: UUID? = null): UUID = transaction {
        Bookings.insert {
            it[Bookings.clientId] = clientId
            it[Bookings.sessionId] = sessionId
            it[Bookings.scheduledAt] = scheduledAt
            it[Bookings.status] = BookingStatus.PENDING
            it[Bookings.note] = note
        }[Bookings.id].value
    }

    fun getBySessionId(sessionId: UUID): List<Pair<Booking, String>> = transaction {
        (Bookings innerJoin Clients).selectAll()
            .where { Bookings.sessionId eq sessionId }
            .orderBy(Bookings.createdAt, SortOrder.ASC)
            .map { row -> row.toBooking() to row[Clients.fullName] }
    }

    fun alreadyBooked(clientId: UUID, sessionId: UUID): Boolean = transaction {
        Bookings.selectAll()
            .where { (Bookings.clientId eq clientId) and (Bookings.sessionId eq sessionId) }
            .any()
    }

    fun getById(id: UUID): Booking? = transaction {
        Bookings.selectAll().where { Bookings.id eq id }.singleOrNull()?.toBooking()
    }

    fun getByClientId(clientId: UUID): List<Booking> = transaction {
        Bookings.selectAll().where { Bookings.clientId eq clientId }
            .orderBy(Bookings.scheduledAt, SortOrder.DESC)
            .map { it.toBooking() }
    }

    fun getAll(): List<Pair<Booking, String>> = transaction {
        (Bookings innerJoin Clients).selectAll()
            .orderBy(Bookings.scheduledAt, SortOrder.DESC)
            .map { row ->
                row.toBooking() to row[Clients.fullName]
            }
    }

    fun setStatus(id: UUID, status: BookingStatus): Boolean = transaction {
        Bookings.update({ Bookings.id eq id }) {
            it[Bookings.status] = status
        } > 0
    }

    private fun ResultRow.toBooking() = Booking(
        id = this[Bookings.id].value,
        clientId = this[Bookings.clientId].value,
        scheduledAt = this[Bookings.scheduledAt],
        status = this[Bookings.status],
        note = this[Bookings.note]
    )
}
