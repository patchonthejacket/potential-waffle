package ru.yarsu.http.handlers.delete

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.restful
import ru.yarsu.http.handlers.userIdPathLens

@Route(method = Method.DELETE, path = "/v2/users/{user-id}")
fun deleteUserHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val id = userIdPathLens(req)

        val existing = storage.getUser(id)
        if (existing == null) {
            notFound(mapOf("Error" to "User not found", "UserId" to id.toString()))
        } else {
            storage.removeUser(id)
            ok(mapOf("UserId" to id.toString(), "Message" to "User deleted successfully"))
        }
    }
