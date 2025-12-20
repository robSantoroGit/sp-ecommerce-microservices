package com.ecommerce.orderservice.exception;

@SuppressWarnings("serial")
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Product not found with id: " + productId);
    }

    public ProductNotFoundException(String message) {
        super(message);
    }
}