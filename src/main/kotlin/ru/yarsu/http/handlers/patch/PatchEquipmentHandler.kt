package ru.yarsu.http.handlers.patch

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

@Route(method = Method.PATCH, path = "/v3/equipment/{equipment-id}")
fun patchEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        // Требуем авторизацию: любой авторизованный пользователь
        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val id = equipmentIdPathLens(req)

        val existing =
            storage.getEquipment(id)
                ?: return@restful notFound(mapOf("EquipmentId" to id.toString(), "Error" to "Элемент техники не найден"))

        // Проверка прав на объект как у друга
        when (user.Role) {
            UserRole.User -> {
                if (existing.User != user.Id) {
                    throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
                }
            }
            UserRole.Admin -> {
                if (existing.ResponsiblePerson != user.Id) {
                    throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
                }
            }
            else -> throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val data =
            validateJson {
                // Ограничение полей для User
                if (user.Role == UserRole.User) {
                    val allowed = setOf("Location", "Operation", "Text")
                    root.fieldNames().asSequence().firstOrNull { it !in allowed }?.let {
                        throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
                    }
                }

                // Проверка обязательных
                val required =
                    if (user.Role ==
                        UserRole.User
                    ) {
                        setOf("Location", "Operation", "Text")
                    } else {
                        setOf("Equipment", "Location", "Operation", "Text")
                    }
                required.forEach { field ->
                    when {
                        !root.has(field) ->
                            validate(
                                Result.failure<String>(
                                    ru.yarsu.http.handlers
                                        .FieldError(field, null),
                                ),
                            )
                        root
                            .get(
                                field,
                            ).isNull ->
                            validate(
                                Result.failure<String>(
                                    ru.yarsu.http.handlers
                                        .FieldError(field, root.get(field)),
                                ),
                            )
                    }
                }

                // Поля
                val equipmentName = optionalTextAllowEmpty("Equipment")
                val category = optionalCategory("Category")
                val guaranteeDate = optionalDate("GuaranteeDate")
                val price = optionalNumber("Price")?.also { validatePrice(it) }
                val location = optionalTextAllowEmpty("Location")
                val responsiblePersonUuid =
                    optionalText(
                        "ResponsiblePerson",
                    )?.let { validateResponsiblePersonUuid("ResponsiblePerson", it) }

                val userField = validateUserField()
                val userUuid = if (userField.fieldProvided && userField.value != null) validateUserUuid("User", userField.value) else null

                val operation = requireText("Operation")
                val text = optionalTextAllowEmpty("Text")

                object {
                    val equipmentName = equipmentName
                    val category = category
                    val guaranteeDate = guaranteeDate
                    val price = price
                    val location = location
                    val responsiblePersonUuid = responsiblePersonUuid
                    val userUuid = userUuid
                    val userProvided = userField.fieldProvided
                    val operation = operation
                    val text = text
                }
            }

        var updatedResult = existing
        var changed = false

        storage.updateEquipment(id) { eq ->
            val newUserUuid: UUID? = if (data.userProvided) data.userUuid else eq.User

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
                    ResponsiblePerson = updatedResult.ResponsiblePerson.toString(),
                    Operation = data.operation ?: "",
                    Text = data.text ?: "",
                    LogDateTime = LocalDateTime.now(),
                ),
            )
            // При изменениях возвращаем 201 только с LogId (как у друга)
            created(mapOf("LogId" to logId.toString()))
        } else {
            ApiResult.NoContent
        }
    }
