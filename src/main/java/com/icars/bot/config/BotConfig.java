package com.icars.bot.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BotConfig {

    private final String token;
    private final String username;
    private final String opsChatId;
    private final List<Long> adminIds;

    public BotConfig() {
        this.token = getEnv("TG_BOT_TOKEN", "");
        this.username = getEnv("BOT_USERNAME", "ICarsPowertrainBot");
        this.opsChatId = getEnv("OPS_CHAT_ID", "");
        String adminIdsStr = getEnv("ADMIN_TG_IDS", "");

        if (token.isEmpty() || opsChatId.isEmpty() || adminIdsStr.isEmpty()) {
            throw new IllegalStateException("Critical environment variables TG_BOT_TOKEN, OPS_CHAT_ID, ADMIN_TG_IDS are not set.");
        }

        try {
            this.adminIds = Arrays.stream(adminIdsStr.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid format for ADMIN_TG_IDS. Must be a comma-separated list of numbers.", e);
        }
    }

    private String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getOpsChatId() {
        return opsChatId;
    }

    public List<Long> getAdminIds() {
        return Collections.unmodifiableList(adminIds);
    }

    public boolean isAdmin(Long userId) {
        return adminIds.contains(userId);
    }
}
