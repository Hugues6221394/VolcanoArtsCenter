package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.ShippingCarrierService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class FedExShippingService implements ShippingCarrierService {
    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private final String accountNumber;

    public FedExShippingService(@Value("${platform.integrations.fedex.base-url:}") String baseUrl,
                                @Value("${platform.integrations.fedex.api-key:}") String apiKey,
                                @Value("${platform.integrations.fedex.api-secret:}") String apiSecret,
                                @Value("${platform.integrations.fedex.account-number:}") String accountNumber) {
        this.restClient = RestClient.builder().build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.accountNumber = accountNumber;
    }

    @Override
    public String provider() { return "FEDEX"; }

    @Override
    public BigDecimal estimate(String destinationCountry, BigDecimal weightKg) {
        if ("Rwanda".equalsIgnoreCase(destinationCountry)) {
            return new BigDecimal("10.00");
        }
        ensureConfigured();
        try {
            String token = oauthToken();
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/rate/v1/rates/quotes")
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of(
                            "accountNumber", Map.of("value", accountNumber),
                            "requestedShipment", Map.of(
                                    "shipper", Map.of("address", Map.of("countryCode", "RW")),
                                    "recipient", Map.of("address", Map.of("countryCode", toIso2(destinationCountry))),
                                    "pickupType", "DROPOFF_AT_FEDEX_LOCATION",
                                    "requestedPackageLineItems", java.util.List.of(Map.of(
                                            "weight", Map.of("units", "KG", "value", weightKg == null ? 1 : weightKg)
                                    ))
                            )
                    ))
                    .retrieve()
                    .body(Map.class);
            Map<?, ?> output = response instanceof Map<?, ?> map ? (Map<?, ?>) map.get("output") : null;
            if (output != null && output.get("rateReplyDetails") instanceof java.util.List<?> details && !details.isEmpty()) {
                Object first = details.getFirst();
                if (first instanceof Map<?, ?> firstMap && firstMap.get("ratedShipmentDetails") instanceof java.util.List<?> rated && !rated.isEmpty()) {
                    Object quote = rated.getFirst();
                    if (quote instanceof Map<?, ?> quoteMap && quoteMap.get("totalNetCharge") instanceof Map<?, ?> charge) {
                        return new BigDecimal(String.valueOf(charge.get("amount")));
                    }
                }
            }
            throw new IllegalStateException("FedEx did not return a shipping quote");
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("FedEx estimate failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String createShipment(String reference) {
        ensureConfigured();
        try {
            String token = oauthToken();
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/ship/v1/shipments")
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of(
                            "labelResponseOptions", "URL_ONLY",
                            "requestedShipment", Map.of(
                                    "shipDatestamp", java.time.LocalDate.now().toString(),
                                    "serviceType", "INTERNATIONAL_PRIORITY",
                                    "packagingType", "YOUR_PACKAGING",
                                    "shipper", Map.of("address", Map.of("countryCode", "RW")),
                                    "recipients", java.util.List.of(Map.of("address", Map.of("countryCode", "US"))),
                                    "pickupType", "DROPOFF_AT_FEDEX_LOCATION",
                                    "shippingChargesPayment", Map.of("paymentType", "SENDER"),
                                    "requestedPackageLineItems", java.util.List.of(Map.of(
                                            "weight", Map.of("units", "KG", "value", 1)
                                    )),
                                    "customerReferences", java.util.List.of(Map.of("customerReferenceType", "CUSTOMER_REFERENCE", "value", reference))
                            ),
                            "accountNumber", Map.of("value", accountNumber)
                    ))
                    .retrieve()
                    .body(Map.class);
            Map<?, ?> output = response instanceof Map<?, ?> map ? (Map<?, ?>) map.get("output") : null;
            if (output != null && output.get("transactionShipments") instanceof java.util.List<?> shipments && !shipments.isEmpty()) {
                Object first = shipments.getFirst();
                if (first instanceof Map<?, ?> firstMap) {
                    Object master = firstMap.get("masterTrackingNumber");
                    if (master != null) {
                        return String.valueOf(master);
                    }
                }
            }
            throw new IllegalStateException("FedEx did not return tracking number");
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("FedEx shipment creation failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private String oauthToken() {
        Map<?, ?> response = restClient.post()
                .uri(baseUrl + "/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("grant_type=client_credentials&client_id="
                        + java.net.URLEncoder.encode(apiKey, java.nio.charset.StandardCharsets.UTF_8)
                        + "&client_secret="
                        + java.net.URLEncoder.encode(apiSecret, java.nio.charset.StandardCharsets.UTF_8))
                .retrieve()
                .body(Map.class);
        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("FedEx OAuth token not returned");
        }
        return String.valueOf(response.get("access_token"));
    }

    private String toIso2(String country) {
        if (country == null || country.isBlank()) {
            return "US";
        }
        return switch (country.trim().toLowerCase()) {
            case "rwanda" -> "RW";
            case "uganda" -> "UG";
            case "kenya" -> "KE";
            case "tanzania" -> "TZ";
            case "france" -> "FR";
            case "belgium" -> "BE";
            case "germany" -> "DE";
            case "united kingdom", "uk" -> "GB";
            case "united states", "usa", "us" -> "US";
            default -> "US";
        };
    }

    private void ensureConfigured() {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank() || accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalStateException("FedEx integration is not configured");
        }
    }
}
