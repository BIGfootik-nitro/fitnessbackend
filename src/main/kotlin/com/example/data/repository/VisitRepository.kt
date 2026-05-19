package com.example.data.repository

import com.example.data.db.tables.Visits
import com.example.domain.model.Visit
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
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

    fun create(clientId: UUID, visitedAt: Instant, note: String?): UUID {
        return transaction {
            Visits.insert {
                it[Visits.clientId] = clientId
                it[Visits.visitedAt] = visitedAt
                it[Visits.note] = note
            }[Visits.id].value
        }
    }
}
