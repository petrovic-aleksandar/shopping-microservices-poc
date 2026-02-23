package com.shopping.events;

public record PaymentResultEvent(
        String orderId,
        String status,
        double amount
) {
}
