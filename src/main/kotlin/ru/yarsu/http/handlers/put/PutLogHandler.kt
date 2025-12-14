package ru.yarsu.http.handlers.put

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ApiResult
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.logIdPathLens
import ru.yarsu.http.handlers.logUpdateFormLens
import ru.yarsu.http.handlers.operationFormField
import ru.yarsu.http.handlers.restful
import ru.yarsu.http.handlers.textFormField

@Route(method = Method.PUT, path = "/v3/log/{log-id}")
fun putLogHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = logIdPathLens(req)

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
                    // Если поля на месте, но данные кривые (редко для String)
                    throw ValidationException(Status.BAD_REQUEST, mapOf("Error" to "Некорректные значения параметров формы"))
                }
                throw ValidationException(Status.BAD_REQUEST, errors)
            }

        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val existing =
            storage.getLog(id) ?: return@restful notFound(mapOf("Error" to "Запись в журнале не найдена", "LogId" to id.toString()))

        // Проверка, что запись принадлежит текущему пользователю
        val currentUserId = user?.Id?.toString()
        if (existing.ResponsiblePerson != currentUserId) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val operation = operationFormField(form)
        val text = textFormField(form)

        storage.updateLog(id) { log ->
            log.copy(
                Operation = operation,
                Text = text,
                LogDateTime = log.LogDateTime,
            )
        }

        ApiResult.NoContent
    }
