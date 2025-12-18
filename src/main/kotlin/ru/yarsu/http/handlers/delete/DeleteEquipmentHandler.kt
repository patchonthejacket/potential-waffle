package ru.yarsu.http.handlers.delete

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.Log
import ru.yarsu.UserRole
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
        if (user == null || user.Role != UserRole.Admin) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val id = equipmentIdPathLens(req)

        val existing =
            storage.getEquipment(id)
                ?: return@restful notFound(mapOf("Error" to "Оборудование не найдено", "EquipmentId" to id.toString()))

        // Согласно спецификации: "Разрешено только для оборудования под ответственностью пользователя"
        if (existing.ResponsiblePerson != user?.Id) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        storage.removeEquipment(id)

        // Добавление записи в журнал согласно спецификации
        val logId = UUID.randomUUID()
        storage.addLog(
            Log(
                Id = logId,
                Equipment = existing.Id,
                ResponsiblePerson = existing.ResponsiblePerson.toString(),
                Operation = "Списание: ${existing.Equipment}",
                Text = "",
                LogDateTime = LocalDateTime.now(),
            ),
        )

        ok(mapOf("LogId" to logId.toString()))
    }
