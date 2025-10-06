package com.icars.bot.service;

import com.icars.bot.config.BotConfig;
import com.icars.bot.domain.EngineAttributes;
import com.icars.bot.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final AbsSender sender;
    private final BotConfig config;

    public NotificationService(AbsSender sender, BotConfig config) {
        this.sender = sender;
        this.config = config;
    }

    /** Оповещение в OPS-чат о новой заявке — БЕЗ Markdown/HTML */
    public void notifyNewOrder(Order order, EngineAttributes attrs) {
        String messageText = buildNewOrderMessagePlain(order, attrs);

        SendMessage message = new SendMessage();
        message.setChatId(config.getOpsChatId()); // сюда отправляем
        message.setText(messageText);

        try {
            logger.info("Sending OPS notification to chatId={}", config.getOpsChatId());
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send new order {} to OPS chatId {}: {}",
                    order.getPublicId(), config.getOpsChatId(), e.getMessage());

            // --- ПЛАН Б: отправим первому администратору в личку ---
            try {
                var admins = config.getAdminIds();
                if (admins != null && !admins.isEmpty()) {
                    Long adminId = admins.get(0);
                    SendMessage fallback = new SendMessage();
                    fallback.setChatId(adminId.toString());
                    fallback.setText("[FALLBACK]\n" + messageText);
                    sender.execute(fallback);
                    logger.info("Fallback sent to admin {}", adminId);
                } else {
                    logger.warn("Fallback skipped: no ADMIN_TG_IDS configured");
                }
            } catch (Exception ex) {
                logger.error("Fallback to admin failed for order {}: {}", order.getPublicId(), ex.getMessage());
            }
        }
    }


    /** Плоский текст без спец. разметки — безопасен для Telegram */
    private String buildNewOrderMessagePlain(Order order, EngineAttributes attrs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault());

        String createdAt = order.getCreatedAt() == null ? "-" : formatter.format(order.getCreatedAt());

        StringBuilder sb = new StringBuilder();
        sb.append("Новая заявка: ").append(nvl(order.getPublicId())).append('\n')
                .append('\n')
                .append("Дата: ").append(createdAt).append('\n')
                .append("Контакт: ").append(nvl(order.getCustomerName()))
                .append(", ").append(nvl(order.getCustomerPhone())).append('\n')
                .append("Доставка: ").append(nvl(order.getDeliveryCity())).append('\n')
                .append('\n')
                .append("Автомобиль: ").append(joinTrim(nvl(attrs.getMake()), nvl(attrs.getModel())))
                .append(attrs.getYear() == null ? "" : " (" + attrs.getYear() + ")").append('\n')
                .append("VIN: ").append(nvl(attrs.getVin())).append('\n')
                .append("Двигатель: ").append(nvl(attrs.getEngineCodeOrDetails())).append('\n')
                .append("Параметры: ").append(nvl(attrs.getFuelType()))
                .append("/")
                .append(Boolean.TRUE.equals(attrs.getIsTurbo()) ? "Turbo" : "NA").append('\n')
                .append("Комплект: ").append(nvl(attrs.getKitDetails()));

        return sb.toString();
    }

    private static String nvl(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String joinTrim(String a, String b) {
        String s = (Objects.toString(a, "").trim() + " " + Objects.toString(b, "").trim()).trim();
        return s.isEmpty() ? "-" : s;
    }
    public void notifyOpsPing() {
        SendMessage sm = new SendMessage();
        sm.setChatId(config.getOpsChatId());
        sm.setText("OPS ping OK");
        try {
            sender.execute(sm);
        } catch (TelegramApiException e) {
            logger.error("OPS ping failed for chatId {}: {}", config.getOpsChatId(), e.getMessage());
        }
    }
}
