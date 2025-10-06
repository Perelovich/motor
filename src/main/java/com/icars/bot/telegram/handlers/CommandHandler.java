package com.icars.bot.telegram.handlers;

import com.icars.bot.telegram.keyboards.ReplyKeyboards;
// если пользуешься своим UTF-8 контролом:
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
import java.util.Locale;
import java.util.ResourceBundle;

public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private final AbsSender sender;

    public CommandHandler(AbsSender sender) {
        this.sender = sender;
    }

    public void handleStart(Update update) {
        long chatId = update.getMessage().getChatId();

        // Локаль по-умолчанию RU. Если хочешь детектить язык — достань его из update.getMessage().getFrom().getLanguageCode()
        Locale locale = I18n.resolve(null, "ru");
        ResourceBundle messages = ResourceBundle.getBundle("i18n.messages", locale, new I18n.UTF8Control());

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messages.getString("welcome"));
        message.setReplyMarkup(ReplyKeyboards.mainMenu(messages)); // показать главное меню

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send start message to chat {}", chatId, e);
        }
    }

    public void handleUnknown(Update update) {
        long chatId = update.getMessage().getChatId();

        // Можно без бандла, просто покажем текст и снова главное меню:
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Неизвестная команда. Используйте меню ниже.");
        message.setReplyMarkup(ReplyKeyboards.mainMenu(
                ResourceBundle.getBundle("i18n.messages", I18n.resolve(null, "ru"), new I18n.UTF8Control())
        ));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send unknown command message to chat {}", chatId, e);
        }
    }

    public void handleFaq(Update update) {
        long chatId = update.getMessage().getChatId();

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
            sender.execute(sm); // <-- ИСПОЛЬЗУЕМ sender, а не bot
        } catch (TelegramApiException e) {
            logger.error("Failed to send FAQ message to chat {}", chatId, e);
        }
    }
}
