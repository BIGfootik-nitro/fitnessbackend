package com.example.data.repository

import com.example.data.db.tables.Bookings
import com.example.data.db.tables.TrainingSessions
import com.example.data.db.tables.Users
import com.example.domain.model.TrainingSession
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class TrainingSessionRepository {

    private fun buildQuery(extraWhere: Op<Boolean>? = null): Query {
        val trainerAlias = Users.alias("trainer")
        val q = TrainingSessions
            .join(trainerAlias, JoinType.LEFT, TrainingSessions.trainerId, trainerAlias[Users.id])
            .join(Bookings, JoinType.LEFT, TrainingSessions.id, Bookings.sessionId)
            .select(TrainingSessions.columns + trainerAlias[Users.username] + Bookings.id.count())

        val withWhere = if (extraWhere != null) q.where(extraWhere) else q

        return withWhere
            .groupBy(*TrainingSessions.columns.toTypedArray(), trainerAlias[Users.username])
            .orderBy(TrainingSessions.scheduledAt, SortOrder.ASC)
    }

    fun getAll(): List<TrainingSession> = transaction {
        val trainerAlias = Users.alias("trainer")
        TrainingSessions
            .join(trainerAlias, JoinType.LEFT, TrainingSessions.trainerId, trainerAlias[Users.id])
            .join(Bookings, JoinType.LEFT, TrainingSessions.id, Bookings.sessionId)
            .select(TrainingSessions.columns + trainerAlias[Users.username] + Bookings.id.count())
            .groupBy(*TrainingSessions.columns.toTypedArray(), trainerAlias[Users.username])
            .orderBy(TrainingSessions.scheduledAt, SortOrder.ASC)
            .map { it.toSession(trainerAlias) }
    }

    fun getUpcoming(): List<TrainingSession> = transaction {
        val trainerAlias = Users.alias("trainer")
        TrainingSessions
            .join(trainerAlias, JoinType.LEFT, TrainingSessions.trainerId, trainerAlias[Users.id])
            .join(Bookings, JoinType.LEFT, TrainingSessions.id, Bookings.sessionId)
            .select(TrainingSessions.columns + trainerAlias[Users.username] + Bookings.id.count())
            .where { TrainingSessions.scheduledAt greaterEq Instant.now() }
            .groupBy(*TrainingSessions.columns.toTypedArray(), trainerAlias[Users.username])
            .orderBy(TrainingSessions.scheduledAt, SortOrder.ASC)
            .map { it.toSession(trainerAlias) }
    }

    fun getById(id: UUID): TrainingSession? = transaction {
        val trainerAlias = Users.alias("trainer")
        TrainingSessions
            .join(trainerAlias, JoinType.LEFT, TrainingSessions.trainerId, trainerAlias[Users.id])
            .join(Bookings, JoinType.LEFT, TrainingSessions.id, Bookings.sessionId)
            .select(TrainingSessions.columns + trainerAlias[Users.username] + Bookings.id.count())
            .where { TrainingSessions.id eq id }
            .groupBy(*TrainingSessions.columns.toTypedArray(), trainerAlias[Users.username])
            .singleOrNull()?.toSession(trainerAlias)
    }

    fun create(title: String, description: String?, scheduledAt: Instant, durationMin: Int,
               trainerId: UUID?, maxCapacity: Int): UUID = transaction {
        TrainingSessions.insert {
            it[TrainingSessions.title] = title
            it[TrainingSessions.description] = description
            it[TrainingSessions.scheduledAt] = scheduledAt
            it[TrainingSessions.durationMin] = durationMin
            it[TrainingSessions.trainerId] = trainerId
            it[TrainingSessions.maxCapacity] = maxCapacity
        }[TrainingSessions.id].value
    }

    fun update(id: UUID, title: String, description: String?, scheduledAt: Instant,
               durationMin: Int, maxCapacity: Int): Boolean = transaction {
        TrainingSessions.update({ TrainingSessions.id eq id }) {
            it[TrainingSessions.title] = title
            it[TrainingSessions.description] = description
            it[TrainingSessions.scheduledAt] = scheduledAt
            it[TrainingSessions.durationMin] = durationMin
            it[TrainingSessions.maxCapacity] = maxCapacity
        } > 0
    }

    fun delete(id: UUID): Boolean = transaction {
        Bookings.update({ Bookings.sessionId eq id }) { it[sessionId] = null }
        TrainingSessions.deleteWhere { TrainingSessions.id eq id } > 0
    }

    private fun ResultRow.toSession(trainerAlias: Alias<Users>): TrainingSession {
        val booked = try { this[Bookings.id.count()].toInt() } catch (_: Exception) { 0 }
        return TrainingSession(
            id = this[TrainingSessions.id].value,
            title = this[TrainingSessions.title],
            description = this[TrainingSessions.description],
            scheduledAt = this[TrainingSessions.scheduledAt],
            durationMin = this[TrainingSessions.durationMin],
            trainerId = this[TrainingSessions.trainerId]?.value,
            trainerName = try { this[trainerAlias[Users.username]] } catch (_: Exception) { null },
            maxCapacity = this[TrainingSessions.maxCapacity],
            bookedCount = booked
        )
    }
}
