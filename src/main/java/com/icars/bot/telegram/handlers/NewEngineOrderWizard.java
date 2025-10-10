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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.MessageFormat;
import java.time.Year;
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

    /* ======================== i18n ======================== */
    private ResourceBundle getMessages(Update update) {
        String lang = "ru";
        User user = update.hasCallbackQuery() ? update.getCallbackQuery().getFrom() : update.getMessage().getFrom();
        if (user != null && user.getLanguageCode() != null && user.getLanguageCode().startsWith("en")) {
            lang = "en";
        }
        return ResourceBundle.getBundle("i18n.messages", java.util.Locale.forLanguageTag(lang));
    }

    /* ======================== ENTRY ======================== */
    public void startWizard(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        ordersInProgress.put(chatId, new Order(
                null, null, userId, chatId,
                OrderCategory.ENGINE, OrderStatus.NEW,
                null, null, null, null, null
        ));
        attributesInProgress.put(chatId, new EngineAttributes());

        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_VIN);

        ResourceBundle msg = getMessages(update);

        // 1) Скрываем reply-клавиатуру отдельным сообщением
        SendMessage hide = new SendMessage(String.valueOf(chatId), " ");
        hide.setReplyMarkup(ReplyKeyboards.hide());
        try { sender.execute(hide); } catch (TelegramApiException ignored) {}

        // 2) Первый вопрос с инлайн «Пропустить»
        askInline(chatId, msg.getString("wizard.engine.ask_vin"), InlineKeyboards.skip(msg));
    }

    /* ======================== FSM ======================== */
    public void handle(Update update) {
        long chatId = update.hasCallbackQuery()
                ? update.getCallbackQuery().getMessage().getChatId()
                : update.getMessage().getChatId();

        String stateStr = userStates.getOrDefault(chatId, "");
        if (!stateStr.startsWith("WIZARD_ENGINE_")) return;

        State currentState = State.valueOf(stateStr.replace("WIZARD_ENGINE_", ""));
        ResourceBundle messages = getMessages(update);

        try {
            switch (currentState) {
                case ASK_VIN -> processVin(update, messages);
                case ASK_MAKE -> processMake(update, messages);
                case ASK_MODEL -> processModel(update, messages);
                case ASK_YEAR -> processYear(update, messages);
                case ASK_ENGINE_CODE -> processEngineCode(update, messages);
                case ASK_FUEL_TURBO -> processFuelTurbo(update, messages);
                case ASK_INJECTION_EURO -> processInjectionEuro(update, messages);
                case ASK_KIT -> processKit(update, messages);
                case ASK_CITY -> processCity(update, messages);
                case ASK_CONTACT -> processContact(update, messages);
                case PREVIEW -> processPreview(update, messages);
                default -> sendError(chatId, messages);
            }
        } catch (Exception e) {
            logger.error("Error in wizard state {}: {}", currentState, e.getMessage(), e);
            sendError(chatId, messages);
        }
    }

    /* ======================== STATES ======================== */

    private void processVin(Update update, ResourceBundle messages) {
        if (update.hasCallbackQuery() && "skip".equals(update.getCallbackQuery().getData())) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            attributesInProgress.get(chatId).setVin("-");
            userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_MAKE);
            editMessageInline(chatId, update.getCallbackQuery().getMessage().getMessageId(),
                    messages.getString("wizard.engine.ask_make"), null);
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String vin = update.getMessage().getText().trim().toUpperCase();
            if (ValidationService.isValidVin(vin)) {
                attributesInProgress.get(chatId).setVin(vin);
                userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_MAKE);
                askInline(chatId, messages.getString("wizard.engine.ask_make"), null);
            } else {
                askInline(chatId, messages.getString("validation.error.vin"), InlineKeyboards.skip(messages));
            }
        }
    }

    private void processMake(Update update, ResourceBundle messages) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        long chatId = update.getMessage().getChatId();
        String make = update.getMessage().getText().trim();
        attributesInProgress.get(chatId).setMake(make);
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_MODEL);
        askInline(chatId, messages.getString("wizard.engine.ask_model"), null);
    }

    private void processModel(Update update, ResourceBundle messages) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        long chatId = update.getMessage().getChatId();
        String model = update.getMessage().getText().trim();
        attributesInProgress.get(chatId).setModel(model);
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_YEAR);
        askInline(chatId, messages.getString("wizard.engine.ask_year"), null);
    }

    private void processYear(Update update, ResourceBundle messages) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        long chatId = update.getMessage().getChatId();
        String yearStr = update.getMessage().getText().trim();
        if (ValidationService.isValidYear(yearStr)) {
            attributesInProgress.get(chatId).setYear(Integer.parseInt(yearStr));
            userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_ENGINE_CODE);
            askInline(chatId, messages.getString("wizard.engine.ask_engine_code"), InlineKeyboards.skip(messages));
        } else {
            String errorMessage = MessageFormat.format(
                    messages.getString("validation.error.year"),
                    Year.now().getValue() + 1
            );
            ask(chatId, errorMessage, null);
        }
    }

    private void processEngineCode(Update update, ResourceBundle messages) {
        long chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            if ("skip".equals(data)) {
                attributesInProgress.get(chatId).setEngineCodeOrDetails(null);
            }
        } else {
            if (!update.hasMessage()) return;
            chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            attributesInProgress.get(chatId).setEngineCodeOrDetails(text == null ? null : text.trim());
        }
        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_FUEL_TURBO);
        askInline(chatId, messages.getString("wizard.engine.ask_fuel_turbo"), InlineKeyboards.fuelTurbo(messages));
    }

    private void processFuelTurbo(Update update, ResourceBundle messages) {
        if (!update.hasCallbackQuery()) return;
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        EngineAttributes attrs = attributesInProgress.get(chatId);
        if (attrs == null) { sendError(chatId, messages); return; }

        if ("skip".equals(data)) {
            attrs.setFuelType("-");
            attrs.setIsTurbo(false);
        } else {
            String[] parts = data.split("_");
            if (parts.length < 2) {
                logger.warn("Unexpected callback data at ASK_FUEL_TURBO: {}", data);
                editMessageInline(chatId, update.getCallbackQuery().getMessage().getMessageId(),
                        messages.getString("wizard.engine.ask_fuel_turbo"),
                        InlineKeyboards.fuelTurbo(messages));
                return;
            }
            attrs.setFuelType(parts[0]);
            attrs.setIsTurbo("turbo".equals(parts[1]));
        }

        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_INJECTION_EURO);
        editMessageInline(
                chatId,
                update.getCallbackQuery().getMessage().getMessageId(),
                messages.getString("wizard.engine.ask_injection_euro"),
                InlineKeyboards.injectionEuro(messages)
        );
    }

    private void processInjectionEuro(Update update, ResourceBundle messages) {
        if (!update.hasCallbackQuery()) return;
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        EngineAttributes attrs = attributesInProgress.get(chatId);
        if (attrs == null) { sendError(chatId, messages); return; }

        if ("skip".equals(data)) {
            attrs.setInjectionType("-");
            attrs.setEuroStandard("na");
        } else {
            String[] parts = data.split("_");
            if (parts.length < 2) {
                logger.warn("Unexpected callback data at ASK_INJECTION_EURO: {}", data);
                askInline(chatId, messages.getString("wizard.engine.ask_injection_euro"), InlineKeyboards.injectionEuro(messages));
                return;
            }
            attrs.setInjectionType(parts[0]);
            attrs.setEuroStandard(parts[1]);
        }

        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_KIT);
        editMessageInline(
                chatId,
                update.getCallbackQuery().getMessage().getMessageId(),
                messages.getString("wizard.engine.ask_kit"),
                InlineKeyboards.skip(messages)
        );
    }

    private void processKit(Update update, ResourceBundle messages) {
        long chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            if ("skip".equals(data)) {
                attributesInProgress.get(chatId).setKitDetails(null);
            }
        } else {
            if (!update.hasMessage()) return;
            chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            attributesInProgress.get(chatId).setKitDetails(text == null ? null : text.trim());
        }

        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_CITY);
        askInline(chatId, messages.getString("wizard.engine.ask_city"), InlineKeyboards.skip(messages));
    }

    private void processCity(Update update, ResourceBundle messages) {
        long chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            if ("skip".equals(data)) {
                ordersInProgress.get(chatId).setDeliveryCity("-");
            }
        } else {
            if (!update.hasMessage()) return;
            chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            ordersInProgress.get(chatId).setDeliveryCity(text == null ? "-" : text.trim());
        }

        userStates.put(chatId, "WIZARD_ENGINE_" + State.ASK_CONTACT);
        // одно сообщение: текст + REPLY "Отправить контакт"
        ask(chatId, messages.getString("wizard.engine.ask_contact"), ReplyKeyboards.requestContact(messages));
    }

    private void processContact(Update update, ResourceBundle messages) {
        if (!update.hasMessage()) return;

        long chatId = update.getMessage().getChatId();
        Order order = ordersInProgress.get(chatId);
        if (order == null) { sendError(chatId, messages); return; }

        String name = order.getCustomerName();
        String phone = order.getCustomerPhone();

        if (update.getMessage().hasContact()) {
            var c = update.getMessage().getContact();
            String raw = c.getPhoneNumber();
            String e164 = com.icars.bot.util.PhoneUtil.formatE164(raw);
            if (e164 == null || !ValidationService.isValidPhoneNumber(e164)) {
                ask(chatId, messages.getString("validation.error.phone"), ReplyKeyboards.requestContact(messages));
                return;
            }
            phone = e164;

            String full = ((c.getFirstName() == null ? "" : c.getFirstName()) +
                    (c.getLastName() == null ? "" : " " + c.getLastName())).trim();
            if (full.isBlank() && update.getMessage().getFrom() != null) {
                full = update.getMessage().getFrom().getFirstName();
            }
            if (!full.isBlank()) name = full;

        } else if (update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim();
            var m = java.util.regex.Pattern
                    .compile("^(?<name>[^,\\d+]{0,100})[,\\s]*?(?<phone>\\+?\\d[\\d\\s()\\-]{6,})$")
                    .matcher(text);
            if (m.find()) {
                String nm = m.group("name");
                String ph = m.group("phone");
                if (nm != null && !nm.trim().isBlank()) {
                    name = nm.trim();
                } else if (name == null || name.isBlank()) {
                    name = (update.getMessage().getFrom() != null ? update.getMessage().getFrom().getFirstName() : "Клиент");
                }
                String e164 = com.icars.bot.util.PhoneUtil.formatE164(ph);
                if (e164 == null || !ValidationService.isValidPhoneNumber(e164)) {
                    ask(chatId, messages.getString("validation.error.phone"), ReplyKeyboards.requestContact(messages));
                    return;
                }
                phone = e164;
            } else {
                String e164 = com.icars.bot.util.PhoneUtil.formatE164(text);
                if (e164 == null || !ValidationService.isValidPhoneNumber(e164)) {
                    ask(chatId, messages.getString("validation.error.phone"), ReplyKeyboards.requestContact(messages));
                    return;
                }
                phone = e164;
                if (name == null || name.isBlank()) {
                    name = (update.getMessage().getFrom() != null ? update.getMessage().getFrom().getFirstName() : "Клиент");
                }
            }
        }

        if (name == null || name.isBlank()) {
            name = (update.getMessage().getFrom() != null ? update.getMessage().getFrom().getFirstName() : "Клиент");
        }
        if (phone == null || !ValidationService.isValidPhoneNumber(phone)) {
            ask(chatId, messages.getString("validation.error.phone"), ReplyKeyboards.requestContact(messages));
            return;
        }

        order.setCustomerName(name);
        order.setCustomerPhone(phone);

        userStates.put(chatId, "WIZARD_ENGINE_" + State.PREVIEW);
        // СПРЯТАТЬ reply-клавиатуру "Отправить номер"
sendHideKeyboard(chatId, messages.getString("wizard.engine.contact_ok")); 
// напр. добавь в i18n: wizard.engine.contact_ok=✅ Контакт получен
        showPreview(chatId, messages);
    }

    private void processPreview(Update update, ResourceBundle messages) {
if ("confirm".equals(data)) {
    Order order = ordersInProgress.get(chatId);
    EngineAttributes attrs = attributesInProgress.get(chatId);

    try {
        String publicId = orderService.createOrder(order, attrs);

        // 1) Чистим состояние
        userStates.remove(chatId);
        ordersInProgress.remove(chatId);
        attributesInProgress.remove(chatId);

        // 2) Закрываем старое превью (редактируем то сообщение с инлайном)
        editMessagePlain(
                chatId,
                messageId,
                "Спасибо! Ваша заявка принята за номером " + publicId + ". Мы свяжемся с вами в ближайшее время."
        );

        // 3) На всякий случай — в отдельном сообщении убираем reply-клаву (если где-то осталась)
        sendHideKeyboard(chatId, messages.getString("wizard.engine.done.thanks"));

        // 4) Новое сообщение с пост-информацией + ГЛАВНОЕ МЕНЮ (reply-клава)
        SendMessage menu = new SendMessage();
        menu.setChatId(chatId);
        menu.setText(buildPostSubmitMessage(publicId));
        menu.setReplyMarkup(ReplyKeyboards.mainMenu(messages));
        sender.execute(menu);

        // 5) Уведомляем OPS
        Order fullOrder = orderService.findByPublicId(publicId).orElseThrow();
        notificationService.notifyNewOrder(fullOrder, attrs);

    } catch (Exception e) {
        logger.error("Failed to save order for chat {}", chatId, e);
        editMessagePlain(chatId, messageId, messages.getString("validation.error.general"));

        // Спрячем клаву и вернём юзера в меню
        sendHideKeyboard(chatId, null);
        SendMessage fallback = new SendMessage();
        fallback.setChatId(chatId);
        fallback.setText(messages.getString("welcome"));
        fallback.setReplyMarkup(ReplyKeyboards.mainMenu(messages));
        try { sender.execute(fallback); } catch (TelegramApiException ignored) {}
    }
    return;
}
    }

    /* ======================== VIEW helpers ======================== */

    private void showPreview(long chatId, ResourceBundle messages) {
        Order order = ordersInProgress.get(chatId);
        EngineAttributes attrs = attributesInProgress.get(chatId);

        String make  = Markdown.escape(nvl(attrs.getMake()));
        String model = Markdown.escape(nvl(attrs.getModel()));
        String year  = String.valueOf(attrs.getYear() == null ? "" : attrs.getYear());
        String engine = Markdown.escape(nvl(attrs.getEngineCodeOrDetails()));
        String fuel = Markdown.escape(nvl(attrs.getFuelType())) + "/" + (Boolean.TRUE.equals(attrs.getIsTurbo()) ? "Turbo" : "NA");
        String vin = Markdown.escape(nvl(attrs.getVin()));
        String kit = Markdown.escape(nvl(attrs.getKitDetails()));
        String city = Markdown.escape(nvl(order.getDeliveryCity()));
        String cname = Markdown.escape(nvl(order.getCustomerName()));
        String cphone = Markdown.escape(nvl(order.getCustomerPhone()));

        String previewBody = MessageFormat.format(
                messages.getString("wizard.engine.preview.body"),
                make, model, year, engine, fuel, vin, kit, city, cname, cphone
        );

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(messages.getString("wizard.engine.preview.title") + previewBody);
        sm.setParseMode("MarkdownV2");
        sm.setReplyMarkup(InlineKeyboards.confirm(messages));
        try { sender.execute(sm); }
        catch (TelegramApiException e) {
            logger.error("Failed to send preview message to chat {}", chatId, e);
            askInline(chatId, messages.getString("validation.error.general"), null);
        }
    }

    private static String nvl(String s) { return s == null ? "-" : s; }

    private void ask(long chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        try { sender.execute(message); }
        catch (TelegramApiException e) { logger.error("Failed to send wizard message to chat {}", chatId, e); }
    }

    private void askPlain(long chatId, String text) {
        SendMessage m = new SendMessage();
        m.setChatId(chatId);
        m.setText(text);
        try { sender.execute(m); }
        catch (TelegramApiException e) { logger.error("Failed to send plain message to chat {}", chatId, e); }
    }

    private void askInline(long chatId, String text, InlineKeyboardMarkup inlineKb) {
        SendMessage m = new SendMessage();
        m.setChatId(chatId);
        m.setText(text);
        if (inlineKb != null) m.setReplyMarkup(inlineKb);
        try { sender.execute(m); }
        catch (TelegramApiException e) { logger.error("Failed to send inline message to chat {}", chatId, e); }
    }

    private void editMessagePlain(long chatId, int messageId, String text,
                                  org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        EditMessageText msg = new EditMessageText();
        msg.setChatId(chatId);
        msg.setMessageId(messageId);
        msg.setText(text); // без parseMode!
        if (keyboard != null) msg.setReplyMarkup(keyboard);
        try { sender.execute(msg); }
        catch (TelegramApiException e) { logger.error("Failed to edit message {} in chat {}", messageId, chatId, e); }
    }

    private void editMessagePlain(long chatId, int messageId, String text) {
        editMessagePlain(chatId, messageId, text, null);
    }

    private void editMessageInline(long chatId, int messageId, String text,
                                   InlineKeyboardMarkup keyboard) {
        EditMessageText msg = new EditMessageText();
        msg.setChatId(chatId);
        msg.setMessageId(messageId);
        msg.setText(text);
        if (keyboard != null) msg.setReplyMarkup(keyboard);
        try { sender.execute(msg); }
        catch (TelegramApiException e) { logger.error("Failed to edit inline message {} in chat {}", messageId, chatId, e); }
    }

    private void editMessageInline(long chatId, int messageId, String text) {
        editMessageInline(chatId, messageId, text, null);
    }

    private void sendMenuAfterFlow(long chatId, String text, ResourceBundle messages) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(text);
        sm.setReplyMarkup(ReplyKeyboards.mainMenu(messages));
        try { sender.execute(sm); }
        catch (TelegramApiException e) { logger.error("Failed to send menu message to chat {}", chatId, e); }
    }

    private String buildPostSubmitMessage(String publicId) {
        return "✅ Заявка отправлена!\n\n" +
                "Номер: " + publicId + "\n" +
                "Что дальше: наш менеджер свяжется с вами в рабочее время, уточнит комплектацию и рассчитает срок/стоимость.\n\n" +
                "Как проверить статус:\n" +
                "— По номеру телефона или по номеру заявки через «Проверить статус» в меню.\n" +
                "— Команда: /status\n\n" +
                "Связь:\n" +
                "— Оперативно: +7 916 691-54-24 (Игорь) @icars_is\n" +
                "— Канал: i CARS PRO — t.me/i_cars_pro\n" +
                "— Двигатели: iCars Engine — t.me/icarsengine";
    }

    private void sendError(long chatId, ResourceBundle messages) {
        askInline(chatId, messages.getString("validation.error.general"), null);
        userStates.remove(chatId);
        ordersInProgress.remove(chatId);
        attributesInProgress.remove(chatId);
    }
    
// Спрятать любую reply-клавиатуру отдельным сообщением
private void sendHideKeyboard(long chatId, String text) {
    SendMessage hide = new SendMessage();
    hide.setChatId(chatId);
    hide.setText(text == null ? "" : text);
    hide.setReplyMarkup(ReplyKeyboards.hide());
    try {
        sender.execute(hide);
    } catch (TelegramApiException e) {
        logger.warn("Failed to hide reply keyboard in chat {}", chatId, e);
    }
}

}
