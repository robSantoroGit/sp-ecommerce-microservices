package com.ecommerce.userService.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("E-Commerce Microservices Platform - User Management Service. " +
                                    "Handles user registration, authentication, and profile management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Rob")  // 👈 Tuo nome
                                .email("robesantoro@gmail.com")  // 👈 Tua email
                                .url("https://github.com/robSantoroGit/sp-ecommerce-microservices"))  // 👈 Tuo repo
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}