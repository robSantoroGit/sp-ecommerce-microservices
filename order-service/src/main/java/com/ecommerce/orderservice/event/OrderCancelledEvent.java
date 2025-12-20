package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.model.OrderStatus;

import java.math.BigDecimal;

/**
 * Event published when an order is cancelled
 */
public class OrderCancelledEvent extends OrderEvent {

    private static final String EVENT_TYPE = "ORDER_CANCELLED";

    private String cancellationReason;

    // Constructors
    public OrderCancelledEvent() {
        super();
    }

    public OrderCancelledEvent(Long orderId, Long userId, OrderStatus status, 
                               BigDecimal totalAmount, String cancellationReason) {
        super(orderId, userId, status, totalAmount);
        this.cancellationReason = cancellationReason;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    // Getter and Setter for cancellationReason
    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    @Override
    public String toString() {
        return "OrderCancelledEvent{" +
                "orderId=" + getOrderId() +
                ", userId=" + getUserId() +
                ", status=" + getStatus() +
                ", totalAmount=" + getTotalAmount() +
                ", cancellationReason='" + cancellationReason + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}