# Bank Cards Management System

REST API система управления банковскими картами на Spring Boot.

## Технологии

- Java 17
- Spring Boot 3.2.2
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Liquibase
- Docker & Docker Compose
- Swagger/OpenAPI

## Функциональность

### Роли пользователей

**Администратор (ADMIN):**
- Создание, блокировка, активация и удаление карт
- Управление пользователями
- Просмотр всех карт в системе

**Пользователь (USER):**
- Просмотр своих карт (с поиском и пагинацией)
- Запрос на блокировку своей карты
- Переводы между своими картами
- Просмотр баланса

### Безопасность

- JWT аутентификация
- AES-256 шифрование номеров карт
- Маскирование номеров карт при отображении (`**** **** **** 1234`)
- Ролевой контроль доступа

## Быстрый старт

### Запуск с Docker Compose (рекомендуется)

```bash
# Клонируйте репозиторий
git clone <repository-url>
cd bankcards

# Запустите приложение
docker-compose up -d

# Проверьте логи
docker-compose logs -f app
```

Приложение будет доступно по адресу: http://localhost:8080

### Запуск локально

**Требования:**
- Java 17+
- Maven 3.8+
- PostgreSQL 15+

```bash
# Создайте базу данных
createdb bankdb

# Настройте переменные окружения (опционально)
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=bankdb
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=your-256-bit-secret-key
export ENCRYPTION_KEY=your-32-bytes-encryption-key

# Соберите и запустите приложение
mvn clean package
java -jar target/bankcards-1.0.0.jar
```

### Запуск тестов

```bash
mvn test
```

## API Документация

После запуска приложения документация доступна по адресам:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
- OpenAPI YAML: [docs/openapi.yaml](docs/openapi.yaml)

## Аутентификация

### Регистрация

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### Вход

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

Ответ содержит JWT токен:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "user@example.com",
  "role": "USER"
}
```

### Использование токена

Добавьте токен в заголовок `Authorization`:
```bash
curl -X GET http://localhost:8080/api/cards \
  -H "Authorization: Bearer <your-token>"
```

## API Endpoints

### Аутентификация (`/api/auth`)

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/register` | Регистрация пользователя |
| POST | `/login` | Вход и получение JWT токена |

### Карты (`/api/cards`)

| Метод | Путь | Роль | Описание |
|-------|------|------|----------|
| GET | `/` | USER | Список своих карт |
| GET | `/all` | ADMIN | Все карты системы |
| GET | `/{id}` | USER/ADMIN | Детали карты |
| POST | `/` | ADMIN | Создание карты |
| PUT | `/{id}/block` | ADMIN | Блокировка карты |
| PUT | `/{id}/activate` | ADMIN | Активация карты |
| DELETE | `/{id}` | ADMIN | Удаление карты |
| POST | `/{id}/request-block` | USER | Запрос блокировки |
| POST | `/transfer` | USER | Перевод между картами |
| GET | `/{id}/balance` | USER | Баланс карты |

### Пользователи (`/api/users`) - только ADMIN

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/` | Список пользователей |
| GET | `/{id}` | Детали пользователя |
| DELETE | `/{id}` | Удаление пользователя |

## Примеры запросов

### Создание карты (Admin)

```bash
curl -X POST http://localhost:8080/api/cards \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "ownerId": 1,
    "expiryDate": "2028-12-31",
    "initialBalance": 1000.00
  }'
```

### Перевод между картами

```bash
curl -X POST http://localhost:8080/api/cards/transfer \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "fromCardId": 1,
    "toCardId": 2,
    "amount": 100.00
  }'
```

### Получение карт с фильтрацией и пагинацией

```bash
curl -X GET "http://localhost:8080/api/cards?status=ACTIVE&page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

## Начальные данные

После запуска создается администратор:
- Email: `admin@bank.com`
- Password: `admin123`

## Переменные окружения

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `DB_HOST` | Хост базы данных | `localhost` |
| `DB_PORT` | Порт базы данных | `5432` |
| `DB_NAME` | Имя базы данных | `bankdb` |
| `DB_USERNAME` | Пользователь БД | `postgres` |
| `DB_PASSWORD` | Пароль БД | `postgres` |
| `JWT_SECRET` | Секретный ключ JWT (мин. 256 бит) | - |
| `ENCRYPTION_KEY` | Ключ шифрования AES (32 символа) | - |

## Структура проекта

```
src/main/java/com/example/bankcards/
├── BankCardsApplication.java     # Точка входа
├── config/                       # Конфигурация
│   ├── SecurityConfig.java       # Spring Security
│   └── OpenApiConfig.java        # Swagger
├── controller/                   # REST контроллеры
├── dto/                          # Data Transfer Objects
│   ├── request/
│   └── response/
├── entity/                       # JPA сущности
├── exception/                    # Обработка ошибок
├── repository/                   # Spring Data репозитории
├── security/                     # JWT компоненты
├── service/                      # Бизнес-логика
└── util/                         # Утилиты (шифрование)
```

## Лицензия

MIT
