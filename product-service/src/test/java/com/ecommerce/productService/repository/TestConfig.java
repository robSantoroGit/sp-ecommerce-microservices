package com.ecommerce.productService.repository;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.ecommerce.productService.model")
@EnableJpaRepositories("com.ecommerce.productService.repository")
public class TestConfig {

}
