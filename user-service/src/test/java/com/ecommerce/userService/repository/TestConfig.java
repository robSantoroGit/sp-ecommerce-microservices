package com.ecommerce.userService.repository;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.ecommerce.userService.repository")
@EntityScan(basePackages = "com.ecommerce.userService.model")
public class TestConfig {

}
