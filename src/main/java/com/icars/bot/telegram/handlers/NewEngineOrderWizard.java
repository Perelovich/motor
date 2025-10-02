package com.icars.bot.telegram.handlers;

import com.icars.bot.config.BotConfig;
import com.icars.bot.domain.EngineAttributes;
import com.icars.bot.domain.Order;
import com.icars.bot.domain.OrderCategory;
import com.icars.bot.domain.OrderStatus;
import com.icars.bot.service.EngineOrderService;
import com.icars.bot.service.NotificationService;
import com.icars.bot.service.OrderService;
import com.icars.bot.service.ValidationService;
import com.icars.bot.telegram.keyboards.InlineKeyboards;
import com.icars.bot.telegram.keyboards.ReplyKeyboards;
import com.icars.bot.util.Markdown;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class NewEngineOrderWizard {
    private static final Logger logger = LoggerFactory.getLogger(NewEngineOrderWizard.class);

    private enum State {
        START, ASK_VIN, ASK_MAKE, ASK_MODEL, ASK_YEAR, ASK_ENGINE_CODE, ASK_FUEL_TURBO,
        ASK_INJECTION_EURO, ASK_KIT, ASK_CITY, ASK_CONTACT, PREVIEW, CONFIRMED
    }

    private final AbsSender sender;
    private final Jdbi jdbi;
    private final BotConfig config;
    private final Map<Long, String> userStates;
    private final Map<Long, Order> ordersInProgress = new ConcurrentHashMap<>();
    private final Map<Long, EngineAttributes> attributesInProgress = new ConcurrentHashMap<>();

    private final OrderService orderService;
    private final EngineOrderService engineOrderService;
    private final NotificationService notificationService;

    public NewEngineOrderWizard(AbsSender sender, Jdbi jdbi, BotConfig config, Map<Long, String> userStates) {
        this.sender = sender;
        this.jdbi = jdbi;
        this.config = config;
        this.userStates = userStates;
        this.orderService = new OrderService(jdbi);
        this.engineOrderService = new EngineOrderService(jdbi);
        this.notificationService = new NotificationService(sender, config);
    }

    private ResourceBundle getMessages(Update update) {
        // Basic language selection, defaulting to Russian
        String lang = "ru";
        User user = update.hasCallbackQuery() ? update.getCallbackQuery().getFrom() : update.getMessage().getFrom();
        if (user.getLanguageCode() != null && user.getLanguageCode().startsWith("en")) {
            lang = "en";
        }
        return ResourceBundle.getBundle("i18n.messages", new java.util.Locale(lang));
    }

    public void startWizard(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        ordersInProgress.put(chatId, new Order(null, null, userId, chatId, OrderCategory.ENGINE, OrderStatus.NEW, null, null, null, null, null));
        attributesInProgress.put(chatId, new EngineAttributes());

        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_VIN);
        ask(chatId, getMessages(update).getString("wizard.engine.ask_vin"), InlineKeyboards.skip(getMessages(update)));
    }

    public void handle(Update update) {
        long chatId = update.hasCallbackQuery() ? update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId();
        String stateStr = userStates.getOrDefault(chatId, "");
        if (!stateStr.startsWith("WIZARD_ENGINE_")) return;

        State currentState = State.valueOf(stateStr.replace("WIZARD_ENGINE_", ""));
        ResourceBundle messages = getMessages(update);

        try {
            switch (currentState) {
                case ASK_VIN: processVin(update, messages); break;
                case ASK_MAKE: processMake(update, messages); break;
                case ASK_MODEL: processModel(update, messages); break;
                case ASK_YEAR: processYear(update, messages); break;
                case ASK_ENGINE_CODE: processEngineCode(update, messages); break;
                case ASK_FUEL_TURBO: processFuelTurbo(update, messages); break;
                case ASK_INJECTION_EURO: processInjectionEuro(update, messages); break;
                case ASK_KIT: processKit(update, messages); break;
                case ASK_CITY: processCity(update, messages); break;
                case ASK_CONTACT: processContact(update, messages); break;
                case PREVIEW: processPreview(update, messages); break;
            }
        } catch (Exception e) {
            logger.error("Error in wizard state {}: {}", currentState, e.getMessage(), e);
            sendError(chatId, messages);
        }
    }

    private void processVin(Update update, ResourceBundle messages) {
        if (update.hasCallbackQuery() && "skip".equals(update.getCallbackQuery().getData())) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            attributesInProgress.get(chatId).setVin("-");
            userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_MAKE);
            editMessage(chatId, update.getCallbackQuery().getMessage().getMessageId(), messages.getString("wizard.engine.ask_make"));
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String vin = update.getMessage().getText().trim().toUpperCase();
            if (ValidationService.isValidVin(vin)) {
                attributesInProgress.get(chatId).setVin(vin);
                userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_MAKE);
                ask(chatId, messages.getString("wizard.engine.ask_make"), null);
            } else {
                ask(chatId, messages.getString("validation.error.vin"), InlineKeyboards.skip(messages));
            }
        }
    }

    // Implement other process... methods similarly
    private void processMake(Update update, ResourceBundle messages) {
        long chatId = update.getMessage().getChatId();
        String make = update.getMessage().getText().trim();
        attributesInProgress.get(chatId).setMake(make);
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_MODEL);
        ask(chatId, messages.getString("wizard.engine.ask_model"), null);
    }

    private void processModel(Update update, ResourceBundle messages) {
        long chatId = update.getMessage().getChatId();
        String model = update.getMessage().getText().trim();
        attributesInProgress.get(chatId).setModel(model);
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_YEAR);
        ask(chatId, messages.getString("wizard.engine.ask_year"), null);
    }

    private void processYear(Update update, ResourceBundle messages) {
        long chatId = update.getMessage().getChatId();
        String yearStr = update.getMessage().getText().trim();
        if (ValidationService.isValidYear(yearStr)) {
            attributesInProgress.get(chatId).setYear(Integer.parseInt(yearStr));
            userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_ENGINE_CODE);
            ask(chatId, messages.getString("wizard.engine.ask_engine_code"), null);
        } else {
            String errorMessage = MessageFormat.format(messages.getString("validation.error.year"), java.time.Year.now().getValue() + 1);
            ask(chatId, errorMessage, null);
        }
    }

    private void processEngineCode(Update update, ResourceBundle messages) {
        long chatId = update.getMessage().getChatId();
        attributesInProgress.get(chatId).setEngineCodeOrDetails(update.getMessage().getText().trim());
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_FUEL_TURBO);
        ask(chatId, messages.getString("wizard.engine.ask_fuel_turbo"), InlineKeyboards.fuelTurbo(messages));
    }

    private void processFuelTurbo(Update update, ResourceBundle messages) {
        if (!update.hasCallbackQuery()) return;
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String[] data = update.getCallbackQuery().getData().split("_");
        EngineAttributes attrs = attributesInProgress.get(chatId);
        attrs.setFuelType(data[0]);
        attrs.setIsTurbo("turbo".equals(data[1]));
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_INJECTION_EURO);
        editMessage(chatId, update.getCallbackQuery().getMessage().getMessageId(),
                messages.getString("wizard.engine.ask_injection_euro"), InlineKeyboards.injectionEuro(messages));
    }

    private void processInjectionEuro(Update update, ResourceBundle messages) {
        if (!update.hasCallbackQuery()) return;
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String[] data = update.getCallbackQuery().getData().split("_");
        EngineAttributes attrs = attributesInProgress.get(chatId);
        attrs.setInjectionType(data[0]);
        attrs.setEuroStandard(data[1]);
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_KIT);
        editMessage(chatId, update.getCallbackQuery().getMessage().getMessageId(), messages.getString("wizard.engine.ask_kit"));
    }

    private void processKit(Update update, ResourceBundle messages) {
        long chatId = update.getMessage().getChatId();
        attributesInProgress.get(chatId).setKitDetails(update.getMessage().getText().trim());
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_CITY);
        ask(chatId, messages.getString("wizard.engine.ask_city"), null);
    }

    private void processCity(Update update, ResourceBundle messages) {
        long chatId = update.getMessage().getChatId();
        ordersInProgress.get(chatId).setDeliveryCity(update.getMessage().getText().trim());
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_CONTACT);
        ask(chatId, messages.getString("wizard.engine.ask_contact"), ReplyKeyboards.requestContact(messages));
    }

    private void processContact(Update update, ResourceBundle messages) {
        if (!update.hasMessage()) return;
        long chatId = update.getMessage().getChatId();
        Order order = ordersInProgress.get(chatId);

        if (update.getMessage().hasContact()) {
            String phone = update.getMessage().getContact().getPhoneNumber();
            order.setCustomerPhone(phone.startsWith("+") ? phone : "+" + phone);
        } else {
            order.setCustomerName(update.getMessage().getText());
        }

        if (order.getCustomerName() != null && order.getCustomerPhone() != null) {
            userStates.put(chatId, "WIZARD_ENGINE_" + State.PREVIEW);
            showPreview(chatId, messages);
        }
    }

    private void processPreview(Update update, ResourceBundle messages) {
        if (!update.hasCallbackQuery()) return;
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        if ("confirm".equals(data)) {
            Order order = ordersInProgress.get(chatId);
            EngineAttributes attrs = attributesInProgress.get(chatId);

            try {
                String publicId = orderService.createOrder(order, attrs);
                userStates.remove(chatId);
                ordersInProgress.remove(chatId);
                attributesInProgress.remove(chatId);

                String confirmationText = MessageFormat.format(messages.getString("wizard.engine.confirmed"), publicId);
                editMessage(chatId, update.getCallbackQuery().getMessage().getMessageId(), confirmationText);

                // Fetch full order to send to admins
                Order fullOrder = orderService.findByPublicId(publicId).orElseThrow();
                notificationService.notifyNewOrder(fullOrder, attrs);

            } catch (Exception e) {
                logger.error("Failed to save order for chat {}", chatId, e);
                editMessage(chatId, update.getCallbackQuery().getMessage().getMessageId(), messages.getString("validation.error.general"));
            }

        } else if ("cancel".equals(data)) {
            userStates.remove(chatId);
            ordersInProgress.remove(chatId);
            attributesInProgress.remove(chatId);
            editMessage(chatId, update.getCallbackQuery().getMessage().getMessageId(), messages.getString("wizard.engine.cancelled"));
        }
    }

    private void showPreview(long chatId, ResourceBundle messages) {
        Order order = ordersInProgress.get(chatId);
        EngineAttributes attrs = attributesInProgress.get(chatId);

        String previewText = MessageFormat.format(messages.getString("wizard.engine.preview.body"),
                attrs.getMake(), attrs.getModel(), attrs.getYear(),
                attrs.getEngineCodeOrDetails(),
                attrs.getFuelType() + "/" + (attrs.getIsTurbo() ? "Turbo" : "NA"),
                attrs.getVin(),
                attrs.getKitDetails(),
                order.getDeliveryCity(),
                order.getCustomerName(),
                Markdown.escape(order.getCustomerPhone())
        );

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(messages.getString("wizard.engine.preview.title") + previewText);
        sm.setParseMode("MarkdownV2");
        sm.setReplyMarkup(InlineKeyboards.confirm(messages));
        try {
            sender.execute(sm);
        } catch (TelegramApiException e) {
            logger.error("Failed to send preview message to chat {}", chatId, e);
        }
    }


    private void ask(long chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send wizard message to chat {}", chatId, e);
        }
    }

    private void editMessage(long chatId, int messageId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(text);
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to edit message {} in chat {}", messageId, chatId, e);
        }
    }

    private void editMessage(long chatId, int messageId, String text) {
        editMessage(chatId, messageId, text, null);
    }

    private void sendError(long chatId, ResourceBundle messages) {
        ask(chatId, messages.getString("validation.error.general"), null);
        userStates.remove(chatId);
        ordersInProgress.remove(chatId);
        attributesInProgress.remove(chatId);
    }
}
