package com.shopping.inventory;

import com.shopping.inventory.model.OrderCreatedEvent;
import com.shopping.inventory.model.ReservationResultEvent;
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

    @KafkaListener(topics = "order.created", groupId = "inventory-group")
    public void consumeOrderCreated(OrderCreatedEvent event) {
        boolean reserved = tryReserve(event.productId(), event.quantity());
        ReservationResultEvent result = new ReservationResultEvent(
                event.orderId(),
                event.productId(),
                event.quantity(),
                reserved
        );
        
        // Publish reservation result
        if (reserved) {
            kafkaTemplate.send("reservation.succeeded", event.orderId(), result);
        } else {
            kafkaTemplate.send("reservation.failed", event.orderId(), result);
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
}
