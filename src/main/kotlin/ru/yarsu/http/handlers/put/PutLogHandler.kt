package ru.yarsu.http.handlers.put

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ApiResult
import ru.yarsu.http.handlers.logIdPathLens
import ru.yarsu.http.handlers.logUpdateFormLens
import ru.yarsu.http.handlers.operationFormField
import ru.yarsu.http.handlers.restful
import ru.yarsu.http.handlers.textFormField

@Route(method = Method.PUT, path = "/v2/log/{log-id}")
fun putLogHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = logIdPathLens(req)
        // PUT /v2/log/{log-id} принимает application/x-www-form-urlencoded
        val form =
            try {
                logUpdateFormLens(req)
            } catch (e: org.http4k.lens.LensFailure) {
                val fields = e.failures.map { it.meta.name }.toSet()
                val errors = mutableMapOf<String, Any>()
                if ("Operation" in fields) {
                    errors["Operation"] = mapOf("Value" to null, "Error" to "Отсутствует поле")
                }
                if ("Text" in fields) {
                    errors["Text"] = mapOf("Value" to null, "Error" to "Отсутствует поле")
                }
                if (errors.isEmpty()) {
                    return@restful ApiResult.Custom(Status.BAD_REQUEST, mapOf("Error" to "Некорректные значения параметров формы"))
                }
                return@restful ApiResult.Custom(Status.BAD_REQUEST, errors)
            }

        val operation = operationFormField(form)
        val text = textFormField(form)

        val existing = storage.getLog(id)
        if (existing == null) {
            notFound(mapOf("LogId" to id.toString()))
        } else {
            storage.updateLog(id) { log ->
                log.copy(
                    Operation = operation,
                    Text = text,
                    // При редактировании журнала время остаётся таким же, как в исходной записи
                    LogDateTime = log.LogDateTime,
                )
            }
            // успешное редактирование журнала — 204 No Content
            ApiResult.NoContent
        }
    }
