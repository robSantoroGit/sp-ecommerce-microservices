package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class OrderStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    // Constructors
    public OrderStatusUpdateDTO() {
    }

    public OrderStatusUpdateDTO(OrderStatus status) {
        this.status = status;
    }

    // Getters and Setters
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}