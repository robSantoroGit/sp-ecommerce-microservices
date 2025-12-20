package com.ecommerce.orderservice.repository;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.ecommerce.orderservice.model")
@EnableJpaRepositories("com.ecommerce.orderservice.repository")
public class TestConfig {

}
