package ru.yarsu.http.handlers.post

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.Equipment
import ru.yarsu.EquipmentStorage
import ru.yarsu.Log
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.POST, path = "/v3/equipment")
fun postEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val data =
            validateJson {
                fun <T> requireOrThrow(
                    value: T?,
                    fieldName: String,
                ): T =
                    value ?: throw ValidationException(
                        Status.BAD_REQUEST,
                        mapOf(fieldName to mapOf("Error" to "Поле обязательно")),
                    )

                object {
                    val Equipment = requireTextAllowEmpty("Equipment") ?: ""
                    val rawCat = requireOrThrow(requireText("Category"), "Category")
                    val Category =
                        validateCategory(rawCat)
                            ?: throw ValidationException(Status.BAD_REQUEST, mapOf("Category" to mapOf("Error" to "Неверная категория")))

                    val GuaranteeDate = requireOrThrow(requireDate("GuaranteeDate"), "GuaranteeDate")
                    val Price = requireOrThrow(requireNumber("Price"), "Price")
                    val Location = requireTextAllowEmpty("Location") ?: ""

                    // User (UUID?), если передан
                    val userText = optionalTextAllowEmpty("User")
                    val User =
                        if (!userText.isNullOrBlank()) {
                            validateUserUuid("User", userText)
                                ?: throw ValidationException(
                                    Status.BAD_REQUEST,
                                    mapOf("User" to mapOf("Error" to "Пользователь не найден")),
                                )
                        } else {
                            null
                        }

                    // Поля для журнала (Operation, Text) при создании
                    val Operation = requireOrThrow(requireText("Operation"), "Operation")
                    val Text = requireOrThrow(requireText("Text"), "Text")
                }
            }

        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        if (!permissions.manageAllEquipment) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val currentUser = user ?: throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Auth required"))

        val newId = UUID.randomUUID()
        val newEquipment =
            Equipment(
                Id = newId,
                Equipment = data.Equipment,
                Category = data.Category,
                GuaranteeDate = data.GuaranteeDate,
                IsUsed = data.User != null,
                Price = data.Price,
                Location = data.Location,
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
                Operation = data.Operation,
                Text = data.Text,
                LogDateTime = LocalDateTime.now(),
            ),
        )

        created(mapOf("Id" to newId.toString()))
    }
