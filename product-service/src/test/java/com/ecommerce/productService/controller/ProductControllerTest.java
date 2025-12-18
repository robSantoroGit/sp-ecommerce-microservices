package com.ecommerce.productService.controller;

import com.ecommerce.productService.dto.CategoryResponseDTO;
import com.ecommerce.productService.dto.ProductRequestDTO;
import com.ecommerce.productService.dto.ProductResponseDTO;
import com.ecommerce.productService.exception.GlobalExceptionHandler;
import com.ecommerce.productService.exception.ResourceNotFoundException;
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
        
        when(productService.createProduct(any(ProductRequestDTO.class)))
            .thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.name", is("MacBook Pro")))
            .andExpect(jsonPath("$.price", is(2499.99)))
            .andExpect(jsonPath("$.stock", is(10)))
            .andExpect(jsonPath("$.category.name", is("Electronics")))
            .andExpect(jsonPath("$.active", is(true)));
        
        verify(productService).createProduct(any(ProductRequestDTO.class));
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
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)));
        
        verify(productService, never()).createProduct(any());
    }
    
    @Test
    void createProduct_ReturnsNotFound_WhenCategoryNotFound() throws Exception {
        // Given
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 10, 999L
        );
        
        when(productService.createProduct(any(ProductRequestDTO.class)))
            .thenThrow(new ResourceNotFoundException("Category not found with id: 999"));
        
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", is("Category not found with id: 999")));
        
        verify(productService).createProduct(any(ProductRequestDTO.class));
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
        
        when(productService.updateProduct(eq(1L), any(ProductRequestDTO.class)))
            .thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.name", is("MacBook Pro 16")))
            .andExpect(jsonPath("$.price", is(2999.99)));
        
        verify(productService).updateProduct(eq(1L), any(ProductRequestDTO.class));
    }
    
    @Test
    void updateProduct_ReturnsNotFound() throws Exception {
        // Given
        ProductRequestDTO requestDTO = new ProductRequestDTO(
            "MacBook", "Description", new BigDecimal("2499.99"), 10, 1L
        );
        
        when(productService.updateProduct(eq(999L), any(ProductRequestDTO.class)))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));
        
        // When & Then
        mockMvc.perform(put("/api/products/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", is("Product not found with id: 999")));
        
        verify(productService).updateProduct(eq(999L), any(ProductRequestDTO.class));
    }
    
    @Test
    void deleteProduct_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(productService).deleteProduct(1L);
        
        // When & Then
        mockMvc.perform(delete("/api/products/1"))
            .andExpect(status().isNoContent());
        
        verify(productService).deleteProduct(1L);
    }
    
    @Test
    void deleteProduct_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Product not found with id: 999"))
            .when(productService).deleteProduct(999L);
        
        // When & Then
        mockMvc.perform(delete("/api/products/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
        
        verify(productService).deleteProduct(999L);
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
        
        when(productService.updateStock(1L, 25)).thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(patch("/api/products/1/stock?quantity=25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.stock", is(25)));
        
        verify(productService).updateStock(1L, 25);
    }
    
    @Test
    void updateStock_ReturnsNotFound() throws Exception {
        // Given
        when(productService.updateStock(999L, 25))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));
        
        // When & Then
        mockMvc.perform(patch("/api/products/999/stock?quantity=25"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
        
        verify(productService).updateStock(999L, 25);
    }
    
    @Test
    void deactivateProduct_ReturnsOk() throws Exception {
        // Given
        ProductResponseDTO responseDTO = new ProductResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("MacBook");
        responseDTO.setActive(false);
        
        when(productService.deactivateProduct(1L)).thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(patch("/api/products/1/deactivate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.active", is(false)));
        
        verify(productService).deactivateProduct(1L);
    }
    
    @Test
    void deactivateProduct_ReturnsNotFound() throws Exception {
        // Given
        when(productService.deactivateProduct(999L))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));
        
        // When & Then
        mockMvc.perform(patch("/api/products/999/deactivate"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
        
        verify(productService).deactivateProduct(999L);
    }
}