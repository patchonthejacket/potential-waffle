package ru.yarsu.http.handlers

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal fun jsonError(
    status: Status,
    message: String,
): Response =
    Response(status)
        .header("Content-Type", "application/json; charset=utf-8")
        .body(JsonMapper.toJson(mapOf("error" to message)))

internal fun parseUuidPath(
    pathValue: String?,
    onErrorMessage: String,
): UUID {
    try {
        return UUID.fromString(pathValue)
    } catch (e: Exception) {
        throw IllegalArgumentException(onErrorMessage)
    }
}

internal fun parsePositiveIntOrDefault(
    param: String?,
    default: Int,
    name: String,
): Int {
    if (param != null) {
        val parsed = param.toIntOrNull() ?: throw IllegalArgumentException("Invalid '$name'")
        if (parsed < 1) throw IllegalArgumentException("Invalid '$name'")
        return parsed
    }
    return default
}

internal fun parseRecordsPerPage(param: String?): Int {
    val allowed = setOf(5, 10, 20, 50)
    if (param != null) {
        val parsed = param.toIntOrNull() ?: throw IllegalArgumentException("Invalid 'records-per-page'")
        if (parsed !in allowed) throw IllegalArgumentException("Invalid 'records-per-page'")
        return parsed
    }
    return 10
}

internal fun parseDateTimeQuery(
    param: String?,
    name: String,
): LocalDateTime {
    if (param == null) throw IllegalArgumentException("Missing $name")
    try {
        return LocalDateTime.parse(param)
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid $name")
    }
}

internal fun parseDate(
    param: String,
    name: String,
): LocalDate =
    try {
        LocalDate.parse(param)
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid $name")
    }

private val FIXED_CATEGORIES: Set<String> = setOf("ПК", "Монитор", "Принтер", "Телефон", "Другое")

internal fun parseCategories(
    raw: String?,
    allowed: Set<String>,
): List<String> {
    if (raw == null) return emptyList()
    val tokens = raw.split(',')
    val result = mutableListOf<String>()
    for (token in tokens) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            // пустой или пробельный элемент — просто игнорируем (без фильтрации по нему)
            continue
        }
        // значение должно точно совпадать без лишних пробелов
        if (token != trimmed || trimmed !in FIXED_CATEGORIES) {
            throw IllegalArgumentException("Invalid category")
        }
        if (trimmed !in result) {
            result += trimmed
        }
    }
    return result
}
