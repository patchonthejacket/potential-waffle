package ru.yarsu.http.handlers.post

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.EquipmentResponse
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.POST, path = "/v2/equipment")
fun postEquipmentHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        try {
            validateJson {
                val equipment = requireTextAllowEmpty("Equipment")
                val category = requireText("Category")
                val price = requireNumber("Price")
                val guaranteeDate = requireDate("GuaranteeDate")
                val location = requireTextAllowEmpty("Location")
                val responsiblePerson = requireText("ResponsiblePerson")
                val user = optionalText("User")
                val operation = requireText("Operation")
                val text = requireText("Text")

                // Валидация
                category?.let { validateCategory(it) }
                price?.let { validatePrice(it) }
                val responsiblePersonUuid = validateResponsiblePersonUuid("ResponsiblePerson", responsiblePerson)
                val userUuid = user?.takeUnless { it.isBlank() }?.let { validateUserUuid("User", it) }

                if (hasErrors()) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Проверяем обязательные поля
                if (equipment == null || category == null || price == null ||
                    guaranteeDate == null || location == null || responsiblePerson == null ||
                    operation == null || text == null
                ) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Создаём оборудование
                val equipmentId = UUID.randomUUID()
                storage.addEquipment(
                    ru.yarsu.Equipment(
                        Id = equipmentId,
                        Equipment = equipment,
                        Category = category,
                        GuaranteeDate = guaranteeDate,
                        IsUsed = userUuid != null,
                        Price = price,
                        Location = location,
                        ResponsiblePerson = responsiblePerson,
                        User = userUuid?.toString(),
                    ),
                )

                // Добавляем запись в журнал
                val logId = UUID.randomUUID()
                storage.addLog(
                    ru.yarsu.Log(
                        Id = logId,
                        Equipment = equipmentId,
                        ResponsiblePerson = responsiblePerson,
                        Operation = operation,
                        Text = text,
                        LogDateTime = LocalDateTime.now(),
                    ),
                )

                created(
                    EquipmentResponse(
                        EquipmentId = equipmentId.toString(),
                        LogId = logId.toString(),
                    ),
                )
            }
        } catch (e: ru.yarsu.http.handlers.ValidationException) {
            json(e.status, e.body)
        }
    }
