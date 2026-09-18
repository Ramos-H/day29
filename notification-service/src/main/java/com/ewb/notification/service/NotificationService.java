package com.ewb.notification.service;

import com.ewb.notification.dto.NotificationEvent;
import com.ewb.notification.entity.NotificationDelivery;
import com.ewb.notification.repository.NotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationDeliveryRepository repository;

    public NotificationService(NotificationDeliveryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationDelivery processEvent(NotificationEvent event) {
        // Idempotent Deduplication Check
        Optional<NotificationDelivery> existing = repository.findByEventId(event.getEventId());
        if (existing.isPresent()) {
            log.info("Duplicate notification event received for eventId {}. Skipping re-delivery.", event.getEventId());
            return existing.get();
        }

        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setEventId(event.getEventId());
        delivery.setExecutionId(event.getExecutionId());
        delivery.setStandingOrderId(event.getStandingOrderId());
        delivery.setCustomerId(event.getCustomerId() != null ? event.getCustomerId() : "CUST-1001");
        delivery.setEventType(event.getEventType());
        delivery.setPaymentReference(event.getPaymentReference());
        delivery.setAmount(event.getAmount());
        delivery.setChannel("SMS");

        String msg = String.format("EWB Alert: Your recurring transfer of ₱%s (%s) has status: %s. %s",
                event.getAmount() != null ? event.getAmount() : "0.00",
                event.getEventType(),
                event.getStatus(),
                event.getDetails() != null ? event.getDetails() : "");
        delivery.setMessage(msg);
        delivery.setStatus("DELIVERED");
        delivery.setDeliveredAt(LocalDateTime.now());

        NotificationDelivery saved = repository.save(delivery);
        log.info("Dispatched notification for customer {}: {}", saved.getCustomerId(), saved.getMessage());
        return saved;
    }

    public List<NotificationDelivery> getNotifications(String customerId) {
        if (customerId != null && !customerId.isBlank()) {
            return repository.findByCustomerIdOrderByDeliveredAtDesc(customerId);
        }
        return repository.findAll();
    }
}
