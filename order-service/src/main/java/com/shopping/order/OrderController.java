package com.shopping.order;

import com.shopping.events.OrderCreatedEvent;
import com.shopping.events.PaymentResultEvent;
import com.shopping.events.ReservationResultEvent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class OrderController {

    private final Map<String, OrderView> orders = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderController(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "order-service", "status", "UP");
    }

    @PostMapping("/orders")
    public OrderView createOrder(@RequestBody CreateOrderRequest request) {
        String id = UUID.randomUUID().toString();
        OrderView order = new OrderView(id, request.userId(), request.productId(), request.quantity(), request.amount(), "CREATED");
        orders.put(id, order);

        OrderCreatedEvent event = new OrderCreatedEvent(id, request.userId(), request.productId(), request.quantity(), request.amount());
        kafkaTemplate.send("order.created", id, event);

        return order;
    }

    @GetMapping("/orders/{id}")
    public OrderView getOrder(@PathVariable String id) {
        return orders.getOrDefault(id, new OrderView(id, "unknown", "unknown", 0, 0, "NOT_FOUND"));
    }

    @KafkaListener(topics = "payment.succeeded", groupId = "order-group")
    public void onPaymentSucceeded(PaymentResultEvent event) {
        orders.computeIfPresent(event.orderId(), (key, existing) ->
                new OrderView(existing.id(), existing.userId(), existing.productId(), existing.quantity(), existing.amount(), "CONFIRMED"));
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-group")
    public void onPaymentFailed(PaymentResultEvent event) {
        orders.computeIfPresent(event.orderId(), (key, existing) ->
                new OrderView(existing.id(), existing.userId(), existing.productId(), existing.quantity(), existing.amount(), "FAILED"));
    }

    @KafkaListener(topics = "reservation.failed", groupId = "order-group")
    public void onReservationFailed(ReservationResultEvent event) {
        orders.computeIfPresent(event.orderId(), (key, existing) ->
                new OrderView(existing.id(), existing.userId(), existing.productId(), existing.quantity(), existing.amount(), "RESERVATION_FAILED"));
    }

    public record CreateOrderRequest(
            @NotBlank String userId,
            @NotBlank String productId,
            @Min(1) int quantity,
            @Min(1) double amount
    ) {
    }

    public record OrderView(String id, String userId, String productId, int quantity, double amount, String status) {
    }
}
