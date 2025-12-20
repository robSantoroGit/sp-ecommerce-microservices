package com.ecommerce.orderservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;

@DataJpaTest
@ContextConfiguration(classes = {TestConfig.class})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Order order1;
    private Order order2;
    private Order order3;

    @BeforeEach
    void setUp() {
        // Order 1 - User 1, PENDING
        order1 = new Order();
        order1.setUserId(1L);
        order1.setStatus(OrderStatus.PENDING);
        order1.setTotalAmount(new BigDecimal("100.00"));
        order1.setDeliveryAddress("Address 1");
        order1.setOrderDate(LocalDateTime.now().minusDays(2));

        OrderItem item1 = new OrderItem();
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("50.00"));
        order1.addItem(item1);

        entityManager.persist(order1);

        // Order 2 - User 1, PAID
        order2 = new Order();
        order2.setUserId(1L);
        order2.setStatus(OrderStatus.PAID);
        order2.setTotalAmount(new BigDecimal("200.00"));
        order2.setDeliveryAddress("Address 2");
        order2.setOrderDate(LocalDateTime.now().minusDays(1));

        OrderItem item2 = new OrderItem();
        item2.setProductId(2L);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("200.00"));
        order2.addItem(item2);

        entityManager.persist(order2);

        // Order 3 - User 2, CONFIRMED
        order3 = new Order();
        order3.setUserId(2L);
        order3.setStatus(OrderStatus.CONFIRMED);
        order3.setTotalAmount(new BigDecimal("150.00"));
        order3.setDeliveryAddress("Address 3");
        order3.setOrderDate(LocalDateTime.now());

        OrderItem item3 = new OrderItem();
        item3.setProductId(1L);
        item3.setQuantity(3);
        item3.setUnitPrice(new BigDecimal("50.00"));
        order3.addItem(item3);

        entityManager.persist(order3);

        entityManager.flush();
    }

    @Test
    void shouldFindOrdersByUserId() {
        // When
        List<Order> orders = orderRepository.findByUserId(1L);

        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getUserId).containsOnly(1L);
        assertThat(orders).extracting(Order::getStatus)
                .containsExactlyInAnyOrder(OrderStatus.PENDING, OrderStatus.PAID);
    }

    @Test
    void shouldFindOrdersByUserIdWithPagination() {
        // When
        Page<Order> page = orderRepository.findByUserId(1L, PageRequest.of(0, 1));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void shouldFindOrdersByUserIdAndStatus() {
        // When
        List<Order> orders = orderRepository.findByUserIdAndStatus(1L, OrderStatus.PAID);

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(orders.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void shouldFindOrdersByStatus() {
        // When
        List<Order> orders = orderRepository.findByStatus(OrderStatus.PENDING);

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldFindOrdersByStatusWithPagination() {
        // When
        Page<Order> page = orderRepository.findByStatus(OrderStatus.PENDING, PageRequest.of(0, 10));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldFindOrdersByOrderDateBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(3);
        LocalDateTime endDate = LocalDateTime.now().minusDays(1).plusHours(1);

        // When
        List<Order> orders = orderRepository.findByOrderDateBetween(startDate, endDate);

        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getId)
                .containsExactlyInAnyOrder(order1.getId(), order2.getId());
    }

    @Test
    void shouldFindOrderByIdWithItems() {
        // When
        Optional<Order> result = orderRepository.findByIdWithItems(order1.getId());

        // Then
        assertThat(result).isPresent();
        Order order = result.get();
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getProductId()).isEqualTo(1L);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void shouldFindOrdersByUserIdWithItems() {
        // When
        List<Order> orders = orderRepository.findByUserIdWithItems(1L);

        // Then
        assertThat(orders).hasSize(2);
        orders.forEach(order -> {
            assertThat(order.getItems()).isNotEmpty();
            assertThat(order.getUserId()).isEqualTo(1L);
        });
    }

    @Test
    void shouldCountOrdersByUserId() {
        // When
        long count = orderRepository.countByUserId(1L);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCountOrdersByStatus() {
        // When
        long count = orderRepository.countByStatus(OrderStatus.PENDING);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldCheckIfOrderExistsByIdAndUserId() {
        // When
        boolean exists = orderRepository.existsByIdAndUserId(order1.getId(), 1L);
        boolean notExists = orderRepository.existsByIdAndUserId(order1.getId(), 2L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFoundByIdWithItems() {
        // When
        Optional<Order> result = orderRepository.findByIdWithItems(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoOrdersForUser() {
        // When
        List<Order> orders = orderRepository.findByUserId(999L);

        // Then
        assertThat(orders).isEmpty();
    }

    @Test
    void shouldSaveOrderWithItems() {
        // Given
        Order newOrder = new Order();
        newOrder.setUserId(3L);
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setTotalAmount(new BigDecimal("300.00"));
        newOrder.setDeliveryAddress("New Address");
        newOrder.setOrderDate(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProductId(3L);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("60.00"));
        newOrder.addItem(item);

        // When
        Order saved = orderRepository.save(newOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Order> found = orderRepository.findByIdWithItems(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().get(0).getProductId()).isEqualTo(3L);
    }

    @Test
    void shouldDeleteOrderWithItems() {
        // Given
        Long orderId = order1.getId();

        // When
        orderRepository.deleteById(orderId);
        entityManager.flush();

        // Then
        Optional<Order> found = orderRepository.findById(orderId);
        assertThat(found).isEmpty();
    }
}