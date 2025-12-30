package com.ecommerce.productService.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.productService.dto.ProductMapper;
import com.ecommerce.productService.dto.ProductRequestDTO;
import com.ecommerce.productService.dto.ProductResponseDTO;
import com.ecommerce.productService.dto.SecurityContext;
import com.ecommerce.productService.exception.ForbiddenException;
import com.ecommerce.productService.exception.ResourceNotFoundException;
import com.ecommerce.productService.model.Category;
import com.ecommerce.productService.model.Product;
import com.ecommerce.productService.repository.CategoryRepository;
import com.ecommerce.productService.repository.ProductRepository;
import com.ecommerce.productService.security.Permission;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    
    public ProductServiceImpl(ProductRepository productRepository,CategoryRepository categoryRepository,ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }
    
    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO dto, SecurityContext securityContext) {
    	log.info("Creating new product with name: {}", dto.getName());
    	if (!securityContext.hasPermission(Permission.PRODUCT_WRITE)) {
    		log.warn("Access denied: user without permission {} tried to create product", securityContext.getUserId());
    		throw new ForbiddenException("Permission denied: product.write required");
    	}
    	// Load and verify category exists
        Category category = categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + dto.getCategoryId()
            ));
        
        // Convert DTO to entity with category
        Product product = productMapper.toEntity(dto, category);
        
        // Save entity
        Product saved = productRepository.save(product);
        
        log.info("Product created successfully: {}", saved.getId());
        
        // Convert entity to response DTO
        return productMapper.toResponseDTO(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
    	log.info("Fetching all products");
    	return productRepository.findAll()
            .stream()
            .map(productMapper::toResponseDTO)
            .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
    	log.info("Fetching product with id: {}", id);
    	Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with id: " + id
            ));
        
        return productMapper.toResponseDTO(product);
    }
    
    @Override
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto,SecurityContext securityContext) {
    	log.info("Updating product with id: {}", id);
    	if (!securityContext.hasPermission(Permission.PRODUCT_WRITE)) {
    		log.warn("Access denied: User {} tried to update product {}", securityContext.getUserId(), id);
    		throw new ForbiddenException("Permission denied: product.write required");
    	}
    	// Find existing product
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with id: " + id
            ));
        
        // Load and verify new category exists
        Category category = categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + dto.getCategoryId()
            ));
        
        // Update entity from DTO
        productMapper.updateEntityFromDTO(product, dto, category);
        
        // Save updated entity
        Product updated = productRepository.save(product);
        
        log.info("Product updated successfully: {}", updated.getId());
        
        // Convert to response DTO
        return productMapper.toResponseDTO(updated);
    }
    
    @Override
    public void deleteProduct(Long id, SecurityContext securityContext) {
    	log.info("Deleting product with id: {}", id);
    	if (!securityContext.hasPermission(Permission.PRODUCT_DELETE)) {
    		log.warn("Access denied: user {} tried to delete product", securityContext.getUserId());
    	    throw new ForbiddenException("Permission denied: product.write required");
    	}
    	// Check if product exists
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "Product not found with id: " + id
            );
        }
        
        // Delete product
        productRepository.deleteById(id);
        log.info("Product deleted successfully: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
    	log.info("Fetching product with category id: {}", categoryId);
    	// Verify category exists
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + categoryId
            ));
        
        // Find products by category (with @EntityGraph - no N+1)
        return productRepository.findByCategory(category)
            .stream()
            .map(productMapper::toResponseDTO)
            .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(String keyword) {
    	log.info("Fetching products with keyword: {}", keyword);
    	// Search by name (case-insensitive, with @EntityGraph)
        return productRepository.findByNameContainingIgnoreCase(keyword)
            .stream()
            .map(productMapper::toResponseDTO)
            .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
    	log.info("Fetching product with category price range between {} and {}", minPrice, maxPrice);
    	// Find products in price range (with @EntityGraph)
        return productRepository.findByPriceBetween(minPrice, maxPrice)
            .stream()
            .map(productMapper::toResponseDTO)
            .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getActiveProducts() {
    	log.info("Fetching active products");
    	// Find only active products (with @EntityGraph)
        return productRepository.findByActiveTrue()
            .stream()
            .map(productMapper::toResponseDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public ProductResponseDTO updateStock(Long id, Integer quantity,SecurityContext securityContext) {
    	log.info("Updating product stock for id: {} and stock: {}", id, quantity);
    	if (!securityContext.getUsername().equals("SYSTEM")) {
    		log.warn("Access denied: non SYSTEM User tried to update product stock for id: {}", id);
    	    throw new ForbiddenException("Permission denied: product.write required");
    	}
    	// Find product
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with id: " + id
            ));
        
        // Update stock
        product.setStock(quantity);
        
        // Save updated entity
        Product updated = productRepository.save(product);
        
        log.info("Product stock updated successfully: {}", updated.getId());
        
        // Convert to response DTO
        return productMapper.toResponseDTO(updated);
    }
    
    @Override
    public ProductResponseDTO deactivateProduct(Long id,SecurityContext securityContext) {
    	log.info("Deactivating product id: {} ", id);
    	if (!securityContext.hasPermission(Permission.PRODUCT_WRITE)) {
    		log.warn("Access denied: User {} tried to deactivate product id: {}", securityContext.getUserId(), id);
    		throw new ForbiddenException("Permission denied: product.write required");
    	}
    	// Find product
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with id: " + id
            ));
        
        // Deactivate (soft delete)
        product.setActive(false);
        
        // Save updated entity
        Product updated = productRepository.save(product);
        
        log.info("Product deactivated successfully: {}", updated.getId());
        
        // Convert to response DTO
        return productMapper.toResponseDTO(updated);
    }
}