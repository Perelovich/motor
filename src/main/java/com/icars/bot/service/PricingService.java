package com.icars.bot.service;

import com.icars.bot.domain.EngineAttributes;
import com.icars.bot.domain.Order;

public class PricingService {

    /**
     * Stub method for estimating the price.
     * In a real application, this would involve complex logic, API calls,
     * or database lookups based on the order details.
     * @param order The main order details.
     * @param attributes The engine-specific attributes.
     * @return A string representing the estimated price range.
     */
    public String estimatePrice(Order order, EngineAttributes attributes) {
        // TODO: Implement actual pricing logic.
        // For now, return a placeholder based on some simple rules.
        int basePrice = 80000;
        if (attributes.getIsTurbo() != null && attributes.getIsTurbo()) {
            basePrice += 20000;
        }
        if (attributes.getYear() != null && attributes.getYear() > 2018) {
            basePrice += 15000;
        }
        return String.format("от %d до %d руб.", basePrice, basePrice + 30000);
    }
}
