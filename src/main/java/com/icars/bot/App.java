package com.icars.bot;

import com.icars.bot.config.BotConfig;
import com.icars.bot.config.DatabaseConfig;
import com.icars.bot.service.NotificationService;
import com.icars.bot.telegram.ICarsBot;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.sql.DataSource;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        logger.info("Starting ICars Powertrain Bot...");

        try {
            // 1) Конфиг
            BotConfig botConfig = new BotConfig();

            DatabaseConfig dbConfig = new DatabaseConfig();
            logger.info("Configuration loaded successfully.");

            // 2) Пул коннектов
            DataSource dataSource = dbConfig.getDataSource();
            logger.info("Database connection pool initialized.");

            // 3) Миграции
            runMigrations(dataSource);
            logger.info("Database migrations executed successfully.");

            // 4) Jdbi + плагины
            Jdbi jdbi = Jdbi.create(dataSource);
            jdbi.installPlugin(new SqlObjectPlugin()); // ВАЖНО: включает attach() для @SqlQuery/@SqlUpdate
            // sanity-check: убедимся, что Jdbi живой
            jdbi.useHandle(h -> h.createQuery("select 1").mapTo(int.class).one());
            logger.info("JDBI instance created.");

            // 5) Telegram bot
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            ICarsBot bot = new ICarsBot(botConfig, jdbi);
            botsApi.registerBot(bot);
            new NotificationService(bot, botConfig).notifyOpsPing();
            logger.info("ICars Powertrain Bot is now running.");

            // 6) Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down ICars Powertrain Bot...");
                dbConfig.closeDataSource();
                logger.info("Database connection pool closed.");
            }));

        } catch (TelegramApiException e) {
            logger.error("Error initializing or registering Telegram bot", e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during application startup", e);
        }
    }

    private static void runMigrations(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }
}
