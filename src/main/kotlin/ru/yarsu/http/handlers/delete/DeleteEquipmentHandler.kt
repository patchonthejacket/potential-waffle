package ru.yarsu.http.handlers.delete

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.equipmentIdPathLens
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.DELETE, path = "/v2/equipment/{equipment-id}")
fun deleteEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = equipmentIdPathLens(req)

        val existing = storage.getEquipment(id)
        if (existing == null) {
            notFound(mapOf("Error" to "Equipment not found", "EquipmentId" to id.toString()))
        } else {
            // создаём запись в журнале о списании до удаления
            val logId = UUID.randomUUID()
            val log =
                ru.yarsu.Log(
                    Id = logId,
                    Equipment = id,
                    ResponsiblePerson = existing.ResponsiblePerson,
                    Operation = "Списание: ${existing.Equipment}",
                    Text = "",
                    LogDateTime = LocalDateTime.now(),
                )
            storage.addLog(log)

            storage.removeEquipment(id)
            // В ответе ожидают только идентификатор записи журнала
            ok(mapOf("LogId" to logId.toString()))
        }
    }
