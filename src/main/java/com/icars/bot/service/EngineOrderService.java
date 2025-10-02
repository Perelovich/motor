package com.icars.bot.service;

import com.icars.bot.domain.EngineAttributes;
import com.icars.bot.repo.EngineAttributesRepository;
import org.jdbi.v3.core.Jdbi;

import java.util.Optional;

public class EngineOrderService {
    private final Jdbi jdbi;

    public EngineOrderService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<EngineAttributes> getAttributesForOrder(long orderId) {
        return jdbi.withExtension(EngineAttributesRepository.class, repo -> repo.findByOrderId(orderId));
    }

    // Add validation logic if needed
    public boolean validate(EngineAttributes attributes) {
        if (attributes.getMake() == null || attributes.getMake().isEmpty()) {
            return false;
        }
        if (attributes.getModel() == null || attributes.getModel().isEmpty()) {
            return false;
        }
        if (!ValidationService.isValidYear(String.valueOf(attributes.getYear()))) {
            return false;
        }
        return true;
    }
}
