package com.shopping.inventory.model;

public record OrderReservation(
        String productId,
        int quantity
) {
}
