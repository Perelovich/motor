package com.icars.bot.telegram.handlers;

import com.icars.bot.telegram.keyboards.ReplyKeyboards;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private final AbsSender sender;

    public CommandHandler(AbsSender sender) {
        this.sender = sender;
    }

    public void handleStart(Update update) {
        long chatId = update.getMessage().getChatId();
        // For simplicity, we default to Russian. A real bot would detect user language.
        ResourceBundle messages = ResourceBundle.getBundle("i18n.messages_ru");

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messages.getString("welcome"));
        message.setReplyMarkup(ReplyKeyboards.mainMenu(messages));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send start message to chat {}", chatId, e);
        }
    }

    public void handleUnknown(Update update) {
        long chatId = update.getMessage().getChatId();
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Неизвестная команда. Используйте меню для навигации.");
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send unknown command message to chat {}", chatId, e);
        }
    }
}
