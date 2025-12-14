package ru.yarsu

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

object CsvReader {
    fun readEquipment(filePath: String): List<Equipment> {
        val file = File(filePath)
        if (!file.exists()) throw IllegalArgumentException("Файл не найден: $filePath")
        return csvReader().readAllWithHeader(file).map { row ->
            val id = row["Id"] ?: throw IllegalArgumentException("Missing field: Id")
            val equipment = row["Equipment"] ?: throw IllegalArgumentException("Missing field: Equipment")
            val category = row["Category"] ?: throw IllegalArgumentException("Missing field: Category")
            val guaranteeDate = row["GuaranteeDate"] ?: throw IllegalArgumentException("Missing field: GuaranteeDate")
            val isUsedStr = row["IsUsed"] ?: throw IllegalArgumentException("Missing field: IsUsed")
            val priceStr = row["Price"] ?: throw IllegalArgumentException("Missing field: Price")
            val location = row["Location"] ?: throw IllegalArgumentException("Missing field: Location")

            // Читаем как UUID
            val responsibleStr = row["ResponsiblePerson"] ?: throw IllegalArgumentException("Missing field: ResponsiblePerson")
            val userStr = row["User"]

            Equipment(
                Id = UUID.fromString(id),
                Equipment = equipment,
                Category = category,
                GuaranteeDate = guaranteeDate,
                IsUsed = isUsedStr.toBoolean(),
                Price = BigDecimal(priceStr),
                Location = location,
                // Преобразуем String -> UUID
                ResponsiblePerson = UUID.fromString(responsibleStr),
                User = if (userStr.isNullOrBlank()) null else UUID.fromString(userStr),
            )
        }
    }

    // readLog можно оставить как есть, или тоже поменять ResponsiblePerson на UUID, если в логах хранятся ID
    fun readLog(filePath: String): List<Log> {
        val file = File(filePath)
        if (!file.exists()) throw IllegalArgumentException("Файл не найден: $filePath")
        return csvReader().readAllWithHeader(file).map { row ->
            val id = row["Id"] ?: throw IllegalArgumentException("Missing field: Id")
            val equipmentId = row["Equipment"] ?: throw IllegalArgumentException("Missing field: Equipment")
            val responsible = row["ResponsiblePerson"] ?: throw IllegalArgumentException("Missing field: ResponsiblePerson")
            val operation = row["Operation"] ?: throw IllegalArgumentException("Missing field: Operation")
            val text = row["Text"] ?: throw IllegalArgumentException("Missing field: Text")
            val dt = row["LogDateTime"] ?: throw IllegalArgumentException("Missing field: LogDateTime")

            Log(
                Id = UUID.fromString(id),
                Equipment = UUID.fromString(equipmentId),
                ResponsiblePerson = responsible, // Здесь можно оставить String (имя), так как это история
                Operation = operation,
                Text = text,
                LogDateTime = LocalDateTime.parse(dt),
            )
        }
    }

    // readUsers мы уже обновляли (добавили Role)
    fun readUsers(filePath: String): List<User> {
        val file = File(filePath)
        if (!file.exists()) throw IllegalArgumentException("Файл не найден: $filePath")
        return csvReader().readAllWithHeader(file).map { row ->
            val id = row["Id"] ?: throw IllegalArgumentException("Missing field: Id")
            val name = row["Name"] ?: throw IllegalArgumentException("Missing field: Name")
            val regDateTime = row["RegistrationDateTime"] ?: throw IllegalArgumentException("Missing field: RegistrationDateTime")
            val email = row["Email"] ?: throw IllegalArgumentException("Missing field: Email")
            val position = row["Position"] ?: throw IllegalArgumentException("Missing field: Position")
            val role = row["Role"] ?: "User"

            User(
                Id = UUID.fromString(id),
                Name = name,
                RegistrationDateTime = LocalDateTime.parse(regDateTime),
                Email = email,
                Position = position,
                Role = role,
            )
        }
    }
}
