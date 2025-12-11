package ru.yarsu.http.handlers.patch

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ApiResult
import ru.yarsu.http.handlers.equipmentIdPathLens
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.PATCH, path = "/v2/equipment/{equipment-id}")
fun patchEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = equipmentIdPathLens(req)
        val existing =
            storage.getEquipment(id)
                ?: return@restful notFound(mapOf("Error" to "Equipment not found", "EquipmentId" to id.toString()))

        try {
            validateJson {
                // Обязательные поля
                val equipment = requireTextAllowEmpty("Equipment")
                val location = requireTextAllowEmpty("Location")
                val responsiblePerson = requireText("ResponsiblePerson")
                val operation = requireText("Operation")
                val text = requireTextAllowEmpty("Text")

                // Опциональные поля
                val price = optionalNumber("Price")
                val guaranteeDate = optionalDate("GuaranteeDate")
                val category = optionalCategory("Category")
                val userField = validateUserField()

                // Бизнес-валидация
                category?.let { validateCategory(it) }
                price?.let { validatePrice(it) }
                val responsiblePersonUuid = validateResponsiblePersonUuid("ResponsiblePerson", responsiblePerson)
                val parsedUserUuid =
                    if (userField.fieldProvided && !userField.explicitNull && userField.value != null) {
                        validateUserUuid("User", userField.value)
                    } else {
                        null
                    }

                if (hasErrors()) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Проверяем обязательные поля
                if (equipment == null || location == null || responsiblePerson == null ||
                    operation == null || text == null
                ) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Применяем обновление
                var changed = false
                storage.updateEquipment(id) { eq ->
                    val newUser =
                        when {
                            !userField.fieldProvided -> eq.User
                            userField.explicitNull -> null
                            parsedUserUuid != null -> parsedUserUuid.toString()
                            else -> eq.User
                        }
                    val updated =
                        eq.copy(
                            Equipment = equipment,
                            Category = category ?: eq.Category,
                            GuaranteeDate = guaranteeDate ?: eq.GuaranteeDate,
                            IsUsed = newUser != null,
                            Price = price ?: eq.Price,
                            Location = location,
                            ResponsiblePerson = responsiblePerson,
                            User = newUser,
                        )
                    if (updated != eq) {
                        changed = true
                    }
                    updated
                }

                if (changed) {
                    val logId = UUID.randomUUID()
                    storage.addLog(
                        ru.yarsu.Log(
                            Id = logId,
                            Equipment = id,
                            ResponsiblePerson = responsiblePerson,
                            Operation = operation,
                            Text = text,
                            LogDateTime = LocalDateTime.now(),
                        ),
                    )
                    created(mapOf("LogId" to logId.toString()))
                } else {
                    ApiResult.NoContent
                }
            }
        } catch (e: ru.yarsu.http.handlers.ValidationException) {
            json(e.status, e.body)
        }
    }
