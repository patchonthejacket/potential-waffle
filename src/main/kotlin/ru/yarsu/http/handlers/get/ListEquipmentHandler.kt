package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v3/equipment")
fun listEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val params = pageParams()

        val sorted = storage.getAllEquipment().sortedWith(compareBy<ru.yarsu.Equipment> { it.Category }.thenBy { it.Id })
        val items =
            sorted.map {
                ru.yarsu.http.handlers.EquipmentListItem(
                    Id = it.Id.toString(),
                    Equipment = it.Equipment,
                    IsUsed = it.IsUsed,
                )
            }

        ok(paginate(items, params))
    }
