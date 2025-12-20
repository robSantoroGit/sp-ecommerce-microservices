package com.ecommerce.orderservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ecommerce.orderservice.bin.OrderServiceApplication;
import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.client.UserServiceClient;
import com.ecommerce.orderservice.dto.OrderItemRequestDTO;
import com.ecommerce.orderservice.dto.OrderRequestDTO;
import com.ecommerce.orderservice.dto.OrderResponseDTO;
import com.ecommerce.orderservice.dto.OrderStatusUpdateDTO;
import com.ecommerce.orderservice.dto.PaymentRequestDTO;
import com.ecommerce.orderservice.dto.ProductDTO;
import com.ecommerce.orderservice.dto.UserDTO;
import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.event.OrderStatusChangedEvent;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.KafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OrderServiceApplication.class)
@TestPropertySource(locations = "classpath:application-test.yml")
@ActiveProfiles("test")
public class OrderServiceIntegrationTest {

	@LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;
    
    @Value("${test.server.host:localhost}")  
    private String host;
    
    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private ProductServiceClient productServiceClient;
    
    @MockitoBean
    private KafkaProducerService kafkaProducerService;

    private UserDTO userDTO;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/orders";
        restTemplate = new TestRestTemplate(new RestTemplateBuilder().rootUri("http://" + host + ":" + port));
        orderRepository.deleteAll();
        objectMapper.findAndRegisterModules();

        // Setup mock UserDTO
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");
        userDTO.setEmail("john@example.com");

        // Setup mock ProductDTO
        productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setName("Test Product");
        productDTO.setPrice(new BigDecimal("50.00"));
        productDTO.setStock(10);
        productDTO.setActive(true);

        // Default mock behaviors
        when(userServiceClient.validateUser(anyLong())).thenReturn(userDTO);
        when(productServiceClient.validateProductAndStock(anyLong(), anyInt())).thenReturn(productDTO);
        when(productServiceClient.getProduct(anyLong())).thenReturn(productDTO);
        doNothing().when(productServiceClient).updateProductStock(anyLong(), anyInt());
        
        // Mock Kafka producer (no-op)
        doNothing().when(kafkaProducerService).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
        doNothing().when(kafkaProducerService).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
        doNothing().when(kafkaProducerService).publishOrderCancelledEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address, 123");
        requestDTO.setItems(List.of(itemDTO));

