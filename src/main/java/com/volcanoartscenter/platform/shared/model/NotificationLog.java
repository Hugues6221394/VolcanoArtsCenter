package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(nullable = false, length = 160)
    private String recipient;

    @Column(length = 220)
    private String subject;

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "external_reference", length = 200)
    private String externalReference;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Channel {
        EMAIL, WHATSAPP
    }

    public enum DeliveryStatus {
        PENDING, SENT, FAILED
    }
}
