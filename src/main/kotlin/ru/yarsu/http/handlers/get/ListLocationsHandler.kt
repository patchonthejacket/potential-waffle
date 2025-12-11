package ru.yarsu.http.handlers.get

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.EquipmentListItem
import ru.yarsu.http.handlers.LocationWithEquipment
import ru.yarsu.http.handlers.restful

@Route(method = Method.GET, path = "/v1/locations")
fun listLocationsHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        val all = storage.getAllEquipment()
        val grouped =
            all
                .groupBy { it.Location }
                .toSortedMap(compareBy { it })
        val result =
            grouped.map { (location, items) ->
                val equipmentItems =
                    items
                        .sortedWith(compareBy({ it.Equipment }, { it.Id }))
                        .map { EquipmentListItem(it.Id.toString(), it.Equipment, it.IsUsed) }
                LocationWithEquipment(location, equipmentItems)
            }
        ok(result)
    }
