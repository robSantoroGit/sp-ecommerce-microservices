package com.ecommerce.productService.dto;

import com.ecommerce.productService.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    
    /**
     * Convert Category entity to CategoryResponseDTO
     * @param category Category entity (must not be null)
     * @return CategoryResponseDTO
     */
    public CategoryResponseDTO toResponseDTO(Category category) {
        return new CategoryResponseDTO(
            category.getId(),
            category.getName(),
            category.getDescription()
        );
    }
    
    /**
     * Convert CategoryRequestDTO to Category entity (for CREATE)
     * @param dto CategoryRequestDTO (must not be null)
     * @return Category entity
     */
    public Category toEntity(CategoryRequestDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        return category;
    }
    
    /**
     * Update existing Category entity from CategoryRequestDTO (for UPDATE)
     * @param category Existing Category entity (must not be null)
     * @param dto CategoryRequestDTO with new values (must not be null)
     */
    public void updateEntityFromDTO(Category category, CategoryRequestDTO dto) {
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
    }
}