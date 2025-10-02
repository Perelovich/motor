package com.icars.bot.service;

import com.icars.bot.domain.OrderStatus;
import com.icars.bot.domain.TimelineEvent;
import com.icars.bot.repo.OrderRepository;
import com.icars.bot.repo.TimelineRepository;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

public class StatusService {
    private final Jdbi jdbi;
    private final NotificationService notificationService;

    public StatusService(Jdbi jdbi, NotificationService notificationService) {
        this.jdbi = jdbi;
        this.notificationService = notificationService;
    }

    public void updateStatus(long orderId, OrderStatus newStatus, String description) {
        jdbi.useTransaction(handle -> {
            OrderRepository orderRepo = handle.attach(OrderRepository.class);
            TimelineRepository timelineRepo = handle.attach(TimelineRepository.class);

            // 1. Update the status in the orders table
            orderRepo.updateStatus(orderId, newStatus);

            // 2. Add a new event to the timeline
            TimelineEvent event = new TimelineEvent(orderId, newStatus, description, true);
            timelineRepo.insert(event);

            // 3. Notify the user (if notifier is configured)
            // if (notificationService != null) {
            //     orderRepo.findById(orderId).ifPresent(order ->
            //         notificationService.notifyStatusChange(order, newStatus, description));
            // }
        });
    }

    public List<TimelineEvent> getPublicTimeline(long orderId) {
        return jdbi.withExtension(TimelineRepository.class, repo -> repo.findPublicEventsByOrderId(orderId));
    }
}
