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
        if (user == null) {
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
                    val userText = optionalTextAllowEmpty("User")
                    val User =
                        if (!userText.isNullOrBlank()) {
                            validateUserUuid("User", userText)
                        } else {
                            null
                        }

                    val Operation = requireText("Operation")
                    val Text = optionalTextAllowEmpty("Text") ?: ""
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
