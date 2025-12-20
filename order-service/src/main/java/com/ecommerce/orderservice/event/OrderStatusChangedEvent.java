package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.model.OrderStatus;

import java.math.BigDecimal;

/**
 * Event published when an order status changes
 */
public class OrderStatusChangedEvent extends OrderEvent {

    private static final String EVENT_TYPE = "ORDER_STATUS_CHANGED";

    private OrderStatus previousStatus;

    // Constructors
    public OrderStatusChangedEvent() {
        super();
    }

    public OrderStatusChangedEvent(Long orderId, Long userId, OrderStatus previousStatus, 
                                   OrderStatus newStatus, BigDecimal totalAmount) {
        super(orderId, userId, newStatus, totalAmount);
        this.previousStatus = previousStatus;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    // Getter and Setter for previousStatus
    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(OrderStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    @Override
    public String toString() {
        return "OrderStatusChangedEvent{" +
                "orderId=" + getOrderId() +
                ", userId=" + getUserId() +
                ", previousStatus=" + previousStatus +
                ", newStatus=" + getStatus() +
                ", totalAmount=" + getTotalAmount() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}