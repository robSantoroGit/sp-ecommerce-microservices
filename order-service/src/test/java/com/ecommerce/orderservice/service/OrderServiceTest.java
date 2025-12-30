package com.ecommerce.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.client.UserServiceClient;
import com.ecommerce.orderservice.dto.OrderItemRequestDTO;
import com.ecommerce.orderservice.dto.OrderItemResponseDTO;
import com.ecommerce.orderservice.dto.OrderMapper;
import com.ecommerce.orderservice.dto.OrderRequestDTO;
import com.ecommerce.orderservice.dto.OrderResponseDTO;
import com.ecommerce.orderservice.dto.PaymentRequestDTO;
import com.ecommerce.orderservice.dto.ProductDTO;
import com.ecommerce.orderservice.dto.SecurityContext;
import com.ecommerce.orderservice.dto.UserDTO;
import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.event.OrderStatusChangedEvent;
import com.ecommerce.orderservice.exception.ForbiddenException;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.InvalidOrderStatusException;
import com.ecommerce.orderservice.exception.OrderCancellationException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.ProductNotFoundException;
import com.ecommerce.orderservice.exception.UserNotFoundException;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.security.Permission;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;
    
    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderItem orderItem;
    private OrderRequestDTO orderRequestDTO;
    private OrderItemRequestDTO itemRequestDTO;
    private OrderResponseDTO orderResponseDTO;
    private UserDTO userDTO;
    private ProductDTO productDTO;
    
    private SecurityContext adminContext;
    private SecurityContext userContext;
    private SecurityContext otherUserContext;

    @BeforeEach
    void setUp() {
    	
    	// Setup SecurityContext
    	adminContext = new SecurityContext(999L, "admin", List.of(
	        Permission.ORDER_READ,
	        Permission.ORDER_WRITE,
	        Permission.ORDER_DELETE
	    ));
	    
	    userContext = new SecurityContext(1L, "user", List.of());
	    otherUserContext = new SecurityContext(99L, "otheruser", List.of());
    	
    	
        // Setup OrderItem
        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setProductId(1L);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("50.00"));
        orderItem.setSubtotal(new BigDecimal("100.00"));

        // Setup Order
        order = new Order();
        order.setId(1L);
        order.setUserId(userContext.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setDeliveryAddress("Test Address");
        order.setOrderDate(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.addItem(orderItem);

        // Setup DTOs
        itemRequestDTO = new OrderItemRequestDTO(1L, 2);

        orderRequestDTO = new OrderRequestDTO();
        orderRequestDTO.setDeliveryAddress("Test Address");
        orderRequestDTO.setItems(List.of(itemRequestDTO));

        OrderItemResponseDTO itemResponseDTO = new OrderItemResponseDTO();
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
        orderResponseDTO.setDeliveryAddress("Test Address");
        orderResponseDTO.setItems(List.of(itemResponseDTO));

        // Setup UserDTO
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");
        userDTO.setEmail("john@example.com");

        // Setup ProductDTO
        productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setName("Test Product");
        productDTO.setPrice(new BigDecimal("50.00"));
        productDTO.setStock(10);
        productDTO.setActive(true);
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        when(userServiceClient.validateUser(userContext.getUserId(), "")).thenReturn(userDTO);
        when(productServiceClient.validateProductAndStock(1L, 2, userContext.getUserId(), "")).thenReturn(productDTO);
        when(orderMapper.toEntity(orderRequestDTO)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDTO(order)).thenReturn(orderResponseDTO);
        //when(productServiceClient.getProduct(1L)).thenReturn(productDTO);

        // When
        OrderResponseDTO result = orderService.createOrder(orderRequestDTO, userContext);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        verify(userServiceClient).validateUser(anyLong(), any());
        verify(productServiceClient).validateProductAndStock(anyLong(), any(), any(), any());
        verify(productServiceClient).updateProductStock(1L, 8); // 10 - 2
        verify(orderRepository).save(any(Order.class));
        verify(kafkaProducerService).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        // Given
        when(userServiceClient.validateUser(999L, String.join(",", adminContext.getScopes()))).thenThrow(new UserNotFoundException(1L));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(orderRequestDTO, adminContext))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: 1");

        verify(userServiceClient).validateUser(anyLong(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenProductDoesNotExist() {
        // Given
        when(userServiceClient.validateUser(999L, String.join(",", adminContext.getScopes()))).thenReturn(userDTO);
        when(productServiceClient.validateProductAndStock(1L, 2, adminContext.getUserId(), String.join(",", adminContext.getScopes())))
                .thenThrow(new ProductNotFoundException(1L));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(orderRequestDTO, adminContext))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found with id: 1");

        verify(userServiceClient).validateUser(anyLong(), any());
        verify(productServiceClient).validateProductAndStock(anyLong(), any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowInsufficientStockExceptionWhenStockInsufficient() {
        // Given
        when(userServiceClient.validateUser(999L,String.join(",", adminContext.getScopes()))).thenReturn(userDTO);
        when(productServiceClient.validateProductAndStock(1L, 2, adminContext.getUserId(), String.join(",", adminContext.getScopes())))
                .thenThrow(new InsufficientStockException(1L, 2, 1));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(orderRequestDTO, adminContext))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        verify(userServiceClient).validateUser(anyLong(), any());
        verify(productServiceClient).validateProductAndStock(anyLong(), any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldGetOrderByIdSuccessfully() {
        // Given
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDTO(order)).thenReturn(orderResponseDTO);
        when(productServiceClient.getProduct(1L, userContext.getUserId(), "")).thenReturn(productDTO);

        // When
        OrderResponseDTO result = orderService.getOrderById(1L, userContext);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).findByIdWithItems(1L);
    }

    @Test
    void shouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {
        // Given
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(1L, adminContext))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with id: 1");
    }
    
    @Test
    void getOrdersByIdShouldThrowForbbidenException() {
        // Given
    	when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
    	
        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(1L, otherUserContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permission denied");
        
        verify(orderMapper, never()).toResponseDTO(any());
    }

    @Test
    void shouldGetOrdersByUserIdSuccessfully() {
        // Given
        List<Order> orders = List.of(order);
        when(userServiceClient.validateUser(userContext.getUserId(), "")).thenReturn(userDTO);
        when(orderRepository.findByUserIdWithItems(1L)).thenReturn(orders);
        when(orderMapper.toResponseDTOList(orders)).thenReturn(List.of(orderResponseDTO));
        when(productServiceClient.getProduct(1L, userContext.getUserId(), "")).thenReturn(productDTO);

        // When
        List<OrderResponseDTO> result = orderService.getOrdersByUserId(1L, userContext);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        verify(userServiceClient).validateUser(anyLong(), any());
        verify(orderRepository).findByUserIdWithItems(1L);
    }

    @Test
    void shouldGetOrdersByUserIdWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        
        when(userServiceClient.validateUser(userContext.getUserId(), "")).thenReturn(userDTO);
        when(orderRepository.findByUserId(1L, pageable)).thenReturn(orderPage);
        when(orderMapper.toResponseDTO(order)).thenReturn(orderResponseDTO);
        when(productServiceClient.getProduct(1L, userContext.getUserId(), "")).thenReturn(productDTO);

        // When
        Page<OrderResponseDTO> result = orderService.getOrdersByUserId(1L, pageable, userContext);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByUserId(1L, pageable);
    }
    
    @Test
    void shouldGetOrdersByUserIdWithPaginationThrowsForbiddenException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When & then
        assertThatThrownBy( () -> orderService.getOrdersByUserId(1L, pageable, otherUserContext))
        	.isInstanceOf(ForbiddenException.class)
        	.hasMessageContaining("Permission denied");

        // Then
        verify(userServiceClient, never()).validateUser(anyLong(), any());
        verify(orderRepository, never()).findByUserId(anyLong());
        verify(orderMapper, never()).toResponseDTO(any());
        verify(productServiceClient, never()).getProduct(anyLong(), any(), any());
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(orderResponseDTO);
        when(productServiceClient.getProduct(1L, userContext.getUserId(), "")).thenReturn(productDTO);

        // When
        OrderResponseDTO result = orderService.processPayment(1L, paymentRequest, userContext);

        // Then
        assertThat(result).isNotNull();
        
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
        verify(kafkaProducerService).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void shouldThrowInvalidOrderStatusExceptionWhenPaymentOnNonPendingOrder() {
        // Given
        order.setStatus(OrderStatus.CONFIRMED);
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.processPayment(1L, paymentRequest, userContext))
                .isInstanceOf(InvalidOrderStatusException.class);

        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void shouldProcessPaymentThrowsForbiddenException() {
        // Given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.processPayment(1L, null, otherUserContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permission denied");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldUpdateOrderStatusSuccessfully() {
        // Given
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDTO(any(Order.class))).thenReturn(orderResponseDTO);
        when(productServiceClient.getProduct(1L, userContext.getUserId(), "")).thenReturn(productDTO);

        // When
        OrderResponseDTO result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED, userContext);

        // Then
        assertThat(result).isNotNull();
        
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(kafkaProducerService).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void shouldThrowInvalidOrderStatusExceptionWhenInvalidTransition() {
        // Given
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED, adminContext))
                .isInstanceOf(InvalidOrderStatusException.class);

        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void shouldUpdateOrderStatusThrowForbiddenException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED, otherUserContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permission denied");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldCancelOrderSuccessfully() {
        // Given
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        when(productServiceClient.getProduct(1L, userContext.getUserId(), "")).thenReturn(productDTO);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.cancelOrder(1L, userContext);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(productServiceClient).updateProductStock(1L, 12); // 10 + 2 restored
        verify(kafkaProducerService).publishOrderCancelledEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void shouldThrowOrderCancellationExceptionWhenOrderNotCancellable() {
        // Given
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, adminContext))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("Cannot cancel order");

        verify(orderRepository, never()).save(any());
    }
    
    @Test
    void orderCancellationShouldThrowForbiddenException() {
        // Given
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, otherUserContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permission denied");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldGetAllOrdersSuccessfully() {
        // Given
        List<Order> orders = List.of(order);
        when(orderRepository.findAll()).thenReturn(orders);
        when(orderMapper.toResponseDTOList(orders)).thenReturn(List.of(orderResponseDTO));
        when(productServiceClient.getProduct(1L, adminContext.getUserId(), String.join(",", adminContext.getScopes()))).thenReturn(productDTO);

        // When
        List<OrderResponseDTO> result = orderService.getAllOrders(adminContext);

        // Then
        assertThat(result).hasSize(1);
        verify(orderRepository).findAll();
    }
    
    @Test
    void shouldGetAllOrdersThrowsForbiddenException() {
        // Given
        // nothing
    	
        // When & then
        assertThatThrownBy( () -> orderService.getAllOrders(otherUserContext))
        	.isInstanceOf(ForbiddenException.class)
        	.hasMessageContaining("Permission denied");

        // Then
        verify(orderRepository, never()).findAll();
    }

    @Test
    void shouldGetAllOrdersWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        
        when(orderRepository.findAll(pageable)).thenReturn(orderPage);
        when(orderMapper.toResponseDTO(order)).thenReturn(orderResponseDTO);
        when(productServiceClient.getProduct(1L, adminContext.getUserId(), String.join(",", adminContext.getScopes()))).thenReturn(productDTO);

        // When
        Page<OrderResponseDTO> result = orderService.getAllOrders(pageable, adminContext);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(pageable);
    }
    
    @Test
    void shouldGetAllOrderWithPaginationThrowsForbiddenException() {
        // Given
    	Pageable pageable = PageRequest.of(0, 10);
    	
        // When & then
        assertThatThrownBy( () -> orderService.getAllOrders(pageable, otherUserContext))
        	.isInstanceOf(ForbiddenException.class)
        	.hasMessageContaining("Permission denied");

        // Then
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }
    
}