package com.ecommerce.productService.service;

import com.ecommerce.productService.dto.CategoryMapper;
import com.ecommerce.productService.dto.CategoryRequestDTO;
import com.ecommerce.productService.dto.CategoryResponseDTO;
import com.ecommerce.productService.dto.SecurityContext;
import com.ecommerce.productService.exception.DuplicateResourceException;
import com.ecommerce.productService.exception.ForbiddenException;
import com.ecommerce.productService.exception.ResourceNotFoundException;
import com.ecommerce.productService.model.Category;
import com.ecommerce.productService.repository.CategoryRepository;
import com.ecommerce.productService.security.Permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    
    public CategoryServiceImpl(CategoryRepository categoryRepository,CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }
    
    @Override
    public CategoryResponseDTO createCategory(CategoryRequestDTO dto, SecurityContext securityContext) {
    	log.info("Creating new category with name: {}", dto.getName());
    	if (!securityContext.hasPermission(Permission.PRODUCT_WRITE)) {
    		log.warn("Access denied: user without permission {} tried to create category", securityContext.getUserId());
    		throw new ForbiddenException("Permission denied: product.write required");
    	}
        // Check if category name already exists
    	if (categoryRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException(
                "Category already exists with name: " + dto.getName()
            );
        }
        
        // Convert DTO to entity
        Category category = categoryMapper.toEntity(dto);
        
        // Save entity
        Category saved = categoryRepository.save(category);
        
        log.info("Category created successfully: {}", saved.getId());
        
        // Convert entity to response DTO
        return categoryMapper.toResponseDTO(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
    	log.info("Fetching all categories");
    	return categoryRepository.findAll()
            .stream()
            .map(categoryMapper::toResponseDTO).toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(Long id) {
    	log.info("Fetching category with id: {}", id);
    	Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + id
            ));
        return categoryMapper.toResponseDTO(category);
    }
    
    @Override
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO dto, SecurityContext securityContext) {
    	log.info("Updating category with id: {}", id);
    	if (!securityContext.hasPermission(Permission.PRODUCT_WRITE)) {
    		log.warn("Access denied: User {} tried to update category {}", securityContext.getUserId(), id);
    		throw new ForbiddenException("Permission denied: product.write required");
    	}
        // Find existing category
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + id
            ));
        
        // Check if new name already exists (if name changed)
        if (!category.getName().equals(dto.getName())) {
            Optional<Category> existingWithName = categoryRepository.findByName(dto.getName());
            if (existingWithName.isPresent() && !existingWithName.get().getId().equals(id)) {
                throw new DuplicateResourceException(
                    "Category already exists with name: " + dto.getName()
                );
            }
        }
        
        // Update entity from DTO
        categoryMapper.updateEntityFromDTO(category, dto);
        
        // Save updated entity
        Category updated = categoryRepository.save(category);
        
        log.info("Category updated successfully: {}", updated.getId());
        
        // Convert to response DTO
        return categoryMapper.toResponseDTO(updated);
    }
    
    @Override
    public void deleteCategory(Long id, SecurityContext securityContext) {
    	log.info("Deleting category with id: {}", id);
    	if (!securityContext.hasPermission(Permission.PRODUCT_DELETE)) {
    		log.warn("Access denied: user {} tried to delete category", securityContext.getUserId());
    		throw new ForbiddenException("Permission denied: product.delete required");
    	}
        
    	// Check if category exists
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "Category not found with id: " + id
            );
        }
        
        // Delete category
        categoryRepository.deleteById(id);
        log.info("Category deleted successfully: {}", id);
    }
}