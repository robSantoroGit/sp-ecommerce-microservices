package com.ecommerce.productService.dto;

import com.ecommerce.productService.model.Product;
import com.ecommerce.productService.model.Category;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    
    private final CategoryMapper categoryMapper;
    
    public ProductMapper(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }
    
    /**
     * Convert Product entity to ProductResponseDTO
     * @param product Product entity (must not be null)
     * @return ProductResponseDTO with nested CategoryResponseDTO
     */
    public ProductResponseDTO toResponseDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setActive(product.isActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        
        // Convert nested Category to CategoryResponseDTO
        dto.setCategory(categoryMapper.toResponseDTO(product.getCategory()));
        
        return dto;
    }
    
    /**
     * Convert ProductRequestDTO to Product entity (for CREATE)
     * @param dto ProductRequestDTO (must not be null)
     * @param category Category entity (must not be null)
     * @return Product entity
     */
    public Product toEntity(ProductRequestDTO dto, Category category) {
        Product product = new Product();
        
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(category);
        
        // active, createdAt, updatedAt initialized in Product constructor
        
        return product;
    }
    
    /**
     * Update existing Product entity from ProductRequestDTO (for UPDATE)
     * @param product Existing Product entity (must not be null)
     * @param dto ProductRequestDTO with new values (must not be null)
     * @param category Category entity (must not be null)
     */
    public void updateEntityFromDTO(Product product, ProductRequestDTO dto, Category category) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(category);
        
        // id, active, createdAt NOT modified
        // updatedAt automatically updated by @PreUpdate
    }
}