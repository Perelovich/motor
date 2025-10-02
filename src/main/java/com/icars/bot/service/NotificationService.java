package com.icars.bot.service;

import com.icars.bot.config.BotConfig;
import com.icars.bot.domain.EngineAttributes;
import com.icars.bot.domain.Order;
import com.icars.bot.util.Markdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final AbsSender sender;
    private final BotConfig config;

    public NotificationService(AbsSender sender, BotConfig config) {
        this.sender = sender;
        this.config = config;
    }

    public void notifyNewOrder(Order order, EngineAttributes attrs) {
        String messageText = buildNewOrderMessage(order, attrs);
        SendMessage message = new SendMessage(config.getOpsChatId(), messageText);
        message.setParseMode("MarkdownV2");
        // TODO: Add inline keyboard for quick actions (e.g., "Accept", "Need Info")

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send new order notification for order {}", order.getPublicId(), e);
        }
    }

    private String buildNewOrderMessage(Order order, EngineAttributes attrs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

        return "*Новая заявка: " + Markdown.escape(order.getPublicId()) + "*\n\n" +
                "*Дата:* " + formatter.format(order.getCreatedAt()) + "\n" +
                "*Контакт:* " + Markdown.escape(order.getCustomerName()) + ", `" + Markdown.escape(order.getCustomerPhone()) + "`\n" +
                "*Доставка:* " + Markdown.escape(order.getDeliveryCity()) + "\n\n" +
                "*Автомобиль:*\n" +
                " ` " + Markdown.escape(attrs.getMake() + " " + attrs.getModel() + " (" + attrs.getYear() + ")") + " `\n" +
                "*VIN:* `" + Markdown.escape(attrs.getVin()) + "`\n" +
                "*Двигатель:* " + Markdown.escape(attrs.getEngineCodeOrDetails()) + "\n" +
                "*Параметры:* " + Markdown.escape(attrs.getFuelType() + ", " + (attrs.getIsTurbo() ? "Турбо" : "Атмо")) + "\n" +
                "*Комплект:* " + Markdown.escape(attrs.getKitDetails());
    }
}
