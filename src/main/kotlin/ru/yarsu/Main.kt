package ru.yarsu

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.io.File
import kotlin.system.exitProcess

class ServerArgs {
    @Parameter(names = ["--equipment-file"], required = true)
    var equipmentFile: String? = null

    @Parameter(names = ["--log-file"], required = true)
    var logFile: String? = null

    @Parameter(names = ["--users-file"], required = true)
    var usersFile: String? = null

    // Файл с задачами для совместимости с тестирующей системой.
    // В текущей реализации не используется, но наличие параметра
    // позволяет JCommander корректно разобрать аргументы и запустить сервер.
    @Parameter(names = ["--tasks-file"])
    var tasksFile: String? = null

    @Parameter(names = ["--port"], required = true)
    var port: Int? = null
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("Error: no arguments provided")
        exitProcess(1)
    }

    if (args[0].startsWith("--")) {
        val serverArgs = ServerArgs()
        val jc = JCommander.newBuilder().addObject(serverArgs).build()

        try {
            jc.parse(*args)
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
            exitProcess(1)
        }

        val equipmentFile =
            serverArgs.equipmentFile ?: run {
                System.err.println("Error: missing --equipment-file")
                exitProcess(1)
            }
        val logFile =
            serverArgs.logFile ?: run {
                System.err.println("Error: missing --log-file")
                exitProcess(1)
            }
        val usersFile =
            serverArgs.usersFile ?: run {
                System.err.println("Error: missing --users-file")
                exitProcess(1)
            }
        val port =
            serverArgs.port ?: run {
                System.err.println("Error: missing --port")
                exitProcess(1)
            }

        if (!File(equipmentFile).exists()) {
            System.err.println("Error: file not found «$equipmentFile»")
            exitProcess(1)
        }
        if (!File(logFile).exists()) {
            System.err.println("Error: file not found «$logFile»")
            exitProcess(1)
        }
        if (!File(usersFile).exists()) {
            System.err.println("Error: file not found «$usersFile»")
            exitProcess(1)
        }
        if (port !in 1024..65535) {
            System.err.println("Error: invalid port")
            exitProcess(1)
        }

        val storage = EquipmentStorage()
        CsvReader.readEquipment(equipmentFile).forEach { storage.addEquipment(it) }
        CsvReader.readLog(logFile).forEach { storage.addLog(it) }
        CsvReader.readUsers(usersFile).forEach { storage.addUser(it) }

        println(
            "Loaded ${storage.getAllEquipment().size} equipment, ${storage.getAllLogs().size} logs, and ${storage.getAllUsers().size} users",
        )

        val app = createApiRoutes(storage)
        val server = app.asServer(Netty(port))

        // Добавляем обработчик завершения для сохранения данных
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("Saving data to CSV files...")
                CsvWriter.writeEquipment(equipmentFile, storage.getAllEquipment())
                CsvWriter.writeLog(logFile, storage.getAllLogs())
                CsvWriter.writeUsers(usersFile, storage.getAllUsers())
                println("Data saved successfully")
            },
        )

        server.start()
        println("Server started on http://localhost:$port")
    } else {
        System.err.println("CLI commands not implemented in this version")
        exitProcess(1)
    }
}
