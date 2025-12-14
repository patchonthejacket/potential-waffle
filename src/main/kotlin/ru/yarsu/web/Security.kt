package ru.yarsu.web

import org.http4k.lens.RequestKey
import ru.yarsu.User

// 1. Класс прав доступа (вернули сюда)
data class Permissions(
    val manageAllEquipment: Boolean = false, // Только Admin
    val manageOwnEquipment: Boolean = false, // User, Admin, Manager
    val manageUsers: Boolean = false, // Admin, Manager
) {
    companion object {
        val Guest = Permissions()

        val User = Permissions(manageOwnEquipment = true)

        val Admin = Permissions(manageAllEquipment = true, manageOwnEquipment = true, manageUsers = true)

        val Manager = Permissions(manageUsers = true, manageOwnEquipment = true)
    }
}

val currentUserLens = RequestKey.optional<User>("user")
val permissionsLens = RequestKey.required<Permissions>("permissions")
