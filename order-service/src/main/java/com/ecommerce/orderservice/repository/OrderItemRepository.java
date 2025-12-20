package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all items for a specific order
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Find all items for a specific product across all orders
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Count items in a specific order
     */
    long countByOrderId(Long orderId);

    /**
     * Check if a product exists in any order item
     */
    boolean existsByProductId(Long productId);

    /**
     * Get total quantity sold for a specific product
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productId = :productId")
    Integer getTotalQuantitySoldByProduct(@Param("productId") Long productId);

    /**
     * Delete all items for a specific order
     */
    void deleteByOrderId(Long orderId);
}