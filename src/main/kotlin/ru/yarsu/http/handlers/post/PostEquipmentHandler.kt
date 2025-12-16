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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.POST, path = "/v3/equipment")
fun postEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        // Allow any authenticated user to add equipment
        // if (user == null) {
        //     throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        // }

        val data =
            validateJson {
                object {
                    val Equipment = optionalTextAllowEmpty("Equipment") ?: ""
                    val rawCat = optionalText("Category")
                    val Category = rawCat?.let { validateCategory(it) }
                    val GuaranteeDate = optionalDate("GuaranteeDate")
                    val Price = optionalNumber("Price")
                    val Location = optionalTextAllowEmpty("Location") ?: ""

                    // User (UUID?), если передан
                    val userText = optionalTextAllowEmpty("User")
                    val User =
                        if (!userText.isNullOrBlank()) {
                            validateUserUuid("User", userText)
                        } else {
                            null
                        }

                    // Поля для журнала (Operation, Text) при создании - делаем опциональными
                    val Operation = optionalText("Operation")
                    val Text = optionalTextAllowEmpty("Text") ?: ""
                }
            }

        // Проверяем обязательные поля после валидации
        val errors = mutableMapOf<String, Any>()

        // Equipment can be empty
        // if (data.Equipment.isNullOrBlank()) {
        //     errors["Equipment"] = mapOf("Error" to "Отсутствует поле")
        // }

        if (data.rawCat.isNullOrBlank()) {
            errors["Category"] = mapOf("Error" to "Отсутствует поле")
        } else if (data.Category == null) {
            errors["Category"] = mapOf("Value" to data.rawCat, "Error" to "Неверная категория")
        }

        if (data.GuaranteeDate == null) {
            errors["GuaranteeDate"] = mapOf("Error" to "Отсутствует поле")
        }

        if (data.Price == null) {
            errors["Price"] = mapOf("Error" to "Отсутствует поле")
        }

        if (data.Location.isBlank()) {
            // Location может быть пустым - это OK
        }

        if (data.Operation.isNullOrBlank()) {
            errors["Operation"] = mapOf("Error" to "Отсутствует поле")
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(Status.BAD_REQUEST, errors)
        }

        val currentUser = user ?: throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Auth required"))

        val newId = UUID.randomUUID()
        val newEquipment =
            Equipment(
                Id = newId,
                Equipment = data.Equipment,
                Category = data.Category ?: "Другое",
                GuaranteeDate = data.GuaranteeDate ?: "",
                IsUsed = data.User != null,
                Price = data.Price ?: BigDecimal.ZERO,
                Location = data.Location,
                ResponsiblePerson = currentUser.Id,
                User = data.User,
            )

        storage.addEquipment(newEquipment)

        // Only add log if Operation is provided
        if (!data.Operation.isNullOrBlank()) {
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
        }

        created(mapOf("Id" to newId.toString()))
    }
