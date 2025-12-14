package ru.yarsu.http.handlers.post

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.POST, path = "/v2/log")
fun postLogHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        try {
            validateJson {
                val equipment = requireText("Equipment")
                val responsiblePerson = requireText("ResponsiblePerson")
                val operation = requireText("Operation")
                val text = requireText("Text")

                if (hasErrors()) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                if (equipment == null || responsiblePerson == null || operation == null || text == null) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Валидация UUID оборудования
                val equipmentUuidResult = parseUuid("Equipment", equipment)
                validateBusiness(equipmentUuidResult)
                val equipmentId = equipmentUuidResult.getOrNull()
                if (equipmentId == null) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }
                if (storage.getEquipment(equipmentId) == null) {
                    validateBusiness(
                        Result.failure<UUID>(
                            ru.yarsu.http.handlers
                                .FieldError("Equipment", root.get("Equipment")),
                        ),
                    )
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Валидация UUID ответственного лица
                val responsiblePersonUuid = validateResponsiblePersonUuid("ResponsiblePerson", responsiblePerson)

                if (hasErrors()) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

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

                created(mapOf("LogId" to logId.toString()))
            }
        } catch (e: ru.yarsu.http.handlers.ValidationException) {
            json(e.status, e.body)
        }
    }
