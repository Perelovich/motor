ICars Powertrain Telegram BotЭтот бот предназначен для сбора и обработки заявок на двигатели и другие агрегаты из Китая.ФункционалОформление заявки на двигатель через пошаговый опросник (wizard).Автоматическая валидация VIN, года выпуска, телефона.Генерация уникального номера заявки (например, IC-240925-001).Уведомление менеджеров в специальный OPS-чат о новой заявке.Проверка статуса заявки по её номеру или номеру телефона.Административные команды для управления заявками.Поддержка двух языков: Русский (по умолчанию) и Английский.Стек технологийЯзык: Java 21Сборка: MavenTelegram API: telegrambots (Long Polling)База данных: PostgreSQL 16Миграции: FlywayДоступ к БД: JDBI 3, HikariCPЛогирование: SLF4J + LogbackШаблонизация: FreemarkerКонтейнеризация: Docker, Docker ComposeЗапуск проекта1. ПодготовкаУстановленные Java 21 (JDK) и Maven.Установленный Docker и Docker Compose.Созданный Telegram-бот и полученный токен.ID чата для операционных уведомлений (OPS-чат).Telegram ID администраторов.2. Запуск через Docker Compose и CLIЗапустите базу данных:Откройте терминал в корне проекта и выполните:docker-compose up -d db
Дождитесь, пока контейнер с PostgreSQL полностью запустится (может занять 15-30 секунд).Настройте переменные окружения:Выполните в том же терминале. Замените <...> своими значениями.# Обязательно: токен вашего бота
export TG_BOT_TOKEN="<YOUR_BOT_TOKEN>"

# Обязательно: ID чата для уведомлений о новых заявках
export OPS_CHAT_ID="<YOUR_OPS_CHAT_ID>"

# Обязательно: ID администраторов через запятую
export ADMIN_TG_IDS="<YOUR_ADMIN_ID_1>,<YOUR_ADMIN_ID_2>"

# Параметры БД (совпадают с docker-compose.yml)
export DB_URL="jdbc:postgresql://localhost:5432/icars"
export DB_USER="postgres"
export DB_PASS="postgres"
Соберите и запустите приложение:mvn clean package
java -jar target/icars-powertrain-bot.jar
3. Запуск в IntelliJ IDEAОткройте проект в IDEA как Maven-проект.Запустите БД командой docker-compose up -d db.Перейдите в Run -> Edit Configurations....Нажмите + и выберите Application.В поле Main class выберите com.icars.bot.App.В секции Environment variables добавьте переменные, описанные в шаге 2 выше (например, TG_BOT_TOKEN=<YOUR_BOT_TOKEN>).Сохраните конфигурацию и запустите её.Карта состояний (FSM) для заказа двигателяSTART -> /engine -> ASK_VIN -> ASK_MAKE -> ASK_MODEL -> ASK_YEAR -> ASK_ENGINE_CODE -> ASK_FUEL_TURBO -> ASK_INJECTION_EURO -> ASK_KIT -> ASK_CITY -> ASK_CONTACT -> PREVIEW -> CONFIRMEDПримеры команд/start - Показать приветственное сообщение и главное меню./engine - Начать процесс оформления заявки на двигатель./status - Проверить статус существующей заявки./admin - (Только для администраторов) Показать доступные админ-команды./admin last [N] - Показать последние N заявок (по умолчанию 5)./admin find <query> - Найти заявки по номеру, VIN или телефону./admin set <public_id> <STATUS_NAME> - Установить новый статус для заявки.