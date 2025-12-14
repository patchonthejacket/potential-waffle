package ru.yarsu

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import org.http4k.core.then
import org.http4k.server.Netty
import org.http4k.server.asServer
import ru.yarsu.web.authFilter
import java.io.File
import kotlin.system.exitProcess

class ServerArgs {
    @Parameter(names = ["--equipment-file"], description = "Path to equipment CSV file", required = true)
    var equipmentFile: String? = null

    @Parameter(names = ["--log-file"], description = "Path to log CSV file", required = true)
    var logFile: String? = null

    @Parameter(names = ["--users-file"], description = "Path to users CSV file", required = true)
    var usersFile: String? = null

    @Parameter(names = ["--secret"], description = "Secret key for HMAC512 signature", required = true)
    var secret: String? = null

    @Parameter(names = ["--tasks-file"], description = "Path to tasks file (optional)")
    var tasksFile: String? = null

    @Parameter(names = ["--port"], description = "Server port", required = true)
    var port: Int? = null

    @Parameter(names = ["--help", "-h"], description = "Show help", help = true)
    var help: Boolean = false
}

fun main(args: Array<String>) {
    val serverArgs = ServerArgs()
    val jc = JCommander.newBuilder().addObject(serverArgs).build()

    try {
        if (args.isEmpty()) {
            jc.usage()
            exitProcess(1)
        }
        jc.parse(*args)

        if (serverArgs.help) {
            jc.usage()
            exitProcess(0)
        }
    } catch (e: Exception) {
        System.err.println("Error parsing arguments: ${e.message}")
        jc.usage()
        exitProcess(1)
    }

    // Безопасное извлечение аргументов (JCommander уже проверил required=true, но для компилятора Kotlin делаем smart cast или elvis)
    val equipmentFilePath =
        serverArgs.equipmentFile ?: run {
            System.err.println("Error: --equipment-file is required")
            exitProcess(1)
        }
    val logFilePath =
        serverArgs.logFile ?: run {
            System.err.println("Error: --log-file is required")
            exitProcess(1)
        }
    val usersFilePath =
        serverArgs.usersFile ?: run {
            System.err.println("Error: --users-file is required")
            exitProcess(1)
        }
    val secretKey =
        serverArgs.secret ?: run {
            System.err.println("Error: --secret is required")
            exitProcess(1)
        }
    val serverPort =
        serverArgs.port ?: run {
            System.err.println("Error: --port is required")
            exitProcess(1)
        }

    // Валидация файлов и порта
    if (!File(equipmentFilePath).exists()) {
        System.err.println("Error: Equipment file not found at «$equipmentFilePath»")
        exitProcess(1)
    }
    if (!File(logFilePath).exists()) {
        System.err.println("Error: Log file not found at «$logFilePath»")
        exitProcess(1)
    }
    if (!File(usersFilePath).exists()) {
        System.err.println("Error: Users file not found at «$usersFilePath»")
        exitProcess(1)
    }
    if (serverPort !in 1024..65535) {
        System.err.println("Error: Invalid port $serverPort. Must be between 1024 and 65535.")
        exitProcess(1)
    }

    // Загрузка данных
    val storage = EquipmentStorage()
    try {
        println("Loading data...")
        CsvReader.readEquipment(equipmentFilePath).forEach { storage.addEquipment(it) }
        CsvReader.readLog(logFilePath).forEach { storage.addLog(it) }
        CsvReader.readUsers(usersFilePath).forEach { storage.addUser(it) }
        println(
            "Loaded ${storage.getAllEquipment().size} equipment items, ${storage.getAllLogs().size} logs, and ${storage.getAllUsers().size} users.",
        )
    } catch (e: Exception) {
        System.err.println("Error reading CSV files: ${e.message}")
        exitProcess(1)
    }

    // Настройка приложения
    // 1. Создаем роуты
    val rawApp = createApiRoutes(storage)

    // 2. Подключаем фильтр аутентификации (передаем секрет для валидации токенов)
    val app = authFilter(storage, secretKey).then(rawApp)

    // Запуск сервера
    val server = app.asServer(Netty(serverPort))

    // Обработчик завершения (Graceful shutdown)
    Runtime.getRuntime().addShutdownHook(
        Thread {
            println("\nStopping server...")
            server.stop()
            println("Saving data to CSV files...")
            try {
                CsvWriter.writeEquipment(equipmentFilePath, storage.getAllEquipment())
                CsvWriter.writeLog(logFilePath, storage.getAllLogs())
                CsvWriter.writeUsers(usersFilePath, storage.getAllUsers())
                println("Data saved successfully.")
            } catch (e: Exception) {
                System.err.println("Error saving data: ${e.message}")
            }
        },
    )

    try {
        server.start()
        println("Server started on http://localhost:$serverPort")
        // Блокируем главный поток, чтобы программа не завершилась (Netty обычно сам держит, но это хорошая практика)
        Thread.currentThread().join()
    } catch (e: Exception) {
        System.err.println("Error starting server: ${e.message}")
        exitProcess(1)
    }
}
