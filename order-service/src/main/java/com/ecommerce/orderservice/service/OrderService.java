package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderRequestDTO;
import com.ecommerce.orderservice.dto.OrderResponseDTO;
import com.ecommerce.orderservice.dto.PaymentRequestDTO;
import com.ecommerce.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    /**
     * Create a new order
     */
    OrderResponseDTO createOrder(OrderRequestDTO requestDTO);

    /**
     * Get order by ID
     */
    OrderResponseDTO getOrderById(Long id);

    /**
     * Get all orders by user ID
     */
    List<OrderResponseDTO> getOrdersByUserId(Long userId);

    /**
     * Get all orders by user ID with pagination
     */
    Page<OrderResponseDTO> getOrdersByUserId(Long userId, Pageable pageable);

    /**
     * Process payment for an order
     */
    OrderResponseDTO processPayment(Long orderId, PaymentRequestDTO paymentRequest);

    /**
     * Update order status
     */
    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus);

    /**
     * Cancel order (restore stock)
     */
    void cancelOrder(Long orderId);

    /**
     * Get all orders
     */
    List<OrderResponseDTO> getAllOrders();

    /**
     * Get all orders with pagination
     */
    Page<OrderResponseDTO> getAllOrders(Pageable pageable);
}