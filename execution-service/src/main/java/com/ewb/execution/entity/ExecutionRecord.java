package com.ewb.execution.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution_records",
       uniqueConstraints = @UniqueConstraint(columnNames = {"standingOrderId", "scheduledOccurrence"}))
public class ExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long standingOrderId;

    @Column(nullable = false)
    private LocalDate scheduledOccurrence;

    private Integer instructionVersion;
    private String status; // PENDING, PROCESSING, SUCCESS, FAILED, UNRESOLVED
    private String paymentReference;
    private String idempotencyKey;

    private String workerClaimId;
    private LocalDateTime leaseExpiresAt;

    private Integer attemptCount;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public ExecutionRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStandingOrderId() { return standingOrderId; }
    public void setStandingOrderId(Long standingOrderId) { this.standingOrderId = standingOrderId; }

    public LocalDate getScheduledOccurrence() { return scheduledOccurrence; }
    public void setScheduledOccurrence(LocalDate scheduledOccurrence) { this.scheduledOccurrence = scheduledOccurrence; }

    public Integer getInstructionVersion() { return instructionVersion; }
    public void setInstructionVersion(Integer instructionVersion) { this.instructionVersion = instructionVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getWorkerClaimId() { return workerClaimId; }
    public void setWorkerClaimId(String workerClaimId) { this.workerClaimId = workerClaimId; }

    public LocalDateTime getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(LocalDateTime leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
