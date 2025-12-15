package ru.yarsu.http.handlers.post

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.User
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.POST, path = "/v3/users")
fun postUserHandler(storage: EquipmentStorage): HttpHandler =
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
                    val Name = requireTextAllowEmpty("Name") ?: ""
                    val Email = requireTextAllowEmpty("Email") ?: ""
                    val Position = requireTextAllowEmpty("Position") ?: ""

                    // Новое поле Role
                    val roleRaw = requireOrThrow(requireText("Role"), "Role")
                    val Role =
                        if (roleRaw in listOf("User", "Admin", "Manager")) {
                            roleRaw
                        } else {
                            throw ValidationException(Status.BAD_REQUEST, mapOf("Role" to mapOf("Error" to "Invalid role")))
                        }
                }
            }

        if (!permissions.manageUsers) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        // Валидация Email (простая) - убрана, так как пустой разрешен

        val userId = UUID.randomUUID()
        val newUser =
            User(
                Id = userId,
                Name = data.Name,
                RegistrationDateTime = LocalDateTime.now(),
                Email = data.Email,
                Position = data.Position,
                Role = data.Role,
            )

        storage.addUser(newUser)

        created(mapOf("Id" to userId.toString()))
    }
