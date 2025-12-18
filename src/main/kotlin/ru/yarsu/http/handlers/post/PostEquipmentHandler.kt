package ru.yarsu.http.handlers.post

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.Equipment
import ru.yarsu.EquipmentStorage
import ru.yarsu.Log
import ru.yarsu.UserRole
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.FieldError
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.POST, path = "/v3/equipment")
fun postEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        // Check auth before any validation to prioritize 401 over 400
        if (user == null || user.Role != UserRole.Admin) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val data =
            validateJson {
                object {
                    val Equipment = requireTextAllowEmpty("Equipment")
                    val Category = requireCategoryAllowDefault("Category")
                    val GuaranteeDate = requireDateFieldAllowEmpty("GuaranteeDate")
                    val Price = requireNumberAllowZero("Price")
                    val Location = requireTextAllowEmpty("Location")

                    // User может быть: null (валидно), отсутствовать (валидно), или строка UUID
                    val userNode = root.get("User")
                    val User: java.util.UUID? =
                        when {
                            userNode == null -> null // поле отсутствует - валидно
                            userNode.isNull -> null // явный null - валидно
                            !userNode.isTextual -> {
                                validateBusiness(Result.failure<String>(FieldError("User", userNode, "Ожидается корректный UUID или null")))
                                null
                            }
                            userNode.asText().isBlank() -> null // пустая строка - null
                            else -> {
                                val r = parseUuid("User", userNode.asText())
                                validateBusiness(r)
                                val uuid = r.getOrNull()
                                // Проверяем существование пользователя
                                if (uuid != null && storage.getUser(uuid) == null) {
                                    validateBusiness(
                                        Result.failure<String>(FieldError("User", userNode, "Ожидается корректный UUID или null")),
                                    )
                                    null
                                } else {
                                    uuid
                                }
                            }
                        }

                    val Operation = requireText("Operation")

                    // Text может быть: null (-> ""), отсутствовать (-> ""), или строка
                    val textNode = root.get("Text")
                    val Text: String =
                        when {
                            textNode == null -> "" // поле отсутствует - default ""
                            textNode.isNull -> "" // явный null - default ""
                            !textNode.isTextual -> {
                                validateBusiness(Result.failure<String>(FieldError("Text", textNode, "Ожидается строка")))
                                ""
                            }
                            else -> textNode.asText()
                        }
                }
            }

        val currentUser = user

        val newId = UUID.randomUUID()
        val newEquipment =
            Equipment(
                Id = newId,
                Equipment = data.Equipment!!,
                Category = data.Category!!,
                GuaranteeDate = data.GuaranteeDate!!,
                IsUsed = data.User != null,
                Price = data.Price!!,
                Location = data.Location!!,
                ResponsiblePerson = currentUser.Id,
                User = data.User,
            )

        storage.addEquipment(newEquipment)

        val logId = UUID.randomUUID()
        storage.addLog(
            Log(
                Id = logId,
                Equipment = newId,
                ResponsiblePerson = currentUser.Id.toString(),
                Operation = data.Operation ?: "",
                Text = data.Text,
                LogDateTime = LocalDateTime.now(),
            ),
        )

        created(
            ru.yarsu.http.handlers
                .EquipmentResponse(EquipmentId = newId.toString(), LogId = logId.toString()),
        )
    }
