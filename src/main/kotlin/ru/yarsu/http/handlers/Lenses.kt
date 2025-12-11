package ru.yarsu.http.handlers

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import org.http4k.lens.BodyLens
import org.http4k.lens.FormField
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.int
import org.http4k.lens.nonEmptyString
import org.http4k.lens.uuid
import org.http4k.lens.webForm

// Линзы для пути
val equipmentIdPathLens = Path.uuid().of("equipment-id")
val logIdPathLens = Path.uuid().of("log-id")
val userIdPathLens = Path.uuid().of("user-id")

// Линзы для query параметров
val pageQueryLens = Query.int().defaulted("page", 1)
val recordsPerPageQueryLens = Query.int().defaulted("records-per-page", 10)

// Валидация для records-per-page - используем функцию вместо map
val allowedRecordsPerPage = setOf(5, 10, 20, 50)

fun getValidatedRecordsPerPage(req: Request): Int {
    val value = recordsPerPageQueryLens(req)
    if (value in allowedRecordsPerPage) {
        return value
    } else {
        throw IllegalArgumentException("Invalid 'records-per-page'. Allowed values: 5, 10, 20, 50")
    }
}

// Линзы для тела запроса (JSON)
val equipmentBodyLens: BodyLens<EquipmentCreateRequest> = Body.auto<EquipmentCreateRequest>().toLens()
val equipmentUpdateBodyLens: BodyLens<EquipmentUpdateRequest> = Body.auto<EquipmentUpdateRequest>().toLens()
val logUpdateBodyLens: BodyLens<LogUpdateRequest> = Body.auto<LogUpdateRequest>().toLens()

// Линзы для формы (application/x-www-form-urlencoded) при редактировании журнала
val operationFormField = FormField.nonEmptyString().required("Operation")
val textFormField = FormField.nonEmptyString().required("Text")
val logUpdateFormLens: BodyLens<WebForm> =
    Body.webForm(Validator.Strict, operationFormField, textFormField).toLens()

// Модели запросов
data class EquipmentCreateRequest(
    val Equipment: String,
    val Category: String,
    val GuaranteeDate: String,
    val Price: java.math.BigDecimal,
    val Location: String,
    val ResponsiblePerson: String,
    val User: String? = null,
    val Operation: String,
    val Text: String? = "",
)

data class EquipmentUpdateRequest(
    // Все поля, описывающие оборудование, кроме Operation/Text, считаем опциональными:
    // их отсутствие в PATCH-запросе означает «поле не изменяется».
    val Equipment: String? = null,
    val Category: String? = null,
    val GuaranteeDate: String? = null,
    val Price: java.math.BigDecimal? = null,
    val Location: String? = null,
    val ResponsiblePerson: String? = null,
    val User: String? = null,
    // Операция и текст журнала при редактировании обязательны
    val Operation: String,
    val Text: String,
)

data class LogUpdateRequest(
    val Operation: String,
    val Text: String,
)
