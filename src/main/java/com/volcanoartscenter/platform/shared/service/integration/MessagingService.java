package com.volcanoartscenter.platform.shared.service.integration;

public interface MessagingService {
    String channel();
    void send(String recipient, String subject, String body);
}
