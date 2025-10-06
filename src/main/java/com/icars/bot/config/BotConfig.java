package com.icars.bot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Загружает конфиг бота из переменных окружения/VM options.
 * - TG_BOT_TOKEN (обязателен)
 * - OPS_CHAT_ID (обязателен) — можно в .env без кавычек; если вдруг есть — снимем
 * - ADMIN_TG_IDS (обязателен) — CSV из числовых ID, например: 5208772935,692069524
 * - BOT_USERNAME (опц.) — дефолт ICarsPowertrainBot
 * - APP_LOCALE_DEFAULT (опц.) — ru или en; дефолт ru
 */
public class BotConfig {
    private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

    private final String token;
    private final String username;
    private final String opsChatId;   // строкой удобно для TelegramBots (поддержка @channel)
    private final Long opsChatIdLong; // для валидации/логов
    private final List<Long> adminIds;
    private final String defaultLocale;

    public BotConfig() {
        this.token        = req("TG_BOT_TOKEN");
        this.username     = opt("BOT_USERNAME", "ICarsPowertrainBot");
        this.opsChatId    = stripQuotes(req("OPS_CHAT_ID"));
        this.opsChatIdLong= parseLong("OPS_CHAT_ID", this.opsChatId);
        this.adminIds     = parseAdminIds(req("ADMIN_TG_IDS"));
        this.defaultLocale= opt("APP_LOCALE_DEFAULT", "ru");

        log.info("BotConfig loaded: username={}, opsChatId={}, admins={}, defaultLocale={}",
                username, opsChatIdLong, adminIds, defaultLocale);
    }

    /* ===== getters ===== */

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getOpsChatId() { return opsChatId; }
    public Long   getOpsChatIdAsLong() { return opsChatIdLong; }
    public List<Long> getAdminIds() { return adminIds; }
    public String getDefaultLocale() { return defaultLocale; }
    public boolean isAdmin(Long userId) { return userId != null && adminIds.contains(userId); }

    /* ===== helpers ===== */

    private static String req(String name) {
        String v = sanitize(System.getenv(name));
        if (isBlank(v)) v = sanitize(System.getProperty(name)); // поддержка -DTG_BOT_TOKEN=...
        if (isBlank(v)) throw new IllegalStateException("Required env var " + name + " is not set.");
        return v;
    }

    private static String opt(String name, String def) {
        String v = sanitize(System.getenv(name));
        if (isBlank(v)) v = sanitize(System.getProperty(name));
        return isBlank(v) ? def : v;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /** убираем BOM/пробелы/окаймляющие кавычки */
    private static String sanitize(String s) {
        if (s == null) return null;
        String t = s.replace("\uFEFF", "").trim();
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    private static String stripQuotes(String s) { return sanitize(s); }

    private static Long parseLong(String name, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(name + " must be a numeric chat id like -1001234567890, got: " + value, e);
        }
    }

    private static List<Long> parseAdminIds(String csv) {
        try {
            return Arrays.stream(csv.split(","))
                    .map(BotConfig::sanitize)
                    .filter(v -> !isBlank(v))
                    .map(Long::parseLong)
                    .distinct()
                    .collect(Collectors.toUnmodifiableList());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid ADMIN_TG_IDS. Use comma-separated numeric IDs, e.g. 111,222.", e);
        }
    }

    @Override public String toString() {
        String masked = token != null && token.length() > 10 ? token.substring(0, 6) + "..." : "***";
        return "BotConfig{token=" + masked + ", username=" + username +
                ", opsChatId=" + opsChatIdLong + ", admins=" + adminIds +
                ", defaultLocale=" + defaultLocale + "}";
    }
}
