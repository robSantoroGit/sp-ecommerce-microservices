package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderRequestDTO;
import com.ecommerce.orderservice.dto.OrderResponseDTO;
import com.ecommerce.orderservice.dto.PaymentRequestDTO;
import com.ecommerce.orderservice.dto.SecurityContext;
import com.ecommerce.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    /**
     * Create a new order
     */
    OrderResponseDTO createOrder(OrderRequestDTO requestDTO, SecurityContext securityContext);

    /**
     * Get order by ID
     */
    OrderResponseDTO getOrderById(Long id, SecurityContext securityContext);

    /**
     * Get all orders by user ID
     */
    List<OrderResponseDTO> getOrdersByUserId(Long userId, SecurityContext securityContext);

    /**
     * Get all orders by user ID with pagination
     */
    Page<OrderResponseDTO> getOrdersByUserId(Long userId, Pageable pageable, SecurityContext securityContext);

    /**
     * Process payment for an order
     */
    OrderResponseDTO processPayment(Long orderId, PaymentRequestDTO paymentRequest, SecurityContext securityContext);

    /**
     * Update order status
     */
    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus, SecurityContext securityContext);

    /**
     * Cancel order (restore stock)
     */
    void cancelOrder(Long orderId, SecurityContext securityContext);

    /**
     * Get all orders
     */
    List<OrderResponseDTO> getAllOrders(SecurityContext securityContext);

    /**
     * Get all orders with pagination
     */
    Page<OrderResponseDTO> getAllOrders(Pageable pageable, SecurityContext securityContext);
}