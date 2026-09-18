package com.ewb.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferResponse {

    private String paymentReference;
    private String idempotencyKey;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String status; // SUCCESS, FAILED_INSUFFICIENT_FUNDS, FAILED_ACCOUNT_FROZEN, FAILED_INVALID_ACCOUNT
    private String message;
    private LocalDateTime timestamp;

    public TransferResponse() {}

    public TransferResponse(String paymentReference, String idempotencyKey, String sourceAccountId,
                            String destinationAccountId, BigDecimal amount, String currency,
                            String status, String message, LocalDateTime timestamp) {
        this.paymentReference = paymentReference;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
