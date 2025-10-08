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
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateRouter {
    private final CommandHandler commandHandler;
    private final NewEngineOrderWizard newEngineOrderWizard;
    private final StatusHandler statusHandler;
    private final AdminHandler adminHandler;

    // chatId -> FSM state
    private final Map<Long, String> userStates = new ConcurrentHashMap<>();

    public UpdateRouter(ICarsBot bot, Jdbi jdbi, BotConfig config) {
        this.commandHandler = new CommandHandler(bot);
        this.newEngineOrderWizard = new NewEngineOrderWizard(bot, jdbi, config, userStates);
        this.statusHandler = new StatusHandler(bot, jdbi, userStates);
        this.adminHandler = new AdminHandler(bot, jdbi, config);
    }

    public void handle(Update update) {
        if (update == null) return;
        if (!(update.hasMessage() || update.hasCallbackQuery())) return;
        if (update.hasChannelPost()) return;

        Long chatId = extractChatId(update);
        if (chatId == null) return;

        String currentState = userStates.getOrDefault(chatId, "START");

        // --- Глобальные callback-и (работают в любом состоянии)
        if (update.hasCallbackQuery()) {
            String data = String.valueOf(update.getCallbackQuery().getData());
            if ("menu:back".equals(data)) {
                userStates.remove(chatId);
                commandHandler.handleStart(update); // покажет главное меню (reply-клава)
                return;
            }
        }

        // --- Команды
        if (update.hasMessage() && update.getMessage().isCommand()) {
            String command = update.getMessage().getText().split(" ")[0];
            userStates.remove(chatId);
            switch (command) {
                case "/start":
                    commandHandler.handleStart(update);
                    break;
                case "/engine":
                    newEngineOrderWizard.startWizard(update);
                    break;
                case "/status":
                    statusHandler.start(update);
                    break;
                case "/faq":
                    commandHandler.handleFaq(update);
                    break;
                case "/admin":
                    adminHandler.handle(update);
                    break;
                default:
                    commandHandler.handleStart(update);
            }
            return;
        }

        // --- FSM (мастеры)
        if (currentState.startsWith("WIZARD_ENGINE_")) {
            newEngineOrderWizard.handle(update);
            return;
        }
        if (currentState.startsWith("STATUS_")) {
            statusHandler.handle(update);
            return;
        }

        // --- Нажатия кнопок главного меню (REPLY-кнопки с текстом)
        if (update.hasMessage() && update.getMessage().hasText()) {
            ResourceBundle rb = commandHandler.getMessages(update);
            final String BTN_ENGINE = rb.getString("menu.order_engine");
            final String BTN_STATUS = rb.getString("menu.check_status");
            final String BTN_FAQ    = rb.getString("menu.faq");

            String text = update.getMessage().getText().trim();
            if (text.equals(BTN_ENGINE)) {
                newEngineOrderWizard.startWizard(update);
                return;
            }
            if (text.equals(BTN_STATUS)) {
                statusHandler.start(update);
                return;
            }
            if (text.equals(BTN_FAQ)) {
                commandHandler.handleFaq(update);
                return;
            }

            // дефолт — показать меню
            commandHandler.handleStart(update);
        }
    }

    /** Безопасное извлечение chatId из любого типа апдейта */
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
        if (u.hasChannelPost() && u.getChannelPost() != null) {
            return u.getChannelPost().getChatId();
        }
        return null;
    }
}
