package com.shopping.events;

public record ReservationResultEvent(
        String orderId,
        String productId,
        int quantity,
        boolean reserved
) {
}
