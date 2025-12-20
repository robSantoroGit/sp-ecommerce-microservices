package com.ecommerce.orderservice.exception;

@SuppressWarnings("serial")
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found with id: " + userId);
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}