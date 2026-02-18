package com.shopping.payment.model;

public record PaymentResultEvent(
        String orderId,
        String status,
        double amount
) {
}
