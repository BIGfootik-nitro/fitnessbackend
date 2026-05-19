package com.example.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.data.dto.*
import com.example.data.repository.*
import com.example.domain.model.SubscriptionType
import com.example.domain.model.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

fun Application.configureRouting() {
    val userRepo = UserRepository()
    val clientRepo = ClientRepository()
    val subRepo = SubscriptionRepository()
    val visitRepo = VisitRepository()

    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val expiry = environment.config.property("jwt.expiry").getString().toLong()

    routing {
        post("/auth/register") {
            val req = call.receive<RegisterRequest>()
            if (req.username.isBlank() || req.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Username and password required"))
                return@post
            }
            if (userRepo.usernameExists(req.username)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Username already taken"))
                return@post
            }
            val role = runCatching { UserRole.valueOf(req.role.uppercase()) }.getOrDefault(UserRole.TRAINER)
            val hash = BCrypt.hashpw(req.password, BCrypt.gensalt())
            userRepo.create(req.username, hash, role)
            call.respond(HttpStatusCode.Created, mapOf("message" to "User created"))
        }

        post("/auth/login") {
            val req = call.receive<LoginRequest>()
            val user = userRepo.findByUsername(req.username)
            if (user == null || !BCrypt.checkpw(req.password, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }
            val token = JWT.create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim("userId", user.id.toString())
                .withClaim("role", user.role.name)
                .withExpiresAt(Date(System.currentTimeMillis() + expiry))
                .sign(Algorithm.HMAC256(secret))
            call.respond(AuthResponse(token))
        }

        authenticate("jwt-auth") {
            // Clients
            get("/clients") {
                val search = call.request.queryParameters["search"] ?: ""
                val clients = clientRepo.getAll(search)
                call.respond(clients.map {
                    ClientResponse(it.id.toString(), it.fullName, it.phone, it.email, it.birthDate?.toString())
                })
            }

            post("/clients") {
                val req = call.receive<ClientRequest>()
                if (req.fullName.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Full name required"))
                    return@post
                }
                val birthDate = req.birthDate?.let { LocalDate.parse(it) }
                val id = clientRepo.create(req.fullName, req.phone, req.email, birthDate)
                call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
            }

            get("/clients/{id}") {
                val id = UUID.fromString(call.parameters["id"])
                val client = clientRepo.getById(id) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))
                    return@get
                }
                call.respond(ClientResponse(client.id.toString(), client.fullName, client.phone, client.email, client.birthDate?.toString()))
            }

            put("/clients/{id}") {
                val id = UUID.fromString(call.parameters["id"])
                val req = call.receive<ClientRequest>()
                if (req.fullName.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Full name required"))
                    return@put
                }
                val birthDate = req.birthDate?.let { LocalDate.parse(it) }
                val updated = clientRepo.update(id, req.fullName, req.phone, req.email, birthDate)
                if (!updated) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))
                else call.respond(HttpStatusCode.OK, mapOf("message" to "Updated"))
            }

            delete("/clients/{id}") {
                val id = UUID.fromString(call.parameters["id"])
                val deleted = clientRepo.delete(id)
                if (!deleted) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))
                else call.respond(HttpStatusCode.OK, mapOf("message" to "Deleted"))
            }

            // Subscriptions
            get("/clients/{id}/subscriptions") {
                val clientId = UUID.fromString(call.parameters["id"])
                val subs = subRepo.getByClientId(clientId)
                call.respond(subs.map {
                    SubscriptionResponse(it.id.toString(), it.clientId.toString(), it.type.name,
                        it.startDate.toString(), it.endDate.toString(), it.isFrozen, it.price.toString())
                })
            }

            post("/clients/{id}/subscriptions") {
                val clientId = UUID.fromString(call.parameters["id"])
                clientRepo.getById(clientId) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))
                    return@post
                }
                val req = call.receive<SubscriptionRequest>()
                val type = SubscriptionType.valueOf(req.type.uppercase())
                val id = subRepo.create(
                    clientId, type,
                    LocalDate.parse(req.startDate),
                    LocalDate.parse(req.endDate),
                    BigDecimal(req.price)
                )
                call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
            }

            patch("/subscriptions/{id}/freeze") {
                val id = UUID.fromString(call.parameters["id"])
                val sub = subRepo.getById(id) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Subscription not found"))
                    return@patch
                }
                subRepo.setFrozen(id, !sub.isFrozen)
                call.respond(HttpStatusCode.OK, mapOf("isFrozen" to !sub.isFrozen))
            }

            // Visits
            get("/clients/{id}/visits") {
                val clientId = UUID.fromString(call.parameters["id"])
                val visits = visitRepo.getByClientId(clientId)
                call.respond(visits.map {
                    VisitResponse(it.id.toString(), it.clientId.toString(), it.visitedAt.toString(), it.note)
                })
            }

            post("/clients/{id}/visits") {
                val clientId = UUID.fromString(call.parameters["id"])
                clientRepo.getById(clientId) ?: run {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Client not found"))
                    return@post
                }
                val req = call.receive<VisitRequest>()
                val id = visitRepo.create(clientId, Instant.parse(req.visitedAt), req.note)
                call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
            }
        }
    }
}
