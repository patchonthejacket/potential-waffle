package ru.yarsu.http.handlers

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.Status
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class FieldError(
    val field: String,
    val node: JsonNode?,
) : Exception()

fun fieldErrorResponse(
    field: String,
    node: JsonNode?,
): Map<String, Any> =
    mapOf(
        field to
            mapOf(
                "Value" to (node ?: JsonMapper.nullNode()),
                "Error" to "Некорректное значение",
            ),
    )

fun validateJsonBody(rawBody: String): Result<JsonNode> =
    try {
        Result.success(JsonMapper.readTree(rawBody))
    } catch (e: Exception) {
        Result.failure(
            ValidationException(
                Status.BAD_REQUEST,
                mapOf(
                    "Value" to rawBody,
                    "Error" to "Некорректный JSON",
                ),
            ),
        )
    }

class ValidationException(
    val status: Status,
    val body: Map<String, Any>,
) : Exception()

fun requireTextField(
    root: JsonNode,
    fieldName: String,
): Result<String> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> {
            val text = node.asText()
            if (text.isBlank()) {
                Result.failure(FieldError(fieldName, node))
            } else {
                Result.success(text)
            }
        }
    }
}

fun requireTextFieldAllowEmpty(
    root: JsonNode,
    fieldName: String,
): Result<String> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> Result.success(node.asText())
    }
}

fun validateTextField(
    root: JsonNode,
    fieldName: String,
    required: Boolean = true,
): Result<String?> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> {
            if (required) {
                Result.failure(FieldError(fieldName, node))
            } else {
                Result.success(null)
            }
        }
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> Result.success(node.asText())
    }
}

fun requireNumberField(
    root: JsonNode,
    fieldName: String,
): Result<BigDecimal> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isNumber -> Result.failure(FieldError(fieldName, node))
        else -> {
            try {
                Result.success(BigDecimal(node.asText()))
            } catch (e: Exception) {
                Result.failure(FieldError(fieldName, node))
            }
        }
    }
}

fun validateNumberField(
    root: JsonNode,
    fieldName: String,
    required: Boolean = true,
): Result<BigDecimal?> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> {
            if (required) {
                Result.failure(FieldError(fieldName, node))
            } else {
                Result.success(null)
            }
        }
        !node.isNumber -> Result.failure(FieldError(fieldName, node))
        else -> {
            try {
                Result.success(BigDecimal(node.asText()))
            } catch (e: Exception) {
                Result.failure(FieldError(fieldName, node))
            }
        }
    }
}

fun requireDateField(
    root: JsonNode,
    fieldName: String,
): Result<String> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> {
            val text = node.asText()
            if (text.isBlank()) {
                Result.failure(FieldError(fieldName, node))
            } else {
                try {
                    LocalDate.parse(text)
                    Result.success(text)
                } catch (e: Exception) {
                    Result.failure(FieldError(fieldName, node))
                }
            }
        }
    }
}

fun validateDateField(
    root: JsonNode,
    fieldName: String,
    required: Boolean = true,
): Result<String?> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> {
            if (required) {
                Result.failure(FieldError(fieldName, node))
            } else {
                Result.success(null)
            }
        }
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> {
            val text = node.asText()
            if (text.isBlank()) {
                Result.failure(FieldError(fieldName, node))
            } else {
                try {
                    LocalDate.parse(text)
                    Result.success(text)
                } catch (e: Exception) {
                    Result.failure(FieldError(fieldName, node))
                }
            }
        }
    }
}

fun validateUuidField(
    root: JsonNode,
    fieldName: String,
    required: Boolean = true,
): Result<String?> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> {
            if (required) {
                Result.failure(FieldError(fieldName, node))
            } else {
                Result.success(null)
            }
        }
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> {
            val text = node.asText()
            try {
                UUID.fromString(text)
                Result.success(text)
            } catch (e: Exception) {
                Result.failure(FieldError(fieldName, node))
            }
        }
    }
}

fun validateOptionalTextField(
    root: JsonNode,
    fieldName: String,
): Result<String?> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.success(null)
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> Result.success(node.asText())
    }
}

fun validateOptionalTextFieldAllowEmpty(
    root: JsonNode,
    fieldName: String,
): Result<String?> {
    val node = root.get(fieldName)
    return when {
        node == null -> Result.success(null)
        node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> Result.success(node.asText())
    }
}

fun collectValidationErrors(results: List<Result<*>>): List<FieldError> =
    results
        .mapNotNull { result ->
            result.exceptionOrNull() as? FieldError
        }

fun getLastValidationError(results: List<Result<*>>): FieldError? =
    results
        .mapNotNull { result ->
            result.exceptionOrNull() as? FieldError
        }.lastOrNull()

fun collectAllValidationErrors(results: List<Result<*>>): Map<String, Any> {
    val errors = collectValidationErrors(results)
    if (errors.isEmpty()) {
        return emptyMap()
    }
    return errors.associate { error ->
        error.field to
            mapOf(
                "Value" to (error.node ?: JsonMapper.nullNode()),
                "Error" to "Некорректное значение",
            )
    }
}

