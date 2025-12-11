# Шпаргалка по типам ручек и что в них делать


## 1) Списки с фильтрами и пагинацией
Признаки:
- GET
- Путь без идентификатора (`/v1/equipment`, `/v1/equipment/search`)
- Есть query-параметры: `page`, `records-per-page`, фильтры
- Возвращаю массив элементов

Что делаю:
1. Беру исходную коллекцию из `storage`.
2. Применяю фильтры из `req.query(...)`.
3. Стабильно сортирую.
4. Делаю постраничную нарезку: `val p = pageParams()` → `paginate(list, p)`.
5. Возвращаю `ok(...)`.

Скелет:
```kotlin
@Route(method = Method.GET, path = "/v1/items")
fun listItems(storage: EquipmentStorage): HttpHandler = restful(storage) {
    val params = pageParams()
    val all = storage.getAllX()
    val filtered = all
        .filter { /* фильтры */ }
        .sortedBy { /* ключ */ }
    ok(paginate(filtered, params).map { /* проекция полей */ })
}
```


- Обязательные запросы — валидирую и при отсутствии отдаю 400 через `json(Status.BAD_REQUEST, ...)`.
- Порядок: фильтр → сортировка → пагинация.
- Пустой результат — это 200 и пустой список.

## 2) Один объект по id
Признаки:
- Путь с идентификатором: `/v1/items/{item-id}`.
- Возвращаю один объект или 404.

Что делаю:
1. Достаю id: `val id = pathUuid("item-id")`.
2. Ищу в `storage`.
3. Если не найден — `notFound(...)` (формат 404 — по требованиям).
4. Если найден — собираю detail и `ok(detail)`.

Скелет:
```kotlin
@Route(method = Method.GET, path = "/v1/items/{item-id}")
fun getItem(storage: EquipmentStorage): HttpHandler = restful(storage) {
    val id = pathUuid("item-id", "Invalid ID")
    val item = storage.getItem(id)
    if (item == null) notFound(mapOf("ItemId" to id.toString()))
    else ok(...)
}
```

Замечания:
- Невалидный id → 400 (автоматом через `pathUuid`).

## 3) Агрегация/статистика
Признаки:
- Путь вида `/v1/.../statistics` или `/v1/.../aggregate`.
- Есть переключатель режима в запросы (например, `by=category|person`).
- Возвращаю не «сырые» элементы, а сводки: count/sum/avg и т.п.

Что делаю:
1. Читаю и валидирую режим (`by`), иначе 400.
2. Беру базовый список.
3. `groupBy { ключ }`.
4. По каждой группе считаю метрики (`count`, `sum`, `avg` …).
5. Стабильно сортирую.
6. `ok(mapOf("SomeKey" to stats))`.

Скелет:
```kotlin
@Route(method = Method.GET, path = "/v1/items/statistics")
fun stats(storage: EquipmentStorage): HttpHandler = restful(storage) {
    val by = req.query("by") ?: json(Status.BAD_REQUEST, mapOf("error" to "by is required"))
    when (by) {
        "category" -> {
            val grouped = storage.getAllX().groupBy { it.Category }
            val stats = grouped.entries.map {
                val total = it.value.fold(0.0) { acc, e -> acc + e.Price.toDouble() }
                CategoryStat(it.key, it.value.size, total)
            }.sortedBy { it.Category }
            ok(mapOf("StatisticsByCategory" to stats))
        }
        else -> json(Status.BAD_REQUEST, mapOf("error" to "Invalid by"))
    }
}
```

Замечания:
- Всегда явно валидирую обязательные query.
- Для денег/Double аккуратно с типами/округлением.

## 4) Поиск/срез (простая фильтрация + limit)
Признаки:
- Путь `/search`, `/filter`, `/by-...`.
- Обязательный параметр поиска (`term`, `name`, `category`).
- Часто достаточно `limit`, можно без пагинации.

Что делаю:
1. Валидирую обязательный параметр.
2. Фильтрую и сортирую.
3. Применяю `limit` (или пагинацию по необходимости).
4. `ok(list)`.

Скелет:
```kotlin
@Route(method = Method.GET, path = "/v1/items/search")
fun search(storage: EquipmentStorage): HttpHandler = restful(storage) {
    val term = req.query("term") ?: json(Status.BAD_REQUEST, mapOf("error" to "term is required"))
    val all = storage.getAllX()
    val filtered = all.filter { it.Name.contains(term, ignoreCase = true) }.sortedBy { it.Name }
    val limit = req.query("limit")?.toIntOrNull() ?: 10
    ok(filtered.take(limit).map { /* DTO */ })
}
```

## Быстрая эвристика выбора шаблона
- В пути есть `{id}` → «один объект».
- Много query и нужен массив → «список с фильтрами/пагинацией».
- Нужны сводные цифры по группам → «агрегация/статистика».
- «Найти по …», «последние N …» → «поиск/срез».

## Общие правила
- Обязательные query валидирую → при отсутствии 400 через `json(...)`.
- В списках: фильтр → сортировка → пагинация/limit.
- Всегда делаю детерминированную сортировку.
- Для «одного объекта»: невалидный id → 400; не найдено → 404; найден → 200.

Этого хватает, чтобы за пару минут сообразить, какой каркас брать, и быстро накидать ручку на текущем фреймворке.
