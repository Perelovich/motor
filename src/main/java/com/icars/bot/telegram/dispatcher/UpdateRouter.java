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
        Long chatId = update.hasCallbackQuery() ? update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId();
        String currentState = userStates.getOrDefault(chatId, "START");

        if (update.hasMessage() && update.getMessage().isCommand()) {
            String command = update.getMessage().getText().split(" ")[0];
            userStates.remove(chatId); // Reset state on any new command
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
                case "/admin":
                    adminHandler.handle(update);
                    break;
                default:
                    commandHandler.handleUnknown(update);
                    break;
            }
        } else if (currentState.startsWith("WIZARD_ENGINE_")) {
            newEngineOrderWizard.handle(update);
        } else if (currentState.startsWith("STATUS_")) {
            statusHandler.handle(update);
        } else {
            // Handle menu button clicks from non-command messages
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                // This is a simplified approach. A more robust solution would use a ResourceBundle
                // or a map to link button texts to commands.
                if (text.contains("Заказать двигатель") || text.contains("Order an engine")) {
                    newEngineOrderWizard.startWizard(update);
                } else if (text.contains("Проверить статус") || text.contains("Check order status")) {
                    statusHandler.start(update);
                } else {
                    commandHandler.handleStart(update); // Default to start menu
                }
            }
        }
    }
}
