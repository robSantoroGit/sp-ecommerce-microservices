package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.model.OrderStatus;

import java.math.BigDecimal;

/**
 * Event published when an order is created
 */
public class OrderCreatedEvent extends OrderEvent {

    private static final String EVENT_TYPE = "ORDER_CREATED";

    // Constructors
    public OrderCreatedEvent() {
        super();
    }

    public OrderCreatedEvent(Long orderId, Long userId, OrderStatus status, BigDecimal totalAmount) {
        super(orderId, userId, status, totalAmount);
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}