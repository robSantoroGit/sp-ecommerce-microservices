package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.model.OrderStatus;

@SuppressWarnings("serial")
public class OrderCancellationException extends RuntimeException {

    public OrderCancellationException(Long orderId, OrderStatus currentStatus) {
        super(String.format("Cannot cancel order %d with status %s. Only PENDING or PAID orders can be cancelled.", 
                orderId, currentStatus));
    }

    public OrderCancellationException(String message) {
        super(message);
    }
}