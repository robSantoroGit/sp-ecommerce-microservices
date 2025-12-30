package com.ecommerce.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    @PostMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        return buildFallbackResponse("User Service");
    }

    @GetMapping("/product-service")
    @PostMapping("/product-service")
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        return buildFallbackResponse("Product Service");
    }

    @GetMapping("/order-service")
    @PostMapping("/order-service")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        return buildFallbackResponse("Order Service");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String serviceName) {
        Map<String, Object> response = Map.of(
            "error", "Service Unavailable",
            "message", serviceName + " is currently unavailable. Please try again later.",
            "timestamp", LocalDateTime.now(),
            "status", 503
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}