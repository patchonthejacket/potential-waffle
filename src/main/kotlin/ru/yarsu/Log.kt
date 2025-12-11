package ru.yarsu

import java.time.LocalDateTime
import java.util.UUID

data class Log(
    val Id: UUID,
    val Equipment: UUID,
    val ResponsiblePerson: String,
    val Operation: String,
    val Text: String,
    val LogDateTime: LocalDateTime,
)
