package com.ecommerce.productService.service;

import java.util.List;

import com.ecommerce.productService.dto.CategoryRequestDTO;
import com.ecommerce.productService.dto.CategoryResponseDTO;
import com.ecommerce.productService.dto.SecurityContext;
import com.ecommerce.productService.exception.DuplicateResourceException;
import com.ecommerce.productService.exception.ResourceNotFoundException;

public interface CategoryService {
    
    /**
     * Create a new category
     * @param dto Category data
     * @return Created category
     * @throws DuplicateResourceException if category name already exists
     */
    CategoryResponseDTO createCategory(CategoryRequestDTO dto, SecurityContext securityContext);
    
    /**
     * Get all categories
     * @return List of all categories
     */
    List<CategoryResponseDTO> getAllCategories();
    
    /**
     * Get category by ID
     * @param id Category ID
     * @return Category details
     * @throws ResourceNotFoundException if category not found
     */
    CategoryResponseDTO getCategoryById(Long id );
    
    /**
     * Update existing category
     * @param id Category ID
     * @param dto Updated category data
     * @return Updated category
     * @throws ResourceNotFoundException if category not found
     * @throws DuplicateResourceException if new name already exists
     */
    CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO dto, SecurityContext securityContext);
    
    /**
     * Delete category
     * @param id Category ID
     * @throws ResourceNotFoundException if category not found
     */
    void deleteCategory(Long id, SecurityContext securityContext);
}