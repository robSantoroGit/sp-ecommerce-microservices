package com.ecommerce.productService.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

import com.ecommerce.productService.model.Category;

@DataJpaTest  // Configura JPA per test, usa H2
@ContextConfiguration(classes = {TestConfig.class})
public class CategoryRepositoryTest {
	
	@Autowired
	private CategoryRepository categoryRepository; 
	
	@Autowired
    private TestEntityManager entityManager;
	
	@Test
	void testSaveCategory() {
	    // Given
	    Category category = new Category("Electronics", "Electronic devices");
	    
	    // When
	    Category saved = categoryRepository.save(category);
	    
	    // Then
	    assertThat(saved.getId()).isNotNull();
	    assertThat(saved.getName()).isEqualTo(category.getName());
	    assertThat(saved.getDescription()).isEqualTo(category.getDescription());
	}
	
	@Test
    void testFindById() {
        // Given
        Category category = new Category("Books", "Books and publications");
        entityManager.persistAndFlush(category);
        
        // When
        Optional<Category> found = categoryRepository.findById(category.getId());
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(category.getName());
        assertThat(found.get().getDescription()).isEqualTo(category.getDescription());
    }
	
	@Test
    void testFindAll() {
        // Given
        entityManager.persist(new Category("Electronics", "Devices"));
        entityManager.persist(new Category("Books", "Publications"));
        entityManager.persist(new Category("Clothing", "Fashion"));
        entityManager.flush();
        
        // When
        List<Category> categories = categoryRepository.findAll();
        
        // Then
        assertThat(categories).hasSize(3);
        assertThat(categories)
            .extracting(Category::getName)
            .containsExactlyInAnyOrder("Electronics", "Books", "Clothing");
    }
    
    @Test
    void testFindByName_Found() {
        // Given
        entityManager.persist(new Category("Electronics", "Electronic devices"));
        entityManager.flush();
        
        // When
        Optional<Category> found = categoryRepository.findByName("Electronics");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Electronics");
        assertThat(found.get().getDescription()).isEqualTo("Electronic devices");
    }
    
    @Test
    void testFindByName_NotFound() {
        // When
        Optional<Category> found = categoryRepository.findByName("NonExistent");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void testExistsByName_True() {
        // Given
        entityManager.persist(new Category("TestCategory", "Test description"));
        entityManager.flush();
        
        // When
        boolean exists = categoryRepository.existsByName("TestCategory");
        
        // Then
        assertThat(exists).isTrue();
    }
    
    @Test
    void testExistsByName_False() {
        // When
        boolean exists = categoryRepository.existsByName("NonExistent");
        
        // Then
        assertThat(exists).isFalse();
    }

}
