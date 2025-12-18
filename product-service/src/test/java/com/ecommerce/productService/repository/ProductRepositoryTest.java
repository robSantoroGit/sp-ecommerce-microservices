package com.ecommerce.productService.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

import com.ecommerce.productService.model.Category;
import com.ecommerce.productService.model.Product;

@DataJpaTest
@ContextConfiguration(classes = TestConfig.class)
public class ProductRepositoryTest {
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private CategoryRepository categoryRepository;
	
	@Autowired
	private TestEntityManager entityManager;
	
	@Test
	void testSaveProduct() {
	    // Given
	    Category category = entityManager.persist(new Category("Electronics", "Devices"));
	    entityManager.flush();
	    
	    Product product = new Product("MacBook Pro", "Apple laptop", new BigDecimal("2499.99"), 15);
	    product.setCategory(category);
	    
	    // When
	    Product saved = productRepository.save(product);
	    
	    // Then
	    assertThat(saved.getId()).isNotNull();
	    assertThat(saved.getName()).isEqualTo(product.getName());
	    assertThat(saved.getPrice()).isEqualByComparingTo(product.getPrice());
	    assertThat(saved.getStock()).isEqualTo(product.getStock());
	    assertThat(saved.getCategory()).isNotNull();
	    assertThat(saved.getCategory().getName()).isEqualTo(product.getCategory().getName());
	    assertThat(saved.isActive()).isTrue();
	    assertThat(saved.getCreatedAt()).isNotNull();
	    assertThat(saved.getUpdatedAt()).isNotNull();
	}
	
	@Test
	void testFindById() {
	    // Given
	    Category category = entityManager.persist(new Category("Books", "Publications"));
	    Product product = new Product("Clean Code", "Programming book",new BigDecimal("45.99"), 100);
	    product.setCategory(category);
	    entityManager.persist(product);
	    entityManager.flush();
	    
	    // When
	    Optional<Product> found = productRepository.findById(product.getId());
	    
	    // Then
	    assertThat(found).isPresent();
	    assertThat(found.get().getName()).isEqualTo(product.getName());
	    assertThat(found.get().getCategory().getName()).isEqualTo(product.getCategory().getName());
	}

	
	@Test
	void testFindAll() {
	    // Given
	    Category electronics = entityManager.persist(new Category("Electronics", "Devices"));
	    Category books = entityManager.persist(new Category("Books", "Publications"));
	    entityManager.flush();
	    
	    Product p1 = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
	    p1.setCategory(electronics);
	    entityManager.persist(p1);
	    
	    Product p2 = new Product("iPhone", "Phone", new BigDecimal("1199.99"), 20);
	    p2.setCategory(electronics);
	    entityManager.persist(p2);
	    
	    Product p3 = new Product("Clean Code", "Book", new BigDecimal("45.99"), 100);
	    p3.setCategory(books);
	    entityManager.persist(p3);
	    
	    entityManager.flush();
	    
	    // When
	    List<Product> products = productRepository.findAll();
	    
	    // Then
	    assertThat(products).hasSize(3);
	    assertThat(products).extracting(Product::getName).containsExactlyInAnyOrder(p1.getName(), p2.getName(), p3.getName());
	}
	
	@Test
	void testFindByCategory() {
	    // Given
	    Category electronics = entityManager.persist(new Category("Electronics", "Devices"));
	    Category books = entityManager.persist(new Category("Books", "Publications"));
	    entityManager.flush();
	    
	    Product p1 = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
	    p1.setCategory(electronics);
	    entityManager.persist(p1);
	    
	    Product p2 = new Product("iPhone", "Phone", new BigDecimal("1199.99"), 20);
	    p2.setCategory(electronics);
	    entityManager.persist(p2);
	    
	    Product p3 = new Product("Clean Code", "Book", new BigDecimal("45.99"), 100);
	    p3.setCategory(books);
	    entityManager.persist(p3);
	    
	    entityManager.flush();
	    
	    // When
	    List<Product> electronicsProducts = productRepository.findByCategory(electronics);
	    
	    // Then
	    assertThat(electronicsProducts).hasSize(2);
	    assertThat(electronicsProducts).extracting(Product::getName).containsExactlyInAnyOrder(p1.getName(),p2.getName());
	}
	
