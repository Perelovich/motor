package com.icars.bot.service;

import java.time.Year;
import java.util.regex.Pattern;

public class ValidationService {

    // VIN should be 17 characters long, and contain only alphanumeric characters,
    // excluding I, O, and Q.
    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

    public static boolean isValidVin(String vin) {
        if (vin == null) {
            return false;
        }
        return VIN_PATTERN.matcher(vin.toUpperCase()).matches();
    }

    public static boolean isValidYear(String yearStr) {
        if (yearStr == null) {
            return false;
        }
        try {
            int year = Integer.parseInt(yearStr);
            int currentYear = Year.now().getValue();
            return year >= 1980 && year <= currentYear + 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // A simple check. For real validation, use a library like Google's libphonenumber.
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) {
            return false;
        }
        // E.164 format: + followed by 1 to 15 digits.
        return phone.matches("^\\+[1-9]\\d{1,14}$");
    }
}
