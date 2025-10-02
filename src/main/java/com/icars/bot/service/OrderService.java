package com.icars.bot.service;

import com.icars.bot.domain.EngineAttributes;
import com.icars.bot.domain.Order;
import com.icars.bot.repo.EngineAttributesRepository;
import com.icars.bot.repo.OrderRepository;
import com.icars.bot.repo.TimelineRepository;
import org.jdbi.v3.core.Jdbi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class OrderService {
    private final Jdbi jdbi;

    public OrderService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public String createOrder(Order order, EngineAttributes attributes) {
        return jdbi.inTransaction(handle -> {
            OrderRepository orderRepo = handle.attach(OrderRepository.class);
            EngineAttributesRepository attrRepo = handle.attach(EngineAttributesRepository.class);
            TimelineRepository timelineRepo = handle.attach(TimelineRepository.class);

            // 1. Generate Public ID
            int todayCount = orderRepo.countToday();
            String publicId = generatePublicId(todayCount + 1);
            order.setPublicId(publicId);

            // 2. Insert Order and get ID
            long orderId = orderRepo.insert(order);
            order.setId(orderId);
            attributes.setOrderId(orderId);

            // 3. Insert Attributes
            attrRepo.insert(attributes);

            // 4. Create initial timeline event
            timelineRepo.insert(new com.icars.bot.domain.TimelineEvent(orderId, order.getStatus(), "Заявка создана", true));

            return publicId;
        });
    }

    private String generatePublicId(int sequence) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        return String.format("IC-%s-%03d", today.format(formatter), sequence);
    }

    public Optional<Order> findByPublicId(String publicId) {
        return jdbi.withExtension(OrderRepository.class, repo -> repo.findByPublicId(publicId));
    }

    public Optional<Order> findLastByPhone(String phone) {
        return jdbi.withExtension(OrderRepository.class, repo -> repo.findLastByPhone(phone));
    }

    public List<Order> findLastOrders(int limit) {
        return jdbi.withExtension(OrderRepository.class, repo -> repo.findLast(limit));
    }

    public List<Order> searchOrders(String query) {
        return jdbi.withExtension(OrderRepository.class, repo -> repo.search("%" + query + "%"));
    }
}
