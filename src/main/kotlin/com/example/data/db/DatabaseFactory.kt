package com.example.data.db

import com.example.data.db.tables.Bookings
import com.example.data.db.tables.Clients
import com.example.data.db.tables.Notifications
import com.example.data.db.tables.Subscriptions
import com.example.data.db.tables.Users
import com.example.data.db.tables.Visits
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.property("database.url").getString()
            driverClassName = config.property("database.driver").getString()
            username = config.property("database.user").getString()
            password = config.property("database.password").getString()
            maximumPoolSize = 10
        }
        Database.connect(HikariDataSource(hikariConfig))
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Users, Clients, Subscriptions, Visits, Bookings, Notifications)
        }
    }
}
