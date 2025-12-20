package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Base class for all order-related events
 */
public abstract class OrderEvent {

    private Long orderId;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;

    // Constructor
    protected OrderEvent() {
        this.timestamp = LocalDateTime.now();
    }

    protected OrderEvent(Long orderId, Long userId, OrderStatus status, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.timestamp = LocalDateTime.now();
    }

    // Abstract method to get event type
    public abstract String getEventType();

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", timestamp=" + timestamp +
                '}';
    }
}