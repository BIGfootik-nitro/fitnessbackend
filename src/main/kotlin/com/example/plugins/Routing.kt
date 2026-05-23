package com.example.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.data.dto.*
import com.example.data.repository.*
import com.example.domain.model.BookingStatus
import com.example.domain.model.SubscriptionType
import com.example.domain.model.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

private fun ApplicationCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.getClaim("userId").asString())

private fun ApplicationCall.role(): UserRole =
    UserRole.valueOf(principal<JWTPrincipal>()!!.payload.getClaim("role").asString())

fun Application.configureRouting() {
    val userRepo = UserRepository()
    val clientRepo = ClientRepository()
    val subRepo = SubscriptionRepository()
    val visitRepo = VisitRepository()
    val bookingRepo = BookingRepository()
    val notificationRepo = NotificationRepository()

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
            val userId = userRepo.create(req.username, hash, role)

            // если это клиент — заводим профиль клиента сразу
            if (role == UserRole.CLIENT) {
                clientRepo.create(req.username, null, null, null, userId)
                notificationRepo.create(userId, "Добро пожаловать!",
                    "Спасибо за регистрацию. Оформите абонемент, чтобы начать тренировки.")
            }
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
            // ===== Профиль текущего пользователя =====
            get("/me") {
                val uid = call.userId()
                val user = userRepo.findById(uid) ?: run {
                    call.respond(HttpStatusCode.NotFound); return@get
                }
                // если клиент без профиля — создаём
                var client = clientRepo.getByUserId(uid)
                if (client == null && user.role == UserRole.CLIENT) {
                    val cid = clientRepo.create(user.username, null, null, null, uid)
                    client = clientRepo.getById(cid)
                    notificationRepo.create(uid, "Добро пожаловать!",
                        "Заполните профиль и оформите абонемент.")
                }
                call.respond(MeResponse(
                    userId = user.id.toString(),
                    username = user.username,
                    role = user.role.name,
                    client = client?.let {
                        ClientResponse(it.id.toString(), it.fullName, it.phone, it.email, it.birthDate?.toString())
                    }
                ))
            }

            put("/me/profile") {
                val uid = call.userId()
                val req = call.receive<ClientRequest>()
                val existing = clientRepo.getByUserId(uid)
                if (existing == null) {
                    // профиля нет — создаём
                    val birthDate = req.birthDate?.let { LocalDate.parse(it) }
                    clientRepo.create(req.fullName.ifBlank { "Client" }, req.phone, req.email, birthDate, uid)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Created"))
                } else {
                    val birthDate = req.birthDate?.let { LocalDate.parse(it) }
                    clientRepo.update(existing.id, req.fullName, req.phone, req.email, birthDate)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Updated"))
                }
            }

            get("/me/subscriptions") {
                val client = clientRepo.getByUserId(call.userId())
                if (client == null) { call.respond(emptyList<Any>()); return@get }
                val subs = subRepo.getByClientId(client.id)
                call.respond(subs.map {
                    SubscriptionResponse(it.id.toString(), it.clientId.toString(), it.type.name,
                        it.startDate.toString(), it.endDate.toString(), it.isFrozen, it.price.toString())
                })
            }

            post("/me/subscriptions") {
                val uid = call.userId()
                val client = clientRepo.getByUserId(uid) ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Fill your profile first"))
                    return@post
                }
                val req = call.receive<SubscriptionRequest>()
                val type = SubscriptionType.valueOf(req.type.uppercase())
                val id = subRepo.create(client.id, type,
                    LocalDate.parse(req.startDate),
                    LocalDate.parse(req.endDate),
                    BigDecimal(req.price))
                notificationRepo.create(uid, "Абонемент оформлен",
                    "Ваш ${type.name.lowercase()} абонемент активен с ${req.startDate} по ${req.endDate}.")
                call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
            }

            get("/me/visits") {
                val client = clientRepo.getByUserId(call.userId())
                if (client == null) { call.respond(emptyList<Any>()); return@get }
                val visits = visitRepo.getByClientId(client.id)
                call.respond(visits.map {
                    VisitResponse(it.id.toString(), it.clientId.toString(), it.visitedAt.toString(), it.note)
                })
            }

            get("/me/bookings") {
                val client = clientRepo.getByUserId(call.userId())
                if (client == null) { call.respond(emptyList<Any>()); return@get }
                val bookings = bookingRepo.getByClientId(client.id)
                call.respond(bookings.map {
                    BookingResponse(it.id.toString(), it.clientId.toString(), null,
                        it.scheduledAt.toString(), it.status.name, it.note)
                })
            }

            post("/me/bookings") {
                val uid = call.userId()
                val client = clientRepo.getByUserId(uid) ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Fill your profile first"))
                    return@post
                }
                val req = call.receive<BookingRequest>()
                val id = bookingRepo.create(client.id, Instant.parse(req.scheduledAt), req.note)
                notificationRepo.create(uid, "Заявка отправлена",
                    "Запись на тренировку ${req.scheduledAt.substringBefore('T')} ожидает подтверждения.")
                call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
            }

            patch("/me/bookings/{id}/cancel") {
                val uid = call.userId()
                val client = clientRepo.getByUserId(uid)
                if (client == null) { call.respond(HttpStatusCode.BadRequest); return@patch }
                val id = UUID.fromString(call.parameters["id"])
                val booking = bookingRepo.getById(id)
                if (booking == null || booking.clientId != client.id) {
                    call.respond(HttpStatusCode.NotFound); return@patch
                }
                bookingRepo.setStatus(id, BookingStatus.CANCELLED)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Cancelled"))
            }

            get("/me/notifications") {
                val list = notificationRepo.getByUserId(call.userId())
                call.respond(list.map {
                    NotificationResponse(it.id.toString(), it.title, it.body, it.read, it.createdAt.toString())
                })
            }

            patch("/me/notifications/{id}/read") {
                val id = UUID.fromString(call.parameters["id"])
                notificationRepo.markRead(id, call.userId())
                call.respond(HttpStatusCode.OK, mapOf("message" to "Marked"))
            }

            // ===== Тренер/админ =====
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

            // тренер видит все брони и подтверждает
            get("/bookings") {
                if (call.role() == UserRole.CLIENT) {
                    call.respond(HttpStatusCode.Forbidden); return@get
                }
                val list = bookingRepo.getAll()
                call.respond(list.map { (b, name) ->
                    BookingResponse(b.id.toString(), b.clientId.toString(), name,
                        b.scheduledAt.toString(), b.status.name, b.note)
                })
            }

            patch("/bookings/{id}/status") {
                if (call.role() == UserRole.CLIENT) {
                    call.respond(HttpStatusCode.Forbidden); return@patch
                }
                val id = UUID.fromString(call.parameters["id"])
                val req = call.receive<BookingStatusUpdate>()
                val newStatus = BookingStatus.valueOf(req.status.uppercase())
                val booking = bookingRepo.getById(id) ?: run {
                    call.respond(HttpStatusCode.NotFound); return@patch
                }
                bookingRepo.setStatus(id, newStatus)
                // уведомление клиенту
                clientRepo.getById(booking.clientId)?.userId?.let { clientUserId ->
                    val title = when (newStatus) {
                        BookingStatus.CONFIRMED -> "Запись подтверждена"
                        BookingStatus.CANCELLED -> "Запись отменена"
                        BookingStatus.PENDING -> "Запись ожидает"
                    }
                    notificationRepo.create(clientUserId, title,
                        "Тренировка на ${booking.scheduledAt.toString().substringBefore('T')}.")
                }
                call.respond(HttpStatusCode.OK, mapOf("status" to newStatus.name))
            }
        }
    }
}
