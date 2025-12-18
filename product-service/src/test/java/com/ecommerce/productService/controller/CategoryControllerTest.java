package com.ecommerce.productService.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.productService.dto.CategoryRequestDTO;
import com.ecommerce.productService.dto.CategoryResponseDTO;
import com.ecommerce.productService.exception.DuplicateResourceException;
import com.ecommerce.productService.exception.GlobalExceptionHandler;
import com.ecommerce.productService.exception.ResourceNotFoundException;
import com.ecommerce.productService.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CategoryController.class)
@Import({CategoryController.class,GlobalExceptionHandler.class})
class CategoryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private CategoryService categoryService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void createCategory_ReturnsCreated() throws Exception {
        // Given
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("Electronics", "Devices");
        CategoryResponseDTO responseDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        when(categoryService.createCategory(any(CategoryRequestDTO.class)))
            .thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.name", is("Electronics")))
            .andExpect(jsonPath("$.description", is("Devices")));
        
        verify(categoryService).createCategory(any(CategoryRequestDTO.class));
    }
    
    @Test
    void createCategory_ReturnsBadRequest_WhenNameIsBlank() throws Exception {
        // Given
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("", "Description");
        
        // When & Then
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.error", is("Bad Request")));
        
        verify(categoryService, never()).createCategory(any());
    }
    
    @Test
    void createCategory_ReturnsBadRequest_WhenNameTooShort() throws Exception {
        // Given
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("A", "Description");
        
        // When & Then
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)));
        
        verify(categoryService, never()).createCategory(any());
    }
    
    @Test
    void createCategory_ReturnsConflict_WhenDuplicateName() throws Exception {
        // Given
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("Electronics", "Devices");
        
        when(categoryService.createCategory(any(CategoryRequestDTO.class)))
            .thenThrow(new DuplicateResourceException("Category already exists with name: Electronics"));
        
        // When & Then
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.error", is("Conflict")))
            .andExpect(jsonPath("$.message", is("Category already exists with name: Electronics")));
        
        verify(categoryService).createCategory(any(CategoryRequestDTO.class));
    }
    
    @Test
    void getAllCategories_ReturnsOk() throws Exception {
        // Given
        CategoryResponseDTO cat1 = new CategoryResponseDTO(1L, "Electronics", "Devices");
        CategoryResponseDTO cat2 = new CategoryResponseDTO(2L, "Books", "Publications");
        List<CategoryResponseDTO> categories = Arrays.asList(cat1, cat2);
        
        when(categoryService.getAllCategories()).thenReturn(categories);
        
        // When & Then
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id", is(1)))
            .andExpect(jsonPath("$[0].name", is("Electronics")))
            .andExpect(jsonPath("$[1].id", is(2)))
            .andExpect(jsonPath("$[1].name", is("Books")));
        
        verify(categoryService).getAllCategories();
    }
    
    @Test
    void getCategoryById_ReturnsOk() throws Exception {
        // Given
        CategoryResponseDTO responseDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        when(categoryService.getCategoryById(1L)).thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(get("/api/categories/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.name", is("Electronics")))
            .andExpect(jsonPath("$.description", is("Devices")));
        
        verify(categoryService).getCategoryById(1L);
    }
    
    @Test
    void getCategoryById_ReturnsNotFound() throws Exception {
        // Given
        when(categoryService.getCategoryById(999L))
            .thenThrow(new ResourceNotFoundException("Category not found with id: 999"));
        
        // When & Then
        mockMvc.perform(get("/api/categories/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.error", is("Not Found")))
            .andExpect(jsonPath("$.message", is("Category not found with id: 999")));
        
        verify(categoryService).getCategoryById(999L);
    }
    
    @Test
    void updateCategory_ReturnsOk() throws Exception {
        // Given
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("Electronics Updated", "New description");
        CategoryResponseDTO responseDTO = new CategoryResponseDTO(1L, "Electronics Updated", "New description");
        
        when(categoryService.updateCategory(eq(1L), any(CategoryRequestDTO.class)))
            .thenReturn(responseDTO);
        
        // When & Then
        mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.name", is("Electronics Updated")))
            .andExpect(jsonPath("$.description", is("New description")));
        
        verify(categoryService).updateCategory(eq(1L), any(CategoryRequestDTO.class));
    }
    
    @Test
    void updateCategory_ReturnsNotFound() throws Exception {
        // Given
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("Electronics", "Description");
        
        when(categoryService.updateCategory(eq(999L), any(CategoryRequestDTO.class)))
            .thenThrow(new ResourceNotFoundException("Category not found with id: 999"));
        
        // When & Then
        mockMvc.perform(put("/api/categories/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", is("Category not found with id: 999")));
        
        verify(categoryService).updateCategory(eq(999L), any(CategoryRequestDTO.class));
    }
    
    @Test
    void updateCategory_ReturnsBadRequest_WhenValidationFails() throws Exception {
        // Given
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("", "Description");
        
        // When & Then
        mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)));
        
        verify(categoryService, never()).updateCategory(any(), any());
    }
    
    @Test
    void deleteCategory_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(categoryService).deleteCategory(1L);
        
        // When & Then
        mockMvc.perform(delete("/api/categories/1"))
            .andExpect(status().isNoContent());
        
        verify(categoryService).deleteCategory(1L);
    }
    
    @Test
    void deleteCategory_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Category not found with id: 999"))
            .when(categoryService).deleteCategory(999L);
        
        // When & Then
        mockMvc.perform(delete("/api/categories/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.message", is("Category not found with id: 999")));
        
        verify(categoryService).deleteCategory(999L);
    }
}