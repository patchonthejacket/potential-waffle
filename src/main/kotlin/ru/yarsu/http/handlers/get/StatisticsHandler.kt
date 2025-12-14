package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.CategoryStat
import ru.yarsu.http.handlers.PersonStat
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import java.util.UUID

@Route(method = Method.GET, path = "/v3/equipment/statistics")
fun statisticsHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val byType =
            req.query("by-type")
                ?: throw ValidationException(Status.BAD_REQUEST, mapOf("by-type" to mapOf("Error" to "Missing required parameter")))

        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        when (byType) {
            "category" -> {
                if (!permissions.manageAllEquipment) {
                    throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
                }

                // Учитываем только технику под ответственностью текущего пользователя (Admin)
                val currentUserId = user?.Id
                val stats =
                    storage
                        .getAllEquipment()
                        .filter { it.ResponsiblePerson == currentUserId }
                        .groupBy { it.Category }
                        .entries
                        .map { (category, items) ->
                            val total = items.fold(0.0) { acc, e -> acc + e.Price.toDouble() }
                            val usersCount = items.mapNotNull { it.User }.distinct().size
                            CategoryStat(category, items.size, usersCount, total)
                        }.sortedBy { it.Category } // Лексикографически

                ok(mapOf("StatisticsByCategory" to stats))
            }
            "person" -> {
                // Группировка по ИМЕНИ ответственного
                val stats =
                    storage
                        .getAllEquipment()
                        .groupBy { equipment ->
                            val rpUuid = equipment.ResponsiblePerson
                            storage.getUser(rpUuid)?.Name ?: "Unknown"
                        }.entries
                        .map { (personName, items) ->
                            val total = items.fold(0.0) { acc, e -> acc + e.Price.toDouble() }
                            val usersCount = items.mapNotNull { it.User }.distinct().size
                            PersonStat(personName, items.size, usersCount, total)
                        }.sortedBy { it.Person }

                ok(mapOf("StatisticsByPerson" to stats))
            }
            else -> throw ValidationException(
                Status.BAD_REQUEST,
                mapOf("by-type" to mapOf("Error" to "Invalid value. Use 'category' or 'person'")),
            )
        }
    }
