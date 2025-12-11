package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v1/logs/for-person")
fun personLogsHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        when (val nameParameter = req.query("name")) {
            null, "" -> json(org.http4k.core.Status.BAD_REQUEST, mapOf("error" to "name is required"))
            else -> {
                val allLogs = storage.getAllLogs()
                val params = pageParams()
                val sortedPersLogs = allLogs.filter { it.ResponsiblePerson == nameParameter }.sortedByDescending { it.LogDateTime }
                val limit = req.query("limit")?.toIntOrNull() ?: 10
                val result =
                    sortedPersLogs.take(limit).map {
                        mapOf(
                            "LogId" to it.Id.toString(),
                            "Operation" to it.Operation,
                            "Equipment" to it.Equipment,
                            "LogDateTime" to it.LogDateTime.toString(),
                            "Text" to it.Text,
                        )
                    }
                ok(result)
            }
        }
    }
