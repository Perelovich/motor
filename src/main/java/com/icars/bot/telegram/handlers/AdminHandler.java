package com.icars.bot.telegram.handlers;

import com.icars.bot.config.BotConfig;
import com.icars.bot.domain.Order;
import com.icars.bot.domain.OrderStatus;
import com.icars.bot.service.OrderService;
import com.icars.bot.service.StatusService;
import com.icars.bot.util.Markdown;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class AdminHandler {
    private static final Logger logger = LoggerFactory.getLogger(AdminHandler.class);

    private final AbsSender sender;
    private final BotConfig config;
    private final OrderService orderService;
    private final StatusService statusService;

    public AdminHandler(AbsSender sender, Jdbi jdbi, BotConfig config) {
        this.sender = sender;
        this.config = config;
        this.orderService = new OrderService(jdbi);
        this.statusService = new StatusService(jdbi, null); // Notifier not needed here
    }

    public void handle(Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        ResourceBundle messages = getMessages(update);

        if (!config.isAdmin(userId)) {
            sendMessage(chatId, messages.getString("admin.unauthorized"));
            return;
        }

        String[] parts = update.getMessage().getText().split("\\s+");
        String command = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "last":
                handleLast(chatId, parts);
                break;
            case "find":
                handleFind(chatId, parts);
                break;
            case "set":
                handleSetStatus(chatId, parts, messages);
                break;
            default:
                sendMessage(chatId, messages.getString("admin.menu"));
        }
    }

    private void handleLast(long chatId, String[] parts) {
        int limit = 5;
        if (parts.length > 2) {
            try {
                limit = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Invalid number format for limit.");
                return;
            }
        }
        List<Order> orders = orderService.findLastOrders(limit);
        sendOrderList(chatId, orders, "Last " + limit + " orders");
    }

    private void handleFind(long chatId, String[] parts) {
        if (parts.length < 3) {
            sendMessage(chatId, "Usage: /admin find <query>");
            return;
        }
        String query = parts[2];
        List<Order> orders = orderService.searchOrders(query);
        sendOrderList(chatId, orders, "Found orders for: " + query);
    }

    private void handleSetStatus(long chatId, String[] parts, ResourceBundle messages) {
        if (parts.length < 4) {
            sendMessage(chatId, "Usage: /admin set <public_id> <STATUS>");
            return;
        }
        String publicId = parts[2].toUpperCase();
        String statusStr = parts[3].toUpperCase();

        try {
            OrderStatus newStatus = OrderStatus.valueOf(statusStr);
            orderService.findByPublicId(publicId).ifPresentOrElse(order -> {
                statusService.updateStatus(order.getId(), newStatus, "Status updated by admin.");
                String msg = MessageFormat.format(messages.getString("admin.status_updated"), publicId, newStatus.name());
                sendMessage(chatId, msg);
            }, () -> sendMessage(chatId, "Order " + publicId + " not found."));
        } catch (IllegalArgumentException e) {
            String msg = MessageFormat.format(messages.getString("admin.error.invalid_status"), statusStr);
            sendMessage(chatId, msg);
        }
    }

    private void sendOrderList(long chatId, List<Order> orders, String title) {
        if (orders.isEmpty()) {
            sendMessage(chatId, "No orders found.");
            return;
        }
        StringBuilder sb = new StringBuilder("*" + Markdown.escape(title) + "*\n\n");
        for (Order order : orders) {
            sb.append(String.format("`%s` \\- `%s` \\- %s, %s\n",
                    Markdown.escape(order.getPublicId()),
                    Markdown.escape(order.getStatus().name()),
                    Markdown.escape(order.getCustomerName()),
                    Markdown.escape(order.getCustomerPhone())
            ));
        }
        sendMessage(chatId, sb.toString(), true);
    }

    private void sendMessage(long chatId, String text) {
        sendMessage(chatId, text, false);
    }

    private void sendMessage(long chatId, String text, boolean isMarkdown) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        if (isMarkdown) {
            message.setParseMode("MarkdownV2");
        }
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send admin message to chat {}", chatId, e);
        }
    }

    private ResourceBundle getMessages(Update update) {
        String lang = update.getMessage().getFrom().getLanguageCode() != null && update.getMessage().getFrom().getLanguageCode().startsWith("en") ? "en" : "ru";
        return ResourceBundle.getBundle("i18n.messages", java.util.Locale.forLanguageTag(lang));
    }
}
