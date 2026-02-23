package com.shopping.events;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        String productId,
        int quantity,
        double amount
) {
}
