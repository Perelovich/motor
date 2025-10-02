package com.icars.bot.domain;

import java.time.Instant;

public class TimelineEvent {
    private Long id;
    private Long orderId;
    private Instant eventTimestamp;
    private OrderStatus eventStatus;
    private String description;
    private Boolean isPublic;

    public TimelineEvent() {}

    public TimelineEvent(Long orderId, OrderStatus eventStatus, String description, Boolean isPublic) {
        this.orderId = orderId;
        this.eventStatus = eventStatus;
        this.description = description;
        this.isPublic = isPublic;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    public OrderStatus getEventStatus() { return eventStatus; }
    public void setEventStatus(OrderStatus eventStatus) { this.eventStatus = eventStatus; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}
