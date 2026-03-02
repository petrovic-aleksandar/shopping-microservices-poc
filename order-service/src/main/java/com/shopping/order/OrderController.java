package com.shopping.order;

import com.shopping.events.OrderCreatedEvent;
import com.shopping.events.PaymentResultEvent;
import com.shopping.events.ReservationResultEvent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

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
        logger.info("Creating new order: id={}, userId={}, productId={}, quantity={}, amount={}", 
            id, request.userId(), request.productId(), request.quantity(), request.amount());
        
        OrderView order = new OrderView(id, request.userId(), request.productId(), request.quantity(), request.amount(), "CREATED");
        orders.put(id, order);
        logger.debug("Order persisted: {}", order);

        OrderCreatedEvent event = new OrderCreatedEvent(id, request.userId(), request.productId(), request.quantity(), request.amount());
        kafkaTemplate.send("order.created", id, event);
        logger.info("OrderCreatedEvent published: orderId={}, event={}", id, event);

        return order;
    }

    @GetMapping("/orders/{id}")
    public OrderView getOrder(@PathVariable String id) {
        return orders.getOrDefault(id, new OrderView(id, "unknown", "unknown", 0, 0, "NOT_FOUND"));
    }

    @KafkaListener(topics = "payment.succeeded", groupId = "order-group")
    public void onPaymentSucceeded(PaymentResultEvent event) {
        logger.info("Payment succeeded event received: orderId={}, status={}, amount={}", 
            event.orderId(), event.status(), event.amount());
        
        orders.computeIfPresent(event.orderId(), (key, existing) -> {
            OrderView updated = new OrderView(existing.id(), existing.userId(), existing.productId(), existing.quantity(), existing.amount(), "CONFIRMED");
            logger.info("Order status updated to CONFIRMED: id={}, details={}", existing.id(), updated);
            return updated;
        });
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-group")
    public void onPaymentFailed(PaymentResultEvent event) {
        logger.warn("Payment failed event received: orderId={}, status={}, amount={}", 
            event.orderId(), event.status(), event.amount());
        
        orders.computeIfPresent(event.orderId(), (key, existing) -> {
            OrderView updated = new OrderView(existing.id(), existing.userId(), existing.productId(), existing.quantity(), existing.amount(), "FAILED");
            logger.error("Order marked as FAILED: id={}, reason=payment_failed");
            return updated;
        });
    }

    @KafkaListener(topics = "reservation.failed", groupId = "order-group")
    public void onReservationFailed(ReservationResultEvent event) {
        logger.warn("Reservation failed event received: orderId={}, productId={}, quantity={}", 
            event.orderId(), event.productId(), event.quantity());
        
        orders.computeIfPresent(event.orderId(), (key, existing) -> {
            OrderView updated = new OrderView(existing.id(), existing.userId(), existing.productId(), existing.quantity(), existing.amount(), "RESERVATION_FAILED");
            logger.error("Order marked as RESERVATION_FAILED: id={}, reason=insufficient_stock");
            return updated;
        });
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
