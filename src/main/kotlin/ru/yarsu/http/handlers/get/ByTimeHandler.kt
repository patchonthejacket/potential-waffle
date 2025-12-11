package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.EquipmentByTime
import ru.yarsu.http.handlers.restful
import java.time.LocalDate

@Route(method = Method.GET, path = "/v2/equipment/by-time")
fun byTimeHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val timeStr = req.query("time") ?: throw IllegalArgumentException("Missing required parameter: time")
        val time =
            try {
                java.time.LocalDateTime.parse(timeStr)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid time format. Expected ISO 8601 format (e.g., 2024-12-31T00:00:00)")
            }
        val params = pageParams()
        val filtered =
            storage
                .getAllEquipment()
                .filter { LocalDate.parse(it.GuaranteeDate) <= time.toLocalDate() }
                .sortedWith(compareBy({ it.Category }, { it.Id }))
        val paginated = paginate(filtered, params)
        val items = paginated.map { EquipmentByTime(it.Id.toString(), it.Equipment, it.GuaranteeDate) }
        ok(items)
    }
