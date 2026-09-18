package com.ewb.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NotificationEvent {
    private String eventId;
    private Long executionId;
    private Long standingOrderId;
    private String customerId;
    private String eventType;
    private String paymentReference;
    private BigDecimal amount;
    private String status;
    private String details;
    private LocalDateTime timestamp;

    public NotificationEvent() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public Long getStandingOrderId() { return standingOrderId; }
    public void setStandingOrderId(Long standingOrderId) { this.standingOrderId = standingOrderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
