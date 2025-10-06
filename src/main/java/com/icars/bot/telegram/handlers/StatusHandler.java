package com.icars.bot.telegram.handlers;

import com.icars.bot.domain.Order;
import com.icars.bot.domain.TimelineEvent;
import com.icars.bot.service.OrderService;
import com.icars.bot.service.StatusService;
import com.icars.bot.util.Markdown;
import com.icars.bot.util.PhoneUtil;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StatusHandler {
    private static final Logger logger = LoggerFactory.getLogger(StatusHandler.class);
    private enum State { WAIT_FOR_IDENTIFIER }

    private final AbsSender sender;
    private final Map<Long, String> userStates;
    private final OrderService orderService;
    private final StatusService statusService;

    public StatusHandler(AbsSender sender, Jdbi jdbi, Map<Long, String> userStates) {
        this.sender = sender;
        this.userStates = userStates;
        this.orderService = new OrderService(jdbi);
        this.statusService = new StatusService(jdbi, null); // Notifier not needed here
    }

    public void start(Update update) {
        long chatId = update.getMessage().getChatId();
        userStates.put(chatId, "STATUS_" + State.WAIT_FOR_IDENTIFIER);
        ResourceBundle messages = getMessages(update);
        ask(chatId, messages.getString("status.ask_identifier"));
    }

    public void handle(Update update) {if (update.hasCallbackQuery()) {
        logger.info("Wizard callback: data='{}', state='{}'",
                update.getCallbackQuery().getData(),
                userStates.getOrDefault(
                        update.getCallbackQuery().getMessage().getChatId(), "n/a")
        );
    }
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        long chatId = update.getMessage().getChatId();
        String stateStr = userStates.getOrDefault(chatId, "");
        if (!stateStr.equals("STATUS_" + State.WAIT_FOR_IDENTIFIER)) return;

        String identifier = update.getMessage().getText().trim();
        ResourceBundle messages = getMessages(update);
        Optional<Order> orderOpt;

        boolean looksLikeId = identifier.matches("(?i)^[A-Z]{2}-\\d{6}-\\d{3}$") || identifier.toLowerCase().startsWith("ic-");
        if (looksLikeId) {
            orderOpt = orderService.findByPublicId(identifier.toUpperCase());
        } else {
            String formattedPhone = com.icars.bot.util.PhoneUtil.formatE164(identifier);
            if (formattedPhone == null || !com.icars.bot.service.ValidationService.isValidPhoneNumber(formattedPhone)) {
                ask(chatId, messages.getString("validation.error.phone"));
                userStates.remove(chatId);
                return;
            }
            orderOpt = orderService.findLastByPhone(formattedPhone);
            if (orderOpt.isEmpty()) {
                ask(chatId, messages.getString("status.not_found"));
                userStates.remove(chatId);
                return;
            }

        }

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            List<TimelineEvent> timeline = statusService.getPublicTimeline(order.getId());
            sendStatusMessage(chatId, order, timeline, messages);
        } else {
            ask(chatId, messages.getString("status.not_found"));
        }
        userStates.remove(chatId);
    }

    private void sendStatusMessage(long chatId, Order order, List<TimelineEvent> timeline, ResourceBundle messages) {
        String title = MessageFormat.format(messages.getString("status.result.title"),
                order.getPublicId(), // без MarkdownV2, просто строка
                order.getStatus().name()
        );

        StringBuilder sb = new StringBuilder(title);

        if (!timeline.isEmpty()) {
            sb.append("\n\n").append(messages.getString("status.result.timeline.title"));
            String timelineEvents = timeline.stream()
                    .map(e -> {
                        java.util.Date dt = e.getEventTimestamp() == null
                                ? new java.util.Date()
                                : java.util.Date.from(e.getEventTimestamp()); // <--- FIX
                        return MessageFormat.format(messages.getString("status.result.timeline.event"),
                                dt,
                                e.getDescription() == null ? "-" : e.getDescription());
                    })
                    .collect(Collectors.joining("\n"));
            sb.append("\n").append(timelineEvents);
        }

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(sb.toString());
        // не ставим parseMode
        try {
            sender.execute(sm);
        } catch (TelegramApiException e) {
            logger.error("Failed to send status message for order {} to chat {}", order.getPublicId(), chatId, e);
        }
    }


    private void ask(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send status handler message to chat {}", chatId, e);
        }
    }

    private ResourceBundle getMessages(Update update) {
        // Basic language selection, defaulting to Russian
        String lang = update.getMessage().getFrom().getLanguageCode() != null && update.getMessage().getFrom().getLanguageCode().startsWith("en") ? "en" : "ru";
        return ResourceBundle.getBundle("i18n.messages", java.util.Locale.forLanguageTag(lang));
    }
}
