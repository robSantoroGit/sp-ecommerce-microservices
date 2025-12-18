package com.ecommerce.productService.repository;

import com.ecommerce.productService.model.Product;
import com.ecommerce.productService.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @EntityGraph(attributePaths = {"category"})
    List<Product> findByCategory(Category category);
    
    @EntityGraph(attributePaths = {"category"})
    List<Product> findByNameContainingIgnoreCase(String name);
    
    @EntityGraph(attributePaths = {"category"})
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    @EntityGraph(attributePaths = {"category"})
    List<Product> findByActiveTrue();
}