package com.ecommerce.orderservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.ecommerce.orderservice.dto.ProductDTO;
import com.ecommerce.orderservice.exception.ExternalServiceException;
import com.ecommerce.orderservice.exception.ForbiddenException;
import com.ecommerce.orderservice.exception.InsufficientStockException;
import com.ecommerce.orderservice.exception.ProductNotFoundException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestClient restClient;
    private final String productServiceUrl;

    public ProductServiceClient(@Value("${services.product-service.url}") String productServiceUrl) {
        this.productServiceUrl = productServiceUrl;
        this.restClient = RestClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    /**
     * Get product by ID
     * @param productId the product ID
     * @return ProductDTO
     * @throws ProductNotFoundException if product not found
     * @throws ExternalServiceException if service call fails
     */
    @CircuitBreaker( name = "productService", fallbackMethod = "getProductFallback" )
    @Retry(name = "productService")
    public ProductDTO getProduct(Long productId, Long authenticatedUserId, String scopes) {
        try {
            logger.debug("Calling Product Service to get product: {}", productId);
            
            ProductDTO product = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .header("X-User-Id", authenticatedUserId.toString())
                    .header("X-User-Scopes", scopes)
                    .retrieve()
                    .body(ProductDTO.class);

            if (product == null) {
                throw new ProductNotFoundException(productId);
            }

            logger.debug("Product retrieved successfully: {}", productId);
            return product;

        } catch (HttpClientErrorException.NotFound e) {
            logger.error("Product not found: {}", productId);
            throw new ProductNotFoundException(productId);
        } 
	    catch (HttpClientErrorException.Forbidden e) {
	        logger.error("Product permission denied for updating stock, with user {} and product {}", authenticatedUserId, productId);
	        throw e;
	    }
        catch (Exception e) {
            logger.error("Error calling Product Service for productId: {}", productId, e);
            throw new ExternalServiceException("Product Service", e);
        }
    }

    /**
     * Validate product availability and stock
     * @param productId the product ID
     * @param requestedQuantity the quantity requested
     * @return ProductDTO if valid
     * @throws ProductNotFoundException if product not found
     * @throws InsufficientStockException if insufficient stock
     */
    public ProductDTO validateProductAndStock(Long productId, Integer requestedQuantity, Long authenticatedUserId, String scopes) {
        ProductDTO product = getProduct(productId, authenticatedUserId, scopes);

        // Check if product is active
        if (product.getActive() == null || !product.getActive()) {
            throw new ProductNotFoundException("Product with id " + productId + " is not active");
        }

        // Check stock availability
        if (product.getStock() < requestedQuantity) {
            throw new InsufficientStockException(productId, requestedQuantity, product.getStock());
        }

        return product;
    }

    /**
     * Update product stock
     * @param productId the product ID
     * @param newStock the new stock quantity
     * @throws ExternalServiceException if service call fails
     */
    @Retry(name = "productService")
    public void updateProductStock(Long productId, Integer newStock) {
        try {
            logger.debug("Calling Product Service to update stock for product: {} to {}", productId, newStock);
            
            restClient.patch()
                    .uri("/api/products/{id}/stock?quantity={quantity}", productId, newStock)
                    .header("X-Username", "SYSTEM")
                    .retrieve()
                    .toBodilessEntity();

            logger.debug("Product stock updated successfully for product: {}", productId);

        } catch (Exception e) {
            logger.error("Error updating stock for productId: {}", productId, e);
            throw new ExternalServiceException("Product Service", e);
        }
    }
    
    // ============================================
    // FALLBACK METHODS
    // ============================================

    /**
     * Fallback for getProduct
     */
    private ProductDTO getProductFallback(Long productId, Long authenticatedUserId, String scopes, Throwable throwable) {
        logger.error("Product Service unavailable. Using fallback for getProduct: {}. Error: {}", 
                productId, throwable.getMessage());
        
        // Opzioni:
        // 1. Restituire dati da cache
        // 2. Restituire ProductDTO con dati minimi
        // 3. Rilancia exception (attuale)
        
        throw new ExternalServiceException("Product Service", 
                "Product Service is currently unavailable. Please try again later.");
    }
    
}