package com.ewb.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;
    private String paymentReference;
    private String requestFingerprint;
    private String status;

    @Column(length = 2000)
    private String responsePayload;
    private LocalDateTime createdAt;

    public IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String paymentReference, String requestFingerprint,
                             String status, String responsePayload, LocalDateTime createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.paymentReference = paymentReference;
        this.requestFingerprint = requestFingerprint;
        this.status = status;
        this.responsePayload = responsePayload;
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
