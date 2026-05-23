package com.example.data.repository

import com.example.data.db.tables.Users
import com.example.domain.model.User
import com.example.domain.model.UserRole
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class UserRepository {

    fun create(username: String, passwordHash: String, role: UserRole): UUID {
        return transaction {
            Users.insert {
                it[Users.username] = username
                it[Users.passwordHash] = passwordHash
                it[Users.role] = role
            }[Users.id].value
        }
    }

    fun findByUsername(username: String): User? {
        return transaction {
            Users.selectAll().where { Users.username eq username }
                .singleOrNull()?.toUser()
        }
    }

    fun findById(id: UUID): User? {
        return transaction {
            Users.selectAll().where { Users.id eq id }
                .singleOrNull()?.toUser()
        }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toUser() = User(
        id = this[Users.id].value,
        username = this[Users.username],
        passwordHash = this[Users.passwordHash],
        role = this[Users.role]
    )

    fun usernameExists(username: String): Boolean {
        return transaction {
            Users.selectAll().where { Users.username eq username }.count() > 0
        }
    }
}
