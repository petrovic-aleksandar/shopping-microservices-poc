package com.shopping.order.model;

public record ReservationResultEvent(
        String orderId,
        String productId,
        int quantity,
        boolean reserved
) {
}
