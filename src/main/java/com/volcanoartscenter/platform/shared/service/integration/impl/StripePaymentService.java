package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;

@Service
public class StripePaymentService implements PaymentGatewayService {
    private final RestClient restClient;
    private final String secretKey;

    public StripePaymentService(@Value("${platform.integrations.stripe.secret-key:}") String secretKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.stripe.com/v1")
                .build();
        this.secretKey = secretKey;
    }

    @Override
    public String provider() { return "STRIPE_CARD"; }

    @Override
    public PaymentResult initialize(String reference, BigDecimal amount, String currency, Map<String, String> metadata) {
        ensureConfigured();
        long cents = amount.multiply(new BigDecimal("100")).longValue();
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/payment_intents")
                    .header("Authorization", "Basic " + basicToken(secretKey))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body("amount=" + cents
                            + "&currency=" + currency.toLowerCase()
                            + "&confirm=false"
                            + "&description=" + encode("VolcanoArts:" + reference)
                            + "&metadata[reference]=" + encode(reference))
                    .retrieve()
                    .body(Map.class);
            String id = response == null ? null : String.valueOf(response.get("id"));
            return new PaymentResult(id != null && !id.isBlank(), id, "Stripe payment intent created");
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Stripe initialization failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public PaymentResult verify(String externalReference) {
        ensureConfigured();
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/payment_intents/{id}", externalReference)
                    .header("Authorization", "Basic " + basicToken(secretKey))
                    .retrieve()
                    .body(Map.class);
            String status = response == null ? "unknown" : String.valueOf(response.get("status"));
            boolean success = "succeeded".equalsIgnoreCase(status) || "requires_capture".equalsIgnoreCase(status);
            return new PaymentResult(success, externalReference, "Stripe status: " + status);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Stripe verification failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private void ensureConfigured() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe integration is not configured");
        }
    }

    private String basicToken(String key) {
        return Base64.getEncoder().encodeToString((key + ":").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
