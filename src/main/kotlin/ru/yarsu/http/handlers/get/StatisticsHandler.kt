package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.CategoryStat
import ru.yarsu.http.handlers.PersonStat
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v2/equipment/statistics")
fun statisticsHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val byType = req.query("by-type") ?: throw IllegalArgumentException("Missing required parameter: by-type")
        when (byType) {
            "category" -> {
                val grouped = storage.getAllEquipment().groupBy { it.Category }
                val stats =
                    grouped.entries
                        .map {
                            val total = it.value.fold(0.0) { acc, e -> acc + e.Price.toDouble() }
                            // Кол-во уникальных пользователей, а не единиц техники
                            val usersCount =
                                it.value
                                    .mapNotNull { e -> e.User }
                                    .distinct()
                                    .size
                            CategoryStat(it.key, it.value.size, usersCount, total)
                        }
                        // По спецификации — лексикографический порядок по Category
                        .sortedBy { it.Category }
                ok(mapOf("StatisticsByCategory" to stats))
            }
            "person" -> {
                val stats =
                    storage
                        .getAllEquipment()
                        .groupBy { equipment ->
                            // Группируем по ФИО пользователя (Name), а не по UUID
                            runCatching { java.util.UUID.fromString(equipment.ResponsiblePerson) }
                                .getOrNull()
                                ?.let { uuid -> storage.getUser(uuid)?.Name }
                                ?: equipment.ResponsiblePerson
                        }.entries
                        .map {
                            val total = it.value.fold(0.0) { acc, e -> acc + e.Price.toDouble() }
                            // Кол-во уникальных пользователей (User), использующих технику этого МОЛ
                            val usersCount =
                                it.value
                                    .mapNotNull { e -> e.User }
                                    .distinct()
                                    .size
                            PersonStat(it.key, it.value.size, usersCount, total)
                        }
                        // По спецификации — лексикографический порядок по Person
                        .sortedBy { it.Person }
                ok(mapOf("StatisticsByPerson" to stats))
            }
            else -> json(Status.BAD_REQUEST, mapOf("error" to "Invalid by-type value. Use 'category' or 'person'"))
        }
    }