        // When
        ResponseEntity<OrderResponseDTO> response = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                OrderResponseDTO.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(1L);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getBody().getDeliveryAddress()).isEqualTo("Test Address, 123");
        assertThat(response.getBody().getItems()).hasSize(1);
        assertThat(response.getBody().getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        // Verify database
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);

        // Verify external service calls
        verify(userServiceClient).validateUser(1L);
        verify(productServiceClient).validateProductAndStock(1L, 2);
        verify(productServiceClient).updateProductStock(1L, 8); // 10 - 2
        verify(kafkaProducerService).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
    }
    
    @Test
    void shouldReturnBadRequestWhenCreateOrderWithEmptyItems() {
        // Given
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of());

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(orderRepository.findAll()).isEmpty();
        
        // Verify no external service calls
        verify(userServiceClient, never()).validateUser(anyLong());
        verify(productServiceClient, never()).validateProductAndStock(anyLong(), anyInt());
    }

    @Test
    void shouldReturnBadRequestWhenCreateOrderWithNullUserId() {
        // Given
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(null);
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    void shouldGetOrderByIdSuccessfully() {
        // Given - Create order first
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // When
        ResponseEntity<OrderResponseDTO> response = restTemplate.getForEntity(
                baseUrl + "/" + orderId,
                OrderResponseDTO.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(orderId);
        assertThat(response.getBody().getUserId()).isEqualTo(1L);
        assertThat(response.getBody().getItems()).hasSize(1);
        assertThat(response.getBody().getItems().get(0).getProductName()).isEqualTo("Test Product");
    }

    @Test
    void shouldReturnNotFoundWhenGetNonExistentOrder() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/999",
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetOrdersByUserIdSuccessfully() {
        // Given - Create 2 orders for user 1
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        
        OrderRequestDTO request1 = new OrderRequestDTO();
        request1.setUserId(1L);
        request1.setDeliveryAddress("Address 1");
        request1.setItems(List.of(itemDTO));
        restTemplate.postForEntity(baseUrl, request1, OrderResponseDTO.class);

        OrderRequestDTO request2 = new OrderRequestDTO();
        request2.setUserId(1L);
        request2.setDeliveryAddress("Address 2");
        request2.setItems(List.of(itemDTO));
        restTemplate.postForEntity(baseUrl, request2, OrderResponseDTO.class);

        // Create 1 order for user 2
        UserDTO user2 = new UserDTO();
        user2.setId(2L);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setEmail("jane@example.com");
        when(userServiceClient.validateUser(2L)).thenReturn(user2);

        OrderRequestDTO request3 = new OrderRequestDTO();
        request3.setUserId(2L);
        request3.setDeliveryAddress("Address 3");
        request3.setItems(List.of(itemDTO));
        restTemplate.postForEntity(baseUrl, request3, OrderResponseDTO.class);

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "?userId=1&page=0&size=10",
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalElements\":2");
        assertThat(response.getBody()).contains("\"userId\":1");
        
        verify(userServiceClient, atLeast(1)).validateUser(1L);
    }

    @Test
    void shouldGetAllOrdersSuccessfully() {
        // Given - Create 3 orders
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        
        for (int i = 1; i <= 3; i++) {
            OrderRequestDTO request = new OrderRequestDTO();
            request.setUserId((long) i);
            request.setDeliveryAddress("Address " + i);
            request.setItems(List.of(itemDTO));
            
            // Setup user mock for each user
            if (i > 1) {
                UserDTO user = new UserDTO();
                user.setId((long) i);
                user.setFirstName("User" + i);
                user.setLastName("Test");
                user.setEmail("user" + i + "@example.com");
                when(userServiceClient.validateUser((long) i)).thenReturn(user);
            }
            
            restTemplate.postForEntity(baseUrl, request, OrderResponseDTO.class);
        }

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "?page=0&size=10",
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalElements\":3");
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given - Create order
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234-5678");

        // When
        ResponseEntity<OrderResponseDTO> response = restTemplate.postForEntity(
                baseUrl + "/" + orderId + "/payment",
                paymentRequest,
                OrderResponseDTO.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PAID);

        // Verify database
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(kafkaProducerService).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void shouldUpdateOrderStatusSuccessfully() {
        // Given - Create and pay order
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // Pay order first (PENDING -> PAID)
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234");
        restTemplate.postForEntity(
                baseUrl + "/" + orderId + "/payment",
                paymentRequest,
                OrderResponseDTO.class
        );

        // When - Update to CONFIRMED
        OrderStatusUpdateDTO statusUpdate = new OrderStatusUpdateDTO(OrderStatus.CONFIRMED);
        HttpEntity<OrderStatusUpdateDTO> request = new HttpEntity<>(statusUpdate);
        
        ResponseEntity<OrderResponseDTO> response = restTemplate.exchange(
                baseUrl + "/" + orderId + "/status",
                HttpMethod.PUT,
                request,
                OrderResponseDTO.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Verify database
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(kafkaProducerService,times(2)).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void shouldReturnBadRequestWhenInvalidStatusTransition() {
        // Given - Create order (PENDING)
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // When - Try invalid transition PENDING -> SHIPPED
        OrderStatusUpdateDTO statusUpdate = new OrderStatusUpdateDTO(OrderStatus.SHIPPED);
        HttpEntity<OrderStatusUpdateDTO> request = new HttpEntity<>(statusUpdate);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + orderId + "/status",
                HttpMethod.PUT,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Invalid status transition");
    }

    @Test
    void shouldCancelOrderSuccessfully() {
        // Given - Create order
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + orderId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify database
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        
        // Verify stock restoration
        verify(productServiceClient).getProduct(1L);
        verify(productServiceClient).updateProductStock(1L, 12); // 10 + 2 restored
        verify(kafkaProducerService).publishOrderCancelledEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void shouldReturnBadRequestWhenCancelNonCancellableOrder() {
        // Given - Create order and mark as DELIVERED
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestDTO,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // Update order to DELIVERED manually
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + orderId,
                HttpMethod.DELETE,
                null,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Cannot cancel order");
                
    }
}