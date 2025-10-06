package com.icars.bot.telegram.dispatcher;

import com.icars.bot.config.BotConfig;
import com.icars.bot.telegram.ICarsBot;
import com.icars.bot.telegram.handlers.AdminHandler;
import com.icars.bot.telegram.handlers.CommandHandler;
import com.icars.bot.telegram.handlers.NewEngineOrderWizard;
import com.icars.bot.telegram.handlers.StatusHandler;
import org.jdbi.v3.core.Jdbi;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateRouter {
    private final CommandHandler commandHandler;
    private final NewEngineOrderWizard newEngineOrderWizard;
    private final StatusHandler statusHandler;
    private final AdminHandler adminHandler;
    private final Map<Long, String> userStates = new ConcurrentHashMap<>();

    public UpdateRouter(ICarsBot bot, Jdbi jdbi, BotConfig config) {
        this.commandHandler = new CommandHandler(bot);
        this.newEngineOrderWizard = new NewEngineOrderWizard(bot, jdbi, config, userStates);
        this.statusHandler = new StatusHandler(bot, jdbi, userStates);
        this.adminHandler = new AdminHandler(bot, jdbi, config);
    }

    public void handle(Update update) {
        // 1) Игнорим канал-посты и всё, где нет message/колбэка
        if (update == null) return;
        if (!(update.hasMessage() || update.hasCallbackQuery())) return;
        if (update.hasChannelPost()) return;

        Long chatId = update.hasCallbackQuery()
                ? update.getCallbackQuery().getMessage().getChatId()
                : update.getMessage().getChatId();

        String currentState = userStates.getOrDefault(chatId, "START");

        // 2) Команды
        if (update.hasMessage() && update.getMessage().isCommand()) {
            String command = update.getMessage().getText().split(" ")[0];
            userStates.remove(chatId); // сбрасываем состояние
            switch (command) {
                case "/start":
                    commandHandler.handleStart(update); // покажет главное меню
                    break;
                case "/engine":
                    newEngineOrderWizard.startWizard(update); // старт мастера
                    break;
                case "/status":
                    statusHandler.start(update);
                    break;
                case "/faq":
                    commandHandler.handleFaq(update); // добавим ниже
                    break;
                case "/admin":
                    adminHandler.handle(update);
                    break;
                default:
                    commandHandler.handleStart(update);
            }
            return;
        }

        // 3) FSM
        if (currentState.startsWith("WIZARD_ENGINE_")) {
            newEngineOrderWizard.handle(update);
            return;
        }
        if (currentState.startsWith("STATUS_")) {
            statusHandler.handle(update);
            return;
        }

        // 4) Нажатия кнопок главного меню (текстовые)
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            if (text.contains("Заказать двигатель") || text.contains("Order an engine")) {
                newEngineOrderWizard.startWizard(update);
            } else if (text.contains("Проверить статус") || text.contains("Check order status")) {
                statusHandler.start(update);
            } else if (text.contains("FAQ") || text.contains("Частые вопросы")) {
                commandHandler.handleFaq(update);
            } else {
                commandHandler.handleStart(update);
            }
        }
    }

    /** Достаём chatId из разных типов апдейтов безопасно */
    private Long extractChatId(Update u) {
        if (u.hasMessage() && u.getMessage() != null) {
            return u.getMessage().getChatId();
        }
        if (u.hasCallbackQuery() && u.getCallbackQuery().getMessage() != null) {
            return u.getCallbackQuery().getMessage().getChatId();
        }
        if (u.hasMyChatMember() && u.getMyChatMember().getChat() != null) {
            return u.getMyChatMember().getChat().getId();
        }
        // channelPost мы игнорим выше, но на всякий:
        if (u.hasChannelPost() && u.getChannelPost() != null) {
            return u.getChannelPost().getChatId();
        }
        return null;
    }
}
