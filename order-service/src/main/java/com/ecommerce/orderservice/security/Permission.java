package com.ecommerce.orderservice.security;

public interface Permission {
    String ORDER_READ = "order.read";
    String ORDER_WRITE = "order.write";
    String ORDER_DELETE = "order.delete";
}