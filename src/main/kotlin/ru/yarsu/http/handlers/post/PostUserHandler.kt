package ru.yarsu.http.handlers.post

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.User
import ru.yarsu.UserRole
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.FieldError
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
                    val Name = requireTextAllowEmpty("Name")
                    val Email = requireTextAllowEmpty("Email")
                    val Position = requireTextAllowEmpty("Position")
                    val roleRaw = requireText("Role")

                    init {
                        if (roleRaw != null && roleRaw !in listOf("User", "Admin", "Manager")) {
                            // Добавляем бизнес-ошибку только если тип корректный, но значение вне допустимого набора
                            validateBusiness(
                                Result.failure<String>(
                                    FieldError(
                                        "Role",
                                        root.get("Role"),
                                        "Ожидается одно из значений: User, Admin, Manager",
                                    ),
                                ),
                            )
                        }
                    }

                    val Role = roleRaw
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
