package ru.yarsu.http.handlers.delete

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.logIdPathLens
import ru.yarsu.http.handlers.restful

@Route(method = Method.DELETE, path = "/v2/log/{log-id}")
fun deleteLogHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        if (user == null) {
            throw ValidationException(org.http4k.core.Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val id = logIdPathLens(req)

        val existing = storage.getLog(id)
        if (existing == null) {
            notFound(mapOf("Error" to "Log not found", "LogId" to id.toString()))
        } else {
            storage.removeLog(id)
            ok(mapOf("LogId" to id.toString(), "Message" to "Log deleted successfully"))
        }
    }
