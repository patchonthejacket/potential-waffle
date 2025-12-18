package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.User
import ru.yarsu.UserRole
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v3/users")
fun listUsersHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        if (user?.Role != UserRole.Manager) {
            throw ValidationException(Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }
        val sorted =
            storage
                .getAllUsers()
                .sortedWith(compareBy<User> { it.Name }.thenBy { it.Id })

        val result =
            sorted.map { u ->
                mapOf(
                    "Id" to u.Id.toString(),
                    "Name" to u.Name,
                    "RegistrationDateTime" to u.RegistrationDateTime.toString(),
                    "Email" to u.Email,
                    "Position" to u.Position,
                )
            }

        ok(result)
    }
