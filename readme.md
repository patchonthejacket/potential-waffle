# Шаблон приложения

Данный шаблон предназначен для использования в рамках курса по разработке
веб-приложений. Он используется для выполнения автоматизированной проверки
результатов работы студентов.

### Использование шаблона во время проверки

Во время автоматизированной проверки из каталога с исходными кодами работы
студента берутся только:
* Каталог с исходными файлами, `src`.
* Файл с упрощёнными настройками сборки, `build.properties.json`.
  Остальные файлы берутся из данного каталога.

Для успешного прохождения проверки студентам рекомендуется изменять только
лишь указанные части проекта.

### Настройка стартового файла приложения

В файле `build.properties.json` нужно указать путь к стартовому файлу
приложения. Для этих целей необходимо корректно заполнить свойство
`mainClass`. Если оно заполнено корректно, то приложение должно запуститься
путём запуска задачи `run`:

## HTTP API Запуск

```bash
./gradlew run --args="--equipment-file src/main/resources/equipment.csv --log-file src/main/resources/log.csv --port 9000"
```

В шаблоне предоставляется стартовый файл `ru.yarsu.Main.kt`. В свойстве
`mainClass` он указывается как `ru.yarsu.MainKt`:

```json
{
  "mainClass": "ru.yarsu.MainKt"
}
```

После внесения изменений в файл `build.properties.json` необходимо
перезагрузить конфигурацию Gradle в среде разработки.

### Настройка списка зависимостей приложения

В файле `build.properties.json` можно указать список Maven-зависимостей
приложения. Они указываются в виде массива строк в поле `dependencies`.

