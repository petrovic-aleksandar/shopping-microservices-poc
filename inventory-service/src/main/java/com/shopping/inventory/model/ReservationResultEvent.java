package com.shopping.inventory.model;

public record ReservationResultEvent(
        String orderId,
        String productId,
        int quantity,
        boolean reserved
) {
}
