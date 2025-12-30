package com.ecommerce.userService.filter;

import com.ecommerce.userService.util.CorrelationIdConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String correlationId = request.getHeader(CorrelationIdConstants.CORRELATION_ID_HEADER);
            System.out.println("DEBUG: Header ricevuto = " + correlationId); // ← AGGIUNGI QUESTO
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            MDC.put(CorrelationIdConstants.CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(CorrelationIdConstants.CORRELATION_ID_HEADER, correlationId);
            
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}