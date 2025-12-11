package ru.yarsu.http.handlers.post

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.POST, path = "/v2/users")
fun postUserHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        try {
            validateJson {
                val name = requireText("Name")
                val email = requireText("Email")
                val position = requireText("Position")

                if (hasErrors()) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                if (name == null || email == null || position == null) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Валидация email (базовая проверка)
                if (!email.contains("@")) {
                    validateBusiness(
                        Result.failure<String>(
                            ru.yarsu.http.handlers
                                .FieldError("Email", root.get("Email")),
                        ),
                    )
                    if (hasErrors()) {
                        return@restful json(Status.BAD_REQUEST, collectErrors())
                    }
                }

                val userId = UUID.randomUUID()
                val newUser =
                    ru.yarsu.User(
                        Id = userId,
                        Name = name,
                        RegistrationDateTime = LocalDateTime.now(),
                        Email = email,
                        Position = position,
                    )
                storage.addUser(newUser)

                created(mapOf("UserId" to userId.toString()))
            }
        } catch (e: ru.yarsu.http.handlers.ValidationException) {
            json(e.status, e.body)
        }
    }
