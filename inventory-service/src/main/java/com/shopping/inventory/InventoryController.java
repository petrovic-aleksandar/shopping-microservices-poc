package com.shopping.inventory;

import com.shopping.events.OrderCreatedEvent;
import com.shopping.events.PaymentResultEvent;
import com.shopping.events.ReservationResultEvent;
import com.shopping.inventory.model.OrderReservation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class InventoryController {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();
    private final Map<String, OrderReservation> reservations = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryController(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        stock.put("product-1", 100);
        stock.put("product-2", 50);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "inventory-service", "status", "UP");
    }

    @GetMapping("/inventory")
    public Map<String, Integer> currentStock() {
        return stock;
    }

    @PostMapping("/inventory/reserve")
    public ReserveResponse reserve(@RequestBody ReserveRequest request) {
        boolean success = tryReserve(request.productId(), request.quantity());
        return new ReserveResponse(request.orderId(), request.productId(), request.quantity(), success);
    }

    @PostMapping("/inventory/add")
    public AddStockResponse addStock(@RequestBody AddStockRequest request) {
        synchronized (stock) {
            int current = stock.getOrDefault(request.productId(), 0);
            stock.put(request.productId(), current + request.quantity());
            return new AddStockResponse(request.productId(), current + request.quantity());
        }
    }

    @KafkaListener(topics = "order.created", groupId = "inventory-group")
    public void consumeOrderCreated(OrderCreatedEvent event) {
        boolean reserved = tryReserve(event.productId(), event.quantity());
        ReservationResultEvent result = new ReservationResultEvent(
                event.orderId(),
                event.productId(),
                event.quantity(),
                reserved
        );
        
        // Store reservation for later compensation if payment fails
        if (reserved) {
            reservations.put(event.orderId(), 
                new OrderReservation(event.productId(), event.quantity()));
            kafkaTemplate.send("reservation.succeeded", event.orderId(), result);
        } else {
            kafkaTemplate.send("reservation.failed", event.orderId(), result);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "inventory-group")
    public void onPaymentFailed(PaymentResultEvent event) {
        // Release reserved inventory when payment fails (compensation)
        releaseReservation(event.orderId());
    }

    private void releaseReservation(String orderId) {
        OrderReservation reservation = reservations.remove(orderId);
        if (reservation != null) {
            synchronized (stock) {
                int current = stock.getOrDefault(reservation.productId(), 0);
                stock.put(reservation.productId(), current + reservation.quantity());
            }
        }
    }

    private boolean tryReserve(String productId, int quantity) {
        synchronized (stock) {
            int existing = stock.getOrDefault(productId, 0);
            if (existing < quantity) {
                return false;
            }
            stock.put(productId, existing - quantity);
            return true;
        }
    }

    public record ReserveRequest(@NotBlank String orderId, @NotBlank String productId, @Min(1) int quantity) {
    }

    public record ReserveResponse(String orderId, String productId, int quantity, boolean reserved) {
    }

    public record AddStockRequest(@NotBlank String productId, @Min(1) int quantity) {
    }

    public record AddStockResponse(String productId, int newTotal) {
    }
}
