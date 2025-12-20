package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.model.OrderStatus;

@SuppressWarnings("serial")
public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(OrderStatus from, OrderStatus to) {
        super(String.format("Invalid status transition from %s to %s", from, to));
    }

    public InvalidOrderStatusException(String message) {
        super(message);
    }
}