package com.shopping.payment;

import com.shopping.events.PaymentResultEvent;
import com.shopping.events.ReservationResultEvent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PaymentController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentController(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "payment-service", "status", "UP");
    }

    @PostMapping("/payments/charge")
    public ChargeResponse charge(@RequestBody ChargeRequest request) {
        String status = request.amount() <= 1000 ? "SUCCEEDED" : "FAILED";
        return new ChargeResponse(request.orderId(), request.amount(), status);
    }

    @KafkaListener(topics = "reservation.succeeded", groupId = "payment-group")
    public void consumeReservationSucceeded(ReservationResultEvent event) {
        // Only process payment after successful reservation
        String status = event.quantity() * 100 <= 1000 ? "SUCCEEDED" : "FAILED"; // Simple logic based on quantity
        PaymentResultEvent result = new PaymentResultEvent(event.orderId(), status, event.quantity() * 100);

        if ("SUCCEEDED".equals(status)) {
            kafkaTemplate.send("payment.succeeded", event.orderId(), result);
        } else {
            kafkaTemplate.send("payment.failed", event.orderId(), result);
        }
    }

    public record ChargeRequest(@NotBlank String orderId, @Min(1) double amount) {
    }

    public record ChargeResponse(String orderId, double amount, String status) {
    }
}
