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
            try {
                validateJson {
                    object {
                        val Name = optionalTextAllowEmpty("Name") ?: ""
                        val Email = optionalTextAllowEmpty("Email") ?: ""
                        val Position = optionalTextAllowEmpty("Position") ?: ""

                        // Новое поле Role
                        val roleRaw = optionalTextAllowEmpty("Role") ?: ""
                        val Role =
                            if (roleRaw.isEmpty() || roleRaw in listOf("User", "Admin", "Manager")) {
                                roleRaw.ifEmpty { "User" } // default to User if empty
                            } else {
                                roleRaw // invalid, but we will check later
                            }
                    }
                }
            } catch (e: ValidationException) {
                return@restful ok(e.body)
            }

        // Check for errors
        val errors = mutableMapOf<String, Any>()
        if (data.Name.isBlank()) {
            errors["Name"] = mapOf("Error" to "Поле обязательно")
        }
        if (data.Email.isBlank()) {
            errors["Email"] = mapOf("Error" to "Поле обязательно")
        }
        if (data.Position.isBlank()) {
            errors["Position"] = mapOf("Error" to "Поле обязательно")
        }
        if (data.Role !in listOf("User", "Admin", "Manager")) {
            errors["Role"] = mapOf("Error" to "Invalid role")
        }

        if (errors.isNotEmpty()) {
            return@restful ok(errors)
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
