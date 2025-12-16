package ru.yarsu

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import java.math.BigDecimal

object CsvWriter {
    fun writeEquipment(
        filePath: String,
        equipment: List<Equipment>,
    ) {
        val file = File(filePath)
        csvWriter().open(file) {
            writeRow("Id", "Equipment", "Category", "GuaranteeDate", "IsUsed", "Price", "Location", "ResponsiblePerson", "User")
            equipment.forEach { eq ->
                writeRow(
                    eq.Id.toString(),
                    eq.Equipment,
                    eq.Category,
                    eq.GuaranteeDate,
                    eq.IsUsed.toString(),
                    eq.Price.toString(),
                    eq.Location,
                    eq.ResponsiblePerson,
                    eq.User ?: "",
                )
            }
        }
    }

    fun writeLog(
        filePath: String,
        logs: List<Log>,
    ) {
        val file = File(filePath)
        csvWriter().open(file) {
            writeRow("Id", "Equipment", "ResponsiblePerson", "Operation", "Text", "LogDateTime")
            logs.forEach { log ->
                writeRow(
                    log.Id.toString(),
                    log.Equipment.toString(),
                    log.ResponsiblePerson,
                    log.Operation,
                    log.Text,
                    log.LogDateTime.toString(),
                )
            }
        }
    }

    fun writeUsers(
        filePath: String,
        users: List<User>,
    ) {
        val file = File(filePath)
        csvWriter().open(file) {
            writeRow("Id", "Name", "RegistrationDateTime", "Email", "Position", "Role")
            users.forEach { user ->
                writeRow(
                    user.Id.toString(),
                    user.Name,
                    user.RegistrationDateTime.toString(),
                    user.Email,
                    user.Position,
                    user.Role.name,
                )
            }
        }
    }
}
