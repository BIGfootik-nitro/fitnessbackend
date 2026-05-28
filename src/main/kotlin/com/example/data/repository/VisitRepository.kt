package com.example.data.repository

import com.example.data.db.tables.Visits
import com.example.domain.model.Visit
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class VisitRepository {

    fun getByClientId(clientId: UUID): List<Visit> {
        return transaction {
            Visits.selectAll().where { Visits.clientId eq clientId }
                .orderBy(Visits.visitedAt, SortOrder.DESC)
                .map {
                    Visit(
                        id = it[Visits.id].value,
                        clientId = it[Visits.clientId].value,
                        visitedAt = it[Visits.visitedAt],
                        note = it[Visits.note]
                    )
                }
        }
    }

    fun create(clientId: UUID, visitedAt: Instant, note: String?): UUID = transaction {
        Visits.insert {
            it[Visits.clientId] = clientId
            it[Visits.visitedAt] = visitedAt
            it[Visits.note] = note
        }[Visits.id].value
    }

    fun delete(id: UUID): Boolean = transaction {
        Visits.deleteWhere { Visits.id eq id } > 0
    }

    fun getAll(): List<Visit> = transaction {
        Visits.selectAll().orderBy(Visits.visitedAt, SortOrder.DESC)
            .map { Visit(it[Visits.id].value, it[Visits.clientId].value, it[Visits.visitedAt], it[Visits.note]) }
    }
}
