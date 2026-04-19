package com.volcanoartscenter.platform.shared.service;

import com.volcanoartscenter.platform.shared.model.ShippingOrder;
import com.volcanoartscenter.platform.shared.model.WebhookEventLog;
import com.volcanoartscenter.platform.shared.repository.ShippingOrderRepository;
import com.volcanoartscenter.platform.shared.repository.WebhookEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessingService {

    private final WebhookEventLogRepository webhookEventLogRepository;
    private final ShippingOrderRepository shippingOrderRepository;

    @Transactional
    public void processPaymentSuccess(String externalEventId, String provider, String transactionId) {
        if (webhookEventLogRepository.existsByExternalEventId(externalEventId)) {
            log.info("Idempotency guard: Webhook event {} from {} already processed. Ignoring.", externalEventId, provider);
            return;
        }

        try {
            shippingOrderRepository.findByPaymentTransactionId(transactionId).ifPresentOrElse(order -> {
                if (order.getPaymentStatus() != ShippingOrder.PaymentStatus.PAID) {
                    order.setPaymentStatus(ShippingOrder.PaymentStatus.PAID);
                    shippingOrderRepository.save(order);
                    log.info("Order {} marked as PAID via {} webhook.", order.getOrderReference(), provider);
                }
            }, () -> {
                log.warn("Webhook processing: No order found for transaction ID {}", transactionId);
            });

            webhookEventLogRepository.save(WebhookEventLog.builder()
                    .externalEventId(externalEventId)
                    .provider(provider)
                    .eventType("payment_success")
                    .status("PROCESSED")
                    .build());
        } catch (Exception e) {
            log.error("Failed to process webhook event {}", externalEventId, e);
            webhookEventLogRepository.save(WebhookEventLog.builder()
                    .externalEventId(externalEventId)
                    .provider(provider)
                    .eventType("payment_success")
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build());
            throw e; // rethrow to let webhook provider know to retry
        }
    }
}
