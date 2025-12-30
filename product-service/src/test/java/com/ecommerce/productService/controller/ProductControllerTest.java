package com.ecommerce.productService.controller;

import com.ecommerce.productService.dto.CategoryResponseDTO;
import com.ecommerce.productService.dto.ProductRequestDTO;
import com.ecommerce.productService.dto.ProductResponseDTO;
import com.ecommerce.productService.dto.SecurityContext;
import com.ecommerce.productService.exception.ForbiddenException;
import com.ecommerce.productService.exception.GlobalExceptionHandler;
import com.ecommerce.productService.exception.ResourceNotFoundException;
import com.ecommerce.productService.security.Permission;
import com.ecommerce.productService.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({ProductController.class,GlobalExceptionHandler.class})
class ProductControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ProductService productService;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void createProduct_ReturnsCreated() throws Exception {
        // Given
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10, 1L
        );
        
        CategoryResponseDTO categoryDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook Pro");
        responseDTO.setPrice(new BigDecimal("2499.99"));
        responseDTO.setStock(10);
        responseDTO.setCategory(categoryDTO);
        responseDTO.setActive(true);
        
        when(productService.createProduct(any(ProductRequestDTO.class), any(SecurityContext.class)))
            .thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.name", is("MacBook Pro")))
            .andExpect(jsonPath("$.price", is(2499.99)))
            .andExpect(jsonPath("$.stock", is(10)))
            .andExpect(jsonPath("$.category.name", is("Electronics")))
            .andExpect(jsonPath("$.active", is(true)));
        
        verify(productService).createProduct(any(ProductRequestDTO.class), any(SecurityContext.class));
    }
    
    @Test
    void createProduct_ReturnsBadRequest_WhenValidationFails() throws Exception {
        // Given - invalid DTO (name blank, price = 0)
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "", "Description", new BigDecimal("0"), 10, 1L
        );
        
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)));
        
        verify(productService, never()).createProduct(any(),any());
    }
    
    @Test
    void createProduct_ReturnsNotFound_WhenCategoryNotFound() throws Exception {
        // Given
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10, 999L
        );
        
        when(productService.createProduct(any(ProductRequestDTO.class), any(SecurityContext.class)))
            .thenThrow(new ResourceNotFoundException("Category not found with id: 999"));
        
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", is("Category not found with id: 999")));
        
        verify(productService).createProduct(any(ProductRequestDTO.class),any(SecurityContext.class));
    }
    
    @Test
    void createProduct_ReturnsForbidden() throws Exception {
        // Given
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10, 999L
        );
        
        when(productService.createProduct(any(ProductRequestDTO.class), any(SecurityContext.class)))
            .thenThrow(new ForbiddenException("Permission denied"));
        
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", ""))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.message", is("Permission denied")));
        
        verify(productService).createProduct(any(ProductRequestDTO.class),any(SecurityContext.class));
    }
    
    @Test
    void getAllProducts_ReturnsOk() throws Exception {
        // Given
        CategoryResponseDTO categoryDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        ProductResponseDTO p1 = new ProductResponseDTO();
        p1.setId(1L);
        p1.setName("MacBook");
        p1.setCategory(categoryDTO);
        
        ProductResponseDTO p2 = new ProductResponseDTO();
        p2.setId(2L);
        p2.setName("iPhone");
        p2.setCategory(categoryDTO);
        
        List<ProductResponseDTO> products = Arrays.asList(p1, p2);
        
        when(productService.getAllProducts()).thenReturn(products);
        
        // When & Then
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name", is("MacBook")))
            .andExpect(jsonPath("$[1].name", is("iPhone")));
        
        verify(productService).getAllProducts();
    }
    
    @Test
    void getProductById_ReturnsOk() throws Exception {
        // Given
        CategoryResponseDTO categoryDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook Pro");
        responseDTO.setCategory(categoryDTO);
        
        when(productService.getProductById(1L)).thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.name", is("MacBook Pro")))
            .andExpect(jsonPath("$.category.name", is("Electronics")));
        
        verify(productService).getProductById(1L);
    }
    
    @Test
    void getProductById_ReturnsNotFound() throws Exception {
        // Given
        when(productService.getProductById(999L))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));
        
        // When & Then
        mockMvc.perform(get("/api/products/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", is("Product not found with id: 999")));
        
        verify(productService).getProductById(999L);
    }
    
    @Test
    void updateProduct_ReturnsOk() throws Exception {
        // Given
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "MacBook Pro 16", "Updated description", new BigDecimal("2999.99"), 15, 1L
        );
        
        CategoryResponseDTO categoryDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook Pro 16");
        responseDTO.setPrice(new BigDecimal("2999.99"));
        responseDTO.setCategory(categoryDTO);
        
        when(productService.updateProduct(eq(1L), any(ProductRequestDTO.class), any(SecurityContext.class)))
            .thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.name", is("MacBook Pro 16")))
            .andExpect(jsonPath("$.price", is(2999.99)));
        
        verify(productService).updateProduct(eq(1L), any(ProductRequestDTO.class), any(SecurityContext.class));
    }
    
    @Test
    void updateProduct_ReturnsNotFound() throws Exception {
        // Given
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "MacBook", "Description", new BigDecimal("2499.99"), 10, 1L
        );
        
        when(productService.updateProduct(eq(999L), any(ProductRequestDTO.class), any(SecurityContext.class)))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));
        
        // When & Then
        mockMvc.perform(put("/api/products/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", is("Product not found with id: 999")));
        
        verify(productService).updateProduct(eq(999L), any(ProductRequestDTO.class), any(SecurityContext.class));
    }
    
    @Test
    void updateProduct_ReturnsForbidden() throws Exception {
        // Given
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10, 999L
        );
        
        when(productService.updateProduct(anyLong(), any(ProductRequestDTO.class), any(SecurityContext.class)))
            .thenThrow(new ForbiddenException("Permission denied"));
        
        // When & Then
        mockMvc.perform(put("/api/products/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", ""))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.message", is("Permission denied")));
        
        verify(productService).updateProduct(anyLong(), any(ProductRequestDTO.class), any(SecurityContext.class));
    }
    
    @Test
    void deleteProduct_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(productService).deleteProduct(eq(1L),any(SecurityContext.class));
        
        // When & Then
        mockMvc.perform(delete("/api/products/1")
        		.header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_DELETE))
            .andExpect(status().isNoContent());
        
        verify(productService).deleteProduct(eq(1L),any());
    }
    
    @Test
    void deleteProduct_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Product not found with id: 999"))
            .when(productService).deleteProduct(eq(999L),any(SecurityContext.class));
        
        // When & Then
        mockMvc.perform(delete("/api/products/999")
        		.header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_DELETE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
        
        verify(productService).deleteProduct(eq(999L),any());
    }
    
    @Test
    void deleteProduct_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new ForbiddenException("Access denied"))
            .when(productService).deleteProduct(eq(999L),any(SecurityContext.class));
        
        // When & Then
        mockMvc.perform(delete("/api/products/999")
        		.header("X-User-Id", "999")
                .header("X-User-Scopes", ""))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)));
        
        verify(productService).deleteProduct(eq(999L),any());
    }
    
    @Test
    void getProductsByCategory_ReturnsOk() throws Exception {
        // Given
        CategoryResponseDTO categoryDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        ProductResponseDTO p1 = new ProductResponseDTO();
        p1.setName("MacBook");
        p1.setCategory(categoryDTO);
        
        ProductResponseDTO p2 = new ProductResponseDTO();
        p2.setName("iPhone");
        p2.setCategory(categoryDTO);
        
        List<ProductResponseDTO> products = Arrays.asList(p1, p2);
        
        when(productService.getProductsByCategory(1L)).thenReturn(products);
        
        // When & Then
        mockMvc.perform(get("/api/products/category/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name", is("MacBook")))
            .andExpect(jsonPath("$[1].name", is("iPhone")));
        
        verify(productService).getProductsByCategory(1L);
    }
    
    @Test
    void getProductsByCategory_ReturnsNotFound_WhenCategoryNotFound() throws Exception {
        // Given
        when(productService.getProductsByCategory(999L))
            .thenThrow(new ResourceNotFoundException("Category not found with id: 999"));
        
        // When & Then
        mockMvc.perform(get("/api/products/category/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
        
        verify(productService).getProductsByCategory(999L);
    }
    
    @Test
    void searchProducts_ReturnsOk() throws Exception {
        // Given
        ProductResponseDTO p1 = new ProductResponseDTO();
        p1.setName("Magic Mouse");
        
        ProductResponseDTO p2 = new ProductResponseDTO();
        p2.setName("Magic Keyboard");
        
        List<ProductResponseDTO> products = Arrays.asList(p1, p2);
        
        when(productService.searchProducts("magic")).thenReturn(products);
        
        // When & Then
        mockMvc.perform(get("/api/products/search?keyword=magic"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name", is("Magic Mouse")))
            .andExpect(jsonPath("$[1].name", is("Magic Keyboard")));
        
        verify(productService).searchProducts("magic");
    }
    
    @Test
    void getProductsByPriceRange_ReturnsOk() throws Exception {
        // Given
        ProductResponseDTO p1 = new ProductResponseDTO();
        p1.setName("Mouse");
        p1.setPrice(new BigDecimal("79.99"));
        
        ProductResponseDTO p2 = new ProductResponseDTO();
        p2.setName("Keyboard");
        p2.setPrice(new BigDecimal("149.99"));
        
        List<ProductResponseDTO> products = Arrays.asList(p1, p2);
        
        when(productService.getProductsByPriceRange(new BigDecimal("50"), new BigDecimal("200")))
            .thenReturn(products);
        
        // When & Then
        mockMvc.perform(get("/api/products/price-range?min=50&max=200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name", is("Mouse")))
            .andExpect(jsonPath("$[1].name", is("Keyboard")));
        
        verify(productService).getProductsByPriceRange(new BigDecimal("50"), new BigDecimal("200"));
    }
    
    @Test
    void getActiveProducts_ReturnsOk() throws Exception {
        // Given
        ProductResponseDTO p1 = new ProductResponseDTO();
        p1.setName("MacBook");
        p1.setActive(true);
        
        ProductResponseDTO p2 = new ProductResponseDTO();
        p2.setName("iPhone");
        p2.setActive(true);
        
        List<ProductResponseDTO> products = Arrays.asList(p1, p2);
        
        when(productService.getActiveProducts()).thenReturn(products);
        
        // When & Then
        mockMvc.perform(get("/api/products/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].active", is(true)))
            .andExpect(jsonPath("$[1].active", is(true)));
        
        verify(productService).getActiveProducts();
    }
    
    @Test
    void updateStock_ReturnsOk() throws Exception {
        // Given
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook");
        responseDTO.setStock(25);
        
        when(productService.updateStock(eq(1L),eq( 25), any(SecurityContext.class))).thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(patch("/api/products/1/stock?quantity=25")
        		.header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.stock", is(25)));
        
        verify(productService).updateStock(eq(1L), eq(25), any());
    }
    
    @Test
    void updateStock_ReturnsNotFound() throws Exception {
        // Given
        when(productService.updateStock(eq(999L), eq(25), any(SecurityContext.class)))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));
        
        // When & Then
        mockMvc.perform(patch("/api/products/999/stock?quantity=25")
        		.header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
        
        verify(productService).updateStock(eq(999L), eq(25), any());
    }
    
    @Test
    void deactivateProduct_ReturnsOk() throws Exception {
        // Given
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook");
        responseDTO.setActive(false);
        
        when(productService.deactivateProduct(eq(1L), any(SecurityContext.class))).thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(patch("/api/products/1/deactivate")
        		.header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.active", is(false)));
        
        verify(productService).deactivateProduct(eq(1L), any());
    }
    
    @Test
    void deactivateProduct_ReturnsNotFound() throws Exception {
        // Given
        when(productService.deactivateProduct(eq(999L),any(SecurityContext.class)))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));
        
        // When & Then
        mockMvc.perform(patch("/api/products/999/deactivate")
        		.header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.PRODUCT_WRITE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
        
        verify(productService).deactivateProduct(eq(999L),any());
    }
}