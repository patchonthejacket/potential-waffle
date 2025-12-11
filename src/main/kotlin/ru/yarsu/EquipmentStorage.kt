package ru.yarsu

import java.util.UUID

class EquipmentStorage {
    private val equipment = mutableMapOf<UUID, Equipment>()
    private val logs = mutableMapOf<UUID, Log>()
    private val users = mutableMapOf<UUID, User>()

    fun addEquipment(eq: Equipment) {
        equipment[eq.Id] = eq
    }

    fun addLog(log: Log) {
        logs[log.Id] = log
    }

    fun addUser(user: User) {
        users[user.Id] = user
    }

    fun getAllEquipment(): List<Equipment> = equipment.values.toList()

    fun getAllLogs(): List<Log> = logs.values.toList()

    fun getAllUsers(): List<User> = users.values.toList()

    fun getEquipment(id: UUID): Equipment? = equipment[id]

    fun getLog(id: UUID): Log? = logs[id]

    fun getUser(id: UUID): User? = users[id]

    fun getLogsByEquipment(equipmentId: UUID): List<Log> = logs.values.filter { it.Equipment == equipmentId }.toList()

    fun updateEquipment(
        id: UUID,
        updateFn: (Equipment) -> Equipment,
    ) {
        val existing = equipment[id] ?: return
        equipment[id] = updateFn(existing)
    }

    fun removeEquipment(id: UUID) {
        equipment.remove(id)
    }

    fun updateLog(
        id: UUID,
        updateFn: (Log) -> Log,
    ) {
        val existing = logs[id] ?: return
        logs[id] = updateFn(existing)
    }

    fun removeLog(id: UUID) {
        logs.remove(id)
    }

    fun updateUser(
        id: UUID,
        updateFn: (User) -> User,
    ) {
        val existing = users[id] ?: return
        users[id] = updateFn(existing)
    }

    fun removeUser(id: UUID) {
        users.remove(id)
    }
}
