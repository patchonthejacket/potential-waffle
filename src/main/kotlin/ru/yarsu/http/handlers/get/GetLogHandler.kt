package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.LogDetail
import ru.yarsu.http.handlers.logIdPathLens
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v2/log/{log-id}")
fun getLogHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = logIdPathLens(req)
        val log = storage.getLog(id)
        if (log == null) {
            notFound(mapOf("LogId" to id.toString()))
        } else {
            val detail =
                LogDetail(
                    log.Id.toString(),
                    log.Equipment.toString(),
                    log.ResponsiblePerson,
                    log.Operation,
                    log.Text,
                    log.LogDateTime.toString(),
                )
            ok(detail)
        }
    }
