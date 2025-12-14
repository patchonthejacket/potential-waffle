package ru.yarsu

import org.http4k.routing.RoutingHttpHandler
import ru.yarsu.http.buildRoutesFrom
import ru.yarsu.http.handlers.delete.deleteEquipmentHandler
import ru.yarsu.http.handlers.delete.deleteLogHandler
import ru.yarsu.http.handlers.delete.deleteUserHandler
import ru.yarsu.http.handlers.get.byTimeHandler
import ru.yarsu.http.handlers.get.getEquipmentHandler
import ru.yarsu.http.handlers.get.getLogByIdHandler
import ru.yarsu.http.handlers.get.getLogHandler
import ru.yarsu.http.handlers.get.listEquipmentHandler
import ru.yarsu.http.handlers.get.listUsersHandler
import ru.yarsu.http.handlers.get.pingHandler
import ru.yarsu.http.handlers.get.statisticsHandler
import ru.yarsu.http.handlers.get.unusedEquipmentHandler
import ru.yarsu.http.handlers.patch.patchEquipmentHandler
import ru.yarsu.http.handlers.patch.patchLogHandler
import ru.yarsu.http.handlers.patch.patchUserHandler
import ru.yarsu.http.handlers.post.postEquipmentHandler
import ru.yarsu.http.handlers.post.postLogHandler
import ru.yarsu.http.handlers.post.postUserHandler
import ru.yarsu.http.handlers.put.putLogHandler
import ru.yarsu.http.handlers.put.putUserHandler

fun createApiRoutes(storage: EquipmentStorage): RoutingHttpHandler =
    buildRoutesFrom(
        storage,
        ::pingHandler,
        ::listEquipmentHandler,
        ::postEquipmentHandler,
        ::patchEquipmentHandler,
        ::deleteEquipmentHandler,
        ::unusedEquipmentHandler,
        ::byTimeHandler,
        ::statisticsHandler,
        ::getLogHandler,
        ::getLogByIdHandler,
        ::putLogHandler,
        ::patchLogHandler,
        ::postLogHandler,
        ::deleteLogHandler,
        ::listUsersHandler,
        ::postUserHandler,
        ::patchUserHandler,
        ::putUserHandler,
        ::deleteUserHandler,
        ::getEquipmentHandler,
    )
