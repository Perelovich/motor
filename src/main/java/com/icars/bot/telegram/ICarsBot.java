package com.icars.bot.telegram;

import com.icars.bot.config.BotConfig;
import com.icars.bot.telegram.dispatcher.UpdateRouter;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ICarsBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(ICarsBot.class);

    private final BotConfig botConfig;
    private final UpdateRouter updateRouter;


    public ICarsBot(BotConfig botConfig, Jdbi jdbi) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.updateRouter = new UpdateRouter(this, jdbi, botConfig);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            updateRouter.handle(update);
        } catch (Exception e) {
            logger.error("Error processing update: " + update.getUpdateId(), e);
            // Optionally, send a message to the user or an admin about the error.
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }
}
