# Руководство по тестированию API

Этот документ содержит команды для тестирования всех эндпоинтов API инвентаризационного списка техники.

## Предварительные требования

1. Запустите сервер:
```bash
./gradlew run --args="--equipment-file src/main/resources/equipment.csv --log-file src/main/resources/log.csv --users-file src/main/resources/users.csv --port 9000"
```

2. Убедитесь, что сервер запущен на порту 9000 (или измените порт в командах ниже).

## Базовые маршруты

### 1. Проверка работоспособности сервера

```bash
curl -X GET http://localhost:9000/ping
```

**Ожидаемый результат:** `{"ping":"pong"}`

## Инвентаризация (Equipment)

### 2. Получение списка техники

```bash
curl -X GET "http://localhost:9000/v2/equipment?page=1&records-per-page=10"
```

**С параметрами пагинации:**
```bash
curl -X GET "http://localhost:9000/v2/equipment?page=2&records-per-page=5"
```

**Ожидаемый результат:** Массив объектов с полями `Id`, `Equipment`, `IsUsed`

### 3. Добавление нового оборудования

```bash
curl -X POST http://localhost:9000/v2/equipment \
  -H "Content-Type: application/json" \
  -d '{
    "Equipment": "Ноутбук Dell Latitude 5520",
    "Category": "ПК",
    "GuaranteeDate": "2025-12-31",
    "IsUsed": false,
    "Price": 45000,
    "Location": "склад",
    "ResponsiblePerson": "de42a00f-7f43-4d10-808d-bee47fdeef49",
    "User": null
  }'
```

**Ожидаемый результат:** `{"Id":"<UUID>"}` со статусом 201

### 4. Получение информации о конкретном элементе техники

```bash
curl -X GET http://localhost:9000/v2/equipment/1d0f861e-9d0c-4340-acad-69ade7b83085
```

**Ожидаемый результат:** Объект с полной информацией об оборудовании и списком связанных логов

### 5. Редактирование оборудования (PATCH)

```bash
curl -X PATCH http://localhost:9000/v2/equipment/1d0f861e-9d0c-4340-acad-69ade7b83085 \
  -H "Content-Type: application/json" \
  -d '{
    "IsUsed": true,
    "Location": "офис, кабинет № 10",
    "User": "de42a00f-7f43-4d10-808d-bee47fdeef49"
  }'
```

**Ожидаемый результат:** `{"Id":"<UUID>"}` со статусом 200

**Пример частичного обновления:**
```bash
curl -X PATCH http://localhost:9000/v2/equipment/1d0f861e-9d0c-4340-acad-69ade7b83085 \  
  -H "Content-Type: application/json" \
  -d '{
    "Price": 3000
  }'
```

### 6. Списание оборудования (DELETE)

```bash
curl -X DELETE http://localhost:9000/v2/equipment/1d0f861e-9d0c-4340-acad-69ade7b83085
```

**Ожидаемый результат:** `{"Id":"<UUID>"}` со статусом 200

### 7. Получение списка свободной техники

```bash
curl -X GET "http://localhost:9000/v2/equipment/unused?page=1&records-per-page=10"
```

**С фильтром по категории:**
```bash
curl -X GET "http://localhost:9000/v2/equipment/unused?category=Монитор&page=1&records-per-page=10"
```

**С несколькими категориями:**
```bash
curl -X GET "http://localhost:9000/v2/equipment/unused?category=ПК,Монитор&page=1&records-per-page=10"
```

**Ожидаемый результат:** Массив объектов с полями `Id`, `Equipment`

### 8. Получение списка техники на замену (по гарантии)

```bash
curl -X GET "http://localhost:9000/v2/equipment/by-time?time=2024-12-31T00:00:00&page=1&records-per-page=10"
```

**Ожидаемый результат:** Массив объектов с полями `Id`, `Equipment`, `GuaranteeDate`

### 9. Получение статистики

**По категориям:**
```bash
curl -X GET "http://localhost:9000/v2/equipment/statistics?by-type=category"
```

**Ожидаемый результат:** 
```json
{
  "StatisticsByCategory": [
    {
      "Category": "ПК",
      "Count": 5,
      "Price": 75000.0
    }
  ]
}
```

**По ответственным лицам:**
```bash
curl -X GET "http://localhost:9000/v2/equipment/statistics?by-type=person"
```

**Ожидаемый результат:**
```json
{
  "StatisticsByPerson": [
    {
      "ResponsiblePerson": "de42a00f-7f43-4d10-808d-bee47fdeef49",
      "Count": 10,
      "Price": 150000.0
    }
  ]
}
```

## Журнал (Log)

### 10. Получение информации о записи в журнале

```bash
curl -X GET http://localhost:9000/v2/log/29478648-3524-4d60-9176-22aad1c3bb4a
```

**Ожидаемый результат:** Объект с полной информацией о записи в журнале

### 11. Редактирование записи в журнале (PUT)

