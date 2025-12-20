package com.ecommerce.orderservice.bin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.ecommerce.orderservice.service","com.ecommerce.orderservice.controller",
		"com.ecommerce.orderservice.client","com.ecommerce.orderservice.dto","com.ecommerce.orderservice.exception"})
@EnableJpaRepositories(basePackages = "com.ecommerce.orderservice.repository")
@EntityScan(basePackages = "com.ecommerce.orderservice.model")
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

}
