package com.ecommerce.orderservice.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.orderservice.dto.OrderItemRequestDTO;
import com.ecommerce.orderservice.dto.OrderItemResponseDTO;
import com.ecommerce.orderservice.dto.OrderRequestDTO;
import com.ecommerce.orderservice.dto.OrderResponseDTO;
import com.ecommerce.orderservice.dto.OrderStatusUpdateDTO;
import com.ecommerce.orderservice.dto.PaymentRequestDTO;
import com.ecommerce.orderservice.dto.SecurityContext;
import com.ecommerce.orderservice.exception.ForbiddenException;
import com.ecommerce.orderservice.exception.GlobalExceptionHandler;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.InvalidOrderStatusException;
import com.ecommerce.orderservice.exception.OrderCancellationException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.UserNotFoundException;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.security.Permission;
import com.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(OrderController.class)
@Import({OrderController.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private OrderRequestDTO orderRequestDTO;
    private OrderResponseDTO orderResponseDTO;
    private OrderItemRequestDTO itemRequestDTO;
    private OrderItemResponseDTO itemResponseDTO;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules(); // For LocalDateTime serialization

        // Setup request DTO
        itemRequestDTO = new OrderItemRequestDTO(1L, 2);
        
        orderRequestDTO = new OrderRequestDTO();
        //orderRequestDTO.setUserId(1L);
        orderRequestDTO.setDeliveryAddress("Test Address, 123");
        orderRequestDTO.setItems(List.of(itemRequestDTO));

        // Setup response DTO
        itemResponseDTO = new OrderItemResponseDTO();
        itemResponseDTO.setId(1L);
        itemResponseDTO.setProductId(1L);
        itemResponseDTO.setProductName("Test Product");
        itemResponseDTO.setQuantity(2);
        itemResponseDTO.setUnitPrice(new BigDecimal("50.00"));
        itemResponseDTO.setSubtotal(new BigDecimal("100.00"));

        orderResponseDTO = new OrderResponseDTO();
        orderResponseDTO.setId(1L);
        orderResponseDTO.setUserId(1L);
        orderResponseDTO.setStatus(OrderStatus.PENDING);
        orderResponseDTO.setTotalAmount(new BigDecimal("100.00"));
        orderResponseDTO.setDeliveryAddress("Test Address, 123");
        orderResponseDTO.setOrderDate(LocalDateTime.now());
        orderResponseDTO.setCreatedAt(LocalDateTime.now());
        orderResponseDTO.setUpdatedAt(LocalDateTime.now());
        orderResponseDTO.setItems(List.of(itemResponseDTO));
    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given
        when(orderService.createOrder(any(OrderRequestDTO.class), any(SecurityContext.class))).thenReturn(orderResponseDTO);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDTO))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.deliveryAddress").value("Test Address, 123"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Test Product"));

        verify(orderService).createOrder(any(OrderRequestDTO.class), any(SecurityContext.class));
    }

    @Test
    void shouldReturnBadRequestWhenCreateOrderWithInvalidData() throws Exception {
        // Given - empty items list
        orderRequestDTO.setItems(List.of());

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDTO))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        // Given
        when(orderService.createOrder(any(OrderRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new UserNotFoundException(1L));

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDTO))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: 1"));
    }

    @Test
    void shouldReturnBadRequestWhenInsufficientStock() throws Exception {
        // Given
        when(orderService.createOrder(any(OrderRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new InsufficientStockException(1L, 10, 5));

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDTO))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));  
    }
    
    @Test
    void shouldReturnForbidden() throws Exception {
        // Given
        when(orderService.createOrder(any(OrderRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new ForbiddenException("Permission denied"));

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDTO))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", containsString("Permission denied")));  
    }


    @Test
    void shouldGetOrderByIdSuccessfully() throws Exception {
        // Given
        when(orderService.getOrderById(eq(1L),any(SecurityContext.class))).thenReturn(orderResponseDTO);

        // When & Then
        mockMvc.perform(get("/api/orders/1")
        		.header("X-User-Id", "1")
                .header("X-User-Scopes", Permission.ORDER_READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(1)));

        verify(orderService).getOrderById(any(),any());
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Given
        when(orderService.getOrderById(eq(999L), any(SecurityContext.class))).thenThrow(new OrderNotFoundException(999L));

        // When & Then
        mockMvc.perform(get("/api/orders/999")
        		.header("X-User-Id", "1")
                .header("X-User-Scopes", Permission.ORDER_READ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }
    
    @Test
    void getOrderByIdShouldReturnForbidden() throws Exception {
        // Given
        when(orderService.getOrderById(eq(99L), any(SecurityContext.class))).thenThrow(new ForbiddenException("Permission denied"));

        // When & Then
        mockMvc.perform(get("/api/orders/99")
        		.header("X-User-Id", "1")
                .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Permission denied"));
    }

    @Test
    void shouldGetOrdersByUserIdSuccessfully() throws Exception {
        // Given
        Page<OrderResponseDTO> page = new PageImpl<>(List.of(orderResponseDTO));
        when(orderService.getOrdersByUserId(eq(1L), any(PageRequest.class), any(SecurityContext.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/orders")
                        .param("userId", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService).getOrdersByUserId(eq(1L), any(PageRequest.class), any());
    }
    
    @Test
    void shouldGetOrdersByUserIdReturnsForbidden() throws Exception {
        // Given
    	when(orderService.getOrdersByUserId(eq(1L), any(PageRequest.class), any(SecurityContext.class)))
        	.thenThrow(new ForbiddenException("Permission denied"));        	

        // When & Then
        mockMvc.perform(get("/api/orders")
                        .param("userId", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Permission denied"));

        verify(orderService).getOrdersByUserId(eq(1L), any(PageRequest.class), any());
    }

    @Test
    void shouldGetAllOrdersWhenUserIdNotProvided() throws Exception {
        // Given
        Page<OrderResponseDTO> page = new PageImpl<>(List.of(orderResponseDTO));
        when(orderService.getAllOrders(any(PageRequest.class), any(SecurityContext.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService).getAllOrders(any(PageRequest.class), any(SecurityContext.class));
    }
    
    @Test
    void shouldGetAllOrdersReturnsForbidden() throws Exception {
        // Given
    	when(orderService.getAllOrders(any(PageRequest.class), any(SecurityContext.class)))
        	.thenThrow(new ForbiddenException("Permission denied"));        	

        // When & Then
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Permission denied"));

        verify(orderService).getAllOrders(any(PageRequest.class), any(SecurityContext.class));
    }

    @Test
    void shouldProcessPaymentSuccessfully() throws Exception {
        // Given
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234-5678");
        orderResponseDTO.setStatus(OrderStatus.PAID);
        when(orderService.processPayment(eq(1L), any(PaymentRequestDTO.class), any(SecurityContext.class)))
                .thenReturn(orderResponseDTO);

        // When & Then
        mockMvc.perform(post("/api/orders/1/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(orderService).processPayment(eq(1L), any(PaymentRequestDTO.class), any(SecurityContext.class));
    }

    @Test
    void shouldReturnBadRequestWhenPaymentOnInvalidStatus() throws Exception {
        // Given
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234-5678");
        when(orderService.processPayment(eq(1L), any(PaymentRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new InvalidOrderStatusException(OrderStatus.CONFIRMED, OrderStatus.PAID));

        // When & Then
        mockMvc.perform(post("/api/orders/1/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message",containsString("Invalid status transition")));
    }
    
    @Test
    void shouldPaymentReturnForbidden() throws Exception {
        // Given
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234-5678");
        when(orderService.processPayment(eq(1L), any(PaymentRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new ForbiddenException("Permission denied"));

        // When & Then
        mockMvc.perform(post("/api/orders/1/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message",containsString("Permission denied")));
    }

    @Test
    void shouldUpdateOrderStatusSuccessfully() throws Exception {
        // Given
        OrderStatusUpdateDTO statusUpdate = new OrderStatusUpdateDTO(OrderStatus.CONFIRMED);
        orderResponseDTO.setStatus(OrderStatus.CONFIRMED);
        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.CONFIRMED), any(SecurityContext.class))).thenReturn(orderResponseDTO);

        // When & Then
        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(orderService).updateOrderStatus(eq(1L), eq(OrderStatus.CONFIRMED), any());
    }

    @Test
    void shouldReturnBadRequestWhenInvalidStatusTransition() throws Exception {
        // Given
        OrderStatusUpdateDTO statusUpdate = new OrderStatusUpdateDTO(OrderStatus.SHIPPED);
        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.SHIPPED), any(SecurityContext.class)))
                .thenThrow(new InvalidOrderStatusException(OrderStatus.PENDING, OrderStatus.SHIPPED));

        // When & Then
        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", Permission.ORDER_WRITE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message",containsString("Invalid status transition")));
    }
    
    @Test
    void shouldUpdateOrderStatusReturnsForbidden() throws Exception {
        // Given
        OrderStatusUpdateDTO statusUpdate = new OrderStatusUpdateDTO(OrderStatus.SHIPPED);
        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.SHIPPED), any(SecurityContext.class)))
                .thenThrow(new ForbiddenException("Permission denied"));

        // When & Then
        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
				        .andExpect(status().isForbidden())
				        .andExpect(jsonPath("$.error").value("Forbidden"))
				        .andExpect(jsonPath("$.message",containsString("Permission denied")));
    }

    @Test
    void shouldCancelOrderSuccessfully() throws Exception {
        // Given
        doNothing().when(orderService).cancelOrder(eq(1L), any(SecurityContext.class));

        // When & Then
        mockMvc.perform(delete("/api/orders/1")
        		.header("X-User-Id", "1")
                .header("X-User-Scopes", Permission.ORDER_DELETE))
                .andExpect(status().isNoContent());

        verify(orderService).cancelOrder(eq(1L), any(SecurityContext.class));
    }

    @Test
    void shouldReturnBadRequestWhenOrderNotCancellable() throws Exception {
        // Given
        doThrow(new OrderCancellationException(1L, OrderStatus.DELIVERED))
                .when(orderService).cancelOrder(eq(1L), any(SecurityContext.class));

        // When & Then
        mockMvc.perform(delete("/api/orders/1")
        		.header("X-User-Id", "1")
                .header("X-User-Scopes", Permission.ORDER_DELETE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message",containsString("Cannot cancel order")));
    }
    
    @Test
    void shouldCancelOrderReturnsForbidden() throws Exception {
        // Given
        doThrow(new ForbiddenException("Permission denied"))
                .when(orderService).cancelOrder(eq(1L), any(SecurityContext.class));

        // When & Then
        mockMvc.perform(delete("/api/orders/1")
        		.header("X-User-Id", "1")
                .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden())
		        .andExpect(jsonPath("$.error").value("Forbidden"))
		        .andExpect(jsonPath("$.message",containsString("Permission denied")));
    }
}