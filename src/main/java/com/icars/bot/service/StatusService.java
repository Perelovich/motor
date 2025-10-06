package com.icars.bot.service;

import com.icars.bot.domain.OrderStatus;
import com.icars.bot.domain.TimelineEvent;
import com.icars.bot.repo.OrderRepository;
import com.icars.bot.repo.TimelineRepository;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Objects;

public class StatusService {
    private final Jdbi jdbi;
    private final NotificationService notificationService; // оставим на будущее (можете использовать для уведомлений)

    public StatusService(Jdbi jdbi, NotificationService notificationService) {
        this.jdbi = jdbi;
        this.notificationService = notificationService;
    }

    /** Обновить статус заказа + записать событие в таймлайн (в транзакции) */
    public void updateStatus(long orderId, OrderStatus newStatus, String description) {
        jdbi.useTransaction(handle -> {
            OrderRepository orderRepo = handle.attach(OrderRepository.class);
            TimelineRepository timelineRepo = handle.attach(TimelineRepository.class);

            // 1) Обновляем статус заказа
            orderRepo.updateStatus(orderId, newStatus);

            // 2) Пишем событие в таймлайн (публичное)
            String desc = Objects.toString(description, "");
            TimelineEvent event = new TimelineEvent(orderId, newStatus, desc, true);
            timelineRepo.insert(event);

            // 3) (опционально) нотификация — включите, если нужно
            // if (notificationService != null) {
            //     orderRepo.findById(orderId).ifPresent(order ->
            //             notificationService.notifyStatusChange(order, newStatus, desc));
            // }
        });
    }

    /** Получить публичные события таймлайна для заказа */
    public List<TimelineEvent> getPublicTimeline(long orderId) {
        // Требует наличия метода в TimelineRepository:
        // List<TimelineEvent> findPublicEventsByOrderId(long orderId);
        return jdbi.withExtension(TimelineRepository.class,
                repo -> repo.findPublicEventsByOrderId(orderId));
    }
}
