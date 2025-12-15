package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.parseCategories
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v3/equipment/unused")
fun unusedEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val params = pageParams()
        val categoryParam = req.query("category")

        val allUnused = storage.getAllEquipment().filter { !it.IsUsed }

        val filtered =
            if (categoryParam != null && categoryParam.isNotBlank()) {
                try {
                    val categories = parseCategories(categoryParam, setOf("ПК", "Монитор", "Принтер", "Телефон", "Другое"))
                    if (categories.isEmpty()) {
                        allUnused
                    } else {
                        allUnused.filter { it.Category in categories }
                    }
                } catch (e: Exception) {
                    throw ValidationException(Status.BAD_REQUEST, mapOf("Error" to "Некорректные значения параметров"))
                }
            } else {
                allUnused
            }

        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val sorted = filtered.sortedWith(compareBy<ru.yarsu.Equipment> { it.Category }.thenBy { it.Id })
        val items =
            sorted.map {
                ru.yarsu.http.handlers.EquipmentUnusedItem(
                    Id = it.Id.toString(),
                    Equipment = it.Equipment,
                )
            }

        ok(paginate(items, params))
    }
