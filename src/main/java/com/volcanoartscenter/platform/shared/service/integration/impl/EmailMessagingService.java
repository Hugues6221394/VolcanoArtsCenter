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
public class EmailMessagingService implements MessagingService {

    private final NotificationLogRepository notificationLogRepository;
    private final RestClient restClient = RestClient.builder().build();

    @Value("${platform.integrations.email.api-url:}")
    private String apiUrl;
    @Value("${platform.integrations.email.api-key:}")
    private String apiKey;
    @Value("${platform.notifications.email.enabled:true}")
    private boolean enabled;
    @Value("${platform.notifications.email.from:noreply@volcanoartscenter.rw}")
    private String fromAddress;

    @Override
    public String channel() { return "EMAIL"; }

    @Override
    public void send(String recipient, String subject, String body) {
        if (!enabled) {
            notificationLogRepository.save(NotificationLog.builder()
                    .channel(NotificationLog.Channel.EMAIL)
                    .recipient(recipient)
                    .subject(subject)
                    .messageBody(body)
                    .status(NotificationLog.DeliveryStatus.PENDING)
                    .externalReference("email-disabled")
                    .build());
            return;
        }
        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Email integration is not configured");
        }
        try {
            java.util.Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(java.util.Map.of(
                            "from", fromAddress,
                            "to", recipient,
                            "subject", subject,
                            "html", body
                    ))
                    .retrieve()
                    .body(java.util.Map.class);
            String externalRef = response == null ? null : String.valueOf(response.getOrDefault("id", "email-api"));
            notificationLogRepository.save(NotificationLog.builder()
                    .channel(NotificationLog.Channel.EMAIL)
                    .recipient(recipient)
                    .subject(subject)
                    .messageBody(body)
                    .status(NotificationLog.DeliveryStatus.SENT)
                    .externalReference(externalRef)
                    .build());
        } catch (RestClientResponseException ex) {
            notificationLogRepository.save(NotificationLog.builder()
                    .channel(NotificationLog.Channel.EMAIL)
                    .recipient(recipient)
                    .subject(subject)
                    .messageBody(body)
                    .status(NotificationLog.DeliveryStatus.FAILED)
                    .externalReference("email-error")
                    .build());
            throw new IllegalStateException("Email delivery failed: " + ex.getResponseBodyAsString(), ex);
        }
    }
}
