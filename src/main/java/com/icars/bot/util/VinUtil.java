package com.icars.bot.util;

import java.util.regex.Pattern;

public class VinUtil {
    private static final Pattern VIN_CLEAN_PATTERN = Pattern.compile("[^A-HJ-NPR-Z0-9]");

    /**
     * Cleans and sanitizes a VIN string.
     * Removes invalid characters (like I, O, Q) and non-alphanumeric symbols.
     * Converts to uppercase.
     * @param rawVin The raw input VIN.
     * @return A sanitized VIN string.
     */
    public static String sanitize(String rawVin) {
        if (rawVin == null) {
            return "";
        }
        return VIN_CLEAN_PATTERN.matcher(rawVin.toUpperCase()).replaceAll("");
    }
}
