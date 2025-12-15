package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import java.time.LocalDate
import java.time.LocalDateTime

@Route(method = Method.GET, path = "/v3/equipment/by-time")
fun byTimeHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val timeStr =
            req.query("time")
                ?: throw ValidationException(Status.BAD_REQUEST, mapOf("time" to mapOf("Error" to "Missing required parameter")))

        val targetDate =
            try {
                // Парсим LocalDateTime, поддерживая формат с миллисекундами и без
                val dateTime =
                    if (timeStr.contains(".")) {
                        // Формат с миллисекундами: 2024-01-01T00:00:00.000
                        val parts = timeStr.split(".")
                        LocalDateTime.parse(parts[0])
                    } else {
                        LocalDateTime.parse(timeStr)
                    }
                dateTime.toLocalDate()
            } catch (e: Exception) {
                throw ValidationException(Status.BAD_REQUEST, mapOf("Error" to "Некорректные значения параметров"))
            }

        val params = pageParams()

        val filtered =
            storage
                .getAllEquipment()
                .filter {
                    try {
                        LocalDate.parse(it.GuaranteeDate) <= targetDate
                    } catch (e: Exception) {
                        false
                    }
                }.sortedWith(compareBy<ru.yarsu.Equipment> { it.Category }.thenBy { it.Id })
        val items =
            filtered.map {
                ru.yarsu.http.handlers.EquipmentByTimeItem(
                    Id = it.Id.toString(),
                    Equipment = it.Equipment,
                    GuaranteeDate = it.GuaranteeDate,
                )
            }

        ok(paginate(items, params))
    }