```bash
curl -X PUT http://localhost:9000/v2/log/29478648-3524-4d60-9176-22aad1c3bb4a \
  -H "Content-Type: application/json" \
  -d '{
    "Equipment": "1d0f861e-9d0c-4340-acad-69ade7b83085",
    "ResponsiblePerson": "de42a00f-7f43-4d10-808d-bee47fdeef49",
    "Operation": "Ремонт",
    "Text": "Выполнен ремонт монитора",
    "LogDateTime": "2024-06-15T14:30:00"
  }'
```

**Ожидаемый результат:** `{"Id":"<UUID>"}` со статусом 200

## Работники (Users)

### 12. Получение списка работников

```bash
curl -X GET "http://localhost:9000/v2/users?page=1&records-per-page=10"
```

**Ожидаемый результат:** Массив объектов с полями `Id`, `Name`, `RegistrationDateTime`, `Email`, `Position`

## Тестирование ошибок и валидации

### Тест неверного UUID в пути

```bash
curl -X GET http://localhost:9000/v2/equipment/invalid-uuid
```

**Ожидаемый результат:** Статус 400 с описанием ошибки

### Тест несуществующего оборудования

```bash
curl -X GET http://localhost:9000/v2/equipment/00000000-0000-0000-0000-000000000000
```

**Ожидаемый результат:** Статус 404 с сообщением об ошибке

### Тест неверной категории при создании

```bash
curl -X POST http://localhost:9000/v2/equipment \
  -H "Content-Type: application/json" \
  -d '{
    "Equipment": "Тест",
    "Category": "НевернаяКатегория",
    "GuaranteeDate": "2025-12-31",
    "IsUsed": false,
    "Price": 1000,
    "Location": "склад",
    "ResponsiblePerson": "de42a00f-7f43-4d10-808d-bee47fdeef49"
  }'
```

**Ожидаемый результат:** Статус 400 с сообщением "Invalid category"

### Тест неверного records-per-page

```bash
curl -X GET "http://localhost:9000/v2/equipment?records-per-page=99"
```

**Ожидаемый результат:** Статус 400 с сообщением об ошибке (разрешены только 5, 10, 20, 50)

### Тест отсутствующего обязательного параметра

```bash
curl -X GET "http://localhost:9000/v2/equipment/by-time"
```

**Ожидаемый результат:** Статус 400 с сообщением об отсутствии параметра `time`

### Тест неверного by-type в статистике

```bash
curl -X GET "http://localhost:9000/v2/equipment/statistics?by-type=invalid"
```

**Ожидаемый результат:** Статус 400 с сообщением об ошибке

## Примеры комплексного тестирования

### Сценарий 1: Создание, обновление и удаление оборудования

```bash
# 1. Создание нового оборудования
RESPONSE=$(curl -s -X POST http://localhost:9000/v2/equipment \
  -H "Content-Type: application/json" \
  -d '{
    "Equipment": "Тестовое оборудование",
    "Category": "Другое",
    "GuaranteeDate": "2026-12-31",
    "IsUsed": false,
    "Price": 5000,
    "Location": "склад",
    "ResponsiblePerson": "de42a00f-7f43-4d10-808d-bee47fdeef49"
  }')

# Извлечение ID из ответа (требует jq или аналогичного инструмента)
EQUIPMENT_ID=$(echo $RESPONSE | jq -r '.Id')

# 2. Получение созданного оборудования
curl -X GET http://localhost:9000/v2/equipment/$EQUIPMENT_ID

# 3. Обновление оборудования
curl -X PATCH http://localhost:9000/v2/equipment/$EQUIPMENT_ID \
  -H "Content-Type: application/json" \
  -d '{
    "IsUsed": true,
    "Location": "офис, кабинет № 1"
  }'

# 4. Удаление оборудования
curl -X DELETE http://localhost:9000/v2/equipment/$EQUIPMENT_ID
```

### Сценарий 2: Тестирование пагинации

```bash
# Первая страница
curl -X GET "http://localhost:9000/v2/equipment?page=1&records-per-page=5"

# Вторая страница
curl -X GET "http://localhost:9000/v2/equipment?page=2&records-per-page=5"

# Третья страница
curl -X GET "http://localhost:9000/v2/equipment?page=3&records-per-page=5"
```

## Проверка журналирования

После выполнения запросов проверьте логи в файле `logs/application.log`. Логи должны быть в формате JSON и содержать информацию о:
- Методе запроса (`http.method`)
- Пути запроса (`http.path`)
- Адресе клиента (`http.address`)
- Статусе ответа (`http.status`)
- Времени обработки (`http.time`)

Пример просмотра логов:
```bash
tail -f logs/application.log
```

## Примечания

1. Все UUID в примерах нужно заменить на реальные UUID из ваших CSV файлов
2. Для удобства работы с JSON ответами рекомендуется использовать `jq`:
   ```bash
   curl -X GET http://localhost:9000/v2/equipment | jq
   ```
3. При тестировании POST/PATCH/PUT запросов убедитесь, что передаете корректный JSON
4. Все даты должны быть в формате ISO 8601 (например, `2024-12-31T00:00:00`)
5. Категории должны быть из списка: "ПК", "Монитор", "Принтер", "Телефон", "Другое"
6. Параметр `records-per-page` может быть только: 5, 10, 20, 50

