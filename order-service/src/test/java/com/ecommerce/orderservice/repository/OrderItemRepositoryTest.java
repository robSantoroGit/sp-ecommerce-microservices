package com.ecommerce.orderservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;

@DataJpaTest
@ContextConfiguration(classes = {TestConfig.class})
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Order order1;
    private Order order2;
    private OrderItem item1;
    private OrderItem item2;
    private OrderItem item3;

    @BeforeEach
    void setUp() {
        // Order 1
        order1 = new Order();
        order1.setUserId(1L);
        order1.setStatus(OrderStatus.PENDING);
        order1.setTotalAmount(new BigDecimal("150.00"));
        order1.setDeliveryAddress("Address 1");
        order1.setOrderDate(LocalDateTime.now());

        item1 = new OrderItem();
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("50.00"));
        order1.addItem(item1);

        item2 = new OrderItem();
        item2.setProductId(2L);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("50.00"));
        order1.addItem(item2);

        entityManager.persist(order1);

        // Order 2
        order2 = new Order();
        order2.setUserId(2L);
        order2.setStatus(OrderStatus.PAID);
        order2.setTotalAmount(new BigDecimal("100.00"));
        order2.setDeliveryAddress("Address 2");
        order2.setOrderDate(LocalDateTime.now());

        item3 = new OrderItem();
        item3.setProductId(1L);
        item3.setQuantity(4);
        item3.setUnitPrice(new BigDecimal("25.00"));
        order2.addItem(item3);

        entityManager.persist(order2);

        entityManager.flush();
    }

    @Test
    void shouldFindItemsByOrderId() {
        // When
        List<OrderItem> items = orderItemRepository.findByOrderId(order1.getId());

        // Then
        assertThat(items).hasSize(2);
        assertThat(items).extracting(OrderItem::getProductId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void shouldFindItemsByProductId() {
        // When
        List<OrderItem> items = orderItemRepository.findByProductId(1L);

        // Then
        assertThat(items).hasSize(2);
        assertThat(items).extracting(OrderItem::getProductId).containsOnly(1L);
        assertThat(items).extracting(OrderItem::getQuantity)
                .containsExactlyInAnyOrder(2, 4);
    }

    @Test
    void shouldCountItemsByOrderId() {
        // When
        long count = orderItemRepository.countByOrderId(order1.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCheckIfProductExistsInOrderItems() {
        // When
        boolean exists = orderItemRepository.existsByProductId(1L);
        boolean notExists = orderItemRepository.existsByProductId(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldGetTotalQuantitySoldByProduct() {
        // When
        Integer totalQuantity = orderItemRepository.getTotalQuantitySoldByProduct(1L);

        // Then
        assertThat(totalQuantity).isEqualTo(6); // 2 + 4
    }

    @Test
    void shouldReturnZeroWhenProductNotSold() {
        // When
        Integer totalQuantity = orderItemRepository.getTotalQuantitySoldByProduct(999L);

        // Then
        assertThat(totalQuantity).isEqualTo(0);
    }

    @Test
    void shouldCalculateSubtotalAutomatically() {
        // When
        List<OrderItem> items = orderItemRepository.findByOrderId(order1.getId());

        // Then
        OrderItem itemWithProduct1 = items.stream()
                .filter(i -> i.getProductId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(itemWithProduct1.getSubtotal())
                .isEqualByComparingTo(new BigDecimal("100.00")); // 2 * 50.00
    }

    @Test
    void shouldReturnEmptyListWhenNoItemsForOrder() {
        // When
        List<OrderItem> items = orderItemRepository.findByOrderId(999L);

        // Then
        assertThat(items).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoItemsForProduct() {
        // When
        List<OrderItem> items = orderItemRepository.findByProductId(999L);

        // Then
        assertThat(items).isEmpty();
    }

    @Test
    void shouldSaveOrderItem() {
        // Given
        Order newOrder = new Order();
        newOrder.setUserId(3L);
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setTotalAmount(new BigDecimal("200.00"));
        newOrder.setDeliveryAddress("Address 3");
        newOrder.setOrderDate(LocalDateTime.now());
        entityManager.persist(newOrder);

        OrderItem newItem = new OrderItem();
        newItem.setProductId(3L);
        newItem.setQuantity(5);
        newItem.setUnitPrice(new BigDecimal("40.00"));
        newOrder.addItem(newItem);

        // When
        OrderItem saved = orderItemRepository.save(newItem);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void shouldDeleteOrderItem() {
        // Given
        Long itemId = item1.getId();

        // When
        orderItemRepository.deleteById(itemId);
        entityManager.flush();

        // Then
        List<OrderItem> items = orderItemRepository.findByOrderId(order1.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getId()).isNotEqualTo(itemId);
    }
}