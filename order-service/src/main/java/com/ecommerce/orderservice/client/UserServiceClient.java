package com.ecommerce.orderservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.ecommerce.orderservice.dto.UserDTO;
import com.ecommerce.orderservice.exception.ExternalServiceException;
import com.ecommerce.orderservice.exception.UserNotFoundException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestClient restClient;
    private final String userServiceUrl;

    public UserServiceClient(@Value("${services.user-service.url}") String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    /**
     * Validate if user exists
     * @param userId the user ID to validate
     * @return UserDTO if user exists
     * @throws UserNotFoundException if user not found
     * @throws ExternalServiceException if service call fails
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "validateUserFallback")
    @Retry(name = "userService")
    public UserDTO validateUser(Long userId) {
        try {
            logger.debug("Calling User Service to validate user: {}", userId);
            
            UserDTO user = restClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .body(UserDTO.class);

            if (user == null) {
                throw new UserNotFoundException(userId);
            }

            logger.debug("User validated successfully: {}", userId);
            return user;

        } catch (HttpClientErrorException.NotFound e) {
            logger.error("User not found: {}", userId);
            throw new UserNotFoundException(userId);
        } catch (Exception e) {
            logger.error("Error calling User Service for userId: {}", userId, e);
            throw new ExternalServiceException("User Service", e);
        }
    }
    
    /**
     * Fallback method when User Service is unavailable
     */
    private UserDTO validateUserFallback(Long userId, Throwable throwable) {
        logger.error("User Service unavailable. Using fallback for userId: {}. Error: {}", 
                userId, throwable.getMessage());
        
        // In produzione potresti:
        // 1. Controllare una cache
        // 2. Usare dati di default
        // 3. Permettere l'ordine con validazione posticipata
        
        // Per ora, rilancia l'eccezione per bloccare l'ordine
        throw new ExternalServiceException("User Service", 
                "User Service is currently unavailable. Please try again later.");
    }
}