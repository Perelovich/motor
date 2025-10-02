package com.icars.bot.domain;

import java.time.Instant;

public class Order {
    private Long id;
    private String publicId;
    private Long telegramUserId;
    private Long telegramChatId;
    private OrderCategory category;
    private OrderStatus status;
    private String customerName;
    private String customerPhone;
    private String deliveryCity;
    private Instant createdAt;
    private Instant updatedAt;

    public Order() {}

    public Order(Long id, String publicId, Long telegramUserId, Long telegramChatId, OrderCategory category,
                 OrderStatus status, String customerName, String customerPhone, String deliveryCity,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.publicId = publicId;
        this.telegramUserId = telegramUserId;
        this.telegramChatId = telegramChatId;
        this.category = category;
        this.status = status;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryCity = deliveryCity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public Long getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(Long telegramUserId) { this.telegramUserId = telegramUserId; }
    public Long getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(Long telegramChatId) { this.telegramChatId = telegramChatId; }
    public OrderCategory getCategory() { return category; }
    public void setCategory(OrderCategory category) { this.category = category; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getDeliveryCity() { return deliveryCity; }
    public void setDeliveryCity(String deliveryCity) { this.deliveryCity = deliveryCity; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
