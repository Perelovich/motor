package com.icars.bot.telegram.dispatcher;

// In a more complex bot, this class could handle cross-cutting concerns
// like logging, authentication checks, language settings, etc.,
// before the UpdateRouter dispatches the update to a specific handler.

// For this project's scope, its functionality is integrated directly
// into the UpdateRouter for simplicity.

public class Middleware {
    // Example method:
    /*
    public boolean checkAdminAccess(Update update, BotConfig config) {
        Long userId = update.hasCallbackQuery()
            ? update.getCallbackQuery().getFrom().getId()
            : update.getMessage().getFrom().getId();
        return config.isAdmin(userId);
    }
    */
}
