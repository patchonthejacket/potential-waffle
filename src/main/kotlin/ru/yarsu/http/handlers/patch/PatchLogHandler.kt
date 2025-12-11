package ru.yarsu.http.handlers.patch

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ApiResult
import ru.yarsu.http.handlers.logIdPathLens
import ru.yarsu.http.handlers.restful

@Route(method = Method.PATCH, path = "/v2/log/{log-id}")
fun patchLogHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = logIdPathLens(req)
        val existing =
            storage.getLog(id)
                ?: return@restful notFound(mapOf("Error" to "Log not found", "LogId" to id.toString()))

        try {
            validateJson {
                // В PATCH поля опциональны
                val operation = optionalText("Operation")
                val text = optionalText("Text")

                if (hasErrors()) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
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
        } catch (e: ru.yarsu.http.handlers.ValidationException) {
            json(e.status, e.body)
        }
    }
