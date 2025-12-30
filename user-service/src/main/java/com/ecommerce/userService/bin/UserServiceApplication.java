package com.ecommerce.userService.bin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.ecommerce.userService.dto","com.ecommerce.userService.service","com.ecommerce.userService.controller"
		,"com.ecommerce.userService.config","com.ecommerce.userService.exception","com.ecommerce.userService.util", "com.ecommerce.userService.filter"})
@EnableJpaRepositories(basePackages = "com.ecommerce.userService.repository")
@EntityScan(basePackages = "com.ecommerce.userService.model")
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
