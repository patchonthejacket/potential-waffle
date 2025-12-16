package ru.yarsu.http.handlers.patch

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

@Route(method = Method.PATCH, path = "/v3/equipment/{equipment-id}")
fun patchEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        // Allow any authenticated user to edit equipment
        // if (user == null) {
        //     throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        // }

        val id = equipmentIdPathLens(req)

        val existing =
            storage.getEquipment(id)
                ?: return@restful notFound(mapOf("Error" to "Оборудование не найдено"))

        // Allow any authenticated user to edit equipment
        // if (!permissions.manageAllEquipment && existing.ResponsiblePerson != user?.Id) {
        //     throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        // }

        val data =
            validateJson {
                val equipmentName = requireTextAllowEmpty("Equipment")
                val categoryRaw = requireText("Category")
                val category = categoryRaw?.let { validateCategory(it) }
                val guaranteeDate = optionalDate("GuaranteeDate")
                val priceRaw = optionalNumber("Price")
                val price = priceRaw?.let { validatePrice(it) }
                val location = requireTextAllowEmpty("Location")

                val responsiblePersonStr = optionalTextAllowEmpty("ResponsiblePerson")
                val responsiblePersonUuid =
                    if (responsiblePersonStr != null) {
                        validateResponsiblePersonUuid("ResponsiblePerson", responsiblePersonStr)
                    } else {
                        null
                    }

                val userField = validateUserField()
                val userUuid =
                    if (userField.fieldProvided && !userField.explicitNull && userField.value != null) {
                        validateUserUuid("User", userField.value)
                    } else {
                        null
                    }

                val operation = requireText("Operation")
                val text = requireTextAllowEmpty("Text")

                if (hasErrors()) {
                    throw ValidationException(Status.BAD_REQUEST, collectErrors())
                }

                object {
                    val equipmentName = equipmentName
                    val category = category
                    val guaranteeDate = guaranteeDate
                    val price = price
                    val location = location
                    val responsiblePersonUuid = responsiblePersonUuid
                    val userUuid = userUuid
                    val operation = operation
                    val text = text
                }
            }

        var updatedResult = existing
        var changed = false

        storage.updateEquipment(id) { eq ->
            val newUserUuid: UUID? =
                when {
                    data.userUuid != null -> data.userUuid
                    else -> eq.User
                }

            val newRpUuid: UUID = data.responsiblePersonUuid ?: eq.ResponsiblePerson

            val updated =
                eq.copy(
                    Equipment = data.equipmentName ?: eq.Equipment,
                    Category = data.category ?: eq.Category,
                    GuaranteeDate = data.guaranteeDate ?: eq.GuaranteeDate,
                    IsUsed = newUserUuid != null,
                    Price = data.price ?: eq.Price,
                    Location = data.location ?: eq.Location,
                    ResponsiblePerson = newRpUuid,
                    User = newUserUuid,
                )

            if (updated != eq) {
                changed = true
                updatedResult = updated
            }
            updated
        }

        if (changed) {
            val logId = UUID.randomUUID()
            storage.addLog(
                Log(
                    Id = logId,
                    Equipment = id,
                    ResponsiblePerson = user?.Id?.toString() ?: "Unknown",
                    Operation = data.operation.toString(),
                    Text = data.text.toString(),
                    LogDateTime = LocalDateTime.now(),
                ),
            )
            // For successful edit, return 200 with Id
            ok(mapOf("Id" to id.toString()))
        } else {
            ApiResult.NoContent
        }
    }
