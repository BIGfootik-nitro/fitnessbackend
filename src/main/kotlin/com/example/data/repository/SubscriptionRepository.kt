package com.example.data.repository

import com.example.data.db.tables.Subscriptions
import com.example.domain.model.Subscription
import com.example.domain.model.SubscriptionType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class SubscriptionRepository {

    fun getByClientId(clientId: UUID): List<Subscription> {
        return transaction {
            Subscriptions.selectAll().where { Subscriptions.clientId eq clientId }
                .orderBy(Subscriptions.startDate, SortOrder.DESC)
                .map { it.toSubscription() }
        }
    }

    fun getById(id: UUID): Subscription? {
        return transaction {
            Subscriptions.selectAll().where { Subscriptions.id eq id }
                .singleOrNull()?.toSubscription()
        }
    }

    fun create(clientId: UUID, type: SubscriptionType, startDate: LocalDate, endDate: LocalDate, price: BigDecimal): UUID {
        return transaction {
            Subscriptions.insert {
                it[Subscriptions.clientId] = clientId
                it[Subscriptions.type] = type
                it[Subscriptions.startDate] = startDate
                it[Subscriptions.endDate] = endDate
                it[Subscriptions.price] = price
            }[Subscriptions.id].value
        }
    }

    fun setFrozen(id: UUID, frozen: Boolean): Boolean {
        return transaction {
            Subscriptions.update({ Subscriptions.id eq id }) {
                it[isFrozen] = frozen
            } > 0
        }
    }

    private fun ResultRow.toSubscription() = Subscription(
        id = this[Subscriptions.id].value,
        clientId = this[Subscriptions.clientId].value,
        type = this[Subscriptions.type],
        startDate = this[Subscriptions.startDate],
        endDate = this[Subscriptions.endDate],
        isFrozen = this[Subscriptions.isFrozen],
        price = this[Subscriptions.price]
    )
}
