package com.icars.bot.telegram.keyboards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;
import java.util.ResourceBundle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

public class ReplyKeyboards {

    public static ReplyKeyboardMarkup mainMenu(ResourceBundle messages) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(messages.getString("menu.button.new_engine")));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(messages.getString("menu.button.status")));
        row2.add(new KeyboardButton(messages.getString("menu.button.faq")));

        // Example for a future feature
        // KeyboardRow row3 = new KeyboardRow();
        // row3.add(new KeyboardButton(messages.getString("menu.button.new_transmission")));

        keyboardMarkup.setKeyboard(List.of(row1, row2));
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup requestContact(ResourceBundle messages) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        KeyboardRow row = new KeyboardRow();
        KeyboardButton button = new KeyboardButton(messages.getString("wizard.engine.button.request_contact"));
        button.setRequestContact(true);
        row.add(button);

        keyboardMarkup.setKeyboard(List.of(row));
        return keyboardMarkup;
    }

    public static ReplyKeyboardRemove hide() {
        return new ReplyKeyboardRemove(true);
    }
}
