package com.ecommerce.apigateway.filter;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestEnrichmentFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestEnrichmentFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_TIMESTAMP_HEADER = "X-Request-Timestamp";
    private static final String GATEWAY_VERSION_HEADER = "X-Gateway-Version";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or retrieve correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID: {}", correlationId);
        }

        // Add headers to request
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(REQUEST_TIMESTAMP_HEADER, Instant.now().toString())
                .header(GATEWAY_VERSION_HEADER, "1.0.0")
                .build();

        log.debug("Request enriched with headers: correlationId={}, timestamp={}", 
                correlationId, Instant.now());

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return -2; // Execute before logging filter
    }
}