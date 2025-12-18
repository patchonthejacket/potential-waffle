package ru.yarsu.http.handlers.put

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ApiResult
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import ru.yarsu.http.handlers.userIdPathLens
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.PUT, path = "/v2/users/{user-id}")
fun putUserHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        if (user == null || user?.Role != ru.yarsu.UserRole.Manager) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val id = userIdPathLens(req)
        val existing =
            storage.getUser(id)
                ?: return@restful notFound(mapOf("Error" to "User not found", "UserId" to id.toString()))

        try {
            validateJson {
                // В PUT все поля обязательны
                val name = requireText("Name")
                val email = requireText("Email")
                val position = requireText("Position")

                if (hasErrors()) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Проверяем обязательные поля
                if (name == null || email == null || position == null) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Валидация email
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

                // Применяем полное обновление (PUT заменяет весь ресурс)
                storage.updateUser(id) { user ->
                    user.copy(
                        Name = name,
                        Email = email,
                        Position = position,
                        // RegistrationDateTime сохраняем из оригинала
                        RegistrationDateTime = user.RegistrationDateTime,
                    )
                }

                ApiResult.NoContent
            }
        } catch (e: ru.yarsu.http.handlers.ValidationException) {
            json(e.status, e.body)
        }
    }
