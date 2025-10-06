package com.icars.bot.telegram.keyboards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.ResourceBundle;

public class InlineKeyboards {

    public static InlineKeyboardMarkup skip(ResourceBundle messages) {
        InlineKeyboardButton b = new InlineKeyboardButton(messages.getString("wizard.engine.skip"));
        b.setCallbackData("skip");
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(List.of(List.of(b)));
        return kb;
    }

    public static InlineKeyboardMarkup confirm(ResourceBundle messages) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton(messages.getString("wizard.engine.preview.confirm"));
        confirmButton.setCallbackData("confirm");
        InlineKeyboardButton cancelButton = new InlineKeyboardButton(messages.getString("wizard.engine.preview.cancel"));
        cancelButton.setCallbackData("cancel");
        markup.setKeyboard(List.of(List.of(confirmButton, cancelButton)));
        return markup;
    }

    public static InlineKeyboardMarkup fuelTurbo(ResourceBundle messages) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton petrolTurbo = new InlineKeyboardButton(messages.getString("wizard.engine.fuel.petrol") + " + " + messages.getString("wizard.engine.turbo.yes"));
        petrolTurbo.setCallbackData("PETROL_turbo");
        InlineKeyboardButton petrolNA = new InlineKeyboardButton(messages.getString("wizard.engine.fuel.petrol") + " + " + messages.getString("wizard.engine.turbo.no"));
        petrolNA.setCallbackData("PETROL_na");

        InlineKeyboardButton dieselTurbo = new InlineKeyboardButton(messages.getString("wizard.engine.fuel.diesel") + " + " + messages.getString("wizard.engine.turbo.yes"));
        dieselTurbo.setCallbackData("DIESEL_turbo");
        InlineKeyboardButton dieselNA = new InlineKeyboardButton(messages.getString("wizard.engine.fuel.diesel") + " + " + messages.getString("wizard.engine.turbo.no"));
        dieselNA.setCallbackData("DIESEL_na");

        InlineKeyboardButton hybrid = new InlineKeyboardButton(messages.getString("wizard.engine.fuel.hybrid"));
        hybrid.setCallbackData("HYBRID_na"); // Assuming most hybrids are NA for simplicity

        markup.setKeyboard(List.of(
                List.of(petrolTurbo, petrolNA),
                List.of(dieselTurbo, dieselNA),
                List.of(hybrid)
        ));
        return markup;
    }

    public static InlineKeyboardMarkup injectionEuro(ResourceBundle messages) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton direct = new InlineKeyboardButton(messages.getString("wizard.engine.injection.direct"));
        direct.setCallbackData("DIRECT_EURO_NA"); // Example callback
        InlineKeyboardButton mpi = new InlineKeyboardButton(messages.getString("wizard.engine.injection.mpi"));
        mpi.setCallbackData("MPI_EURO_NA");
        InlineKeyboardButton commonRail = new InlineKeyboardButton(messages.getString("wizard.engine.injection.common_rail"));
        commonRail.setCallbackData("COMMON_RAIL_EURO_NA");
        InlineKeyboardButton na = new InlineKeyboardButton(messages.getString("wizard.engine.euro.na"));
        na.setCallbackData("UNKNOWN_EURO_NA");

        markup.setKeyboard(List.of(
                List.of(direct, mpi),
                List.of(commonRail, na)
        ));
        return markup;
    }
}
