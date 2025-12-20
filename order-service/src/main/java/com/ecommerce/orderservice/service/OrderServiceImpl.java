package com.ecommerce.orderservice.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.client.UserServiceClient;
import com.ecommerce.orderservice.dto.OrderItemRequestDTO;
import com.ecommerce.orderservice.dto.OrderItemResponseDTO;
import com.ecommerce.orderservice.dto.OrderMapper;
import com.ecommerce.orderservice.dto.OrderRequestDTO;
import com.ecommerce.orderservice.dto.OrderResponseDTO;
import com.ecommerce.orderservice.dto.PaymentRequestDTO;
import com.ecommerce.orderservice.dto.ProductDTO;
import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.event.OrderStatusChangedEvent;
import com.ecommerce.orderservice.exception.InvalidOrderStatusException;
import com.ecommerce.orderservice.exception.OrderCancellationException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    private final KafkaProducerService kafkaProducerService;

    public OrderServiceImpl(OrderRepository orderRepository,
                           OrderMapper orderMapper,
                           UserServiceClient userServiceClient,
                           ProductServiceClient productServiceClient,
                           KafkaProducerService kafkaProducerService) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.userServiceClient = userServiceClient;
        this.productServiceClient = productServiceClient;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO requestDTO) {
        logger.info("Creating order for user: {}", requestDTO.getUserId());

        // 1. Validate user exists
        userServiceClient.validateUser(requestDTO.getUserId());

        // 2. Validate products and check stock availability
        Map<Long, ProductDTO> productMap = new HashMap<>();
        for (OrderItemRequestDTO itemDTO : requestDTO.getItems()) {
            ProductDTO product = productServiceClient.validateProductAndStock(
                    itemDTO.getProductId(), 
                    itemDTO.getQuantity()
            );
            productMap.put(product.getId(), product);
        }

        // 3. Create Order entity
        Order order = orderMapper.toEntity(requestDTO);
        order.setStatus(OrderStatus.PENDING);

        // 4. Create OrderItems and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequestDTO itemDTO : requestDTO.getItems()) {
            ProductDTO product = productMap.get(itemDTO.getProductId());
            
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            // subtotal calculated automatically by @PrePersist
            
            order.addItem(orderItem);
            
            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemSubtotal);
        }
        
        order.setTotalAmount(totalAmount);

        // 5. Update product stock
        for (OrderItemRequestDTO itemDTO : requestDTO.getItems()) {
            ProductDTO product = productMap.get(itemDTO.getProductId());
            Integer newStock = product.getStock() - itemDTO.getQuantity();
            productServiceClient.updateProductStock(product.getId(), newStock);
        }

        // 6. Save order
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully with id: {}", savedOrder.getId());

        // 7. Enrich response with product names
        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(savedOrder);
        enrichOrderItemsWithProductNames(responseDTO, productMap);

        // 8. Publish Kafka event - order.created
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getStatus(),
                savedOrder.getTotalAmount()
        );
        kafkaProducerService.publishOrderCreatedEvent(event);

        return responseDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        logger.debug("Getting order by id: {}", id);
        
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(order);
        enrichOrderItemsWithProductNames(responseDTO);

        return responseDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        logger.debug("Getting orders for user: {}", userId);
        
        // Validate user exists
        userServiceClient.validateUser(userId);

        List<Order> orders = orderRepository.findByUserIdWithItems(userId);
        List<OrderResponseDTO> responseDTOs = orderMapper.toResponseDTOList(orders);

        // Enrich all orders with product names
        responseDTOs.forEach(this::enrichOrderItemsWithProductNames);

        return responseDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByUserId(Long userId, Pageable pageable) {
        logger.debug("Getting orders for user: {} with pagination", userId);
        
        // Validate user exists
        userServiceClient.validateUser(userId);

        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        
        return orders.map(order -> {
            OrderResponseDTO dto = orderMapper.toResponseDTO(order);
            enrichOrderItemsWithProductNames(dto);
            return dto;
        });
    }

    @Override
    @Transactional
    public OrderResponseDTO processPayment(Long orderId, PaymentRequestDTO paymentRequest) {
        logger.info("Processing payment for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Validate current status is PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                    order.getStatus(), 
                    OrderStatus.PAID
            );
        }

        // TODO: Actual payment processing logic would go here
        // For now, we just update the status

        // Store previous status for event
        OrderStatus previousStatus = order.getStatus();
        
        order.setStatus(OrderStatus.PAID);
        Order savedOrder = orderRepository.save(order);

        logger.info("Payment processed successfully for order: {}", orderId);

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(savedOrder);
        enrichOrderItemsWithProductNames(responseDTO);

        // Publish Kafka event - order.status.changed
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                previousStatus,
                savedOrder.getStatus(),
                savedOrder.getTotalAmount()
        );
        kafkaProducerService.publishOrderStatusChangedEvent(event);

        return responseDTO;
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {
        logger.info("Updating order status: {} to {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Validate status transition
        if (!OrderStatus.isValidTransition(order.getStatus(), newStatus)) {
            throw new InvalidOrderStatusException(order.getStatus(), newStatus);
        }
        
        // Store previous status for event
        OrderStatus previousStatus = order.getStatus();

        order.setStatus(newStatus);
        
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        logger.info("Order status updated successfully: {} -> {}", orderId, newStatus);

        OrderResponseDTO responseDTO = orderMapper.toResponseDTO(savedOrder);
        enrichOrderItemsWithProductNames(responseDTO);

        // Publish Kafka event - order.status.changed
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                previousStatus,
                savedOrder.getStatus(),
                savedOrder.getTotalAmount()
        );
        kafkaProducerService.publishOrderStatusChangedEvent(event);

        return responseDTO;
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        logger.info("Cancelling order: {}", orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Validate order can be cancelled
        if (!order.getStatus().isCancellable()) {
            throw new OrderCancellationException(orderId, order.getStatus());
        }

        // Restore product stock
        for (OrderItem item : order.getItems()) {
            ProductDTO product = productServiceClient.getProduct(item.getProductId());
            Integer restoredStock = product.getStock() + item.getQuantity();
            productServiceClient.updateProductStock(item.getProductId(), restoredStock);
            logger.debug("Restored stock for product {}: +{} (new total: {})", 
                    item.getProductId(), item.getQuantity(), restoredStock);
        }

        // Update order status
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        logger.info("Order cancelled successfully: {}", orderId);

        // Publish Kafka event - order.cancelled
        OrderCancelledEvent event = new OrderCancelledEvent(
        		order.getId(),
        		order.getUserId(),
        		order.getStatus(),
        		order.getTotalAmount(),
                "User cancellation"  // Default reason
        );
        kafkaProducerService.publishOrderCancelledEvent(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        logger.debug("Getting all orders");
        
        List<Order> orders = orderRepository.findAll();
        List<OrderResponseDTO> responseDTOs = orderMapper.toResponseDTOList(orders);

        // Enrich all orders with product names
        responseDTOs.forEach(this::enrichOrderItemsWithProductNames);

        return responseDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        logger.debug("Getting all orders with pagination");
        
        Page<Order> orders = orderRepository.findAll(pageable);
        
        return orders.map(order -> {
            OrderResponseDTO dto = orderMapper.toResponseDTO(order);
            enrichOrderItemsWithProductNames(dto);
            return dto;
        });
    }

    /**
     * Enrich OrderItemResponseDTOs with product names from Product Service
     */
    private void enrichOrderItemsWithProductNames(OrderResponseDTO orderDTO) {
        for (OrderItemResponseDTO itemDTO : orderDTO.getItems()) {
            try {
                ProductDTO product = productServiceClient.getProduct(itemDTO.getProductId());
                itemDTO.setProductName(product.getName());
            } catch (Exception e) {
                logger.warn("Could not fetch product name for productId: {}", itemDTO.getProductId());
                itemDTO.setProductName("Unknown Product");
            }
        }
    }

    /**
     * Enrich OrderItemResponseDTOs with product names from pre-fetched product map
     */
    private void enrichOrderItemsWithProductNames(OrderResponseDTO orderDTO, Map<Long, ProductDTO> productMap) {
        for (OrderItemResponseDTO itemDTO : orderDTO.getItems()) {
            ProductDTO product = productMap.get(itemDTO.getProductId());
            if (product != null) {
                itemDTO.setProductName(product.getName());
            } else {
                itemDTO.setProductName("Unknown Product");
            }
        }
    }
}