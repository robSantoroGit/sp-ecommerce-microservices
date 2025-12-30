package com.ecommerce.productService.service;

import com.ecommerce.productService.dto.ProductRequestDTO;
import com.ecommerce.productService.dto.ProductResponseDTO;
import com.ecommerce.productService.dto.SecurityContext;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    
    /**
     * Create a new product
     * @param dto Product data
     * @return Created product
     * @throws ResourceNotFoundException if category not found
     */
    ProductResponseDTO createProduct(ProductRequestDTO dto, SecurityContext securityContext);
    
    /**
     * Get all products
     * @return List of all products
     */
    List<ProductResponseDTO> getAllProducts();
    
    /**
     * Get product by ID
     * @param id Product ID
     * @return Product details
     * @throws ResourceNotFoundException if product not found
     */
    ProductResponseDTO getProductById(Long id);
    
    /**
     * Update existing product
     * @param id Product ID
     * @param dto Updated product data
     * @return Updated product
     * @throws ResourceNotFoundException if product or category not found
     */
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto, SecurityContext securityContext);
    
    /**
     * Delete product
     * @param id Product ID
     * @throws ResourceNotFoundException if product not found
     */
    void deleteProduct(Long id, SecurityContext securityContext);
    
    /**
     * Get products by category
     * @param categoryId Category ID
     * @return List of products in category
     * @throws ResourceNotFoundException if category not found
     */
    List<ProductResponseDTO> getProductsByCategory(Long categoryId);
    
    /**
     * Search products by name (case-insensitive)
     * @param keyword Search keyword
     * @return List of matching products
     */
    List<ProductResponseDTO> searchProducts(String keyword);
    
    /**
     * Get products within price range
     * @param minPrice Minimum price (inclusive)
     * @param maxPrice Maximum price (inclusive)
     * @return List of products in price range
     */
    List<ProductResponseDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * Get only active products
     * @return List of active products
     */
    List<ProductResponseDTO> getActiveProducts();
    
    /**
     * Update product stock
     * @param id Product ID
     * @param quantity New stock quantity
     * @return Updated product
     * @throws ResourceNotFoundException if product not found
     */
    ProductResponseDTO updateStock(Long id, Integer quantity, SecurityContext securityContext);
    
    /**
     * Deactivate product (soft delete)
     * @param id Product ID
     * @return Deactivated product
     * @throws ResourceNotFoundException if product not found
     */
    ProductResponseDTO deactivateProduct(Long id, SecurityContext securityContext);
}