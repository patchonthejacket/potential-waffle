package ru.yarsu.http.handlers.delete

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.ValidationException
import ru.yarsu.http.handlers.restful
import ru.yarsu.http.handlers.userIdPathLens

@Route(method = Method.DELETE, path = "/v2/users/{user-id}")
fun deleteUserHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        if (user == null || user?.Role != ru.yarsu.UserRole.Manager) {
            throw ValidationException(org.http4k.core.Status.UNAUTHORIZED, mapOf("Error" to "Отказано в авторизации"))
        }

        val id = userIdPathLens(req)

        val existing = storage.getUser(id)
        if (existing == null) {
            notFound(mapOf("Error" to "User not found", "UserId" to id.toString()))
        } else {
            storage.removeUser(id)
            ok(mapOf("UserId" to id.toString(), "Message" to "User deleted successfully"))
        }
    }
