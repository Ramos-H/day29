package com.ewb.execution.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private String eventId; // UUID

    private Long executionId;
    private String eventType; // PAYMENT_COMPLETED, PAYMENT_FAILED

    @Column(length = 2000)
    private String payload;

    private String publicationStatus; // PENDING, PUBLISHED, FAILED
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public OutboxEvent() {}

    public OutboxEvent(String eventId, Long executionId, String eventType, String payload,
                       String publicationStatus, Integer retryCount, LocalDateTime createdAt) {
        this.eventId = eventId;
        this.executionId = executionId;
        this.eventType = eventType;
        this.payload = payload;
        this.publicationStatus = publicationStatus;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(String publicationStatus) { this.publicationStatus = publicationStatus; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
