package ru.yarsu.http.handlers.patch

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ApiResult
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.logIdPathLens
import ru.yarsu.http.handlers.restful

@Route(method = Method.PATCH, path = "/v3/log/{log-id}")
fun patchLogHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = logIdPathLens(req)
        val existing =
            storage.getLog(id)
                ?: return@restful notFound(mapOf("LogId" to id.toString()))

        validateJson {
            // В PATCH поля опциональны
            val operation = optionalText("Operation")
            val text = optionalText("Text")

            if (hasErrors()) {
                throw ValidationException(Status.BAD_REQUEST, collectErrors())
            }

            // Проверка авторизации после валидации JSON
            if (user == null) {
                throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
            }

            // Применяем частичное обновление
            storage.updateLog(id) { log ->
                log.copy(
                    Operation = operation ?: log.Operation,
                    Text = text ?: log.Text,
                    // При редактировании журнала время остаётся таким же
                    LogDateTime = log.LogDateTime,
                )
            }

            ApiResult.NoContent
        }
    }
