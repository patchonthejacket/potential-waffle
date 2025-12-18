package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.equipmentIdPathLens
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v3/equipment/{equipment-id}")
fun getEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val id = equipmentIdPathLens(req)

        val item =
            storage.getEquipment(id)
                ?: return@restful notFound(mapOf("Error" to "Элемент техники не найден", "EquipmentId" to id.toString()))

        // auth checked before parsing

        val responsiblePerson = storage.getUser(item.ResponsiblePerson)
        val user = item.User?.let { storage.getUser(it) }
        val logs =
            storage
                .getLogsByEquipment(id)
                .sortedWith(compareByDescending<ru.yarsu.Log> { it.LogDateTime }.thenBy { it.Id })
                .map {
                    ru.yarsu.http.handlers.LogRef(
                        Id = it.Id.toString(),
                        ResponsiblePerson = it.ResponsiblePerson,
                        Operation = it.Operation,
                        Text = it.Text,
                        LogDateTime = it.LogDateTime.toString(),
                    )
                }

        val detail =
            ru.yarsu.http.handlers.EquipmentDetail(
                Id = item.Id.toString(),
                Equipment = item.Equipment,
                Category = item.Category,
                GuaranteeDate = item.GuaranteeDate,
                IsUsed = item.IsUsed,
                Price = item.Price.toDouble(),
                Location = item.Location,
                ResponsiblePerson = item.ResponsiblePerson.toString(),
                ResponsiblePersonName = responsiblePerson?.Name,
                User = item.User?.toString(),
                UserName = user?.Name,
                Log = logs,
            )

        ok(detail)
    }
