package com.icars.bot.util;

public class PhoneUtil {

    /**
     * Formats a phone number to E.164 standard.
     * Example: 89991234567 -> +79991234567
     * @param phone Raw phone number string.
     * @return Formatted phone number or the original string if formatting fails.
     */
    public static String formatE164(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11 && (digits.startsWith("7") || digits.startsWith("8"))) {
            return "+7" + digits.substring(1);
        }
        if (digits.length() > 10) { // For other countries, assume it's already okay
            return "+" + digits;
        }
        return phone; // Return original if unsure
    }
}
