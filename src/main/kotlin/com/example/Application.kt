package com.example

import com.example.data.db.DatabaseFactory
import com.example.data.db.Seeder
import com.example.plugins.*
import io.ktor.server.application.*

fun Application.module() {
    DatabaseFactory.init(environment.config)
    Seeder.seedIfEmpty()
    configureSerialization()
    configureAuth()
    configureStatusPages()
    configureRouting()
}
