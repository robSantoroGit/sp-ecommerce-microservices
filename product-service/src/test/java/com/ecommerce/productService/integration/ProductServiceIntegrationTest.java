package com.ecommerce.productService.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ecommerce.productService.bin.ProductServiceApplication;
import com.ecommerce.productService.dto.CategoryRequestDTO;
import com.ecommerce.productService.dto.CategoryResponseDTO;
import com.ecommerce.productService.dto.ErrorResponse;
import com.ecommerce.productService.dto.ProductRequestDTO;
import com.ecommerce.productService.dto.ProductResponseDTO;
import com.ecommerce.productService.repository.CategoryRepository;
import com.ecommerce.productService.repository.ProductRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ProductServiceApplication.class)
@TestPropertySource(locations = "classpath:application-test.yml")
@ActiveProfiles("test")
class ProductServiceIntegrationTest {
    
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Value("${test.server.host:localhost}")  
    private String host;
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
    	restTemplate = new TestRestTemplate(new RestTemplateBuilder().rootUri("http://" + host + ":" + port));
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }
    
    @Test
    void verifyGlobalExceptionHandlerIsLoaded() {
        // Verify GlobalExceptionHandler bean exists
        boolean handlerExists = applicationContext.containsBean("globalExceptionHandler");
        System.out.println("GlobalExceptionHandler loaded: " + handlerExists);
        
        assertThat(handlerExists).isTrue();
    }
    
    @Test
    void fullCategoryCRUD_WorksEndToEnd() {
        // 1. CREATE Category
        CategoryRequestDTO createRequest = new CategoryRequestDTO("Electronics", "Electronic devices");
        
        ResponseEntity<CategoryResponseDTO> createResponse = restTemplate.postForEntity(
            "/api/categories",
            createRequest,
            CategoryResponseDTO.class
        );
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("Electronics");
        
        Long categoryId = createResponse.getBody().getId();
        
        // 2. GET Category by ID
        ResponseEntity<CategoryResponseDTO> getResponse = restTemplate.getForEntity(
            "/api/categories/" + categoryId,
            CategoryResponseDTO.class
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("Electronics");
        
        // 3. UPDATE Category
        CategoryRequestDTO updateRequest = new CategoryRequestDTO("Electronics Updated", "New description");
        
        ResponseEntity<CategoryResponseDTO> updateResponse = restTemplate.exchange(
            "/api/categories/" + categoryId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest),
            CategoryResponseDTO.class
        );
        
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().getName()).isEqualTo("Electronics Updated");
        assertThat(updateResponse.getBody().getDescription()).isEqualTo("New description");
        
        // 4. DELETE Category
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/categories/" + categoryId,
            HttpMethod.DELETE,
            null,
            Void.class
        );
        
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        
        // Debug: verify deletion
        System.out.println("Category exists in DB after DELETE: " + categoryRepository.existsById(categoryId));
        // 5. Verify deletion - GET should return 404
        ResponseEntity<ErrorResponse> notFoundResponse = restTemplate.getForEntity(
            "/api/categories/" + categoryId,
            ErrorResponse.class
        );
        
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void createCategory_DuplicateName_ReturnsConflict() {
        // 1. Create first category
        CategoryRequestDTO request = new CategoryRequestDTO("Electronics", "Devices");
        restTemplate.postForEntity("/api/categories", request, CategoryResponseDTO.class);
        
        // 2. Try to create duplicate
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/categories",
            request,
            ErrorResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).contains("already exists");
    }
    
    @Test
    void getAllCategories_ReturnsMultipleCategories() {
        // 1. Create multiple categories
        restTemplate.postForEntity("/api/categories",
            new CategoryRequestDTO("Electronics", "Devices"),
            CategoryResponseDTO.class);
        
        restTemplate.postForEntity("/api/categories",
            new CategoryRequestDTO("Books", "Publications"),
            CategoryResponseDTO.class);
        
        restTemplate.postForEntity("/api/categories",
            new CategoryRequestDTO("Clothing", "Fashion"),
            CategoryResponseDTO.class);
        
        // 2. GET all categories
        ResponseEntity<List<CategoryResponseDTO>> response = restTemplate.exchange(
            "/api/categories",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CategoryResponseDTO>>() {}
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody())
            .extracting(CategoryResponseDTO::getName)
            .containsExactlyInAnyOrder("Electronics", "Books", "Clothing");
    }
    
    @Test
    void fullProductCRUD_WorksEndToEnd() {
        // 1. Create Category first
        CategoryRequestDTO categoryRequest = new CategoryRequestDTO("Electronics", "Devices");
        ResponseEntity<CategoryResponseDTO> categoryResponse = restTemplate.postForEntity(
            "/api/categories",
            categoryRequest,
            CategoryResponseDTO.class
        );
        Long categoryId = categoryResponse.getBody().getId();
        
        // 2. CREATE Product
        ProductRequestDTO createRequest = new ProductRequestDTO(
            "MacBook Pro",
            "Apple laptop",
            new BigDecimal("2499.99"),
            10,
            categoryId
        );
        
        ResponseEntity<ProductResponseDTO> createResponse = restTemplate.postForEntity(
            "/api/products",
            createRequest,
            ProductResponseDTO.class
        );
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("MacBook Pro");
        assertThat(createResponse.getBody().getCategory().getName()).isEqualTo("Electronics");
        
        Long productId = createResponse.getBody().getId();
        
        // 3. GET Product by ID
        ResponseEntity<ProductResponseDTO> getResponse = restTemplate.getForEntity(
            "/api/products/" + productId,
            ProductResponseDTO.class
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("MacBook Pro");
        
        // 4. UPDATE Product
        ProductRequestDTO updateRequest = new ProductRequestDTO(
            "MacBook Pro 16",
            "Updated description",
            new BigDecimal("2999.99"),
            15,
            categoryId
        );
        
        ResponseEntity<ProductResponseDTO> updateResponse = restTemplate.exchange(
            "/api/products/" + productId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest),
            ProductResponseDTO.class
        );
        
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().getName()).isEqualTo("MacBook Pro 16");
        assertThat(updateResponse.getBody().getPrice()).isEqualByComparingTo(new BigDecimal("2999.99"));
        
        // 5. DELETE Product
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/products/" + productId,
            HttpMethod.DELETE,
            null,
            Void.class
        );
        
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
    
    @Test
    void createProduct_CategoryNotFound_ReturnsNotFound() {
        // Try to create product with non-existent category
        ProductRequestDTO request = new ProductRequestDTO(
            "MacBook Pro",
            "Laptop",
            new BigDecimal("2499.99"),
            10,
            999L  // Non-existent category
        );
        
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/products",
            request,
            ErrorResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("Category not found");
    }
    
    @Test
    void getProductsByCategory_ReturnsFilteredProducts() {
        // 1. Create categories
        ResponseEntity<CategoryResponseDTO> electronicsResponse = restTemplate.postForEntity(
            "/api/categories",
            new CategoryRequestDTO("Electronics", "Devices"),
            CategoryResponseDTO.class
        );
        Long electronicsId = electronicsResponse.getBody().getId();
        
        ResponseEntity<CategoryResponseDTO> booksResponse = restTemplate.postForEntity(
            "/api/categories",
            new CategoryRequestDTO("Books", "Publications"),
            CategoryResponseDTO.class
        );
        Long booksId = booksResponse.getBody().getId();
        
        // 2. Create products in different categories
        restTemplate.postForEntity("/api/products",
            new ProductRequestDTO("MacBook", "Laptop", new BigDecimal("2499.99"), 10, electronicsId),
            ProductResponseDTO.class);
        
        restTemplate.postForEntity("/api/products",
            new ProductRequestDTO("iPhone", "Phone", new BigDecimal("1199.99"), 20, electronicsId),
            ProductResponseDTO.class);
        
        restTemplate.postForEntity("/api/products",
            new ProductRequestDTO("Clean Code", "Book", new BigDecimal("45.99"), 100, booksId),
            ProductResponseDTO.class);
        
        // 3. GET products by Electronics category
        ResponseEntity<List<ProductResponseDTO>> response = restTemplate.exchange(
            "/api/products/category/" + electronicsId,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ProductResponseDTO>>() {}
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
            .extracting(ProductResponseDTO::getName)
            .containsExactlyInAnyOrder("MacBook", "iPhone");
    }
    
    @Test
    void searchProducts_ReturnsMatchingProducts() {
        // 1. Create category
        ResponseEntity<CategoryResponseDTO> categoryResponse = restTemplate.postForEntity(
            "/api/categories",
            new CategoryRequestDTO("Electronics", "Devices"),
            CategoryResponseDTO.class
        );
        Long categoryId = categoryResponse.getBody().getId();
        
        // 2. Create products
        restTemplate.postForEntity("/api/products",
            new ProductRequestDTO("Magic Mouse", "Wireless mouse", new BigDecimal("79.99"), 50, categoryId),
            ProductResponseDTO.class);
        
        restTemplate.postForEntity("/api/products",
            new ProductRequestDTO("Magic Keyboard", "Wireless keyboard", new BigDecimal("149.99"), 30, categoryId),
            ProductResponseDTO.class);
        
        restTemplate.postForEntity("/api/products",
            new ProductRequestDTO("MacBook Pro", "Laptop", new BigDecimal("2499.99"), 10, categoryId),
            ProductResponseDTO.class);
        
        // 3. Search for "magic"
        ResponseEntity<List<ProductResponseDTO>> response = restTemplate.exchange(
            "/api/products/search?keyword=magic",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ProductResponseDTO>>() {}
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
            .extracting(ProductResponseDTO::getName)
            .containsExactlyInAnyOrder("Magic Mouse", "Magic Keyboard");
    }
    
    @Test
    void updateStock_AndDeactivate_Workflow() {
        // 1. Create category and product
        ResponseEntity<CategoryResponseDTO> categoryResponse = restTemplate.postForEntity(
            "/api/categories",
            new CategoryRequestDTO("Electronics", "Devices"),
            CategoryResponseDTO.class
        );
        Long categoryId = categoryResponse.getBody().getId();
        
        ResponseEntity<ProductResponseDTO> productResponse = restTemplate.postForEntity(
            "/api/products",
            new ProductRequestDTO("MacBook", "Laptop", new BigDecimal("2499.99"), 10, categoryId),
            ProductResponseDTO.class
        );
        Long productId = productResponse.getBody().getId();
        
        // 2. Update stock
        ResponseEntity<ProductResponseDTO> stockUpdateResponse = restTemplate.exchange(
            "/api/products/" + productId + "/stock?quantity=25",
            HttpMethod.PATCH,
            null,
            ProductResponseDTO.class
        );
        
        assertThat(stockUpdateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stockUpdateResponse.getBody().getStock()).isEqualTo(25);
        
        // 3. Deactivate product
        ResponseEntity<ProductResponseDTO> deactivateResponse = restTemplate.exchange(
            "/api/products/" + productId + "/deactivate",
            HttpMethod.PATCH,
            null,
            ProductResponseDTO.class
        );
        
        assertThat(deactivateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivateResponse.getBody().isActive()).isFalse();
        
        // 4. Verify GET still works
        ResponseEntity<ProductResponseDTO> getResponse = restTemplate.getForEntity(
            "/api/products/" + productId,
            ProductResponseDTO.class
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().isActive()).isFalse();
        assertThat(getResponse.getBody().getStock()).isEqualTo(25);
    }
}