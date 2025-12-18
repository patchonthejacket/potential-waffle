package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.EquipmentUnused
import ru.yarsu.http.handlers.parseCategories
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v1/equipment/search")
fun searchByCategoryHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        // Public (lab #1): if Authorization header present but invalid -> 401
        val hasAuthHeader = req.header("Authorization") != null
        if (hasAuthHeader && user == null) {
            return@restful json(org.http4k.core.Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }
        when (val categoryParameter = req.query("category")) {
            null, "" -> json(org.http4k.core.Status.BAD_REQUEST, mapOf("error" to "category is required"))
            else -> {
                val all = storage.getAllEquipment()
                val params = pageParams()
                val categories = parseCategories(categoryParameter, all.map { it.Category }.toSet())
                val filtered = all.filter { it.Category in categories }.sortedBy { it.Equipment }
                val paginated = paginate(filtered, params)
                val items = paginated.map { EquipmentUnused(it.Id.toString(), it.Equipment) }
                ok(items)
            }
        }
    }
