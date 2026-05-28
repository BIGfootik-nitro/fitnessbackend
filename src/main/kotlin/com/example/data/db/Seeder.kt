package com.example.data.db

import com.example.data.db.tables.*
import com.example.domain.model.BookingStatus
import com.example.domain.model.SubscriptionType
import com.example.domain.model.UserRole
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

object Seeder {

    fun seedIfEmpty() {
        val hasUsers = transaction { Users.selectAll().count() > 0 }
        if (hasUsers) return

        transaction {
            // --- users ---
            val adminId = Users.insert {
                it[username] = "admin"
                it[passwordHash] = BCrypt.hashpw("admin123", BCrypt.gensalt())
                it[role] = UserRole.ADMIN
            }[Users.id].value

            val trainerId = Users.insert {
                it[username] = "trener"
                it[passwordHash] = BCrypt.hashpw("trener123", BCrypt.gensalt())
                it[role] = UserRole.TRAINER
            }[Users.id].value

            val client1UserId = Users.insert {
                it[username] = "ivan"
                it[passwordHash] = BCrypt.hashpw("ivan123", BCrypt.gensalt())
                it[role] = UserRole.CLIENT
            }[Users.id].value

            val client2UserId = Users.insert {
                it[username] = "maria"
                it[passwordHash] = BCrypt.hashpw("maria123", BCrypt.gensalt())
                it[role] = UserRole.CLIENT
            }[Users.id].value

            // --- clients ---
            val c1 = Clients.insert {
                it[fullName] = "Иванов Иван Иванович"
                it[phone] = "+7 900 111-22-33"
                it[email] = "ivan@mail.ru"
                it[birthDate] = LocalDate.of(1992, 3, 15)
                it[userId] = client1UserId
            }[Clients.id].value

            val c2 = Clients.insert {
                it[fullName] = "Петрова Мария Сергеевна"
                it[phone] = "+7 900 444-55-66"
                it[email] = "maria@mail.ru"
                it[birthDate] = LocalDate.of(1995, 7, 22)
                it[userId] = client2UserId
            }[Clients.id].value

            val c3 = Clients.insert {
                it[fullName] = "Сидоров Алексей Петрович"
                it[phone] = "+7 900 777-88-99"
                it[email] = null
                it[birthDate] = LocalDate.of(1988, 11, 5)
                it[userId] = null
            }[Clients.id].value

            val c4 = Clients.insert {
                it[fullName] = "Козлова Елена Андреевна"
                it[phone] = "+7 900 123-45-67"
                it[email] = "elena@gmail.com"
                it[birthDate] = LocalDate.of(2000, 1, 30)
                it[userId] = null
            }[Clients.id].value

            val c5 = Clients.insert {
                it[fullName] = "Новиков Дмитрий Олегович"
                it[phone] = "+7 901 234-56-78"
                it[email] = null
                it[birthDate] = null
                it[userId] = null
            }[Clients.id].value

            val c6 = Clients.insert {
                it[fullName] = "Фёдорова Анна Викторовна"
                it[phone] = "+7 902 345-67-89"
                it[email] = "anna@yandex.ru"
                it[birthDate] = LocalDate.of(1997, 5, 14)
                it[userId] = null
            }[Clients.id].value

            val c7 = Clients.insert {
                it[fullName] = "Морозов Сергей Николаевич"
                it[phone] = "+7 903 456-78-90"
                it[email] = null
                it[birthDate] = LocalDate.of(1985, 9, 20)
                it[userId] = null
            }[Clients.id].value

            // --- subscriptions ---
            val now = LocalDate.now()
            fun addSub(cid: UUID, type: SubscriptionType, start: LocalDate, months: Long, price: String) =
                Subscriptions.insert {
                    it[clientId] = cid
                    it[Subscriptions.type] = type
                    it[startDate] = start
                    it[endDate] = start.plusMonths(months)
                    it[Subscriptions.price] = BigDecimal(price)
                }[Subscriptions.id].value

            addSub(c1, SubscriptionType.MONTHLY, now.minusDays(10), 1, "3000")
            addSub(c2, SubscriptionType.QUARTERLY, now.minusDays(30), 3, "7500")
            addSub(c3, SubscriptionType.ANNUAL, now.minusMonths(2), 12, "24000")
            addSub(c4, SubscriptionType.MONTHLY, now.minusMonths(2), 1, "3000")  // expired
            addSub(c5, SubscriptionType.MONTHLY, now.minusDays(5), 1, "3000")
            addSub(c6, SubscriptionType.QUARTERLY, now.minusDays(15), 3, "7500")

            // --- training sessions ---
            val past1 = Instant.now().minus(7, ChronoUnit.DAYS)
            val past2 = Instant.now().minus(3, ChronoUnit.DAYS)
            val past3 = Instant.now().minus(1, ChronoUnit.DAYS)
            val future1 = Instant.now().plus(1, ChronoUnit.DAYS)
            val future2 = Instant.now().plus(3, ChronoUnit.DAYS)
            val future3 = Instant.now().plus(5, ChronoUnit.DAYS)
            val future4 = Instant.now().plus(7, ChronoUnit.DAYS)
            val future5 = Instant.now().plus(10, ChronoUnit.DAYS)

            val s1 = TrainingSessions.insert {
                it[title] = "Силовая тренировка"
                it[description] = "Базовые упражнения со свободными весами. Подходит для всех уровней."
                it[scheduledAt] = past1
                it[durationMin] = 60
                it[TrainingSessions.trainerId] = trainerId
                it[maxCapacity] = 8
            }[TrainingSessions.id].value

            val s2 = TrainingSessions.insert {
                it[title] = "Кардио"
                it[description] = "Интервальная кардио-тренировка на беговых дорожках."
                it[scheduledAt] = past2
                it[durationMin] = 45
                it[TrainingSessions.trainerId] = trainerId
                it[maxCapacity] = 10
            }[TrainingSessions.id].value

            val s3 = TrainingSessions.insert {
                it[title] = "Йога"
                it[description] = "Утренняя практика для восстановления и гибкости."
                it[scheduledAt] = past3
                it[durationMin] = 60
                it[TrainingSessions.trainerId] = null
                it[maxCapacity] = 12
            }[TrainingSessions.id].value

            val s4 = TrainingSessions.insert {
                it[title] = "Силовая тренировка"
                it[description] = "Базовые упражнения со свободными весами."
                it[scheduledAt] = future1
                it[durationMin] = 60
                it[TrainingSessions.trainerId] = trainerId
                it[maxCapacity] = 8
            }[TrainingSessions.id].value

            val s5 = TrainingSessions.insert {
                it[title] = "Функциональный тренинг"
                it[description] = "Тренировка с использованием собственного веса и TRX."
                it[scheduledAt] = future2
                it[durationMin] = 50
                it[TrainingSessions.trainerId] = trainerId
                it[maxCapacity] = 6
            }[TrainingSessions.id].value

            val s6 = TrainingSessions.insert {
                it[title] = "Кардио + растяжка"
                it[description] = "Комбинированная тренировка: кардио 30 мин + растяжка 20 мин."
                it[scheduledAt] = future3
                it[durationMin] = 50
                it[TrainingSessions.trainerId] = null
                it[maxCapacity] = 10
            }[TrainingSessions.id].value

            TrainingSessions.insert {
                it[title] = "Персональная тренировка (группа)"
                it[description] = "Мини-группа до 4 человек. Индивидуальный подход."
                it[scheduledAt] = future4
                it[durationMin] = 60
                it[TrainingSessions.trainerId] = trainerId
                it[maxCapacity] = 4
            }

            TrainingSessions.insert {
                it[title] = "Йога"
                it[description] = "Вечерняя практика. Расслабление и медитация."
                it[scheduledAt] = future5
                it[durationMin] = 60
                it[TrainingSessions.trainerId] = null
                it[maxCapacity] = 12
            }

            // --- bookings for sessions ---
            fun book(cid: UUID, sid: UUID, status: BookingStatus) = Bookings.insert {
                it[clientId] = cid
                it[sessionId] = sid
                it[scheduledAt] = Instant.now()
                it[Bookings.status] = status
                it[note] = null
            }[Bookings.id].value

            // past sessions: bookings + visits
            val b1 = book(c1, s1, BookingStatus.CONFIRMED)
            val b2 = book(c2, s1, BookingStatus.CONFIRMED)
            val b3 = book(c3, s1, BookingStatus.CONFIRMED)
            book(c1, s2, BookingStatus.CONFIRMED)
            book(c2, s2, BookingStatus.CONFIRMED)
            book(c4, s3, BookingStatus.CONFIRMED)
            book(c5, s3, BookingStatus.CANCELLED)

            // future sessions bookings
            book(c1, s4, BookingStatus.PENDING)
            book(c2, s4, BookingStatus.CONFIRMED)
            book(c3, s5, BookingStatus.PENDING)
            book(c1, s6, BookingStatus.PENDING)

            // client1 & 2 bookings linked to userId (for /me/bookings)
            Bookings.insert {
                it[clientId] = c1
                it[sessionId] = s4
                it[scheduledAt] = future1
                it[status] = BookingStatus.CONFIRMED
                it[note] = "Запись через приложение"
            }

            // --- visits (past attended) ---
            fun visit(cid: UUID, daysAgo: Long, note: String?) = Visits.insert {
                it[clientId] = cid
                it[visitedAt] = Instant.now().minus(daysAgo, ChronoUnit.DAYS)
                it[Visits.note] = note
            }

            visit(c1, 7, "Силовая — хорошая работа")
            visit(c1, 3, "Кардио")
            visit(c2, 7, "Силовая")
            visit(c2, 3, "Кардио — уставшая, но довольна")
            visit(c3, 7, "Первое посещение")
            visit(c4, 1, "Йога — понравилось")
            visit(c5, 14, null)
            visit(c6, 10, "Хорошая техника")
            visit(c7, 5, null)

            // --- welcome notifications ---
            listOf(client1UserId, client2UserId).forEach { uid ->
                Notifications.insert {
                    it[userId] = uid
                    it[title] = "Добро пожаловать!"
                    it[body] = "Рады видеть вас в нашем фитнес-центре. Запишитесь на тренировку!"
                    it[read] = false
                }
            }
        }
    }
}
