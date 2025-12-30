package com.ecommerce.productService.service;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @Mock
    private ProductMapper productMapper;
    
    @InjectMocks
    private ProductServiceImpl productService;
    
    private SecurityContext adminContext;
    private SecurityContext userContext;
    
    @BeforeEach
    void setUp() {
        adminContext = new SecurityContext(999L, "admin", List.of(
            Permission.PRODUCT_READ,
            Permission.PRODUCT_WRITE,
            Permission.PRODUCT_DELETE
        ));
        
        userContext = new SecurityContext(1L, "user", List.of());
    }
    
    @Test
    void createProduct_Success() {
        // Given
        ProductRequestDTO dto = new ProductRequestDTO(
            "MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10, 1L
        );
        
        Category category = new Category("Electronics", "Devices");
        category.setId(1L);
        
        Product product = new Product("MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10);
        product.setId(1L);
        product.setCategory(category);
        
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook Pro");
        
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productMapper.toEntity(dto, category)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponseDTO(product)).thenReturn(responseDTO);
        
        // When
        ProductResponseDTO result = productService.createProduct(dto, adminContext);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("MacBook Pro");
        
        verify(categoryRepository).findById(1L);
        verify(productMapper).toEntity(dto, category);
        verify(productRepository).save(any(Product.class));
        verify(productMapper).toResponseDTO(product);
    }
    
    @Test
    void createProduct_ThrowsNotFoundException_WhenCategoryNotFound() {
        // Given
        ProductRequestDTO dto = new ProductRequestDTO(
            "MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10, 999L
        );
        
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> productService.createProduct(dto, adminContext))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category not found with id: 999");
        
        verify(categoryRepository).findById(999L);
        verify(productMapper, never()).toEntity(any(), any());
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void createProduct_ThrowsForbiddenException() {
        // Given
        ProductRequestDTO dto = new ProductRequestDTO(
            "MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10, 999L
        );
        
        // When & Then
        assertThatThrownBy(() -> productService.createProduct(dto, userContext))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Permission denied");
        
        verify(categoryRepository, never()).findById(anyLong());
        verify(productMapper, never()).toEntity(any(), any());
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void getAllProducts_ReturnsAllProducts() {
        // Given
        Category category = new Category("Electronics", "Devices");
        
        Product p1 = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
        p1.setId(1L);
        p1.setCategory(category);
        
        Product p2 = new Product("iPhone", "Phone", new BigDecimal("1199.99"), 20);
        p2.setId(2L);
        p2.setCategory(category);
        
        ProductResponseDTO dto1 = new ProductResponseDTO();
        dto1.setId(1L);
        dto1.setName("MacBook");
        
        ProductResponseDTO dto2 = new ProductResponseDTO();
        dto2.setId(2L);
        dto2.setName("iPhone");
        
        when(productRepository.findAll()).thenReturn(Arrays.asList(p1, p2));
        when(productMapper.toResponseDTO(p1)).thenReturn(dto1);
        when(productMapper.toResponseDTO(p2)).thenReturn(dto2);
        
        // When
        List<ProductResponseDTO> result = productService.getAllProducts();
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponseDTO::getName)
            .containsExactly("MacBook", "iPhone");
        
        verify(productRepository).findAll();
        verify(productMapper, times(2)).toResponseDTO(any(Product.class));
    }
    
    @Test
    void getProductById_Success() {
        // Given
        Category category = new Category("Electronics", "Devices");
        
        Product product = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
        product.setId(1L);
        product.setCategory(category);
        
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook");
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponseDTO(product)).thenReturn(responseDTO);
        
        // When
        ProductResponseDTO result = productService.getProductById(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("MacBook");
        
        verify(productRepository).findById(1L);
        verify(productMapper).toResponseDTO(product);
    }
    
    @Test
    void getProductById_ThrowsNotFoundException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> productService.getProductById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with id: 999");
        
        verify(productRepository).findById(999L);
        verify(productMapper, never()).toResponseDTO(any());
    }
    
    @Test
    void updateProduct_Success() {
        // Given
        Category oldCategory = new Category("Electronics", "Devices");
        oldCategory.setId(1L);
        
        Category newCategory = new Category("Computers", "Computing devices");
        newCategory.setId(2L);
        
        Product existingProduct = new Product("MacBook", "Old description", new BigDecimal("2499.99"), 10);
        existingProduct.setId(1L);
        existingProduct.setCategory(oldCategory);
        
        ProductRequestDTO dto = new ProductRequestDTO(
            "MacBook Pro", "New description", new BigDecimal("2999.99"), 15, 2L
        );
        
        Product updatedProduct = new Product("MacBook Pro", "New description", new BigDecimal("2999.99"), 15);
        updatedProduct.setId(1L);
        updatedProduct.setCategory(newCategory);
        
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook Pro");
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
        when(productMapper.toResponseDTO(updatedProduct)).thenReturn(responseDTO);
        
        // When
        ProductResponseDTO result = productService.updateProduct(1L, dto, adminContext);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("MacBook Pro");
        
        verify(productRepository).findById(1L);
        verify(categoryRepository).findById(2L);
        verify(productMapper).updateEntityFromDTO(existingProduct, dto, newCategory);
        verify(productRepository).save(existingProduct);
        verify(productMapper).toResponseDTO(updatedProduct);
    }
    
    @Test
    void updateProduct_ThrowsNotFoundException_WhenProductNotFound() {
        // Given
        ProductRequestDTO dto = new ProductRequestDTO(
            "MacBook", "Description", new BigDecimal("2499.99"), 10, 1L
        );
        
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(999L, dto, adminContext))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with id: 999");
        
        verify(productRepository).findById(999L);
        verify(categoryRepository, never()).findById(any());
        verify(productMapper, never()).updateEntityFromDTO(any(), any(), any());
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void updateProduct_ThrowsNotFoundException_WhenCategoryNotFound() {
        // Given
        Category category = new Category("Electronics", "Devices");
        category.setId(1L);
        
        Product existingProduct = new Product("MacBook", "Description", new BigDecimal("2499.99"), 10);
        existingProduct.setId(1L);
        existingProduct.setCategory(category);
        
        ProductRequestDTO dto = new ProductRequestDTO(
            "MacBook", "Description", new BigDecimal("2499.99"), 10, 999L
        );
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(1L, dto, adminContext))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category not found with id: 999");
        
        verify(productRepository).findById(1L);
        verify(categoryRepository).findById(999L);
        verify(productMapper, never()).updateEntityFromDTO(any(), any(), any());
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void updateProduct_ThrowsForbiddenException() {
        // Given
        ProductRequestDTO dto = new ProductRequestDTO(
            "MacBook", "Description", new BigDecimal("2499.99"), 10, 1L
        );
        
        
        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(999L, dto, userContext))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Permission denied");
        
        verify(productRepository, never()).findById(anyLong());
        verify(categoryRepository, never()).findById(any());
        verify(productMapper, never()).updateEntityFromDTO(any(), any(), any());
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void deleteProduct_Success() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);
        
        // When
        productService.deleteProduct(1L, adminContext);
        
        // Then
        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }
    
    @Test
    void deleteProduct_ThrowsNotFoundException() {
        // Given
        when(productRepository.existsById(999L)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct(999L, adminContext))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with id: 999");
        
        verify(productRepository).existsById(999L);
        verify(productRepository, never()).deleteById(any());
    }
    
    @Test
    void deleteProduct_ThrowsForbiddenException() {
        
        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct(999L, userContext))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Permission denied");
        
        verify(productRepository,never()).existsById(anyLong());
        verify(productRepository, never()).deleteById(any());
    }
    
    @Test
    void getProductsByCategory_Success() {
        // Given
        Category category = new Category("Electronics", "Devices");
        category.setId(1L);
        
        Product p1 = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
        p1.setCategory(category);
        
        Product p2 = new Product("iPhone", "Phone", new BigDecimal("1199.99"), 20);
        p2.setCategory(category);
        
        ProductResponseDTO dto1 = new ProductResponseDTO();
        dto1.setName("MacBook");
        
        ProductResponseDTO dto2 = new ProductResponseDTO();
        dto2.setName("iPhone");
        
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.findByCategory(category)).thenReturn(Arrays.asList(p1, p2));
        when(productMapper.toResponseDTO(p1)).thenReturn(dto1);
        when(productMapper.toResponseDTO(p2)).thenReturn(dto2);
        
        // When
        List<ProductResponseDTO> result = productService.getProductsByCategory(1L);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponseDTO::getName)
            .containsExactly("MacBook", "iPhone");
        
        verify(categoryRepository).findById(1L);
        verify(productRepository).findByCategory(category);
        verify(productMapper, times(2)).toResponseDTO(any(Product.class));
    }
    
    @Test
    void getProductsByCategory_ThrowsNotFoundException_WhenCategoryNotFound() {
        // Given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> productService.getProductsByCategory(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category not found with id: 999");
        
        verify(categoryRepository).findById(999L);
        verify(productRepository, never()).findByCategory(any());
    }
    
    @Test
    void searchProducts_ReturnsMatchingProducts() {
        // Given
        Product p1 = new Product("Magic Mouse", "Wireless mouse", new BigDecimal("79.99"), 50);
        Product p2 = new Product("Magic Keyboard", "Wireless keyboard", new BigDecimal("149.99"), 30);
        
        ProductResponseDTO dto1 = new ProductResponseDTO();
        dto1.setName("Magic Mouse");
        
        ProductResponseDTO dto2 = new ProductResponseDTO();
        dto2.setName("Magic Keyboard");
        
        when(productRepository.findByNameContainingIgnoreCase("magic"))
            .thenReturn(Arrays.asList(p1, p2));
        when(productMapper.toResponseDTO(p1)).thenReturn(dto1);
        when(productMapper.toResponseDTO(p2)).thenReturn(dto2);
        
        // When
        List<ProductResponseDTO> result = productService.searchProducts("magic");
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponseDTO::getName)
            .containsExactly("Magic Mouse", "Magic Keyboard");
        
        verify(productRepository).findByNameContainingIgnoreCase("magic");
        verify(productMapper, times(2)).toResponseDTO(any(Product.class));
    }
    
    @Test
    void getProductsByPriceRange_ReturnsProductsInRange() {
        // Given
        Product p1 = new Product("Mouse", "Wireless mouse", new BigDecimal("79.99"), 50);
        Product p2 = new Product("Keyboard", "Wireless keyboard", new BigDecimal("149.99"), 30);
        
        ProductResponseDTO dto1 = new ProductResponseDTO();
        dto1.setName("Mouse");
        
        ProductResponseDTO dto2 = new ProductResponseDTO();
        dto2.setName("Keyboard");
        
        BigDecimal min = new BigDecimal("50");
        BigDecimal max = new BigDecimal("200");
        
        when(productRepository.findByPriceBetween(min, max))
            .thenReturn(Arrays.asList(p1, p2));
        when(productMapper.toResponseDTO(p1)).thenReturn(dto1);
        when(productMapper.toResponseDTO(p2)).thenReturn(dto2);
        
        // When
        List<ProductResponseDTO> result = productService.getProductsByPriceRange(min, max);
        
        // Then
        assertThat(result).hasSize(2);
        
        verify(productRepository).findByPriceBetween(min, max);
        verify(productMapper, times(2)).toResponseDTO(any(Product.class));
    }
    
    @Test
    void getActiveProducts_ReturnsOnlyActiveProducts() {
        // Given
        Product p1 = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
        p1.setActive(true);
        
        Product p2 = new Product("iPhone", "Phone", new BigDecimal("1199.99"), 20);
        p2.setActive(true);
        
        ProductResponseDTO dto1 = new ProductResponseDTO();
        dto1.setName("MacBook");
        
        ProductResponseDTO dto2 = new ProductResponseDTO();
        dto2.setName("iPhone");
        
        when(productRepository.findByActiveTrue()).thenReturn(Arrays.asList(p1, p2));
        when(productMapper.toResponseDTO(p1)).thenReturn(dto1);
        when(productMapper.toResponseDTO(p2)).thenReturn(dto2);
        
        // When
        List<ProductResponseDTO> result = productService.getActiveProducts();
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponseDTO::getName)
            .containsExactly("MacBook", "iPhone");
        
        verify(productRepository).findByActiveTrue();
        verify(productMapper, times(2)).toResponseDTO(any(Product.class));
    }
    
    @Test
    void updateStock_Success() {
        // Given
        Category category = new Category("Electronics", "Devices");
        
        Product product = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
        product.setId(1L);
        product.setCategory(category);
        
        Product updatedProduct = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 25);
        updatedProduct.setId(1L);
        updatedProduct.setCategory(category);
        
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setStock(25);
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
        when(productMapper.toResponseDTO(updatedProduct)).thenReturn(responseDTO);
        
        // When
        ProductResponseDTO result = productService.updateStock(1L, 25, adminContext);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStock()).isEqualTo(25);
        
        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
        verify(productMapper).toResponseDTO(updatedProduct);
    }
    
    @Test
    void updateStock_ThrowsNotFoundException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> productService.updateStock(999L, 25, adminContext))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with id: 999");
        
        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void updateStock_ThrowsForbiddenException() {
        
        // When & Then
        assertThatThrownBy(() -> productService.updateStock(999L, 25, userContext))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Permission denied");
        
        verify(productRepository,never()).findById(anyLong());
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void deactivateProduct_Success() {
        // Given
        Category category = new Category("Electronics", "Devices");
        
        Product product = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
        product.setId(1L);
        product.setCategory(category);
        product.setActive(true);
        
        Product deactivatedProduct = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
        deactivatedProduct.setId(1L);
        deactivatedProduct.setCategory(category);
        deactivatedProduct.setActive(false);
        
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setActive(false);
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(deactivatedProduct);
        when(productMapper.toResponseDTO(deactivatedProduct)).thenReturn(responseDTO);
        
        // When
        ProductResponseDTO result = productService.deactivateProduct(1L,adminContext);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isFalse();
        
        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
        verify(productMapper).toResponseDTO(deactivatedProduct);
    }
    
    @Test
    void deactivateProduct_ThrowsNotFoundException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> productService.deactivateProduct(999L, adminContext))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with id: 999");
        
        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any());
    }
    
    @Test
    void deactivateProduct_ThrowsForbiddenException() {
        
        // When & Then
        assertThatThrownBy(() -> productService.deactivateProduct(999L, userContext))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Permission denied");
        
        verify(productRepository, never()).findById(anyLong());
        verify(productRepository, never()).save(any());
    }
}