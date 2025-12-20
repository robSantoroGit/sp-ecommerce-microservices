package com.ecommerce.orderservice.exception;

@SuppressWarnings("serial")
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String serviceName, String message) {
        super(String.format("Error calling %s: %s", serviceName, message));
    }

    public ExternalServiceException(String serviceName, Throwable cause) {
        super(String.format("Error calling %s: %s", serviceName, cause.getMessage()), cause);
    }
}