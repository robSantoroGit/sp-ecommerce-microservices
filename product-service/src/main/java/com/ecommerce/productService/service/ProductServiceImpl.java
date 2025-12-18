package com.ecommerce.productService.service;

import com.ecommerce.productService.dto.ProductMapper;
import com.ecommerce.productService.dto.ProductRequestDTO;
import com.ecommerce.productService.dto.ProductResponseDTO;
import com.ecommerce.productService.exception.ResourceNotFoundException;
import com.ecommerce.productService.model.Category;
import com.ecommerce.productService.model.Product;
import com.ecommerce.productService.repository.CategoryRepository;
import com.ecommerce.productService.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    
    public ProductServiceImpl(ProductRepository productRepository,CategoryRepository categoryRepository,ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }
    
    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        // Load and verify category exists
        Category category = categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + dto.getCategoryId()
            ));
        
        // Convert DTO to entity with category
        Product product = productMapper.toEntity(dto, category);
        
        // Save entity
        Product saved = productRepository.save(product);
        
        // Convert entity to response DTO
        return productMapper.toResponseDTO(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll()
            .stream()
            .map(productMapper::toResponseDTO)
            .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with id: " + id
            ));
        
        return productMapper.toResponseDTO(product);
    }
    
    @Override
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
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
        
        // Convert to response DTO
        return productMapper.toResponseDTO(updated);
    }
    
    @Override
    public void deleteProduct(Long id) {
        // Check if product exists
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "Product not found with id: " + id
            );
        }
        
        // Delete product
        productRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
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
        // Search by name (case-insensitive, with @EntityGraph)
        return productRepository.findByNameContainingIgnoreCase(keyword)
            .stream()
            .map(productMapper::toResponseDTO)
            .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        // Find products in price range (with @EntityGraph)
        return productRepository.findByPriceBetween(minPrice, maxPrice)
            .stream()
            .map(productMapper::toResponseDTO)
            .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getActiveProducts() {
        // Find only active products (with @EntityGraph)
        return productRepository.findByActiveTrue()
            .stream()
            .map(productMapper::toResponseDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public ProductResponseDTO updateStock(Long id, Integer quantity) {
        // Find product
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with id: " + id
            ));
        
        // Update stock
        product.setStock(quantity);
        
        // Save updated entity
        Product updated = productRepository.save(product);
        
        // Convert to response DTO
        return productMapper.toResponseDTO(updated);
    }
    
    @Override
    public ProductResponseDTO deactivateProduct(Long id) {
        // Find product
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product not found with id: " + id
            ));
        
        // Deactivate (soft delete)
        product.setActive(false);
        
        // Save updated entity
        Product updated = productRepository.save(product);
        
        // Convert to response DTO
        return productMapper.toResponseDTO(updated);
    }
}