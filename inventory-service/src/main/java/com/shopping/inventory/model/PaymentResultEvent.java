package com.shopping.inventory.model;

public record PaymentResultEvent(
        String orderId,
        String status,
        double amount
) {
}
