package com.shopping.order.model;

public record PaymentResultEvent(
        String orderId,
        String status,
        double amount
) {
}
