package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.service.WebhookProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookProcessingService webhookProcessingService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody Map<String, Object> payload) {
        
        // In a production environment with the Stripe SDK, we would use:
        // Event event = Webhook.constructEvent(payloadString, signature, endpointSecret);
        
        try {
            // Simulated Stripe Event Parsing
            String type = (String) payload.get("type");
            String eventId = (String) payload.get("id");
            
            log.info("Received Stripe webhook. Event: {}, Type: {}", eventId, type);

            if ("checkout.session.completed".equals(type) || "payment_intent.succeeded".equals(type)) {
                // Extract the relevant transaction reference from Stripe data object
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                @SuppressWarnings("unchecked")
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                
                String transactionId = (String) object.get("id"); // checkout session id or intent id
                
                webhookProcessingService.processPaymentSuccess(eventId, "STRIPE", transactionId);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            // Return 400 to signal to Stripe that it should retry exactly as the spec requires
            return ResponseEntity.badRequest().body("Webhook processing failed");
        }
    }
}
