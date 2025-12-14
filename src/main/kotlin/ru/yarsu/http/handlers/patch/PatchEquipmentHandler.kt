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
        val id = equipmentIdPathLens(req)

        validateJson {
            val existing =
                storage.getEquipment(id)
                    ?: return@restful notFound(mapOf("Error" to "Equipment not found", "EquipmentId" to id.toString()))
            val equipmentName = optionalTextAllowEmpty("Equipment")
            val category = optionalCategory("Category")
            val guaranteeDate = optionalDate("GuaranteeDate")
            val priceRaw = optionalNumber("Price")
            val price = priceRaw?.let { validatePrice(it) }
            val location = optionalTextAllowEmpty("Location")

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

            if (user == null) {
                throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
            }

            var updatedResult = existing
            var changed = false

            storage.updateEquipment(id) { eq ->
                val newUserUuid: UUID? =
                    when {
                        !userField.fieldProvided -> eq.User
                        userField.explicitNull -> null
                        userUuid != null -> userUuid
                        else -> eq.User
                    }

                val newRpUuid: UUID = responsiblePersonUuid ?: eq.ResponsiblePerson

                val updated =
                    eq.copy(
                        Equipment = if (equipmentName != null) equipmentName else eq.Equipment,
                        Category = category ?: eq.Category,
                        GuaranteeDate = guaranteeDate ?: eq.GuaranteeDate,
                        IsUsed = newUserUuid != null,
                        Price = price ?: eq.Price,
                        Location = if (location != null) location else eq.Location,
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
                        Operation = operation.toString(),
                        Text = text.toString(),
                        LogDateTime = LocalDateTime.now(),
                    ),
                )
                // For successful edit, return 201 with EquipmentId and LogId
                created(
                    ru.yarsu.http.handlers
                        .EquipmentResponse(EquipmentId = id.toString(), LogId = logId.toString()),
                )
            } else {
                ApiResult.NoContent
            }
        }
    }
