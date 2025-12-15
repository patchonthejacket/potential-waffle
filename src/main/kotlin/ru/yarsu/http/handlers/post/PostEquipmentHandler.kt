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
        // Check auth first before JSON validation
        if (user == null) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        if (!permissions.manageAllEquipment) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

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

        val currentUser = user ?: throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Auth required"))

        val newId = UUID.randomUUID()
        val newEquipment =
            Equipment(
                Id = newId,
                Equipment = data.Equipment,
                Category = data.Category ?: "Другое", // Default category if not provided
                GuaranteeDate = data.GuaranteeDate ?: "", // Default empty string if not provided
                IsUsed = data.User != null,
                Price = data.Price ?: BigDecimal.ZERO, // Default price if not provided
                Location = data.Location,
                ResponsiblePerson = currentUser.Id,
                User = data.User,
            )

        storage.addEquipment(newEquipment)

        // Only add log if Operation is provided
        if (data.Operation != null) {
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
