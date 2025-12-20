package com.ecommerce.orderservice.exception;

@SuppressWarnings("serial")
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, Integer requested, Integer available) {
        super(String.format("Insufficient stock for product %d. Requested: %d, Available: %d", 
                productId, requested, available));
    }

    public InsufficientStockException(String message) {
        super(message);
    }
}