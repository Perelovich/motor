package com.icars.bot.domain;

public enum OrderStatus {
    NEW,
    NEED_INFO,
    QUOTING,
    WAITING_PREPAY,
    PURCHASED,
    IN_TRANSIT_CN,
    CUSTOMS,
    IN_TRANSIT,
    READY_FOR_PICKUP,
    DONE,
    CANCELLED;
}
