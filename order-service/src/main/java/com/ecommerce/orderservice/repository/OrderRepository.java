package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders by userId with pagination
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all orders by userId (without pagination)
     */
    List<Order> findByUserId(Long userId);

    /**
     * Find orders by userId and status
     */
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    /**
     * Find orders by status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders by status with pagination
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find orders created between two dates
     */
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find order by id with items (eager fetch to avoid N+1)
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    /**
     * Find orders by userId with items (eager fetch)
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = :userId")
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);

    /**
     * Count orders by userId
     */
    long countByUserId(Long userId);

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);

    /**
     * Check if order exists by userId and id
     */
    boolean existsByIdAndUserId(Long id, Long userId);
}