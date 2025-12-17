package ru.yarsu.http.handlers.post

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.User
import ru.yarsu.UserRole
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID

@Route(method = Method.POST, path = "/v3/users")
fun postUserHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        // Проверка авторизации ДО парсинга JSON
        if (user == null || user.Role != UserRole.Manager) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val data =
            validateJson {
                object {
                    val Name = requireText("Name")
                    val Email = requireText("Email")
                    val Position = requireText("Position")
                    val roleRaw = requireText("Role")
                    val Role =
                        if (roleRaw in listOf("User", "Admin", "Manager")) {
                            roleRaw
                        } else {
                            throw ValidationException(
                                Status.BAD_REQUEST,
                                mapOf("Role" to mapOf("Value" to roleRaw, "Error" to "Ожидается одно из значений: User, Admin, Manager")),
                            )
                        }
                }
            }

        val userId = UUID.randomUUID()
        val newUser =
            User(
                Id = userId,
                Name = data.Name!!,
                RegistrationDateTime = LocalDateTime.now(),
                Email = data.Email!!,
                Position = data.Position!!,
                Role = enumValueOf<UserRole>(data.Role!!),
            )

        storage.addUser(newUser)

        created(mapOf("Id" to userId.toString()))
    }
