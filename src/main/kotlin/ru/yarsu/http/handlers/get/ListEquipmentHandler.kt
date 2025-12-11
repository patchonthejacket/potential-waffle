package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.EquipmentListItem
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v2/equipment")
fun listEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val params = pageParams()
        val sorted = storage.getAllEquipment().sortedWith(compareBy({ it.Category }, { it.Id }))
        val paginated = paginate(sorted, params)
        val items = paginated.map { EquipmentListItem(it.Id.toString(), it.Equipment, it.IsUsed) }
        ok(items)
    }
