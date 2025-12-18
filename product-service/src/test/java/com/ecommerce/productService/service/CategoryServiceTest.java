package com.ecommerce.productService.service;

import com.ecommerce.productService.dto.CategoryMapper;
import com.ecommerce.productService.dto.CategoryRequestDTO;
import com.ecommerce.productService.dto.CategoryResponseDTO;
import com.ecommerce.productService.exception.DuplicateResourceException;
import com.ecommerce.productService.exception.ResourceNotFoundException;
import com.ecommerce.productService.model.Category;
import com.ecommerce.productService.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @Mock
    private CategoryMapper categoryMapper;
    
    @InjectMocks
    private CategoryServiceImpl categoryService;
    
    @Test
    void createCategory_Success() {
        // Given
        CategoryRequestDTO dto = new CategoryRequestDTO("Electronics", "Devices");
        Category category = new Category("Electronics", "Devices");
        category.setId(1L);
        CategoryResponseDTO responseDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryMapper.toEntity(dto)).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toResponseDTO(category)).thenReturn(responseDTO);
        
        // When
        CategoryResponseDTO result = categoryService.createCategory(dto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
        
        verify(categoryRepository).existsByName("Electronics");
        verify(categoryMapper).toEntity(dto);
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toResponseDTO(category);
    }
    
    @Test
    void createCategory_ThrowsDuplicateException() {
        // Given
        CategoryRequestDTO dto = new CategoryRequestDTO("Electronics", "Devices");
        
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> categoryService.createCategory(dto))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Category already exists with name: Electronics");
        
        verify(categoryRepository).existsByName("Electronics");
        verify(categoryMapper, never()).toEntity(any());
        verify(categoryRepository, never()).save(any());
    }
    
    @Test
    void getAllCategories_ReturnsAllCategories() {
        // Given
        Category cat1 = new Category("Electronics", "Devices");
        cat1.setId(1L);
        Category cat2 = new Category("Books", "Publications");
        cat2.setId(2L);
        
        CategoryResponseDTO dto1 = new CategoryResponseDTO(1L, "Electronics", "Devices");
        CategoryResponseDTO dto2 = new CategoryResponseDTO(2L, "Books", "Publications");
        
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(cat1, cat2));
        when(categoryMapper.toResponseDTO(cat1)).thenReturn(dto1);
        when(categoryMapper.toResponseDTO(cat2)).thenReturn(dto2);
        
        // When
        List<CategoryResponseDTO> result = categoryService.getAllCategories();
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryResponseDTO::getName)
            .containsExactly("Electronics", "Books");
        
        verify(categoryRepository).findAll();
        verify(categoryMapper, times(2)).toResponseDTO(any(Category.class));
    }
    
    @Test
    void getCategoryById_Success() {
        // Given
        Category category = new Category("Electronics", "Devices");
        category.setId(1L);
        CategoryResponseDTO responseDTO = new CategoryResponseDTO(1L, "Electronics", "Devices");
        
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponseDTO(category)).thenReturn(responseDTO);
        
        // When
        CategoryResponseDTO result = categoryService.getCategoryById(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
        
        verify(categoryRepository).findById(1L);
        verify(categoryMapper).toResponseDTO(category);
    }
    
    @Test
    void getCategoryById_ThrowsNotFoundException() {
        // Given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> categoryService.getCategoryById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category not found with id: 999");
        
        verify(categoryRepository).findById(999L);
        verify(categoryMapper, never()).toResponseDTO(any());
    }
    
    @Test
    void updateCategory_Success() {
        // Given
        Category existingCategory = new Category("Electronics", "Old description");
        existingCategory.setId(1L);
        
        CategoryRequestDTO dto = new CategoryRequestDTO("Electronics", "New description");
        
        Category updatedCategory = new Category("Electronics", "New description");
        updatedCategory.setId(1L);
        
        CategoryResponseDTO responseDTO = new CategoryResponseDTO(1L, "Electronics", "New description");
        
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);
        when(categoryMapper.toResponseDTO(updatedCategory)).thenReturn(responseDTO);
        
        // When
        CategoryResponseDTO result = categoryService.updateCategory(1L, dto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("New description");
        
        verify(categoryRepository).findById(1L);
        verify(categoryMapper).updateEntityFromDTO(existingCategory, dto);
        verify(categoryRepository).save(existingCategory);
        verify(categoryMapper).toResponseDTO(updatedCategory);
    }
    
    @Test
    void updateCategory_ThrowsNotFoundException() {
        // Given
        CategoryRequestDTO dto = new CategoryRequestDTO("Electronics", "Description");
        
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(999L, dto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category not found with id: 999");
        
        verify(categoryRepository).findById(999L);
        verify(categoryMapper, never()).updateEntityFromDTO(any(), any());
        verify(categoryRepository, never()).save(any());
    }
    
    @Test
    void updateCategory_ThrowsDuplicateException_WhenNameChangedToExistingName() {
        // Given
        Category existingCategory = new Category("Electronics", "Description");
        existingCategory.setId(1L);
        
        Category otherCategory = new Category("Books", "Publications");
        otherCategory.setId(2L);
        
        CategoryRequestDTO dto = new CategoryRequestDTO("Books", "New description");
        
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findByName("Books")).thenReturn(Optional.of(otherCategory));
        
        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, dto))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Category already exists with name: Books");
        
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).findByName("Books");
        verify(categoryMapper, never()).updateEntityFromDTO(any(), any());
        verify(categoryRepository, never()).save(any());
    }
    
    @Test
    void updateCategory_Success_WhenNameUnchanged() {
        // Given
        Category existingCategory = new Category("Electronics", "Old description");
        existingCategory.setId(1L);
        
        CategoryRequestDTO dto = new CategoryRequestDTO("Electronics", "New description");
        
        Category updatedCategory = new Category("Electronics", "New description");
        updatedCategory.setId(1L);
        
        CategoryResponseDTO responseDTO = new CategoryResponseDTO(1L, "Electronics", "New description");
        
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);
        when(categoryMapper.toResponseDTO(updatedCategory)).thenReturn(responseDTO);
        
        // When
        CategoryResponseDTO result = categoryService.updateCategory(1L, dto);
        
        // Then
        assertThat(result).isNotNull();
        
        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never()).findByName(any());  // Name unchanged, no duplicate check
        verify(categoryMapper).updateEntityFromDTO(existingCategory, dto);
        verify(categoryRepository).save(existingCategory);
    }
    
    @Test
    void deleteCategory_Success() {
        // Given
        when(categoryRepository.existsById(1L)).thenReturn(true);
        
        // When
        categoryService.deleteCategory(1L);
        
        // Then
        verify(categoryRepository).existsById(1L);
        verify(categoryRepository).deleteById(1L);
    }
    
    @Test
    void deleteCategory_ThrowsNotFoundException() {
        // Given
        when(categoryRepository.existsById(999L)).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category not found with id: 999");
        
        verify(categoryRepository).existsById(999L);
        verify(categoryRepository, never()).deleteById(any());
    }
}