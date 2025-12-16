package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.uuid
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful

val equipmentIdQuery = Query.uuid().optional("equipment_id")

@Route(method = Method.GET, path = "/v3/log")
fun getLogHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val params = pageParams()
        val filterEqId = equipmentIdQuery(req)

        // Allow any authenticated user to get logs
        // if (!permissions.manageAllEquipment) {
        //     throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        // }

        val logs =
            if (filterEqId != null) {
                storage.getAllLogs().filter { it.Equipment == filterEqId }
            } else {
                storage.getAllLogs()
            }

        val sorted = logs.sortedWith(compareByDescending<ru.yarsu.Log> { it.LogDateTime }.thenBy { it.Id })

        ok(paginate(sorted, params))
    }
