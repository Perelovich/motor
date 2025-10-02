package com.icars.bot.util;

public class Markdown {

    /**
     * Escapes characters that are special in Telegram's MarkdownV2 format.
     * @param text The text to escape.
     * @return The escaped text, safe to use in a MarkdownV2 message.
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        // Characters to escape: _ * [ ] ( ) ~ ` > # + - = | { } . !
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}
