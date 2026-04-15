package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.model.NotificationLog;
import com.volcanoartscenter.platform.shared.repository.NotificationLogRepository;
import com.volcanoartscenter.platform.shared.service.integration.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class WhatsAppMessagingService implements MessagingService {

    private final NotificationLogRepository notificationLogRepository;
    private final RestClient restClient = RestClient.builder().build();

    @Value("${platform.integrations.whatsapp.api-url:}")
    private String apiUrl;
    @Value("${platform.integrations.whatsapp.api-token:}")
    private String apiToken;
    @Value("${platform.notifications.whatsapp.enabled:false}")
    private boolean enabled;
    @Value("${platform.notifications.whatsapp.businessNumber:}")
    private String businessNumber;

    @Override
    public String channel() { return "WHATSAPP"; }

    @Override
    public void send(String recipient, String subject, String body) {
        if (!enabled) {
            notificationLogRepository.save(NotificationLog.builder()
                    .channel(NotificationLog.Channel.WHATSAPP)
                    .recipient(recipient)
                    .subject(subject)
                    .messageBody(body)
                    .status(NotificationLog.DeliveryStatus.PENDING)
                    .externalReference("whatsapp-disabled")
                    .build());
            return;
        }
        if (apiUrl == null || apiUrl.isBlank() || apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("WhatsApp integration is not configured");
        }
        try {
            java.util.Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiToken)
                    .body(java.util.Map.of(
                            "messaging_product", "whatsapp",
                            "to", recipient,
                            "type", "text",
                            "from", businessNumber,
                            "text", java.util.Map.of("body", subject + "\n" + body)
                    ))
                    .retrieve()
                    .body(java.util.Map.class);
            String externalRef = response == null ? null : String.valueOf(response.getOrDefault("messages", "whatsapp-api"));
            notificationLogRepository.save(NotificationLog.builder()
                    .channel(NotificationLog.Channel.WHATSAPP)
                    .recipient(recipient)
                    .subject(subject)
                    .messageBody(body)
                    .status(NotificationLog.DeliveryStatus.SENT)
                    .externalReference(externalRef)
                    .build());
        } catch (RestClientResponseException ex) {
            notificationLogRepository.save(NotificationLog.builder()
                    .channel(NotificationLog.Channel.WHATSAPP)
                    .recipient(recipient)
                    .subject(subject)
                    .messageBody(body)
                    .status(NotificationLog.DeliveryStatus.FAILED)
                    .externalReference("whatsapp-error")
                    .build());
            throw new IllegalStateException("WhatsApp delivery failed: " + ex.getResponseBodyAsString(), ex);
        }
    }
}
