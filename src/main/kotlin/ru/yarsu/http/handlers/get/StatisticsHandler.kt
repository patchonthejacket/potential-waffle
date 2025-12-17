package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.UserRole
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.CategoryStat
import ru.yarsu.http.handlers.PersonStat
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import org.http4k.core.Status.Companion.OK as ok

@Route(method = Method.GET, path = "/v3/equipment/statistics")
fun statisticsHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val byType =
            req.query("by-type")
                ?: throw ValidationException(Status.BAD_REQUEST, mapOf("by-type" to mapOf("Error" to "Missing required parameter")))

        if (user == null || user.Role != UserRole.Admin) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        if (user?.Role != UserRole.Admin) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        when (byType) {
            "category" -> {
                val stats =
                    storage
                        .getAllEquipment()
                        .groupBy { it.Category }
                        .entries
                        .map { (category, items) ->
                            val total = items.fold(0.0) { acc, e -> acc + e.Price.toDouble() }
                            val usersCount = items.mapNotNull { it.User }.distinct().size
                            CategoryStat(category, items.size, usersCount, total)
                        }.sortedBy { it.Category }

                ok(mapOf("StatisticsByCategory" to stats))
            }
            "person" -> {
                val stats =
                    storage
                        .getAllEquipment()
                        .groupBy { it.ResponsiblePerson }
                        .entries
                        .map { (personId, items) ->
                            val person = storage.getUser(personId)
                            val personName = person?.Name ?: "Unknown"
                            val total = items.fold(0.0) { acc, e -> acc + e.Price.toDouble() }
                            val usersCount = items.mapNotNull { it.User }.distinct().size
                            PersonStat(personName, items.size, usersCount, total)
                        }.sortedByDescending { it.Person }

                ok(mapOf("StatisticsByPerson" to stats))
            }
            else -> throw ValidationException(Status.BAD_REQUEST, mapOf("by-type" to mapOf("Error" to "Invalid by-type parameter")))
        }
    }
