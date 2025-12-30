package com.ecommerce.orderservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.ecommerce.orderservice.security.Permission;
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
        when(userServiceClient.validateUser(anyLong(), anyString())).thenReturn(userDTO);
        when(productServiceClient.validateProductAndStock(anyLong(), anyInt(), anyLong(), anyString())).thenReturn(productDTO);
        when(productServiceClient.getProduct(anyLong(), anyLong(), anyString())).thenReturn(productDTO);
        doNothing().when(productServiceClient).updateProductStock(anyLong(), anyInt());
        
        // Mock Kafka producer (no-op)
        doNothing().when(kafkaProducerService).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
        doNothing().when(kafkaProducerService).publishOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
        doNothing().when(kafkaProducerService).publishOrderCancelledEvent(any(OrderCancelledEvent.class));
    }
    
    // Crea HttpHeaders helper nel setUp()
    private HttpHeaders createHeaders(Long userId, String scopes) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        headers.set("X-User-Scopes", scopes);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        //requestDTO.setUserId(1L);
        requestDTO.setDeliveryAddress("Test Address, 123");
        requestDTO.setItems(List.of(itemDTO));
        
        HttpHeaders headers = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> request = new HttpEntity<OrderRequestDTO>(requestDTO,headers);

        // When
        ResponseEntity<OrderResponseDTO> response = restTemplate.postForEntity(
                baseUrl,
                request,
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
        verify(userServiceClient).validateUser(1L,"");
        verify(productServiceClient).validateProductAndStock(1L, 2, 1L, "");
        verify(productServiceClient).updateProductStock(1L, 8); // 10 - 2
        verify(kafkaProducerService).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
    }
    
    @Test
    void shouldReturnBadRequestWhenCreateOrderWithEmptyItems() {
        // Given
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of());
        
        HttpHeaders headers = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> request = new HttpEntity<OrderRequestDTO>(requestDTO,headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(orderRepository.findAll()).isEmpty();
        
        // Verify no external service calls
        verify(userServiceClient, never()).validateUser(anyLong(), anyString());
        verify(productServiceClient, never()).validateProductAndStock(anyLong(), anyInt(), anyLong(), anyString());
    }

    @Test
    void shouldReturnBadRequestWhenCreateOrderWithNullUserId() {
        // Given
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("pippo","pluto");

        HttpEntity<OrderRequestDTO> request = new HttpEntity<OrderRequestDTO>(requestDTO,headers );
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                request,
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
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        HttpHeaders headers = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> requestForPost = new HttpEntity<OrderRequestDTO>(requestDTO,headers);
        
        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestForPost,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        
        HttpEntity<Void> requestForGet = new HttpEntity<Void>(headers);
        // When
        ResponseEntity<OrderResponseDTO> response = restTemplate.exchange(
                baseUrl + "/" + orderId,
                HttpMethod.GET,
                requestForGet,
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
        // Given
    	HttpHeaders headers = createHeaders(1L,"");
    	HttpEntity<Void> requestForGet = new HttpEntity<Void>(headers);
    	
    	// When
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/999",
                HttpMethod.GET,
                requestForGet,
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
        request1.setDeliveryAddress("Address 1");
        request1.setItems(List.of(itemDTO));
        
        HttpHeaders headers1 = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> requestForPost1 = new HttpEntity<OrderRequestDTO>(request1,headers1);
        restTemplate.postForEntity(baseUrl, requestForPost1, OrderResponseDTO.class);

        OrderRequestDTO request2 = new OrderRequestDTO();
        request2.setDeliveryAddress("Address 2");
        request2.setItems(List.of(itemDTO));
        HttpEntity<OrderRequestDTO> requestForPost2 = new HttpEntity<OrderRequestDTO>(request2,headers1);
        restTemplate.postForEntity(baseUrl, requestForPost2, OrderResponseDTO.class);

        // Create 1 order for user 2
        UserDTO user2 = new UserDTO();
        user2.setId(2L);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setEmail("jane@example.com");
        when(userServiceClient.validateUser(2L,"")).thenReturn(user2);

        OrderRequestDTO request3 = new OrderRequestDTO();
        request3.setDeliveryAddress("Address 3");
        request3.setItems(List.of(itemDTO));
        HttpHeaders headers2 = createHeaders(2L,"");
        HttpEntity<OrderRequestDTO> requestForPost3 = new HttpEntity<OrderRequestDTO>(request3,headers2);
        
        restTemplate.postForEntity(baseUrl, requestForPost3, OrderResponseDTO.class);

        // When
        HttpEntity<Void> requestForGet = new HttpEntity<Void>(headers1);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?userId=1&page=0&size=10",
                HttpMethod.GET,
                requestForGet,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalElements\":2");
        assertThat(response.getBody()).contains("\"userId\":1");
        
        verify(userServiceClient, atLeast(1)).validateUser(1L,"");
    }

    @Test
    void shouldGetAllOrdersSuccessfully() {
        // Given - Create 3 orders
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        
        for (int i = 1; i <= 3; i++) {
            OrderRequestDTO request = new OrderRequestDTO();
            request.setDeliveryAddress("Address " + i);
            request.setItems(List.of(itemDTO));
            
            // Setup user mock for each user
            if (i > 1) {
                UserDTO user = new UserDTO();
                user.setId((long) i);
                user.setFirstName("User" + i);
                user.setLastName("Test");
                user.setEmail("user" + i + "@example.com");
                when(userServiceClient.validateUser((long) i, "")).thenReturn(user);
            }
            HttpHeaders headers = createHeaders((long) i,"");
            HttpEntity<OrderRequestDTO> requestForPost = new HttpEntity<OrderRequestDTO>(request,headers);
            restTemplate.postForEntity(baseUrl, requestForPost, OrderResponseDTO.class);
        }

        // When
        HttpHeaders headersForGet = createHeaders(999L,Permission.ORDER_READ);
        HttpEntity<Void> requestForGet = new HttpEntity<Void>(headersForGet);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?page=0&size=10",
                HttpMethod.GET,
                requestForGet,
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
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        HttpHeaders headers1 = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> requestForPost1 = new HttpEntity<OrderRequestDTO>(requestDTO,headers1);
        
        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestForPost1,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234-5678");

        // When
        HttpEntity<PaymentRequestDTO> requestForPost2 = new HttpEntity<PaymentRequestDTO>(paymentRequest,headers1);
        ResponseEntity<OrderResponseDTO> response = restTemplate.postForEntity(
                baseUrl + "/" + orderId + "/payment",
                requestForPost2,
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
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        HttpHeaders headers1 = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> requestForPost1 = new HttpEntity<OrderRequestDTO>(requestDTO,headers1);
        
        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestForPost1,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // Pay order first (PENDING -> PAID)
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO("CREDIT_CARD", "1234");
        HttpEntity<PaymentRequestDTO> requestForPost2 = new HttpEntity<PaymentRequestDTO>(paymentRequest,headers1);
        restTemplate.postForEntity(
                baseUrl + "/" + orderId + "/payment",
                requestForPost2,
                OrderResponseDTO.class
        );

        // When - Update to CONFIRMED
        OrderStatusUpdateDTO statusUpdate = new OrderStatusUpdateDTO(OrderStatus.CONFIRMED);
        HttpEntity<OrderStatusUpdateDTO> request = new HttpEntity<>(statusUpdate,headers1);
        
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
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        HttpHeaders headers1 = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> requestForPost1 = new HttpEntity<OrderRequestDTO>(requestDTO,headers1);
        
        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestForPost1,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // When - Try invalid transition PENDING -> SHIPPED
        OrderStatusUpdateDTO statusUpdate = new OrderStatusUpdateDTO(OrderStatus.SHIPPED);
        HttpEntity<OrderStatusUpdateDTO> request = new HttpEntity<>(statusUpdate,headers1);
        
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
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        HttpHeaders headers1 = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> requestForPost1 = new HttpEntity<OrderRequestDTO>(requestDTO,headers1);
        
        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestForPost1,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // When
        HttpEntity<Void> requestForDelete = new HttpEntity<Void>(headers1);
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/" + orderId,
                HttpMethod.DELETE,
                requestForDelete,
                Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify database
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        
        // Verify stock restoration
        verify(productServiceClient).getProduct(1L,1L,"");
        verify(productServiceClient).updateProductStock(1L, 12); // 10 + 2 restored
        verify(kafkaProducerService).publishOrderCancelledEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void shouldReturnBadRequestWhenCancelNonCancellableOrder() {
        // Given - Create order and mark as DELIVERED
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(1L, 2);
        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setDeliveryAddress("Test Address");
        requestDTO.setItems(List.of(itemDTO));

        HttpHeaders headers1 = createHeaders(1L,"");
        HttpEntity<OrderRequestDTO> requestForPost1 = new HttpEntity<OrderRequestDTO>(requestDTO,headers1);
        
        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.postForEntity(
                baseUrl,
                requestForPost1,
                OrderResponseDTO.class
        );
        Long orderId = createResponse.getBody().getId();

        // Update order to DELIVERED manually
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        // When
        HttpEntity<Void> requestForDelete = new HttpEntity<Void>(headers1);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/" + orderId,
                HttpMethod.DELETE,
                requestForDelete,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Cannot cancel order");
                
    }
}