package com.example.data.repository

import com.example.data.db.tables.Clients
import com.example.data.db.tables.Subscriptions
import com.example.data.db.tables.Visits
import com.example.domain.model.Client
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

class ClientRepository {

    fun getAll(search: String = ""): List<Client> {
        return transaction {
            val query = if (search.isBlank()) {
                Clients.selectAll()
            } else {
                Clients.selectAll().where {
                    Clients.fullName.lowerCase() like "%${search.lowercase()}%"
                }
            }
            query.orderBy(Clients.createdAt, SortOrder.DESC).map { it.toClient() }
        }
    }

    fun getById(id: UUID): Client? {
        return transaction {
            Clients.selectAll().where { Clients.id eq id }
                .singleOrNull()?.toClient()
        }
    }

    fun create(fullName: String, phone: String?, email: String?, birthDate: LocalDate?): UUID {
        return transaction {
            Clients.insert {
                it[Clients.fullName] = fullName
                it[Clients.phone] = phone
                it[Clients.email] = email
                it[Clients.birthDate] = birthDate
            }[Clients.id].value
        }
    }

    fun update(id: UUID, fullName: String, phone: String?, email: String?, birthDate: LocalDate?): Boolean {
        return transaction {
            Clients.update({ Clients.id eq id }) {
                it[Clients.fullName] = fullName
                it[Clients.phone] = phone
                it[Clients.email] = email
                it[Clients.birthDate] = birthDate
            } > 0
        }
    }

    fun delete(id: UUID): Boolean {
        return transaction {
            Visits.deleteWhere { Visits.clientId eq id }
            Subscriptions.deleteWhere { Subscriptions.clientId eq id }
            Clients.deleteWhere { Clients.id eq id } > 0
        }
    }

    private fun ResultRow.toClient() = Client(
        id = this[Clients.id].value,
        fullName = this[Clients.fullName],
        phone = this[Clients.phone],
        email = this[Clients.email],
        birthDate = this[Clients.birthDate]
    )
}
