package com.shopping.payment.model;

public record ReservationResultEvent(
        String orderId,
        String productId,
        int quantity,
        boolean reserved
) {
}
