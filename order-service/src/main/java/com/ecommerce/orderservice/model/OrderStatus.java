package com.ecommerce.orderservice.model;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    PAID,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    // Valid status transitions
    private static final Set<String> VALID_TRANSITIONS = Set.of(
        "PENDING->PAID",
        "PAID->CONFIRMED",
        "PAID->CANCELLED",
        "CONFIRMED->SHIPPED",
        "SHIPPED->DELIVERED",
        "PENDING->CANCELLED"
    );

    /**
     * Check if transition from current status to new status is valid
     */
    public static boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) {
            return true; // Same status is always valid
        }
        String transition = from.name() + "->" + to.name();
        return VALID_TRANSITIONS.contains(transition);
    }

    /**
     * Get valid next statuses from current status
     */
    public static Set<OrderStatus> getValidNextStatuses(OrderStatus current) {
        return switch (current) {
            case PENDING -> Set.of(PAID, CANCELLED);
            case PAID -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(SHIPPED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of(); // Terminal states
        };
    }

    /**
     * Check if status is terminal (cannot be changed)
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    /**
     * Check if order can be cancelled from this status
     */
    public boolean isCancellable() {
        return this == PENDING || this == PAID;
    }
}