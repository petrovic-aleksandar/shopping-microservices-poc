package com.shopping.order.model;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        String productId,
        int quantity,
        double amount
) {
}
