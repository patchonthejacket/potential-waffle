package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.EquipmentDetail
import ru.yarsu.http.handlers.LogRef
import ru.yarsu.http.handlers.equipmentIdPathLens
import ru.yarsu.http.handlers.restful
import java.util.UUID

@Route(method = Method.GET, path = "/v2/equipment/{equipment-id}")
fun getEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = equipmentIdPathLens(req)
        val eq = storage.getEquipment(id)
        if (eq == null) {
            notFound(mapOf("Error" to "Equipment not found", "EquipmentId" to id.toString()))
        } else {
            val responsiblePersonUser =
                runCatching { UUID.fromString(eq.ResponsiblePerson) }
                    .getOrNull()
                    ?.let { storage.getUser(it) }
            val userUser =
                eq.User?.let { userId ->
                    runCatching { UUID.fromString(userId) }
                        .getOrNull()
                        ?.let { storage.getUser(it) }
                }

            val logs =
                storage
                    .getLogsByEquipment(id)
                    .sortedWith(compareByDescending<ru.yarsu.Log> { it.LogDateTime }.thenBy { it.Id })
                    .map {
                        LogRef(
                            it.Id.toString(),
                            it.ResponsiblePerson,
                            it.Operation,
                            it.Text,
                            it.LogDateTime.toString(),
                        )
                    }
            val detail =
                EquipmentDetail(
                    eq.Id.toString(),
                    eq.Equipment,
                    eq.Category,
                    eq.GuaranteeDate,
                    eq.IsUsed,
                    eq.Price.toDouble(),
                    eq.Location,
                    eq.ResponsiblePerson,
                    responsiblePersonUser?.Name,
                    eq.User,
                    userUser?.Name,
                    logs,
                )
            ok(detail)
        }
    }
