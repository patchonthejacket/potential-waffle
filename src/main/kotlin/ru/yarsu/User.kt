package ru.yarsu

import java.time.LocalDateTime
import java.util.UUID

data class User(
    val Id: UUID,
    val Name: String,
    val RegistrationDateTime: LocalDateTime,
    val Email: String,
    val Position: String,
    val Role: UserRole,
)
