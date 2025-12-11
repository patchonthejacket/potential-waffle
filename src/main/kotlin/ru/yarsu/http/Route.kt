package ru.yarsu.http

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import kotlin.reflect.KFunction

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Route(
    val method: Method,
    val path: String,
)

data class RouteDefinition(
    val method: Method,
    val path: String,
    val factory: (ru.yarsu.EquipmentStorage) -> HttpHandler,
)

fun routeFrom(function: KFunction<*>): RouteDefinition {
    val ann =
        function.annotations.filterIsInstance<Route>().firstOrNull()
            ?: throw IllegalArgumentException("Function ${function.name} must be annotated with @Route")

    @Suppress("UNCHECKED_CAST")
    val factory = function as (ru.yarsu.EquipmentStorage) -> HttpHandler
    return RouteDefinition(ann.method, ann.path, factory)
}
