package com.ecommerce.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

	public RateLimiterConfig() {
		System.out.println("========== RateLimiterConfig INITIALIZED ==========");
	}

	@Bean
	public KeyResolver userKeyResolver() {
		System.out.println("========== userKeyResolver BEAN CREATED ==========");
		return exchange -> {
			String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

			if (userId == null || userId.isBlank()) {
				// IP address come fallback
				String ip = exchange.getRequest().getRemoteAddress()
						.getAddress().getHostAddress();
				System.out.println("DEBUG KeyResolver: IP = " + ip);  // ← DEBUG
				return Mono.just(ip);
			}

			System.out.println("DEBUG KeyResolver: UserId = " + userId);  // ← DEBUG
			return Mono.just(userId);
		};
	}
}