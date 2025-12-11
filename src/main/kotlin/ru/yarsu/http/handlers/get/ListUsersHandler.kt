package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.restful

data class UserListItem(
    val Id: String,
    val Name: String,
    val RegistrationDateTime: String,
    val Email: String,
    val Position: String,
)

@Route(method = Method.GET, path = "/v2/users")
fun listUsersHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val params = pageParams()
        val sorted = storage.getAllUsers().sortedWith(compareBy({ it.Name }, { it.Id }))
        val paginated = paginate(sorted, params)
        val items =
            paginated.map {
                UserListItem(
                    it.Id.toString(),
                    it.Name,
                    it.RegistrationDateTime.toString(),
                    it.Email,
                    it.Position,
                )
            }
        ok(items)
    }
