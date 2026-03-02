package com.shopping.payment;

import com.shopping.events.PaymentResultEvent;
import com.shopping.events.ReservationResultEvent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

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
        logger.info("Processing payment charge: orderId={}, amount={}", request.orderId(), request.amount());
        String status = request.amount() <= 1000 ? "SUCCEEDED" : "FAILED";
        logger.info("Payment charge completed: orderId={}, amount={}, status={}", request.orderId(), request.amount(), status);
        return new ChargeResponse(request.orderId(), request.amount(), status);
    }

    @KafkaListener(topics = "reservation.succeeded", groupId = "payment-group")
    public void consumeReservationSucceeded(ReservationResultEvent event) {
        logger.info("Processing ReservationSucceededEvent: orderId={}, productId={}, quantity={}", 
            event.orderId(), event.productId(), event.quantity());
        
        // Only process payment after successful reservation
        double amount = event.quantity() * 100;
        String status = amount <= 1000 ? "SUCCEEDED" : "FAILED"; // Simple logic based on quantity
        PaymentResultEvent result = new PaymentResultEvent(event.orderId(), status, amount);

        if ("SUCCEEDED".equals(status)) {
            logger.info("Payment processing SUCCEEDED: orderId={}, productId={}, quantity={}, amount={}", 
                event.orderId(), event.productId(), event.quantity(), amount);
            kafkaTemplate.send("payment.succeeded", event.orderId(), result);
        } else {
            logger.warn("Payment processing FAILED: orderId={}, productId={}, quantity={}, amount={}, reason=amount_exceeds_limit", 
                event.orderId(), event.productId(), event.quantity(), amount);
            kafkaTemplate.send("payment.failed", event.orderId(), result);
        }
    }

    public record ChargeRequest(@NotBlank String orderId, @Min(1) double amount) {
    }

    public record ChargeResponse(String orderId, double amount, String status) {
    }
}