	@Test
	void testFindByNameContainingIgnoreCase() {
	    // Given
	    Category category = entityManager.persist(new Category("Electronics", "Devices"));
	    entityManager.flush();
	    
	    Product p1 = new Product("Magic Mouse", "Wireless mouse", new BigDecimal("79.99"), 50);
	    p1.setCategory(category);
	    entityManager.persist(p1);
	    
	    Product p2 = new Product("Magic Keyboard", "Wireless keyboard", new BigDecimal("149.99"), 30);
	    p2.setCategory(category);
	    entityManager.persist(p2);
	    
	    Product p3 = new Product("MacBook Pro", "Laptop", new BigDecimal("2499.99"), 10);
	    p3.setCategory(category);
	    entityManager.persist(p3);
	    
	    entityManager.flush();
	    
	    // When
	    List<Product> magicProducts = productRepository.findByNameContainingIgnoreCase("magic");
	    
	    // Then
	    assertThat(magicProducts).hasSize(2);
	    assertThat(magicProducts).extracting(Product::getName).containsExactlyInAnyOrder(p1.getName(), p2.getName());
	}
	
	@Test
	void testFindByPriceBetween() {
	    // Given
	    Category category = entityManager.persist(new Category("Electronics", "Devices"));
	    entityManager.flush();
	    
	    Product p1 = new Product("Magic Mouse", "Mouse", new BigDecimal("79.99"), 50);
	    p1.setCategory(category);
	    entityManager.persist(p1);
	    
	    Product p2 = new Product("Magic Keyboard", "Keyboard", new BigDecimal("149.99"), 30);
	    p2.setCategory(category);
	    entityManager.persist(p2);
	    
	    Product p3 = new Product("MacBook Pro", "Laptop", new BigDecimal("2499.99"), 10);
	    p3.setCategory(category);
	    entityManager.persist(p3);
	    
	    entityManager.flush();
	    
	    // When
	    List<Product> affordableProducts = productRepository.findByPriceBetween(new BigDecimal("50"), new BigDecimal("200"));
	    
	    // Then
	    assertThat(affordableProducts).hasSize(2);
	    assertThat(affordableProducts).extracting(Product::getName).containsExactlyInAnyOrder(p1.getName(), p2.getName());
	}
	
	@Test
	void testFindByActiveTrue() {
	    // Given
	    Category category = entityManager.persist(new Category("Electronics", "Devices"));
	    entityManager.flush();
	    
	    Product p1 = new Product("MacBook Pro", "Laptop", new BigDecimal("2499.99"), 10);
	    p1.setCategory(category);
	    entityManager.persist(p1);
	    
	    Product p2 = new Product("iPhone 15", "Phone", new BigDecimal("1199.99"), 20);
	    p2.setCategory(category);
	    entityManager.persist(p2);
	    
	    Product p3 = new Product("iPhone 14", "Old phone", new BigDecimal("899.99"), 0);
	    p3.setCategory(category);
	    p3.setActive(false);  // Inactive
	    entityManager.persist(p3);
	    
	    entityManager.flush();
	    
	    // When
	    List<Product> activeProducts = productRepository.findByActiveTrue();
	    
	    // Then
	    assertThat(activeProducts).hasSize(2);
	    assertThat(activeProducts).extracting(Product::getName).containsExactlyInAnyOrder(p1.getName(), p2.getName());
	    assertThat(activeProducts).allMatch(Product::isActive);
	}
	
	@Test
	void testEntityGraphLoadsCategory() {
	    // Given
	    Category category = entityManager.persist(new Category("Electronics", "Devices"));
	    entityManager.flush();
	    
	    Product p1 = new Product("MacBook", "Laptop", new BigDecimal("2499.99"), 10);
	    p1.setCategory(category);
	    entityManager.persist(p1);
	    
	    Product p2 = new Product("iPhone", "Phone", new BigDecimal("1199.99"), 20);
	    p2.setCategory(category);
	    entityManager.persist(p2);
	    
	    entityManager.flush();
	    entityManager.clear();  // Clear persistence context
	    
	    // When
	    List<Product> products = productRepository.findByActiveTrue();
	    
	    // Then
	    assertThat(products).hasSize(2);
	    
	    // Accessing category should NOT trigger additional query (already loaded by @EntityGraph)
	    for (Product product : products) {
	        assertThat(product.getCategory()).isNotNull();
	        assertThat(product.getCategory().getName()).isEqualTo(category.getName());
	    }
	    
	    // If @EntityGraph working correctly, no LazyInitializationException
	}
}
