package com.ecommerce.orderservice.exception;

@SuppressWarnings("serial")
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("Order not found with id: " + id);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}