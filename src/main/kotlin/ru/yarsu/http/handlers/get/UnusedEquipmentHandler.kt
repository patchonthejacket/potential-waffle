package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.EquipmentUnused
import ru.yarsu.http.handlers.parseCategories
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v2/equipment/unused")
fun unusedEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val all = storage.getAllEquipment()
        val params = pageParams()
        val categoryParam = req.query("category")
        val categories = parseCategories(categoryParam, all.map { it.Category }.toSet())

        val filtered =
            all
                .filter { !it.IsUsed }
                .filter { categories.isEmpty() || it.Category in categories }
                .sortedWith(compareBy({ it.Category }, { it.Id }))
        val paginated = paginate(filtered, params)
        val items = paginated.map { EquipmentUnused(it.Id.toString(), it.Equipment) }
        ok(items)
    }
