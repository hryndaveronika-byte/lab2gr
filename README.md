# Smart Library — лабораторна робота №2

REST API для автоматизації роботи бібліотеки. Java 21, Maven, Spring Boot 3.3.5, Spring Data JPA, H2.

## Запуск в IntelliJ IDEA

1. Розпакуйте ZIP і в IntelliJ IDEA виберіть **Open**, вкажіть папку `smart-library` (або файл `pom.xml` як Maven-проєкт).
2. Налаштуйте **Project SDK = JDK 21** (File → Project Structure → Project). Якщо JDK 21 відсутня, встановіть її через IntelliJ.
3. Дочекайтеся імпорту Maven і завантаження залежностей (для першого запуску потрібен інтернет).
4. Запустіть `SmartLibraryApplication.java` зеленою кнопкою **Run**.
5. API доступне за адресою `http://localhost:8080`.

У терміналі IntelliJ (за наявності Maven):

```bash
mvn clean test
mvn spring-boot:run
```

База H2 зберігається у папці `data/` і не видаляється після перезапуску. Папка ігнорується Git. Консоль H2: `http://localhost:8080/h2-console`, JDBC URL: `jdbc:h2:file:./data/smartlibrary`, username: `sa`, password: порожній. Для відкриття файлової БД через консоль може знадобитись закрити інші підключення до цього файлу; API працює незалежно від консолі.

## API

| Метод | Endpoint | Дія |
| --- | --- | --- |
| POST | `/api/readers` | Створити читача |
| POST | `/api/books` | Створити книгу |
| GET | `/api/books` | Всі книги |
| POST | `/api/loans` | Видати книгу |
| GET | `/api/readers/{readerId}/reliability` | Перевірити надійність |
| POST | `/api/loans/{loanId}/return` | Повернути книгу |
| GET | `/api/readers/{readerId}/loans/active` | Активні позики читача |

Відправляйте `Content-Type: application/json` у POST-запитах.

### 1. Створити читача
```http
POST http://localhost:8080/api/readers
Content-Type: application/json

{"firstName":"Anna","lastName":"Ivanenko","email":"anna@example.com"}
```
Email приводиться до нижнього регістру; повторний email (незалежно від регістру) → `409 Conflict`.

### 2. Створити книгу
```http
POST http://localhost:8080/api/books
Content-Type: application/json

{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884"}
```
`isbn` необов'язковий. Нова книга має статус `AVAILABLE`.

### 3. Список книг
```http
GET http://localhost:8080/api/books
```

### 4. Видати книгу
```http
POST http://localhost:8080/api/loans
Content-Type: application/json

{"readerId":1,"bookId":1}
```
Створюється позика з `borrowedAt` і `dueAt` (+14 днів); книга стає `BORROWED`. Недоступну книгу чи перевищений ліміт видати не можна (`409`).

### 5. Надійність
```http
GET http://localhost:8080/api/readers/1/reliability
```
Приклад відповіді:
```json
{"readerId":1,"reliable":false,"returnedLast30Days":0,"allReturnedOnTime":true,"hasOverdueActiveLoans":false,"activeLoans":1,"maxActiveLoans":2}
```
Читач **надійний**, лише коли за 30 днів включно до моменту перевірки повернуто мінімум 3 книги, всі ці повернення були вчасні й немає жодної простроченої активної позики. Надійний ліміт — 5, звичайний — 2.

### 6. Повернути книгу
```http
POST http://localhost:8080/api/loans/1/return
```
Позика залишається в історії, `returnedAt` заповнюється, книга стає `AVAILABLE`. Повторне повернення → `409`.

### 7. Активні позики читача
```http
GET http://localhost:8080/api/readers/1/loans/active
```

## Правила та особливості

- Усі дати/час у JSON — **UTC** (`LocalDateTime`, формат `2026-11-19T12:00:00`).
- Позика вважається простроченою, коли `dueAt < now` і `returnedAt = null`.
- Повернення вважається вчасним, якщо `returnedAt <= dueAt`.
- Вікно перевірки: `now - 30 days <= returnedAt <= now`.
- Блокування читача та книги під час видачі захищає від одночасного перевищення ліміту та подвійної видачі тієї ж книги.
- Повідомлення про помилки: `400` (невалідний JSON/поля), `404` (немає книги, читача чи позики), `409` (порушення бізнес-правил/унікальності).

## Тести

Відкрийте `src/test/java/.../SmartLibraryIntegrationTest.java` і натисніть **Run** або виконайте `mvn test`. Тести перевіряють ліміт 2, надійність та ліміт 5, прострочене повернення, збереження історії, повторне повернення, унікальність email та коди API.

## Надсилання

Завантажте код у GitHub-репозиторій. У листі на `yehor.bobyk@uzhnu.edu.ua` надішліть посилання на репозиторій, унизу — своє прізвище й ім'я. Звіт не потрібен.
