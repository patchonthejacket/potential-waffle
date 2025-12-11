package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/ping")
fun pingHandler(
    @Suppress("UNUSED_PARAMETER") storage: EquipmentStorage,
): HttpHandler =
    restful(storage) {
        ok(mapOf("ping" to "pong"))
    }