Например, для добавления зависимости от библиотеки
[kotlin-csv](https://github.com/jsoizo/kotlin-csv) необходимо в список
добавить зависимость: `"com.jsoizo:kotlin-csv:1.10.0"`.
Следовательно, в файле `build.properties.json` будет указано:

```json
{
  "dependencies": [
    "com.jsoizo:kotlin-csv:1.10.0"
  ]
}
```

После внесения изменений в файл `build.properties.json` необходимо
перезагрузить конфигурацию Gradle в среде разработки.

## Шпаргалка

- `post/` - создание
- `put/` - полное обновление
- `patch/` - частичное обновление
- `delete/` - удаление

Каждый обработчик помечается `@Route` с методом и путём, потом регистрируется в `ApiRouting.kt`. Все используют `restful()`

---

### POST - создание

Создаем файл в `post/`, например `PostUserHandler.kt`
```kotlin
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import ru.yarsu.EquipmentStorage
import ru.yarsu.http.Route
import ru.yarsu.http.handlers.restful
import java.time.LocalDateTime
import java.util.UUID
```

Функция с `@Route`:
```kotlin
@Route(method = Method.POST, path = "/v2/users")
fun postUserHandler(storage: EquipmentStorage): HttpHandler =
    restful(storage) {
        try {
            validateJson {
                // валидация полей
            }
        } catch (e: ru.yarsu.http.handlers.ValidationException) {
            json(e.status, e.body)
        }
    }
```

Внутри `validateJson` валидируем поля:
- `requireText("FieldName")` - обязательное непустое
- `requireTextAllowEmpty("FieldName")` - обязательное, но может быть пустым
- `requireNumber("FieldName")` - число
- `requireDate("FieldName")` - дата
- `optionalText("FieldName")` - опциональное

Если нужна бизнес-валидация (например, email):
```kotlin
if (!email.contains("@")) {
    validateBusiness(Result.failure<String>(ru.yarsu.http.handlers.FieldError("Email", root.get("Email"))))
    if (hasErrors()) {
        return@restful json(Status.BAD_REQUEST, collectErrors())
    }
}
```

Проверяем ошибки и создаем ресурс:
```kotlin
if (hasErrors()) {
    return@restful json(Status.BAD_REQUEST, collectErrors())
}

val userId = UUID.randomUUID()
val newUser = ru.yarsu.User(
    Id = userId,
    Name = name,
    RegistrationDateTime = LocalDateTime.now(),
    Email = email,
    Position = position,
)
storage.addUser(newUser)
```

Если нужны косвенные изменения (например, при создании оборудования автоматически создаётся запись в журнале):
```kotlin
val logId = UUID.randomUUID()
storage.addLog(ru.yarsu.Log(...))
```

Возвращаем `201 Created`:
```kotlin
created(mapOf("UserId" to userId.toString()))
// или если несколько полей:
created(EquipmentResponse(EquipmentId = ..., LogId = ...))
```


---

### PUT - полное обновление

PUT заменяет весь ресурс, поэтому все поля обязательны. Сначала извлекаем ID из пути:
```kotlin
val id = userIdPathLens(req)
```

Если такой линзы нет, создай в `Lenses.kt`:
```kotlin
val userIdPathLens = Path.uuid().of("user-id")
```

Проверяем существование:
```kotlin
val existing = storage.getUser(id)
    ?: return@restful notFound(mapOf("Error" to "User not found", "UserId" to id.toString()))
```

Тело запроса обычно JSON (проще):
```kotlin
try {
    validateJson {
        val name = requireText("Name")
        val email = requireText("Email")
        // все поля обязательны
    }
} catch (e: ru.yarsu.http.handlers.ValidationException) {
    json(e.status, e.body)
}
```

Или form-urlencoded (как в `PutLogHandler.kt`). Для этого нужны линзы в `Lenses.kt`:
```kotlin
val operationFormField = FormField.nonEmptyString().required("Operation")
val textFormField = FormField.nonEmptyString().required("Text")
val logUpdateFormLens: BodyLens<WebForm> =
    Body.webForm(Validator.Strict, operationFormField, textFormField).toLens()
```

Валидируем все поля, обновляем и возвращаем `204 No Content`:
```kotlin
storage.updateUser(id) { user ->
    user.copy(
        Name = name,
        Email = email,
        Position = position,
        RegistrationDateTime = user.RegistrationDateTime, // сохраняем неизменяемые поля
    )
}
ApiResult.NoContent
```

Примеры: `PutUserHandler.kt` (JSON), `PutLogHandler.kt` (form-urlencoded)

---

### PATCH - частичное обновление

Поля опциональны, обновляются только те, что переданы. Извлекаем ID и проверяем существование (как в PUT).

Используем `validateJson` и опциональные методы:
- `optionalText("FieldName")` - опциональное текстовое
- `optionalNumber("FieldName")` - опциональное число
- `optionalDate("FieldName")` - опциональная дата
- `optionalCategory("FieldName")` - опциональная категория
- `validateUserField()` - для поля User (может быть null, отсутствовать или иметь значение)

Бизнес-валидация только для переданных полей:
```kotlin
category?.let { validateCategory(it) }
price?.let { validatePrice(it) }
val responsiblePersonUuid = validateResponsiblePersonUuid("ResponsiblePerson", responsiblePerson)
```

Обновляем только переданные поля:
```kotlin
var changed = false
storage.updateEquipment(id) { eq ->
    val updated = eq.copy(
        Equipment = equipment,
        Category = category ?: eq.Category,  // старое значение, если не передано
        Price = price ?: eq.Price,
    )
    if (updated != eq) {
        changed = true
    }
    updated
}
```

Если нужны косвенные изменения (например, запись в журнале при изменении):
```kotlin
if (changed) {
    val logId = UUID.randomUUID()
    storage.addLog(ru.yarsu.Log(...))
    created(mapOf("LogId" to logId.toString()))
} else {
    ApiResult.NoContent
}
```

Возвращаем `201 Created` (если были косвенные изменения) или `204 No Content`.

Примеры: `PatchEquipmentHandler.kt`, `PatchUserHandler.kt`, `PatchLogHandler.kt`

---

### DELETE - удаление

Извлекаем ID, проверяем существование:
```kotlin
val id = equipmentIdPathLens(req)
val existing = storage.getEquipment(id)
if (existing == null) {
    notFound(mapOf("Error" to "Equipment not found", "EquipmentId" to id.toString()))
} else {
    // удаление
}
```

Если нужны косвенные изменения (например, запись в журнале перед удалением):
```kotlin
val logId = UUID.randomUUID()
val log = ru.yarsu.Log(
    Id = logId,
    Equipment = id,
    ResponsiblePerson = existing.ResponsiblePerson,
    Operation = "Списание: ${existing.Equipment}",
    Text = "",
    LogDateTime = LocalDateTime.now(),
)
storage.addLog(log)
```

Удаляем и возвращаем `200 OK`:
```kotlin
storage.removeEquipment(id)
ok(mapOf("LogId" to logId.toString()))
```

Примеры: `DeleteEquipmentHandler.kt`, `DeleteUserHandler.kt`, `DeleteLogHandler.kt`

---


### Линзы для path параметров

Если в пути новый параметр (например, `{category-id}`), создай линзу в `Lenses.kt`:
```kotlin
val categoryIdPathLens = Path.uuid().of("category-id")
```

Потом используем: `val id = categoryIdPathLens(req)`

### Обработка ошибок

`restful()` автоматически ловит ошибки:
- `LensFailure` → 400 Bad Request
- `IllegalArgumentException` → 400 Bad Request
- остальные исключения → 400 Bad Request

Для валидации JSON лови `ValidationException`:
```kotlin
catch (e: ru.yarsu.http.handlers.ValidationException) {
    json(e.status, e.body)
}
```

### Методы валидации в JsonValidationContext

**Обязательные:**
- `requireText(field)` - непустое текстовое
- `requireTextAllowEmpty(field)` - текстовое (может быть пустым)
- `requireNumber(field)` - число
- `requireDate(field)` - дата

**Опциональные:**
- `optionalText(field)` - опциональное текстовое
- `optionalTextAllowEmpty(field)` - опциональное текстовое (может быть пустым)
- `optionalNumber(field)` - опциональное число
- `optionalDate(field)` - опциональная дата
- `optionalCategory(field)` - опциональная категория

**Бизнес-валидация:**
- `validateUserUuid(field, value)` - UUID пользователя + проверка существования
- `validateResponsiblePersonUuid(field, value)` - UUID ответственного лица
- `validateCategory(value)` - категория
- `validatePrice(value)` - цена (неотрицательная)
- `validateUserField()` - специальная обработка User (может быть null, отсутствовать или иметь значение)

**Проверка ошибок:**
- `hasErrors()` - есть ли ошибки
- `collectErrors()` - собрать все ошибки
