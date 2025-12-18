package ru.yarsu.http.handlers.patch

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ApiResult
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import ru.yarsu.http.handlers.userIdPathLens
import java.util.UUID

@Route(method = Method.PATCH, path = "/v2/users/{user-id}")
fun patchUserHandler(storage: EquipmentStorage): HttpHandler =
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
                val name = optionalText("Name")
                val email = optionalText("Email")
                val position = optionalText("Position")

                if (hasErrors()) {
                    return@restful json(Status.BAD_REQUEST, collectErrors())
                }

                // Валидация email если передан
                email?.let {
                    if (!it.contains("@")) {
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
                }

                // Применяем обновление
                var changed = false
                storage.updateUser(id) { user ->
                    val updated =
                        user.copy(
                            Name = name ?: user.Name,
                            Email = email ?: user.Email,
                            Position = position ?: user.Position,
                        )
                    if (updated != user) {
                        changed = true
                    }
                    updated
                }

                if (changed) {
                    created(mapOf("UserId" to id.toString()))
                } else {
                    ApiResult.NoContent
                }
            }
        } catch (e: ru.yarsu.http.handlers.ValidationException) {
            json(e.status, e.body)
        }
    }
