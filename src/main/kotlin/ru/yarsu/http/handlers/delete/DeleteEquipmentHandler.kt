package ru.yarsu.http.handlers.delete

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.Log
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ApiResult
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.equipmentIdPathLens
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.DELETE, path = "/v3/equipment/{equipment-id}")
fun deleteEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = equipmentIdPathLens(req)

        val existing =
            storage.getEquipment(id)
                ?: return@restful notFound(mapOf("Error" to "Оборудование не найдено", "EquipmentId" to id.toString()))

        if (!permissions.manageAllEquipment) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val currentUser = user
        val logs = storage.getLogsByEquipment(id)

        if (logs.isEmpty()) {
            // Если нет журнала, возвращаем 200 OK с EquipmentId
            storage.removeEquipment(id)
            ok(
                ru.yarsu.http.handlers
                    .EquipmentResponse(EquipmentId = id.toString(), LogId = null),
            )
        } else {
            // Если есть журнал, создаем запись и возвращаем 200 OK с EquipmentId и LogId
            val logId = UUID.randomUUID()
            storage.addLog(
                Log(
                    Id = logId,
                    Equipment = id,
                    ResponsiblePerson = currentUser?.Id.toString(),
                    Operation = "Списание: ${existing.Equipment}",
                    Text = "",
                    LogDateTime = LocalDateTime.now(),
                ),
            )
            storage.removeEquipment(id)
            ok(
                ru.yarsu.http.handlers
                    .EquipmentResponse(EquipmentId = id.toString(), LogId = logId.toString()),
            )
        }
    }
