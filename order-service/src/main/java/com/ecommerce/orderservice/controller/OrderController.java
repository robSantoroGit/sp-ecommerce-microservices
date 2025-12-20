package com.ecommerce.orderservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.orderservice.dto.OrderRequestDTO;
import com.ecommerce.orderservice.dto.OrderResponseDTO;
import com.ecommerce.orderservice.dto.OrderStatusUpdateDTO;
import com.ecommerce.orderservice.dto.PaymentRequestDTO;
import com.ecommerce.orderservice.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Create a new order with items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "User or Product not found"),
            @ApiResponse(responseCode = "503", description = "External service unavailable")
    })
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderRequestDTO requestDTO) {
        OrderResponseDTO response = orderService.createOrder(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieve order details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        OrderResponseDTO response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get orders by user ID or all orders", 
               description = "Retrieve orders filtered by user ID (optional) with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Page<OrderResponseDTO>> getOrders(
            @Parameter(description = "User ID (optional)") @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<OrderResponseDTO> response;
        if (userId != null) {
            response = orderService.getOrdersByUserId(userId, pageable);
        } else {
            response = orderService.getAllOrders(pageable);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/payment")
    @Operation(summary = "Process payment for order", description = "Process payment and update order status to PAID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid order status for payment"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponseDTO> processPayment(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Valid @RequestBody PaymentRequestDTO paymentRequest) {
        OrderResponseDTO response = orderService.processPayment(id, paymentRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Update order status (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateDTO statusUpdate) {
        OrderResponseDTO response = orderService.updateOrderStatus(id, statusUpdate.getStatus());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel order", description = "Cancel order and restore product stock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}