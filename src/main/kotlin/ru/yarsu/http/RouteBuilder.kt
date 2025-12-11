package ru.yarsu.http

import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import ru.yarsu.EquipmentStorage
import kotlin.reflect.KFunction

fun buildRoutesFrom(
    storage: EquipmentStorage,
    vararg handlers: KFunction<*>,
): RoutingHttpHandler {
    val defs = handlers.map { routeFrom(it) }
    return routes(
        defs.map { def -> def.path bind def.method to def.factory(storage) },
    )
}
