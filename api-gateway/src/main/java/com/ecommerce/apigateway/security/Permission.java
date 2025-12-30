package com.ecommerce.apigateway.security;

public interface Permission {
    // User permissions
    String USER_READ = "user.read";
    String USER_WRITE = "user.write";
    String USER_DELETE = "user.delete";
    
    // Product permissions
    String PRODUCT_READ = "product.read";
    String PRODUCT_WRITE = "product.write";
    String PRODUCT_DELETE = "product.delete";
    
    // Order permissions
    String ORDER_READ = "order.read";
    String ORDER_WRITE = "order.write";
    String ORDER_DELETE = "order.delete";
}