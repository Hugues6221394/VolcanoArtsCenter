package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class MtnMomoPaymentService implements PaymentGatewayService {
    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String targetEnvironment;
    private final String subscriptionKey;

    public MtnMomoPaymentService(@Value("${platform.integrations.mtn.base-url:}") String baseUrl,
                                 @Value("${platform.integrations.mtn.api-key:}") String apiKey,
                                 @Value("${platform.integrations.mtn.target-environment:sandbox}") String targetEnvironment,
                                 @Value("${platform.integrations.mtn.subscription-key:}") String subscriptionKey) {
        this.restClient = RestClient.builder().build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.targetEnvironment = targetEnvironment;
        this.subscriptionKey = subscriptionKey;
    }

    @Override
    public String provider() { return "MTN_MOMO"; }

    @Override
    public PaymentResult initialize(String reference, BigDecimal amount, String currency, Map<String, String> metadata) {
        ensureConfigured();
        String payer = metadata == null ? null : metadata.get("msisdn");
        if (payer == null || payer.isBlank()) {
            throw new IllegalArgumentException("MTN MoMo requires msisdn in metadata");
        }
        String externalReference = java.util.UUID.randomUUID().toString();
        try {
            restClient.post()
                    .uri(baseUrl + "/collection/v1_0/requesttopay")
                    .header("X-Reference-Id", externalReference)
                    .header("X-Target-Environment", targetEnvironment)
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "amount", amount.toPlainString(),
                            "currency", currency,
                            "externalId", reference,
                            "payer", Map.of("partyIdType", "MSISDN", "partyId", payer),
                            "payerMessage", "Volcano Arts Center payment",
                            "payeeNote", "Reference " + reference
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return new PaymentResult(true, externalReference, "MTN request created");
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("MTN MoMo initialization failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public PaymentResult verify(String externalReference) {
        ensureConfigured();
        try {
            Map<?, ?> response = restClient.get()
                    .uri(baseUrl + "/collection/v1_0/requesttopay/{reference}", externalReference)
                    .header("X-Target-Environment", targetEnvironment)
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(Map.class);
            String status = response == null ? "UNKNOWN" : String.valueOf(response.get("status"));
            boolean success = "SUCCESSFUL".equalsIgnoreCase(status);
            return new PaymentResult(success, externalReference, "MTN status: " + status);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("MTN MoMo verification failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private void ensureConfigured() {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank() || subscriptionKey == null || subscriptionKey.isBlank()) {
            throw new IllegalStateException("MTN MoMo integration is not configured");
        }
    }
}
