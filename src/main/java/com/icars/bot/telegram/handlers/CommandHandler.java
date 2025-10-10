package com.icars.bot.telegram.handlers;

import com.icars.bot.telegram.keyboards.ReplyKeyboards;
import com.icars.bot.i18n.I18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private final AbsSender sender;

    public CommandHandler(AbsSender sender) {
        this.sender = sender;
    }

    /** Универсально достаём chatId из любого типа апдейта */
    private Long extractChatId(Update update) {
        if (update == null) return null;
        if (update.hasMessage() && update.getMessage() != null) {
            return update.getMessage().getChatId();
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        if (update.hasMyChatMember() && update.getMyChatMember().getChat() != null) {
            return update.getMyChatMember().getChat().getId();
        }
        if (update.hasChatJoinRequest() && update.getChatJoinRequest().getChat() != null) {
            return update.getChatJoinRequest().getChat().getId();
        }
        return null;
    }

    /** Берём i18n bundle по языку пользователя, по умолчанию ru */
    private ResourceBundle getMessages(Update update) {
        String lang = "ru";
        var user = update != null
                ? (update.hasCallbackQuery()
                    ? update.getCallbackQuery().getFrom()
                    : (update.getMessage() != null ? update.getMessage().getFrom() : null))
                : null;
        if (user != null && user.getLanguageCode() != null && user.getLanguageCode().startsWith("en")) {
            lang = "en";
        }
        return ResourceBundle.getBundle("i18n.messages", I18n.resolve(null, lang), new I18n.UTF8Control());
    }

    public void handleStart(Update update) {
        Long chatId = extractChatId(update);
        if (chatId == null) return;

        ResourceBundle messages = getMessages(update);

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(messages.getString("welcome"));
        sm.setReplyMarkup(ReplyKeyboards.mainMenu(messages));
        try {
            sender.execute(sm);
        } catch (TelegramApiException e) {
            logger.error("Failed to send /start welcome", e);
        }
    }

    public void handleUnknown(Update update) {
        Long chatId = extractChatId(update);
        if (chatId == null) return;

        ResourceBundle messages = getMessages(update);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Неизвестная команда. Используйте меню ниже.");
        message.setReplyMarkup(ReplyKeyboards.mainMenu(messages));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send unknown command message to chat {}", chatId, e);
        }
    }

    public void handleFaq(Update update) {
        Long chatId = extractChatId(update);
        if (chatId == null) return;

        String text = """
i CARS PRO — основной канал (с 2019):
— Импорт авто 🚗 🇩🇪🇰🇷🇯🇵🇨🇳
— Поиск авто, финансы, логистика, таможня, учёт.
— Находим фабрики и поставщиков в Китае.
Связь: +7 916 691-54-24 Игорь @icars_is

iCars Engine — двигатели в наличии и под заказ.

Выберите раздел:
""";

        InlineKeyboardButton b1 = new InlineKeyboardButton("i CARS PRO — канал");
        b1.setUrl("https://t.me/i_cars_pro");

        InlineKeyboardButton b2 = new InlineKeyboardButton("iCars Engine — двигатели");
        b2.setUrl("https://t.me/icarsengine");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(b1));
        rows.add(List.of(b2));

        InlineKeyboardMarkup ik = new InlineKeyboardMarkup();
        ik.setKeyboard(rows);

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(text);
        sm.setReplyMarkup(ik);

        try {
            sender.execute(sm);
        } catch (TelegramApiException e) {
            logger.error("Failed to send FAQ message to chat {}", chatId, e);
        }
    }
}