inline fun <T> Result<T>.getOrReturnError(
    root: JsonNode,
    onError: (FieldError) -> Nothing,
): T =
    getOrElse { e ->
        val error =
            e as? FieldError
                ?: throw IllegalStateException("Unexpected error type: ${e::class.simpleName}")
        onError(error)
    }

inline fun <T> Result<T>.getOrReturnFieldError(
    fieldName: String,
    root: JsonNode,
    onError: (Map<String, Any>) -> Nothing,
): T =
    getOrElse {
        val error =
            it as? FieldError
                ?: throw IllegalStateException("Unexpected error type: ${it::class.simpleName}")
        onError(fieldErrorResponse(error.field, error.node))
    }

inline fun <T> extractRequired(
    root: JsonNode,
    fieldName: String,
    validator: (JsonNode, String) -> Result<T>,
    onError: (Map<String, Any>) -> Nothing,
): T = validator(root, fieldName).getOrReturnFieldError(fieldName, root, onError)

inline fun <T> extractOptional(
    root: JsonNode,
    fieldName: String,
    validator: (JsonNode, String) -> Result<T?>,
    onError: (Map<String, Any>) -> Nothing,
): T? {
    val node = root.get(fieldName)
    // Если поле отсутствует - это нормально для опционального поля
    if (node == null) {
        return null
    }
    // Если поле явно null - это ошибка для опциональных полей в PATCH
    if (node.isNull) {
        onError(fieldErrorResponse(fieldName, node))
        return null // unreachable
    }
    // Если поле присутствует, но имеет некорректный тип - возвращаем ошибку
    val result = validator(root, fieldName)
    return if (result.isFailure) {
        result.getOrReturnFieldError(fieldName, root, onError)
        null // unreachable
    } else {
        result.getOrNull()
    }
}

inline fun <T> Result<T>.onValidationError(onError: (FieldError) -> Nothing): T =
    getOrElse { e ->
        val error = e as? FieldError ?: throw IllegalStateException("Unexpected error type: ${e::class.simpleName}")
        onError(error)
    }

fun validateCategory(
    category: String,
    root: JsonNode,
): Result<String> {
    val allowedCategories = setOf("ПК", "Монитор", "Принтер", "Телефон", "Другое")
    return if (category in allowedCategories) {
        Result.success(category)
    } else {
        Result.failure(FieldError("Category", root.get("Category")))
    }
}

fun validatePrice(
    price: BigDecimal,
    root: JsonNode,
): Result<BigDecimal> =
    if (price >= BigDecimal.ZERO) {
        Result.success(price)
    } else {
        Result.failure(FieldError("Price", root.get("Price")))
    }

fun requireNumberAllowZero(
    root: JsonNode,
    fieldName: String,
): Result<BigDecimal> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isNumber -> Result.failure(FieldError(fieldName, node))
        else -> {
            try {
                val value = BigDecimal(node.asText())
                if (value >= BigDecimal.ZERO) {
                    Result.success(value)
                } else {
                    Result.failure(FieldError(fieldName, node))
                }
            } catch (e: Exception) {
                Result.failure(FieldError(fieldName, node))
            }
        }
    }
}

fun requireTextAllowEmpty(
    root: JsonNode,
    fieldName: String,
): Result<String> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> Result.success(node.asText())
    }
}

fun requireCategoryAllowDefault(
    root: JsonNode,
    fieldName: String,
): Result<String> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> {
            val category = node.asText()
            val allowedCategories = setOf("ПК", "Монитор", "Принтер", "Телефон", "Другое")
            if (category in allowedCategories) {
                Result.success(category)
            } else {
                Result.failure(FieldError(fieldName, node))
            }
        }
    }
}

fun requireDateFieldAllowEmpty(
    root: JsonNode,
    fieldName: String,
): Result<String> {
    val node = root.get(fieldName)
    return when {
        node == null || node.isNull -> Result.failure(FieldError(fieldName, node))
        !node.isTextual -> Result.failure(FieldError(fieldName, node))
        else -> {
            val text = node.asText()
            if (text.isBlank()) {
                Result.success(text)
            } else {
                try {
                    LocalDate.parse(text)
                    Result.success(text)
                } catch (e: Exception) {
                    Result.failure(FieldError(fieldName, node))
                }
            }
        }
    }
}

fun validateUserExists(
    userId: UUID,
    storage: ru.yarsu.EquipmentStorage,
    root: JsonNode,
): Result<UUID> =
    if (storage.getUser(userId) != null) {
        Result.success(userId)
    } else {
        Result.failure(FieldError("User", root.get("User")))
    }

fun validateResponsiblePersonExists(
    responsiblePersonId: UUID,
    storage: ru.yarsu.EquipmentStorage,
    root: JsonNode,
): Result<UUID> =
    if (storage.getUser(responsiblePersonId) != null) {
        Result.success(responsiblePersonId)
    } else {
        Result.failure(FieldError("ResponsiblePerson", root.get("ResponsiblePerson")))
    }

fun parseUuid(
    fieldName: String,
    value: String,
    root: JsonNode,
): Result<UUID> =
    try {
        Result.success(UUID.fromString(value))
    } catch (e: Exception) {
        Result.failure(FieldError(fieldName, root.get(fieldName)))
    }
