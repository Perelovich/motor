package com.icars.bot.i18n;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

public class I18n {
    private final ResourceBundle bundle;

    public I18n(Locale locale) {
        // ваши файлы лежат в resources/i18n/messages_*.properties
        this.bundle = ResourceBundle.getBundle("i18n.messages", locale, new UTF8Control());
    }

    public String t(String key, Object... args) {
        String pattern = bundle.getString(key);
        return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    /** Загружает .properties как UTF-8 */
    public static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, java.io.IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            try (InputStream is = loader.getResourceAsStream(resourceName)) {
                if (is == null) return null;
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
        }
    }

    /** Удобный хелпер выбора локали с фолбэком */
    public static Locale resolve(String langCode, String defaultLang) {
        String lc = (langCode == null || langCode.isBlank()) ? defaultLang : langCode.toLowerCase(Locale.ROOT);
        // поддерживаем только ru/en, остальное — на дефолт
        if (!lc.startsWith("ru") && !lc.startsWith("en")) lc = defaultLang;
        return Locale.forLanguageTag(lc);
    }
}
